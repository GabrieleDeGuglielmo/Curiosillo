package com.example.curiosillo.firebase

import com.example.curiosillo.data.BadgeSbloccato
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user   = result.user!!
        // Crea il documento profilo su Firestore
        creaProfiloSeNonEsiste(user.uid, username, email)
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun loginGoogle(idToken: String): Result<FirebaseUser> = try {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result     = auth.signInWithCredential(credential).await()
        val user       = result.user!!
        val isNuovo    = result.additionalUserInfo?.isNewUser ?: false
        if (isNuovo) {
            creaProfiloSeNonEsiste(user.uid, user.displayName ?: "Curioso", user.email ?: "")
        }
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun logout() = auth.signOut()

    // ── Profilo ───────────────────────────────────────────────────────────────

    private suspend fun creaProfiloSeNonEsiste(uid: String, username: String, email: String) {
        db.collection("users").document(uid)
            .collection("data").document("profile")
            .set(mapOf(
                "username"       to username,
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
     * Sincronizza il voto di un utente su una curiosità.
     * Path: voti/{externalId}
     *   → campi "likes" e "dislikes": contatori aggregati
     *   → subcollection "utenti/{uid}": voto individuale per-utente
     */
    suspend fun sincronizzaVoto(externalId: String, vecchioVoto: Int?, nuovoVoto: Int?) {
        if (vecchioVoto == nuovoVoto) return
        try {
            val docRef = db.collection("voti").document(externalId)
            val uidRef = docRef.collection("utenti").document(uid ?: "anonimo")

            db.runTransaction { tx ->
                val snap     = tx.get(docRef)
                var likes    = snap.getLong("likes")    ?: 0L
                var dislikes = snap.getLong("dislikes") ?: 0L

                // Rimuovi vecchio voto dal contatore
                when (vecchioVoto) {
                    1  -> likes    = maxOf(0, likes - 1)
                    -1 -> dislikes = maxOf(0, dislikes - 1)
                }
                // Aggiungi nuovo voto al contatore
                when (nuovoVoto) {
                    1  -> likes++
                    -1 -> dislikes++
                }

                tx.set(docRef, mapOf("likes" to likes, "dislikes" to dislikes), SetOptions.merge())

                if (nuovoVoto != null) {
                    tx.set(uidRef, mapOf(
                        "uid"       to (uid ?: ""),
                        "voto"      to nuovoVoto,
                        "timestamp" to System.currentTimeMillis()
                    ))
                } else {
                    tx.delete(uidRef)
                }
            }.await()
        } catch (_: Exception) {}
    }

    /** Carica tutti i documenti voti — usato dalla schermata admin */
    suspend fun caricaTuttiVoti(): List<VotoCuriosita> = try {
        val docs = db.collection("voti").get().await()
        docs.map { doc ->
            VotoCuriosita(
                externalId = doc.id,
                likes      = doc.getLong("likes")    ?: 0L,
                dislikes   = doc.getLong("dislikes") ?: 0L
            )
        }.sortedByDescending { it.likes + it.dislikes }
    } catch (_: Exception) { emptyList() }

    /** Carica i voti per-utente di una singola curiosità — usato dalla schermata admin */
    suspend fun caricaVotiPerUtente(externalId: String): List<VotoUtente> = try {
        val docs = db.collection("voti").document(externalId)
            .collection("utenti").get().await()
        docs.map { doc ->
            VotoUtente(
                uid       = doc.getString("uid") ?: "",
                voto      = (doc.getLong("voto") ?: 0L).toInt(),
                timestamp = doc.getLong("timestamp") ?: 0L
            )
        }
    } catch (_: Exception) { emptyList() }

    data class VotoCuriosita(
        val externalId: String,
        val likes:      Long,
        val dislikes:   Long
    )

    data class VotoUtente(
        val uid:       String,
        val voto:      Int,
        val timestamp: Long
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
}