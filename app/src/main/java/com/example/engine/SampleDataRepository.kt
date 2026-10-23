package com.example.engine

import com.example.model.SpreadsheetDataset

/**
 * Provides realistic, fictional business sample datasets to test all cleaning operations.
 * Fulfills all criteria of Section 16: duplicate rows, missing emails, unpadded spaces,
 * mixed casing, mixed dates, empty rows, invalid numbers, leading zeros, and Unicode names.
 */
object SampleDataRepository {

    val SAMPLE_CUSTOMERS = SpreadsheetDataset(
        name = "Sample_Ecommerce_Customers_Messy.csv",
        sheetName = "Customers",
        columns = listOf(
            "Customer_ID",
            "Full_Name",
            "Email",
            "Phone_Number",
            "Signup_Date",
            "Lifetime_Spend",
            "Account_Status",
            "Postal_Code"
        ),
        rows = listOf(
            listOf("00142", " Alice Cooper ", "alice.cooper@example.com", "(555) 234-5678", "2023-01-15", "$1,450.00", "ACTIVE", "02134"),
            listOf("00891", "BOB MARTIN", "bob.martin@company.org", "5559876543", "03/22/2023", "890.50", "active", "07001"),
            listOf("00142", " Alice Cooper ", "alice.cooper@example.com", "(555) 234-5678", "2023-01-15", "$1,450.00", "ACTIVE", "02134"), // Exact Duplicate
            listOf("00315", "Renée Müller", "renee.muller@example.de", "+1 555-432-1098", "14-Aug-2023", "2,340.00", "Active", "01002"),
            listOf("", "", "", "", "", "", "", ""), // Empty row
            listOf("00488", "jane smith", "jane-invalid-email@", "123", "2023-05-18", "N/A", "PENDING", "90210"), // Invalid email, phone, spend
            listOf("00502", "François  Dupont", "francois@example.fr", "(555) 876-5432", "11/04/2023", "$450.25", "pending", "08817"),
            listOf("00620", "DAVID  WILLIAMS", "", "555-019-2834", "2023-09-01", "$3,100.00", "ACTIVE", "00501"), // Missing email
            listOf("00711", "sarah connor", "sarah.c@sky.net", "555-111-2222", "2023-12-05", "150.00", "Suspended", "30301"),
            listOf("00891", "BOB MARTIN", "bob.martin@company.org", "5559876543", "03/22/2023", "890.50", "active", "07001"), // Duplicate row
            listOf("00934", " Carlos Rodriguez ", "carlos.r@correo.es", "(555) 345-6789", "07/19/2023", "$5,210.80", "Active", "75001"),
            listOf("00105", "EMILY  WATSON", "emily.watson@domain.com", "555.678.9012", "2023-04-10", "420.00", "ACTIVE", "06001"),
            listOf("00233", "michael brown", "mbrown@", "(555) 789-0123", "2023-10-30", "zero", "CANCELLED", "10001"), // Invalid email & spend
            listOf("", "", "", "", "", "", "", ""), // Another empty row
            listOf("00677", "Olivia Taylor ", "olivia.t@example.org", "+1 (555) 901-2345", "12/28/2023", "$1,890.00", "Active", "94102")
        ),
        sourceFormat = "CSV"
    )

    val SAMPLE_SALES = SpreadsheetDataset(
        name = "Sample_Sales_Pipeline.csv",
        sheetName = "Deals",
        columns = listOf("Deal_ID", "Company_Name", "Deal_Stage", "Estimated_Value", "Close_Date", "Sales_Rep"),
        rows = listOf(
            listOf("D-1001", "Acme  Corporation", "Proposal", "$45,000.00", "2024-02-15", "John  Doe "),
            listOf("D-1002", "Globex Inc", "CLOSED WON", "$120,500.00", "01/18/2024", "Jane Smith"),
            listOf("D-1003", "Initech LLC ", "qualified", "15,000", "2024-03-30", "Bob Martin"),
            listOf("D-1004", "Umbrella  Corp", "closed won", "$85,000", "2024-04-12", "John  Doe "),
            listOf("D-1001", "Acme  Corporation", "Proposal", "$45,000.00", "2024-02-15", "John  Doe "), // Duplicate
            listOf("D-1005", "Soylent Industries", "Discovery", "$25,000.00", "15/05/2024", "Sarah Connor")
        ),
        sourceFormat = "CSV"
    )

    val SAMPLE_INVENTORY = SpreadsheetDataset(
        name = "Sample_Inventory_SKU_Log.xlsx",
        sheetName = "Inventory",
        columns = listOf("SKU_Code", "Item_Title", "Category", "In_Stock", "Unit_Price", "Supplier_Code"),
        rows = listOf(
            listOf("004921", "Wireless Ergonomic Mouse ", "ELECTRONICS", "145", "$39.99", "SUP-01"),
            listOf("008812", "Mechanical Keyboard RGB", "electronics", "28", "$119.50", "SUP-04"),
            listOf("004921", "Wireless Ergonomic Mouse ", "ELECTRONICS", "145", "$39.99", "SUP-01"), // Duplicate
            listOf("001205", "USB-C Braided Cable (2m)", "Accessories ", "out of stock", "$14.99", "SUP-02"), // Text in numeric stock
            listOf("009341", "Ultra-wide Monitor 34\"", "Electronics", "12", "$499.00", "SUP-01")
        ),
        sourceFormat = "XLSX"
    )
}
