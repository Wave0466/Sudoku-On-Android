package com.example.sudoku.activities

import android.os.Bundle
import android.os.SystemClock
import android.widget.Button
import android.widget.Chronometer
import android.widget.EditText
import android.widget.ProgressBar
import android.view.View
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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

    // 新增一个成员变量，用于安全地保存游戏结束时的时间
    private var timeWhenStopped: Long = 0
    private var isTimerStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "数独游戏" // 默认标题

        sudokuBoardView = findViewById(R.id.sudokuBoardView)
        chronometer = findViewById(R.id.chronometer)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)

        val gameMode = intent.getStringExtra("GAME_MODE") ?: "LOCAL"
        val difficulty = intent.getIntExtra("DIFFICULTY", 1)

        if (gameMode == "API") {
            supportActionBar?.title = "网络数独"
        }

        viewModel.startGame(gameMode, difficulty)

        setupObservers()
        setupListeners()

        onBackPressedDispatcher.addCallback(this) {
            showExitConfirmationDialog()
        }

        isTimerStarted = false
    }

    private fun setupObservers() {
        viewModel.sudokuBoard.observe(this) { board ->
            board?.let {
                sudokuBoardView.setBoard(it)
                if (!isTimerStarted) {
                    chronometer.base = SystemClock.elapsedRealtime()
                    chronometer.start()
                    isTimerStarted = true
                }
            }
        }
        viewModel.selectedCell.observe(this) {
            it?.let { sudokuBoardView.setSelectedCell(it.first, it.second) }
        }
        viewModel.isGameWon.observe(this) { isWon ->
            if (isWon && !isFinishing) {
                // 停止计时器
                chronometer.stop()
                // 立刻将停止时的时间差保存在我们自己的变量里
                timeWhenStopped = SystemClock.elapsedRealtime() - chronometer.base

                // 调用 showWinDialog，不再传递任何参数
                showWinDialog()
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

        val numberButtons = mapOf(
            R.id.btn1 to 1, R.id.btn2 to 2, R.id.btn3 to 3, R.id.btn4 to 4, R.id.btn5 to 5,
            R.id.btn6 to 6, R.id.btn7 to 7, R.id.btn8 to 8, R.id.btn9 to 9, R.id.btnClear to 0
        )
        numberButtons.forEach { (buttonId, number) ->
            findViewById<Button>(buttonId)?.setOnClickListener {
                viewModel.selectedCell.value?.let {
                    viewModel.setNumber(number)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        showExitConfirmationDialog()
        return true
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("返回主菜单")
            .setMessage("确定要退出当前游戏吗？您的进度将不会被保存。")
            .setPositiveButton("确定") { _, _ -> finish() }
            .setNegativeButton("取消", null)
            .show()
    }

    // showWinDialog 的方法签名已修改，不再接收参数
    private fun showWinDialog() {
        val gameMode = intent.getStringExtra("GAME_MODE") ?: "LOCAL"

        // 只有本地游戏才显示保存分数的对话框
        if (gameMode == "LOCAL") {
            val editText = EditText(this)
            editText.hint = "输入你的名字"
            AlertDialog.Builder(this)
                .setTitle("恭喜！你赢了！")
                // 使用我们自己保存的 timeWhenStopped 变量
                .setMessage("用时: ${timeWhenStopped / 1000} 秒")
                .setView(editText)
                .setPositiveButton("保存") { _, _ ->
                    val name = editText.text.toString().ifEmpty { "Anonymous" }
                    val difficulty = intent.getIntExtra("DIFFICULTY", 1)
                    // 在创建 Score 对象时，也使用我们自己的变量
                    LeaderboardManager.saveScore(this, Score(name, timeWhenStopped, difficulty))
                    finish()
                }
                .setNegativeButton("取消") { _, _ -> finish() }
                .setCancelable(false)
                .show()
        } else {
            // 网络游戏只显示一个简单的祝贺，然后返回
            AlertDialog.Builder(this)
                .setTitle("恭喜！你赢了！")
                .setMessage("用时: ${timeWhenStopped / 1000} 秒")
                .setPositiveButton("返回主菜单") { _, _ -> finish() }
                .setCancelable(false)
                .show()
        }
    }
}