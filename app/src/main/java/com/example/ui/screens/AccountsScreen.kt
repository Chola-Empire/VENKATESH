package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AccountCategory
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.ui.components.EmptyLedgerState
import com.example.ui.components.HairlineDivider
import com.example.ui.components.LedgerSurfaceCard
import com.example.ui.components.ProjectFilterBar
import com.example.ui.components.SegmentedFilterButton
import com.example.ui.components.TransactionRow
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.TabularAmountStyle
import com.example.ui.viewmodel.AccountsSubTab
import com.example.ui.viewmodel.AccountsUiState
import com.example.ui.viewmodel.DateFilter
import com.example.util.Formatters

@Composable
fun AccountsScreen(
  state: AccountsUiState,
  activeSubTab: AccountsSubTab,
  onSelectSubTab: (AccountsSubTab) -> Unit,
  onProjectFilterChange: (String?) -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onQuickAddForSubTab: (AccountsSubTab) -> Unit,
  onEditTransaction: (TransactionEntity) -> Unit,
  onReceiptPdf: (TransactionEntity) -> Unit,
  filterFn: (List<TransactionEntity>, String?, DateFilter, String) -> List<TransactionEntity>,
) {
  val s = state.strings
  val projectNames = state.projects.associate { it.id to it.name }

  val baseFiltered =
    filterFn(
      state.transactions,
      state.selectedProjectIdFilter,
      DateFilter.ALL_TIME,
      state.searchQuery,
    )

  val tabFiltered =
    when (activeSubTab) {
      AccountsSubTab.ALL_LEDGER -> baseFiltered
      AccountsSubTab.MATERIALS ->
        baseFiltered.filter { it.type == TransactionType.EXPENSE_MATERIAL.name }
      AccountsSubTab.LABOUR ->
        baseFiltered.filter { it.type == TransactionType.EXPENSE_LABOUR.name }
      AccountsSubTab.DAILY_SITE ->
        baseFiltered.filter { it.type == TransactionType.EXPENSE_SITE.name }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Sub-tab segmented strip
    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SegmentedFilterButton(
          text = s.tabAllLedger,
          selected = activeSubTab == AccountsSubTab.ALL_LEDGER,
          onClick = { onSelectSubTab(AccountsSubTab.ALL_LEDGER) },
          modifier = Modifier.weight(1f),
        )
        SegmentedFilterButton(
          text = s.tabMaterials,
          selected = activeSubTab == AccountsSubTab.MATERIALS,
          onClick = { onSelectSubTab(AccountsSubTab.MATERIALS) },
          modifier = Modifier.weight(1f),
        )
        SegmentedFilterButton(
          text = s.tabLabour,
          selected = activeSubTab == AccountsSubTab.LABOUR,
          onClick = { onSelectSubTab(AccountsSubTab.LABOUR) },
          modifier = Modifier.weight(1f),
        )
        SegmentedFilterButton(
          text = s.tabDailySite,
          selected = activeSubTab == AccountsSubTab.DAILY_SITE,
          onClick = { onSelectSubTab(AccountsSubTab.DAILY_SITE) },
          modifier = Modifier.weight(1f),
        )
      }
    }

    // Search + Add button row
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = state.searchQuery,
          onValueChange = onSearchQueryChange,
          placeholder = { Text(s.searchPlaceholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
          leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
          singleLine = true,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.weight(1f),
        )
        Button(
          onClick = { onQuickAddForSubTab(activeSubTab) },
          modifier = Modifier.height(54.dp),
          shape = RoundedCornerShape(10.dp),
        ) {
          Icon(Icons.Outlined.Add, contentDescription = s.quickEntry, modifier = Modifier.size(20.dp))
        }
      }
    }

    // Project filter chips
    item {
      ProjectFilterBar(
        projects = state.projects,
        selectedProjectId = state.selectedProjectIdFilter,
        allLabel = s.filterAllProjects,
        onSelect = onProjectFilterChange,
      )
    }

    // Dedicated Material Accounts: Cement, Steel, Sand cards
    if (activeSubTab == AccountsSubTab.MATERIALS) {
      item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          state.materialMetrics.forEach { metric ->
            LedgerSurfaceCard(modifier = Modifier.weight(1f)) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = metric.category.label(s),
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = Formatters.formatInr(metric.totalSpend),
                  style = TabularAmountStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "${Formatters.formatQty(metric.totalQuantity)} ${metric.unitLabel}",
                  style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                )
              }
            }
          }
        }
      }
    }

    // Dedicated Labour & Contractor Summary
    if (activeSubTab == AccountsSubTab.LABOUR) {
      val labourTotal = tabFiltered.sumOf { it.amount }
      val contractorTotal =
        tabFiltered.filter { it.category == AccountCategory.CONTRACTOR.name }.sumOf { it.amount }
      val wagesTotal = labourTotal - contractorTotal
      item {
        LedgerSurfaceCard {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column {
              Text(s.tabLabour, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(
                Formatters.formatInr(wagesTotal),
                style = TabularAmountStyle.copy(fontSize = 17.sp),
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(s.contractor, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(
                Formatters.formatInr(contractorTotal),
                style = TabularAmountStyle.copy(fontSize = 17.sp),
                color = ExpenseCrimson,
              )
            }
          }
        }
      }
    }

    // Daily Site Expenses grouped by date
    if (activeSubTab == AccountsSubTab.DAILY_SITE) {
      if (tabFiltered.isEmpty()) {
        item { EmptyLedgerState(s.noTransactionsYet) }
      } else {
        val grouped =
          tabFiltered.groupBy { tx ->
            when {
              Formatters.isToday(tx.dateMillis) -> s.today
              Formatters.isYesterday(tx.dateMillis) -> s.yesterday
              else -> Formatters.formatDate(tx.dateMillis)
            }
          }
        grouped.forEach { (dateLabel, entries) ->
          item {
            Row(
              modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(dateLabel, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(
                Formatters.formatInr(entries.sumOf { it.amount }),
                style = TabularAmountStyle.copy(fontSize = 14.sp),
                color = ExpenseCrimson,
              )
            }
          }
          item {
            LedgerSurfaceCard {
              Column {
                entries.forEachIndexed { idx, tx ->
                  TransactionRow(
                    transaction = tx,
                    projectName = projectNames[tx.projectId],
                    strings = s,
                    onClick = { onEditTransaction(tx) },
                  )
                  if (idx < entries.lastIndex) HairlineDivider()
                }
              }
            }
          }
        }
      }
    } else {
      if (tabFiltered.isEmpty()) {
        item { EmptyLedgerState(s.noTransactionsYet) }
      } else {
        item {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          ) {
            Column {
              tabFiltered.forEachIndexed { idx, tx ->
                TransactionRow(
                  transaction = tx,
                  projectName = projectNames[tx.projectId],
                  strings = s,
                  onClick = { onEditTransaction(tx) },
                  onReceiptPdfClick =
                    if (tx.type == TransactionType.INCOME_CUSTOMER_PAYMENT.name) {
                      { onReceiptPdf(tx) }
                    } else null,
                )
                if (idx < tabFiltered.lastIndex) HairlineDivider()
              }
            }
          }
        }
      }
    }
  }
}
