package com.sintelis.timemark.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.sintelis.timemark.domain.SessionState
import java.io.File
import java.time.LocalDate
import java.util.concurrent.Executors

class SessionStore(context: Context) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val sessionFile = File(context.filesDir, "timemark_session.json")
    private val ioExecutor = Executors.newSingleThreadExecutor()

    fun load(): SessionState {
        if (!sessionFile.exists()) {
            return defaultSession()
        }
        return try {
            val raw = sessionFile.readText(Charsets.UTF_8)
            val loaded = gson.fromJson(raw, SessionState::class.java) ?: return defaultSession()
            val normalized = normalizeLoadedSession(loaded)
            if (normalized != loaded) {
                save(normalized)
            }
            normalized
        } catch (_: Exception) {
            defaultSession()
        }
    }

    /**
     * Guards against Gson's Unsafe deserialization leaving newly added Kotlin fields as null
     * when upgrading from an older timemark_session.json schema.
     */
    @Suppress("USELESS_ELVIS")
    private fun normalizeLoadedSession(loaded: SessionState): SessionState {
        val safeConfig = loaded.watermarkConfig ?: com.sintelis.timemark.domain.WatermarkConfig()
        val cleanedBrand = if (loaded.watermarkSchemaVersion < 2 && safeConfig.brandSubText == "Sintelis 1.21") {
            ""
        } else {
            safeConfig.brandSubText ?: ""
        }
        val safeRows = (loaded.rows ?: emptyList()).map { row ->
            row.copy(
                photos = row.photos ?: emptyMap(),
                inspectionPhotos = row.inspectionPhotos ?: emptyList()
            )
        }
        val safeLocation = loaded.location ?: com.sintelis.timemark.domain.LocationState()
        val today = LocalDate.now().toString()
        return loaded.copy(
            workDateIso = loaded.workDateIso ?: today,
            rows = safeRows,
            location = safeLocation.copy(
                addressLine1 = safeLocation.addressLine1 ?: "Area Stasiun / Jalur KA",
                addressLine2 = safeLocation.addressLine2 ?: "UPT Resor Sintelis 1.21 Bogor"
            ),
            watermarkConfig = safeConfig.copy(
                brandTopText = safeConfig.brandTopText ?: "Timemark",
                brandSubText = cleanedBrand,
                dateFormatStyle = safeConfig.dateFormatStyle ?: com.sintelis.timemark.domain.DateFormatStyle.PILL_SHORT_MONTH
            ),
            manualDateIso = loaded.manualDateIso ?: (loaded.workDateIso ?: today),
            watermarkSchemaVersion = 2
        )
    }

    fun save(state: SessionState) {
        val json = gson.toJson(state)
        ioExecutor.execute {
            runCatching {
                val tmp = File(sessionFile.parentFile, "${sessionFile.name}.tmp")
                tmp.writeText(json, Charsets.UTF_8)
                if (!tmp.renameTo(sessionFile)) {
                    sessionFile.writeText(json, Charsets.UTF_8)
                    tmp.delete()
                }
            }
        }
    }

    fun defaultSession(): SessionState {
        val today = LocalDate.now().toString()
        return SessionState(
            workDateIso = today,
            globalStartMinuteOfDay = 8 * 60,
            manualDateIso = today,
            manualMinuteOfDay = 8 * 60
        )
    }
}
