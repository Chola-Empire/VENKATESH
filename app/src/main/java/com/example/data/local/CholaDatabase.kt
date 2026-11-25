package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [ClientEntity::class, ProjectEntity::class, TransactionEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class CholaDatabase : RoomDatabase() {
  abstract fun clientDao(): ClientDao

  abstract fun projectDao(): ProjectDao

  abstract fun transactionDao(): TransactionDao

  companion object {
    @Volatile private var instance: CholaDatabase? = null

    fun getInstance(context: Context): CholaDatabase {
      return instance
        ?: synchronized(this) {
          instance
            ?: Room.databaseBuilder(
                context.applicationContext,
                CholaDatabase::class.java,
                "chola_empire_accounts.db",
              )
              .build()
              .also { instance = it }
        }
    }
  }
}
