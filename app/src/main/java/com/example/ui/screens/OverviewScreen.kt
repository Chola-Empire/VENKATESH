package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.ui.components.EmptyLedgerState
import com.example.ui.components.HairlineDivider
import com.example.ui.components.LedgerSurfaceCard
import com.example.ui.components.MetricCell
import com.example.ui.components.TransactionRow
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TabularAmountStyle
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AccountsUiState
import com.example.ui.viewmodel.ProjectSummary
import com.example.util.Formatters

@Composable
fun OverviewScreen(
  state: AccountsUiState,
  onQuickEntry: (TransactionType) -> Unit,
  onOpenProject: (ProjectSummary) -> Unit,
  onSeeAllProjects: () -> Unit,
  onEditTransaction: (TransactionEntity) -> Unit,
  onReceiptPdf: (TransactionEntity) -> Unit,
) {
  val s = state.strings
  val projectNames = state.projects.associate { it.id to it.name }
  val recent = state.transactions.take(8)

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Primary Financial Anchor
    item {
      LedgerSurfaceCard {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = s.netProfitLoss,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = Formatters.formatInr(state.netProfitLoss),
            style = TabularAmountStyle.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold),
            color = if (state.netProfitLoss >= 0) IncomeGreen else ExpenseCrimson,
          )
          Spacer(modifier = Modifier.height(14.dp))
          HairlineDivider()
          Spacer(modifier = Modifier.height(14.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCell(
              label = s.totalReceived,
              amount = state.totalReceived,
              accentColor = IncomeGreen,
              modifier = Modifier.weight(1f),
            )
            MetricCell(
              label = s.totalExpenses,
              amount = state.totalExpenses,
              accentColor = ExpenseCrimson,
              modifier = Modifier.weight(1f),
            )
          }
          Spacer(modifier = Modifier.height(12.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCell(
              label = s.totalContract,
              amount = state.totalContractValue,
              modifier = Modifier.weight(1f),
            )
            MetricCell(
              label = s.pendingBalance,
              amount = state.totalPendingBalance,
              accentColor = WarningAmber,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }

    // Quick Entry Grid (2x2)
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = s.quickActions,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          QuickActionTile(
            icon = Icons.Outlined.AccountBalanceWallet,
            label = s.addCustomerPayment,
            onClick = { onQuickEntry(TransactionType.INCOME_CUSTOMER_PAYMENT) },
            modifier = Modifier.weight(1f),
          )
          QuickActionTile(
            icon = Icons.Outlined.Construction,
            label = s.addMaterial,
            onClick = { onQuickEntry(TransactionType.EXPENSE_MATERIAL) },
            modifier = Modifier.weight(1f),
          )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          QuickActionTile(
            icon = Icons.Outlined.Engineering,
            label = s.addLabour,
            onClick = { onQuickEntry(TransactionType.EXPENSE_LABOUR) },
            modifier = Modifier.weight(1f),
          )
          QuickActionTile(
            icon = Icons.Outlined.ReceiptLong,
            label = s.addSiteExpense,
            onClick = { onQuickEntry(TransactionType.EXPENSE_SITE) },
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    // Active Projects Snapshot
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = s.activeProjects,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        if (state.projectSummaries.isNotEmpty()) {
          TextButton(onClick = onSeeAllProjects) { Text(s.navProjects) }
        }
      }
    }

    if (state.projectSummaries.isEmpty()) {
      item { EmptyLedgerState(s.noProjectsYet) }
    } else {
      items(state.projectSummaries.take(3), key = { it.project.id }) { summary ->
        LedgerSurfaceCard(onClick = { onOpenProject(summary) }) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = summary.project.name,
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text =
                    listOfNotNull(summary.client?.name, summary.project.location.takeIf { it.isNotBlank() })
                      .joinToString(" · "),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = s.pendingBalance,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = Formatters.formatInr(summary.pendingFromCustomer),
                  style = TabularAmountStyle.copy(fontSize = 15.sp),
                  color = WarningAmber,
                )
              }
            }
            Spacer(modifier = Modifier.height(10.dp))
            HairlineDivider()
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text(
                text = "${s.income}: ${Formatters.formatInr(summary.totalIncome)}",
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                color = IncomeGreen,
              )
              Text(
                text = "${s.expense}: ${Formatters.formatInr(summary.totalExpense)}",
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                color = ExpenseCrimson,
              )
            }
          }
        }
      }
    }

    // Recent Ledger Entries
    item {
      Text(
        text = s.recentTransactions,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
      )
    }

    if (recent.isEmpty()) {
      item { EmptyLedgerState(s.noTransactionsYet) }
    } else {
      item {
        LedgerSurfaceCard {
          Column {
            recent.forEachIndexed { idx, tx ->
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
              if (idx < recent.lastIndex) {
                HairlineDivider()
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun QuickActionTile(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.height(54.dp).clickable(onClick = onClick),
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
