package com.example.sudoku.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

object NetworkManager {

    private const val SERVER_IP = "10.33.107.234"
    private const val SERVER_PORT = 54321

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null

    // 创建一个独立的协程作用域，用于在后台处理网络任务
    // 使用 SupervisorJob 确保一个子协程失败不会影响其他协程
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var messageListenerJob: Job? = null

    // 使用SharedFlow向UI层发送从服务器收到的消息
    // replay=1 可以在新的订阅者加入时，立即收到上一条消息
    private val _messageFlow = MutableSharedFlow<String>(replay = 1)
    val messageFlow = _messageFlow.asSharedFlow()

    fun connect() {
        if (socket?.isConnected == true) return // 防止重复连接

        coroutineScope.launch {
            try {
                socket = Socket(SERVER_IP, SERVER_PORT)
                writer = PrintWriter(socket!!.getOutputStream(), true)
                reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                println("网络管理器：成功连接到服务器！")

                // 连接成功后，立即开始监听消息
                listenForMessages()
            } catch (e: Exception) {
                println("网络管理器：连接失败 - ${e.message}")
                _messageFlow.tryEmit("CONNECTION_FAILED") // 使用 tryEmit 避免挂起
            }
        }
    }

    private fun listenForMessages() {
        // 在启动新的监听前，确保旧的已经取消
        messageListenerJob?.cancel()
        messageListenerJob = coroutineScope.launch {
            try {
                while (isActive) {
                    val message = reader?.readLine()
                    if (message != null) {
                        // 将收到的消息发射出去
                        println("收到消息: $message")
                        _messageFlow.emit(message)
                    } else {
                        break // readLine返回null，意味着连接已关闭
                    }
                }
            } catch (e: Exception) {
                println("网络管理器：消息监听中断 - ${e.message}")
            } finally {
                // 连接断开时发送一个明确的事件
                println("网络管理器：连接已断开。")
                _messageFlow.emit("DISCONNECTED")
            }
        }
    }

    fun sendMessage(message: String) {
        // 在单独的协程中发送消息，避免阻塞调用者
        coroutineScope.launch {
            if (writer != null) {
                writer?.println(message)
                println("发送消息: $message")
            } else {
                println("发送失败: writer 未初始化。")
            }
        }
    }

    fun disconnect() {
        // 取消所有协程并关闭socket
        coroutineScope.cancel()
        try {
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            socket = null
        }
    }
}