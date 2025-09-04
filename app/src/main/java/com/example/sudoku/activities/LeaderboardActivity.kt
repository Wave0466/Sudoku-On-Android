package com.example.sudoku.activities

import android.os.Bundle
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
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // 显示返回箭头

        val recyclerView = findViewById<RecyclerView>(R.id.leaderboardRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val scores = LeaderboardManager.getScores(this)
        recyclerView.adapter = LeaderboardAdapter(scores)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}