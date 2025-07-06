package com.example.markdownparcer.presentation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.markdownparcer.domain.model.MarkDownElement
import com.example.markdownparcer.domain.model.ParsedMarkDown
import com.example.markdownparcer.domain.model.TextPart
import com.example.markdownparcer.domain.parcer.MarkDownParser
import com.example.markdownparcer.presentation.model.ViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MarkdownViewModel(
    private val parser: MarkDownParser,
) : ViewModel() {


    private val _viewStateFlow = MutableStateFlow<ViewState>(ViewState.Idle())
    val viewState = _viewStateFlow.asStateFlow()

    private val _parsedMarkdownFlow = MutableStateFlow<ParsedMarkDown?>(null)
    val parsedMarkdownFlow = _parsedMarkdownFlow.asStateFlow()

    private val _originalMarkdownFlow = MutableStateFlow<String?>(null)
    val originalMarkdownFlow = _originalMarkdownFlow.asStateFlow()


    fun parse(text: String) {
        viewModelScope.launch {
            _viewStateFlow.value = ViewState.Loading()
            _originalMarkdownFlow.value = text
            val parsed = parser.parse(text)
            _parsedMarkdownFlow.value = ParsedMarkDown(parsed)
            _viewStateFlow.value = ViewState.Success("Файл успешно загружен")
        }
    }

    fun loadMarkdownFromUrl(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _viewStateFlow.value = ViewState.Loading()
            try {
                val url =
                    URL(url)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val text = reader.readText()
                reader.close()
                connection.disconnect()
                parse(text)
                _viewStateFlow.value = ViewState.Success("Файл успешно загружен")
            } catch (e: Exception) {
                _viewStateFlow.value = ViewState.Error("Ошибка загрузки из сети")
            }
        }
    }

    fun loadImagesBitmaps() {
        viewModelScope.launch(Dispatchers.IO) {
            _viewStateFlow.value = ViewState.Loading()
            val currentParsedMarkDown = _parsedMarkdownFlow.value ?: return@launch
            val updatedContent = currentParsedMarkDown.content.map { element ->
                when (element) {
                    is MarkDownElement.Image -> {
                        val bmp = loadBitmapFromUrl(element.url)
                        element.copy(bitmap = bmp)
                    }

                    is MarkDownElement.Paragraph -> {
                        val updatedParts = element.textPart.map { part ->
                            when (part) {
                                is TextPart.InlineImage -> {
                                    val bmp = loadBitmapFromUrl(part.url)
                                    part.copy(bitmap = bmp)
                                }

                                else -> part
                            }
                        }
                        element.copy(textPart = updatedParts)
                    }

                    else -> element
                }
            }
            _parsedMarkdownFlow.value = ParsedMarkDown(updatedContent)
            _viewStateFlow.value = ViewState.Success("Изображения успешно загружены")

        }

    }


    private fun loadBitmapFromUrl(url: String): Bitmap? {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(input)
            input.close()
            connection.disconnect()
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    fun editMarkdown(text: String) {
        parse(text)
    }

    private val testText = "# Заголовок уровня 1\n" +
            "\n" +
            "## Заголовок уровня 2\n" +
            "\n" +
            "### Заголовок уровня 3\n" +
            "\n" +
            "Простой абзац без форматирования.\n" +
            "\n" +
            "Абзац с *курсивом*, **жирным текстом**, ~~зачёркнутым~~ и **смешанным *жирно-курсивным* стилем**.\n" +
            "\n" +
            "![пример](https://www.syngenta.by/sites/g/files/kgtney481/files/media/image/2020/06/09/zayac-rusak.jpg) \n" +
            "\n" +
            "И ещё один абзац с встроенной картинкой ![встроенная](https://cdn-icons-png.flaticon.com/512/1829/1829007.png) и текстом после неё.\n" +
            "\n" +
            "Пример таблицы:\n" +
            "\n" +
            "| Заголовок 1 | Заголовок 2 |\n" +
            "|------------|-------------|\n" +
            "| Ячейка 1   | Ячейка 2    |\n" +
            "| Ячейка 3   | Ячейка 4    |"


}