package com.liuqingyue.chainsight

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*

@Entity
data class Account(
    @PrimaryKey val uid: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "balance") val balance: Double,
    @ColumnInfo(name = "last_update") val lastUpdate: String,
)

@Entity
data class ManuallyAccount(
    @PrimaryKey val uid: String,
    @ColumnInfo(name = "contract") val contract: String,
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "account") val account: String,
    @ColumnInfo(name = "symbol") val symbol: String,
    @ColumnInfo(name = "price") val price: Double,
    @ColumnInfo(name = "change24h") val change24h: Double,
)

@Dao
interface ManuallyAccountDao {
    @Query("SELECT * FROM ManuallyAccount")
    fun getAll(): List<ManuallyAccount>

    @Query("SELECT * FROM ManuallyAccount WHERE account IN (:account)")
    fun loadByAccountId(account: String): LiveData<List<ManuallyAccount>>

    @Query("SELECT * FROM ManuallyAccount WHERE account IN (:account)")
    fun loadByAccountIdWithoutLive(account: String): List<ManuallyAccount>

    @Update
    fun update(manuallyAccount: ManuallyAccount)

    @Insert
    fun insert(token: ManuallyAccount)
}

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

@Database(entities = [ManuallyAccount::class], version = 1, exportSchema = false)
abstract class AppDatabase2: RoomDatabase() {
    companion object{
        @Volatile
        private var INSTANCE: AppDatabase2? = null

        fun getDatabase(context: Context): AppDatabase2 {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase2::class.java,
                    "manually_account_database"
                ).allowMainThreadQueries().build()
                INSTANCE = instance
                instance
            }
        }

    }
    abstract fun getManuallyAccountDao(): ManuallyAccountDao
}