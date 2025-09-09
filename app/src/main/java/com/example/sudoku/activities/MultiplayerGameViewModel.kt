package com.example.sudoku.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.sudoku.model.SudokuCell
import android.util.Log

class MultiplayerGameViewModel : ViewModel() {
    val sudokuBoard = MutableLiveData<Array<Array<SudokuCell>>>()
    val selectedCell = MutableLiveData<Pair<Int, Int>?>()

    fun initializeBoard(boardData: IntArray) {
        Log.d("Multiplayer_Debug", "ViewModel: initializeBoard 被调用。")
        val newBoard = Array(9) { row ->
            Array(9) { col ->
                val value = boardData[row * 9 + col]
                // 假设初始给定的数字都是 starting cell
                SudokuCell(row, col, value, value != 0)
            }
        }
        sudokuBoard.value = newBoard
        selectedCell.value = null
        Log.d("Multiplayer_Debug", "ViewModel: LiveData 已更新。")
    }

    fun selectCell(row: Int, col: Int) {
        selectedCell.value = Pair(row, col)
        // 联机模式下可以简化或自定义高亮逻辑，这里暂时不做处理
    }

    fun updateCell(row: Int, col: Int, number: Int) {
        val currentBoard = sudokuBoard.value ?: return
        val cell = currentBoard[row][col]
        if (!cell.isStartingCell) {
            cell.value = number
            sudokuBoard.value = currentBoard
        }
    }
}