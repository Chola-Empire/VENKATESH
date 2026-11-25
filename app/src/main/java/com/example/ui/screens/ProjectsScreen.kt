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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ClientEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectStatus
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.i18n.AppStrings
import com.example.ui.components.EmptyLedgerState
import com.example.ui.components.HairlineDivider
import com.example.ui.components.LedgerSurfaceCard
import com.example.ui.components.MetricCell
import com.example.ui.components.SelectorField
import com.example.ui.components.TransactionRow
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TabularAmountStyle
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AccountsUiState
import com.example.ui.viewmodel.ProjectSummary
import com.example.util.Formatters

@Composable
fun ProjectsScreen(
  state: AccountsUiState,
  selectedProjectId: String?,
  onSelectProject: (String?) -> Unit,
  onSaveProject: (String?, String, String?, String, Double, ProjectStatus, String) -> Unit,
  onDeleteProject: (String) -> Unit,
  onAddEntryForProject: (String, TransactionType) -> Unit,
  onEditTransaction: (TransactionEntity) -> Unit,
  onReceiptPdf: (TransactionEntity) -> Unit,
  onProjectInvoicePdf: (String) -> Unit,
) {
  val s = state.strings
  var editingProject by remember { mutableStateOf<ProjectEntity?>(null) }
  var showProjectDialog by remember { mutableStateOf(false) }

  val selectedSummary = state.projectSummaries.firstOrNull { it.project.id == selectedProjectId }

  if (selectedSummary != null) {
    ProjectDetailView(
      summary = selectedSummary,
      transactions = state.transactions.filter { it.projectId == selectedSummary.project.id },
      strings = s,
      onBack = { onSelectProject(null) },
      onEditProject = {
        editingProject = selectedSummary.project
        showProjectDialog = true
      },
      onDeleteProject = {
        onDeleteProject(selectedSummary.project.id)
        onSelectProject(null)
      },
      onAddEntry = { type -> onAddEntryForProject(selectedSummary.project.id, type) },
      onEditTransaction = onEditTransaction,
      onReceiptPdf = onReceiptPdf,
      onInvoicePdf = { onProjectInvoicePdf(selectedSummary.project.id) },
    )
  } else {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = s.projectSummary,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Button(
            onClick = {
              editingProject = null
              showProjectDialog = true
            }
          ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(s.addProject, maxLines = 1)
          }
        }
      }

      if (state.projectSummaries.isEmpty()) {
        item { EmptyLedgerState(s.noProjectsYet) }
      } else {
        items(state.projectSummaries, key = { it.project.id }) { summary ->
          val statusEnum =
            ProjectStatus.entries.firstOrNull { it.name == summary.project.status }
              ?: ProjectStatus.ACTIVE
          LedgerSurfaceCard(onClick = { onSelectProject(summary.project.id) }) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = summary.project.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  val meta =
                    buildList {
                        summary.client?.name?.let { add(it) }
                        if (summary.project.location.isNotBlank()) add(summary.project.location)
                        add(statusEnum.label(s))
                      }
                      .joinToString(" · ")
                  Text(
                    text = meta,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                IconButton(onClick = { onProjectInvoicePdf(summary.project.id) }) {
                  Icon(
                    imageVector = Icons.Outlined.PictureAsPdf,
                    contentDescription = s.generateInvoicePdf,
                    tint = MaterialTheme.colorScheme.primary,
                  )
                }
              }

              Spacer(modifier = Modifier.height(12.dp))
              HairlineDivider()
              Spacer(modifier = Modifier.height(12.dp))

              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCell(s.totalContract, summary.project.contractValue, modifier = Modifier.weight(1f))
                MetricCell(
                  s.totalReceived,
                  summary.totalIncome,
                  accentColor = IncomeGreen,
                  modifier = Modifier.weight(1f),
                )
              }
              Spacer(modifier = Modifier.height(10.dp))
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCell(
                  s.totalExpenses,
                  summary.totalExpense,
                  accentColor = ExpenseCrimson,
                  modifier = Modifier.weight(1f),
                )
                MetricCell(
                  s.pendingBalance,
                  summary.pendingFromCustomer,
                  accentColor = WarningAmber,
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }
        }
      }
    }
  }

  if (showProjectDialog) {
    ProjectEditorDialog(
      project = editingProject,
      clients = state.clients,
      strings = s,
      onDismiss = { showProjectDialog = false },
      onSave = { name, clientId, location, contractVal, status, notes ->
        onSaveProject(editingProject?.id, name, clientId, location, contractVal, status, notes)
        showProjectDialog = false
      },
    )
  }
}

