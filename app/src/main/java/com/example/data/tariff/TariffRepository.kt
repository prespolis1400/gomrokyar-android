package com.example.data.tariff

import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

data class IndexedTariff(
    val dto: TariffItemDto,
    val normalizedCode: String,
    val normalizedTitle: String
)

class TariffRepository(private val context: Context) {

    private val mutex = Mutex()
    private var cachedTariffs: List<IndexedTariff>? = null

    suspend fun preloadIfNeeded() = withContext(Dispatchers.IO) {
        ensureLoaded()
    }

    private suspend fun ensureLoaded(): List<IndexedTariff> {
        cachedTariffs?.let { return it }
        return mutex.withLock {
            cachedTariffs?.let { return it }
            val loaded = loadFromAssets()
            cachedTariffs = loaded
            loaded
        }
    }

    private fun loadFromAssets(): List<IndexedTariff> {
        val list = ArrayList<IndexedTariff>(9100)
        try {
            context.assets.open("tariffs.json").use { inputStream ->
                JsonReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.beginArray()
                    var idCounter = 1L

                    while (reader.hasNext()) {
                        reader.beginObject()
                        var rawCode = ""
                        var title = ""
                        var customsDuty = 4.0
                        var commercialBenefit = 0.0
                        var commercialBenefitNote: String? = null

                        while (reader.hasNext()) {
                            when (reader.nextName()) {
                                "ردیف تعرفه" -> {
                                    rawCode = when (reader.peek()) {
                                        JsonToken.NUMBER -> reader.nextLong().toString()
                                        JsonToken.STRING -> reader.nextString()
                                        else -> {
                                            reader.skipValue()
                                            ""
                                        }
                                    }.trim()
                                }
                                "شرح فارسی" -> {
                                    title = if (reader.peek() == JsonToken.STRING) {
                                        reader.nextString().trim()
                                    } else {
                                        reader.skipValue()
                                        ""
                                    }
                                }
                                "حقوق گمرکی" -> {
                                    when (reader.peek()) {
                                        JsonToken.NUMBER -> customsDuty = reader.nextDouble()
                                        JsonToken.STRING -> {
                                            val s = reader.nextString().trim()
                                            customsDuty = s.toDoubleOrNull() ?: 4.0
                                        }
                                        else -> {
                                            reader.skipValue()
                                            customsDuty = 4.0
                                        }
                                    }
                                }
                                "سود بازرگانی" -> {
                                    when (reader.peek()) {
                                        JsonToken.NUMBER -> commercialBenefit = reader.nextDouble()
                                        JsonToken.STRING -> {
                                            val s = reader.nextString().trim()
                                            if (s == "صفر" || s == "0") {
                                                commercialBenefit = 0.0
                                            } else {
                                                val parsed = s.toDoubleOrNull()
                                                if (parsed != null) {
                                                    commercialBenefit = parsed
                                                } else {
                                                    commercialBenefitNote = s
                                                    // Extract percent rates from condition
                                                    val percentMatches = Regex("""(\d+)\s*درصد""").findAll(s)
                                                        .mapNotNull { it.groupValues[1].toDoubleOrNull() }
                                                        .toList()
                                                    commercialBenefit = if (percentMatches.isNotEmpty()) {
                                                        percentMatches.last() // standard/higher rate
                                                    } else {
                                                        val allDigits = Regex("""\d+""").findAll(s)
                                                            .mapNotNull { it.value.toDoubleOrNull() }
                                                            .toList()
                                                        if (allDigits.isNotEmpty()) allDigits.last() else 0.0
                                                    }
                                                }
                                            }
                                        }
                                        else -> {
                                            reader.skipValue()
                                            commercialBenefit = 0.0
                                        }
                                    }
                                }
                                else -> reader.skipValue()
                            }
                        }
                        reader.endObject()

                        // Ensure 8-digit representation with leading zeros
                        val cleanCode = if (rawCode.isNotEmpty() && rawCode.length < 8 && rawCode.all { it.isDigit() }) {
                            rawCode.padStart(8, '0')
                        } else {
                            rawCode
                        }

                        val importDuty = customsDuty + commercialBenefit

                        val dto = TariffItemDto(
                            id = idCounter++,
                            tariffCode = cleanCode,
                            tariffTitle = title,
                            suq = null,
                            customsDuty = customsDuty,
                            commercialBenefit = commercialBenefit,
                            importDuty = importDuty,
                            commercialBenefitNote = commercialBenefitNote
                        )

                        list.add(
                            IndexedTariff(
                                dto = dto,
                                normalizedCode = PersianNormalizer.normalize(cleanCode),
                                normalizedTitle = PersianNormalizer.normalize(title)
                            )
                        )
                    }
                    reader.endArray()
                }
            }
        } catch (e: Exception) {
            Log.e("TariffRepository", "Error reading tariffs.json from assets", e)
        }
        return list
    }

    suspend fun getInitialItems(limit: Int = 30): List<TariffItemDto> = withContext(Dispatchers.Default) {
        val all = ensureLoaded()
        all.take(limit).map { it.dto }
    }

    suspend fun search(query: String, limit: Int = 80): List<TariffItemDto> = withContext(Dispatchers.Default) {
        val all = ensureLoaded()
        val normQuery = PersianNormalizer.normalize(query)
        if (normQuery.isBlank()) {
            return@withContext all.take(limit).map { it.dto }
        }

        val isNumeric = normQuery.all { it.isDigit() }
        val tokens = normQuery.split(" ").filter { it.isNotBlank() }

        if (isNumeric) {
            // Search primarily by HS Code
            val exactPrefix = ArrayList<TariffItemDto>()
            val containsCode = ArrayList<TariffItemDto>()

            for (item in all) {
                if (item.normalizedCode.startsWith(normQuery)) {
                    exactPrefix.add(item.dto)
                    if (exactPrefix.size >= limit) break
                } else if (item.normalizedCode.contains(normQuery)) {
                    containsCode.add(item.dto)
                }
            }

            return@withContext (exactPrefix + containsCode).take(limit)
        }

        // Text search across Persian titles & codes
        val scoredList = ArrayList<Pair<Int, TariffItemDto>>()

        for (item in all) {
            val title = item.normalizedTitle
            val code = item.normalizedCode

            // Check if all tokens match
            var allMatch = true
            for (token in tokens) {
                if (!title.contains(token) && !code.contains(token)) {
                    allMatch = false
                    break
                }
            }

            if (allMatch) {
                var score = 0
                if (title.startsWith(normQuery)) score += 100
                if (title.contains(normQuery)) score += 50
                if (code.contains(normQuery)) score += 30

                // Bonus if first word matches
                if (tokens.isNotEmpty() && title.startsWith(tokens[0])) score += 20

                scoredList.add(score to item.dto)
            }
        }

        scoredList.sortedByDescending { it.first }
            .take(limit)
            .map { it.second }
    }
}
