package com.example.sudoku.model

// 这个类匹配最外层的 JSON 结构
data class SudokuApiResponse(
    val reason: String,
    val result: SudokuResult?, // result 可能为空，所以设为可空
    val error_code: Int
)

// 这个类匹配 result 字段内部的结构
data class SudokuResult(
    val puzzle: Array<IntArray>,
    val solution: Array<IntArray>
)