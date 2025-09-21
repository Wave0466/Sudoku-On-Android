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
import androidx.lifecycle.lifecycleScope
import com.example.sudoku.R
import com.example.sudoku.ui.theme.SudokuBoardView
import com.example.sudoku.utils.LeaderboardManager
import com.example.sudoku.utils.NetworkManager
import com.example.sudoku.utils.Score
import com.example.sudoku.viewmodels.GameViewModel
import kotlinx.coroutines.launch

class GameActivity : AppCompatActivity() {

    private val viewModel: GameViewModel by viewModels()
    private lateinit var sudokuBoardView: SudokuBoardView
    private lateinit var chronometer: Chronometer
    private lateinit var loadingProgressBar: ProgressBar

    private var gameMode: String = "LOCAL"
    private var timeWhenStopped: Long = 0
    private var isTimerStarted = false

    private var isResultSent = false      // 标记 “我是否已发送了完成请求”
    private var isGameFinished = false    // 标记 “服务器是否已发来最终结果”

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sudokuBoardView = findViewById(R.id.sudokuBoardView)
        chronometer = findViewById(R.id.chronometer)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)

        gameMode = intent.getStringExtra("GAME_MODE") ?: "LOCAL"
        val difficulty = intent.getIntExtra("DIFFICULTY", 1)
        val puzzleString = intent.getStringExtra("PUZZLE_STRING")

        when (gameMode) {
            "ONLINE" -> {
                supportActionBar?.title = "在线对战"
                observeServerMessages()
                if (puzzleString != null) {
                    viewModel.startGameWithOnlinePuzzle(puzzleString)
                }
            }
            "API" -> {
                supportActionBar?.title = "网络数独"
                viewModel.startGame(gameMode, difficulty)
            }
            else -> { // LOCAL
                supportActionBar?.title = "数独游戏"
                viewModel.startGame(gameMode, difficulty)
            }
        }

        setupObservers()
        setupListeners()

        onBackPressedDispatcher.addCallback(this) { showExitConfirmationDialog() }
    }

    private fun observeServerMessages() {
        lifecycleScope.launch {
            NetworkManager.messageFlow.collect { message ->
                handleServerMessage(message)
            }
        }
    }

    private fun handleServerMessage(message: String) {
        runOnUiThread {
            if (isGameFinished || isFinishing) return@runOnUiThread

            val parts = message.split(":", limit = 3)
            val command = parts[0]

            when (command) {
                "S_GAME_OVER" -> {
                    isGameFinished = true // 标记游戏彻底结束
                    chronometer.stop()

                    val result = parts.getOrNull(1)
                    val time = parts.getOrNull(2)

                    val title = if (result == "WIN") "恭喜，你赢了！" else "很遗憾，你输了..."
                    val finalMessage = "胜利者用时: $time 秒"

                    AlertDialog.Builder(this)
                        .setTitle(title)
                        .setMessage(finalMessage)
                        .setPositiveButton("返回主菜单") { _, _ -> finish() }
                        .setCancelable(false)
                        .show()
                }
                "S_OPPONENT_LEFT" -> {
                    isGameFinished = true // 标记游戏彻底结束
                    chronometer.stop()

                    AlertDialog.Builder(this)
                        .setTitle("对手已断开连接")
                        .setMessage("游戏已结束。")
                        .setPositiveButton("返回主菜单") { _, _ -> finish() }
                        .setCancelable(false)
                        .show()
                }
            }
        }
    }

    private fun showWinDialog() {
        val timeInSeconds = timeWhenStopped / 1000

        when (gameMode) {
            "ONLINE" -> {
                // 使用 isResultSent 标志位来防止重复发送
                if (isResultSent) return
                isResultSent = true // 立刻标记为已发送，确保只发送一次

                // 在线模式下，只发送消息，不显示任何本地弹窗
                NetworkManager.sendMessage("C_FINISH_GAME:$timeInSeconds")
            }
            "LOCAL" -> {
                if (isGameFinished) return
                isGameFinished = true
                val editText = EditText(this)
                editText.hint = "输入你的名字"
                AlertDialog.Builder(this)
                    .setTitle("恭喜！你赢了！")
                    .setMessage("用时: $timeInSeconds 秒")
                    .setView(editText)
                    .setPositiveButton("保存") { _, _ ->
                        val name = editText.text.toString().ifEmpty { "Anonymous" }
                        val difficulty = intent.getIntExtra("DIFFICULTY", 1)
                        LeaderboardManager.saveScore(this, Score(name, timeWhenStopped, difficulty))
                        finish()
                    }
                    .setNegativeButton("取消") { _, _ -> finish() }
                    .setCancelable(false)
                    .show()
            }
            else -> { // API Mode
                if (isGameFinished) return
                isGameFinished = true
                AlertDialog.Builder(this)
                    .setTitle("恭喜！你赢了！")
                    .setMessage("用时: $timeInSeconds 秒")
                    .setPositiveButton("返回主菜单") { _, _ -> finish() }
                    .setCancelable(false)
                    .show()
            }
        }
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
                chronometer.stop()
                timeWhenStopped = SystemClock.elapsedRealtime() - chronometer.base
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
            .setPositiveButton("确定") { _, _ ->
                // 在退出前，检查是否为在线对战模式
                if (gameMode == "ONLINE") {
                    // 如果是，主动向服务器发送离开游戏的指令
                    NetworkManager.sendMessage("C_LEAVE_GAME")
                }
                // 然后再关闭 Activity
                finish()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}