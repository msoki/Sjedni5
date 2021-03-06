package hr.ferit.matijasokol.sjedni5.ui.fragments.createDeleteQuestions

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import hr.ferit.matijasokol.sjedni5.R
import hr.ferit.matijasokol.sjedni5.models.Categories
import hr.ferit.matijasokol.sjedni5.models.Question
import hr.ferit.matijasokol.sjedni5.models.Resource
import hr.ferit.matijasokol.sjedni5.models.Term
import hr.ferit.matijasokol.sjedni5.other.*
import hr.ferit.matijasokol.sjedni5.other.Constants.CATEGORY_FIELD
import hr.ferit.matijasokol.sjedni5.other.Constants.LEVEL_FIELD
import hr.ferit.matijasokol.sjedni5.other.Constants.QUESTION_COLLECTION
import hr.ferit.matijasokol.sjedni5.other.Constants.TERMS_COLLECTION
import hr.ferit.matijasokol.sjedni5.other.Constants.TEXT_FIELD
import hr.ferit.matijasokol.sjedni5.ui.activities.QuizActivity
import hr.ferit.matijasokol.sjedni5.ui.base.BaseFragment
import hr.ferit.matijasokol.sjedni5.ui.dialogs.questionInput.QuestionInputDialog
import hr.ferit.matijasokol.sjedni5.ui.dialogs.termInput.TermInputDialog
import jp.wasabeef.recyclerview.animators.SlideInDownAnimator
import kotlinx.android.synthetic.main.fragment_create_delete_questions.*

@AndroidEntryPoint
class CreateDeleteQuestionsFragment : BaseFragment(R.layout.fragment_create_delete_questions) {

    private val viewModel: CreateDeleteQuestionsViewModel by viewModels()
    private var dialog: AlertDialog? = null

