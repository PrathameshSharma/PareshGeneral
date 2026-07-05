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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    val monthsList = listOf(
        "January", "February", "March", "April", "May", "June", 
        "July", "August", "September", "October", "November", "December"
    )
    val yearsList = listOf("2024", "2025", "2026", "2027", "2028", "2029", "2030")

    var selectedMonthIndex by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR).toString()) }

    var showMonthMenu by remember { mutableStateOf(false) }
    var showYearMenu by remember { mutableStateOf(false) }

    // 1. Calculations
    val activeRentals = remember(rentals) { rentals.filter { !it.isReceived } }
    val totalActiveCount = activeRentals.size
    val outstandingBalance = remember(activeRentals) { activeRentals.sumOf { it.balance } }

    val selectedMonthNum = selectedMonthIndex + 1
    val selectedYearNum = selectedYear.toIntOrNull() ?: currentYear

    var monthlyRevenue = 0.0
    var yearlyRevenue = 0.0

    for (rental in rentals) {
        if (rental.deliveryDate.isNotBlank()) {
            val datePart = rental.deliveryDate.substringBefore(' ')
            val parts = datePart.split("/")
            if (parts.size == 3) {
                val delMonth = parts[1].toIntOrNull() ?: 0
                val delYear = parts[2].toIntOrNull() ?: 0
                if (delYear == selectedYearNum) {
                    yearlyRevenue += rental.rent
                    if (delMonth == selectedMonthNum) {
                        monthlyRevenue += rental.rent
                    }
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
        Text(
            text = "Performance Dashboard",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Operational metrics for ${monthsList[selectedMonthIndex]} $selectedYear",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Month & Year Selector Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Month Selector Card
            Box(modifier = Modifier.weight(1f)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMonthMenu = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = monthsList[selectedMonthIndex],
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Month",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                DropdownMenu(
                    expanded = showMonthMenu,
                    onDismissRequest = { showMonthMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    monthsList.forEachIndexed { index, mName ->
                        DropdownMenuItem(
                            text = { Text(mName, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium) },
                            onClick = {
                                selectedMonthIndex = index
                                showMonthMenu = false
                            }
                        )
                    }
                }
            }

            // Year Selector Card
            Box(modifier = Modifier.weight(1f)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showYearMenu = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedYear,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Year",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                DropdownMenu(
                    expanded = showYearMenu,
                    onDismissRequest = { showYearMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    yearsList.forEach { yName ->
                        DropdownMenuItem(
                            text = { Text(yName, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium) },
                            onClick = {
                                selectedYear = yName
                                showYearMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2x2 Grid of metrics
        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                title = "Active Rentals",
                value = "$totalActiveCount sets",
                subText = "Currently rented out",
                icon = Icons.Default.Info,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            MetricCard(
                title = "Pending Balance",
                value = "Rs. ${String.format(Locale.US, "%,.0f", outstandingBalance)}",
                subText = "Outstanding collection",
                icon = Icons.Default.Warning,
                color = MaterialTheme.colorScheme.secondary,
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
                color = Color(0xFF43A047),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            MetricCard(
                title = "Yearly Revenue",
                value = "Rs. ${String.format(Locale.US, "%,.0f", yearlyRevenue)}",
                subText = "Earned in $currentYear",
                icon = Icons.Default.Star,
                color = Color(0xFF1E88E5),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Return Warnings Panel
        Text(
            text = "Return Warnings & Alerts",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (overdueAlerts.isEmpty() && dueTodayAlerts.isEmpty() && dueTomorrowAlerts.isEmpty()) {
            val isDark = androidx.compose.foundation.isSystemInDarkTheme()
            val greenColor = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
            val greenBg = if (isDark) Color(0xFF143B1A) else Color(0xFFE8F5E9)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = greenBg)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "No alerts",
                        tint = greenColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("All caught up!", fontWeight = FontWeight.Bold, color = greenColor, fontSize = 14.sp)
                        Text("No pending or overdue jewelry returns found.", color = greenColor.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            }
        } else {
            val isDark = androidx.compose.foundation.isSystemInDarkTheme()
            val overdueColor = if (isDark) Color(0xFFEF5350) else Color.Red
            val dueTodayColor = if (isDark) Color(0xFFFFB74D) else Color(0xFFFF9800)
            val dueTomorrowColor = if (isDark) Color(0xFF64B5F6) else Color(0xFF1976D2)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                overdueAlerts.forEach { WarningItem(context, it, "Overdue", overdueColor) }
                dueTodayAlerts.forEach { WarningItem(context, it, "Due Today", dueTodayColor) }
                dueTomorrowAlerts.forEach { WarningItem(context, it, "Due Tomorrow", dueTomorrowColor) }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Popular Inventory Panel
        Text(
            text = "Most Popular Jewellery Items",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (popularItems.isEmpty()) {
            Text("No jewelry descriptions registered yet.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$count rentals",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            // Visual bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            ) {
                                val fraction = count.toFloat() / maxRentals.toFloat()
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .height(8.dp)
                                        .background(MaterialTheme.colorScheme.secondary)
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
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
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
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subText,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Jewellery: ${rental.jewelryDetails}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Return Date: ${rental.returnDate}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
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
