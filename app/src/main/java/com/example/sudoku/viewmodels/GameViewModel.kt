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

    fun startGame(difficulty: Int) {
        isLoading.postValue(true)
        viewModelScope.launch {
            val generatedBoard = Generator().generate(difficulty)
            initialBoard = generatedBoard

            val board = Array(9) { row ->
                Array(9) { col ->
                    val value = initialBoard[row][col]
                    SudokuCell(row, col, value, value != 0)
                }
            }
            sudokuBoard.postValue(board)
            selectedCell.postValue(null)
            isGameWon.postValue(false)
            isLoading.postValue(false)
        }
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

        // 优化点 1: 在一次循环中完成所有状态重置和高亮计算
        val selectedValue = if (selected != null) board[selected.first][selected.second].value else 0

        for (r in 0..8) {
            for (c in 0..8) {
                val cell = board[r][c]
                // 重置状态
                cell.isHighlighted = false
                cell.isConflicting = false
                // 计算高亮
                if (selectedValue != 0 && cell.value == selectedValue) {
                    cell.isHighlighted = true
                }
            }
        }

        // 优化点 2: 冲突计算通过直接创建 Array 避免了不必要的 List 转换
        for (r in 0..8) {
            findConflicts(board[r])
        }
        for (c in 0..8) {
            val column = Array(9) { r -> board[r][c] }
            findConflicts(column)
        }
        for (br in 0..2) for (bc in 0..2) {
            val box = Array(9) { i ->
                val r = br * 3 + i / 3
                val c = bc * 3 + i % 3
                board[r][c]
            }
            findConflicts(box)
        }

        checkWinCondition(board)
        sudokuBoard.value = board
    }

    // 优化点 3: findConflicts 的参数类型改为 Array<SudokuCell> 避免在调用时创建临时列表
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
}