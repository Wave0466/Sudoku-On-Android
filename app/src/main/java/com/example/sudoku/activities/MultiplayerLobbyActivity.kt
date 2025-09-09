package com.example.sudoku.activities

import android.content.Intent
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.sudoku.R
import com.example.sudoku.utils.GameSocketManager
import com.example.sudoku.utils.NsdHelper
import kotlinx.coroutines.*
import android.util.Log

class MultiplayerLobbyActivity : AppCompatActivity(), NsdHelper.NsdListener {

    private lateinit var tvStatus: TextView
    private lateinit var btnCreateGame: Button
    private lateinit var btnJoinGame: Button
    private lateinit var nsdHelper: NsdHelper

    private var isHost = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multiplayer_lobby)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "联机大厅"

        tvStatus = findViewById(R.id.tvStatus)
        btnCreateGame = findViewById(R.id.btnCreateGame)
        btnJoinGame = findViewById(R.id.btnJoinGame)

        nsdHelper = NsdHelper(this)
        nsdHelper.setNsdListener(this)

        // 监听来自 GameSocketManager 的全局 LiveData
        setupSocketObservers()

        btnCreateGame.setOnClickListener {
            isHost = true
            btnCreateGame.isEnabled = false
            btnJoinGame.isEnabled = false
            tvStatus.text = "正在初始化主机..."
            GameSocketManager.startServer()
        }

        btnJoinGame.setOnClickListener {
            isHost = false
            btnCreateGame.isEnabled = false
            btnJoinGame.isEnabled = false
            nsdHelper.discoverServices()
            tvStatus.text = "正在搜索房间..."
        }

        onBackPressedDispatcher.addCallback(this) {
            closeServicesAndFinish()
        }
    }

    private fun setupSocketObservers() {
        // 监听服务器启动事件 (只有主机会触发)
        GameSocketManager.serverState.observe(this) { state ->
            if (isHost && state != null) {
                val serviceName = "数独房间-${(100..999).random()}"
                nsdHelper.registerService(state.port, serviceName)
                // 移除 serverState 的观察，防止重复触发
                GameSocketManager.serverState.removeObservers(this)
            }
        }

        // 监听连接成功事件
        GameSocketManager.connectionState.observe(this) { state ->
            if (state == GameSocketManager.ConnectionState.CONNECTED) {
                if (!isFinishing) {
                    Toast.makeText(this, "连接成功！准备在3秒后进入游戏...", Toast.LENGTH_LONG).show()
                    Log.d("Lobby_Debug", "连接成功！准备在3秒后进入游戏...")

                    MainScope().launch {
                        delay(3000) // 强制暂停 3 秒

                        Log.d("Lobby_Debug", "延迟结束，正式启动 MultiplayerGameActivity！")

                        val intent = Intent(this@MultiplayerLobbyActivity, MultiplayerGameActivity::class.java).apply {
                            putExtra("IS_HOST", isHost)
                        }
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        closeServicesAndFinish()
        return true
    }

    private fun closeServicesAndFinish() {
        if (isHost) {
            nsdHelper.tearDown()
            GameSocketManager.closeAll()
            Toast.makeText(this, "房间已关闭", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        nsdHelper.tearDown()
        GameSocketManager.closeAll()
    }

    // --- NsdListener Callbacks ---

    override fun onServiceDiscovered(service: NsdServiceInfo) {
        runOnUiThread {
            if (!isHost) tvStatus.text = "发现房间: ${service.serviceName}, 正在获取地址..."
        }
    }

    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
        runOnUiThread {
            if (!isHost) {
                tvStatus.text = "已找到房间，正在连接..."
                serviceInfo.host?.let {
                    GameSocketManager.connectToServer(it, serviceInfo.port)
                }
                nsdHelper.stopDiscovery()
            }
        }
    }

    override fun onServiceLost(service: NsdServiceInfo) {
        runOnUiThread {
            if (!isHost) {
                tvStatus.text = "房间 '${service.serviceName}' 已消失, 请重新搜索"
                btnJoinGame.isEnabled = true
                btnCreateGame.isEnabled = true
            }
        }
    }

    override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
        runOnUiThread {
            if (isHost) {
                tvStatus.text = "房间创建成功，等待对手加入..."
            }
        }
    }

    override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
        runOnUiThread {
            if (isHost) {
                tvStatus.text = "广播房间失败，请检查网络权限"
                btnCreateGame.isEnabled = true
                btnJoinGame.isEnabled = true
            }
        }
    }

    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
        runOnUiThread {
            if (!isHost) {
                tvStatus.text = "获取房间地址失败, 请重试"
                btnJoinGame.isEnabled = true
                btnCreateGame.isEnabled = true
            }
        }
    }
}