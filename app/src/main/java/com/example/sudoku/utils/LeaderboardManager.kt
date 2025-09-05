package com.example.sudoku.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object LeaderboardManager {

    private const val PREFS_NAME = "SudokuLeaderboard"
    private val gson = Gson()

    // 这个私有方法会根据难度生成不同的存储键名
    // 例如，简单难度 -> "scores_1", 中等 -> "scores_2"
    private fun getScoresKey(difficulty: Int) = "scores_$difficulty"

    /**
     * 保存一条新的分数记录。
     * 它会自动读取对应难度的旧记录，添加新记录，排序，然后保存回去。
     */
    fun saveScore(context: Context, score: Score) {
        val scoresKey = getScoresKey(score.difficulty)
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 1. 读取对应难度的旧分数列表
        val json = sharedPreferences.getString(scoresKey, null)
        val type = object : TypeToken<MutableList<Score>>() {}.type
        val scores: MutableList<Score> = if (json == null) mutableListOf() else gson.fromJson(json, type)

        // 2. 添加新分数
        scores.add(score)

        // 3. 按照用时（从低到高）对列表进行排序
        scores.sortBy { it.timeInMillis }

        // 4. (可选) 只保留分数最高的（用时最短的）前 10 名
        val topScores = scores.take(10)

        // 5. 将更新后的列表转换回 JSON 字符串并保存
        val newJson = gson.toJson(topScores)
        sharedPreferences.edit().putString(scoresKey, newJson).apply()
    }

    fun getScores(context: Context, difficulty: Int): List<Score> {
        val scoresKey = getScoresKey(difficulty)
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = sharedPreferences.getString(scoresKey, null)

        return if (json == null) {
            // 如果没有保存过记录，返回一个空列表
            emptyList()
        } else {
            // 如果有记录，将其从 JSON 字符串转换回 Score 列表
            val type = object : TypeToken<List<Score>>() {}.type
            gson.fromJson(json, type)
        }
    }
}