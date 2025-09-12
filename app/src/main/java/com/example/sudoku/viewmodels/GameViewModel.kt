package com.example.sudoku.viewmodels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sudoku.game.Generator
import com.example.sudoku.model.SudokuCell
import com.example.sudoku.utils.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameViewModel : ViewModel() {

    val sudokuBoard = MutableLiveData<Array<Array<SudokuCell>>>()
    val selectedCell = MutableLiveData<Pair<Int, Int>?>()
    val isGameWon = MutableLiveData<Boolean>(false)
    val isLoading = MutableLiveData<Boolean>(false)

    private var initialBoard: Array<IntArray> = emptyArray()
    private val generator = Generator()

    fun startGame(mode: String, difficulty: Int) {
        isLoading.value = true
        viewModelScope.launch {
            Log.d("GameViewModel_Debug", "startGame 调用，模式: $mode, 难度: $difficulty")
            // 在后台线程获取棋盘数据
            val board = when (mode) {
                "API" -> {
                    Log.d("GameViewModel_Debug", "选择 API 路径获取棋盘...")
                    fetchBoardFromApi(difficulty)
                }
                else -> {
                    Log.d("GameViewModel_Debug", "选择 LOCAL 路径生成棋盘...")
                    generator.generate(difficulty)
                }
            }

            // 棋盘加载后，在主线程更新UI
            withContext(Dispatchers.Main) {
                initializeBoardFrom2DArray(board)
                isLoading.value = false
            }
        }
    }

    private suspend fun fetchBoardFromApi(difficulty: Int): Array<IntArray> {
        val apiKey = "1133790bb60f6a1689da684096082830"
        val difficultyStr = when (difficulty) {
            1 -> "easy"
            2 -> "normal"
            3 -> "hard"
            4 -> "veryhard"
            else -> "easy" // 提供一个默认值以防万一
        }

        return try {
            Log.d("GameViewModel_Debug", "正在向 API 发起请求...")
            val response = RetrofitInstance.api.generateSudoku(apiKey, difficultyStr)
            if (response.error_code == 0 && response.result != null) {
                Log.d("GameViewModel_Debug", "API 请求成功！")
                response.result.puzzle
            } else {
                Log.e("GameViewModel_Debug", "API 返回错误: (${response.error_code}) ${response.reason}")
                createEmptyBoard("API 返回错误")
            }
        } catch (e: Exception) {
            Log.e("GameViewModel_Debug", "网络请求异常", e)
            createEmptyBoard("网络异常")
        }
    }

    fun initializeBoard(boardData: IntArray) {
        val board2D = Array(9) { r ->
            IntArray(9) { c ->
                boardData[r * 9 + c]
            }
        }
        initializeBoardFrom2DArray(board2D)
    }

    private fun initializeBoardFrom2DArray(boardData: Array<IntArray>) {
        initialBoard = boardData.map { it.clone() }.toTypedArray()
        val newBoard = Array(9) { row ->
            Array(9) { col ->
                val value = initialBoard[row][col]
                SudokuCell(row, col, value, value != 0)
            }
        }
        sudokuBoard.value = newBoard
        selectedCell.value = null
        isGameWon.value = false
        updateCellStates()
    }

    fun selectCell(row: Int, col: Int) {
        selectedCell.value = Pair(row, col)
        updateCellStates()
    }

    fun setNumber(number: Int) {
        val selCellPos = selectedCell.value ?: return
        val currentBoard = sudokuBoard.value ?: return
        val cell = currentBoard[selCellPos.first][selCellPos.second]
        if (!cell.isStartingCell) {
            cell.value = number
            updateCellStates()
        }
    }

    private fun updateCellStates() {
        val board = sudokuBoard.value ?: return
        val selected = selectedCell.value
        val selectedValue = if (selected != null) board[selected.first][selected.second].value else 0
        for (r in 0..8) {
            for (c in 0..8) {
                val cell = board[r][c]
                cell.isHighlighted = false
                cell.isConflicting = false
                if (selectedValue != 0 && cell.value == selectedValue) {
                    cell.isHighlighted = true
                }
            }
        }
        for (r in 0..8) { findConflicts(board[r]) }
        for (c in 0..8) { findConflicts(Array(9) { r -> board[r][c] }) }
        for (br in 0..2) for (bc in 0..2) {
            findConflicts(Array(9) { i -> val r = br * 3 + i / 3; val c = bc * 3 + i % 3; board[r][c] })
        }
        checkWinCondition(board)
        sudokuBoard.value = board
    }

    private fun findConflicts(cells: Array<SudokuCell>) {
        val seen = mutableMapOf<Int, MutableList<SudokuCell>>()
        cells.forEach { cell ->
            if (cell.value != 0) {
                seen.getOrPut(cell.value) { mutableListOf() }.add(cell)
            }
        }
        seen.values.filter { it.size > 1 }.flatten().forEach { it.isConflicting = true }
    }

    private fun checkWinCondition(board: Array<Array<SudokuCell>>) {
        var isFull = true
        var hasConflict = false
        for (row in board) {
            for (cell in row) {
                if (cell.value == 0) isFull = false
                if (cell.isConflicting) hasConflict = true
            }
        }
        if (isFull && !hasConflict) {
            isGameWon.value = true
        }
    }

    private fun createEmptyBoard(reason: String): Array<IntArray> {
        Log.d("GameViewModel_Debug", "创建了一个空棋盘，原因: $reason")
        return Array(9) { IntArray(9) { 0 } }
    }
}