package com.example.markdownparcer.domain.model

import android.graphics.Bitmap

sealed class MarkDownElement {
    data class Header(val level: Int, val text: String) : MarkDownElement()
    data class Paragraph(val textPart: List<TextPart>) : MarkDownElement()
    data class Table(val table: List<TableElement>) : MarkDownElement()
    data class Image(val description: String, val url: String, val bitmap: Bitmap?) :
        MarkDownElement()
}
