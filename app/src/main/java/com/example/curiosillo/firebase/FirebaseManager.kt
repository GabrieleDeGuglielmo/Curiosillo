package com.example.curiosillo.firebase

import android.util.Log
import com.example.curiosillo.data.Scoperta
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

object FirebaseManager {

    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    val utenteCorrente: FirebaseUser?
        get() = auth.currentUser
    val uid: String?
        get() = auth.currentUser?.uid
    val isLoggato: Boolean
        get() = auth.currentUser != null

    // ── Auth ──────────────────────────────────────────────────────────────────

    suspend fun loginEmail(email: String, password: String): Result<FirebaseUser> =
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                Result.success(result.user!!)
            } catch (e: Exception) {
                Result.failure(e)
            }

    suspend fun registraEmail(
            email: String,
            password: String,
            username: String
    ): Result<FirebaseUser> =
            try {
                val cleanUsername = username.trim()
                if (isUsernameOccupato(cleanUsername)) {
                    throw Exception("Username già in uso")
                }
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user!!
                val profileUpdates = userProfileChangeRequest { displayName = cleanUsername }
                user.updateProfile(profileUpdates).await()
                creaProfiloSeNonEsiste(user.uid, cleanUsername, email)
                Result.success(user)
            } catch (e: Exception) {
                Result.failure(e)
            }

    suspend fun loginGoogle(idToken: String): Result<Pair<FirebaseUser, Boolean>> =
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                val user = result.user!!
                val isNuovo = result.additionalUserInfo?.isNewUser ?: false
                Result.success(Pair(user, isNuovo))
            } catch (e: Exception) {
                Result.failure(e)
            }

    fun logout() = auth.signOut()

    // ── Profilo ───────────────────────────────────────────────────────────────

    suspend fun isUsernameOccupato(username: String): Boolean {
        val target = username.trim().lowercase()
        if (target.isBlank()) return false
        val currentUid = uid

        return try {
            val doc = db.collection("usernames").document(target).get().await()
            if (doc.exists()) {
                val ownerUid = doc.getString("uid")
                ownerUid != null && ownerUid != currentUid
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Errore isUsernameOccupato: ${e.message}")
            false
        }
    }

    suspend fun creaProfiloSeNonEsiste(uid: String, username: String, email: String) {
        val cleanNick = username.trim()
        auth.currentUser?.let { user ->
            if (user.displayName != cleanNick) {
                val profileUpdates = userProfileChangeRequest { displayName = cleanNick }
                user.updateProfile(profileUpdates).await()
            }
        }

        // Salvataggio nella collection usernames per garantire univocità
        try {
            db.collection("usernames")
                    .document(cleanNick.lowercase())
                    .set(mapOf("uid" to uid, "original" to cleanNick))
                    .await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Errore salvataggio username: ${e.message}")
        }

        db.collection("users")
                .document(uid)
                .set(
                        mapOf(
                                "username" to cleanNick,
                                "email" to email,
                                "xp" to 0,
                                "streakCorrente" to 0,
                                "streakMassima" to 0,
                                "ultimoAccesso" to -1L,
                                "isAdmin" to false,
                                "ban" to false,
                                "commentiRimossi" to 0,
                                "badges" to emptyList<String>(),
                                "pillole_bookmark" to emptyList<String>(),
                                "pillole_lette" to emptyList<String>(),
                                "quiz_risposti" to emptyList<String>(),
                                "pillole_ignorate" to emptyList<String>(),
                                "pillole_note" to emptyMap<String, String>(),
                                "hardcore_record" to 0,
                                "hardcore_partite" to 0
                        ),
                        SetOptions.merge()
                )
                .await()
    }

    suspend fun caricaProfilo(uid: String): Map<String, Any>? =
            try {
                val doc = db.collection("users").document(uid).get().await()
                if (doc.exists()) doc.data else null
            } catch (e: Exception) {
                null
            }

    suspend fun aggiornaProfilo(uid: String, dati: Map<String, Any>) {
        try {
            db.collection("users").document(uid).set(dati, SetOptions.merge()).await()
        } catch (_: Exception) {}
    }

    suspend fun cambiaUsername(nuovoUsername: String): Result<Unit> =
            try {
                val user = auth.currentUser ?: throw Exception("Utente non loggato")
                val cleanNick = nuovoUsername.trim()
                val oldNick = user.displayName

                if (isUsernameOccupato(cleanNick)) {
                    throw Exception("Username già in uso")
                }

                val profileUpdates = userProfileChangeRequest { displayName = cleanNick }
                user.updateProfile(profileUpdates).await()

                // Rimuovi vecchio username e aggiungi il nuovo
                if (!oldNick.isNullOrBlank()) {
                    db.collection("usernames").document(oldNick.lowercase()).delete().await()
                }
                db.collection("usernames")
                        .document(cleanNick.lowercase())
                        .set(mapOf("uid" to user.uid, "original" to cleanNick))
                        .await()

                aggiornaProfilo(user.uid, mapOf("username" to cleanNick))

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

    suspend fun cambiaPassword(nuovaPassword: String): Result<Unit> =
            try {
                val user = auth.currentUser ?: throw Exception("Utente non loggato")
                user.updatePassword(nuovaPassword).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

    fun isGoogleUser(): Boolean {
        return auth.currentUser?.providerData?.any { it.providerId == "google.com" } ?: false
    }

    suspend fun recuperaPassword(email: String): Result<Unit> =
            try {
                auth.sendPasswordResetEmail(email).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

    suspend fun eliminaDatiUtente(uid: String) {
        try {
            val profilo = db.collection("users").document(uid).get().await()
            val username = profilo.getString("username")
            if (!username.isNullOrBlank()) {
                db.collection("usernames").document(username.lowercase()).delete().await()
                Log.d("FirebaseManager", "Username '$username' rimosso da usernames")
            }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Errore rimozione username: ${e.message}")
        }

        db.collection("users").document(uid).delete().await()
    }

    suspend fun eliminaAccount() {
        auth.currentUser?.delete()?.await()
    }

    suspend fun anonimizzaCommentiUtente(uid: String) {
        try {
            val snapshot = db.collectionGroup("lista").whereEqualTo("uid", uid).get().await()
            if (!snapshot.isEmpty) {
                val batch = db.batch()
                snapshot.forEach { doc ->
                    if (doc.reference.path.contains("commenti/")) {
                        batch.update(
                                doc.reference,
                                mapOf("autore" to "Utente eliminato", "uid" to "")
                        )
                    }
                }
                batch.commit().await()
            }
        } catch (_: Exception) {}
    }

    // ── Badge ─────────────────────────────────────────────────────────────────

    suspend fun caricaBadge(uid: String): List<String> =
            try {
                val doc = db.collection("users").document(uid).get().await()
                @Suppress("UNCHECKED_CAST") (doc.get("badges") as? List<String>) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

    suspend fun aggiungiBadge(uid: String, badgeId: String) {
        try {
            db.collection("users")
                    .document(uid)
                    .update("badges", FieldValue.arrayUnion(badgeId))
                    .await()
        } catch (_: Exception) {}
    }

    // ── Pillole lette ─────────────────────────────────────────────────────────

    suspend fun caricaPilloleLette(uid: String): List<String> =
            try {
                val doc = db.collection("users").document(uid).get().await()
                @Suppress("UNCHECKED_CAST")
                (doc.get("pillole_lette") as? List<String>) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

    suspend fun aggiungiPillolaLetta(uid: String, externalId: String) {
        try {
            db.collection("users")
                    .document(uid)
                    .update("pillole_lette", FieldValue.arrayUnion(externalId))
                    .await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Errore aggiungiPillolaLetta: ${e.message}")
        }
    }

    // ── Pillole bookmark ─────────────────────────────────────────────────────

    suspend fun caricaBookmark(uid: String): List<String> =
            try {
                val doc = db.collection("users").document(uid).get().await()
                @Suppress("UNCHECKED_CAST")
                (doc.get("pillole_bookmark") as? List<String>) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }

    suspend fun aggiungiBookmark(uid: String, externalId: String) {
        try {
            db.collection("users")
                    .document(uid)
                    .update("pillole_bookmark", FieldValue.arrayUnion(externalId))
                    .await()
        } catch (_: Exception) {}
    }

    suspend fun rimuoviBookmark(uid: String, externalId: String) {
        try {
            db.collection("users")
                    .document(uid)
                    .update("pillole_bookmark", FieldValue.arrayRemove(externalId))
                    .await()
        } catch (_: Exception) {}
    }

    // ── Pillole ignorate ──────────────────────────────────────────────────────

    suspend fun caricaIgnorate(uid: String): List<String> =
            try {
                val doc = db.collection("users").document(uid).get().await()
                @Suppress("UNCHECKED_CAST")
                (doc.get("pillole_ignorate") as? List<String>) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }

    suspend fun aggiungiIgnorata(uid: String, externalId: String) {
        try {
            db.collection("users")
                    .document(uid)
                    .update("pillole_ignorate", FieldValue.arrayUnion(externalId))
                    .await()
        } catch (_: Exception) {}
    }

    suspend fun rimuoviIgnorata(uid: String, externalId: String) {
        try {
            db.collection("users")
                    .document(uid)
                    .update("pillole_ignorate", FieldValue.arrayRemove(externalId))
                    .await()
        } catch (_: Exception) {}
    }

    // ── Note pillole ──────────────────────────────────────────────────────────

    suspend fun caricaNote(uid: String): Map<String, String> =
            try {
                val doc = db.collection("users").document(uid).get().await()
                @Suppress("UNCHECKED_CAST")
                (doc.get("pillole_note") as? Map<String, String>) ?: emptyMap()
            } catch (_: Exception) {
                emptyMap()
            }

    suspend fun salvaNota(uid: String, externalId: String, nota: String) {
        try {
            val ref = db.collection("users").document(uid)
            if (nota.isBlank()) {
                ref.update("pillole_note.$externalId", FieldValue.delete()).await()
            } else {
                ref.update("pillole_note.$externalId", nota).await()
            }
        } catch (_: Exception) {}
    }

    // ── Rimuovi pillola letta (es. reset) ─────────────────────────────────────

    suspend fun rimuoviPillolaLetta(uid: String, externalId: String) {
        try {
            db.collection("users")
                    .document(uid)
                    .update("pillole_lette", FieldValue.arrayRemove(externalId))
                    .await()
        } catch (_: Exception) {}
    }

    // ── Quiz risposti ─────────────────────────────────────────────────────────

    suspend fun caricaQuizRisposti(uid: String): List<String> =
            try {
                val doc = db.collection("users").document(uid).get().await()
                @Suppress("UNCHECKED_CAST")
                (doc.get("quiz_risposti") as? List<String>) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }

    suspend fun aggiungiQuizRisposto(uid: String, questionId: Int) {
        try {
            db.collection("users")
                    .document(uid)
                    .update("quiz_risposti", FieldValue.arrayUnion(questionId.toString()))
                    .await()
        } catch (_: Exception) {}
    }

    // ── Sopravvivenza ─────────────────────────────────────────────────────────

    suspend fun salvaStatisticheHardcore(record: Int, totalePartite: Int) {
        val u = uid ?: return
        try {
            db.collection("users")
                    .document(u)
                    .update(mapOf("hardcore_record" to record, "hardcore_partite" to totalePartite))
                    .await()
        } catch (_: Exception) {}
    }

    // ── Reset progressi utente ───────────────────────────────────────────────

    suspend fun resetProgressiUtente(uid: String) {
        try {
            db.collection("users")
                    .document(uid)
                    .update(
                            mapOf(
                                    "pillole_lette" to emptyList<String>(),
                                    "pillole_bookmark" to emptyList<String>(),
                                    "pillole_ignorate" to emptyList<String>(),
                                    "pillole_note" to emptyMap<String, String>(),
                                    "quiz_risposti" to emptyList<String>(),
                                    "xp" to 0,
                                    "streakCorrente" to 0,
                                    "streakMassima" to 0,
                                    "badges" to emptyList<String>(),
                                    "hardcore_record" to 0,
                                    "hardcore_partite" to 0
                            )
                    )
                    .await()

            // Elimina anche le scoperte
            val scoperte = db.collection("users").document(uid).collection("scoperte").get().await()
            if (!scoperte.isEmpty) {
                val batch = db.batch()
                for (doc in scoperte.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Errore reset progressi: ${e.message}")
        }
    }

    // ── Commenti ──────────────────────────────────────────────────────────────

    data class Commento(
            val id: String = "",
            val testo: String = "",
            val autore: String = "Anonimo",
            val uid: String = "",
            val timestamp: Long = 0L
    )

    private val PAROLE_VIETATE =
            setOf(
                    "gdg",
                    "cazzo",
                    "vaffanculo",
                    "stronzo",
                    "stronza",
                    "puttana",
                    "coglione",
                    "cogliona",
                    "merda",
                    "fanculo",
                    "minchia",
                    "figlio di puttana",
                    "bastardo",
                    "bastarda",
                    "idiota",
                    "imbecille",
                    "deficiente",
                    "ritardato",
                    "ritardata",
                    "frocio",
                    "ricchione",
                    "negro",
                    "negra",
                    "troia",
                    "mignotta",
                    "zoccola",
                    "baldracca",
                    "finocchio",
                    "testa di cazzo",
                    "pezzo di merda",
                    "cretino",
                    "cretina",
                    "schifoso",
                    "schifosa",
                    "scemo",
                    "scema",
                    "handicappato",
                    "porco dio",
                    "dio cane",
                    "dio porco",
                    "dio maiale",
                    "bucaiolo",
                    "bagascia",
                    "battona",
                    "bottana",
                    "cessa",
                    "cesso",
                    "culattone",
                    "culatone",
                    "fancazzista",
                    "fica",
                    "figa",
                    "fregna",
                    "gnocca",
                    "infame",
                    "lota",
                    "mignottone",
                    "mona",
                    "picio",
                    "pirla",
                    "pompinara",
                    "porca",
                    "puttaniere",
                    "rottinculo",
                    "sculattone",
                    "sfaccimm",
                    "sfigato",
                    "sfigata",
                    "terrone",
                    "terrona",
                    "zoccolone",
                    "negri",
                    "negre"
            )

    /**
     * Normalizza il testo per facilitare il controllo delle parole vietate. Rimuove spazi tra le
     * lettere e gestisce il leetspeak.
     */
    private fun normalizzaTesto(testo: String): String {
        val base =
                testo.lowercase()
                        .replace('4', 'a')
                        .replace('@', 'a')
                        .replace('3', 'e')
                        .replace('1', 'i')
                        .replace('!', 'i')
                        .replace('0', 'o')
                        .replace('5', 's')
                        .replace('$', 's')
                        .replace('7', 't')
                        .replace('+', 't')

        // Rimuove TUTTI gli spazi e la punteggiatura
        return base.filter { it.isLetterOrDigit() }
    }

    fun contienePaloroleVietate(testo: String): Boolean {
        val normalizzato = normalizzaTesto(testo)
        return PAROLE_VIETATE.any { normalizzato.contains(it) }
    }

    suspend fun caricaCommenti(externalId: String): List<Commento> =
            try {
                val docs =
                        db.collection("commenti")
                                .document(externalId)
                                .collection("lista")
                                .orderBy("timestamp")
                                .get()
                                .await()
                docs.map { doc ->
                    Commento(
                            id = doc.id,
                            testo = doc.getString("testo") ?: "",
                            autore = doc.getString("autore") ?: "Anonimo",
                            uid = doc.getString("uid") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }

    /**
     * Carica TUTTI i commenti caricando tutti i documenti 'lista' ed estraendo solo quelli dei
     * commenti
     */
    suspend fun caricaTuttiICommentiModerazione(): List<Pair<String, List<Commento>>> =
            try {
                val snapshot = db.collectionGroup("lista").get().await()
                snapshot.documents
                        .filter { it.reference.path.contains("commenti/") }
                        .groupBy { it.reference.parent.parent!!.id }
                        .map { (exId, docs) ->
                            exId to
                                    docs
                                            .map { doc ->
                                                Commento(
                                                        id = doc.id,
                                                        testo = doc.getString("testo") ?: "",
                                                        autore = doc.getString("autore")
                                                                        ?: "Anonimo",
                                                        uid = doc.getString("uid") ?: "",
                                                        timestamp = doc.getLong("timestamp") ?: 0L
                                                )
                                            }
                                            .sortedByDescending { it.timestamp }
                        }
            } catch (e: Exception) {
                Log.e("FirebaseManager", "Errore caricaTuttiICommentiModerazione", e)
                emptyList()
            }

    suspend fun aggiungiCommento(externalId: String, testo: String): Result<Unit> =
            try {
                val autore = auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Anonimo"
                val uid = auth.currentUser?.uid ?: ""

                // Assicuriamoci che il documento padre esista esplicitamente
                db.collection("commenti")
                        .document(externalId)
                        .set(mapOf("lastUpdate" to System.currentTimeMillis()), SetOptions.merge())
                        .await()

                db.collection("commenti")
                        .document(externalId)
                        .collection("lista")
                        .add(
                                mapOf(
                                        "testo" to testo,
                                        "autore" to autore,
                                        "uid" to uid,
                                        "timestamp" to System.currentTimeMillis()
                                )
                        )
                        .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

    suspend fun eliminaCommento(
            externalId: String,
            commentoId: String,
            autoreUid: String? = null
    ): Result<Unit> =
            try {
                db.collection("commenti")
                        .document(externalId)
                        .collection("lista")
                        .document(commentoId)
                        .delete()
                        .await()

                // Se è specificato l'autore, incrementiamo il contatore e banniamo a 3
                if (!autoreUid.isNullOrBlank()) {
                    val userRef = db.collection("users").document(autoreUid)
                    db
                            .runTransaction { transaction ->
                                val snap = transaction.get(userRef)
                                if (snap.exists()) {
                                    val rimossi = (snap.getLong("commentiRimossi") ?: 0L) + 1
                                    transaction.update(userRef, "commentiRimossi", rimossi)
                                    if (rimossi >= 3) {
                                        transaction.update(userRef, "ban", true)
                                        transaction.update(
                                                userRef,
                                                "banMotivazione",
                                                "Troppi commenti rimossi dai moderatori (limite: 3)"
                                        )
                                    }
                                }
                            }
                            .await()
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

    // ── Segnalazioni ──────────────────────────────────────────────────────────

    suspend fun inviaSegnalazione(externalId: String, tipo: String, testo: String): Result<Unit> =
            try {
                val currentUid = uid ?: "anonimo"
                val parentRef = db.collection("segnalazioni").document(externalId)
                parentRef
                        .collection("lista")
                        .add(
                                mapOf(
                                        "tipo" to tipo,
                                        "testo" to testo,
                                        "uid" to currentUid,
                                        "creatoAt" to System.currentTimeMillis(),
                                        "letta" to false
                                )
                        )
                        .await()
                parentRef
                        .set(mapOf("totale" to FieldValue.increment(1)), SetOptions.merge())
                        .await()
                notificaAdminNuovaSegnalazione(externalId, tipo)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

    private suspend fun notificaAdminNuovaSegnalazione(externalId: String, tipo: String) {
        try {
            val curiositaDoc = db.collection("curiosita").document(externalId).get().await()
            val titoloCuriosita = curiositaDoc.getString("titolo") ?: externalId
            val adminSnap = db.collection("users").whereEqualTo("isAdmin", true).get().await()

            if (adminSnap.isEmpty) return

            val ora = System.currentTimeMillis()
            val batch = db.batch()

            adminSnap.documents.forEach { adminDoc ->
                val notificaRef =
                        db.collection("notifiche")
                                .document(adminDoc.id)
                                .collection("lista")
                                .document()
                batch.set(
                        notificaRef,
                        mapOf(
                                "titolo" to "Nuova segnalazione 🚩",
                                "corpo" to
                                        "È stata segnalata la curiosità «$titoloCuriosita» per: $tipo",
                                "letta" to false,
                                "creatoAt" to ora,
                                "externalId" to externalId,
                                "tipo" to "segnalazione_admin"
                        )
                )
            }
            batch.commit().await()
        } catch (_: Exception) {}
    }

    suspend fun caricaTutteSegnalazioni(): List<SegnalazioneConTitolo> =
            try {
                val docs = db.collection("segnalazioni").get().await()
                docs
                        .map { doc ->
                            SegnalazioneConTitolo(
                                    externalId = doc.id,
                                    totale = doc.getLong("totale") ?: 0L
                            )
                        }
                        .sortedByDescending { it.totale }
            } catch (_: Exception) {
                emptyList()
            }

    suspend fun caricaSegnalazioniPerCuriosita(externalId: String): List<SegnalazioneItem> =
            try {
                db.collection("segnalazioni")
                        .document(externalId)
                        .collection("lista")
                        .orderBy(
                                "creatoAt",
                                com.google.firebase.firestore.Query.Direction.DESCENDING
                        )
                        .get()
                        .await()
                        .documents
                        .mapNotNull { doc ->
                            SegnalazioneItem(
                                    id = doc.id,
                                    tipo = doc.getString("tipo") ?: "",
                                    testo = doc.getString("testo") ?: "",
                                    uid = doc.getString("uid") ?: "",
                                    creatoAt = doc.getLong("creatoAt") ?: 0L,
                                    letta = doc.getBoolean("letta") ?: false
                            )
                        }
            } catch (_: Exception) {
                emptyList()
            }

    suspend fun segnaSegnalazioneLetta(externalId: String, segnalazioneId: String) {
        try {
            db.collection("segnalazioni")
                    .document(externalId)
                    .collection("lista")
                    .document(segnalazioneId)
                    .update("letta", true)
                    .await()
        } catch (_: Exception) {}
    }

    data class SegnalazioneConTitolo(val externalId: String, val totale: Long)
    data class SegnalazioneItem(
            val id: String,
            val tipo: String,
            val testo: String,
            val uid: String,
            val creatoAt: Long,
            val letta: Boolean
    )

    // ── Admin ─────────────────────────────────────────────────────────────────

    suspend fun isAdmin(): Boolean {
        val currentUid = uid ?: return false
        return try {
            val doc = db.collection("users").document(currentUid).get().await()
            doc.getBoolean("isAdmin") == true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun isUtenteBannato(uid: String): Boolean =
            try {
                val doc = db.collection("users").document(uid).get().await()
                doc.getBoolean("ban") == true
            } catch (_: Exception) {
                false
            }

    // ── Admin: gestione curiosità ─────────────────────────────────────────────

    data class CuriositaRemota(
            val externalId: String,
            val titolo: String,
            val corpo: String,
            val categoria: String,
            val emoji: String,
            val domanda: String? = null,
            val rispostaCorretta: String? = null,
            val risposteErrate: List<String>? = null,
            val spiegazione: String? = null
    )

    suspend fun salvaCuriosita(c: CuriositaRemota): Result<Unit> =
            try {
                val docRef = db.collection("curiosita").document(c.externalId)
                val esistente = docRef.get().await().exists()

                val data =
                        mutableMapOf<String, Any?>(
                                "titolo" to c.titolo,
                                "corpo" to c.corpo,
                                "categoria" to c.categoria,
                                "emoji" to c.emoji,
                                "domanda" to c.domanda,
                                "risposta_corretta" to c.rispostaCorretta,
                                "risposte_errate" to c.risposteErrate,
                                "spiegazione" to c.spiegazione
                        )

                docRef.set(data, SetOptions.merge()).await()

                if (esistente) {
                    notificaAggiornamento(c.externalId, c.titolo)
                }

                incrementaVersione()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

    private suspend fun notificaAggiornamento(externalId: String, titoloCuriosita: String) {
        try {
            val segnalazioniSnap =
                    db.collection("segnalazioni")
                            .document(externalId)
                            .collection("lista")
                            .whereEqualTo("letta", false)
                            .get()
                            .await()

            if (segnalazioniSnap.isEmpty) return
            val uidSegnalanti =
                    segnalazioniSnap
                            .documents
                            .mapNotNull { it.getString("uid") }
                            .filter { it.isNotBlank() && it != "anonimo" }
                            .distinct()
            val ora = System.currentTimeMillis()
            val batch = db.batch()
            segnalazioniSnap.documents.forEach { batch.update(it.reference, "letta", true) }

            uidSegnalanti.forEach { uidUtente ->
                val notificaRef =
                        db.collection("notifiche")
                                .document(uidUtente)
                                .collection("lista")
                                .document()
                batch.set(
                        notificaRef,
                        mapOf(
                                "titolo" to "Curiosità aggiornata ✏️",
                                "corpo" to
                                        "«$titoloCuriosita» è stata revisionata dopo la tua segnalazione. Grazie!",
                                "letta" to false,
                                "creatoAt" to ora,
                                "externalId" to externalId,
                                "tipo" to "pillola"
                        )
                )
            }
            batch.commit().await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "notificaAggiornamento failed", e)
        }
    }

    // ── Notifiche in-app ──────────────────────────────────────────────────────

    data class NotificaInApp(
            val id: String,
            val titolo: String,
            val corpo: String,
            val creatoAt: Long,
            val externalId: String?,
            val tipo: String = "pillola"
    )

    suspend fun caricaNotifichePendenti(): List<NotificaInApp> {
        val currentUid = uid ?: return emptyList()
        return try {
            db.collection("notifiche")
                    .document(currentUid)
                    .collection("lista")
                    .whereEqualTo("letta", false)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { doc ->
                        NotificaInApp(
                                id = doc.id,
                                titolo = doc.getString("titolo") ?: return@mapNotNull null,
                                corpo = doc.getString("corpo") ?: "",
                                creatoAt = doc.getLong("creatoAt") ?: 0L,
                                externalId = doc.getString("externalId"),
                                tipo = doc.getString("tipo") ?: "pillola"
                        )
                    }
                    .sortedByDescending { it.creatoAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun segnaNotificaLetta(notificaId: String) {
        val currentUid = uid ?: return
        try {
            db.collection("notifiche")
                    .document(currentUid)
                    .collection("lista")
                    .document(notificaId)
                    .update("letta", true)
                    .await()
        } catch (_: Exception) {}
    }

    suspend fun segnaNotificheTutteLette(notifiche: List<NotificaInApp>) {
        val currentUid = uid ?: return
        if (notifiche.isEmpty()) return
        try {
            val batch = db.batch()
            notifiche.forEach { n ->
                val ref =
                        db.collection("notifiche")
                                .document(currentUid)
                                .collection("lista")
                                .document(n.id)
                batch.update(ref, "letta", true)
            }
            batch.commit().await()
        } catch (_: Exception) {}
    }

    suspend fun importaBulk(lista: List<CuriositaRemota>): Result<Int> =
            try {
                lista.chunked(200).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { c ->
                        val docRef = db.collection("curiosita").document(c.externalId)
                        batch.set(
                                docRef,
                                mapOf(
                                        "titolo" to c.titolo,
                                        "corpo" to c.corpo,
                                        "categoria" to c.categoria,
                                        "emoji" to c.emoji,
                                        "domanda" to c.domanda,
                                        "risposta_corretta" to c.rispostaCorretta,
                                        "risposte_errate" to c.risposteErrate,
                                        "spiegazione" to c.spiegazione
                                )
                        )
                    }
                    batch.commit().await()
                }
                incrementaVersione()
                Result.success(lista.size)
            } catch (e: Exception) {
                Result.failure(e)
            }

    suspend fun eliminaCuriosita(externalId: String): Result<Unit> =
            try {
                db.collection("curiosita").document(externalId).delete().await()
                incrementaVersione()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

    /** Carica TUTTI le curiosita dal server - FORMATO PIATTO */
    suspend fun caricaTutteLeCuriositaRemote(): List<CuriositaRemota> =
            try {
                val snap =
                        db.collection("curiosita")
                                .whereNotEqualTo("__name__", "_meta_")
                                .get()
                                .await()
                val docs = snap.documents.filter { it.id != "_meta_" }

                docs.map { doc ->
                    // Supporta sia formato flat (nuovo) che annidato (vecchio)
                    val quizObj = doc.get("quiz") as? Map<String, Any>

                    val domanda = doc.getString("domanda") ?: quizObj?.get("domanda") as? String

                    val rispostaCorretta =
                            doc.getString("risposta_corretta")
                                    ?: doc.getString("rispostaCorretta")
                                            ?: quizObj?.get("risposta_corretta") as? String
                                            ?: quizObj?.get("rispostaCorretta") as? String

                    @Suppress("UNCHECKED_CAST")
                    val risposteErrate =
                            (doc.get("risposte_errate")
                                    ?: doc.get("risposteErrate") ?: quizObj?.get("risposte_errate")
                                            ?: quizObj?.get("risposteErrate")) as?
                                    List<String>

                    val spiegazione =
                            doc.getString("spiegazione") ?: quizObj?.get("spiegazione") as? String

                    CuriositaRemota(
                            externalId = doc.id,
                            titolo = doc.getString("titolo") ?: "",
                            corpo = doc.getString("corpo") ?: "",
                            categoria = doc.getString("categoria") ?: "",
                            emoji = doc.getString("emoji") ?: "💡",
                            domanda = domanda?.ifBlank { null },
                            rispostaCorretta = rispostaCorretta?.ifBlank { null },
                            risposteErrate = risposteErrate,
                            spiegazione = spiegazione?.ifBlank { null }
                    )
                }
            } catch (e: Exception) {
                Log.e("FirebaseManager", "Errore caricaTutteLeCuriositaRemote: ${e.message}")
                emptyList()
            }

    private suspend fun incrementaVersione() {
        try {
            val metaRef = db.collection("curiosita").document("_meta_")
            db
                    .runTransaction { tx ->
                        val snap = tx.get(metaRef)
                        val current = snap.getLong("versione") ?: 0L
                        tx.set(metaRef, mapOf("versione" to current + 1))
                    }
                    .await()
        } catch (_: Exception) {}
    }

    suspend fun inviaBroadcast(titolo: String, corpo: String): Result<Unit> =
            try {
                val ora = System.currentTimeMillis()
                val broadcastId = "b$ora"
                val utenti = db.collection("users").get().await().documents.map { it.id }
                if (utenti.isEmpty()) throw Exception("Nessun utente")
                utenti.chunked(400).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { uidUtente ->
                        val ref =
                                db.collection("notifiche")
                                        .document(uidUtente)
                                        .collection("lista")
                                        .document(broadcastId)
                        batch.set(
                                ref,
                                mapOf(
                                        "titolo" to titolo,
                                        "corpo" to corpo,
                                        "letta" to false,
                                        "creatoAt" to ora,
                                        "externalId" to null,
                                        "tipo" to "broadcast"
                                )
                        )
                    }
                    batch.commit().await()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }

    /** SCRIPT DI MIGRAZIONE: sposta i quiz dalle subcollection al documento principale */
    suspend fun eseguiMigrazioneQuizPiatta() {
        try {
            val snap = db.collection("curiosita").get().await()
            val docs = snap.documents.filter { it.id != "_meta_" }

            coroutineScope {
                docs.forEach { doc ->
                    async {
                        // Se non ha ancora il campo "domanda", proviamo a prenderlo dalla
                        // subcollection
                        if (doc.getString("domanda") == null) {
                            val quizDoc =
                                    db.collection("curiosita")
                                            .document(doc.id)
                                            .collection("quiz")
                                            .document("domanda")
                                            .get()
                                            .await()

                            if (quizDoc.exists()) {
                                db.collection("curiosita")
                                        .document(doc.id)
                                        .update(
                                                mapOf(
                                                        "domanda" to quizDoc.getString("domanda"),
                                                        "risposta_corretta" to
                                                                quizDoc.getString(
                                                                        "rispostaCorretta"
                                                                ),
                                                        "risposte_errate" to
                                                                quizDoc.get("risposteErrate"),
                                                        "spiegazione" to
                                                                quizDoc.getString("spiegazione")
                                                )
                                        )
                                        .await()
                                Log.d("Migration", "Migrato quiz per ${doc.id}")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Migration", "Errore migrazione: ${e.message}")
        }
    }

    // ── Scoperte AR ───────────────────────────────────────────────────────────

    suspend fun salvaScoperta(uid: String, scoperta: Scoperta): String {
        val ref = db.collection("users").document(uid).collection("scoperte").document()
        val data =
                mapOf(
                        "titolo" to scoperta.titolo,
                        "descrizione" to scoperta.descrizione,
                        "categoria" to scoperta.categoria,
                        "dataScoperta" to scoperta.dataScoperta
                )
        ref.set(data).await()
        return ref.id
    }

    suspend fun getScoperte(uid: String): List<Scoperta> {
        return try {
            val snp =
                    db.collection("users")
                            .document(uid)
                            .collection("scoperte")
                            .orderBy(
                                    "dataScoperta",
                                    com.google.firebase.firestore.Query.Direction.DESCENDING
                            )
                            .get()
                            .await()
            snp.documents.map { doc ->
                Scoperta(
                        firestoreId = doc.id,
                        titolo = doc.getString("titolo") ?: "",
                        descrizione = doc.getString("descrizione") ?: "",
                        categoria = doc.getString("categoria") ?: "",
                        dataScoperta = doc.getLong("dataScoperta") ?: 0L
                )
            }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Errore getScoperte", e)
            emptyList()
        }
    }

    suspend fun getCuriositaPerSync(limit: Int = 100): List<CuriositaRemota> {
        val snp = db.collection("curiosita").limit(limit.toLong()).get().await()
        return snp.documents.filter { it.id != "_meta_" }.map { doc ->
            CuriositaRemota(
                    externalId = doc.id,
                    titolo = doc.getString("titolo") ?: "",
                    corpo = doc.getString("corpo") ?: "",
                    categoria = doc.getString("categoria") ?: "Generale",
                    emoji = doc.getString("emoji") ?: "💡"
            )
        }
    }
}
