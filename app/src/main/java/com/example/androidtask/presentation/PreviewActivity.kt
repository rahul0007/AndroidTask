package com.example.androidtask.presentation
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.androidtask.R
import com.example.androidtask.databinding.ActivityPreviewBinding
import com.example.androidtask.data.ChairModel
import com.example.androidtask.presentation.table.TableView
import com.example.androidtask.data.TableModel
import com.google.gson.Gson

class PreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolBar.ivBack.setOnClickListener {
            finish()
        }

        val images = listOf(
            R.drawable.ic_one,
            R.drawable.ic_two,
            R.drawable.ic_three,
            R.drawable.circle_user_solid_full
        )

        val tablesJson = intent.getStringExtra("tables")
        val chairsJson = intent.getStringExtra("chairs")

        val tables: List<TableModel> =
            Gson().fromJson(tablesJson, Array<TableModel>::class.java).toList()
        val chairs: List<ChairModel> =
            Gson().fromJson(chairsJson, Array<ChairModel>::class.java).toList()

        val usersPlaced = mutableMapOf<String, Int>()
        tables.forEach { usersPlaced[it.id] = 0 }

        binding.canvasPreview.post {
            val canvasWidth = binding.canvasPreview.width
            val canvasHeight = binding.canvasPreview.height

            tables.forEach { table ->
                val tableView = TableView(this)
                val widthPx = (table.widthRatio * canvasWidth).toInt()
                val heightPx = (table.heightRatio * canvasHeight).toInt()
                val lp = FrameLayout.LayoutParams(widthPx, heightPx)
                tableView.layoutParams = lp
                tableView.x = table.xRatio * canvasWidth
                tableView.y = table.yRatio * canvasHeight
                tableView.rotation = table.rotation
                tableView.bind(table, widthPx, heightPx)
                binding.canvasPreview.addView(tableView)
            }

            chairs.forEach { chair ->
                val chairView = ImageView(this)

                val parentTable = tables.find { it.id == chair.tableId }
                if (parentTable != null) {
                    val already = usersPlaced[parentTable.id] ?: 0
                    if (already < parentTable.seatedUsers) {
                        val randomIcon = images.random()
                        chairView.setImageResource(randomIcon)
                        usersPlaced[parentTable.id] = already + 1
                    } else {
                        chairView.setImageResource(R.drawable.chair_solid_full)
                    }
                } else {
                    chairView.setImageResource(R.drawable.chair_solid_full)
                }

                val lp = FrameLayout.LayoutParams(
                    chair.width.toInt(),
                    chair.height.toInt()
                )
                chairView.layoutParams = lp
                chairView.x = chair.x
                chairView.y = chair.y
                chairView.rotation = chair.rotation
                binding.canvasPreview.addView(chairView)
            }
        }
    }
}



