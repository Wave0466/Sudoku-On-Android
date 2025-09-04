package com.example.sudoku.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sudoku.R
import com.example.sudoku.utils.Score
import java.util.concurrent.TimeUnit

class LeaderboardAdapter(private val scores: List<Score>) :
    RecyclerView.Adapter<LeaderboardAdapter.ScoreViewHolder>() {

    class ScoreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rankTextView: TextView = view.findViewById(R.id.rankTextView)
        val nameTextView: TextView = view.findViewById(R.id.nameTextView)
        val timeTextView: TextView = view.findViewById(R.id.timeTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_score, parent, false)
        return ScoreViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScoreViewHolder, position: Int) {
        val score = scores[position]
        holder.rankTextView.text = "${position + 1}."
        holder.nameTextView.text = score.playerName

        val minutes = TimeUnit.MILLISECONDS.toMinutes(score.timeInMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(score.timeInMillis) % 60
        holder.timeTextView.text = String.format("%02d:%02d", minutes, seconds)
    }

    override fun getItemCount() = scores.size
}
