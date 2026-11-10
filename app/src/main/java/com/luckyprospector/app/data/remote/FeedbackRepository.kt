package com.luckyprospector.app.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.luckyprospector.app.domain.SharingRequiresSignInException
import java.time.Instant
import kotlinx.coroutines.tasks.await

enum class FeedbackCategory { IDEA, PROBLEM, WRONG_DETECTION }

class FeedbackLimitReachedException : IllegalStateException("Daily feedback limit reached")

/**
 * Suggestions from the Profile screen, written to a collection nobody can read
 * from the app; the team reads it in the Firebase console.
 *
 * The message carries no account id and no name. Sign-in is still required,
 * because the daily limit that keeps the collection free of spam is counted per
 * account in the user's private subtree, the same way as publications.
 */
class FeedbackRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    suspend fun send(
        category: FeedbackCategory,
        message: String,
        contact: String?,
        appVersion: String,
        language: String,
    ) {
        val uid = auth.currentUser?.uid ?: throw SharingRequiresSignInException()
        val text = message.trim().take(MAX_MESSAGE)
        require(text.isNotEmpty()) { "Empty feedback" }

        val quotaRef = firestore.collection(FirestoreSharedSightingRepository.USERS).document(uid)
            .collection(FirestoreSharedSightingRepository.LIMITS).document(QUOTA_DOCUMENT)
        val quotaWrite = nextQuotaWrite(quotaRef.get().await().data, Instant.now(), MAX_PER_DAY, LAST_SENT_AT)
            ?: throw FeedbackLimitReachedException()

        val document = buildMap<String, Any> {
            put("category", category.name)
            put("message", text)
            contact?.trim()?.take(MAX_CONTACT)?.takeIf(String::isNotEmpty)?.let { put("contact", it) }
            put("appVersion", appVersion.take(32))
            put("language", language.take(16))
            put("createdAt", FieldValue.serverTimestamp())
        }
        firestore.batch()
            .set(firestore.collection(COLLECTION).document(), document)
            .apply { if (quotaWrite.startsNewWindow) set(quotaRef, quotaWrite.fields) else update(quotaRef, quotaWrite.fields) }
            .commit()
            .await()
    }

    companion object {
        internal const val COLLECTION = "feedback"
        internal const val QUOTA_DOCUMENT = "feedback"
        internal const val LAST_SENT_AT = "lastSentAt"

        /** Mirrored in firestore.rules. */
        const val MAX_PER_DAY = 10
        const val MAX_MESSAGE = 2000
        const val MAX_CONTACT = 200
    }
}
