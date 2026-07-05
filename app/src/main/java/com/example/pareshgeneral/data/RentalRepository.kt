package com.example.pareshgeneral.data

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class RentalRepository(private val context: Context) {

    private val rentalsFile = File(context.filesDir, "rentals.json")
    private val imagesDir = File(context.filesDir, "images").apply {
        if (!exists()) mkdirs()
    }

    private val allRentals = mutableListOf<Rental>()
    private val _rentals = MutableStateFlow<List<Rental>>(emptyList())
    val rentals: StateFlow<List<Rental>> = _rentals.asStateFlow()

    init {
        loadRentals()
    }

    private fun loadRentals() {
        if (!rentalsFile.exists()) {
            _rentals.value = emptyList()
            return
        }
        try {
            val jsonString = rentalsFile.readText()
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<Rental>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val imagesArray = obj.optJSONArray("images")
                val imagesList = mutableListOf<String>()
                if (imagesArray != null) {
                    for (j in 0 until imagesArray.length()) {
                        imagesList.add(imagesArray.getString(j))
                    }
                }
                list.add(
                    Rental(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        date = obj.optString("date", ""),
                        name = obj.optString("name", ""),
                        contact = obj.optString("contact", ""),
                        jewelryNo = obj.optString("jewelryNo", ""),
                        jewelryDetails = obj.optString("jewelryDetails", ""),
                        deliveryDate = obj.optString("deliveryDate", ""),
                        returnDate = obj.optString("returnDate", ""),
                        rent = obj.optDouble("rent", 0.0),
                        advance = obj.optDouble("advance", 0.0),
                        balance = obj.optDouble("balance", 0.0),
                        refundAmount = obj.optDouble("refundAmount", 0.0),
                        images = imagesList,
                        isReceived = obj.optBoolean("isReceived", false),
                        lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis()),
                        isDeleted = obj.optBoolean("isDeleted", false)
                    )
                )
            }
            allRentals.clear()
            allRentals.addAll(list)
            _rentals.value = list.filter { !it.isDeleted }
        } catch (e: Exception) {
            Log.e("RentalRepository", "Error loading rentals", e)
            _rentals.value = emptyList()
        }
    }

    private fun saveAllRentals(list: List<Rental>) {
        try {
            val jsonArray = JSONArray()
            for (rental in list) {
                val obj = JSONObject().apply {
                    put("id", rental.id)
                    put("date", rental.date)
                    put("name", rental.name)
                    put("contact", rental.contact)
                    put("jewelryNo", rental.jewelryNo)
                    put("jewelryDetails", rental.jewelryDetails)
                    put("deliveryDate", rental.deliveryDate)
                    put("returnDate", rental.returnDate)
                    put("rent", rental.rent)
                    put("advance", rental.advance)
                    put("balance", rental.balance)
                    put("refundAmount", rental.refundAmount)
                    put("images", JSONArray(rental.images))
                    put("isReceived", rental.isReceived)
                    put("lastUpdated", rental.lastUpdated)
                    put("isDeleted", rental.isDeleted)
                }
                jsonArray.put(obj)
            }
            rentalsFile.writeText(jsonArray.toString())
            _rentals.value = list.filter { !it.isDeleted }
        } catch (e: Exception) {
            Log.e("RentalRepository", "Error saving rentals", e)
        }
    }

    fun saveRental(rental: Rental, tempImageUris: List<Uri>): Rental {
        val copiedImagePaths = mutableListOf<String>()
        for (uri in tempImageUris) {
            val fileExtension = getFileExtension(uri) ?: "jpg"
            val newFile = File(imagesDir, "img_${UUID.randomUUID()}.$fileExtension")
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(newFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                copiedImagePaths.add(newFile.absolutePath)
            } catch (e: Exception) {
                Log.e("RentalRepository", "Error copying image: $uri", e)
                if (uri.scheme == "file") {
                    val file = File(uri.path ?: "")
                    if (file.exists()) {
                        copiedImagePaths.add(file.absolutePath)
                    }
                }
            }
        }

        val finalRental = rental.copy(
            images = copiedImagePaths,
            lastUpdated = System.currentTimeMillis()
        )
        allRentals.add(0, finalRental)
        saveAllRentals(allRentals)
        return finalRental
    }

    fun deleteRental(rentalId: String) {
        val index = allRentals.indexOfFirst { it.id == rentalId }
        if (index != -1) {
            val original = allRentals[index]
            val updated = original.copy(
                isDeleted = true,
                lastUpdated = System.currentTimeMillis()
            )
            allRentals[index] = updated
            saveAllRentals(allRentals)
        }
    }

    fun updateReceivedStatus(rentalId: String, isReceived: Boolean) {
        val index = allRentals.indexOfFirst { it.id == rentalId }
        if (index != -1) {
            val original = allRentals[index]
            val updatedRental = original.copy(
                isReceived = isReceived,
                balance = if (isReceived) 0.0 else original.balance,
                lastUpdated = System.currentTimeMillis()
            )
            allRentals[index] = updatedRental
            saveAllRentals(allRentals)
        }
    }

    fun getAllRentalsForSync(): List<Rental> {
        return allRentals.toList()
    }

    fun mergeSyncedRentals(syncedList: List<Rental>) {
        val syncedMap = syncedList.associateBy { it.id }
        val mergedList = mutableListOf<Rental>()
        val processedIds = mutableSetOf<String>()

        for (local in allRentals) {
            val synced = syncedMap[local.id]
            if (synced != null) {
                if (synced.lastUpdated >= local.lastUpdated) {
                    mergedList.add(synced)
                } else {
                    mergedList.add(local)
                }
            } else {
                mergedList.add(local)
            }
            processedIds.add(local.id)
        }

        for (synced in syncedList) {
            if (!processedIds.contains(synced.id)) {
                mergedList.add(synced)
            }
        }

        // Cleanup local files for soft-deleted items
        for (rental in mergedList) {
            if (rental.isDeleted) {
                for (path in rental.images) {
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }
        }

        allRentals.clear()
        allRentals.addAll(mergedList)
        saveAllRentals(allRentals)
    }

    fun getGoogleSheetUrl(): String {
        val prefs = context.getSharedPreferences("PareshGeneralPrefs", Context.MODE_PRIVATE)
        val saved = prefs.getString("google_sheet_url", "")
        val defaultUrl = "https://script.google.com/macros/s/AKfycbxERgNVm7jCQuvxw5i2-ahCeHhRxPtX7l65az7Ih-3cvD6xD2ra_IjR_p_tpQSd_e9HNw/exec"
        return if (saved.isNullOrBlank()) defaultUrl else saved
    }

    fun saveGoogleSheetUrl(url: String) {
        val prefs = context.getSharedPreferences("PareshGeneralPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("google_sheet_url", url).apply()
    }

    private fun getFileExtension(uri: Uri): String? {
        return try {
            val mimeType = context.contentResolver.getType(uri)
            mimeType?.substringAfterLast('/')
        } catch (e: Exception) {
            null
        }
    }
}
