package com.example.sudoku.activities

import android.os.Bundle
import android.os.SystemClock
import android.widget.Button
import android.widget.Chronometer
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.ProgressBar
import com.example.sudoku.R
import com.example.sudoku.ui.theme.SudokuBoardView
import com.example.sudoku.utils.LeaderboardManager
import com.example.sudoku.utils.Score
import com.example.sudoku.viewmodels.GameViewModel

class GameActivity : AppCompatActivity() {

    private val viewModel: GameViewModel by viewModels()
    private lateinit var sudokuBoardView: SudokuBoardView
    private lateinit var chronometer: Chronometer
    private lateinit var loadingProgressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "数独游戏"

        // 初始化视图
        sudokuBoardView = findViewById(R.id.sudokuBoardView)
        chronometer = findViewById(R.id.chronometer)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)

        setupObservers()
        setupListeners()

        val difficulty = intent.getIntExtra("DIFFICULTY", 1)
        viewModel.startGame(difficulty)

        chronometer.base = SystemClock.elapsedRealtime()
        chronometer.start()
    }

    // 重写这个方法来处理系统 ActionBar 上的返回箭头点击
    override fun onSupportNavigateUp(): Boolean {
        showExitConfirmationDialog()
        return true
    }

    // ... 其余所有方法 (showExitConfirmationDialog, setupObservers, etc.) 保持不变 ...
    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("返回主菜单")
            .setMessage("确定要退出当前游戏吗？您的进度将不会被保存。")
            .setPositiveButton("确定") { _, _ ->
                finish()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun setupObservers() {
        viewModel.sudokuBoard.observe(this) { board ->
            if (board != null) {
                sudokuBoardView.setBoard(board)
            }
        }
        viewModel.selectedCell.observe(this) { cell ->
            cell?.let {
                sudokuBoardView.setSelectedCell(it.first, it.second)
            }
        }
        viewModel.isGameWon.observe(this) { isWon ->
            if (isWon) {
                chronometer.stop()
                val timeElapsed = SystemClock.elapsedRealtime() - chronometer.base
                showWinDialog(timeElapsed)
            }
        }
        viewModel.isLoading.observe(this) { isLoading ->
            loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun setupListeners() {
        sudokuBoardView.setOnCellTouchListener { row, col ->
            viewModel.selectCell(row, col)
        }

        val numberButtons = listOf<Button>(
            findViewById(R.id.btn1), findViewById(R.id.btn2), findViewById(R.id.btn3),
            findViewById(R.id.btn4), findViewById(R.id.btn5), findViewById(R.id.btn6),
            findViewById(R.id.btn7), findViewById(R.id.btn8), findViewById(R.id.btn9)
        )
        numberButtons.forEachIndexed { index, button ->
            button.setOnClickListener { viewModel.setNumber(index + 1) }
        }
        findViewById<Button>(R.id.btnClear).setOnClickListener { viewModel.setNumber(0) }
    }

    private fun showWinDialog(timeInMillis: Long) {
        val editText = EditText(this)
        editText.hint = "输入你的名字"

        AlertDialog.Builder(this)
            .setTitle("恭喜！你赢了！")
            .setMessage("用时: ${timeInMillis / 1000} 秒")
            .setView(editText)
            .setPositiveButton("保存") { _, _ ->
                val name = editText.text.toString().ifEmpty { "Anonymous" }
                val difficulty = intent.getIntExtra("DIFFICULTY", 1)
                val newScore = Score(
                    playerName = name,
                    timeInMillis = timeInMillis,
                    difficulty = difficulty
                )
                LeaderboardManager.saveScore(this, newScore)
                finish()
            }
            .setNegativeButton("取消") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }
}