package com.example.sudoku.activities

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sudoku.R
import com.example.sudoku.adapter.LeaderboardAdapter
import com.example.sudoku.utils.LeaderboardManager

class LeaderboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        val difficulty = intent.getIntExtra("DIFFICULTY", 1)
        val title = intent.getStringExtra("TITLE") ?: "排行榜"

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = title

        val recyclerView = findViewById<RecyclerView>(R.id.leaderboardRecyclerView)
        val emptyTextView = findViewById<TextView>(R.id.emptyTextView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val scores = LeaderboardManager.getScores(this, difficulty)

        // ================== START: 关键逻辑 ==================
        if (scores.isEmpty()) {
            // 如果没有分数，隐藏列表，显示提示文字
            recyclerView.visibility = View.GONE
            emptyTextView.visibility = View.VISIBLE
        } else {
            // 如果有分数，显示列表，隐藏提示文字
            recyclerView.visibility = View.VISIBLE
            emptyTextView.visibility = View.GONE
            recyclerView.adapter = LeaderboardAdapter(scores)
        }
        // ==================  END: 关键逻辑  ==================
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}