package com.example.pareshgeneral.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GoogleSheetsSync {
    private val client = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun sendToGoogleSheet(apiUrl: String, rental: Rental): Boolean = withContext(Dispatchers.IO) {
        if (apiUrl.isBlank()) {
            Log.w("GoogleSheetsSync", "API URL is blank")
            return@withContext false
        }
        try {
            val json = JSONObject().apply {
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
            }

            var currentUrl = apiUrl
            var redirectCount = 0
            val maxRedirects = 5
            var isPost = true

            while (redirectCount < maxRedirects) {
                val requestBuilder = Request.Builder().url(currentUrl)
                val request = if (isPost) {
                    val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)
                    requestBuilder.post(body).build()
                } else {
                    requestBuilder.get().build()
                }

                client.newCall(request).execute().use { response ->
                    val code = response.code
                    Log.d("GoogleSheetsSync", "URL: $currentUrl, Response Code: $code")

                    if (code == 302 || code == 307 || code == 308 || code == 303) {
                        val location = response.header("Location")
                        if (location != null) {
                            currentUrl = location
                            redirectCount++
                            isPost = false
                            Log.d("GoogleSheetsSync", "Redirecting to: $location (Count: $redirectCount)")
                            return@use // continue loop in outside scope
                        }
                    }

                    val isSuccess = response.isSuccessful
                    val bodyStr = response.body?.string() ?: ""
                    Log.d("GoogleSheetsSync", "Success: $isSuccess, Body: $bodyStr")
                    return@withContext isSuccess
                }
            }
            Log.e("GoogleSheetsSync", "Too many redirects")
            return@withContext false
        } catch (e: Exception) {
            Log.e("GoogleSheetsSync", "Error uploading to sheet", e)
            return@withContext false
        }
    }
}
