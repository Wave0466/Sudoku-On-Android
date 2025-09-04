package com.example.sudoku.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object LeaderboardManager {
    private const val PREFS_NAME = "SudokuLeaderboard"
    private const val KEY_SCORES = "scores"

    fun saveScore(context: Context, score: Score) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val scores = getScores(context).toMutableList()
        scores.add(score)
        scores.sortBy { it.timeInMillis }
        val topScores = scores.take(20)

        val gson = Gson()
        val jsonString = gson.toJson(topScores)
        prefs.edit().putString(KEY_SCORES, jsonString).apply()
    }

    fun getScores(context: Context): List<Score> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_SCORES, null) ?: return emptyList()

        val gson = Gson()
        val type = object : TypeToken<List<Score>>() {}.type
        return gson.fromJson(jsonString, type)
    }
}