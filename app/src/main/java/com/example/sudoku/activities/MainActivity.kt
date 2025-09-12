package com.example.sudoku.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.sudoku.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnNewGame).setOnClickListener {
            showDifficultyDialog()
        }

        // 为新按钮添加监听器
        findViewById<Button>(R.id.btnApiGame).setOnClickListener {
            showApiDifficultyDialog()
        }

        findViewById<Button>(R.id.btnLeaderboard).setOnClickListener {
            showLeaderboardDialog()
        }
    }

    private fun showApiDifficultyDialog() {
        // 根据 API 文档，定义所有支持的难度
        val apiDifficulties = arrayOf("简单 (easy)", "普通 (normal)", "困难 (hard)", "非常困难 (very hard)")
        AlertDialog.Builder(this)
            .setTitle("选择网络难度")
            .setItems(apiDifficulties) { _, which ->
                startApiGame(which + 1)
            }
            .show()
    }

    private fun showDifficultyDialog() {
        val difficulties = arrayOf("简单", "中等", "困难")
        AlertDialog.Builder(this)
            .setTitle("选择难度")
            .setItems(difficulties) { _, which ->
                startGame(which + 1)
            }
            .show()
    }

    private fun startGame(difficulty: Int) {
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra("GAME_MODE", "LOCAL")
            putExtra("DIFFICULTY", difficulty)
        }
        startActivity(intent)
    }

    private fun startApiGame(difficulty: Int) {
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra("GAME_MODE", "API")
            putExtra("DIFFICULTY", difficulty) // 传递用户选择的难度
        }
        startActivity(intent)
    }

    private fun showLeaderboardDialog() {
        val leaderboardOptions = arrayOf("简单榜", "中等榜", "困难榜")
        AlertDialog.Builder(this)
            .setTitle("选择排行榜")
            .setItems(leaderboardOptions) { _, which ->
                showLeaderboard(which + 1, leaderboardOptions[which])
            }
            .show()
    }

    private fun showLeaderboard(difficulty: Int, title: String) {
        val intent = Intent(this, LeaderboardActivity::class.java).apply {
            putExtra("DIFFICULTY", difficulty)
            putExtra("TITLE", title)
        }
        startActivity(intent)
    }
}