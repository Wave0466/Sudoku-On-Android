package com.example.sudoku.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sudoku.game.Generator
import com.example.sudoku.model.SudokuCell
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    val sudokuBoard = MutableLiveData<Array<Array<SudokuCell>>>()
    val selectedCell = MutableLiveData<Pair<Int, Int>?>()
    val isGameWon = MutableLiveData<Boolean>(false)
    val isLoading = MutableLiveData<Boolean>(false)

    private var initialBoard: Array<IntArray> = emptyArray()
    private var solution: Array<IntArray> = emptyArray() // 存储完整解

    fun startGame(difficulty: Int) {
        isLoading.postValue(true) // 开始加载
        viewModelScope.launch { // 在 ViewModel 的协程作用域中启动一个新协程
            // generator.generate 现在是一个 suspend 函数，只能在协程中调用
            // 它会自动在后台线程执行
            val generatedBoard = Generator().generate(difficulty)
            initialBoard = generatedBoard // 保存初始题目

            val board = Array(9) { row ->
                Array(9) { col ->
                    val value = initialBoard[row][col]
                    SudokuCell(row, col, value, value != 0)
                }
            }
            sudokuBoard.postValue(board) // 在后台线程用 postValue 更新 LiveData
            selectedCell.postValue(null)
            isGameWon.postValue(false)
            isLoading.postValue(false) // 加载完成
        }
    }

    fun selectCell(row: Int, col: Int) {
        selectedCell.postValue(Pair(row, col))
    }

    fun setNumber(number: Int) {
        val selCell = selectedCell.value ?: return
        val currentBoard = sudokuBoard.value ?: return

        val cell = currentBoard[selCell.first][selCell.second]
        if (!cell.isStartingCell) {
            cell.value = number
            sudokuBoard.postValue(currentBoard) // 通知 UI 更新
            checkWinCondition()
        }
    }

    // 提示功能
    fun getHint() {
        // (提示功能实现见后续 Solver 部分)
        // 简单思路：找到当前选中格子的正确答案并填入
    }

    private fun checkWinCondition() {
        val board = sudokuBoard.value ?: return
        for (i in 0..8) {
            for (j in 0..8) {
                if (board[i][j].value == 0) return // 还有空格
                if (!isMoveValid(i, j, board[i][j].value)) {
                    return // 有冲突
                }
            }
        }
        isGameWon.postValue(true)
    }

    private fun isMoveValid(row: Int, col: Int, num: Int): Boolean {
        val board = sudokuBoard.value ?: return false
        // 检查行
        for (c in 0..8) {
            if (c != col && board[row][c].value == num) return false
        }
        // 检查列
        for (r in 0..8) {
            if (r != row && board[r][col].value == num) return false
        }
        // 检查 3x3
        val startRow = row / 3 * 3
        val startCol = col / 3 * 3
        for (r in startRow until startRow + 3) {
            for (c in startCol until startCol + 3) {
                if ((r != row || c != col) && board[r][c].value == num) return false
            }
        }
        return true
    }
}