package com.example.sudoku.ui.theme

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.sudoku.model.SudokuCell
import kotlin.math.min

class SudokuBoardView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var board: Array<Array<SudokuCell>>? = null
    private var selectedRow = -1
    private var selectedCol = -1
    private var cellSize = 0f
    private var onCellTouchListener: ((Int, Int) -> Unit)? = null

    // --- Paint Objects ---
    private val thickLinePaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 4f
    }
    private val thinLinePaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 2f
    }
    private val selectedCellPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = Color.parseColor("#66b2eb") // Light blue
    }
    private val relatedCellPaint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = Color.parseColor("#b0c4de") // Lighter blue
    }
    private val startingNumberPaint = Paint().apply {
        color = Color.BLACK
        textSize = 64f
        textAlign = Paint.Align.CENTER
    }
    private val userNumberPaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 64f
        textAlign = Paint.Align.CENTER
    }

    fun setBoard(board: Array<Array<SudokuCell>>) {
        this.board = board
        invalidate()
    }

    fun setSelectedCell(row: Int, col: Int) {
        selectedRow = row
        selectedCol = col
        invalidate()
    }

    fun setOnCellTouchListener(listener: (Int, Int) -> Unit) {
        this.onCellTouchListener = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = min(measuredWidth, measuredHeight)
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cellSize = (w / 9f)
        startingNumberPaint.textSize = cellSize / 1.5f
        userNumberPaint.textSize = cellSize / 1.5f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBackground(canvas)
        drawGrid(canvas)
        drawNumbers(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        if (selectedRow == -1 || selectedCol == -1) return

        // Highlight selected cell
        canvas.drawRect(selectedCol * cellSize, selectedRow * cellSize,
            (selectedCol + 1) * cellSize, (selectedRow + 1) * cellSize, selectedCellPaint)

        // Highlight related row, col, and box
        for (i in 0..8) {
            canvas.drawRect(i * cellSize, selectedRow * cellSize, (i + 1) * cellSize, (selectedRow + 1) * cellSize, relatedCellPaint)
            canvas.drawRect(selectedCol * cellSize, i * cellSize, (selectedCol + 1) * cellSize, (i + 1) * cellSize, relatedCellPaint)
        }
        val startRow = selectedRow / 3 * 3
        val startCol = selectedCol / 3 * 3
        for (r in startRow until startRow + 3) {
            for (c in startCol until startCol + 3) {
                canvas.drawRect(c * cellSize, r * cellSize, (c + 1) * cellSize, (r + 1) * cellSize, relatedCellPaint)
            }
        }
    }

    private fun drawGrid(canvas: Canvas) {
        for (i in 0..9) {
            val paint = if (i % 3 == 0) thickLinePaint else thinLinePaint
            canvas.drawLine(i * cellSize, 0f, i * cellSize, height.toFloat(), paint)
            canvas.drawLine(0f, i * cellSize, width.toFloat(), i * cellSize, paint)
        }
    }

    private fun drawNumbers(canvas: Canvas) {
        board?.forEach { row ->
            row.forEach { cell ->
                if (cell.value != 0) {
                    val text = cell.value.toString()
                    val paint = if (cell.isStartingCell) startingNumberPaint else userNumberPaint
                    val textBounds = Rect()
                    paint.getTextBounds(text, 0, text.length, textBounds)
                    val textHeight = textBounds.height()

                    canvas.drawText(
                        text,
                        (cell.col * cellSize) + cellSize / 2,
                        (cell.row * cellSize) + cellSize / 2 + textHeight / 2,
                        paint
                    )
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val row = (event.y / cellSize).toInt()
                val col = (event.x / cellSize).toInt()
                if (row in 0..8 && col in 0..8) {
                    onCellTouchListener?.invoke(row, col)
                }
                true
            }
            else -> false
        }
    }
}