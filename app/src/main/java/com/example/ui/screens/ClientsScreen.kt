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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ClientEntity
import com.example.data.local.TransactionEntity
import com.example.i18n.AppStrings
import com.example.ui.components.EmptyLedgerState
import com.example.ui.components.HairlineDivider
import com.example.ui.components.LedgerSurfaceCard
import com.example.ui.components.MetricCell
import com.example.ui.components.TransactionRow
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AccountsUiState
import com.example.ui.viewmodel.ClientSummary

@Composable
fun ClientsScreen(
  state: AccountsUiState,
  selectedClientId: String?,
  onSelectClient: (String?) -> Unit,
  onSaveClient: (String?, String, String, String, String, String) -> Unit,
  onDeleteClient: (String) -> Unit,
  onRecordPaymentForClient: (ClientSummary) -> Unit,
  onEditTransaction: (TransactionEntity) -> Unit,
  onReceiptPdf: (TransactionEntity) -> Unit,
  onProjectInvoicePdf: (String) -> Unit,
) {
  val s = state.strings
  var editingClient by remember { mutableStateOf<ClientEntity?>(null) }
  var showClientDialog by remember { mutableStateOf(false) }

  val selectedSummary = state.clientSummaries.firstOrNull { it.client.id == selectedClientId }

  if (selectedSummary != null) {
    ClientLedgerDetailView(
      summary = selectedSummary,
      strings = s,
      onBack = { onSelectClient(null) },
      onEditClient = {
        editingClient = selectedSummary.client
        showClientDialog = true
      },
      onDeleteClient = {
        onDeleteClient(selectedSummary.client.id)
        onSelectClient(null)
      },
      onRecordPayment = { onRecordPaymentForClient(selectedSummary) },
      onEditTransaction = onEditTransaction,
      onReceiptPdf = onReceiptPdf,
      onProjectInvoicePdf = onProjectInvoicePdf,
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
            text = s.clientLedger,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Button(
            onClick = {
              editingClient = null
              showClientDialog = true
            }
          ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(s.addClient, maxLines = 1)
          }
        }
      }

      if (state.clientSummaries.isEmpty()) {
        item { EmptyLedgerState(s.noClientsYet) }
      } else {
        items(state.clientSummaries, key = { it.client.id }) { summary ->
          LedgerSurfaceCard(onClick = { onSelectClient(summary.client.id) }) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = summary.client.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  val sub =
                    listOfNotNull(
                        summary.client.phone.takeIf { it.isNotBlank() },
                        summary.projects.joinToString(", ") { it.name }.takeIf { it.isNotBlank() },
                      )
                      .joinToString(" · ")
                  if (sub.isNotBlank()) {
                    Text(
                      text = sub,
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
                OutlinedButton(onClick = { onRecordPaymentForClient(summary) }) {
                  Text(s.recordPayment, maxLines = 1)
                }
              }
              Spacer(modifier = Modifier.height(12.dp))
              HairlineDivider()
              Spacer(modifier = Modifier.height(12.dp))
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCell(s.billedContract, summary.totalContractValue, modifier = Modifier.weight(1f))
                MetricCell(
                  s.receivedFromClient,
                  summary.totalPaid,
                  accentColor = IncomeGreen,
                  modifier = Modifier.weight(1f),
                )
                MetricCell(
                  s.pendingBalance,
                  summary.pendingBalance,
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

  if (showClientDialog) {
    ClientEditorDialog(
      client = editingClient,
      strings = s,
      onDismiss = { showClientDialog = false },
      onSave = { name, phone, address, gst, notes ->
        onSaveClient(editingClient?.id, name, phone, address, gst, notes)
        showClientDialog = false
      },
    )
  }
}

@Composable
private fun ClientLedgerDetailView(
  summary: ClientSummary,
  strings: AppStrings,
  onBack: () -> Unit,
  onEditClient: () -> Unit,
  onDeleteClient: () -> Unit,
  onRecordPayment: () -> Unit,
  onEditTransaction: (TransactionEntity) -> Unit,
  onReceiptPdf: (TransactionEntity) -> Unit,
  onProjectInvoicePdf: (String) -> Unit,
) {
  var showDeleteConfirm by remember { mutableStateOf(false) }
  val projectNames = summary.projects.associate { it.id to it.name }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = strings.close)
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(summary.client.name, style = MaterialTheme.typography.titleLarge)
          if (summary.client.phone.isNotBlank() || summary.client.address.isNotBlank()) {
            Text(
              text =
                listOfNotNull(
                    summary.client.phone.takeIf { it.isNotBlank() },
                    summary.client.address.takeIf { it.isNotBlank() },
                  )
                  .joinToString(" · "),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 2,
            )
          }
        }
        IconButton(onClick = onEditClient) {
          Icon(Icons.Outlined.Edit, contentDescription = strings.edit)
        }
        IconButton(onClick = { showDeleteConfirm = true }) {
          Icon(Icons.Outlined.Delete, contentDescription = strings.delete, tint = ExpenseCrimson)
        }
      }
    }

    item {
      LedgerSurfaceCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCell(strings.billedContract, summary.totalContractValue, modifier = Modifier.weight(1f))
            MetricCell(
              strings.receivedFromClient,
              summary.totalPaid,
              accentColor = IncomeGreen,
              modifier = Modifier.weight(1f),
            )
            MetricCell(
              strings.pendingBalance,
              summary.pendingBalance,
              accentColor = WarningAmber,
              modifier = Modifier.weight(1f),
            )
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRecordPayment, modifier = Modifier.weight(1f)) {
              Text(strings.recordPayment, maxLines = 1)
            }
            if (summary.projects.isNotEmpty()) {
              OutlinedButton(
                onClick = { onProjectInvoicePdf(summary.projects.first().id) },
                modifier = Modifier.weight(1f),
              ) {
                Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.generateInvoicePdf, maxLines = 1, overflow = TextOverflow.Ellipsis)
              }
            }
          }
        }
      }
    }

    if (summary.payments.isEmpty()) {
      item { EmptyLedgerState(strings.noTransactionsYet) }
    } else {
      item {
        LedgerSurfaceCard {
          Column {
            summary.payments.forEachIndexed { idx, tx ->
              TransactionRow(
                transaction = tx,
                projectName = projectNames[tx.projectId],
                strings = strings,
                onClick = { onEditTransaction(tx) },
                onReceiptPdfClick = { onReceiptPdf(tx) },
              )
              if (idx < summary.payments.lastIndex) HairlineDivider()
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
      text = { Text("${strings.delete} ${summary.client.name}?") },
      confirmButton = {
        TextButton(
          onClick = {
            showDeleteConfirm = false
            onDeleteClient()
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
private fun ClientEditorDialog(
  client: ClientEntity?,
  strings: AppStrings,
  onDismiss: () -> Unit,
  onSave: (String, String, String, String, String) -> Unit,
) {
  var name by remember { mutableStateOf(client?.name.orEmpty()) }
  var phone by remember { mutableStateOf(client?.phone.orEmpty()) }
  var address by remember { mutableStateOf(client?.address.orEmpty()) }
  var gst by remember { mutableStateOf(client?.gstNumber.orEmpty()) }
  var notes by remember { mutableStateOf(client?.notes.orEmpty()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (client == null) strings.addClient else strings.editClient) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(strings.clientName) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text(strings.phone) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = address,
          onValueChange = { address = it },
          label = { Text(strings.address) },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = gst,
          onValueChange = { gst = it },
          label = { Text(strings.gstNumber) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
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
        onClick = { onSave(name, phone, address, gst, notes) },
      ) {
        Text(strings.save)
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } },
  )
}
