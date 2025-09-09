package com.example.sudoku.model

/**
 * 这是一个密封类 (sealed class)，用于表示所有可能的游戏操作。
 * 这样做的好处是，我们可以限制操作的种类，使代码更安全。
 */
sealed class GameAction {
    /**
     *  当一个玩家填入或清除一个数字时，发送这个操作。
     *  @param row 格子的行号 (0-8)
     *  @param col 格子的列号 (0-8)
     *  @param number 填入的数字 (1-9)，如果是 0，则表示清除
     */
    data class FillCell(val row: Int, val col: Int, val number: Int) : GameAction()

    /**
     *  当一个玩家选中一个格子时，发送这个操作。
     *  （可选功能，可以用来让对手看到你的光标位置）
     *  @param row 选中的行号 (0-8)
     *  @param col 选中的列号 (0-8)
     */
    data class SelectCell(val row: Int, val col: Int) : GameAction()

    /**
     *  游戏开始时，主机向客户端发送初始棋盘。
     *  @param board 一个包含81个数字的数组，代表初始谜题
     */
    data class StartGame(val board: IntArray) : GameAction()

    /**
     *  可以根据需要添加更多操作，例如：
     *  - data class ChatMessage(val message: String) : GameAction()
     *  - object PauseGame : GameAction()
     */
}