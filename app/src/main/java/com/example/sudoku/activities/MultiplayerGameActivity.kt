package com.example.sudoku.activities

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.sudoku.R
import com.example.sudoku.game.Generator
import com.example.sudoku.model.GameAction
import com.example.sudoku.ui.theme.SudokuBoardView
import com.example.sudoku.utils.GameSocketManager
import com.example.sudoku.utils.NsdHelper
import com.example.sudoku.viewmodels.MultiplayerGameViewModel
import kotlinx.coroutines.*
import java.net.InetAddress

class MultiplayerGameActivity : AppCompatActivity() {

    private val TAG = "MultiplayerGame_Debug"
    private val viewModel: MultiplayerGameViewModel by viewModels()
    private lateinit var sudokuBoardView: SudokuBoardView
    private lateinit var tvGameStatus: TextView
    private lateinit var nsdHelper: NsdHelper

    private var isHost = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "LIFECYC-LE: onCreate - START")
        setContentView(R.layout.activity_multiplayer_game)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "联机对战"

        sudokuBoardView = findViewById(R.id.sudokuBoardView)
        tvGameStatus = findViewById(R.id.tvGameStatus)

        nsdHelper = NsdHelper(this)

        setupSocketObservers()
        setupViewModelObservers()
        setupUIListeners()

        isHost = intent.getBooleanExtra("IS_HOST", false)
        Log.d(TAG, "onCreate: 我的角色是 - ${if (isHost) "主机" else "客户端"}")

        if (isHost) {
            tvGameStatus.text = "正在初始化主机..."
        } else {
            tvGameStatus.text = "正在连接主机..."
        }
        Log.d(TAG, "LIFECYCLE: onCreate - END")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "LIFECYCLE: onResume - Activity 已可见")

        // 现在可以直接使用 GameSocketManager.isConnected
        if (GameSocketManager.isConnected) {
            handleConnection()
        } else {
            if (!isHost) {
                val hostAddress = intent.getStringExtra("HOST_ADDRESS")
                val port = intent.getIntExtra("PORT", 0)
                if (hostAddress != null) {
                    GameSocketManager.connectToServer(InetAddress.getByName(hostAddress), port)
                }
            } else {
                GameSocketManager.startServer()
            }
        }
    }

    private fun setupSocketObservers() {
        GameSocketManager.receivedAction.observe(this) { action ->
            Log.d(TAG, "OBSERVER: receivedAction - 收到 Action: $action")
            when (action) {
                is GameAction.FillCell -> viewModel.updateCell(action.row, action.col, action.number)
                is GameAction.SelectCell -> viewModel.selectCell(action.row, action.col)
                is GameAction.StartGame -> {
                    viewModel.initializeBoard(action.board)
                    tvGameStatus.text = "游戏开始！"
                }
            }
        }

        GameSocketManager.connectionState.observe(this) { state ->
            Log.d(TAG, "OBSERVER: connectionState - 状态变为: $state")
            if (state == GameSocketManager.ConnectionState.CONNECTED) {
                handleConnection()
            } else if (state == GameSocketManager.ConnectionState.DISCONNECTED) {
                if (!isFinishing) {
                    Log.e(TAG, "OBSERVER: connectionState - 检测到连接断开，即将调用 finish()!")
                    Toast.makeText(this, "连接已断开", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }

        GameSocketManager.serverState.observe(this) { state ->
            state?.let {
                if (isHost) {
                    tvGameStatus.text = "正在生成棋盘并广播房间..."
                    CoroutineScope(Dispatchers.IO).launch {
                        val generatedBoardArray = Generator().generate(1).flatMap { it.toList() }.toIntArray()
                        withContext(Dispatchers.Main) {
                            viewModel.initializeBoard(generatedBoardArray)
                            val serviceName = "数独房间-${(100..999).random()}"
                            nsdHelper.registerService(it.port, serviceName)
                            // 移除观察，防止重复触发
                            GameSocketManager.serverState.removeObservers(this@MultiplayerGameActivity)
                        }
                    }
                }
            }
        }
    }

    private fun handleConnection() {
        Toast.makeText(this, "连接成功!", Toast.LENGTH_SHORT).show()
        if (isHost) {
            tvGameStatus.text = "对手已连接！游戏开始"
            val boardArray = viewModel.sudokuBoard.value?.flatMap { row -> row.map { it.value } }?.toIntArray()
            if (boardArray != null) {
                GameSocketManager.sendAction(GameAction.StartGame(boardArray))
            }
        } else {
            tvGameStatus.text = "连接主机成功！等待棋盘..."
        }
    }

    private fun setupViewModelObservers() {
        viewModel.sudokuBoard.observe(this) { board ->
            Log.d(TAG, "Activity: LiveData 发生变化，正在刷新棋盘UI...")
            board?.let { sudokuBoardView.setBoard(it) }
        }
        viewModel.selectedCell.observe(this) {
            it?.let { sudokuBoardView.setSelectedCell(it.first, it.second) }
        }
    }

    private fun setupUIListeners() {
        // ... UI 监听器代码保持不变 ...
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "LIFECYCLE: onDestroy - Activity 正在销毁")
        if (isHost) {
            nsdHelper.tearDown()
        }
        // 不管是谁，离开游戏界面都应该断开连接
        GameSocketManager.closeAll()
    }

    override fun onSupportNavigateUp(): Boolean {
        showExitConfirmationDialog()
        return true
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("退出对战")
            .setMessage("确定要退出当前对战吗？")
            .setPositiveButton("确定") { _, _ -> finish() }
            .setNegativeButton("取消", null)
            .show()
    }
}