package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.flow.Flow
import retrofit2.http.GET

@Entity(tableName = "currencies")
data class CurrencyEntity(
    @PrimaryKey
    val code: String,
    val name: String,
    val rate: Double
)

@Dao
interface CurrencyDao {
    @Query("SELECT * FROM currencies ORDER BY name ASC")
    fun getAllCurrencies(): Flow<List<CurrencyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrencies(currencies: List<CurrencyEntity>)

    @Query("SELECT COUNT(*) FROM currencies")
    suspend fun getCount(): Int
}

@Database(entities = [CurrencyEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun currencyDao(): CurrencyDao
}

@JsonClass(generateAdapter = true)
data class CurrencyResponse(
    val success: Boolean,
    val data: List<CurrencyItem>?
)

@JsonClass(generateAdapter = true)
data class CurrencyItem(
    val code: String,
    val currency: String,
    val rate: Double
)

interface CurrencyApi {
    @GET("/")
    suspend fun getCurrencies(): CurrencyResponse
}

class CurrencyRepository(
    private val currencyDao: CurrencyDao,
    private val currencyApi: CurrencyApi
) {
    val allCurrencies: Flow<List<CurrencyEntity>> = currencyDao.getAllCurrencies()

    suspend fun refreshCurrenciesIfNeeded() {
        try {
            val count = currencyDao.getCount()
            if (count == 0) {
                forceRefreshCurrencies()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun forceRefreshCurrencies() {
        try {
            val response = currencyApi.getCurrencies()
            if (response.success && response.data != null) {
                val entities = response.data.map {
                    CurrencyEntity(
                        code = it.code,
                        name = it.currency,
                        rate = it.rate
                    )
                }
                currencyDao.insertCurrencies(entities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // In case of error (e.g. offline on first run), we can insert fallback currencies
            val count = currencyDao.getCount()
            if (count == 0) {
                insertFallbackCurrencies()
            }
        }
    }

    private suspend fun insertFallbackCurrencies() {
        val fallback = listOf(
            CurrencyEntity("USD", "دلار آمریکا (USD)", 1310661.0),
            CurrencyEntity("EUR", "یورو (EUR)", 1551964.0),
            CurrencyEntity("AED", "درهم امارات (AED)", 356885.0)
        )
        currencyDao.insertCurrencies(fallback)
    }
}
