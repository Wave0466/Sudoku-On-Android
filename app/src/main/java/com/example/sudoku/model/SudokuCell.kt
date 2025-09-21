package com.example.sudoku.model

data class SudokuCell(
    val row: Int,
    val col: Int,
    var value: Int,
    val isStartingCell: Boolean = false,

    var isHighlighted: Boolean = false,  // 用于同数字高亮
    var isConflicting: Boolean = false   // 用于冲突标记
)