package com.example.androidtask.presentation

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.androidtask.R
import com.example.androidtask.databinding.ActivityMainBinding
import com.example.androidtask.data.local.ChairModel
import com.example.androidtask.data.local.LayoutData
import com.example.androidtask.data.local.TableModel
import com.example.androidtask.presentation.table.TableView
import com.google.gson.Gson
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val tables = mutableListOf<TableModel>()
    private var chairCounter = 0
    private val chairStack = mutableListOf<ImageView>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Default tables
        if (LayoutData.tables.isEmpty()) {
            LayoutData.tables.add(TableModel("T1", 0.1f, 0.2f, 0.18f, 0f, 2, seatedUsers = 1))
            LayoutData.tables.add(TableModel("T2", 0.2f, 0.4f, 0.18f, 0f, 2, seatedUsers = 2))
            LayoutData.tables.add(TableModel("T3", 0.5f, 0.5f, 0.18f, 0f, 6, seatedUsers = 5))
            LayoutData.tables.add(TableModel("T4", 0.3f, 0.75f, 0.18f, 0f, 8, seatedUsers = 6))
        }
        tables.clear()
        tables.addAll(LayoutData.tables)

        binding.imageChair.setOnClickListener { addChair() }
        binding.imageViewUndo.setOnClickListener { undoLastChair() }
        binding.tvPreview.setOnClickListener { openPreview() }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) loadTables()
    }

    private fun loadTables() {
        binding.canvasContainer.removeAllViews()
        tables.forEach { table ->
            val tableView = TableView(this)
            val widthPx = (table.widthRatio * binding.canvasContainer.width).toInt()
            val heightPx = tableView.calculateHeightForCapacity(table.capacity, widthPx)
            tableView.layoutParams = FrameLayout.LayoutParams(widthPx, heightPx)
            tableView.x = table.xRatio * binding.canvasContainer.width
            tableView.y = table.yRatio * binding.canvasContainer.height
            tableView.rotation = table.rotation
            tableView.tag = table.id
            tableView.bind(table, widthPx, heightPx)
            tableView.setOnTouchListener(TableTouchListener(tableView))
            binding.canvasContainer.addView(tableView)
        }

        // Reload chairs
        LayoutData.chairs.forEach { chair ->
            addChairView(
                x = chair.x,
                y = chair.y,
                width = chair.width.toInt(),
                height = chair.height.toInt(),
                rotation = chair.rotation,
                tableId = chair.tableId
            )
        }
    }

    private inner class TableTouchListener(private val view: FrameLayout) : View.OnTouchListener {
        private var dX = 0f
        private var dY = 0f
        private var initialRotation = 0f
        private var initialAngle = 0f

        private fun angleBetweenPoints(x1: Float, y1: Float, x2: Float, y2: Float): Float {
            return Math.toDegrees(Math.atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())).toFloat()
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = v.x - event.rawX
                    dY = v.y - event.rawY
                    initialRotation = v.rotation
                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (event.pointerCount >= 2) {
                        initialAngle = angleBetweenPoints(
                            event.getX(0), event.getY(0),
                            event.getX(1), event.getY(1)
                        ) - v.rotation
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount == 1) {
                        v.x = max(0f, event.rawX + dX)
                        v.y = max(0f, event.rawY + dY)
                    } else if (event.pointerCount >= 2) {
                        val currentAngle = angleBetweenPoints(
                            event.getX(0), event.getY(0),
                            event.getX(1), event.getY(1)
                        )
                        v.rotation = currentAngle - initialAngle
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    saveTablePosition(view)
                }
            }
            return true
        }
    }

    private fun saveTablePosition(view: FrameLayout) {
        val id = view.tag as String
        val table = tables.first { it.id == id }
        table.xRatio = view.x / binding.canvasContainer.width
        table.yRatio = view.y / binding.canvasContainer.height
        table.widthRatio = view.width.toFloat() / binding.canvasContainer.width
        table.heightRatio = view.height.toFloat() / binding.canvasContainer.height
        table.rotation = view.rotation
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addChairView(
        x: Float,
        y: Float,
        width: Int,
        height: Int,
        rotation: Float,
        tableId: String?
    ) {
        val chairView = ImageView(this)
        chairView.setImageResource(R.drawable.chair_solid_full)
        chairView.layoutParams = FrameLayout.LayoutParams(width, height)
        chairView.x = x
        chairView.y = y
        chairView.rotation = rotation
        chairView.tag = "chair_${chairCounter++}"
        chairView.setTag(R.id.tag_table_id, tableId)

        var dX = 0f
        var dY = 0f
        var rotationAngle = rotation
        var lastTap = 0L

        chairView.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = v.x - e.rawX
                    dY = v.y - e.rawY
                    val now = System.currentTimeMillis()
                    if (now - lastTap < 300) {
                        rotationAngle = (rotationAngle + 90f) % 360
                        v.rotation = rotationAngle
                    }
                    lastTap = now
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    v.x = max(0f, e.rawX + dX)
                    v.y = max(0f, e.rawY + dY)
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val tId = findNearbyTableForChair(v.x, v.y, v.width, v.height)
                    v.setTag(R.id.tag_table_id, tId)
                    true
                }

                else -> false
            }
        }

        binding.canvasContainer.addView(chairView)
        chairStack.add(chairView)
    }

    private fun addChair() {
        val size = (0.08f * binding.canvasContainer.width).toInt()
        addChairView(100f, 100f, size, size, 0f, null)
    }

    private fun undoLastChair() {
        if (chairStack.isNotEmpty()) {
            val lastChair = chairStack.removeAt(chairStack.size - 1)
            binding.canvasContainer.removeView(lastChair)
        }
    }

    private fun captureLayout(): Pair<List<TableModel>, List<ChairModel>> {
        val chairsList = mutableListOf<ChairModel>()
        for (i in 0 until binding.canvasContainer.childCount) {
            val child = binding.canvasContainer.getChildAt(i)
            if (child is ImageView && child.tag.toString().startsWith("chair")) {
                val tableId = (child.getTag(R.id.tag_table_id) as? String)
                    ?: findNearbyTableForChair(child.x, child.y, child.width, child.height)

                chairsList.add(
                    ChairModel(
                        x = child.x,
                        y = child.y,
                        width = child.width.toFloat(),
                        height = child.height.toFloat(),
                        rotation = child.rotation,
                        tableId = tableId ?: ""
                    )
                )
            } else if (child is TableView) {
                saveTablePosition(child)
            }
        }

        LayoutData.tables.clear()
        LayoutData.tables.addAll(tables)
        LayoutData.chairs.clear()
        LayoutData.chairs.addAll(chairsList)

        return tables to chairsList
    }

    private fun openPreview() {
        val (tablesData, chairsData) = captureLayout()
        val intent = Intent(this, PreviewActivity::class.java)
        intent.putExtra("tables", Gson().toJson(tablesData))
        intent.putExtra("chairs", Gson().toJson(chairsData))
        startActivity(intent)
    }

    private fun findNearbyTableForChair(
        x: Float,
        y: Float,
        w: Int,
        h: Int,
        range: Float = 200f
    ): String? {
        var nearestTableId: String? = null
        var minDistance = Float.MAX_VALUE

        tables.forEach { t ->
            val tx = t.xRatio * binding.canvasContainer.width
            val ty = t.yRatio * binding.canvasContainer.height
            val tw = t.widthRatio * binding.canvasContainer.width
            val th = t.heightRatio * binding.canvasContainer.height

            val tableCenterX = tx + tw / 2
            val tableCenterY = ty + th / 2
            val chairCenterX = x + w / 2
            val chairCenterY = y + h / 2

            val distance = Math.hypot(
                (chairCenterX - tableCenterX).toDouble(),
                (chairCenterY - tableCenterY).toDouble()
            ).toFloat()

            if (distance < minDistance && distance <= range) {
                minDistance = distance
                nearestTableId = t.id
            }
        }

        return nearestTableId
    }
}


