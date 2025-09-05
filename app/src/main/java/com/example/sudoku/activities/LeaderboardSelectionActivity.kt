package com.example.sudoku.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sudoku.R

class LeaderboardSelectionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard_selection)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "选择榜单"

        findViewById<Button>(R.id.btnEasyBoard).setOnClickListener {
            showLeaderboard(1, "简单榜")
        }
        findViewById<Button>(R.id.btnMediumBoard).setOnClickListener {
            showLeaderboard(2, "中等榜")
        }
        findViewById<Button>(R.id.btnHardBoard).setOnClickListener {
            showLeaderboard(3, "困难榜")
        }

        // 为“战绩查询”按钮添加一个临时的提示
        findViewById<Button>(R.id.btnStats).setOnClickListener {
            // TODO: 未来在这里跳转到真正的战绩查询 Activity
            Toast.makeText(this, "战绩查询功能正在开发中...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLeaderboard(difficulty: Int, title: String) {
        val intent = Intent(this, LeaderboardActivity::class.java).apply {
            putExtra("DIFFICULTY", difficulty)
            putExtra("TITLE", title)
        }
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}