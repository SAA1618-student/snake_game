package com.example.snake

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.random.Random

class GameView(context: Context) : View(context) {

    private val cols = 20
    private val rows = 28
    private var cell = 0f

    private val snake = ArrayDeque<Pair<Int, Int>>() // head at first
    private var dir = Dir.RIGHT
    private var pendingDir = Dir.RIGHT
    private var food =  Pair(5, 5)
    private var score = 0
    private var gameOver = false

    private val bgPaint = Paint().apply { isAntiAlias = true }
    private val snakePaint = Paint().apply { isAntiAlias = true }
    private val headPaint = Paint().apply { isAntiAlias = true }
    private val foodPaint = Paint().apply { isAntiAlias = true }
    private val textPaint = Paint().apply { isAntiAlias = true; textSize = 48f }

    private var lastX = 0f
    private var lastY = 0f

    private val handler = Handler(Looper.getMainLooper())
    private val tickMs = 120L
    private val tick = object : Runnable {
        override fun run() {
            if (!gameOver) {
                step()
                invalidate()
                handler.postDelayed(this, tickMs)
            }
        }
    }

    init {
        reset()
        handler.postDelayed(tick, tickMs)
        isClickable = true
    }

    private fun reset() {
        snake.clear()
        val startX = cols / 2
        val startY = rows / 2
        snake.addFirst(startX to startY)
        snake.addLast((startX - 1) to startY)
        snake.addLast((startX - 2) to startY)
        dir = Dir.RIGHT
        pendingDir = Dir.RIGHT
        score = 0
        gameOver = false
        spawnFood()
    }

    private fun spawnFood() {
        while (true) {
            val x = Random.nextInt(cols)
            val y = Random.nextInt(rows)
            if (snake.none { it.first == x && it.second == y }) {
                food = x to y
                return
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val cw = w / cols.toFloat()
        val ch = h / rows.toFloat()
        cell = minOf(cw, ch)

        // Colors (use default paints without specifying exact colors elsewhere)
        bgPaint.setARGB(255, 18, 18, 18)
        snakePaint.setARGB(255, 100, 220, 120)
        headPaint.setARGB(255, 80, 180, 255)
        foodPaint.setARGB(255, 255, 100, 100)
        textPaint.setARGB(255, 240, 240, 240)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // draw food
        drawCell(canvas, food.first, food.second, foodPaint)

        // draw snake
        snake.forEachIndexed { idx, (x, y) ->
            drawCell(canvas, x, y, if (idx == 0) headPaint else snakePaint)
        }

        canvas.drawText("Score: $score", 16f, 56f, textPaint)

        if (gameOver) {
            val msg = "Game Over! Tap to restart"
            val tw = textPaint.measureText(msg)
            canvas.drawText(msg, (width - tw) / 2f, height / 2f, textPaint)
        }
    }

    private fun drawCell(canvas: Canvas, gx: Int, gy: Int, p: Paint) {
        val left = gx * cell
        val top = gy * cell
        val rect = RectF(left + 2, top + 2, left + cell - 2, top + cell - 2)
        canvas.drawRoundRect(rect, cell * 0.2f, cell * 0.2f, p)
    }

    private fun step() {
        // apply queued direction if not reversing
        if (!isOpposite(dir, pendingDir)) dir = pendingDir

        val head = snake.first()
        var nx = head.first
        var ny = head.second
        when (dir) {
            Dir.UP -> ny -= 1
            Dir.DOWN -> ny += 1
            Dir.LEFT -> nx -= 1
            Dir.RIGHT -> nx += 1
        }

        // wall collision
        if (nx !in 0 until cols || ny !in 0 until rows) {
            gameOver = true
            return
        }

        // self collision
        if (snake.any { it.first == nx && it.second == ny }) {
            gameOver = true
            return
        }

        snake.addFirst(nx to ny)

        if (nx == food.first && ny == food.second) {
            score += 1
            spawnFood()
        } else {
            snake.removeLast()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gameOver && event.action == MotionEvent.ACTION_DOWN) {
            reset()
            handler.postDelayed(tick, tickMs)
            invalidate()
            return true
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - lastX
                val dy = event.y - lastY
                if (abs(dx) > abs(dy)) {
                    pendingDir = if (dx > 0) Dir.RIGHT else Dir.LEFT
                } else {
                    pendingDir = if (dy > 0) Dir.DOWN else Dir.UP
                }
            }
        }
        return true
    }

    private fun isOpposite(a: Dir, b: Dir): Boolean {
        return (a == Dir.UP && b == Dir.DOWN) ||
               (a == Dir.DOWN && b == Dir.UP) ||
               (a == Dir.LEFT && b == Dir.RIGHT) ||
               (a == Dir.RIGHT && b == Dir.LEFT)
    }

    enum class Dir { UP, DOWN, LEFT, RIGHT }
}