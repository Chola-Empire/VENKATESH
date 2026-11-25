package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object Formatters {
  private val indianLocale = Locale("en", "IN")
  private val inrCurrencyFormat: NumberFormat =
    NumberFormat.getCurrencyInstance(indianLocale).apply {
      maximumFractionDigits = 0
      minimumFractionDigits = 0
    }
  private val inrPreciseFormat: NumberFormat =
    NumberFormat.getCurrencyInstance(indianLocale).apply {
      maximumFractionDigits = 2
      minimumFractionDigits = 2
    }

  private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
  private val shortDateFormat = SimpleDateFormat("dd MMM", Locale.ENGLISH)
  private val dateTimeFileFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.ENGLISH)

  fun formatInr(amount: Double, showDecimals: Boolean = false): String {
    val formatter = if (showDecimals) inrPreciseFormat else inrCurrencyFormat
    val formatted = formatter.format(abs(amount))
    return if (amount < 0) "-$formatted" else formatted
  }

  fun formatQty(qty: Double): String {
    return if (qty % 1.0 == 0.0) qty.toLong().toString()
    else String.format(Locale.ENGLISH, "%.2f", qty)
  }

  fun formatDate(millis: Long): String = dateFormat.format(Date(millis))

  fun formatShortDate(millis: Long): String = shortDateFormat.format(Date(millis))

  fun formatFileStamp(millis: Long = System.currentTimeMillis()): String =
    dateTimeFileFormat.format(Date(millis))

  fun isToday(millis: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = millis }
    val cal2 = Calendar.getInstance()
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
      cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
  }

  fun isYesterday(millis: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = millis }
    val cal2 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
      cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
  }

  fun startOfCurrentMonthMillis(): Long {
    return Calendar.getInstance()
      .apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
      .timeInMillis
  }
}
