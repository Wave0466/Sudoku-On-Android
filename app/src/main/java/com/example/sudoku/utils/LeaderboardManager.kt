package com.example.sudoku.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object LeaderboardManager {

    private const val PREFS_NAME = "SudokuLeaderboard"
    private val gson = Gson()

    private fun getScoresKey(difficulty: Int) = "scores_$difficulty"

    fun saveScore(context: Context, score: Score) {
        val scoresKey = getScoresKey(score.difficulty)
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val json = sharedPreferences.getString(scoresKey, null)
        val type = object : TypeToken<MutableList<Score>>() {}.type
        val scores: MutableList<Score> = if (json == null) mutableListOf() else gson.fromJson(json, type)

        scores.add(score)

        scores.sortBy { it.timeInMillis }

        val topScores = scores.take(10)

        val newJson = gson.toJson(topScores)
        sharedPreferences.edit().putString(scoresKey, newJson).apply()
    }

    fun getScores(context: Context, difficulty: Int): List<Score> {
        val scoresKey = getScoresKey(difficulty)
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = sharedPreferences.getString(scoresKey, null)

        return if (json == null) {
            emptyList()
        } else {
            val type = object : TypeToken<List<Score>>() {}.type
            gson.fromJson(json, type)
        }
    }
}