package com.example.sudoku.model

data class SudokuCell(
    val row: Int,
    val col: Int,
    var value: Int,
    val isStartingCell: Boolean = false // 标记是否为题目初始数字
)