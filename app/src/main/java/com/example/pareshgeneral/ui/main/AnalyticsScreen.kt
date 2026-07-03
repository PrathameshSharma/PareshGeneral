package com.example.pareshgeneral.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pareshgeneral.data.Rental
import com.example.pareshgeneral.data.RentalRepository
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen(repository: RentalRepository) {
    val context = LocalContext.current
    val rentals by repository.rentals.collectAsStateWithLifecycle()

    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH) + 1
    val currentYear = calendar.get(Calendar.YEAR)
    val now = Calendar.getInstance()

    // 1. Calculations
    val activeRentals = remember(rentals) { rentals.filter { !it.isReceived } }
    val totalActiveCount = activeRentals.size
    val outstandingBalance = remember(activeRentals) { activeRentals.sumOf { it.balance } }

    var monthlyRevenue = 0.0
    var yearlyRevenue = 0.0

    for (rental in rentals) {
        val parts = rental.date.split("/")
        if (parts.size == 3) {
            val rentMonth = parts[1].toIntOrNull() ?: 0
            val rentYear = parts[2].toIntOrNull() ?: 0
            if (rentYear == currentYear) {
                yearlyRevenue += rental.rent
                if (rentMonth == currentMonth) {
                    monthlyRevenue += rental.rent
                }
            }
        }
    }

    // 2. Return Warning Parsing
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()) }
    val todayDayOfYear = now.get(Calendar.DAY_OF_YEAR)
    val todayYear = now.get(Calendar.YEAR)

    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    val tomorrowDayOfYear = tomorrow.get(Calendar.DAY_OF_YEAR)
    val tomorrowYear = tomorrow.get(Calendar.YEAR)

    val overdueAlerts = remember(activeRentals) { mutableListOf<Rental>() }
    val dueTodayAlerts = remember(activeRentals) { mutableListOf<Rental>() }
    val dueTomorrowAlerts = remember(activeRentals) { mutableListOf<Rental>() }

    // Clear lists on recalculations to avoid duplicates
    overdueAlerts.clear()
    dueTodayAlerts.clear()
    dueTomorrowAlerts.clear()

    for (rental in activeRentals) {
        if (rental.returnDate.isNotBlank()) {
            try {
                val returnTime = sdf.parse(rental.returnDate)
                if (returnTime != null) {
                    val returnCal = Calendar.getInstance().apply { time = returnTime }
                    if (returnCal.timeInMillis < now.timeInMillis) {
                        overdueAlerts.add(rental)
                    } else if (returnCal.get(Calendar.YEAR) == todayYear && returnCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear) {
                        dueTodayAlerts.add(rental)
                    } else if (returnCal.get(Calendar.YEAR) == tomorrowYear && returnCal.get(Calendar.DAY_OF_YEAR) == tomorrowDayOfYear) {
                        dueTomorrowAlerts.add(rental)
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    // 3. Popular Inventory calculations
    val popularItems = remember(rentals) {
        rentals.filter { it.jewelryDetails.isNotBlank() }
            .groupBy {
                val rawName = it.jewelryDetails.trim()
                if (rawName.isNotEmpty()) {
                    rawName.substring(0, 1).uppercase(Locale.getDefault()) + rawName.substring(1).lowercase(Locale.getDefault())
                } else {
                    "Unnamed Jewellery"
                }
            }
            .map { (name, list) -> Pair(name, list.size) }
            .sortedByDescending { it.second }
            .take(5)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Welcome and Month display
        val monthName = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()) }
        Text(
            text = "Performance Dashboard",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4A0E17)
        )
        Text(
            text = "Operational metrics for $monthName",
            fontSize = 13.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 2x2 Grid of metrics
        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                title = "Active Rentals",
                value = "$totalActiveCount sets",
                subText = "Currently rented out",
                icon = Icons.Default.Info,
                color = Color(0xFF4A0E17),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            MetricCard(
                title = "Pending Balance",
                value = "Rs. ${String.format(Locale.US, "%,.0f", outstandingBalance)}",
                subText = "Outstanding collection",
                icon = Icons.Default.Warning,
                color = Color(0xFFD4AF37),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                title = "Monthly Revenue",
                value = "Rs. ${String.format(Locale.US, "%,.0f", monthlyRevenue)}",
                subText = "Earned this month",
                icon = Icons.Default.Star,
                color = Color(0xFF2E7D32),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            MetricCard(
                title = "Yearly Revenue",
                value = "Rs. ${String.format(Locale.US, "%,.0f", yearlyRevenue)}",
                subText = "Earned in $currentYear",
                icon = Icons.Default.Star,
                color = Color(0xFF1565C0),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Return Warnings Panel
        Text(
            text = "Return Warnings & Alerts",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4A0E17)
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (overdueAlerts.isEmpty() && dueTodayAlerts.isEmpty() && dueTomorrowAlerts.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "No alerts",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("All caught up!", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 14.sp)
                        Text("No pending or overdue jewelry returns found.", color = Color(0xFF2E7D32).copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                overdueAlerts.forEach { WarningItem(context, it, "Overdue", Color.Red) }
                dueTodayAlerts.forEach { WarningItem(context, it, "Due Today", Color(0xFFFF9800)) }
                dueTomorrowAlerts.forEach { WarningItem(context, it, "Due Tomorrow", Color(0xFF1976D2)) }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Popular Inventory Panel
        Text(
            text = "Most Popular Jewellery Items",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4A0E17)
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (popularItems.isEmpty()) {
            Text("No jewelry descriptions registered yet.", color = Color.Gray, fontSize = 12.sp)
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val maxRentals = popularItems.firstOrNull()?.second ?: 1
                    popularItems.forEach { (name, count) ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.DarkGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$count rentals",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4A0E17)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            // Visual bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray.copy(alpha = 0.3f))
                            ) {
                                val fraction = count.toFloat() / maxRentals.toFloat()
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .height(8.dp)
                                        .background(Color(0xFFD4AF37))
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subText: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(116.dp)
            .border(1.dp, Color(0xFFD4AF37).copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Text(
                    text = value,
                    color = Color(0xFF4A0E17),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subText,
                    color = Color.LightGray,
                    fontSize = 9.sp,
                    lineHeight = 10.sp
                )
            }
        }
    }
}

@Composable
fun WarningItem(context: Context, rental: Rental, status: String, color: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(color.copy(alpha = 0.1f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = status.uppercase(Locale.getDefault()),
                            color = color,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = rental.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Jewellery: ${rental.jewelryDetails}",
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Return Date: ${rental.returnDate}",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { triggerWhatsAppReminder(context, rental) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366), contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(34.dp)
                    .width(86.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("WhatsApp", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun triggerWhatsAppReminder(context: Context, rental: Rental) {
    val message = """
*परेश जनरल (Paresh General)*
ज्वेलरी रिमाइंडर (Jewellery Return Reminder)
----------------------------------------
प्रिय *${rental.name}*,
यह आपके द्वारा किराए पर ली गई ज्वेलरी वापस करने की तारीख का एक विनम्र निवेदन है।

*ज्वेलरी विवरण:* ${rental.jewelryDetails}
*वापसी का समय:* ${rental.returnDate}
*बाकी राशि (Balance):* Rs. ${rental.balance}

कृपया समय पर ज्वेलरी लौटाने में सहयोग करें। धन्यवाद!
    """.trimIndent()

    try {
        val encodedMessage = URLEncoder.encode(message, "UTF-8")
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
