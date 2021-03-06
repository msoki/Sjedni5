package hr.ferit.matijasokol.sjedni5.data.firebase

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObject
import hr.ferit.matijasokol.sjedni5.models.Player
import hr.ferit.matijasokol.sjedni5.models.Question
import hr.ferit.matijasokol.sjedni5.models.Term
import hr.ferit.matijasokol.sjedni5.other.Constants.ADMINS_COLLECTION
import hr.ferit.matijasokol.sjedni5.other.Constants.IMAGE_NAME_FIELD
import hr.ferit.matijasokol.sjedni5.other.Constants.PLAYERS_COLLECTION
import hr.ferit.matijasokol.sjedni5.other.Constants.QUESTION_COLLECTION
import hr.ferit.matijasokol.sjedni5.other.Constants.TERMS_COLLECTION
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Named

class FirestoreService @Inject constructor(
    @Named(QUESTION_COLLECTION) private val questionsCollectionReference: CollectionReference,
    @Named(PLAYERS_COLLECTION) private val playersCollectionReference: CollectionReference,
    @Named(TERMS_COLLECTION) private val termsCollectionReference: CollectionReference,
    @Named(ADMINS_COLLECTION) private val adminsCollectionReference: CollectionReference
) {

    suspend fun getQuestions(): QuerySnapshot = questionsCollectionReference.get().await()

    suspend fun getTerms(): QuerySnapshot = termsCollectionReference.get().await()

    suspend fun getAdmins(): QuerySnapshot = adminsCollectionReference.get().await()

    suspend fun uploadPlayer(player: Player): DocumentReference = playersCollectionReference.add(player).await()

    suspend fun getPlayers() = playersCollectionReference.get().await().documents

    suspend fun deleteAllPlayers(documents: List<DocumentSnapshot>) = documents.forEach {
        it.reference.delete().await()
    }

    suspend fun uploadQuestion(question: Question): DocumentReference = questionsCollectionReference.add(question).await()

    suspend fun uploadTerm(term: Term, extension: String): String = termsCollectionReference.add(term).await().id.also {
        termsCollectionReference.document(it).update(IMAGE_NAME_FIELD, "$it.$extension").await()
    }

    suspend fun deleteQuestion(documentSnapshot: DocumentSnapshot) = documentSnapshot.reference.delete().await()

    suspend fun undoDeleteQuestion(documentSnapshot: DocumentSnapshot) = documentSnapshot.toObject<Question>()?.let {
        documentSnapshot.reference.set(it).await()
    }

    suspend fun deleteTerm(documentSnapshot: DocumentSnapshot) = documentSnapshot.reference.delete().await()

    suspend fun undoDeleteTerm(documentSnapshot: DocumentSnapshot) = documentSnapshot.toObject<Term>()?.let {
        documentSnapshot.reference.set(it).await()
    }
}