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

        if (scores.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyTextView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyTextView.visibility = View.GONE
            recyclerView.adapter = LeaderboardAdapter(scores)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}