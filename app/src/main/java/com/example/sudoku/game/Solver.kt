package com.example.sudoku.game

class Solver {
    private var board: Array<IntArray> = Array(9) { IntArray(9) }
    private var solutionCount = 0

    fun solve(boardToSolve: Array<IntArray>): Boolean {
        this.board = boardToSolve.map { it.clone() }.toTypedArray()
        return backtrackSolve()
    }

    fun getSolution(): Array<IntArray> = board

    fun countSolutions(boardToCount: Array<IntArray>): Int {
        this.board = boardToCount.map { it.clone() }.toTypedArray()
        solutionCount = 0
        backtrackCountSolutions()
        return solutionCount
    }

    private fun backtrackSolve(): Boolean {
        val emptyCell = findEmptyCell() ?: return true
        val (row, col) = emptyCell

        for (num in 1..9) {
            if (isMoveValid(row, col, num)) {
                board[row][col] = num
                if (backtrackSolve()) {
                    return true
                }
                board[row][col] = 0 // Backtrack
            }
        }
        return false
    }

    private fun backtrackCountSolutions() {
        if (solutionCount > 1) return

        val emptyCell = findEmptyCell()
        if (emptyCell == null) {
            solutionCount++
            return
        }

        val (row, col) = emptyCell
        for (num in 1..9) {
            if (isMoveValid(row, col, num)) {
                board[row][col] = num
                backtrackCountSolutions()
                board[row][col] = 0 // Backtrack
            }
        }
    }

    private fun findEmptyCell(): Pair<Int, Int>? {
        for (r in 0..8) {
            for (c in 0..8) {
                if (board[r][c] == 0) return r to c
            }
        }
        return null
    }

    private fun isMoveValid(row: Int, col: Int, num: Int): Boolean {
        for (i in 0..8) {
            if (board[row][i] == num || board[i][col] == num) return false
        }
        val startRow = row - row % 3
        val startCol = col - col % 3
        for (i in 0..2) {
            for (j in 0..2) {
                if (board[i + startRow][j + startCol] == num) return false
            }
        }
        return true
    }
}