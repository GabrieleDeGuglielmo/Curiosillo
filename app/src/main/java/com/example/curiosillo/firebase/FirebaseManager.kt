package com.example.curiosillo.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

object FirebaseManager {

    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val db:   FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    val utenteCorrente: FirebaseUser? get() = auth.currentUser
    val uid:            String?       get() = auth.currentUser?.uid
    val isLoggato:      Boolean       get() = auth.currentUser != null

    // ── Auth ──────────────────────────────────────────────────────────────────

    suspend fun loginEmail(email: String, password: String): Result<FirebaseUser> = try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        Result.success(result.user!!)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun registraEmail(email: String, password: String, username: String): Result<FirebaseUser> = try {
        val cleanUsername = username.trim()
        // Verifica se lo username è già occupato
        if (isUsernameOccupato(cleanUsername)) {
            throw Exception("Username già in uso")
        }
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user   = result.user!!
        // Imposta lo username su Auth Profile
        val profileUpdates = userProfileChangeRequest {
            displayName = cleanUsername
        }
        user.updateProfile(profileUpdates).await()
        // Crea il documento profilo su Firestore
        creaProfiloSeNonEsiste(user.uid, cleanUsername, email)
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun loginGoogle(idToken: String): Result<Pair<FirebaseUser, Boolean>> = try {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result     = auth.signInWithCredential(credential).await()
        val user       = result.user!!
        val isNuovo    = result.additionalUserInfo?.isNewUser ?: false
        // Non creiamo il profilo qui se è nuovo, lo faremo dopo la conferma dello username
        Result.success(Pair(user, isNuovo))
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun logout() = auth.signOut()

    // ── Profilo ───────────────────────────────────────────────────────────────

    /** 
     * Verifica se uno username esiste già.
     * Cerca direttamente in users/{uid}/data/profile dove username è un campo.
     */
    suspend fun isUsernameOccupato(username: String): Boolean {
        val target = username.trim()
        if (target.isBlank()) return false
        val currentUid = uid

        return try {
            val snapshot = db.collectionGroup("data")
                .whereEqualTo("username", target)
                .limit(2)
                .get().await()

            val altri = snapshot.documents.filter { doc ->
                // users/{uid}/data/profile -> parent.parent is users/{uid}
                doc.reference.parent.parent?.id != currentUid
            }
            altri.isNotEmpty()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Errore isUsernameOccupato: ${e.message}")
            false
        }
    }

    suspend fun creaProfiloSeNonEsiste(uid: String, username: String, email: String) {
        val cleanNick = username.trim()

        // Imposta anche il displayName su Firebase Auth se non già presente o diverso
        auth.currentUser?.let { user ->
            if (user.displayName != cleanNick) {
                val profileUpdates = userProfileChangeRequest {
                    displayName = cleanNick
                }
                user.updateProfile(profileUpdates).await()
            }
        }

        // Crea il profilo utente su Firestore
        db.collection("users").document(uid)
            .collection("data").document("profile")
            .set(mapOf(
                "username"       to cleanNick,
                "email"          to email,
                "xp"             to 0,
                "streakCorrente" to 0,
                "streakMassima"  to 0,
                "ultimoAccesso"  to -1L
            ), SetOptions.merge()).await()
    }

    suspend fun caricaProfilo(uid: String): Map<String, Any>? = try {
        val doc = db.collection("users").document(uid)
            .collection("data").document("profile").get().await()
        if (doc.exists()) doc.data else null
    } catch (e: Exception) { null }

    suspend fun aggiornaProfilo(uid: String, dati: Map<String, Any>) {
        try {
            db.collection("users").document(uid)
                .collection("data").document("profile")
                .set(dati, SetOptions.merge()).await()
        } catch (_: Exception) {}
    }

    suspend fun cambiaUsername(nuovoUsername: String): Result<Unit> = try {
        val user = auth.currentUser ?: throw Exception("Utente non loggato")
        val cleanNick = nuovoUsername.trim()

        if (isUsernameOccupato(cleanNick)) {
            throw Exception("Username già in uso")
        }

        val profileUpdates = userProfileChangeRequest {
            displayName = cleanNick
        }
        user.updateProfile(profileUpdates).await()
        aggiornaProfilo(user.uid, mapOf("username" to cleanNick))

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun cambiaPassword(nuovaPassword: String): Result<Unit> = try {
        val user = auth.currentUser ?: throw Exception("Utente non loggato")
        user.updatePassword(nuovaPassword).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun isGoogleUser(): Boolean {
        return auth.currentUser?.providerData?.any { it.providerId == "google.com" } ?: false
    }

    suspend fun recuperaPassword(email: String): Result<Unit> = try {
        auth.sendPasswordResetEmail(email).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Elimina tutti i documenti Firestore dell'utente
    suspend fun eliminaDatiUtente(uid: String) {
        val ref = db.collection("users").document(uid).collection("data")
        val docs = ref.get().await()
        docs.forEach { ref.document(it.id).delete().await() }
        db.collection("users").document(uid).delete().await()
    }

    // Cancella l'account Firebase Auth
    suspend fun eliminaAccount() {
        auth.currentUser?.delete()?.await()
    }

    /**
     * Cerca tutti i commenti dell'utente su Firestore e li anonimizza:
     * rimuove uid e sostituisce autore con "Utente eliminato".
     * Usa una collection group query su "lista".
     */
    suspend fun anonimizzaCommentiUtente(uid: String) {
        try {
            val snapshot = db.collectionGroup("lista")
                .whereEqualTo("uid", uid)
                .get().await()
            snapshot.forEach { doc ->
                doc.reference.update(mapOf(
                    "autore" to "Utente eliminato",
                    "uid"    to ""
                )).await()
            }
        } catch (_: Exception) {}
    }

    // ── Badge ─────────────────────────────────────────────────────────────────

    suspend fun caricaBadge(uid: String): List<String> = try {
        val doc = db.collection("users").document(uid)
            .collection("data").document("badges").get().await()
        @Suppress("UNCHECKED_CAST")
        (doc.get("ids") as? List<String>) ?: emptyList()
    } catch (e: Exception) { emptyList() }

    suspend fun aggiungiBadge(uid: String, badgeId: String) {
        try {
            val ref = db.collection("users").document(uid)
                .collection("data").document("badges")
            val doc = ref.get().await()
            @Suppress("UNCHECKED_CAST")
            val ids = ((doc.get("ids") as? List<String>) ?: emptyList()).toMutableList()
            if (badgeId !in ids) {
                ids.add(badgeId)
                ref.set(mapOf("ids" to ids)).await()
            }
        } catch (_: Exception) {}
    }

    // ── Pillole lette ─────────────────────────────────────────────────────────

    suspend fun caricaPilloleLette(uid: String): List<String> = try {
        val doc = db.collection("users").document(uid)
            .collection("data").document("pillole_lette").get().await()
        @Suppress("UNCHECKED_CAST")
        (doc.get("ids") as? List<String>) ?: emptyList()
    } catch (e: Exception) { emptyList() }

    suspend fun aggiungiPillolaLetta(uid: String, externalId: String) {
        try {
            val ref = db.collection("users").document(uid)
                .collection("data").document("pillole_lette")
            val doc = ref.get().await()
            @Suppress("UNCHECKED_CAST")
            val ids = ((doc.get("ids") as? List<String>) ?: emptyList()).toMutableList()
            if (externalId !in ids) {
                ids.add(externalId)
                ref.set(mapOf("ids" to ids)).await()
            }
        } catch (_: Exception) {}
    }

    // ── Commenti ──────────────────────────────────────────────────────────────

    data class Commento(
        val id:        String = "",
        val testo:     String = "",
        val autore:    String = "Anonimo",
        val uid:       String = "",
        val timestamp: Long   = 0L
    )

    private val PAROLE_VIETATE = setOf(
        "gdg", "cazzo", "vaffanculo", "stronzo", "stronza", "puttana", "coglione",
        "cogliona", "merda", "fanculo", "minchia", "figlio di puttana", "bastardo",
        "bastarda", "idiota", "imbecille", "deficiente", "ritardato", "ritardata",
        "frocio", "ricchione", "negro"
    )

    fun contienePaloroleVietate(testo: String): Boolean {
        val lower = testo.lowercase()
        return PAROLE_VIETATE.any { lower.contains(it) }
    }

    suspend fun caricaCommenti(externalId: String): List<Commento> = try {
        val docs = db.collection("commenti")
            .document(externalId)
            .collection("lista")
            .orderBy("timestamp")
            .get().await()
        docs.map { doc ->
            Commento(
                id        = doc.id,
                testo     = doc.getString("testo") ?: "",
                autore    = doc.getString("autore") ?: "Anonimo",
                uid       = doc.getString("uid") ?: "",
                timestamp = doc.getLong("timestamp") ?: 0L
            )
        }
    } catch (e: Exception) { emptyList() }

    suspend fun aggiungiCommento(externalId: String, testo: String): Result<Unit> = try {
        val autore = auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Anonimo"
        val uid    = auth.currentUser?.uid ?: ""
        db.collection("commenti")
            .document(externalId)
            .collection("lista")
            .add(mapOf(
                "testo"     to testo,
                "autore"    to autore,
                "uid"       to uid,
                "timestamp" to System.currentTimeMillis()
            )).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun eliminaCommento(externalId: String, commentoId: String): Result<Unit> = try {
        db.collection("commenti")
            .document(externalId)
            .collection("lista")
            .document(commentoId)
            .delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── Voti (like/dislike) ───────────────────────────────────────────────────

    /**
     * Invia una segnalazione per una curiosità.
     * Path: segnalazioni/{externalId}/lista/{id}
     *   → campi: tipo, testo, uid, creatoAt
     */
    suspend fun inviaSegnalazione(externalId: String, tipo: String, testo: String): Result<Unit> = try {
        val currentUid = uid ?: "anonimo"
        val parentRef = db.collection("segnalazioni").document(externalId)
        // Aggiunge la segnalazione nella subcollection
        parentRef.collection("lista").add(mapOf(
            "tipo"     to tipo,
            "testo"    to testo,
            "uid"      to currentUid,
            "creatoAt" to System.currentTimeMillis(),
            "letta"    to false
        )).await()
        // Crea/aggiorna il documento padre con il contatore totale
        parentRef.set(
            mapOf("totale" to FieldValue.increment(1)),
            SetOptions.merge()
        ).await()

        // Notifica gli admin della nuova segnalazione
        notificaAdminNuovaSegnalazione(externalId, tipo)

        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    private suspend fun notificaAdminNuovaSegnalazione(externalId: String, tipo: String) {
        try {
            // Recupera il titolo della curiosità per la notifica
            val curiositaDoc = db.collection("curiosita").document(externalId).get().await()
            val titoloCuriosita = curiositaDoc.getString("titolo") ?: externalId

            // Trova tutti gli admin
            val adminSnap = db.collection("users")
                .whereEqualTo("isAdmin", true)
                .get().await()

            if (adminSnap.isEmpty) return

            val ora = System.currentTimeMillis()
            val batch = db.batch()

            adminSnap.documents.forEach { adminDoc ->
                val notificaRef = db.collection("notifiche")
                    .document(adminDoc.id)
                    .collection("lista")
                    .document()
                batch.set(notificaRef, mapOf(
                    "titolo"     to "Nuova segnalazione 🚩",
                    "corpo"      to "È stata segnalata la curiosità «$titoloCuriosita» per: $tipo",
                    "letta"      to false,
                    "creatoAt"   to ora,
                    "externalId" to externalId,
                    "tipo"       to "segnalazione_admin" // Tipo specifico per distinguere
                ))
            }
            batch.commit().await()
        } catch (_: Exception) {}
    }

    /** Carica tutte le segnalazioni — usato dalla schermata admin */
    suspend fun caricaTutteSegnalazioni(): List<SegnalazioneConTitolo> = try {
        val docs = db.collection("segnalazioni").get().await()
        docs.map { doc ->
            SegnalazioneConTitolo(
                externalId = doc.id,
                totale     = doc.getLong("totale") ?: 0L
            )
        }.sortedByDescending { it.totale }
    } catch (_: Exception) { emptyList() }

    suspend fun caricaSegnalazioniPerCuriosita(externalId: String): List<SegnalazioneItem> = try {
        db.collection("segnalazioni")
            .document(externalId)
            .collection("lista")
            .orderBy("creatoAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get().await()
            .documents.mapNotNull { doc ->
                SegnalazioneItem(
                    id       = doc.id,
                    tipo     = doc.getString("tipo")  ?: "",
                    testo    = doc.getString("testo") ?: "",
                    uid      = doc.getString("uid")   ?: "",
                    creatoAt = doc.getLong("creatoAt") ?: 0L,
                    letta    = doc.getBoolean("letta") ?: false
                )
            }
    } catch (_: Exception) { emptyList() }

    suspend fun segnaSegnalazioneLetta(externalId: String, segnalazioneId: String) {
        try {
            db.collection("segnalazioni")
                .document(externalId)
                .collection("lista")
                .document(segnalazioneId)
                .update("letta", true).await()
        } catch (_: Exception) {}
    }

    data class SegnalazioneConTitolo(
        val externalId: String,
        val totale:     Long
    )

    data class SegnalazioneItem(
        val id:       String,
        val tipo:     String,
        val testo:    String,
        val uid:      String,
        val creatoAt: Long,
        val letta:    Boolean
    )

    // ── Admin ─────────────────────────────────────────────────────────────────

    /** Controlla se l'utente corrente ha il campo isAdmin: true su Firestore */
    suspend fun isAdmin(): Boolean {
        val currentUid = uid ?: return false
        return try {
            val doc = db.collection("users").document(currentUid).get().await()
            doc.getBoolean("isAdmin") == true
        } catch (_: Exception) { false }
    }

    // ── Admin: gestione curiosità ─────────────────────────────────────────────

    data class CuriositaRemota(
        val externalId:      String,
        val titolo:          String,
        val corpo:           String,
        val categoria:       String,
        val emoji:           String,
        val domanda:         String?       = null,
        val rispostaCorretta: String?      = null,
        val risposteErrate:  List<String>? = null,
        val spiegazione:     String?       = null
    )

    /** Aggiunge o sovrascrive una curiosità su Firestore.
     *  Se era una MODIFICA (documento già esistente) e aveva dislike:
     *  - azzera i dislike nel contatore
     *  - elimina il voto -1 per ogni utente che aveva messo dislike
     *  - crea una notifica in-app per ognuno di loro */
    suspend fun salvaCuriosita(c: CuriositaRemota): Result<Unit> = try {
        val docRef = db.collection("curiosita").document(c.externalId)

        // Controlla se è una modifica (documento già esiste)
        val esistente = docRef.get().await().exists()

        docRef.set(mapOf(
            "titolo"    to c.titolo,
            "corpo"     to c.corpo,
            "categoria" to c.categoria,
            "emoji"     to c.emoji
        )).await()

        if (c.domanda != null && c.rispostaCorretta != null) {
            docRef.collection("quiz").document("domanda").set(mapOf(
                "domanda"          to c.domanda,
                "rispostaCorretta" to c.rispostaCorretta,
                "risposteErrate"   to (c.risposteErrate ?: emptyList<String>()),
                "spiegazione"      to (c.spiegazione ?: "")
            )).await()
        }

        // Se è una modifica, notifica gli utenti che avevano segnalato la pillola
        if (esistente) {
            notificaAggiornamento(c.externalId, c.titolo)
        }

        incrementaVersione()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    /** Quando una pillola viene aggiornata, notifica chi l'aveva segnalata. */
    private suspend fun notificaAggiornamento(externalId: String, titoloCuriosita: String) {
        try {
            val segnalazioniSnap = db.collection("segnalazioni")
                .document(externalId)
                .collection("lista")
                .whereEqualTo("letta", false)
                .get().await()

            if (segnalazioniSnap.isEmpty) return

            // Raccogli uid unici dei segnalanti
            val uidSegnalanti = segnalazioniSnap.documents
                .mapNotNull { it.getString("uid") }
                .filter { it.isNotBlank() && it != "anonimo" }
                .distinct()

            val ora = System.currentTimeMillis()

            // Invia notifiche e segna segnalazioni come lette in un unico batch
            val batch = db.batch()

            // Segna tutte le segnalazioni come lette
            segnalazioniSnap.documents.forEach { batch.update(it.reference, "letta", true) }

            // Aggiunge le notifiche al batch per ogni segnalante
            uidSegnalanti.forEach { uidUtente ->
                val notificaRef = db.collection("notifiche")
                    .document(uidUtente)
                    .collection("lista")
                    .document()
                batch.set(notificaRef, mapOf(
                    "titolo"     to "Curiosità aggiornata ✏️",
                    "corpo"      to "«$titoloCuriosita» è stata revisionata dopo la tua segnalazione. Grazie!",
                    "letta"      to false,
                    "creatoAt"   to ora,
                    "externalId" to externalId,
                    "tipo"       to "pillola"
                ))
            }

            // Commit atomico: o tutto va a buon fine o niente
            batch.commit().await()
        } catch (e: Exception) {
            // Logga l'errore invece di inghiottirlo silenziosamente
            android.util.Log.e("FirebaseManager", "notificaAggiornamento failed for $externalId", e)
        }
    }

    // ── Notifiche in-app ──────────────────────────────────────────────────────

    data class NotificaInApp(
        val id:          String,
        val titolo:      String,
        val corpo:       String,
        val creatoAt:    Long,
        val externalId:  String?,
        val tipo:        String = "pillola"   // "pillola" | "broadcast"
    )

    suspend fun caricaNotifichePendenti(): List<NotificaInApp> {
        val currentUid = uid ?: return emptyList()
        return try {
            db.collection("notifiche")
                .document(currentUid)
                .collection("lista")
                .whereEqualTo("letta", false)
                .get().await()
                .documents.mapNotNull { doc ->
                    NotificaInApp(
                        id         = doc.id,
                        titolo     = doc.getString("titolo")     ?: return@mapNotNull null,
                        corpo      = doc.getString("corpo")      ?: "",
                        creatoAt   = doc.getLong("creatoAt")     ?: 0L,
                        externalId = doc.getString("externalId"),
                        tipo       = doc.getString("tipo")       ?: "pillola"
                    )
                }.sortedByDescending { it.creatoAt }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun segnaNotificaLetta(notificaId: String) {
        val currentUid = uid ?: return
        try {
            db.collection("notifiche")
                .document(currentUid)
                .collection("lista")
                .document(notificaId)
                .update("letta", true).await()
        } catch (_: Exception) {}
    }

    suspend fun segnaNotificheTutteLette(notifiche: List<NotificaInApp>) {
        val currentUid = uid ?: return
        if (notifiche.isEmpty()) return
        try {
            val batch = db.batch()
            notifiche.forEach { n ->
                val ref = db.collection("notifiche")
                    .document(currentUid)
                    .collection("lista")
                    .document(n.id)
                batch.update(ref, "letta", true)
            }
            batch.commit().await()
        } catch (_: Exception) {}
    }

    /** Import bulk: lista di curiosità → Firestore in batch */
    suspend fun importaBulk(lista: List<CuriositaRemota>): Result<Int> = try {
        // Firestore batch limit: 500 operazioni per batch
        lista.chunked(200).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { c ->
                val docRef = db.collection("curiosita").document(c.externalId)
                batch.set(docRef, mapOf(
                    "titolo"    to c.titolo,
                    "corpo"     to c.corpo,
                    "categoria" to c.categoria,
                    "emoji"     to c.emoji
                ))
            }
            batch.commit().await()

            // Quiz separati (non entrano nel batch per via delle subcollection)
            chunk.forEach { c ->
                if (c.domanda != null && c.rispostaCorretta != null) {
                    db.collection("curiosita").document(c.externalId)
                        .collection("quiz").document("domanda")
                        .set(mapOf(
                            "domanda"          to c.domanda,
                            "rispostaCorretta" to c.rispostaCorretta,
                            "risposteErrate"   to (c.risposteErrate ?: emptyList<String>()),
                            "spiegazione"      to (c.spiegazione ?: "")
                        )).await()
                }
            }
        }
        incrementaVersione()
        Result.success(lista.size)
    } catch (e: Exception) { Result.failure(e) }

    /** Elimina una curiosità da Firestore */
    suspend fun eliminaCuriosita(externalId: String): Result<Unit> = try {
        val docRef = db.collection("curiosita").document(externalId)
        // Elimina il quiz se presente
        val quizDoc = docRef.collection("quiz").document("domanda").get().await()
        if (quizDoc.exists()) docRef.collection("quiz").document("domanda").delete().await()
        docRef.delete().await()
        incrementaVersione()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    /** Carica tutte le curiosità da Firestore (solo per admin) */
    suspend fun caricaTutteLeCuriositaRemote(): List<CuriositaRemota> = try {
        val snap = db.collection("curiosita")
            .whereNotEqualTo("__name__", "_meta_")
            .get().await()

        val docs = snap.documents.filter { it.id != "_meta_" }

        // Recupera tutti i quiz in parallelo instead of sequentially
        coroutineScope {
            docs.map { doc ->
                async {
                    val titolo = doc.getString("titolo") ?: return@async null
                    val quizDoc = try {
                        db.collection("curiosita").document(doc.id)
                            .collection("quiz").document("domanda").get().await()
                    } catch (_: Exception) { null }

                    @Suppress("UNCHECKED_CAST")
                    val risposteErrate = quizDoc?.get("risposteErrate") as? List<String>

                    CuriositaRemota(
                        externalId       = doc.id,
                        titolo           = titolo,
                        corpo            = doc.getString("corpo")     ?: "",
                        categoria        = doc.getString("categoria") ?: "",
                        emoji            = doc.getString("emoji")     ?: "",
                        domanda          = quizDoc?.getString("domanda"),
                        rispostaCorretta = quizDoc?.getString("rispostaCorretta"),
                        risposteErrate   = risposteErrate,
                        spiegazione      = quizDoc?.getString("spiegazione")
                    )
                }
            }.awaitAll().filterNotNull()
        }
    } catch (_: Exception) { emptyList() }

    private suspend fun incrementaVersione() {
        try {
            val metaRef = db.collection("curiosita").document("_meta_")
            db.runTransaction { tx ->
                val snap    = tx.get(metaRef)
                val current = snap.getLong("versione") ?: 0L
                tx.set(metaRef, mapOf("versione" to current + 1))
            }.await()
        } catch (_: Exception) {}
    }

    // ── Broadcast admin → tutti gli utenti ───────────────────────────────────

    suspend fun inviaBroadcast(titolo: String, corpo: String): Result<Unit> {
        return try {
            val ora         = System.currentTimeMillis()
            val broadcastId = "b$ora"

            val utenti = db.collection("users").get().await().documents.map { it.id }
            if (utenti.isEmpty()) return Result.failure(Exception("Nessun utente trovato"))

            utenti.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { uidUtente ->
                    val ref = db.collection("notifiche")
                        .document(uidUtente)
                        .collection("lista")
                        .document(broadcastId)
                    batch.set(ref, mapOf(
                        "titolo"     to titolo,
                        "corpo"      to corpo,
                        "letta"      to false,
                        "creatoAt"   to ora,
                        "externalId" to null,
                        "tipo"       to "broadcast"
                    ))
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}
