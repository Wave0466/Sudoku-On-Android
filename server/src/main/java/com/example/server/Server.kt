package com.example.server

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

// 使用 Player 类来封装 Socket 和发送通道
class Player(val socket: Socket) {
    val sendChannel = Channel<String>(Channel.UNLIMITED)
}

// 游戏会话，现在包含 Player 对象
data class GameSession(val player1: Player, val player2: Player)

// 服务器的主协程作用域
private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

// 游戏裁判，管理匹配和游戏会话
object GameArbiter {
    private val waitingPlayerMutex = Mutex()
    private var waitingPlayer: Player? = null
    // 使用 ConcurrentHashMap 来确保线程安全
    private val activeGames = ConcurrentHashMap<Player, GameSession>()

    suspend fun leaveQueue(player: Player): Boolean {
        var left = false
        waitingPlayerMutex.withLock {
            // 只有当等待的玩家是当前请求离开的玩家时，才执行操作
            if (waitingPlayer == player) {
                waitingPlayer = null
                println("玩家 [${player.socket.remoteAddress}] 已离开匹配队列。")
                left = true
            }
        }
        return left
    }

    suspend fun onPlayerConnected(player: Player) {
        waitingPlayerMutex.withLock {
            val opponent = waitingPlayer
            if (opponent == null) {
                waitingPlayer = player
                player.send("S_WAITING_FOR_OPPONENT")
                println("玩家 [${player.socket.remoteAddress}] 进入匹配队列。")
            } else {
                waitingPlayer = null
                println("匹配成功: [${opponent.socket.remoteAddress}] vs [${player.socket.remoteAddress}]")

                val session = GameSession(opponent, player)
                activeGames[opponent] = session
                activeGames[player] = session

                val puzzle = generateNewSudokuPuzzleAsString()
                session.broadcast("S_GAME_START:$puzzle")
            }
        }
    }

    fun onPlayerFinished(player: Player, time: String) {
        val session = activeGames.remove(player) ?: return

        val opponent = if (session.player1 == player) session.player2 else session.player1
        activeGames.remove(opponent)

        val winner = player
        val loser = opponent

        println("游戏结束: 赢家 [${winner.socket.remoteAddress}]，用时 $time 秒")

        // 立即向赢家和输家发送游戏结束指令
        winner.send("S_GAME_OVER:WIN:$time")
        loser.send("S_GAME_OVER:LOSE:$time")
    }

    fun onPlayerDisconnected(player: Player) {
        val session = activeGames[player]
        if (session != null) {
            val opponent = if (session.player1 == player) session.player2 else session.player1
            println("玩家 [${player.socket.remoteAddress}] 在游戏中掉线。")
            opponent.send("S_OPPONENT_LEFT")

            activeGames.remove(session.player1)
            activeGames.remove(session.player2)
        } else if (waitingPlayer == player) {
            waitingPlayer = null
            println("等待中的玩家 [${player.socket.remoteAddress}] 掉线。")
        } else {
            println("玩家 [${player.socket.remoteAddress}] 断开连接。")
        }
    }
}

// 将消息放入玩家的发送通道
fun Player.send(message: String) {
    serverScope.launch {
        if (!sendChannel.isClosedForSend) {
            sendChannel.send(message)
        }
    }
}

// 广播消息
fun GameSession.broadcast(message: String) {
    this.player1.send(message)
    this.player2.send(message)
}

// 这个协程专门负责从玩家的发送通道中取出消息并写入Socket
suspend fun handleOutgoingMessages(player: Player) {
    val writeChannel = player.socket.openWriteChannel(autoFlush = true)
    try {
        for (message in player.sendChannel) {
            writeChannel.writeStringUtf8("$message\n")
        }
    } catch (e: Exception) {
        // Handle exceptions
    }
}

// 这个协程专门负责从Socket读取消息并处理
suspend fun handleIncomingMessages(player: Player) {
    val readChannel = player.socket.openReadChannel()
    try {
        while (true) {
            val message = readChannel.readUTF8Line() ?: break
            val parts = message.split(":", limit = 2)
            val command = parts[0]
            val data = parts.getOrNull(1) ?: ""

            when (command) {
                "C_JOIN_QUEUE" -> {
                    GameArbiter.onPlayerConnected(player)
                }
                "C_FINISH_GAME" -> {
                    GameArbiter.onPlayerFinished(player, data)
                }
                "C_LEAVE_QUEUE" -> {
                    if (GameArbiter.leaveQueue(player)) {
                        player.send("S_LEFT_QUEUE")
                    }
                }
                "C_LEAVE_GAME" -> {
                    GameArbiter.onPlayerDisconnected(player)
                }
            }
        }
    } catch (e: Exception) {
        // Handle exceptions
    } finally {
        GameArbiter.onPlayerDisconnected(player)
        player.sendChannel.close()
        player.socket.close()
    }
}

fun main() = runBlocking {
    val selectorManager = SelectorManager(Dispatchers.IO)
    val serverSocket = aSocket(selectorManager).tcp().bind("0.0.0.0", 54321)
    println("数独对战服务器已启动: ${serverSocket.localAddress}")

    while (true) {
        val socket = serverSocket.accept()
        println("新玩家连接: ${socket.remoteAddress}")
        val player = Player(socket)

        // 为每个玩家启动两个独立的协程：一个读，一个写
        serverScope.launch { handleIncomingMessages(player) }
        serverScope.launch { handleOutgoingMessages(player) }
    }
}

suspend fun generateNewSudokuPuzzleAsString(): String {
    val generator = Generator()
    val boardArray = generator.generate(1) // Random.nextInt(1, 4)
    return boardArray.joinToString("") { row: IntArray -> row.joinToString("") }
}