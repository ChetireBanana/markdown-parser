package com.example.markdownparcer.domain.model

sealed class TableElement {
    data class TableHeader(val cells: List<String>) : TableElement()
    data class TableHeaderDivider(val positioning: List<String>) : TableElement()
    data class TableRow(val cells: List<String>) : TableElement()
}
