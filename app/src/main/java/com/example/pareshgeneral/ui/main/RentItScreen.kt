package com.example.pareshgeneral.ui.main

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.pareshgeneral.data.GoogleSheetsSync
import com.example.pareshgeneral.data.Rental
import com.example.pareshgeneral.data.RentalRepository
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.net.URLEncoder

@Composable
fun RentItScreen(repository: RentalRepository) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val initialDate = remember { dateFormat.format(Date()) }

    // Form state variables
    var date by remember { mutableStateOf(initialDate) }
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var jewelryNo by remember { mutableStateOf("") }
    var jewelryDetails by remember { mutableStateOf("") }
    var deliveryDate by remember { mutableStateOf("") }
    var returnDate by remember { mutableStateOf("") }
    var rent by remember { mutableStateOf("") }
    var advance by remember { mutableStateOf("") }

    val selectedImageUris = remember { mutableStateListOf<Uri>() }
    var tempCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }

    // Auto-calculate values
    val priceVal = jewelryNo.toDoubleOrNull() ?: 0.0
    val rentVal = rent.toDoubleOrNull() ?: 0.0
    val advanceVal = advance.toDoubleOrNull() ?: 0.0
    val balance = priceVal - advanceVal
    val refundVal = priceVal - rentVal

    // Image pickers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris.addAll(uris)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = tempCameraUriString?.let { Uri.parse(it) }
        if (success && uri != null) {
            selectedImageUris.add(uri)
        }
    }

    fun triggerCamera() {
        try {
            val directory = File(context.cacheDir, "camera_temp")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val tempFile = File(directory, "camera_capture_${System.currentTimeMillis()}.jpg")
            if (!tempFile.exists()) {
                tempFile.createNewFile()
            }

            val uri = FileProvider.getUriForFile(
                context,
                "com.example.pareshgeneral.fileprovider",
                tempFile
            )
            tempCameraUriString = uri.toString()
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Error opening camera: ${e.localizedMessage ?: e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    fun resetForm() {
        date = initialDate
        name = ""
        contact = ""
        jewelryNo = ""
        jewelryDetails = ""
        deliveryDate = ""
        returnDate = ""
        rent = ""
        advance = ""
        selectedImageUris.clear()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Invoice Header Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF4A0E17).copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Bridal Jewellery Bill Details",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A0E17)
                )
                Text(
                    text = "Feed the details to generate digital receipt",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Form Fields
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Date") },
                modifier = Modifier
                    .weight(1f)
                    .clickable { showDatePicker(context) { date = it } },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = Color.Black,
                    disabledBorderColor = Color.LightGray,
                    disabledLabelColor = Color.DarkGray
                )
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = contact,
            onValueChange = { contact = it },
            label = { Text("Contact No.") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = jewelryNo,
            onValueChange = { jewelryNo = it },
            label = { Text("Jewellery Price") },
            prefix = { Text("Rs. ") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = jewelryDetails,
            onValueChange = { jewelryDetails = it },
            label = { Text("Jewellery Details") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = deliveryDate,
            onValueChange = { },
            label = { Text("Delivery Date & Time") },
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDateTimePicker(context) { deliveryDate = it } },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = Color.Black,
                disabledBorderColor = Color.LightGray,
                disabledLabelColor = Color.DarkGray
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = returnDate,
            onValueChange = { },
            label = { Text("Return Date & Time") },
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDateTimePicker(context) { returnDate = it } },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = Color.Black,
                disabledBorderColor = Color.LightGray,
                disabledLabelColor = Color.DarkGray
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = rent,
                onValueChange = { rent = it },
                label = { Text("Rent") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = advance,
                onValueChange = { advance = it },
                label = { Text("Advance") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = String.format(Locale.US, "%.2f", balance),
                onValueChange = { },
                label = { Text("Balance") },
                enabled = false,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = Color(0xFF4A0E17),
                    disabledBorderColor = Color(0xFF4A0E17).copy(alpha = 0.5f),
                    disabledLabelColor = Color.DarkGray
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = String.format(Locale.US, "%.2f", refundVal),
                onValueChange = { },
                label = { Text("Refund Amount") },
                enabled = false,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = Color(0xFF4A0E17),
                    disabledBorderColor = Color(0xFF4A0E17).copy(alpha = 0.5f),
                    disabledLabelColor = Color.DarkGray
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Image Section
        Text(
            text = "Jewellery Photos",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4A0E17)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { galleryLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Gallery")
            }
            Button(
                onClick = { triggerCamera() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Camera")
            }
        }

        if (selectedImageUris.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(selectedImageUris) { index, uri ->
                    Box(modifier = Modifier.size(90.dp)) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Image preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.TopEnd)
                                .background(Color.Red.copy(alpha = 0.8f), CircleShape)
                                .clickable { selectedImageUris.removeAt(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove image",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Terms and conditions card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("नियम व शर्ते (Terms & Conditions):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF4A0E17))
                Spacer(modifier = Modifier.height(4.dp))
                val terms = listOf(
                    "एक बार ज्वेलरी बुक कराने के बाद उसे किसी भी कारण से कैंसिल नहीं किया जाएगा।",
                    "यदि ग्राहक द्वारा बुकिंग कैंसिल की जाती है तो एडवांस रिफंड नहीं होगा।",
                    "डिलीवरी के समय ग्राहक को आभूषणों की जांच स्वयं करना होगा।",
                    "किसी भी तरह के नुकसान, क्षति और ज्वेलरी के देर से लौटाने पर अतिरिक्त भुगतान करना होगा।",
                    "ज्वेलरी का किराया केवल २४ घंटे के लिए होगा।",
                    "अतिरिक्त समय लगने पे उसका भी किराया लिया जाएगा।"
                )
                terms.forEach { term ->
                    Text("• $term", fontSize = 11.sp, lineHeight = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Reset and Confirm Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = { resetForm() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4A0E17)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("Reset", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    if (name.isBlank() || contact.isBlank() || deliveryDate.isBlank() || returnDate.isBlank()) {
                        Toast.makeText(context, "Please fill Name, Contact, and Dates!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val rentalObj = Rental(
                        date = date,
                        name = name.trim(),
                        contact = contact.trim(),
                        jewelryNo = jewelryNo.trim(),
                        jewelryDetails = jewelryDetails.trim(),
                        deliveryDate = deliveryDate,
                        returnDate = returnDate,
                        rent = rentVal,
                        advance = advanceVal,
                        balance = balance,
                        refundAmount = refundVal
                    )

                    // 1. Save locally
                    val saved = repository.saveRental(rentalObj, selectedImageUris)

                    // 2. Upload to sheets asynchronously
                    val sheetUrl = repository.getGoogleSheetUrl()
                    if (sheetUrl.isNotBlank()) {
                        coroutineScope.launch {
                            val success = GoogleSheetsSync.sendToGoogleSheet(sheetUrl, saved)
                            if (success) {
                                Toast.makeText(context, "Synced to Google Sheets successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Google Sheet sync failed. Check URL.", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Saved locally. Google Sheet URL not configured.", Toast.LENGTH_LONG).show()
                    }

                    // 3. Share to WhatsApp
                    shareToWhatsApp(context, saved)

                    // Reset form after confirm
                    resetForm()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A0E17), contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp)
            ) {
                Text("Confirm", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

fun showDatePicker(context: Context, onDateSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val formattedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year)
            onDateSelected(formattedDate)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

fun showDateTimePicker(context: Context, onDateTimeSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    val timeCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                        set(Calendar.MINUTE, minute)
                    }
                    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
                    val formatted = sdf.format(timeCal.time)
                    onDateTimeSelected(formatted)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            ).show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

fun shareToWhatsApp(context: Context, rental: Rental) {
    val message = """
*परेश जनरल (Paresh General)*
Bridal Jewellery On Rent
मेन मार्केट, कपडा लाईन, वर्धा
Contact: 9923717890, 9158444806
----------------------------------------
*RECEIPT / INVOICE*
Date: ${rental.date}
Customer Name: ${rental.name}
Contact No.: ${rental.contact}
Jewellery Price: Rs. ${rental.jewelryNo}
Jewelry Details: ${rental.jewelryDetails}
Delivery: ${rental.deliveryDate}
Return: ${rental.returnDate}
Rent: Rs. ${rental.rent}
Advance: Rs. ${rental.advance}
*Balance: Rs. ${rental.balance}*
Refund Deposit: Rs. ${rental.refundAmount}
----------------------------------------
*नियम व शर्ते (Terms & Conditions):*
1. एक बार ज्वेलरी बुक कराने के बाद उसे किसी भी कारण से कैंसिल नहीं किया जाएगा।
2. यदि ग्राहक द्वारा बुकिंग कैंसिल की जाती है तो एडवांस रिफंड नहीं होगा।
3. डिलीवरी के समय ग्राहक को आभूषणों की जांच स्वयं करना होगा।
4. किसी भी तरह के नुकसान, क्षति और ज्वेलरी के देर से लौटाने पर अतिरिक्त भुगतान करना होगा।
5. ज्वेलरी का किराया केवल २४ घंटे के लिए होगा।
6. अतिरिक्त समय लगने पे उसका भी किराया लिया जाएगा।

मुझे सभी नियम एवं शर्ते स्वीकार हैं।
    """.trimIndent()

    try {
        val encodedMessage = URLEncoder.encode(message, "UTF-8")
        // Format phone number by prefixing 91 (India) if it is 10 digits
        var cleanPhone = rental.contact.filter { it.isDigit() }
        if (cleanPhone.length == 10) {
            cleanPhone = "91$cleanPhone"
        }
        val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "WhatsApp is not installed!", Toast.LENGTH_SHORT).show()
    }
}
