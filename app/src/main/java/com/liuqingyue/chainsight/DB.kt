package com.liuqingyue.chainsight

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity
data class Account(
    @PrimaryKey val uid: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "balance") val balance: Double,
    @ColumnInfo(name = "last_update") val lastUpdate: String,
)

@Dao
interface AccountDao {
    @Query("SELECT * FROM Account")
    fun getAll(): List<Account>

    @Query("SELECT * FROM Account")
    fun getObserverAll(): LiveData<List<Account>>

    @Query("SELECT * FROM Account WHERE uid IN (:id)")
    fun loadById(id: String): Account

    @Query("SELECT * FROM Account WHERE name In (:name)")
    fun findByName(name: String): Account

    @Update
    fun update(account: Account)

    @Insert
    fun insert(account: Account)

    @Delete
    fun delete(account: Account)
}

@Database(entities = [Account::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    companion object{
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "account_database"
                ).allowMainThreadQueries().build()
                INSTANCE = instance
                instance
            }
        }

    }
    abstract fun getAccountDao(): AccountDao
}