package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.i18n.AppLanguage
import com.example.pdf.PdfDocumentGenerator
import com.example.ui.components.HairlineDivider
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.ClientsScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.ReportsAndBackupScreen
import com.example.ui.sheets.EntryEditorSheet
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AccountsSubTab
import com.example.ui.viewmodel.AccountsViewModel
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.UiEvent
import com.example.util.Formatters

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        CholaEmpireApp()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CholaEmpireApp(vm: AccountsViewModel = viewModel()) {
  val context = LocalContext.current
  val state by vm.uiState.collectAsStateWithLifecycle()
  val s = state.strings

  var activeTab by remember { mutableStateOf(MainTab.OVERVIEW) }
  var accountsSubTab by remember { mutableStateOf(AccountsSubTab.ALL_LEDGER) }
  var selectedProjectIdDetail by remember { mutableStateOf<String?>(null) }
  var selectedClientIdDetail by remember { mutableStateOf<String?>(null) }

  var showEntrySheet by remember { mutableStateOf(false) }
  var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
  var entryInitialType by remember { mutableStateOf(TransactionType.EXPENSE_MATERIAL) }
  var entryInitialProjectId by remember { mutableStateOf<String?>(null) }
  var entryInitialClientId by remember { mutableStateOf<String?>(null) }

  var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
  val snackbarHostState = remember { SnackbarHostState() }

  val exportJsonLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
      uri?.let { vm.exportBackupToUri(context, it) }
    }

  val exportCsvLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
      uri?.let { vm.exportCsvToUri(context, it) }
    }

  val restoreJsonLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) pendingRestoreUri = uri
    }

  LaunchedEffect(Unit) {
    vm.events.collect { event ->
      when (event) {
        is UiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
        is UiEvent.OpenPdf -> PdfDocumentGenerator.openOrSharePdf(context, event.uri, event.title)
      }
    }
  }

  fun openNewEntry(
    type: TransactionType,
    projectId: String? = state.selectedProjectIdFilter,
    clientId: String? = null,
  ) {
    editingTransaction = null
    entryInitialType = type
    entryInitialProjectId = projectId ?: state.projects.firstOrNull()?.id
    entryInitialClientId = clientId
    showEntrySheet = true
  }

  fun openEditEntry(tx: TransactionEntity) {
    editingTransaction = tx
    showEntrySheet = true
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      Column {
        TopAppBar(
          colors =
            TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.surface,
              titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
          title = {
            Column {
              Text(
                text = s.brandTitle,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text =
                  "${s.brandSubtitle} · ${if (state.isOnline) s.onlineReady else s.offlineReady}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          },
          actions = {
            Surface(
              modifier =
                Modifier.padding(end = 12.dp)
                  .height(36.dp)
                  .clickable { vm.toggleLanguage() },
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
              Box(
                modifier = Modifier.padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text =
                    if (state.language == AppLanguage.ENGLISH) "தமிழ்" else "EN",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.primary,
                  maxLines = 1,
                )
              }
            }
          },
        )
        HairlineDivider()
      }
    },
    floatingActionButton = {
      if (state.projects.isNotEmpty()) {
        FloatingActionButton(
          onClick = { openNewEntry(TransactionType.EXPENSE_MATERIAL) },
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Outlined.Add, contentDescription = s.quickEntry)
            Spacer(modifier = Modifier.width(6.dp))
            Text(s.quickEntry, maxLines = 1)
          }
        }
      }
    },
    bottomBar = {
      Column {
        HairlineDivider()
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
          NavigationBarItem(
            selected = activeTab == MainTab.OVERVIEW,
            onClick = { activeTab = MainTab.OVERVIEW },
            icon = { Icon(Icons.Outlined.Dashboard, contentDescription = null) },
            label = { Text(s.navOverview, maxLines = 1, overflow = TextOverflow.Ellipsis) },
          )
          NavigationBarItem(
            selected = activeTab == MainTab.PROJECTS,
            onClick = {
              selectedProjectIdDetail = null
              activeTab = MainTab.PROJECTS
            },
            icon = { Icon(Icons.Outlined.Business, contentDescription = null) },
            label = { Text(s.navProjects, maxLines = 1, overflow = TextOverflow.Ellipsis) },
          )
          NavigationBarItem(
            selected = activeTab == MainTab.ACCOUNTS,
            onClick = { activeTab = MainTab.ACCOUNTS },
            icon = { Icon(Icons.Outlined.AccountBalance, contentDescription = null) },
            label = { Text(s.navAccounts, maxLines = 1, overflow = TextOverflow.Ellipsis) },
          )
          NavigationBarItem(
            selected = activeTab == MainTab.CLIENTS,
            onClick = {
              selectedClientIdDetail = null
              activeTab = MainTab.CLIENTS
            },
            icon = { Icon(Icons.Outlined.Groups, contentDescription = null) },
            label = { Text(s.navClients, maxLines = 1, overflow = TextOverflow.Ellipsis) },
          )
          NavigationBarItem(
            selected = activeTab == MainTab.REPORTS,
            onClick = { activeTab = MainTab.REPORTS },
            icon = { Icon(Icons.Outlined.Assessment, contentDescription = null) },
            label = { Text(s.navReports, maxLines = 1, overflow = TextOverflow.Ellipsis) },
          )
        }
      }
    },
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      when (activeTab) {
        MainTab.OVERVIEW ->
          OverviewScreen(
            state = state,
            onQuickEntry = { type -> openNewEntry(type) },
            onOpenProject = { summary ->
              selectedProjectIdDetail = summary.project.id
              activeTab = MainTab.PROJECTS
            },
            onSeeAllProjects = { activeTab = MainTab.PROJECTS },
            onEditTransaction = { openEditEntry(it) },
            onReceiptPdf = { vm.generateReceiptPdf(context, it) },
          )
        MainTab.PROJECTS ->
          ProjectsScreen(
            state = state,
            selectedProjectId = selectedProjectIdDetail,
            onSelectProject = { selectedProjectIdDetail = it },
            onSaveProject = { id, name, clientId, location, contractVal, status, notes ->
              vm.saveProject(id, name, clientId, location, contractVal, status, notes)
            },
            onDeleteProject = { vm.deleteProject(it) },
            onAddEntryForProject = { pid, type -> openNewEntry(type, projectId = pid) },
            onEditTransaction = { openEditEntry(it) },
            onReceiptPdf = { vm.generateReceiptPdf(context, it) },
            onProjectInvoicePdf = { vm.generateProjectInvoicePdf(context, it) },
          )
        MainTab.ACCOUNTS ->
          AccountsScreen(
            state = state,
            activeSubTab = accountsSubTab,
            onSelectSubTab = { accountsSubTab = it },
            onProjectFilterChange = { vm.setProjectFilter(it) },
            onSearchQueryChange = { vm.setSearchQuery(it) },
            onQuickAddForSubTab = { sub ->
              val type =
                when (sub) {
                  AccountsSubTab.ALL_LEDGER,
                  AccountsSubTab.MATERIALS -> TransactionType.EXPENSE_MATERIAL
                  AccountsSubTab.LABOUR -> TransactionType.EXPENSE_LABOUR
                  AccountsSubTab.DAILY_SITE -> TransactionType.EXPENSE_SITE
                }
              openNewEntry(type)
            },
            onEditTransaction = { openEditEntry(it) },
            onReceiptPdf = { vm.generateReceiptPdf(context, it) },
            filterFn = { all, pid, df, q -> vm.filterTransactions(all, pid, df, q) },
          )
        MainTab.CLIENTS ->
          ClientsScreen(
            state = state,
            selectedClientId = selectedClientIdDetail,
            onSelectClient = { selectedClientIdDetail = it },
            onSaveClient = { id, name, phone, address, gst, notes ->
              vm.saveClient(id, name, phone, address, gst, notes)
            },
            onDeleteClient = { vm.deleteClient(it) },
            onRecordPaymentForClient = { clientSummary ->
              openNewEntry(
                type = TransactionType.INCOME_CUSTOMER_PAYMENT,
                projectId = clientSummary.projects.firstOrNull()?.id,
                clientId = clientSummary.client.id,
              )
            },
            onEditTransaction = { openEditEntry(it) },
            onReceiptPdf = { vm.generateReceiptPdf(context, it) },
            onProjectInvoicePdf = { vm.generateProjectInvoicePdf(context, it) },
          )
        MainTab.REPORTS ->
          ReportsAndBackupScreen(
            state = state,
            onProjectFilterChange = { vm.setProjectFilter(it) },
            onDateFilterChange = { vm.setDateFilter(it) },
            onLanguageChange = { vm.setLanguage(it) },
            onExportProfitLossPdf = {
              vm.generateProfitLossPdf(context, state.selectedProjectIdFilter, state.dateFilter)
            },
            onExportJsonBackup = {
              exportJsonLauncher.launch("CholaEmpire_Backup_${Formatters.formatFileStamp()}.json")
            },
            onRestoreJsonBackup = {
              restoreJsonLauncher.launch(arrayOf("application/json", "*/*"))
            },
            onExportCsvLedger = {
              exportCsvLauncher.launch("CholaEmpire_Ledger_${Formatters.formatFileStamp()}.csv")
            },
            onClearAllData = { vm.clearAllData() },
            filterFn = { all, pid, df, q -> vm.filterTransactions(all, pid, df, q) },
          )
      }
    }
  }

  if (showEntrySheet) {
    EntryEditorSheet(
      existing = editingTransaction,
      initialType = entryInitialType,
      initialProjectId = entryInitialProjectId,
      initialClientId = entryInitialClientId,
      projects = state.projects,
      clients = state.clients,
      strings = s,
      onDismiss = {
        showEntrySheet = false
        editingTransaction = null
      },
      onSave = {
        id,
        projectId,
        clientId,
        type,
        category,
        title,
        partyName,
        qty,
        unit,
        rate,
        amount,
        mode,
        ref,
        dateMillis,
        notes ->
        vm.saveTransaction(
          id = id,
          projectId = projectId,
          clientId = clientId,
          type = type,
          category = category,
          title = title,
          partyName = partyName,
          quantity = qty,
          unit = unit,
          unitRate = rate,
          amount = amount,
          paymentMode = mode,
          referenceNo = ref,
          dateMillis = dateMillis,
          notes = notes,
        )
      },
      onDelete = { txId -> vm.deleteTransaction(txId) },
      onGenerateReceiptPdf = { tx -> vm.generateReceiptPdf(context, tx) },
    )
  }

  if (pendingRestoreUri != null) {
    AlertDialog(
      onDismissRequest = { pendingRestoreUri = null },
      title = { Text(s.confirmRestoreTitle) },
      text = { Text(s.confirmRestoreMessage) },
      confirmButton = {
        TextButton(
          onClick = {
            pendingRestoreUri?.let { vm.restoreBackupFromUri(context, it) }
            pendingRestoreUri = null
          }
        ) {
          Text(s.confirm)
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingRestoreUri = null }) { Text(s.cancel) }
      },
    )
  }
}
