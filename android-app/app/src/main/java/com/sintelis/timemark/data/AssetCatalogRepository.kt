package com.sintelis.timemark.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.sintelis.timemark.domain.AssetCatalog
import java.io.File

class AssetCatalogRepository(private val context: Context) {

    private val gson = Gson()
    private val overrideFile = File(context.filesDir, "custom_assets.json")

    fun loadCatalog(): AssetCatalog {
        val rawJson = if (overrideFile.exists()) {
            overrideFile.readText(Charsets.UTF_8)
        } else {
            context.assets.open("data_acuan_tenaga_gabungan.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
        }
        return gson.fromJson(rawJson, AssetCatalog::class.java)
    }

    fun importCatalogFromUri(uri: Uri): AssetCatalog {
        val rawJson = context.contentResolver.openInputStream(uri)
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            ?: error("Tidak dapat membaca file JSON yang dipilih.")

        val parsed = gson.fromJson(rawJson, AssetCatalog::class.java)
        require(parsed.aset.isNotEmpty()) {
            "File JSON tidak memiliki daftar 'aset' yang valid."
        }
        overrideFile.writeText(rawJson, Charsets.UTF_8)
        return parsed
    }

    fun resetToBundled(): AssetCatalog {
        if (overrideFile.exists()) {
            overrideFile.delete()
        }
        return loadCatalog()
    }

    fun isUsingCustomCatalog(): Boolean = overrideFile.exists()
}
