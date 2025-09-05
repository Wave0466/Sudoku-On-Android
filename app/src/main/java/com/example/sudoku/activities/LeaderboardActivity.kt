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

        // 直接使用系统主题提供的 ActionBar，不涉及任何 Toolbar 控件
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "排行榜"

        val recyclerView = findViewById<RecyclerView>(R.id.leaderboardRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 默认显示“简单”难度的排行榜 (difficulty = 1)
        val scores = LeaderboardManager.getScores(this, 1)
        recyclerView.adapter = LeaderboardAdapter(scores)
    }

    // 处理系统 ActionBar 上的返回箭头点击
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}