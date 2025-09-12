package com.example.sudoku.game

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Generator {
    private val solver = Solver()

    // 这个方法现在只负责本地生成
    suspend fun generate(difficulty: Int): Array<IntArray> = withContext(Dispatchers.Default) {
        val holesToDig = when (difficulty) {
            1 -> 35 // 简单
            2 -> 45 // 中等
            else -> 50 // 困难
        }

        val fullBoard = generateFullSolution()
        digHoles(fullBoard, holesToDig)
    }

    private fun generateFullSolution(): Array<IntArray> {
        val board = Array(9) { IntArray(9) }
        fill(board)
        return board
    }

    private fun fill(board: Array<IntArray>): Boolean {
        val emptyCell = findEmpty(board) ?: return true
        val (row, col) = emptyCell
        val numbers = (1..9).toMutableList().shuffled()

        for (num in numbers) {
            if (isValid(board, row, col, num)) {
                board[row][col] = num
                if (fill(board)) {
                    return true
                }
                board[row][col] = 0
            }
        }
        return false
    }

    private fun digHoles(fullBoard: Array<IntArray>, holesToDig: Int): Array<IntArray> {
        val puzzle = fullBoard.map { it.clone() }.toTypedArray()
        var holesDug = 0
        val cells = (0..80).toMutableList().shuffled()

        for (cellIndex in cells) {
            if (holesDug >= holesToDig) break
            val row = cellIndex / 9
            val col = cellIndex % 9

            val temp = puzzle[row][col]
            if (temp == 0) continue

            puzzle[row][col] = 0
            if (solver.countSolutions(puzzle) != 1) {
                puzzle[row][col] = temp
            } else {
                holesDug++
            }
        }
        return puzzle
    }

    private fun findEmpty(board: Array<IntArray>): Pair<Int, Int>? {
        for (r in 0..8) for (c in 0..8) if (board[r][c] == 0) return r to c
        return null
    }

    private fun isValid(board: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        for (i in 0..8) if (board[row][i] == num || board[i][col] == num) return false
        val startRow = row - row % 3
        val startCol = col - col % 3
        for (i in 0..2) for (j in 0..2) if (board[i + startRow][j + startCol] == num) return false
        return true
    }
}