package com.example.markdownparcer.domain.parcer

import com.example.markdownparcer.domain.model.MarkDownElement
import com.example.markdownparcer.domain.model.TableElement
import com.example.markdownparcer.domain.model.TablePositioning
import com.example.markdownparcer.domain.model.TextPart

class MarkDownParser() {

    val imageRegex = Regex("""!\[([^]]+)]\(([^)]+)\)""")

    fun parse(markdownText: String): List<MarkDownElement> {
        val result = mutableListOf<MarkDownElement>()
        val lines = markdownText.split("\n")
        var currentLine = 0

        while (currentLine < lines.size) {
            if (lines[currentLine].isNotEmpty()) {
                // Header parsing
                if (lines[currentLine].startsWith("#")) {
                    result.add(headerParsing(lines[currentLine]))
                }
                // Image parsing
                else if (lines[currentLine].startsWith("![")) {
                    val match = isStandaloneImageLine(lines[currentLine])
                    if (match != null) {
                        result.add(imageParsing(match))
                    } else {
                        val (parsedPart, _) = paragraphParsing(lines[currentLine], 0)
                        result.add(MarkDownElement.Paragraph(parsedPart))
                    }
                }
                // Table parsing
                else if (lines[currentLine].startsWith("|")) {
                    val dividersCount = lines[currentLine].count { it == '|' }
                    if (dividersCount > 1) {
                        val tableBeginsFromLine = currentLine
                        var tableEndsAtLine = currentLine

                        while (tableEndsAtLine < lines.size && lines[tableEndsAtLine].count { it == '|' } == dividersCount) {
                            tableEndsAtLine++
                        }

                        if (tableEndsAtLine - tableBeginsFromLine >= 3) {
                            val tableLines = lines.subList(tableBeginsFromLine, tableEndsAtLine)
                            val dividerRegex = Regex("""\|?(\s*:?-+:?\s*\|)+\s*""")
                            if (dividerRegex.matches(tableLines[1])) {
                                result.add(tableParsing(tableLines))
                                currentLine = tableEndsAtLine
                                continue
                            }
                        } else {
                            val (parsedPart, _) = paragraphParsing(lines[currentLine], 0)
                            result.add(MarkDownElement.Paragraph(parsedPart))
                        }
                    }
                }
                // Paragraph parsing
                else {
                    val (parsedPart, _) = paragraphParsing(lines[currentLine], 0)
                    result.add(MarkDownElement.Paragraph(parsedPart))
                }
            }
            currentLine++
        }

        return result
    }


    fun headerParsing(line: String): MarkDownElement {
        var charIndex = 0
        var headingLevel = 0
        while (charIndex < line.length && line[charIndex] == '#') {
            headingLevel++
            charIndex++
        }
        while (charIndex < line.length && line[charIndex] == ' ') {
            charIndex++
        }

        val headingText = line.substring(charIndex)
        var endIndex = headingText.length - 1
        while (endIndex >= 0 && (headingText[endIndex] == ' ' || headingText[endIndex] == '#')) {
            endIndex--
        }
        val clearedHeadingText = headingText.substring(0, endIndex + 1)
        return MarkDownElement.Header(level = headingLevel, text = clearedHeadingText)
    }

    private fun tableParsing(lines: List<String>): MarkDownElement {

        val tableHeader = TableElement.TableHeader(
            lines[0].split("|").drop(1).dropLast(1).map { it.trim() }
        )

        val tableHeaderDivider = lines[1].split("|").drop(1).dropLast(1).map { it.trim() }
        val tableHeaderPositioning = TableElement.TableHeaderDivider(
            tableHeaderDivider.map {
                when {
                    it.startsWith(":") && it.endsWith(":") -> {
                        TablePositioning.CENTER
                    }

                    it.startsWith(":") -> TablePositioning.START
                    it.endsWith(":") -> TablePositioning.END
                    else -> TablePositioning.CENTER
                }
            }
        )

        val tableRows = mutableListOf<TableElement>()
        for (otherLine in lines.subList(2, lines.size)) {
            val row = otherLine.split("|").drop(1).dropLast(1).map { it.trim() }
            tableRows.add(TableElement.TableRow(row))
        }

        val resultTableList = mutableListOf<TableElement>()
        resultTableList.add(tableHeader)
        resultTableList.add(tableHeaderPositioning)
        resultTableList.addAll(tableRows)

        return MarkDownElement.Table(resultTableList)
    }


    fun imageParsing(regexResult: MatchResult): MarkDownElement {
        val descriptor = regexResult.groupValues[1]
        val url = regexResult.groupValues[2]
        return MarkDownElement.Image(descriptor, url, null)
    }

    fun isStandaloneImageLine(line: String): MatchResult? {
        val trimmed = line.trim()
        val match = imageRegex.matchEntire(trimmed)
        return if (match != null && match.range.first == 0 && match.range.last == trimmed.length - 1) {
            match
        } else null
    }

