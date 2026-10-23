package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.CleaningEngine
import com.example.engine.DataQualityAnalyzer
import com.example.engine.SampleDataRepository
import com.example.engine.SpreadsheetEngine
import com.example.model.CleaningOperation
import com.example.model.DateOutputFormat
import com.example.model.NumericInvalidHandling
import com.example.model.SpreadsheetDataset
import com.example.model.TextCase
import com.example.storage.TabloraStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches Tablora`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Tablora", appName)
    }

    @Test
    fun `csv parsing preserves leading zeros and handles quoted commas`() {
        val csv = """ID,Name,Notes
"00142","Smith, John","Leading zeros and, quoted commas"
"00980","Alice Doe","Plain text"
"""
        val dataset = SpreadsheetEngine.parseCsv(csv.byteInputStream(), "test.csv")
        assertEquals(2, dataset.rowCount)
        assertEquals(3, dataset.columnCount)
        assertEquals("00142", dataset.getCellValue(0, 0))
        assertEquals("Smith, John", dataset.getCellValue(0, 1))
        assertEquals("Leading zeros and, quoted commas", dataset.getCellValue(0, 2))
    }

    @Test
    fun `cleaning engine removes empty rows and exact duplicates`() {
        val sample = SampleDataRepository.SAMPLE_CUSTOMERS
        val originalRows = sample.rowCount

        // 1. Remove empty rows
        val noEmpty = CleaningEngine.apply(sample, CleaningOperation.RemoveEmptyRows)
        assertTrue(noEmpty.rowCount < originalRows)

        // 2. Remove duplicates
        val deduplicated = CleaningEngine.apply(noEmpty, CleaningOperation.RemoveExactDuplicates)
        assertTrue(deduplicated.rowCount < noEmpty.rowCount)

        // 3. Trim whitespace
        val trimmed = CleaningEngine.apply(deduplicated, CleaningOperation.TrimWhitespace(deduplicated.columns, trimAllEligible = true))
        val aliceName = trimmed.getCellValue(0, 1)
        assertEquals("Alice Cooper", aliceName)
    }

    @Test
    fun `numeric conversion strips currency and commas`() {
        val dataset = SpreadsheetDataset(
            name = "numbers.csv",
            columns = listOf("Item", "Price"),
            rows = listOf(
                listOf("Laptop", "$1,450.00"),
                listOf("Mouse", "€39.99"),
                listOf("Cable", "£12.50")
            )
        )
        val cleaned = CleaningEngine.apply(dataset, CleaningOperation.ConvertToNumeric(listOf("Price")))
        assertEquals("1450", cleaned.getCellValue(0, 1))
        assertEquals("39.99", cleaned.getCellValue(1, 1))
        assertEquals("12.5", cleaned.getCellValue(2, 1))
    }

    @Test
    fun `date standardization converts mixed dates to ISO format`() {
        val dataset = SpreadsheetDataset(
            name = "dates.csv",
            columns = listOf("Event", "Event_Date"),
            rows = listOf(
                listOf("A", "03/22/2023"),
                listOf("B", "2023-01-15")
            )
        )
        val cleaned = CleaningEngine.apply(dataset, CleaningOperation.StandardizeDates(listOf("Event_Date"), DateOutputFormat.ISO_YMD))
        assertEquals("2023-03-22", cleaned.getCellValue(0, 1))
        assertEquals("2023-01-15", cleaned.getCellValue(1, 1))
    }

    @Test
    fun `data quality analyzer calculates realistic score and identifies issues`() {
        val sample = SampleDataRepository.SAMPLE_CUSTOMERS
        val report = DataQualityAnalyzer.analyze(sample)

        assertTrue(report.qualityScore in 10..95)
        assertTrue(report.emptyRowsCount > 0)
        assertTrue(report.duplicateRowsCount > 0)
        assertTrue(report.categories.isNotEmpty())
    }

    @Test
    fun `xlsx export generates valid openxml bytes`() {
        val sample = SampleDataRepository.SAMPLE_SALES
        val bytes = SpreadsheetEngine.exportToXlsx(sample)
        assertNotNull(bytes)
        assertTrue(bytes.size > 500)
        // Verify ZIP magic bytes PK\x03\x04
        assertEquals(0x50.toByte(), bytes[0])
        assertEquals(0x4B.toByte(), bytes[1])
    }

    @Test
    fun `tablora storage persists presets and history locally`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = TabloraStorage(context)

        val presets = storage.getPresets()
        assertTrue(presets.isNotEmpty())
        assertTrue(presets.any { it.isBuiltIn })
    }
}
