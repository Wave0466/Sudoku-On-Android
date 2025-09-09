package com.example.sudoku.utils

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.sudoku.model.GameAction
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

object GameSocketManager {

    private const val TAG = "GameSocketManager" // 常量使用大写是规范的
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(GameAction::class.java, GameActionAdapter())
        .create()

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var writer: PrintWriter? = null

    val receivedAction = MutableLiveData<GameAction>()
    val connectionState = MutableLiveData<ConnectionState>()
    val serverState = MutableLiveData<ServerState?>()

    private val isConnectedInternal = AtomicBoolean(false)
    // 对外暴露一个简单的 Boolean getter，隐藏 AtomicBoolean 的实现细节
    val isConnected: Boolean
        get() = isConnectedInternal.get()

    enum class ConnectionState { IDLE, CONNECTED, DISCONNECTED }
    data class ServerState(val port: Int)

    fun startServer() {
        if (!scope.isActive) { scope = CoroutineScope(Dispatchers.IO + SupervisorJob()) }
        scope.launch {
            try {
                serverSocket = ServerSocket(0)
                val actualPort = serverSocket!!.localPort
                Log.d(TAG, "服务器已在端口 $actualPort 启动...")
                serverState.postValue(ServerState(actualPort))
                val socket = serverSocket?.accept()
                handleConnection(socket)
            } catch (e: Exception) {
                if (scope.isActive) {
                    Log.e(TAG, "服务器启动失败", e)
                    closeAll()
                }
            }
        }
    }

    fun connectToServer(address: InetAddress, port: Int) {
        if (!scope.isActive) { scope = CoroutineScope(Dispatchers.IO + SupervisorJob()) }
        scope.launch {
            try {
                val socket = Socket(address, port)
                handleConnection(socket)
            } catch (e: Exception) {
                if (scope.isActive) {
                    Log.e(TAG, "连接服务器失败", e)
                    closeAll()
                }
            }
        }
    }

    private fun handleConnection(socket: Socket?) {
        clientSocket = socket
        if (clientSocket == null) { return }
        isConnectedInternal.set(true)
        Log.d(TAG, "连接已建立!")
        connectionState.postValue(ConnectionState.CONNECTED)

        writer = PrintWriter(clientSocket!!.getOutputStream(), true)

        scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(clientSocket!!.getInputStream()))
                while (isActive && isConnectedInternal.get()) {
                    val line = reader.readLine() ?: break
                    try {
                        val action = gson.fromJson(line, GameAction::class.java)
                        receivedAction.postValue(action)
                    } catch (e: Exception) {
                        Log.e(TAG, "JSON 解析失败", e)
                    }
                }
            } catch (e: Exception) {
                if (isConnectedInternal.get()) {
                    Log.e(TAG, "读取消息出错", e)
                }
            } finally {
                closeAll()
            }
        }
    }

    fun sendAction(action: GameAction) {
        scope.launch {
            if (isConnectedInternal.get()) {
                writer?.println(gson.toJson(action))
            }
        }
    }

    fun closeAll() {
        if (isConnectedInternal.compareAndSet(true, false)) {
            Log.d(TAG, "正在关闭所有连接...")
            connectionState.postValue(ConnectionState.DISCONNECTED)
            try {
                scope.cancel()
                writer?.close()
                clientSocket?.close()
                serverSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "关闭 Socket 时出错", e)
            } finally {
                writer = null
                clientSocket = null
                serverSocket = null
                serverState.postValue(null)
            }
        }
    }
}