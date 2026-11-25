package com.example

import com.example.data.local.AccountCategory
import com.example.data.local.SeedData
import com.example.data.local.TransactionType
import com.example.i18n.AppLanguage
import com.example.i18n.stringsFor
import com.example.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountsUnitTest {
  @Test
  fun formatInr_usesIndianNumbering() {
    val formatted = Formatters.formatInr(1250000.0)
    assertTrue(formatted.contains("12,50,000"))
    assertTrue(formatted.contains("₹"))
  }

  @Test
  fun seedData_computesExpectedProfitAndPendingBalance() {
    val now = 1764000000000L
    val projects = SeedData.sampleProjects(now)
    val txs = SeedData.sampleTransactions(now)

    val totalContract = projects.sumOf { it.contractValue }
    val totalIncome =
      txs.filter { it.type == TransactionType.INCOME_CUSTOMER_PAYMENT.name }.sumOf { it.amount }
    val totalExpense =
      txs.filter { it.type != TransactionType.INCOME_CUSTOMER_PAYMENT.name }.sumOf { it.amount }
    val cementSpend = txs.filter { it.category == AccountCategory.CEMENT.name }.sumOf { it.amount }

    assertEquals(8050000.0, totalContract, 0.01)
    assertEquals(2700000.0, totalIncome, 0.01)
    assertEquals(5350000.0, totalContract - totalIncome, 0.01)
    assertTrue(totalExpense > 0.0)
    assertEquals(167350.0, cementSpend, 0.01)
  }

  @Test
  fun bilingualStrings_provideBothEnglishAndTamil() {
    val en = stringsFor(AppLanguage.ENGLISH)
    val ta = stringsFor(AppLanguage.TAMIL)
    assertEquals("Chola Empire", en.brandTitle)
    assertEquals("சோழா எம்பயர்", ta.brandTitle)
  }
}
