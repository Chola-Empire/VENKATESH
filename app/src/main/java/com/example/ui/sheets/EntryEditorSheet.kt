package com.example.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AccountCategory
import com.example.data.local.ClientEntity
import com.example.data.local.PaymentMode
import com.example.data.local.ProjectEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.i18n.AppStrings
import com.example.ui.components.SegmentedFilterButton
import com.example.ui.components.SelectorField
import com.example.ui.theme.ExpenseCrimson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditorSheet(
  existing: TransactionEntity?,
  initialType: TransactionType,
  initialProjectId: String?,
  initialClientId: String?,
  projects: List<ProjectEntity>,
  clients: List<ClientEntity>,
  strings: AppStrings,
  onDismiss: () -> Unit,
  onSave: (
    id: String?,
    projectId: String,
    clientId: String?,
    type: TransactionType,
    category: AccountCategory,
    title: String,
    partyName: String,
    quantity: Double?,
    unit: String?,
    unitRate: Double?,
    amount: Double,
    paymentMode: PaymentMode,
    referenceNo: String,
    dateMillis: Long,
    notes: String,
  ) -> Unit,
  onDelete: ((String) -> Unit)? = null,
  onGenerateReceiptPdf: ((TransactionEntity) -> Unit)? = null,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var type by remember {
    mutableStateOf(
      existing?.let { tx -> TransactionType.entries.firstOrNull { it.name == tx.type } }
        ?: initialType
    )
  }
  var category by remember {
    mutableStateOf(
      existing?.let { tx -> AccountCategory.entries.firstOrNull { it.name == tx.category } }
        ?: AccountCategory.forType(type).first()
    )
  }
  var projectId by remember {
    mutableStateOf(
      existing?.projectId
        ?: initialProjectId
        ?: projects.firstOrNull()?.id.orEmpty()
    )
  }
  var clientId by remember {
    mutableStateOf(
      existing?.clientId
        ?: initialClientId
        ?: projects.firstOrNull { it.id == projectId }?.clientId
    )
  }
  var title by remember { mutableStateOf(existing?.title.orEmpty()) }
  var partyName by remember {
    val defaultParty =
      if (type == TransactionType.INCOME_CUSTOMER_PAYMENT) {
        clients.firstOrNull { it.id == clientId }?.name.orEmpty()
      } else {
        ""
      }
    mutableStateOf(existing?.partyName ?: defaultParty)
  }
  var qtyText by remember {
    mutableStateOf(
      existing?.quantity?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }.orEmpty()
    )
  }
  var unitText by remember { mutableStateOf(existing?.unit ?: category.defaultUnit.orEmpty()) }
  var rateText by remember {
    mutableStateOf(
      existing?.unitRate?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }.orEmpty()
    )
  }
  var amountText by remember {
    mutableStateOf(
      existing?.amount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }.orEmpty()
    )
  }
  var paymentMode by remember {
    mutableStateOf(
      existing?.let { tx -> PaymentMode.entries.firstOrNull { it.name == tx.paymentMode } }
        ?: PaymentMode.CASH
    )
  }
  var referenceNo by remember { mutableStateOf(existing?.referenceNo.orEmpty()) }
  var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }

  val categoryOptions = AccountCategory.forType(type).map { it to it.label(strings) }
  val selectedProjectLabel =
    projects.firstOrNull { it.id == projectId }?.name ?: strings.noProjectsYet
  val clientOptions: List<Pair<String?, String>> =
    listOf(null to "—") + clients.map { it.id to it.name }
  val selectedClientLabel = clientOptions.firstOrNull { it.first == clientId }?.second ?: "—"

  fun recalculateAmount(newQty: String, newRate: String) {
    val q = newQty.toDoubleOrNull()
    val r = newRate.toDoubleOrNull()
    if (q != null && r != null && q > 0 && r > 0) {
      val total = q * r
      amountText = if (total % 1.0 == 0.0) total.toLong().toString() else String.format(java.util.Locale.ENGLISH, "%.2f", total)
    }
  }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (existing == null) strings.quickEntry else strings.edit,
          style = MaterialTheme.typography.titleLarge,
        )
        Row {
          if (existing != null &&
            existing.type == TransactionType.INCOME_CUSTOMER_PAYMENT.name &&
            onGenerateReceiptPdf != null
          ) {
            IconButton(onClick = { onGenerateReceiptPdf(existing) }) {
              Icon(
                Icons.Outlined.PictureAsPdf,
                contentDescription = strings.generateReceiptPdf,
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
          if (existing != null && onDelete != null) {
            IconButton(
              onClick = {
                onDelete(existing.id)
                onDismiss()
              }
            ) {
              Icon(Icons.Outlined.Delete, contentDescription = strings.delete, tint = ExpenseCrimson)
            }
          }
        }
      }

      // Entry Type Selector (2x2 clean grid)
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          SegmentedFilterButton(
            text = strings.typeIncome,
            selected = type == TransactionType.INCOME_CUSTOMER_PAYMENT,
            onClick = {
              type = TransactionType.INCOME_CUSTOMER_PAYMENT
              category = AccountCategory.forType(type).first()
              unitText = ""
              if (partyName.isBlank()) {
                partyName = clients.firstOrNull { it.id == clientId }?.name.orEmpty()
              }
            },
            modifier = Modifier.weight(1f),
          )
          SegmentedFilterButton(
            text = strings.typeMaterial,
            selected = type == TransactionType.EXPENSE_MATERIAL,
            onClick = {
              type = TransactionType.EXPENSE_MATERIAL
              category = AccountCategory.forType(type).first()
              unitText = category.defaultUnit.orEmpty()
            },
            modifier = Modifier.weight(1f),
          )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          SegmentedFilterButton(
            text = strings.typeLabour,
            selected = type == TransactionType.EXPENSE_LABOUR,
            onClick = {
              type = TransactionType.EXPENSE_LABOUR
              category = AccountCategory.forType(type).first()
              unitText = category.defaultUnit.orEmpty()
            },
            modifier = Modifier.weight(1f),
          )
          SegmentedFilterButton(
            text = strings.typeSiteExpense,
            selected = type == TransactionType.EXPENSE_SITE,
            onClick = {
              type = TransactionType.EXPENSE_SITE
              category = AccountCategory.forType(type).first()
              unitText = category.defaultUnit.orEmpty()
            },
            modifier = Modifier.weight(1f),
          )
        }
      }

      // Project & Category
      if (projects.isNotEmpty()) {
        SelectorField(
          label = strings.selectProject,
          selectedLabel = selectedProjectLabel,
          options = projects.map { it.id to it.name },
          onSelected = { newPid ->
            projectId = newPid
            val proj = projects.firstOrNull { it.id == newPid }
            if (type == TransactionType.INCOME_CUSTOMER_PAYMENT && proj?.clientId != null) {
              clientId = proj.clientId
              if (partyName.isBlank()) {
                partyName = clients.firstOrNull { it.id == proj.clientId }?.name.orEmpty()
              }
            }
          },
        )
      }

      SelectorField(
        label = strings.selectCategory,
        selectedLabel = category.label(strings),
        options = categoryOptions,
        onSelected = { newCat ->
          category = newCat
          unitText = newCat.defaultUnit.orEmpty()
        },
      )

      if (type == TransactionType.INCOME_CUSTOMER_PAYMENT && clients.isNotEmpty()) {
        SelectorField(
          label = strings.selectClient,
          selectedLabel = selectedClientLabel,
          options = clientOptions,
          onSelected = { newCid ->
            clientId = newCid
            clients.firstOrNull { it.id == newCid }?.let { partyName = it.name }
          },
        )
      }

      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text(strings.titleDescLabel) },
        placeholder = { Text(category.label(strings), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )

      OutlinedTextField(
        value = partyName,
        onValueChange = { partyName = it },
        label = { Text(strings.partyNameLabel) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )

      // Quantity, Unit, Unit Rate (useful for Cement/Steel/Sand/Labour Days/JCB Hours)
      if (type != TransactionType.INCOME_CUSTOMER_PAYMENT) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = qtyText,
            onValueChange = {
              qtyText = it.filter { ch -> ch.isDigit() || ch == '.' }
              recalculateAmount(qtyText, rateText)
            },
            label = { Text(strings.quantity) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = unitText,
            onValueChange = { unitText = it },
            label = { Text(strings.unit) },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = rateText,
            onValueChange = {
              rateText = it.filter { ch -> ch.isDigit() || ch == '.' }
              recalculateAmount(qtyText, rateText)
            },
            label = { Text(strings.unitRate) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
      }

      OutlinedTextField(
        value = amountText,
        onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
        label = { Text(strings.amountInr) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SelectorField(
          label = strings.paymentMode,
          selectedLabel = paymentMode.label(strings),
          options = PaymentMode.entries.map { it to it.label(strings) },
          onSelected = { paymentMode = it },
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = referenceNo,
          onValueChange = { referenceNo = it },
          label = { Text(strings.referenceNo) },
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
      }

      OutlinedTextField(
        value = notes,
        onValueChange = { notes = it },
        label = { Text(strings.notes) },
        maxLines = 2,
        modifier = Modifier.fillMaxWidth(),
      )

      Spacer(modifier = Modifier.height(4.dp))

      val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
          Text(strings.cancel)
        }
        Button(
          enabled = projectId.isNotBlank() && parsedAmount > 0.0,
          onClick = {
            val resolvedTitle = title.ifBlank { category.label(strings) }
            onSave(
              existing?.id,
              projectId,
              if (type == TransactionType.INCOME_CUSTOMER_PAYMENT) clientId else null,
              type,
              category,
              resolvedTitle,
              partyName,
              qtyText.toDoubleOrNull(),
              unitText.ifBlank { null },
              rateText.toDoubleOrNull(),
              parsedAmount,
              paymentMode,
              referenceNo,
              existing?.dateMillis ?: System.currentTimeMillis(),
              notes,
            )
            onDismiss()
          },
          modifier = Modifier.weight(1f),
        ) {
          Text(strings.save)
        }
      }
    }
  }
}
