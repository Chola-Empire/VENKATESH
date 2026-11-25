package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.data.local.PaymentMode
import com.example.data.local.ProjectEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.i18n.AppStrings
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TabularAmountStyle
import com.example.util.Formatters

@Composable
fun LedgerSurfaceCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  content: @Composable () -> Unit,
) {
  Surface(
    modifier =
      modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
  ) {
    content()
  }
}

@Composable
fun MetricCell(
  label: String,
  amount: Double,
  accentColor: Color = MaterialTheme.colorScheme.onSurface,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = Formatters.formatInr(amount),
      style = TabularAmountStyle.copy(fontSize = 18.sp),
      color = accentColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
fun TransactionRow(
  transaction: TransactionEntity,
  projectName: String?,
  strings: AppStrings,
  onClick: () -> Unit,
  onReceiptPdfClick: (() -> Unit)? = null,
) {
  val isIncome = transaction.type == TransactionType.INCOME_CUSTOMER_PAYMENT.name
  val amountColor = if (isIncome) IncomeGreen else ExpenseCrimson
  val categoryLabel =
    AccountCategory.entries.firstOrNull { it.name == transaction.category }?.label(strings)
      ?: transaction.category
  val modeLabel =
    PaymentMode.entries.firstOrNull { it.name == transaction.paymentMode }?.label(strings)
      ?: transaction.paymentMode

  Row(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier =
        Modifier.size(36.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = if (isIncome) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
        contentDescription = if (isIncome) strings.income else strings.expense,
        tint = amountColor,
        modifier = Modifier.size(18.dp),
      )
    }

    Spacer(modifier = Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = transaction.title.ifBlank { categoryLabel },
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(2.dp))
      val qtyPart =
        if (transaction.quantity != null && transaction.quantity > 0) {
          "${Formatters.formatQty(transaction.quantity)} ${transaction.unit.orEmpty()} · "
        } else {
          ""
        }
      val metaParts =
        buildList {
            add(categoryLabel)
            if (transaction.partyName.isNotBlank()) add(transaction.partyName)
            if (!projectName.isNullOrBlank()) add(projectName)
          }
          .joinToString(" · ")
      Text(
        text = "$qtyPart$metaParts",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(2.dp))
      val footerParts =
        buildList {
            add(Formatters.formatDate(transaction.dateMillis))
            add(modeLabel)
            if (transaction.referenceNo.isNotBlank()) add(transaction.referenceNo)
          }
          .joinToString(" · ")
      Text(
        text = footerParts,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    Spacer(modifier = Modifier.width(8.dp))

    Column(horizontalAlignment = Alignment.End) {
      Text(
        text = (if (isIncome) "+ " else "- ") + Formatters.formatInr(transaction.amount),
        style = TabularAmountStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
        color = amountColor,
      )
      if (isIncome && onReceiptPdfClick != null) {
        IconButton(onClick = onReceiptPdfClick, modifier = Modifier.size(32.dp)) {
          Icon(
            imageVector = Icons.Outlined.PictureAsPdf,
            contentDescription = strings.generateReceiptPdf,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}

@Composable
fun ProjectFilterBar(
  projects: List<ProjectEntity>,
  selectedProjectId: String?,
  allLabel: String,
  onSelect: (String?) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    SegmentedFilterButton(
      text = allLabel,
      selected = selectedProjectId == null,
      onClick = { onSelect(null) },
    )
    projects.forEach { project ->
      SegmentedFilterButton(
        text = project.name,
        selected = selectedProjectId == project.id,
        onClick = { onSelect(project.id) },
      )
    }
  }
}

@Composable
fun SegmentedFilterButton(
  text: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.height(40.dp).clickable(onClick = onClick),
    shape = RoundedCornerShape(8.dp),
    color =
      if (selected) MaterialTheme.colorScheme.primary
      else MaterialTheme.colorScheme.surface,
    border =
      BorderStroke(
        1.dp,
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
      ),
  ) {
    Box(modifier = Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
      Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
        color =
          if (selected) MaterialTheme.colorScheme.onPrimary
          else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun <T> SelectorField(
  label: String,
  selectedLabel: String,
  options: List<Pair<T, String>>,
  onSelected: (T) -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  Box(modifier = modifier.fillMaxWidth()) {
    Surface(
      modifier = Modifier.fillMaxWidth().clickable { expanded = true },
      shape = RoundedCornerShape(8.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
      Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = selectedLabel,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      options.forEach { (value, display) ->
        DropdownMenuItem(
          text = { Text(display) },
          onClick = {
            onSelected(value)
            expanded = false
          },
        )
      }
    }
  }
}

@Composable
fun EmptyLedgerState(message: String) {
  LedgerSurfaceCard {
    Column(
      modifier = Modifier.fillMaxWidth().padding(28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Icon(
        imageVector = Icons.Outlined.Description,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(32.dp),
      )
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun HairlineDivider() {
  HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
}
