package com.example.curiosillo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.data.CategoryPreferences
import com.example.curiosillo.repository.CuriosityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class QuizUiModel(
    val questionId: Int,
    val questionText: String,
    val answers: List<String>,
    val correctAnswer: String,
    val explanation: String,
    val category: String = ""
)

sealed class QuizUiState {
    object Loading : QuizUiState()
    object NoQuestions : QuizUiState()
    data class Question(
        val question: QuizUiModel,
        val current: Int,
        val total: Int,
        val score: Int
    ) : QuizUiState()

    data class Answered(
        val question: QuizUiModel, val selectedAnswer: String,
        val isCorrect: Boolean, val current: Int, val total: Int, val score: Int
    ) : QuizUiState()

    data class Summary(val score: Int, val total: Int) : QuizUiState()
}

class QuizViewModel(private val repo: CuriosityRepository, private val prefs: CategoryPreferences) :
    ViewModel() {
    private val _state = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    private var questions = listOf<QuizUiModel>()
    private var currentIndex = 0
    private var score = 0

    init {
        startQuiz()
    }

    fun startQuiz() {
        viewModelScope.launch {
            _state.value = QuizUiState.Loading
            val categorie = prefs.categorieAttive.first()
            val available = repo.countAvailableQuestions(categorie)
            if (available == 0) { _state.value = QuizUiState.NoQuestions; return@launch }
            val raw = repo.getQuizQuestionsWithCategory(minOf(available, 5), categorie)
            questions = raw.map { q ->
                QuizUiModel(q.id, q.questionText,
                    listOf(q.correctAnswer, q.wrongAnswer1, q.wrongAnswer2, q.wrongAnswer3).shuffled(),
                    q.correctAnswer, q.explanation, q.category)
            }
            currentIndex = 0; score = 0; push()
        }
    }

    fun answer(sel: String) {
        val s = _state.value as? QuizUiState.Question ?: return
        val ok = sel == s.question.correctAnswer
        if (ok) score++
        viewModelScope.launch {
            repo.salvaRisposta(s.question.questionId, ok)
        }
        _state.value = QuizUiState.Answered(s.question, sel, ok, s.current, s.total, score)
    }

    fun next() {
        currentIndex++; push()
    }

    private fun push() {
        _state.value = if (currentIndex < questions.size)
            QuizUiState.Question(questions[currentIndex], currentIndex + 1, questions.size, score)
        else QuizUiState.Summary(score, questions.size)
    }

    class Factory(
        private val repo: CuriosityRepository,
        private val prefs: CategoryPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(c: Class<T>): T =
            QuizViewModel(repo, prefs) as T
    }
}