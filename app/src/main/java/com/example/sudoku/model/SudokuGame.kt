package com.example.sudoku.model

class SudokuGame {
    var selectedRow: Int = -1
    var selectedCol: Int = -1

    // 0 表示空格子
    // 可以用一个更复杂的 Cell 类来存储初始值、用户输入值等状态
    val board: Array<IntArray> = Array(9) { IntArray(9) }
    private val initialBoard: Array<IntArray> = Array(9) { IntArray(9) } // 存储初始题目

    init {
        // ... 初始化时可以加载一个谜题
    }

    // 设置用户输入的数字
    fun setNumber(row: Int, col: Int, number: Int) {
        if (isCellInitial(row, col)) return // 初始数字不可修改
        if (number in 0..9) {
            board[row][col] = number
        }
    }

    fun getNumber(row: Int, col: Int): Int = board[row][col]

    fun isCellInitial(row: Int, col: Int): Boolean = initialBoard[row][col] != 0

    // 检查游戏是否完成并正确
    fun isGameWon(): Boolean {
        for (i in 0..8) {
            for (j in 0..8) {
                if (board[i][j] == 0) return false // 还有空格
                if (!isMoveValid(i, j, board[i][j], true)) return false // 有冲突
            }
        }
        return true
    }

    // 检查某个数字放在(row, col)是否合法
    fun isMoveValid(row: Int, col: Int, num: Int, isCheckingSolution: Boolean = false): Boolean {
        // 检查行
        for (c in 0..8) {
            if (c != col && board[row][c] == num) return false
        }
        // 检查列
        for (r in 0..8) {
            if (r != row && board[r][col] == num) return false
        }
        // 检查 3x3 小方块
        val startRow = row / 3 * 3
        val startCol = col / 3 * 3
        for (r in startRow until startRow + 3) {
            for (c in startCol until startCol + 3) {
                if ((r != row || c != col) && board[r][c] == num) return false
            }
        }
        return true
    }
}