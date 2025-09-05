package com.example.sudoku.utils

data class Score(
    val playerName: String,
    val timeInMillis: Long,
    val difficulty: Int // 1=简单, 2=中等, 3=困难
)