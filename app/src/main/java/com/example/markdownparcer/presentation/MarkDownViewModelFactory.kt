package com.example.markdownparcer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.markdownparcer.domain.parcer.MarkDownParser


@Suppress("UNCHECKED_CAST")
class MarkdownViewModelFactory(
    private val markDownParser: MarkDownParser
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MarkdownViewModel::class.java)) {
            return MarkdownViewModel(parser = markDownParser) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}