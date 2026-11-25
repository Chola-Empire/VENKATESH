package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.AccountCategory
import com.example.data.local.ClientEntity
import com.example.data.local.PaymentMode
import com.example.data.local.ProjectEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.i18n.AppStrings
import com.example.util.Formatters
import java.io.File
import java.io.FileOutputStream

object PdfDocumentGenerator {
  private const val PAGE_WIDTH = 595 // A4 width in PostScript points (72 dpi)
  private const val PAGE_HEIGHT = 842 // A4 height
  private const val MARGIN = 40f

  private val bronzeColor = Color.rgb(154, 52, 18)
  private val inkColor = Color.rgb(24, 24, 27)
  private val mutedColor = Color.rgb(100, 100, 110)
  private val borderColor = Color.rgb(220, 215, 205)
  private val headerBgColor = Color.rgb(247, 245, 240)
  private val greenColor = Color.rgb(15, 118, 110)
  private val redColor = Color.rgb(185, 28, 28)

  fun generatePaymentReceipt(
    context: Context,
    transaction: TransactionEntity,
    project: ProjectEntity?,
    client: ClientEntity?,
    totalPaidForProject: Double,
    strings: AppStrings,
  ): Uri {
    val doc = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
    val page = doc.startPage(pageInfo)
    val canvas = page.canvas

    var y = drawBrandHeader(canvas, strings.paymentReceipt.uppercase(), "REC-${transaction.id.takeLast(6).uppercase()}")

    y += 18f
    val labelPaint = paint(10f, mutedColor, false)
    val valuePaint = paint(12f, inkColor, true)

    drawKeyValueRow(canvas, MARGIN, y, "Date", Formatters.formatDate(transaction.dateMillis), labelPaint, valuePaint)
    drawKeyValueRow(canvas, 320f, y, "Receipt / Ref No", transaction.referenceNo.ifBlank { transaction.id.takeLast(8).uppercase() }, labelPaint, valuePaint)
    y += 42f

    drawKeyValueRow(canvas, MARGIN, y, "Received From (Client)", client?.name ?: transaction.partyName, labelPaint, valuePaint)
    drawKeyValueRow(
      canvas,
      320f,
      y,
      "Payment Mode",
      PaymentMode.entries.firstOrNull { it.name == transaction.paymentMode }?.label(strings) ?: transaction.paymentMode,
      labelPaint,
      valuePaint,
    )
    y += 42f

    if (project != null) {
      drawKeyValueRow(canvas, MARGIN, y, "Project", "${project.name} (${project.location})", labelPaint, valuePaint)
      val pending = (project.contractValue - totalPaidForProject).coerceAtLeast(0.0)
      drawKeyValueRow(canvas, 320f, y, "Remaining Contract Balance", Formatters.formatInr(pending, true), labelPaint, valuePaint)
      y += 48f
    }

    // Highlighted Amount Box
    val boxPaint = Paint().apply { color = headerBgColor }
    val boxStroke =
      Paint().apply {
        color = bronzeColor
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
      }
    canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 74f, boxPaint)
    canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 74f, boxStroke)

    canvas.drawText("AMOUNT RECEIVED", MARGIN + 16f, y + 24f, paint(10f, mutedColor, true))
    canvas.drawText(
      Formatters.formatInr(transaction.amount, true),
      MARGIN + 16f,
      y + 54f,
      paint(22f, greenColor, true),
    )
    y += 96f

    canvas.drawText("Description / Particulars", MARGIN, y, labelPaint)
    y += 16f
    canvas.drawText(transaction.title, MARGIN, y, paint(12f, inkColor, false))
    if (transaction.notes.isNotBlank()) {
      y += 18f
      canvas.drawText("Notes: ${transaction.notes}", MARGIN, y, paint(10f, mutedColor, false))
    }

    drawFooterSignature(canvas)
    doc.finishPage(page)

    return saveAndGetUri(context, doc, "Receipt_${Formatters.formatFileStamp()}.pdf")
  }

  fun generateProjectInvoice(
    context: Context,
    project: ProjectEntity,
    client: ClientEntity?,
    transactions: List<TransactionEntity>,
    strings: AppStrings,
  ): Uri {
    val doc = PdfDocument()
    var pageNumber = 1
    var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
    var canvas = page.canvas

    var y = drawBrandHeader(canvas, strings.projectStatementInvoice.uppercase(), "INV-${project.id.takeLast(6).uppercase()}")
    y += 16f

    val labelPaint = paint(10f, mutedColor, false)
    val valuePaint = paint(11.5f, inkColor, true)

    drawKeyValueRow(canvas, MARGIN, y, "Project", "${project.name} · ${project.location}", labelPaint, valuePaint)
    drawKeyValueRow(canvas, 330f, y, "Statement Date", Formatters.formatDate(System.currentTimeMillis()), labelPaint, valuePaint)
    y += 38f

    drawKeyValueRow(canvas, MARGIN, y, "Bill To (Client)", client?.name ?: "Walk-in / Direct Client", labelPaint, valuePaint)
    if (!client?.phone.isNullOrBlank()) {
      drawKeyValueRow(canvas, 330f, y, "Contact", client?.phone.orEmpty(), labelPaint, valuePaint)
    }
    y += 38f

    if (!client?.address.isNullOrBlank()) {
      canvas.drawText("Address: ${client?.address}", MARGIN, y, paint(10f, mutedColor, false))
      y += 16f
    }
    if (!client?.gstNumber.isNullOrBlank()) {
      canvas.drawText("Client GSTIN: ${client?.gstNumber}", MARGIN, y, paint(10f, mutedColor, false))
      y += 16f
    }

    val totalReceived =
      transactions.filter { it.type == TransactionType.INCOME_CUSTOMER_PAYMENT.name }.sumOf { it.amount }
    val totalExpenses =
      transactions.filter { it.type != TransactionType.INCOME_CUSTOMER_PAYMENT.name }.sumOf { it.amount }
    val pending = (project.contractValue - totalReceived).coerceAtLeast(0.0)

    y += 8f
    val summaryBg = Paint().apply { color = headerBgColor }
    canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 54f, summaryBg)
    drawKeyValueRow(canvas, MARGIN + 12f, y + 18f, "Contract Value", Formatters.formatInr(project.contractValue), labelPaint, valuePaint)
    drawKeyValueRow(canvas, MARGIN + 185f, y + 18f, "Total Received", Formatters.formatInr(totalReceived), labelPaint, paint(11.5f, greenColor, true))
    drawKeyValueRow(canvas, MARGIN + 355f, y + 18f, "Pending Balance", Formatters.formatInr(pending), labelPaint, paint(11.5f, redColor, true))
    y += 72f

    // Table Header
    y = drawTableHeader(canvas, y)
    val rowPaint = paint(9.5f, inkColor, false)
    val rowBold = paint(9.5f, inkColor, true)
    val linePaint = Paint().apply { color = borderColor; strokeWidth = 0.8f }

    val sorted = transactions.sortedBy { it.dateMillis }
    for (tx in sorted) {
      if (y > PAGE_HEIGHT - 90f) {
        doc.finishPage(page)
        pageNumber++
        page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        canvas = page.canvas
        y = 48f
        y = drawTableHeader(canvas, y)
      }
      val catLabel = AccountCategory.entries.firstOrNull { it.name == tx.category }?.label(strings) ?: tx.category
      val isIncome = tx.type == TransactionType.INCOME_CUSTOMER_PAYMENT.name
      val amountText = (if (isIncome) "+ " else "- ") + Formatters.formatInr(tx.amount)
      val amountPaint = paint(9.5f, if (isIncome) greenColor else inkColor, true)

      canvas.drawText(Formatters.formatShortDate(tx.dateMillis), MARGIN + 4f, y + 14f, rowPaint)
      canvas.drawText(catLabel.take(18), MARGIN + 65f, y + 14f, rowPaint)
      canvas.drawText(tx.title.take(30), MARGIN + 175f, y + 14f, rowBold)
      canvas.drawText(tx.partyName.take(16), MARGIN + 345f, y + 14f, rowPaint)
      canvas.drawText(amountText, PAGE_WIDTH - MARGIN - amountPaint.measureText(amountText) - 4f, y + 14f, amountPaint)
      canvas.drawLine(MARGIN, y + 22f, PAGE_WIDTH - MARGIN, y + 22f, linePaint)
      y += 24f
    }

    y += 14f
    if (y > PAGE_HEIGHT - 110f) {
      doc.finishPage(page)
      pageNumber++
      page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
      canvas = page.canvas
      y = 56f
    }
    canvas.drawText(
      "Site Cost Incurred: ${Formatters.formatInr(totalExpenses)}   |   Net Project Cashflow: ${Formatters.formatInr(totalReceived - totalExpenses)}",
      MARGIN,
      y,
      paint(10f, mutedColor, true),
    )

    drawFooterSignature(canvas)
    doc.finishPage(page)
    return saveAndGetUri(context, doc, "Invoice_${project.name.replace(" ", "_")}_${Formatters.formatFileStamp()}.pdf")
  }

  fun generateProfitLossPdf(
    context: Context,
    titleScope: String,
    dateScope: String,
    totalIncome: Double,
    cementCost: Double,
    steelCost: Double,
    sandCost: Double,
    otherMaterialCost: Double,
    labourCost: Double,
    siteExpenseCost: Double,
    strings: AppStrings,
  ): Uri {
    val doc = PdfDocument()
    val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
    val canvas = page.canvas

    var y = drawBrandHeader(canvas, strings.profitLossReport.uppercase(), "P&L-${Formatters.formatFileStamp()}")
    y += 16f

    canvas.drawText("Scope: $titleScope   ·   Period: $dateScope", MARGIN, y, paint(11f, mutedColor, true))
    y += 28f

    val totalExpense = cementCost + steelCost + sandCost + otherMaterialCost + labourCost + siteExpenseCost
    val netProfit = totalIncome - totalExpense
    val margin = if (totalIncome > 0) (netProfit / totalIncome) * 100.0 else 0.0

    val rows =
      listOf(
        "Total Customer Payments Received (Revenue)" to totalIncome,
        "Material — Cement" to -cementCost,
        "Material — Steel / TMT" to -steelCost,
        "Material — Sand / M-Sand" to -sandCost,
        "Material — Bricks, Jelly & Others" to -otherMaterialCost,
        "Labour & Contractor Payments" to -labourCost,
        "Daily Site Expenses" to -siteExpenseCost,
      )

    val linePaint = Paint().apply { color = borderColor; strokeWidth = 1f }
    val textPaint = paint(11f, inkColor, false)
    val boldPaint = paint(11f, inkColor, true)

    for ((label, value) in rows) {
      canvas.drawText(label, MARGIN + 6f, y + 18f, textPaint)
      val amtStr = Formatters.formatInr(value, true)
      val valPaint = paint(11f, if (value >= 0) greenColor else inkColor, true)
      canvas.drawText(amtStr, PAGE_WIDTH - MARGIN - valPaint.measureText(amtStr) - 6f, y + 18f, valPaint)
      canvas.drawLine(MARGIN, y + 28f, PAGE_WIDTH - MARGIN, y + 28f, linePaint)
      y += 32f
    }

    y += 14f
    val bgPaint = Paint().apply { color = headerBgColor }
    canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 66f, bgPaint)
    canvas.drawText("TOTAL EXPENSES", MARGIN + 14f, y + 24f, paint(10f, mutedColor, true))
    canvas.drawText(Formatters.formatInr(totalExpense, true), MARGIN + 14f, y + 48f, boldPaint)

    val netLabel = if (netProfit >= 0) "NET PROFIT (${String.format(java.util.Locale.ENGLISH, "%.1f", margin)}%)" else "NET LOSS"
    val netColor = if (netProfit >= 0) greenColor else redColor
    canvas.drawText(netLabel, 320f, y + 24f, paint(10f, mutedColor, true))
    canvas.drawText(Formatters.formatInr(netProfit, true), 320f, y + 48f, paint(18f, netColor, true))

    drawFooterSignature(canvas)
    doc.finishPage(page)
    return saveAndGetUri(context, doc, "PnL_Report_${Formatters.formatFileStamp()}.pdf")
  }

  private fun drawBrandHeader(canvas: Canvas, docTitle: String, docNo: String): Float {
    val topPaint = Paint().apply { color = bronzeColor }
    canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 10f, topPaint)

    canvas.drawText("CHOLA EMPIRE CONSTRUCTION", MARGIN, 48f, paint(18f, bronzeColor, true))
    canvas.drawText("Accounts, Material & Site Engineering Ledger", MARGIN, 66f, paint(10f, mutedColor, false))

    val titlePaint = paint(13f, inkColor, true)
    canvas.drawText(docTitle, PAGE_WIDTH - MARGIN - titlePaint.measureText(docTitle), 48f, titlePaint)
    val noPaint = paint(10f, mutedColor, false)
    canvas.drawText("Ref: $docNo", PAGE_WIDTH - MARGIN - noPaint.measureText("Ref: $docNo"), 65f, noPaint)

    val divider = Paint().apply { color = borderColor; strokeWidth = 1.2f }
    canvas.drawLine(MARGIN, 80f, PAGE_WIDTH - MARGIN, 80f, divider)
    return 86f
  }

  private fun drawTableHeader(canvas: Canvas, y: Float): Float {
    val bg = Paint().apply { color = headerBgColor }
    canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 22f, bg)
    val p = paint(9.5f, mutedColor, true)
    canvas.drawText("DATE", MARGIN + 4f, y + 15f, p)
    canvas.drawText("CATEGORY", MARGIN + 65f, y + 15f, p)
    canvas.drawText("PARTICULARS", MARGIN + 175f, y + 15f, p)
    canvas.drawText("PARTY", MARGIN + 345f, y + 15f, p)
    val amt = "AMOUNT (INR)"
    canvas.drawText(amt, PAGE_WIDTH - MARGIN - p.measureText(amt) - 4f, y + 15f, p)
    return y + 26f
  }

  private fun drawKeyValueRow(
    canvas: Canvas,
    x: Float,
    y: Float,
    label: String,
    value: String,
    labelPaint: Paint,
    valuePaint: Paint,
  ) {
    canvas.drawText(label, x, y, labelPaint)
    canvas.drawText(value, x, y + 16f, valuePaint)
  }

  private fun drawFooterSignature(canvas: Canvas) {
    val linePaint = Paint().apply { color = borderColor; strokeWidth = 1f }
    canvas.drawLine(MARGIN, PAGE_HEIGHT - 64f, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 64f, linePaint)
    canvas.drawText(
      "Generated by Chola Empire Construction Accounts",
      MARGIN,
      PAGE_HEIGHT - 44f,
      paint(9f, mutedColor, false),
    )
    val sig = "Authorized Signatory"
    val sigPaint = paint(10f, inkColor, true)
    canvas.drawText(sig, PAGE_WIDTH - MARGIN - sigPaint.measureText(sig), PAGE_HEIGHT - 44f, sigPaint)
  }

  private fun paint(size: Float, color: Int, bold: Boolean): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      textSize = size
      this.color = color
      typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
    }

  private fun saveAndGetUri(context: Context, doc: PdfDocument, fileName: String): Uri {
    val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
    val outFile = File(dir, fileName)
    FileOutputStream(outFile).use { doc.writeTo(it) }
    doc.close()
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
  }

  fun openOrSharePdf(context: Context, uri: Uri, title: String) {
    val viewIntent =
      Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    val shareIntent =
      Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    val chooser =
      Intent.createChooser(shareIntent, title).apply {
        putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(viewIntent))
      }
    context.startActivity(chooser)
  }
}
