package com.example.androidtask.data.local
data class TableModel(
    val id: String,
    var xRatio: Float,
    var yRatio: Float,
    var widthRatio: Float,
    var heightRatio: Float,
    var capacity: Int,
    var seatedUsers: Int = 0,
    var rotation: Float = 0f
)
