package com.example.markdownparcer


import com.example.markdownparcer.domain.model.MarkDownElement
import com.example.markdownparcer.domain.model.TableElement
import com.example.markdownparcer.domain.model.TextPart
import com.example.markdownparcer.domain.parcer.MarkDownParser

import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class MarkdownParserUnitTest {

    val parser = MarkDownParser()


    @Test
    fun `parse headers 1 and 2 levels`() {
        val header1 = "# Header 1"
        val header2 = "## Header 2"

        val result1 = parser.parse(header1)
        val result2 = parser.parse(header2)

        assertEquals(1, result1.size)
        assertEquals(MarkDownElement.Header(1, "Header 1"), result1[0])

        assertEquals(1, result2.size)
        assertEquals(MarkDownElement.Header(2, "Header 2"), result2[0])
    }

    @Test
    fun `parse bold styles text`() {
        val boldText = "**bold**"

        val mainElement = parser.parse(boldText)
        val paragraph = mainElement[0] as MarkDownElement.Paragraph
        val textPart = paragraph.textPart

        val innerPart = textPart[0] as TextPart.BoldText

        assertEquals("bold", innerPart.text)
    }

    @Test
    fun `parse italic styles text`() {
        val italicText = "*italic*"

        val mainElement = parser.parse(italicText)
        val paragraph = mainElement[0] as MarkDownElement.Paragraph
        val textPart = paragraph.textPart

        val innerPart = textPart[0] as TextPart.ItalicText
        assertEquals("italic", innerPart.text)
    }

    @Test
    fun `parse italic underline styles text`() {
        val italicText = "_italic_"

        val mainElement = parser.parse(italicText)
        val paragraph = mainElement[0] as MarkDownElement.Paragraph
        val textPart = paragraph.textPart

        assertEquals(1, textPart.size)
        val innerPart = textPart[0] as TextPart.ItalicText
        assertEquals("italic", innerPart.text)
    }

    @Test
    fun `parse not italic underline styles text`() {
        val italicText = "it_alic"

        val mainElement = parser.parse(italicText)
        val paragraph = mainElement[0] as MarkDownElement.Paragraph
        val textPart = paragraph.textPart

        val innerPart = textPart[0] as TextPart.SimpleText
        assertEquals(1, textPart.size)
        assertEquals("it_alic", innerPart.text)
    }

    @Test
    fun `parse strike through styles text`() {
        val strikeThroughText = "~~strike through~~"

        val mainElement = parser.parse(strikeThroughText)
        val paragraph = mainElement[0] as MarkDownElement.Paragraph
        val textPart = paragraph.textPart

        val innerPart = textPart[0] as TextPart.StrikeThroughText

        assertEquals("strike through", innerPart.text)
    }

    @Test
    fun `parse image`() {
        val image = "![description](link)"
        val result = parser.parse(image)

        val imageElement = result[0] as MarkDownElement.Image

        assertEquals(
            MarkDownElement.Image(description = "description", url = "link", null),
            imageElement
        )
    }

    @Test
    fun `isStandaloneImageLine test true`() {
        val image = "![alt text](https://example.com/image.png)"
        val result = parser.isStandaloneImageLine(image)
        assertNotNull(result)
    }

    @Test
    fun `isStandaloneImageLine test false`() {
        val image = "это встроенная в текст картинка ![alt text](https://example.com/image.png)"
        val result = parser.isStandaloneImageLine(image)
        assertNull(result)
    }

    @Test
    fun `parse markdown table`() {
        val markdownTable = """
        | Header 1 | Header 2 | Header 3 |
        |:--------|:---:|----------:|
        | Cell 1 | Cell 2 | Cell 3 |
        
    """.trimIndent()

        val result = parser.parse(markdownTable)
        assertEquals(1, result.size)

        val table = result[0] as MarkDownElement.Table
        val elements = table.table

        val header = elements[0] as TableElement.TableHeader
        assertEquals(listOf("Header 1", "Header 2", "Header 3"), header.cells)


        val row1 = elements[2] as TableElement.TableRow

        assertEquals(listOf("Cell 1", "Cell 2", "Cell 3"), row1.cells)
    }

}