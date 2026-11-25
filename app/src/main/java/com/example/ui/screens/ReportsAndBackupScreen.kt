package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AccountCategory
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.i18n.AppLanguage
import com.example.ui.components.HairlineDivider
import com.example.ui.components.LedgerSurfaceCard
import com.example.ui.components.MetricCell
import com.example.ui.components.ProjectFilterBar
import com.example.ui.components.SegmentedFilterButton
import com.example.ui.theme.CholaBronze
import com.example.ui.theme.CholaGold
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TabularAmountStyle
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AccountsUiState
import com.example.ui.viewmodel.DateFilter
import com.example.util.Formatters

@Composable
fun ReportsAndBackupScreen(
  state: AccountsUiState,
  onProjectFilterChange: (String?) -> Unit,
  onDateFilterChange: (DateFilter) -> Unit,
  onLanguageChange: (AppLanguage) -> Unit,
  onExportProfitLossPdf: () -> Unit,
  onExportJsonBackup: () -> Unit,
  onRestoreJsonBackup: () -> Unit,
  onExportCsvLedger: () -> Unit,
  onClearAllData: () -> Unit,
  filterFn: (List<TransactionEntity>, String?, DateFilter, String) -> List<TransactionEntity>,
) {
  val s = state.strings
  var showClearConfirm by remember { mutableStateOf(false) }

  val scoped = filterFn(state.transactions, state.selectedProjectIdFilter, state.dateFilter, "")
  val income = scoped.filter { it.type == TransactionType.INCOME_CUSTOMER_PAYMENT.name }.sumOf { it.amount }
  val cement = scoped.filter { it.category == AccountCategory.CEMENT.name }.sumOf { it.amount }
  val steel = scoped.filter { it.category == AccountCategory.STEEL.name }.sumOf { it.amount }
  val sand = scoped.filter { it.category == AccountCategory.SAND.name }.sumOf { it.amount }
  val otherMat =
    scoped
      .filter {
        it.type == TransactionType.EXPENSE_MATERIAL.name &&
          it.category !in
            listOf(AccountCategory.CEMENT.name, AccountCategory.STEEL.name, AccountCategory.SAND.name)
      }
      .sumOf { it.amount }
  val labour = scoped.filter { it.type == TransactionType.EXPENSE_LABOUR.name }.sumOf { it.amount }
  val site = scoped.filter { it.type == TransactionType.EXPENSE_SITE.name }.sumOf { it.amount }
  val totalExpense = cement + steel + sand + otherMat + labour + site
  val netProfit = income - totalExpense
  val marginPct = if (income > 0) (netProfit / income) * 100.0 else 0.0

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(s.profitLossReport, style = MaterialTheme.typography.titleLarge)
        Button(onClick = onExportProfitLossPdf) {
          Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(s.exportReportPdf, maxLines = 1)
        }
      }
    }

    // Period Filter
    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SegmentedFilterButton(
          text = s.dateRangeAll,
          selected = state.dateFilter == DateFilter.ALL_TIME,
          onClick = { onDateFilterChange(DateFilter.ALL_TIME) },
          modifier = Modifier.weight(1f),
        )
        SegmentedFilterButton(
          text = s.dateRangeMonth,
          selected = state.dateFilter == DateFilter.THIS_MONTH,
          onClick = { onDateFilterChange(DateFilter.THIS_MONTH) },
          modifier = Modifier.weight(1f),
        )
        SegmentedFilterButton(
          text = s.dateRange30Days,
          selected = state.dateFilter == DateFilter.LAST_30_DAYS,
          onClick = { onDateFilterChange(DateFilter.LAST_30_DAYS) },
          modifier = Modifier.weight(1f),
        )
      }
    }

    // Project Scope Filter
    item {
      ProjectFilterBar(
        projects = state.projects,
        selectedProjectId = state.selectedProjectIdFilter,
        allLabel = s.filterAllProjects,
        onSelect = onProjectFilterChange,
      )
    }

    // P&L Summary Card
    item {
      LedgerSurfaceCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
              Text(
                if (netProfit >= 0) s.profit else s.loss,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                Formatters.formatInr(netProfit),
                style = TabularAmountStyle.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                color = if (netProfit >= 0) IncomeGreen else ExpenseCrimson,
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                s.profitMargin,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                "${String.format(java.util.Locale.ENGLISH, "%.1f", marginPct)}%",
                style = TabularAmountStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                color = if (netProfit >= 0) IncomeGreen else ExpenseCrimson,
              )
            }
          }
          HairlineDivider()
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCell(s.income, income, accentColor = IncomeGreen, modifier = Modifier.weight(1f))
            MetricCell(s.expense, totalExpense, accentColor = ExpenseCrimson, modifier = Modifier.weight(1f))
          }
        }
      }
    }

    // Cost Breakdown Bars
    item {
      val costItems =
        listOf(
          Triple(s.cement, cement, CholaBronze),
          Triple(s.steel, steel, Color(0xFF334155)),
          Triple(s.sand, sand, CholaGold),
          Triple(s.otherMaterial, otherMat, Color(0xFF6B7280)),
          Triple(s.tabLabour, labour, ExpenseCrimson),
          Triple(s.tabDailySite, site, WarningAmber),
        )
      LedgerSurfaceCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(s.expenseBreakdown, style = MaterialTheme.typography.titleMedium)
          HairlineDivider()
          costItems.forEach { (label, amt, barColor) ->
            val ratio = if (totalExpense > 0) (amt / totalExpense).toFloat().coerceIn(0f, 1f) else 0f
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                  Formatters.formatInr(amt),
                  style = TabularAmountStyle.copy(fontSize = 13.sp),
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
              Box(
                modifier =
                  Modifier.fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
              ) {
                if (ratio > 0f) {
                  Box(
                    modifier =
                      Modifier.fillMaxWidth(ratio)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(barColor)
                  )
                }
              }
            }
          }
        }
      }
    }

    // Language Section
    item {
      LedgerSurfaceCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(s.languageSection, style = MaterialTheme.typography.titleMedium)
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SegmentedFilterButton(
              text = AppLanguage.ENGLISH.displayName,
              selected = state.language == AppLanguage.ENGLISH,
              onClick = { onLanguageChange(AppLanguage.ENGLISH) },
              modifier = Modifier.weight(1f),
            )
            SegmentedFilterButton(
              text = AppLanguage.TAMIL.displayName,
              selected = state.language == AppLanguage.TAMIL,
              onClick = { onLanguageChange(AppLanguage.TAMIL) },
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }

    // Cloud Backup + Offline Queue & Export/Restore
    item {
      LedgerSurfaceCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = if (state.isOnline) Icons.Outlined.CloudQueue else Icons.Outlined.CloudOff,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(s.cloudBackupTitle, style = MaterialTheme.typography.titleMedium)
          }
          Text(
            text = s.cloudBackupInfo,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "${s.localEntriesQueued}: ${state.transactions.size}",
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.primary,
          )
          HairlineDivider()
          Text(s.backupAndRestore, style = MaterialTheme.typography.titleSmall)
          Button(onClick = onExportJsonBackup, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(s.exportJsonBackup)
          }
          OutlinedButton(onClick = onRestoreJsonBackup, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(s.restoreJsonBackup)
          }
          OutlinedButton(onClick = onExportCsvLedger, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(s.exportCsvLedger)
          }
          TextButton(onClick = { showClearConfirm = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(
              Icons.Outlined.DeleteForever,
              contentDescription = null,
              tint = ExpenseCrimson,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(s.clearAllData, color = ExpenseCrimson)
          }
        }
      }
    }
  }

  if (showClearConfirm) {
    AlertDialog(
      onDismissRequest = { showClearConfirm = false },
      title = { Text(s.confirmClearTitle) },
      text = { Text(s.confirmClearMessage) },
      confirmButton = {
        TextButton(
          onClick = {
            showClearConfirm = false
            onClearAllData()
          }
        ) {
          Text(s.delete, color = ExpenseCrimson)
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfirm = false }) { Text(s.cancel) }
      },
    )
  }
}