@Composable
private fun ProjectDetailView(
  summary: ProjectSummary,
  transactions: List<TransactionEntity>,
  strings: AppStrings,
  onBack: () -> Unit,
  onEditProject: () -> Unit,
  onDeleteProject: () -> Unit,
  onAddEntry: (TransactionType) -> Unit,
  onEditTransaction: (TransactionEntity) -> Unit,
  onReceiptPdf: (TransactionEntity) -> Unit,
  onInvoicePdf: () -> Unit,
) {
  var showDeleteConfirm by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = strings.close)
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = summary.project.name,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text =
              listOfNotNull(summary.client?.name, summary.project.location.takeIf { it.isNotBlank() })
                .joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(onClick = onInvoicePdf) {
          Icon(
            Icons.Outlined.PictureAsPdf,
            contentDescription = strings.generateInvoicePdf,
            tint = MaterialTheme.colorScheme.primary,
          )
        }
        IconButton(onClick = onEditProject) {
          Icon(Icons.Outlined.Edit, contentDescription = strings.edit)
        }
        IconButton(onClick = { showDeleteConfirm = true }) {
          Icon(Icons.Outlined.Delete, contentDescription = strings.delete, tint = ExpenseCrimson)
        }
      }
    }

    item {
      LedgerSurfaceCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCell(strings.totalContract, summary.project.contractValue, modifier = Modifier.weight(1f))
            MetricCell(
              strings.pendingBalance,
              summary.pendingFromCustomer,
              accentColor = WarningAmber,
              modifier = Modifier.weight(1f),
            )
          }
          HairlineDivider()
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCell(
              strings.totalReceived,
              summary.totalIncome,
              accentColor = IncomeGreen,
              modifier = Modifier.weight(1f),
            )
            MetricCell(
              strings.totalExpenses,
              summary.totalExpense,
              accentColor = ExpenseCrimson,
              modifier = Modifier.weight(1f),
            )
          }
          HairlineDivider()
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(strings.netProfitLoss, style = MaterialTheme.typography.titleSmall)
            Text(
              text = Formatters.formatInr(summary.netProfitLoss),
              style = TabularAmountStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
              color = if (summary.netProfitLoss >= 0) IncomeGreen else ExpenseCrimson,
            )
          }
        }
      }
    }

    // Cost Head Breakdown for Project
    item {
      LedgerSurfaceCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(strings.expenseBreakdown, style = MaterialTheme.typography.titleMedium)
          HairlineDivider()
          BreakdownLine(strings.cement, summary.cementCost)
          BreakdownLine(strings.steel, summary.steelCost)
          BreakdownLine(strings.sand, summary.sandCost)
          BreakdownLine(strings.tabLabour, summary.labourCost)
          BreakdownLine(strings.tabDailySite, summary.siteExpenseCost)
        }
      }
    }

    // Quick Add Buttons for this Project
    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = { onAddEntry(TransactionType.INCOME_CUSTOMER_PAYMENT) },
          modifier = Modifier.weight(1f),
        ) {
          Text(strings.recordPayment, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Button(
          onClick = { onAddEntry(TransactionType.EXPENSE_MATERIAL) },
          modifier = Modifier.weight(1f),
        ) {
          Text(strings.addMaterial, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
      }
    }

    if (transactions.isEmpty()) {
      item { EmptyLedgerState(strings.noTransactionsYet) }
    } else {
      item {
        LedgerSurfaceCard {
          Column {
            transactions.forEachIndexed { idx, tx ->
              TransactionRow(
                transaction = tx,
                projectName = null,
                strings = strings,
                onClick = { onEditTransaction(tx) },
                onReceiptPdfClick =
                  if (tx.type == TransactionType.INCOME_CUSTOMER_PAYMENT.name) {
                    { onReceiptPdf(tx) }
                  } else null,
              )
              if (idx < transactions.lastIndex) HairlineDivider()
            }
          }
        }
      }
    }
  }

  if (showDeleteConfirm) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text(strings.delete) },
      text = { Text("${strings.delete} ${summary.project.name}?") },
      confirmButton = {
        TextButton(
          onClick = {
            showDeleteConfirm = false
            onDeleteProject()
          }
        ) {
          Text(strings.delete, color = ExpenseCrimson)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirm = false }) { Text(strings.cancel) }
      },
    )
  }
}

@Composable
private fun BreakdownLine(label: String, amount: Double) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(
      Formatters.formatInr(amount),
      style = TabularAmountStyle.copy(fontSize = 14.sp),
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Composable
private fun ProjectEditorDialog(
  project: ProjectEntity?,
  clients: List<ClientEntity>,
  strings: AppStrings,
  onDismiss: () -> Unit,
  onSave: (String, String?, String, Double, ProjectStatus, String) -> Unit,
) {
  var name by remember { mutableStateOf(project?.name.orEmpty()) }
  var clientId by remember { mutableStateOf(project?.clientId) }
  var location by remember { mutableStateOf(project?.location.orEmpty()) }
  var contractText by remember {
    mutableStateOf(if (project != null && project.contractValue > 0) project.contractValue.toLong().toString() else "")
  }
  var status by remember {
    mutableStateOf(
      ProjectStatus.entries.firstOrNull { it.name == project?.status } ?: ProjectStatus.ACTIVE
    )
  }
  var notes by remember { mutableStateOf(project?.notes.orEmpty()) }

  val clientOptions: List<Pair<String?, String>> =
    listOf(null to "—") + clients.map { it.id to it.name }
  val selectedClientLabel = clientOptions.firstOrNull { it.first == clientId }?.second ?: "—"

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (project == null) strings.addProject else strings.editProject) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(strings.projectName) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        SelectorField(
          label = strings.selectClient,
          selectedLabel = selectedClientLabel,
          options = clientOptions,
          onSelected = { clientId = it },
        )
        OutlinedTextField(
          value = location,
          onValueChange = { location = it },
          label = { Text(strings.location) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = contractText,
          onValueChange = { contractText = it.filter { ch -> ch.isDigit() || ch == '.' } },
          label = { Text(strings.contractValue) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        SelectorField(
          label = strings.status,
          selectedLabel = status.label(strings),
          options = ProjectStatus.entries.map { it to it.label(strings) },
          onSelected = { status = it },
        )
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text(strings.notes) },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        enabled = name.isNotBlank(),
        onClick = {
          onSave(
            name,
            clientId,
            location,
            contractText.toDoubleOrNull() ?: 0.0,
            status,
            notes,
          )
        },
      ) {
        Text(strings.save)
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } },
  )
}
