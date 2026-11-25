package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.room.withTransaction
import com.example.data.local.CholaBackupPayload
import com.example.data.local.CholaDatabase
import com.example.data.local.ClientEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.SeedData
import com.example.data.local.TransactionEntity
import com.example.i18n.AppLanguage
import com.example.util.Formatters
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

class AccountsRepository(context: Context) {
  private val appContext = context.applicationContext
  private val db = CholaDatabase.getInstance(appContext)
  private val clientDao = db.clientDao()
  private val projectDao = db.projectDao()
  private val transactionDao = db.transactionDao()

  private val prefs: SharedPreferences =
    appContext.getSharedPreferences("chola_empire_prefs", Context.MODE_PRIVATE)

  private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val backupAdapter = moshi.adapter(CholaBackupPayload::class.java).indent("  ")

  val clients: Flow<List<ClientEntity>> = clientDao.observeAll()
  val projects: Flow<List<ProjectEntity>> = projectDao.observeAll()
  val transactions: Flow<List<TransactionEntity>> = transactionDao.observeAll()

  fun getSavedLanguage(): AppLanguage {
    val code = prefs.getString(KEY_LANGUAGE, AppLanguage.ENGLISH.code)
    return AppLanguage.entries.firstOrNull { it.code == code } ?: AppLanguage.ENGLISH
  }

  fun saveLanguage(language: AppLanguage) {
    prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
  }

  suspend fun seedInitialDataIfFirstRun() =
    withContext(Dispatchers.IO) {
      if (!prefs.getBoolean(KEY_SEEDED, false)) {
        val existingProjects = projectDao.getAll()
        val existingClients = clientDao.getAll()
        if (existingProjects.isEmpty() && existingClients.isEmpty()) {
          val now = System.currentTimeMillis()
          clientDao.upsertAll(SeedData.sampleClients(now))
          projectDao.upsertAll(SeedData.sampleProjects(now))
          transactionDao.upsertAll(SeedData.sampleTransactions(now))
        }
        prefs.edit().putBoolean(KEY_SEEDED, true).apply()
      }
    }

  suspend fun upsertClient(client: ClientEntity) =
    withContext(Dispatchers.IO) { clientDao.upsert(client) }

  suspend fun deleteClient(clientId: String) =
    withContext(Dispatchers.IO) { clientDao.deleteById(clientId) }

  suspend fun upsertProject(project: ProjectEntity) =
    withContext(Dispatchers.IO) { projectDao.upsert(project) }

  suspend fun deleteProject(projectId: String) =
    withContext(Dispatchers.IO) { projectDao.deleteById(projectId) }

  suspend fun upsertTransaction(transaction: TransactionEntity) =
    withContext(Dispatchers.IO) { transactionDao.upsert(transaction) }

  suspend fun deleteTransaction(transactionId: String) =
    withContext(Dispatchers.IO) { transactionDao.deleteById(transactionId) }

  suspend fun exportJsonBackup(): String =
    withContext(Dispatchers.IO) {
      val payload =
        CholaBackupPayload(
          exportedAt = System.currentTimeMillis(),
          clients = clientDao.getAll(),
          projects = projectDao.getAll(),
          transactions = transactionDao.getAll(),
        )
      backupAdapter.toJson(payload)
    }

  suspend fun restoreJsonBackup(jsonString: String): Result<Int> =
    withContext(Dispatchers.IO) {
      runCatching {
        val payload =
          backupAdapter.fromJson(jsonString)
            ?: throw IllegalArgumentException("Invalid backup format")
        db.withTransaction {
          transactionDao.deleteAll()
          projectDao.deleteAll()
          clientDao.deleteAll()
          clientDao.upsertAll(payload.clients)
          projectDao.upsertAll(payload.projects)
          transactionDao.upsertAll(payload.transactions)
        }
        prefs.edit().putBoolean(KEY_SEEDED, true).apply()
        payload.transactions.size
      }
    }

  suspend fun exportCsvLedger(): String =
    withContext(Dispatchers.IO) {
      val projectsMap = projectDao.getAll().associateBy { it.id }
      val clientsMap = clientDao.getAll().associateBy { it.id }
      val txs = transactionDao.getAll()
      val sb = StringBuilder()
      sb.appendLine(
        "Date,Project,Client,Type,Category,Title,Party,Qty,Unit,UnitRate,AmountINR,PaymentMode,ReferenceNo,Notes"
      )
      for (tx in txs) {
        val projectName = projectsMap[tx.projectId]?.name.orEmpty()
        val clientName = tx.clientId?.let { clientsMap[it]?.name }.orEmpty()
        sb.appendLine(
          listOf(
              Formatters.formatDate(tx.dateMillis),
              projectName,
              clientName,
              tx.type,
              tx.category,
              tx.title,
              tx.partyName,
              tx.quantity?.toString().orEmpty(),
              tx.unit.orEmpty(),
              tx.unitRate?.toString().orEmpty(),
              tx.amount.toString(),
              tx.paymentMode,
              tx.referenceNo,
              tx.notes,
            )
            .joinToString(",") { escapeCsv(it) }
        )
      }
      sb.toString()
    }

  suspend fun clearAllData() =
    withContext(Dispatchers.IO) {
      db.withTransaction {
        transactionDao.deleteAll()
        projectDao.deleteAll()
        clientDao.deleteAll()
      }
      prefs.edit().putBoolean(KEY_SEEDED, true).apply()
    }

  fun observeConnectivity(): Flow<Boolean> = callbackFlow {
    val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun checkCurrent(): Boolean {
      val network = cm.activeNetwork ?: return false
      val caps = cm.getNetworkCapabilities(network) ?: return false
      return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    trySend(checkCurrent())

    val callback =
      object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
          trySend(true)
        }

        override fun onLost(network: Network) {
          trySend(checkCurrent())
        }
      }

    cm.registerDefaultNetworkCallback(callback)
    awaitClose { cm.unregisterNetworkCallback(callback) }
  }

  private fun escapeCsv(value: String): String {
    val clean = value.replace("\"", "\"\"")
    return "\"$clean\""
  }

  companion object {
    private const val KEY_LANGUAGE = "app_language"
    private const val KEY_SEEDED = "has_seeded_initial_data"
  }
}
