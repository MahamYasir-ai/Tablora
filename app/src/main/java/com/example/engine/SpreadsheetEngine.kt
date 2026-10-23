package com.example.engine

import com.example.model.CsvDelimiter
import com.example.model.SpreadsheetDataset
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Pure Kotlin spreadsheet processing engine supporting CSV and XLSX reading/writing.
 * Completely local, zero-leak, high-performance, and RFC-4180 compliant.
 */
object SpreadsheetEngine {

    // -------------------------------------------------------------
    // CSV PARSING & SERIALIZATION
    // -------------------------------------------------------------

    /**
     * Auto-detect delimiter from the first few lines of CSV content.
     */
    fun detectDelimiter(content: String): Char {
        val candidates = listOf(',', ';', '\t', '|')
        val lines = content.lineSequence().take(5).filter { it.isNotBlank() }.toList()
        if (lines.isEmpty()) return ','

        var bestChar = ','
        var highestScore = -1

        for (ch in candidates) {
            val counts = lines.map { line ->
                var count = 0
                var inQuotes = false
                for (c in line) {
                    if (c == '"') inQuotes = !inQuotes
                    else if (c == ch && !inQuotes) count++
                }
                count
            }
            val minCount = counts.minOrNull() ?: 0
            val maxCount = counts.maxOrNull() ?: 0
            // If every sample line has the exact same non-zero count of this delimiter, it's a great match
            if (minCount > 0 && minCount == maxCount && minCount > highestScore) {
                highestScore = minCount
                bestChar = ch
            }
        }
        return bestChar
    }

    /**
     * Parse CSV input stream into a SpreadsheetDataset.
     */
    fun parseCsv(
        inputStream: InputStream,
        filename: String = "data.csv",
        customDelimiter: Char? = null,
        hasHeaders: Boolean = true
    ): SpreadsheetDataset {
        val text = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        val delimiter = customDelimiter ?: detectDelimiter(text)
        val allRows = parseCsvString(text, delimiter)

        if (allRows.isEmpty()) {
            return SpreadsheetDataset(
                name = filename,
                columns = emptyList(),
                rows = emptyList(),
                sourceFormat = "CSV"
            )
        }

        val rawColumns = if (hasHeaders && allRows.isNotEmpty()) allRows.first() else emptyList()
        val rawDataRows = if (hasHeaders && allRows.size > 1) allRows.drop(1) else allRows

        // Normalize column count & handle duplicate or empty column headers
        val maxCols = maxOf(rawColumns.size, rawDataRows.maxOfOrNull { it.size } ?: 0)
        val sanitizedColumns = mutableListOf<String>()
        val seenNames = mutableMapOf<String, Int>()

        for (i in 0 until maxCols) {
            val rawName = rawColumns.getOrNull(i)?.trim().orEmpty()
            val baseName = if (rawName.isBlank()) "Column_${i + 1}" else rawName
            val count = seenNames.getOrDefault(baseName, 0)
            seenNames[baseName] = count + 1
            if (count > 0) {
                sanitizedColumns.add("${baseName}_$count")
            } else {
                sanitizedColumns.add(baseName)
            }
        }

        // Pad all data rows to match column count
        val normalizedRows = rawDataRows.map { row ->
            if (row.size < maxCols) {
                row + List(maxCols - row.size) { "" }
            } else if (row.size > maxCols) {
                row.take(maxCols)
            } else {
                row
            }
        }

        return SpreadsheetDataset(
            name = filename,
            columns = sanitizedColumns,
            rows = normalizedRows,
            sourceFormat = "CSV"
        )
    }