    private val questionRecyclerAdapterCategory1 by lazy {
        getAdapterForQuestionCategory(
            Categories.CATEGORY_1
        )
    }
    private val questionRecyclerAdapterCategory2 by lazy {
        getAdapterForQuestionCategory(
            Categories.CATEGORY_2
        )
    }
    private val termsRecyclerAdapter by lazy { getAdapterForTermCategory() }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigate(CreateDeleteQuestionsFragmentDirections.actionCreateDeleteQuestionsFragmentToMenuFragment())
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        (requireActivity() as QuizActivity).viewModel.setTitle(getString(R.string.questions_terms))
    }

    override fun setUpUi() {
        setHasOptionsMenu(true)
        setListeners()
        setRecycler()
        setObservers()
        radioGroup.check(rbCategory1.id)
    }

    private fun setObservers() {
        viewModel.questionDeleteResponse.observe(viewLifecycleOwner, { response ->
            when (response) {
                is Resource.Loading -> { /* NO-OP*/
                }
                is Resource.Success -> {
                    response.data?.let {
                        showSnackbarForQuestions(it)
                    }
                }
                is Resource.Error -> {
                    response.message?.let {
                        rootElement.showSnackbar(it)
                    }
                }
            }
        })

        viewModel.termDeleteResponse.observe(viewLifecycleOwner, { response ->
            when (response) {
                is Resource.Loading -> { /* NO-OP*/
                }
                is Resource.Success -> {
                    response.data?.let {
                        showSnackbarForTerms(it)
                    }
                }
                is Resource.Error -> {
                    response.message?.let {
                        rootElement.showSnackbar(it)
                    }
                }
            }
        })

        viewModel.playersDeleteResponse.observe(viewLifecycleOwner, { response ->
            when (response) {
                is Resource.Loading -> {
                    showProgressDialog()
                }
                is Resource.Success -> {
                    dialog?.dismiss()
                    response.data?.let {
                        requireContext().displayMessage(it)
                    }
                }
                is Resource.Error -> {
                    dialog?.dismiss()
                    response.data?.let {
                        requireContext().displayMessage(it)
                    }
                }
            }
        })
    }

    private fun setListeners() {
        fab.setOnClickListener {
            openAppropriateDialog()
        }

        radioGroup.setOnCheckedChangeListener { _, radioButtonId ->
            recycler.adapter = when (radioButtonId) {
                rbCategory1.id -> {
                    startListening(radioButtonId)
                    questionRecyclerAdapterCategory1
                }
                rbCategory2.id -> {
                    startListening(radioButtonId)
                    questionRecyclerAdapterCategory2
                }
                else -> {
                    startListening(radioButtonId)
                    termsRecyclerAdapter
                }
            }
        }
    }

    private fun openAppropriateDialog() {
        when (radioGroup.checkedRadioButtonId) {
            rbCategory1.id, rbCategory2.id -> {
                if (hasInternetConnection(requireContext())) {
                    openAddQuestionDialog()
                } else {
                    rootElement.showSnackbar(
                        getString(R.string.int_conn_need_add_quest_terms),
                        Snackbar.LENGTH_LONG
                    )
                }
            }
            else -> if (hasInternetConnection(requireContext())) {
                openAddTermDialog()
            } else {
                rootElement.showSnackbar(
                    getString(R.string.int_conn_need_add_quest_terms),
                    Snackbar.LENGTH_LONG
                )
            }
        }
    }

    private fun openAddQuestionDialog() {
        val level = when (radioGroup.checkedRadioButtonId) {
            R.id.rbCategory1 -> 1
            else -> 2
        }
        val dialog = QuestionInputDialog.newInstance(level)
        dialog.show(childFragmentManager, "")
    }

    private fun openAddTermDialog() {
        val dialog = TermInputDialog.newInstance()
        dialog.show(childFragmentManager, "")
    }

    private fun startListening(radioButtonId: Int) {
        when (radioButtonId) {
            rbCategory1.id -> {
                questionRecyclerAdapterCategory1.startListening()
                questionRecyclerAdapterCategory2.stopListening()
                termsRecyclerAdapter.stopListening()
            }
            rbCategory2.id -> {
                questionRecyclerAdapterCategory1.stopListening()
                questionRecyclerAdapterCategory2.startListening()
                termsRecyclerAdapter.stopListening()
            }
            rbCategory3.id -> {
                questionRecyclerAdapterCategory1.stopListening()
                questionRecyclerAdapterCategory2.stopListening()
                termsRecyclerAdapter.startListening()
            }
        }
    }

    private fun stopListening() {
        questionRecyclerAdapterCategory1.stopListening()
        questionRecyclerAdapterCategory2.stopListening()
        termsRecyclerAdapter.stopListening()
    }

    private fun getAdapterForQuestionCategory(category: Categories): QuestionRecyclerAdapter {
        val query = Firebase.firestore
            .collection(QUESTION_COLLECTION)
            .whereEqualTo(CATEGORY_FIELD, category.type)
            .orderBy(LEVEL_FIELD)

        val options = FirestoreRecyclerOptions.Builder<Question>()
            .setQuery(query, Question::class.java)
            .build()

        return QuestionRecyclerAdapter(options)
    }

    private fun getAdapterForTermCategory(): TermsRecyclerAdapter {
        val query = Firebase.firestore
            .collection(TERMS_COLLECTION)
            .orderBy(TEXT_FIELD)

        val options = FirestoreRecyclerOptions.Builder<Term>()
            .setQuery(query, Term::class.java)
            .build()

        return TermsRecyclerAdapter(options)
    }

    private fun setRecycler() {
        recycler.apply {
            adapter = questionRecyclerAdapterCategory1
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = SlideInDownAnimator().apply {
                addDuration = 200
            }
            setHasFixedSize(true)
        }

        ItemTouchHelper(RecyclerSwipeCallback { onItemSwiped(it) }).attachToRecyclerView(recycler)
    }

    private fun onItemSwiped(position: Int) {
        when (radioGroup.checkedRadioButtonId) {
            R.id.rbCategory1 -> {
                viewModel.deleteQuestion(questionRecyclerAdapterCategory1.getItemSnapshot(position))
            }
            R.id.rbCategory2 -> {
                viewModel.deleteQuestion(questionRecyclerAdapterCategory2.getItemSnapshot(position))
            }
            R.id.rbCategory3 -> {
                viewModel.deleteTermFromFirestore(termsRecyclerAdapter.getItemSnapshot(position))
            }
        }
    }

    private fun showSnackbarForQuestions(documentSnapshot: DocumentSnapshot) {
        Snackbar.make(rootElement, getString(R.string.question_deleted), Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.undo)) {
                viewModel.undoDeleteQuestion(documentSnapshot)
            }.show()
    }

    private fun showSnackbarForTerms(documentSnapshot: DocumentSnapshot) {
        var delete = true

        val snackbar =
            Snackbar.make(rootElement, getString(R.string.term_deleted), Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.undo)) {
                    viewModel.undoDeleteTerm(documentSnapshot)
                    delete = false
                }

        snackbar.show()
        snackbar.addCallback(object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                super.onDismissed(transientBottomBar, event)
                val term = documentSnapshot.toObject<Term>()
                if (delete && term != null) {
                    viewModel.deleteTermFromStorage(term.imageName)
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.create_delete_frag_options_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.deletePlayers -> {
                requireContext().showAlertDialog(
                    R.string.delete_all_players,
                    R.string.players_permanently_deleted,
                    R.string.yes,
                    R.string.no
                ) {
                    deleteAllPlayers()
                }
                true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    private fun deleteAllPlayers() {
        viewModel.checkPlayersCollection()
    }

    private fun showProgressDialog() {
        if (dialog == null) {
            dialog = AlertDialog.Builder(requireContext())
                .setCancelable(false)
                .setView(requireActivity().layoutInflater.inflate(R.layout.dialog_progress, null))
                .create()
        }

        dialog?.show()
    }

    override fun onStart() {
        super.onStart()
        startListening(radioGroup.checkedRadioButtonId)
    }

    override fun onStop() {
        super.onStop()
        stopListening()
    }
}