    private fun paragraphParsing(
        string: String,
        startIndex: Int,
        endMarker: String? = null
    ): Pair<List<TextPart>, Int> {
        val parts = mutableListOf<TextPart>()
        var currentIndex = startIndex

        while (currentIndex < string.length) {
            when {
                endMarker != null && string.startsWith(endMarker, currentIndex) -> {
                    return parts to (currentIndex + endMarker.length)
                }
                // Bold parsing
                string.startsWith("**", currentIndex) -> {
                    val endIndex = string.indexOf("**", currentIndex + 2)
                    if (endIndex != -1) {
                        val content = string.substring(currentIndex + 2, endIndex)
                        parts.add(TextPart.BoldText(content))
                        currentIndex = endIndex + 2
                    } else {
                        parts.add(TextPart.SimpleText("**"))
                        currentIndex += 2
                    }
                }

                string.startsWith("__", currentIndex) && isValidMarker(
                    string,
                    currentIndex,
                    "__"
                ) -> {
                    var endIndex = string.indexOf("__", currentIndex + 2)
                    while (endIndex != -1 && !isValidMarker(string, endIndex, "__")) {
                        endIndex = string.indexOf("__", endIndex + 2)
                    }
                    if (endIndex != -1) {
                        val content = string.substring(currentIndex + 2, endIndex)
                        parts.add(TextPart.BoldText(content))
                        currentIndex = endIndex + 2
                    } else {
                        parts.add(TextPart.SimpleText("__"))
                        currentIndex += 2
                    }
                }

                // Italic parsing
                string.startsWith("*", currentIndex) -> {
                    val endIndex = string.indexOf("*", currentIndex + 1)
                    if (endIndex != -1) {
                        val content = string.substring(currentIndex + 1, endIndex)
                        parts.add(TextPart.ItalicText(content))
                        currentIndex = endIndex + 1
                    } else {
                        parts.add(TextPart.SimpleText("*"))
                        currentIndex += 1
                    }
                }

                string.startsWith("_", currentIndex) && isValidMarker(
                    string,
                    currentIndex,
                    "_"
                ) -> {
                    var endIndex = string.indexOf("_", currentIndex + 1)
                    while (endIndex != -1 && !isValidMarker(string, endIndex, "_")) {
                        endIndex = string.indexOf("_", endIndex + 1)
                    }
                    if (endIndex != -1) {
                        val content = string.substring(currentIndex + 1, endIndex)
                        parts.add(TextPart.ItalicText(content))
                        currentIndex = endIndex + 1
                    } else {
                        parts.add(TextPart.SimpleText("_"))
                        currentIndex += 1
                    }
                }

                // Strikethrough parsing
                string.startsWith("~~", currentIndex) -> {
                    val endIndex = string.indexOf("~~", currentIndex + 2)
                    if (endIndex != -1) {
                        val content = string.substring(currentIndex + 2, endIndex)
                        parts.add(TextPart.StrikeThroughText(content))
                        currentIndex = endIndex + 2
                    } else {
                        parts.add(TextPart.SimpleText("~~"))
                        currentIndex += 2
                    }
                }

                //Inline Image parsing
                string.startsWith("![", currentIndex) -> {
                    val matchResult = imageRegex.find(string.substring(currentIndex))
                    if (matchResult != null) {
                        val description = matchResult.groupValues[1]
                        val url = matchResult.groupValues[2]
                        parts.add(TextPart.InlineImage(description, url))
                        currentIndex += matchResult.value.length
                    } else {
                        parts.add(TextPart.SimpleText("!["))
                        currentIndex += 2
                    }
                }

                else -> {
                    val nextMarkerIndex = findNextValidMarker(string, currentIndex, endMarker)

                    val content = string.substring(currentIndex, nextMarkerIndex)
                    if (content.isNotEmpty()) {
                        parts.add(TextPart.SimpleText(content))
                    }
                    currentIndex = nextMarkerIndex

                }
            }
        }
        return parts to currentIndex
    }


    private fun isValidMarker(string: String, index: Int, marker: String): Boolean {
        val afterMarkerIndex = index + marker.length

        val previousChar = if (index > 0) string[index - 1] else null
        val nextChar = if (afterMarkerIndex < string.length) string[afterMarkerIndex] else null


        val isNotInsideWord =
            !(previousChar?.isLetterOrDigit() == true && nextChar?.isLetterOrDigit() == true)

        return isNotInsideWord
    }

    fun findNextValidMarker(string: String, currentIndex: Int, endMarker: String?): Int {
        val markers = listOf("**", "__", "*", "_", "~~", "![") + listOfNotNull(endMarker)

        for (i in currentIndex until string.length) {
            for (marker in markers) {
                if (string.startsWith(marker, i)) {
                    if (marker == "_" || marker == "__") {
                        if (isValidMarker(string, i, marker)) {
                            return i
                        }
                    } else return i
                }
            }
        }
        return string.length
    }


}