    /**
     * RFC-4180 compliant CSV parser that supports multiline quoted cells, escaped quotes,
     * and preserves leading zeros.
     */
    fun parseCsvString(csvText: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentCell = StringBuilder()
        var inQuotes = false
        var i = 0
        val len = csvText.length

        while (i < len) {
            val c = csvText[i]

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < len && csvText[i + 1] == '"') {
                        // Escaped quote
                        currentCell.append('"')
                        i++ // Skip second quote
                    } else {
                        // End of quote
                        inQuotes = false
                    }
                } else {
                    currentCell.append(c)
                }
            } else {
                when (c) {
                    '"' -> {
                        inQuotes = true
                    }
                    delimiter -> {
                        currentRow.add(currentCell.toString())
                        currentCell.setLength(0)
                    }
                    '\r' -> {
                        if (i + 1 < len && csvText[i + 1] == '\n') {
                            i++ // Skip \n
                        }
                        currentRow.add(currentCell.toString())
                        currentCell.setLength(0)
                        rows.add(currentRow.toList())
                        currentRow.clear()
                    }
                    '\n' -> {
                        currentRow.add(currentCell.toString())
                        currentCell.setLength(0)
                        rows.add(currentRow.toList())
                        currentRow.clear()
                    }
                    else -> {
                        currentCell.append(c)
                    }
                }
            }
            i++
        }

        // Add last cell and row if not empty
        if (currentCell.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentCell.toString())
            rows.add(currentRow.toList())
        }

        return rows
    }

    /**
     * Serialize dataset to CSV string using RFC-4180 escaping.
     */
    fun exportToCsv(
        dataset: SpreadsheetDataset,
        delimiter: Char = ',',
        columnsToExport: List<String> = dataset.columns
    ): String {
        val colIndices = columnsToExport.map { dataset.columns.indexOf(it) }.filter { it != -1 }
        val sb = StringBuilder()

        // Headers
        sb.append(colIndices.joinToString(delimiter.toString()) { idx ->
            escapeCsvCell(dataset.columns[idx], delimiter)
        })
        sb.append("\r\n")

        // Rows
        for (row in dataset.rows) {
            val rowString = colIndices.joinToString(delimiter.toString()) { idx ->
                val cellVal = if (idx in row.indices) row[idx] else ""
                escapeCsvCell(cellVal, delimiter)
            }
            sb.append(rowString)
            sb.append("\r\n")
        }

        return sb.toString()
    }

    private fun escapeCsvCell(value: String, delimiter: Char): String {
        val needsQuotes = value.contains(delimiter) ||
                value.contains('"') ||
                value.contains('\n') ||
                value.contains('\r') ||
                value.startsWith(' ') ||
                value.endsWith(' ')

        return if (needsQuotes) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    // -------------------------------------------------------------
    // XLSX PARSING & GENERATION
    // -------------------------------------------------------------

    /**
     * Inspect sheet names from an XLSX input stream.
     */
    fun getXlsxSheetNames(inputStream: InputStream): List<String> {
        val sheets = mutableListOf<String>()
        val zis = ZipInputStream(inputStream)
        var entry = zis.nextEntry
        while (entry != null) {
            if (entry.name == "xl/workbook.xml") {
                val xml = zis.bufferedReader(StandardCharsets.UTF_8).readText()
                // Parse <sheet name="..." sheetId="..."/>
                val regex = Regex("""<sheet[^>]*name="([^"]+)"""")
                val matches = regex.findAll(xml)
                for (m in matches) {
                    sheets.add(m.groupValues[1])
                }
                break
            }
            entry = zis.nextEntry
        }
        return if (sheets.isEmpty()) listOf("Sheet1") else sheets
    }

    /**
     * Parse an XLSX file into a SpreadsheetDataset.
     */
    fun parseXlsx(
        inputStream: InputStream,
        filename: String = "data.xlsx",
        selectedSheetName: String? = null
    ): SpreadsheetDataset {
        val sharedStrings = mutableListOf<String>()
        var sheetXml: String? = null
        val availableSheets = mutableListOf<String>()

        val bytes = inputStream.readBytes()

        // 1. Inspect workbook.xml and sharedStrings.xml
        ZipInputStream(bytes.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                when {
                    entry.name == "xl/sharedStrings.xml" -> {
                        val xml = zis.bufferedReader(StandardCharsets.UTF_8).readText()
                        parseSharedStrings(xml, sharedStrings)
                    }
                    entry.name == "xl/workbook.xml" -> {
                        val xml = zis.bufferedReader(StandardCharsets.UTF_8).readText()
                        val regex = Regex("""<sheet[^>]*name="([^"]+)"""")
                        regex.findAll(xml).forEach { availableSheets.add(it.groupValues[1]) }
                    }
                }
                entry = zis.nextEntry
            }
        }

        val sheetTarget = if (availableSheets.isNotEmpty() && selectedSheetName != null) {
            val idx = availableSheets.indexOf(selectedSheetName)
            if (idx != -1) "xl/worksheets/sheet${idx + 1}.xml" else "xl/worksheets/sheet1.xml"
        } else {
            "xl/worksheets/sheet1.xml"
        }

        // 2. Read target sheet XML
        ZipInputStream(bytes.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == sheetTarget || (sheetXml == null && entry.name.startsWith("xl/worksheets/sheet"))) {
                    sheetXml = zis.bufferedReader(StandardCharsets.UTF_8).readText()
                    if (entry.name == sheetTarget) break
                }
                entry = zis.nextEntry
            }
        }

        if (sheetXml == null) {
            return SpreadsheetDataset(
                name = filename,
                columns = emptyList(),
                rows = emptyList(),
                sourceFormat = "XLSX"
            )
        }

        val parsedRows = parseSheetXml(sheetXml!!, sharedStrings)
        if (parsedRows.isEmpty()) {
            return SpreadsheetDataset(
                name = filename,
                columns = emptyList(),
                rows = emptyList(),
                sourceFormat = "XLSX"
            )
        }

        val headerRow = parsedRows.first()
        val dataRows = parsedRows.drop(1)
        val maxCols = maxOf(headerRow.size, dataRows.maxOfOrNull { it.size } ?: 0)

        val columns = (0 until maxCols).map { i ->
            val h = headerRow.getOrNull(i)?.trim().orEmpty()
            if (h.isBlank()) "Column_${i + 1}" else h
        }

        val normalizedRows = dataRows.map { row ->
            if (row.size < maxCols) row + List(maxCols - row.size) { "" }
            else if (row.size > maxCols) row.take(maxCols)
            else row
        }

        return SpreadsheetDataset(
            name = filename,
            sheetName = selectedSheetName ?: availableSheets.firstOrNull() ?: "Sheet1",
            availableSheets = if (availableSheets.isEmpty()) listOf("Sheet1") else availableSheets,
            columns = columns,
            rows = normalizedRows,
            sourceFormat = "XLSX"
        )
    }

    private fun parseSharedStrings(xml: String, outList: MutableList<String>) {
        // Shared strings have <si><t>Text</t></si> or complex text runs <si><r><t>...</t></r></si>
        val siRegex = Regex("""<si>(.*?)</si>""", RegexOption.DOT_MATCHES_ALL)
        val tRegex = Regex("""<t[^>]*>(.*?)</t>""", RegexOption.DOT_MATCHES_ALL)

        for (siMatch in siRegex.findAll(xml)) {
            val siContent = siMatch.groupValues[1]
            val textBuilder = StringBuilder()
            for (tMatch in tRegex.findAll(siContent)) {
                textBuilder.append(unescapeXml(tMatch.groupValues[1]))
            }
            outList.add(textBuilder.toString())
        }
    }

    private fun parseSheetXml(xml: String, sharedStrings: List<String>): List<List<String>> {
        val resultRows = mutableListOf<List<String>>()
        val rowRegex = Regex("""<row[^>]*>(.*?)</row>""", RegexOption.DOT_MATCHES_ALL)
        val cellRegex = Regex("""<c\s+([^>]*?)>(.*?)</c>""", RegexOption.DOT_MATCHES_ALL)
        val emptyCellRegex = Regex("""<c\s+([^>]*?)/>""", RegexOption.DOT_MATCHES_ALL)

        for (rowMatch in rowRegex.findAll(xml)) {
            val rowContent = rowMatch.groupValues[1]
            val cellMap = mutableMapOf<Int, String>()

            // Extract cells
            for (cMatch in cellRegex.findAll(rowContent)) {
                val attrs = cMatch.groupValues[1]
                val body = cMatch.groupValues[2]
                val colIndex = extractColumnIndex(attrs)

                val isSharedString = attrs.contains("""t="s"""")
                val isInlineString = attrs.contains("""t="inlineStr"""")

                val value = when {
                    isSharedString -> {
                        val vVal = Regex("""<v>(.*?)</v>""").find(body)?.groupValues?.get(1)
                        val sIdx = vVal?.toIntOrNull()
                        if (sIdx != null && sIdx in sharedStrings.indices) sharedStrings[sIdx] else ""
                    }
                    isInlineString -> {
                        val tVal = Regex("""<t[^>]*>(.*?)</t>""").find(body)?.groupValues?.get(1)
                        unescapeXml(tVal.orEmpty())
                    }
                    else -> {
                        val vVal = Regex("""<v>(.*?)</v>""").find(body)?.groupValues?.get(1)
                        unescapeXml(vVal.orEmpty())
                    }
                }
                if (colIndex >= 0) {
                    cellMap[colIndex] = value
                }
            }

            if (cellMap.isNotEmpty()) {
                val maxCol = (cellMap.keys.maxOrNull() ?: 0) + 1
                val rowCells = (0 until maxCol).map { col -> cellMap.getOrDefault(col, "") }
                resultRows.add(rowCells)
            } else {
                resultRows.add(emptyList())
            }
        }

        return resultRows
    }

    private fun extractColumnIndex(attrs: String): Int {
        val rMatch = Regex("""r="([A-Z]+)(\d+)"""").find(attrs) ?: return -1
        val colLetters = rMatch.groupValues[1]
        var colIndex = 0
        for (c in colLetters) {
            colIndex = colIndex * 26 + (c - 'A' + 1)
        }
        return colIndex - 1
    }

    private fun columnLetter(colIndex: Int): String {
        var n = colIndex
        val sb = StringBuilder()
        while (n >= 0) {
            sb.append(('A'.code + (n % 26)).toChar())
            n = n / 26 - 1
        }
        return sb.reverse().toString()
    }

    /**
     * Generate standard, valid XLSX byte array.
     */
    fun exportToXlsx(
        dataset: SpreadsheetDataset,
        columnsToExport: List<String> = dataset.columns
    ): ByteArray {
        val colIndices = columnsToExport.map { dataset.columns.indexOf(it) }.filter { it != -1 }
        val baos = ByteArrayOutputStream()
        val zos = ZipOutputStream(baos)

        // 1. [Content_Types].xml
        zos.putNextEntry(ZipEntry("[Content_Types].xml"))
        zos.write(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>""".toByteArray(StandardCharsets.UTF_8)
        )
        zos.closeEntry()

        // 2. _rels/.rels
        zos.putNextEntry(ZipEntry("_rels/.rels"))
        zos.write(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""".toByteArray(StandardCharsets.UTF_8)
        )
        zos.closeEntry()

        // 3. xl/_rels/workbook.xml.rels
        zos.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
        zos.write(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>""".toByteArray(StandardCharsets.UTF_8)
        )
        zos.closeEntry()

        // 4. xl/styles.xml
        zos.putNextEntry(ZipEntry("xl/styles.xml"))
        zos.write(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>
  <fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills>
  <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs>
</styleSheet>""".toByteArray(StandardCharsets.UTF_8)
        )
        zos.closeEntry()

        // 5. xl/workbook.xml
        zos.putNextEntry(ZipEntry("xl/workbook.xml"))
        zos.write(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Cleaned Data" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>""".toByteArray(StandardCharsets.UTF_8)
        )
        zos.closeEntry()

        // 6. xl/worksheets/sheet1.xml
        zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
        val sheetWriter = zos.bufferedWriter(StandardCharsets.UTF_8)
        sheetWriter.write(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
"""
        )

        // Write Header Row (Row 1)
        sheetWriter.write("""    <row r="1">""")
        for ((cIdx, origIdx) in colIndices.withIndex()) {
            val cellRef = "${columnLetter(cIdx)}1"
            val text = escapeXml(dataset.columns[origIdx])
            sheetWriter.write("""<c r="$cellRef" t="inlineStr"><is><t>$text</t></is></c>""")
        }
        sheetWriter.write("</row>\n")

        // Write Data Rows (Row 2 .. N+1)
        for ((rIdx, row) in dataset.rows.withIndex()) {
            val rowNum = rIdx + 2
            sheetWriter.write("""    <row r="$rowNum">""")
            for ((cIdx, origIdx) in colIndices.withIndex()) {
                val cellVal = if (origIdx in row.indices) row[origIdx] else ""
                val cellRef = "${columnLetter(cIdx)}$rowNum"
                val text = escapeXml(cellVal)
                sheetWriter.write("""<c r="$cellRef" t="inlineStr"><is><t>$text</t></is></c>""")
            }
            sheetWriter.write("</row>\n")
        }

        sheetWriter.write(
            """  </sheetData>
</worksheet>"""
        )
        sheetWriter.flush()
        zos.closeEntry()

        zos.finish()
        return baos.toByteArray()
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun unescapeXml(str: String): String {
        return str.replace("&apos;", "'")
            .replace("&quot;", "\"")
            .replace("&gt;", ">")
            .replace("&lt;", "<")
            .replace("&amp;", "&")
    }
}
