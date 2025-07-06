package com.example.markdownparcer.domain.model

import android.graphics.Bitmap

sealed class TextPart {
    data class SimpleText(val text: String) : TextPart()
    data class BoldText(val text: String) : TextPart()
    data class ItalicText(val text: String) : TextPart()
    data class StrikeThroughText(val text: String) : TextPart()
    data class InlineImage(val description: String, val url: String, val bitmap: Bitmap? = null) :
        TextPart()
}

