package com.example.sudoku.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.sudoku.R // <--- 检查这行是否和你的项目包名匹配

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 关键！确保这里加载的是 activity_main
        setContentView(R.layout.activity_main)

        // 找到布局文件中的按钮
        findViewById<Button>(R.id.btnNewGame).setOnClickListener {
            showDifficultyDialog()
        }
        findViewById<Button>(R.id.btnLeaderboard).setOnClickListener {
            startActivity(Intent(this, LeaderboardActivity::class.java))
        }
    }

    private fun showDifficultyDialog() {
        val difficulties = arrayOf("简单", "中等", "困难")
        AlertDialog.Builder(this)
            .setTitle("选择难度")
            .setItems(difficulties) { _, which ->
                // which is 0 for Easy, 1 for Medium, 2 for Hard
                startGame(which + 1)
            }
            .show()
    }

    private fun startGame(difficulty: Int) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("DIFFICULTY", difficulty)
        startActivity(intent)
    }
}