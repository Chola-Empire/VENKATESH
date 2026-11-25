package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
  @Query("SELECT * FROM clients ORDER BY name ASC")
  fun observeAll(): Flow<List<ClientEntity>>

  @Query("SELECT * FROM clients ORDER BY name ASC")
  suspend fun getAll(): List<ClientEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(client: ClientEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(clients: List<ClientEntity>)

  @Query("DELETE FROM clients WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM clients")
  suspend fun deleteAll()
}

@Dao
interface ProjectDao {
  @Query("SELECT * FROM projects ORDER BY createdAt DESC")
  fun observeAll(): Flow<List<ProjectEntity>>

  @Query("SELECT * FROM projects ORDER BY createdAt DESC")
  suspend fun getAll(): List<ProjectEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(project: ProjectEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(projects: List<ProjectEntity>)

  @Query("DELETE FROM projects WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM projects")
  suspend fun deleteAll()
}

@Dao
interface TransactionDao {
  @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, createdAt DESC")
  fun observeAll(): Flow<List<TransactionEntity>>

  @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, createdAt DESC")
  suspend fun getAll(): List<TransactionEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(transaction: TransactionEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(transactions: List<TransactionEntity>)

  @Query("DELETE FROM transactions WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM transactions")
  suspend fun deleteAll()
}
