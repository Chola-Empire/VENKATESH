package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccountCategory
import com.example.data.local.ClientEntity
import com.example.data.local.PaymentMode
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectStatus
import com.example.data.local.SyncStatus
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.repository.AccountsRepository
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.i18n.stringsFor
import com.example.pdf.PdfDocumentGenerator
import com.example.util.Formatters
import java.util.UUID
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab {
  OVERVIEW,
  PROJECTS,
  ACCOUNTS,
  CLIENTS,
  REPORTS,
}

enum class AccountsSubTab {
  ALL_LEDGER,
  MATERIALS,
  LABOUR,
  DAILY_SITE,
}

enum class DateFilter {
  ALL_TIME,
  THIS_MONTH,
  LAST_30_DAYS,
}

data class ProjectSummary(
  val project: ProjectEntity,
  val client: ClientEntity?,
  val totalIncome: Double,
  val totalExpense: Double,
  val materialCost: Double,
  val cementCost: Double,
  val steelCost: Double,
  val sandCost: Double,
  val labourCost: Double,
  val siteExpenseCost: Double,
  val transactionCount: Int,
) {
  val pendingFromCustomer: Double
    get() = (project.contractValue - totalIncome).coerceAtLeast(0.0)

  val netProfitLoss: Double
    get() = totalIncome - totalExpense
}

data class ClientSummary(
  val client: ClientEntity,
  val projects: List<ProjectEntity>,
  val totalContractValue: Double,
  val totalPaid: Double,
  val payments: List<TransactionEntity>,
) {
  val pendingBalance: Double
    get() = (totalContractValue - totalPaid).coerceAtLeast(0.0)
}

data class MaterialMetric(
  val category: AccountCategory,
  val totalSpend: Double,
  val totalQuantity: Double,
  val unitLabel: String,
  val entryCount: Int,
)

data class AccountsUiState(
  val language: AppLanguage = AppLanguage.ENGLISH,
  val strings: AppStrings = stringsFor(AppLanguage.ENGLISH),
  val isOnline: Boolean = true,
  val projects: List<ProjectEntity> = emptyList(),
  val clients: List<ClientEntity> = emptyList(),
  val transactions: List<TransactionEntity> = emptyList(),
  val projectSummaries: List<ProjectSummary> = emptyList(),
  val clientSummaries: List<ClientSummary> = emptyList(),
  val totalContractValue: Double = 0.0,
  val totalReceived: Double = 0.0,
  val totalExpenses: Double = 0.0,
  val netProfitLoss: Double = 0.0,
  val totalPendingBalance: Double = 0.0,
  val localQueueCount: Int = 0,
  val materialMetrics: List<MaterialMetric> = emptyList(),
  val selectedProjectIdFilter: String? = null,
  val searchQuery: String = "",
  val dateFilter: DateFilter = DateFilter.ALL_TIME,
)

sealed interface UiEvent {
  data class ShowMessage(val message: String) : UiEvent
  data class OpenPdf(val uri: Uri, val title: String) : UiEvent
}

class AccountsViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = AccountsRepository(application)

  private val _language = MutableStateFlow(repository.getSavedLanguage())
  private val _selectedProjectIdFilter = MutableStateFlow<String?>(null)
  private val _searchQuery = MutableStateFlow("")
  private val _dateFilter = MutableStateFlow(DateFilter.ALL_TIME)

  private val _events = MutableSharedFlow<UiEvent>()
  val events = _events.asSharedFlow()

  private val baseDataFlow =
    combine(
      repository.projects,
      repository.clients,
      repository.transactions,
      repository.observeConnectivity(),
    ) { projects, clients, transactions, isOnline ->
      Quadruple(projects, clients, transactions, isOnline)
    }

  private val filterFlow =
    combine(_language, _selectedProjectIdFilter, _searchQuery, _dateFilter) {
      lang,
      projFilter,
      search,
      dateFilter ->
      Quadruple(lang, projFilter, search, dateFilter)
    }

  val uiState: StateFlow<AccountsUiState> =
    combine(baseDataFlow, filterFlow) { data, filters ->
        val (projects, clients, transactions, isOnline) = data
        val (lang, projFilter, search, dateFilter) = filters
        val strings = stringsFor(lang)
        val clientMap = clients.associateBy { it.id }

        val projectSummaries =
          projects.map { project ->
            val pTx = transactions.filter { it.projectId == project.id }
            val income =
              pTx.filter { it.type == TransactionType.INCOME_CUSTOMER_PAYMENT.name }.sumOf { it.amount }
            val material =
              pTx.filter { it.type == TransactionType.EXPENSE_MATERIAL.name }.sumOf { it.amount }
            val cement = pTx.filter { it.category == AccountCategory.CEMENT.name }.sumOf { it.amount }
            val steel = pTx.filter { it.category == AccountCategory.STEEL.name }.sumOf { it.amount }
            val sand = pTx.filter { it.category == AccountCategory.SAND.name }.sumOf { it.amount }
            val labour =
              pTx.filter { it.type == TransactionType.EXPENSE_LABOUR.name }.sumOf { it.amount }
            val site = pTx.filter { it.type == TransactionType.EXPENSE_SITE.name }.sumOf { it.amount }
            ProjectSummary(
              project = project,
              client = project.clientId?.let { clientMap[it] },
              totalIncome = income,
              totalExpense = material + labour + site,
              materialCost = material,
              cementCost = cement,
              steelCost = steel,
              sandCost = sand,
              labourCost = labour,
              siteExpenseCost = site,
              transactionCount = pTx.size,
            )
          }

        val clientSummaries =
          clients.map { client ->
            val cProjects = projects.filter { it.clientId == client.id }
            val cProjectIds = cProjects.map { it.id }.toSet()
            val cPayments =
              transactions.filter {
                it.type == TransactionType.INCOME_CUSTOMER_PAYMENT.name &&
                  (it.clientId == client.id || it.projectId in cProjectIds)
              }
            ClientSummary(
              client = client,
              projects = cProjects,
              totalContractValue = cProjects.sumOf { it.contractValue },
              totalPaid = cPayments.sumOf { it.amount },
              payments = cPayments,
            )
          }

        val totalContract = projects.sumOf { it.contractValue }
        val totalReceived =
          transactions
            .filter { it.type == TransactionType.INCOME_CUSTOMER_PAYMENT.name }
            .sumOf { it.amount }
        val totalExpenses =
          transactions
            .filter { it.type != TransactionType.INCOME_CUSTOMER_PAYMENT.name }
            .sumOf { it.amount }
        val totalPending = projectSummaries.sumOf { it.pendingFromCustomer }

        val scopedForMaterials =
          if (projFilter == null) transactions else transactions.filter { it.projectId == projFilter }
        val materialCategories =
          listOf(
            AccountCategory.CEMENT to strings.bagsUnit,
            AccountCategory.STEEL to strings.kgUnit,
            AccountCategory.SAND to strings.unitsLoad,
          )
        val materialMetrics =
          materialCategories.map { (cat, unit) ->
            val catTxs = scopedForMaterials.filter { it.category == cat.name }
            MaterialMetric(
              category = cat,
              totalSpend = catTxs.sumOf { it.amount },
              totalQuantity = catTxs.sumOf { it.quantity ?: 0.0 },
              unitLabel = unit,
              entryCount = catTxs.size,
            )
          }

        AccountsUiState(
          language = lang,
          strings = strings,
          isOnline = isOnline,
          projects = projects,
          clients = clients,
          transactions = transactions,
          projectSummaries = projectSummaries,
          clientSummaries = clientSummaries,
          totalContractValue = totalContract,
          totalReceived = totalReceived,
          totalExpenses = totalExpenses,
          netProfitLoss = totalReceived - totalExpenses,
          totalPendingBalance = totalPending,
          localQueueCount = transactions.count { it.syncStatus == SyncStatus.LOCAL_ONLY.name },
          materialMetrics = materialMetrics,
          selectedProjectIdFilter = projFilter,
          searchQuery = search,
          dateFilter = dateFilter,
        )
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccountsUiState())

  init {
    viewModelScope.launch { repository.seedInitialDataIfFirstRun() }
  }

  fun toggleLanguage() {
    val next =
      if (_language.value == AppLanguage.ENGLISH) AppLanguage.TAMIL else AppLanguage.ENGLISH
    _language.value = next
    repository.saveLanguage(next)
  }

  fun setLanguage(language: AppLanguage) {
    _language.value = language
    repository.saveLanguage(language)
  }

  fun setProjectFilter(projectId: String?) {
    _selectedProjectIdFilter.value = projectId
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setDateFilter(filter: DateFilter) {
    _dateFilter.value = filter
  }

  fun saveClient(
    id: String?,
    name: String,
    phone: String,
    address: String,
    gstNumber: String,
    notes: String,
  ) {
    viewModelScope.launch {
      val existing = uiState.value.clients.firstOrNull { it.id == id }
      repository.upsertClient(
        ClientEntity(
          id = id ?: "client_${UUID.randomUUID().toString().take(8)}",
          name = name.trim(),
          phone = phone.trim(),
          address = address.trim(),
          gstNumber = gstNumber.trim(),
          notes = notes.trim(),
          createdAt = existing?.createdAt ?: System.currentTimeMillis(),
        )
      )
    }
  }

  fun deleteClient(clientId: String) {
    viewModelScope.launch { repository.deleteClient(clientId) }
  }

  fun saveProject(
    id: String?,
    name: String,
    clientId: String?,
    location: String,
    contractValue: Double,
    status: ProjectStatus,
    notes: String,
  ) {
    viewModelScope.launch {
      val existing = uiState.value.projects.firstOrNull { it.id == id }
      repository.upsertProject(
        ProjectEntity(
          id = id ?: "proj_${UUID.randomUUID().toString().take(8)}",
          name = name.trim(),
          clientId = clientId,
          location = location.trim(),
          contractValue = contractValue,
          status = status.name,
          startDate = existing?.startDate ?: System.currentTimeMillis(),
          notes = notes.trim(),
          createdAt = existing?.createdAt ?: System.currentTimeMillis(),
        )
      )
    }
  }

  fun deleteProject(projectId: String) {
    viewModelScope.launch {
      if (_selectedProjectIdFilter.value == projectId) {
        _selectedProjectIdFilter.value = null
      }
      repository.deleteProject(projectId)
    }
  }

  fun saveTransaction(
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
  ) {
    viewModelScope.launch {
      val existing = uiState.value.transactions.firstOrNull { it.id == id }
      repository.upsertTransaction(
        TransactionEntity(
          id = id ?: "tx_${UUID.randomUUID().toString().take(8)}",
          projectId = projectId,
          clientId = clientId,
          type = type.name,
          category = category.name,
          title = title.trim(),
          partyName = partyName.trim(),
          quantity = quantity,
          unit = unit?.trim()?.takeIf { it.isNotEmpty() },
          unitRate = unitRate,
          amount = amount,
          paymentMode = paymentMode.name,
          referenceNo = referenceNo.trim(),
          dateMillis = dateMillis,
          notes = notes.trim(),
          syncStatus = SyncStatus.LOCAL_ONLY.name,
          createdAt = existing?.createdAt ?: System.currentTimeMillis(),
        )
      )
    }
  }

  fun deleteTransaction(transactionId: String) {
    viewModelScope.launch { repository.deleteTransaction(transactionId) }
  }

  fun exportBackupToUri(context: Context, uri: Uri) {
    viewModelScope.launch {
      val strings = uiState.value.strings
      runCatching {
          val json = repository.exportJsonBackup()
          context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
          } ?: error("Cannot open output stream")
        }
        .onSuccess { _events.emit(UiEvent.ShowMessage(strings.backupExportedSuccess)) }
        .onFailure { _events.emit(UiEvent.ShowMessage(it.message ?: strings.restoreFailed)) }
    }
  }

  fun exportCsvToUri(context: Context, uri: Uri) {
    viewModelScope.launch {
      val strings = uiState.value.strings
      runCatching {
          val csv = repository.exportCsvLedger()
          context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(csv.toByteArray(Charsets.UTF_8))
          } ?: error("Cannot open output stream")
        }
        .onSuccess { _events.emit(UiEvent.ShowMessage(strings.csvExportedSuccess)) }
        .onFailure { _events.emit(UiEvent.ShowMessage(it.message ?: strings.restoreFailed)) }
    }
  }

  fun restoreBackupFromUri(context: Context, uri: Uri) {
    viewModelScope.launch {
      val strings = uiState.value.strings
      runCatching {
          val json =
            context.contentResolver.openInputStream(uri)?.use { input ->
              input.bufferedReader(Charsets.UTF_8).readText()
            } ?: error("Cannot read file")
          repository.restoreJsonBackup(json).getOrThrow()
        }
        .onSuccess { count ->
          _events.emit(UiEvent.ShowMessage("${strings.restoreSuccess} ($count)"))
        }
        .onFailure { _events.emit(UiEvent.ShowMessage(strings.restoreFailed)) }
    }
  }

  fun clearAllData() {
    viewModelScope.launch {
      repository.clearAllData()
      _selectedProjectIdFilter.value = null
    }
  }

  fun generateReceiptPdf(context: Context, transaction: TransactionEntity) {
    viewModelScope.launch {
      val state = uiState.value
      val strings = state.strings
      runCatching {
          val project = state.projects.firstOrNull { it.id == transaction.projectId }
          val client =
            (transaction.clientId ?: project?.clientId)?.let { cid ->
              state.clients.firstOrNull { it.id == cid }
            }
          val totalPaidForProject =
            if (project == null) 0.0
            else
              state.transactions
                .filter {
                  it.projectId == project.id &&
                    it.type == TransactionType.INCOME_CUSTOMER_PAYMENT.name
                }
                .sumOf { it.amount }
          PdfDocumentGenerator.generatePaymentReceipt(
            context = context,
            transaction = transaction,
            project = project,
            client = client,
            totalPaidForProject = totalPaidForProject,
            strings = strings,
          )
        }
        .onSuccess { uri -> _events.emit(UiEvent.OpenPdf(uri, strings.paymentReceipt)) }
        .onFailure { _events.emit(UiEvent.ShowMessage(strings.pdfFailed)) }
    }
  }

  fun generateProjectInvoicePdf(context: Context, projectId: String) {
    viewModelScope.launch {
      val state = uiState.value
      val strings = state.strings
      val project = state.projects.firstOrNull { it.id == projectId } ?: return@launch
      runCatching {
          val client = project.clientId?.let { cid -> state.clients.firstOrNull { it.id == cid } }
          val pTx = state.transactions.filter { it.projectId == project.id }
          PdfDocumentGenerator.generateProjectInvoice(
            context = context,
            project = project,
            client = client,
            transactions = pTx,
            strings = strings,
          )
        }
        .onSuccess { uri -> _events.emit(UiEvent.OpenPdf(uri, strings.projectStatementInvoice)) }
        .onFailure { _events.emit(UiEvent.ShowMessage(strings.pdfFailed)) }
    }
  }

  fun generateProfitLossPdf(
    context: Context,
    scopeProjectId: String?,
    dateFilter: DateFilter,
  ) {
    viewModelScope.launch {
      val state = uiState.value
      val strings = state.strings
      runCatching {
          val filtered = filterTransactions(state.transactions, scopeProjectId, dateFilter, "")
          val scopeName =
            scopeProjectId?.let { id -> state.projects.firstOrNull { it.id == id }?.name }
              ?: strings.filterAllProjects
          val dateLabel =
            when (dateFilter) {
              DateFilter.ALL_TIME -> strings.dateRangeAll
              DateFilter.THIS_MONTH -> strings.dateRangeMonth
              DateFilter.LAST_30_DAYS -> strings.dateRange30Days
            }
          val income =
            filtered
              .filter { it.type == TransactionType.INCOME_CUSTOMER_PAYMENT.name }
              .sumOf { it.amount }
          val cement = filtered.filter { it.category == AccountCategory.CEMENT.name }.sumOf { it.amount }
          val steel = filtered.filter { it.category == AccountCategory.STEEL.name }.sumOf { it.amount }
          val sand = filtered.filter { it.category == AccountCategory.SAND.name }.sumOf { it.amount }
          val otherMat =
            filtered
              .filter {
                it.type == TransactionType.EXPENSE_MATERIAL.name &&
                  it.category !in
                    listOf(
                      AccountCategory.CEMENT.name,
                      AccountCategory.STEEL.name,
                      AccountCategory.SAND.name,
                    )
              }
              .sumOf { it.amount }
          val labour =
            filtered.filter { it.type == TransactionType.EXPENSE_LABOUR.name }.sumOf { it.amount }
          val site =
            filtered.filter { it.type == TransactionType.EXPENSE_SITE.name }.sumOf { it.amount }

          PdfDocumentGenerator.generateProfitLossPdf(
            context = context,
            titleScope = scopeName,
            dateScope = dateLabel,
            totalIncome = income,
            cementCost = cement,
            steelCost = steel,
            sandCost = sand,
            otherMaterialCost = otherMat,
            labourCost = labour,
            siteExpenseCost = site,
            strings = strings,
          )
        }
        .onSuccess { uri -> _events.emit(UiEvent.OpenPdf(uri, strings.profitLossReport)) }
        .onFailure { _events.emit(UiEvent.ShowMessage(strings.pdfFailed)) }
    }
  }

  fun filterTransactions(
    all: List<TransactionEntity>,
    projectId: String?,
    dateFilter: DateFilter,
    query: String,
  ): List<TransactionEntity> {
    val now = System.currentTimeMillis()
    val cutoff =
      when (dateFilter) {
        DateFilter.ALL_TIME -> 0L
        DateFilter.THIS_MONTH -> Formatters.startOfCurrentMonthMillis()
        DateFilter.LAST_30_DAYS -> now - 30L * 24L * 60L * 60L * 1000L
      }
    val q = query.trim().lowercase()
    return all.filter { tx ->
      (projectId == null || tx.projectId == projectId) &&
        tx.dateMillis >= cutoff &&
        (q.isEmpty() ||
          tx.title.lowercase().contains(q) ||
          tx.partyName.lowercase().contains(q) ||
          tx.referenceNo.lowercase().contains(q) ||
          tx.category.lowercase().contains(q))
    }
  }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
