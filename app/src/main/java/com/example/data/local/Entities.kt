package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.i18n.AppStrings
import com.squareup.moshi.JsonClass

enum class ProjectStatus {
  ACTIVE,
  ON_HOLD,
  COMPLETED;

  fun label(strings: AppStrings): String =
    when (this) {
      ACTIVE -> strings.statusActive
      ON_HOLD -> strings.statusOnHold
      COMPLETED -> strings.statusCompleted
    }
}

enum class TransactionType {
  INCOME_CUSTOMER_PAYMENT,
  EXPENSE_MATERIAL,
  EXPENSE_LABOUR,
  EXPENSE_SITE;

  val isIncome: Boolean
    get() = this == INCOME_CUSTOMER_PAYMENT

  fun label(strings: AppStrings): String =
    when (this) {
      INCOME_CUSTOMER_PAYMENT -> strings.typeIncome
      EXPENSE_MATERIAL -> strings.typeMaterial
      EXPENSE_LABOUR -> strings.typeLabour
      EXPENSE_SITE -> strings.typeSiteExpense
    }
}

enum class AccountCategory(val type: TransactionType, val defaultUnit: String?) {
  CUSTOMER_ADVANCE(TransactionType.INCOME_CUSTOMER_PAYMENT, null),
  MILESTONE_PAYMENT(TransactionType.INCOME_CUSTOMER_PAYMENT, null),
  FINAL_SETTLEMENT(TransactionType.INCOME_CUSTOMER_PAYMENT, null),
  CEMENT(TransactionType.EXPENSE_MATERIAL, "Bags"),
  STEEL(TransactionType.EXPENSE_MATERIAL, "Kg"),
  SAND(TransactionType.EXPENSE_MATERIAL, "Units"),
  BRICKS_AGGREGATE(TransactionType.EXPENSE_MATERIAL, "Loads"),
  OTHER_MATERIAL(TransactionType.EXPENSE_MATERIAL, "Units"),
  MASON(TransactionType.EXPENSE_LABOUR, "Days"),
  HELPER(TransactionType.EXPENSE_LABOUR, "Days"),
  CONTRACTOR(TransactionType.EXPENSE_LABOUR, null),
  ELECTRICIAN_PLUMBER(TransactionType.EXPENSE_LABOUR, null),
  TRANSPORT(TransactionType.EXPENSE_SITE, null),
  FOOD_TEA(TransactionType.EXPENSE_SITE, null),
  EQUIPMENT_RENT(TransactionType.EXPENSE_SITE, "Hours"),
  UTILITY_PETTY(TransactionType.EXPENSE_SITE, null);

  fun label(strings: AppStrings): String =
    when (this) {
      CUSTOMER_ADVANCE -> strings.customerAdvance
      MILESTONE_PAYMENT -> strings.milestonePayment
      FINAL_SETTLEMENT -> strings.finalSettlement
      CEMENT -> strings.cement
      STEEL -> strings.steel
      SAND -> strings.sand
      BRICKS_AGGREGATE -> strings.bricksAggregate
      OTHER_MATERIAL -> strings.otherMaterial
      MASON -> strings.mason
      HELPER -> strings.helper
      CONTRACTOR -> strings.contractor
      ELECTRICIAN_PLUMBER -> strings.electricianPlumber
      TRANSPORT -> strings.transport
      FOOD_TEA -> strings.foodTea
      EQUIPMENT_RENT -> strings.equipmentRent
      UTILITY_PETTY -> strings.utilityPetty
    }

  companion object {
    fun forType(type: TransactionType): List<AccountCategory> = entries.filter { it.type == type }
  }
}

enum class PaymentMode {
  CASH,
  UPI,
  BANK_TRANSFER,
  CHEQUE;

  fun label(strings: AppStrings): String =
    when (this) {
      CASH -> strings.modeCash
      UPI -> strings.modeUpi
      BANK_TRANSFER -> strings.modeBank
      CHEQUE -> strings.modeCheque
    }
}

enum class SyncStatus {
  LOCAL_ONLY,
  SYNCED,
}

@JsonClass(generateAdapter = true)
@Entity(tableName = "clients")
data class ClientEntity(
  @PrimaryKey val id: String,
  val name: String,
  val phone: String,
  val address: String,
  val gstNumber: String = "",
  val notes: String = "",
  val createdAt: Long = System.currentTimeMillis(),
)

@JsonClass(generateAdapter = true)
@Entity(
  tableName = "projects",
  foreignKeys =
    [
      ForeignKey(
        entity = ClientEntity::class,
        parentColumns = ["id"],
        childColumns = ["clientId"],
        onDelete = ForeignKey.SET_NULL,
      )
    ],
  indices = [Index("clientId")],
)
data class ProjectEntity(
  @PrimaryKey val id: String,
  val name: String,
  val clientId: String?,
  val location: String,
  val contractValue: Double,
  val status: String = ProjectStatus.ACTIVE.name,
  val startDate: Long = System.currentTimeMillis(),
  val notes: String = "",
  val createdAt: Long = System.currentTimeMillis(),
)

@JsonClass(generateAdapter = true)
@Entity(
  tableName = "transactions",
  foreignKeys =
    [
      ForeignKey(
        entity = ProjectEntity::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index("projectId"), Index("clientId"), Index("dateMillis")],
)
data class TransactionEntity(
  @PrimaryKey val id: String,
  val projectId: String,
  val clientId: String?,
  val type: String,
  val category: String,
  val title: String,
  val partyName: String,
  val quantity: Double? = null,
  val unit: String? = null,
  val unitRate: Double? = null,
  val amount: Double,
  val paymentMode: String = PaymentMode.CASH.name,
  val referenceNo: String = "",
  val dateMillis: Long = System.currentTimeMillis(),
  val notes: String = "",
  val syncStatus: String = SyncStatus.LOCAL_ONLY.name,
  val createdAt: Long = System.currentTimeMillis(),
)

@JsonClass(generateAdapter = true)
data class CholaBackupPayload(
  val version: Int = 1,
  val exportedAt: Long = System.currentTimeMillis(),
  val appName: String = "Chola Empire Construction Accounts",
  val clients: List<ClientEntity>,
  val projects: List<ProjectEntity>,
  val transactions: List<TransactionEntity>,
)
