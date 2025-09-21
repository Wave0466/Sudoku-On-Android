package com.example.sudoku.ui.theme

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.sudoku.R
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
        color = ContextCompat.getColor(context, R.color.black)
        strokeWidth = 4f
    }
    private val thinLinePaint = Paint().apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.black)
        strokeWidth = 2f
    }

    private val boxBackgroundPaint = Paint().apply { style = Paint.Style.FILL; color = ContextCompat.getColor(context, R.color.sudoku_box_background) }
    private val selectedCellPaint = Paint().apply { style = Paint.Style.FILL; color = ContextCompat.getColor(context, R.color.sudoku_cell_selected_bg) }
    private val relatedCellPaint = Paint().apply { style = Paint.Style.FILL; color = ContextCompat.getColor(context, R.color.sudoku_cell_related_bg) }
    private val highlightedCellPaint = Paint().apply { style = Paint.Style.FILL; color = ContextCompat.getColor(context, R.color.sudoku_cell_highlighted_bg) }
    private val conflictBackgroundPaint = Paint().apply { style = Paint.Style.FILL; color = ContextCompat.getColor(context, R.color.sudoku_conflict_bg) }

    private val startingNumberPaint = Paint().apply { color = ContextCompat.getColor(context, R.color.black); textAlign = Paint.Align.CENTER }
    private val userNumberPaint = Paint().apply { color = ContextCompat.getColor(context, R.color.sudoku_user_text); textAlign = Paint.Align.CENTER }
    private val conflictUserNumberPaint = Paint().apply { color = ContextCompat.getColor(context, R.color.sudoku_conflict_user_text); textAlign = Paint.Align.CENTER }


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
        val numberTextSize = cellSize / 1.5f
        startingNumberPaint.textSize = numberTextSize
        userNumberPaint.textSize = numberTextSize
        conflictUserNumberPaint.textSize = numberTextSize
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBoxBackgrounds(canvas)

        board?.let {
            drawCellBackgrounds(canvas, it)
            drawGrid(canvas)
            drawNumbers(canvas, it)
        }
    }

    private fun drawBoxBackgrounds(canvas: Canvas) {
        for (r in 0..8) {
            for (c in 0..8) {
                if ((r / 3 + c / 3) % 2 == 0) {
                    canvas.drawRect(c * cellSize, r * cellSize, (c + 1) * cellSize, (r + 1) * cellSize, boxBackgroundPaint)
                }
            }
        }
    }

    private fun drawCellBackgrounds(canvas: Canvas, board: Array<Array<SudokuCell>>) {
        for (r in 0..8) {
            for (c in 0..8) {
                val cell = board[r][c]

                val isRelated = (selectedRow != -1 && (r == selectedRow || c == selectedCol || (r/3 == selectedRow/3 && c/3 == selectedCol/3)))

                if (isRelated) {
                    canvas.drawRect(c * cellSize, r * cellSize, (c + 1) * cellSize, (r + 1) * cellSize, relatedCellPaint)
                }

                if (cell.isHighlighted) {
                    canvas.drawRect(c * cellSize, r * cellSize, (c + 1) * cellSize, (r + 1) * cellSize, highlightedCellPaint)
                }

                if (cell.isConflicting) {
                    canvas.drawRect(c * cellSize, r * cellSize, (c + 1) * cellSize, (r + 1) * cellSize, conflictBackgroundPaint)
                }

                if (r == selectedRow && c == selectedCol) {
                    canvas.drawRect(c * cellSize, r * cellSize, (c + 1) * cellSize, (r + 1) * cellSize, selectedCellPaint)
                }
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

    private fun drawNumbers(canvas: Canvas, board: Array<Array<SudokuCell>>) {
        val textBounds = Rect()
        for (r in 0..8) {
            for (c in 0..8) {
                val cell = board[r][c]
                if (cell.value == 0) continue

                val text = cell.value.toString()

                val paint = when {
                    cell.isConflicting && !cell.isStartingCell -> conflictUserNumberPaint
                    cell.isStartingCell -> startingNumberPaint
                    else -> userNumberPaint
                }

                paint.getTextBounds(text, 0, text.length, textBounds)
                val textHeight = textBounds.height()
                canvas.drawText(text, (c * cellSize) + cellSize / 2, (r * cellSize) + cellSize / 2 + textHeight / 2, paint)
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