package com.example.androidtask.presentation.table
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import com.example.androidtask.R
import com.example.androidtask.data.TableModel

class TableView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var label: TextView? = null

    fun bind(table: TableModel, widthPx: Int, heightPx: Int) {
        removeAllViews()
        val layoutRes = if (table.capacity <= 3) R.layout.table_small_rounded else R.layout.table_large_pill
        val view = LayoutInflater.from(context).inflate(layoutRes, this, false)
        view.layoutParams = LayoutParams(widthPx, heightPx)
        addView(view)
        label = view.findViewById(R.id.tableLabel)
        label?.text = table.capacity.toString()
    }

    fun calculateHeightForCapacity(capacity: Int, widthPx: Int): Int {
        return when (capacity) {
            2,3 -> widthPx
            6 -> widthPx * 2
            8 -> (widthPx * 2.2).toInt()
            else -> widthPx * 2
        }
    }
}




