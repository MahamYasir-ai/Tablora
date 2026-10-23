package com.example.engine

import com.example.model.AiProposedPlan
import com.example.model.CleaningOperation
import com.example.model.DateOutputFormat
import com.example.model.EmailAction
import com.example.model.FillStrategy
import com.example.model.SpreadsheetDataset
import com.example.model.TextCase

/**
 * AI-assisted cleaning advisor for "Ask Tablora".
 * Operates with privacy-first architecture: examines only column headers, inferred types,
 * and aggregate issue metrics. Translates natural language intent into structured,
 * validated operations that require explicit user approval.
 */
object AiCleaningAssistant {

    /**
     * Generates a structured cleaning plan based on user prompt and dataset schema.
     */
    fun generatePlan(
        prompt: String,
        dataset: SpreadsheetDataset
    ): AiProposedPlan {
        val lowerPrompt = prompt.lowercase()
        val proposedOps = mutableListOf<CleaningOperation>()
        val rationaleBuilder = StringBuilder()

        val columns = dataset.columns
        val dateCols = columns.filter {
            val l = it.lowercase()
            l.contains("date") || l.contains("time") || l.contains("created") || l.contains("dob")
        }
        val emailCols = columns.filter { it.lowercase().contains("email") }
        val textCols = columns.filter {
            val l = it.lowercase()
            l.contains("name") || l.contains("status") || l.contains("company") || l.contains("title") || l.contains("category")
        }
        val numericCols = columns.filter {
            val l = it.lowercase()
            l.contains("price") || l.contains("spend") || l.contains("value") || l.contains("amount") || l.contains("total") || l.contains("stock") || l.contains("qty")
        }

        // Rule 1: Empty rows/cols
        if (lowerPrompt.contains("empty") || lowerPrompt.contains("blank") || lowerPrompt.contains("prepare") || lowerPrompt.contains("clean all")) {
            proposedOps.add(CleaningOperation.RemoveEmptyRows)
            rationaleBuilder.append("• Remove fully blank rows to prevent corrupting downstream analytics.\n")
        }

        // Rule 2: Duplicates
        if (lowerPrompt.contains("duplicate") || lowerPrompt.contains("dedup") || lowerPrompt.contains("prepare") || lowerPrompt.contains("clean all")) {
            val matchingCol = emailCols.firstOrNull() ?: columns.firstOrNull { it.lowercase().contains("id") }
            if (matchingCol != null && lowerPrompt.contains(matchingCol.lowercase())) {
                proposedOps.add(CleaningOperation.RemoveDuplicatesByColumns(listOf(matchingCol)))
                rationaleBuilder.append("• Deduplicate records by unique identifier '$matchingCol'.\n")
            } else {
                proposedOps.add(CleaningOperation.RemoveExactDuplicates)
                rationaleBuilder.append("• Remove exact duplicate rows across all columns.\n")
            }
        }

        // Rule 3: Trim spaces
        if (lowerPrompt.contains("trim") || lowerPrompt.contains("space") || lowerPrompt.contains("clean all") || lowerPrompt.contains("prepare") || lowerPrompt.contains("whitespace")) {
            proposedOps.add(CleaningOperation.TrimWhitespace(columns, trimAllEligible = true))
            proposedOps.add(CleaningOperation.NormalizeInternalSpaces(columns))
            rationaleBuilder.append("• Strip leading/trailing whitespace and collapse repeated internal spaces.\n")
        }

        // Rule 4: Dates
        if (lowerPrompt.contains("date") || lowerPrompt.contains("standardize date") || lowerPrompt.contains("iso") || lowerPrompt.contains("clean all") || lowerPrompt.contains("prepare")) {
            if (dateCols.isNotEmpty()) {
                val targetFmt = if (lowerPrompt.contains("us") || lowerPrompt.contains("mm/dd")) DateOutputFormat.US_MDY
                else DateOutputFormat.ISO_YMD
                proposedOps.add(CleaningOperation.StandardizeDates(dateCols, targetFmt))
                rationaleBuilder.append("• Standardize date columns (${dateCols.joinToString(", ")}) to ${targetFmt.label}.\n")
            }
        }

        // Rule 5: Email validation
        if (lowerPrompt.contains("email") || lowerPrompt.contains("contact") || lowerPrompt.contains("clean all")) {
            if (emailCols.isNotEmpty()) {
                val targetEmailCol = emailCols.first()
                if (lowerPrompt.contains("remove invalid") || lowerPrompt.contains("delete invalid")) {
                    proposedOps.add(CleaningOperation.ValidateEmail(targetEmailCol, EmailAction.REMOVE_INVALID_ROWS))
                    rationaleBuilder.append("• Filter out rows with unparseable email addresses in '$targetEmailCol'.\n")
                } else {
                    proposedOps.add(CleaningOperation.ValidateEmail(targetEmailCol, EmailAction.TRIM_AND_LOWER))
                    rationaleBuilder.append("• Normalize email casing and trim whitespace in '$targetEmailCol'.\n")
                }
            }
        }

        // Rule 6: Currency / Numbers
        if (lowerPrompt.contains("number") || lowerPrompt.contains("numeric") || lowerPrompt.contains("currency") || lowerPrompt.contains("price") || lowerPrompt.contains("spend")) {
            if (numericCols.isNotEmpty()) {
                proposedOps.add(CleaningOperation.ConvertToNumeric(numericCols, stripCurrencyAndCommas = true))
                rationaleBuilder.append("• Strip currency symbols and parse values into clean numbers on: ${numericCols.joinToString(", ")}.\n")
            }
        }

        // Rule 7: Text case conversion
        if (lowerPrompt.contains("lowercase") || lowerPrompt.contains("lower case")) {
            val targets = textCols.ifEmpty { columns.take(2) }
            proposedOps.add(CleaningOperation.ConvertCase(targets, TextCase.LOWER))
            rationaleBuilder.append("• Convert text to lowercase on: ${targets.joinToString(", ")}.\n")
        } else if (lowerPrompt.contains("uppercase") || lowerPrompt.contains("upper case")) {
            val targets = textCols.ifEmpty { columns.take(2) }
            proposedOps.add(CleaningOperation.ConvertCase(targets, TextCase.UPPER))
            rationaleBuilder.append("• Convert text to uppercase on: ${targets.joinToString(", ")}.\n")
        } else if (lowerPrompt.contains("title case") || lowerPrompt.contains("capitalize")) {
            val targets = textCols.ifEmpty { columns.take(2) }
            proposedOps.add(CleaningOperation.ConvertCase(targets, TextCase.TITLE))
            rationaleBuilder.append("• Convert text to Title Case on: ${targets.joinToString(", ")}.\n")
        }

        // Rule 8: Fill missing values
        if (lowerPrompt.contains("fill") || lowerPrompt.contains("missing value") || lowerPrompt.contains("n/a")) {
            val targetCols = columns.take(3)
            proposedOps.add(CleaningOperation.FillMissingValues(targetCols, FillStrategy.PLACEHOLDER_NA))
            rationaleBuilder.append("• Fill blank cells with 'N/A' placeholder on: ${targetCols.joinToString(", ")}.\n")
        }

        // Default fallback if query didn't match specific triggers: comprehensive general clean recommendation
        if (proposedOps.isEmpty()) {
            proposedOps.add(CleaningOperation.RemoveEmptyRows)
            proposedOps.add(CleaningOperation.RemoveExactDuplicates)
            proposedOps.add(CleaningOperation.TrimWhitespace(columns, trimAllEligible = true))
            if (dateCols.isNotEmpty()) proposedOps.add(CleaningOperation.StandardizeDates(dateCols, DateOutputFormat.ISO_YMD))
            if (numericCols.isNotEmpty()) proposedOps.add(CleaningOperation.ConvertToNumeric(numericCols))

            rationaleBuilder.append("• Discard blank and duplicate rows.\n")
            rationaleBuilder.append("• Trim all text padding and sanitize inconsistent formats across columns.\n")
        }

        return AiProposedPlan(
            userPrompt = prompt,
            planSummary = "Proposed ${proposedOps.size} cleaning step(s) based on your intent.",
            rationale = rationaleBuilder.toString().trimEnd(),
            operations = proposedOps,
            requiresApproval = true
        )
    }
}
