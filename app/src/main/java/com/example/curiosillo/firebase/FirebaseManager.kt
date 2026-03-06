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
}
