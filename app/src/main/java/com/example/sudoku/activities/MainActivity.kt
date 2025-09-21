package com.example.sudoku.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sudoku.R
import com.example.sudoku.utils.NetworkManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var matchingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 在 App 启动时，提前建立好网络连接
        NetworkManager.connect()
        // 开始监听来自服务器的全局消息
        observeServerMessages()

        findViewById<Button>(R.id.btnNewGame).setOnClickListener {
            showDifficultyDialog()
        }
        findViewById<Button>(R.id.btnApiGame).setOnClickListener {
            showApiDifficultyDialog()
        }
        findViewById<Button>(R.id.btnLeaderboard).setOnClickListener {
            showLeaderboardDialog()
        }
        findViewById<Button>(R.id.btnOnlineGame).setOnClickListener {
            startOnlineMatch()
        }
    }

    private fun startOnlineMatch() {
        matchingDialog = AlertDialog.Builder(this)
            .setTitle("正在匹配对手...")
            .setMessage("请稍候，我们正在为您寻找一位玩家。")
            .setCancelable(false)
            .setNegativeButton("取消") { dialog, _ ->
                // 当用户点击“取消”时，向服务器发送离开队列的指令
                NetworkManager.sendMessage("C_LEAVE_QUEUE")
                dialog.dismiss() // 关闭对话框
            }
            .show()

        // 通过 NetworkManager 向服务器发送“我要加入匹配队列”的指令
        NetworkManager.sendMessage("C_JOIN_QUEUE")
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
            // 解析指令，格式为 "指令:数据"
            val parts = message.split(":", limit = 2)
            val command = parts[0]
            val data = parts.getOrNull(1)

            when (command) {
                // 当服务器告诉我们匹配成功，游戏开始
                "S_GAME_START" -> {
                    matchingDialog?.dismiss()
                    if (data != null) {
                        val intent = Intent(this, GameActivity::class.java).apply {
                            putExtra("GAME_MODE", "ONLINE")
                            putExtra("PUZZLE_STRING", data)
                        }
                        startActivity(intent)
                    }
                }
                // 当连接服务器失败时
                "CONNECTION_FAILED" -> {
                    matchingDialog?.dismiss()
                    Toast.makeText(this, "连接对战服务器失败，请检查网络", Toast.LENGTH_LONG).show()
                }
                // 服务器确认你已成功离开队列
                "S_LEFT_QUEUE" -> {
                    Toast.makeText(this, "已取消匹配", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showApiDifficultyDialog() {
        val apiDifficulties = arrayOf("简单 (easy)", "普通 (normal)", "困难 (hard)", "非常困难 (very hard)")
        AlertDialog.Builder(this)
            .setTitle("选择网络难度")
            .setItems(apiDifficulties) { _, which ->
                startApiGame(which + 1)
            }
            .show()
    }

    private fun showDifficultyDialog() {
        val difficulties = arrayOf("简单", "中等", "困难")
        AlertDialog.Builder(this)
            .setTitle("选择难度")
            .setItems(difficulties) { _, which ->
                startGame(which + 1)
            }
            .show()
    }

    private fun startGame(difficulty: Int) {
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra("GAME_MODE", "LOCAL")
            putExtra("DIFFICULTY", difficulty)
        }
        startActivity(intent)
    }

    private fun startApiGame(difficulty: Int) {
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra("GAME_MODE", "API")
            putExtra("DIFFICULTY", difficulty)
        }
        startActivity(intent)
    }

    private fun showLeaderboardDialog() {
        val leaderboardOptions = arrayOf("简单榜", "中等榜", "困难榜")
        AlertDialog.Builder(this)
            .setTitle("选择排行榜")
            .setItems(leaderboardOptions) { _, which ->
                showLeaderboard(which + 1, leaderboardOptions[which])
            }
            .show()
    }

    private fun showLeaderboard(difficulty: Int, title: String) {
        val intent = Intent(this, LeaderboardActivity::class.java).apply {
            putExtra("DIFFICULTY", difficulty)
            putExtra("TITLE", title)
        }
        startActivity(intent)
    }
}