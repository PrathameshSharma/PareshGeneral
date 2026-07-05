package com.example.pareshgeneral.ui.main

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.pareshgeneral.data.Rental
import com.example.pareshgeneral.data.RentalRepository
import com.example.pareshgeneral.data.GoogleSheetsSync
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentedOutScreen(repository: RentalRepository) {
    val context = LocalContext.current
    val rentals by repository.rentals.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var isSyncing by remember { mutableStateOf(false) }

    var selectedRental by remember { mutableStateOf<Rental?>(null) }
    var isDeleteMode by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var rentalToDelete by remember { mutableStateOf<Rental?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var currentSortOption by remember { mutableStateOf("Invoice Date (Newest)") }
    var showSortMenu by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    val filteredRentals = remember(rentals, searchQuery, selectedFilter, currentSortOption) {
        var list = rentals.filter { rental ->
            val query = searchQuery.trim().lowercase()
            query.isBlank() || 
                rental.name.lowercase().contains(query) ||
                rental.contact.contains(query) ||
                rental.jewelryDetails.lowercase().contains(query)
        }

        if (selectedFilter != "All") {
            list = list.filter { getRentalStatus(it) == selectedFilter }
        }

        val dateParser = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val dateTimeParser = java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a", java.util.Locale.getDefault())

        val parseDateSafe = { dateStr: String ->
            try { dateParser.parse(dateStr)?.time ?: 0L } catch(e: Exception) { 0L }
        }
        val parseDateTimeSafe = { dateTimeStr: String ->
            try { dateTimeParser.parse(dateTimeStr)?.time ?: 0L } catch(e: Exception) { 0L }
        }

        when (currentSortOption) {
            "Return Date (Soonest)" -> list.sortedBy { parseDateTimeSafe(it.returnDate) }
            "Return Date (Latest)" -> list.sortedByDescending { parseDateTimeSafe(it.returnDate) }
            "Delivery Date (Soonest)" -> list.sortedBy { parseDateTimeSafe(it.deliveryDate) }
            "Delivery Date (Latest)" -> list.sortedByDescending { parseDateTimeSafe(it.deliveryDate) }
            "Invoice Date (Newest)" -> list.sortedByDescending { parseDateSafe(it.date) }
            "Invoice Date (Oldest)" -> list.sortedBy { parseDateSafe(it.date) }
            else -> list
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main list screen (blurs when modal is visible)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .blur(if (selectedRental != null) 12.dp else 0.dp)
                .padding(16.dp)
        ) {
            // Header with three dots menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Rented Out Jewelry",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A0E17)
                    )
                    Text(
                        text = "${filteredRentals.size} items found (${rentals.size} total)",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (!isSyncing) {
                                isSyncing = true
                                coroutineScope.launch {
                                    val url = repository.getGoogleSheetUrl()
                                    val localList = repository.getAllRentalsForSync()
                                    val synced = GoogleSheetsSync.syncWithCloud(url, localList)
                                    if (synced != null) {
                                        repository.mergeSyncedRentals(synced)
                                        android.widget.Toast.makeText(context, "Database synced successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "Failed to sync database. Check internet/URL settings.", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                    isSyncing = false
                                }
                            }
                        },
                        enabled = !isSyncing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync Cloud",
                            tint = Color(0xFF4A0E17)
                        )
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = Color(0xFF4A0E17)
                            )
                        }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = if (isDeleteMode) "Exit Delete Mode" else "Delete Rental",
                                    color = if (isDeleteMode) Color.Black else Color.Red
                                ) 
                            },
                            onClick = {
                                isDeleteMode = !isDeleteMode
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

            // Search text field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, contact or jewellery...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search", tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4A0E17),
                    focusedLabelColor = Color(0xFF4A0E17),
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter & Sort Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Scrollable filter row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(listOf("All", "Pending", "On Rent", "Received")) { filterOpt ->
                        val isSelected = selectedFilter == filterOpt
                        val chipBg = if (isSelected) Color(0xFF4A0E17) else Color.LightGray.copy(alpha = 0.15f)
                        val chipText = if (isSelected) Color.White else Color.DarkGray
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(chipBg)
                                .clickable { selectedFilter = filterOpt }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = filterOpt, color = chipText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Sort Dropdown
                Box {
                    OutlinedButton(
                        onClick = { showSortMenu = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4A0E17))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sort", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        val sortOptions = listOf(
                            "Invoice Date (Newest)",
                            "Invoice Date (Oldest)",
                            "Return Date (Soonest)",
                            "Return Date (Latest)",
                            "Delivery Date (Soonest)",
                            "Delivery Date (Latest)"
                        )
                        sortOptions.forEach { sortOpt ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = sortOpt, 
                                        fontWeight = if (currentSortOption == sortOpt) FontWeight.Bold else FontWeight.Normal,
                                        color = if (currentSortOption == sortOpt) Color(0xFF4A0E17) else Color.Black
                                    ) 
                                },
                                onClick = {
                                    currentSortOption = sortOpt
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (rentals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No jewelry rented out currently",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            } else {
                if (filteredRentals.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No rentals match your search criteria",
                            fontSize = 15.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredRentals, key = { it.id }) { rental ->
                            RentalCard(
                                rental = rental,
                                isDeleteMode = isDeleteMode,
                                onClick = { selectedRental = rental },
                                onDeleteClick = { rentalToDelete = rental }
                            )
                        }
                    }
                }
            }
        }

        // View Detail Bottom Sheet
        if (selectedRental != null) {
            val rental = selectedRental!!
            val status = getRentalStatus(rental)
            val isEnabled = status == "On Rent" || status == "Received"

            ModalBottomSheet(
                onDismissRequest = { selectedRental = null },
                sheetState = sheetState,
                containerColor = Color.White,
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.LightGray) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    // Title Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = rental.name,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4A0E17)
                            )
                            Text(
                                text = rental.contact,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                        StatusBadge(status = status)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.5f))

                    // Shortcuts Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SheetActionButton(
                            icon = Icons.Default.Share,
                            label = "Share Receipt",
                            onClick = { shareToWhatsApp(context, rental) }
                        )
                        Spacer(modifier = Modifier.width(24.dp))
                        SheetActionButton(
                            icon = if (rental.isReceived) Icons.Default.Refresh else Icons.Default.CheckCircle,
                            label = if (rental.isReceived) "Mark Active" else "Mark Received",
                            enabled = isEnabled,
                            onClick = {
                                val newStatus = !rental.isReceived
                                repository.updateReceivedStatus(rental.id, newStatus)
                                selectedRental = rental.copy(
                                    isReceived = newStatus,
                                    balance = if (newStatus) 0.0 else rental.balance
                                )
                            }
                        )
                        Spacer(modifier = Modifier.width(24.dp))
                        SheetActionButton(
                            icon = Icons.Default.Delete,
                            label = "Delete Log",
                            tintColor = Color.Red,
                            onClick = {
                                selectedRental = null
                                rentalToDelete = rental
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.5f))

                    // Scrollable details content
                    DetailRow("Invoice Date", rental.date)
                    DetailRow("Jewellery Price", "Rs. ${rental.jewelryNo}")
                    DetailRow("Jewellery Details", rental.jewelryDetails)
                    DetailRow("Delivery Date & Time", rental.deliveryDate)
                    DetailRow("Expected Return Date", rental.returnDate)
                    DetailRow("Rent", "Rs. ${rental.rent}")
                    DetailRow("Advance", "Rs. ${rental.advance}")
                    DetailRow("Balance", "Rs. ${rental.balance}", isBold = true)
                    DetailRow("Refund Deposit", "Rs. ${rental.refundAmount}")

                    Spacer(modifier = Modifier.height(16.dp))

                    // Image Gallery inside sheet
                    if (rental.images.isNotEmpty()) {
                        Text(
                            text = "Photos",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF4A0E17)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                        ) {
                            items(rental.images) { path ->
                                val file = File(path)
                                AsyncImage(
                                    model = file,
                                    contentDescription = "Jewelry photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Delete Confirmation Dialog
        if (rentalToDelete != null) {
            AlertDialog(
                onDismissRequest = { rentalToDelete = null },
                title = { Text("Delete Rental Record?") },
                text = { Text("Are you sure you want to delete the rental record for ${rentalToDelete?.name}? This action cannot be undone and will delete all photos.") },
                confirmButton = {
                    Button(
                        onClick = {
                            rentalToDelete?.let {
                                repository.deleteRental(it.id)
                                Toast.makeText(context, "Deleted rental for ${it.name}", Toast.LENGTH_SHORT).show()
                            }
                            rentalToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { rentalToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (isSyncing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF4A0E17))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Syncing with cloud database...",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4A0E17),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RentalCard(
    rental: Rental,
    isDeleteMode: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // First image thumbnail
            if (rental.images.isNotEmpty()) {
                val file = File(rental.images[0])
                AsyncImage(
                    model = file,
                    contentDescription = "Jewelry thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(Color.LightGray.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "No photo",
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rental.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF4A0E17),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(status = getRentalStatus(rental))
                }
                Text(
                    text = rental.jewelryDetails,
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Del: ${rental.deliveryDate.substringBefore(' ')}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Bal: Rs. ${rental.balance}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (rental.balance > 0) Color(0xFFE53935) else Color(0xFF43A047)
                    )
                }
            }

            // Optional delete icon
            if (isDeleteMode) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, isBold: Boolean = false) {
    if (value.isBlank()) return
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = Color.Gray
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                color = if (isBold) Color(0xFF4A0E17) else Color.Black
            )
        }
        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(top = 4.dp))
    }
}

fun getRentalStatus(rental: Rental): String {
    if (rental.isReceived) return "Received"
    return try {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a", java.util.Locale.getDefault())
        val deliveryDate = sdf.parse(rental.deliveryDate)
        if (deliveryDate != null && java.util.Date().after(deliveryDate)) {
            "On Rent"
        } else {
            "Pending"
        }
    } catch (e: Exception) {
        "On Rent"
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        "Received" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "On Rent" -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        else -> Color(0xFFFFF3E0) to Color(0xFFE65100)
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun SheetActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tintColor: Color = Color(0xFF4A0E17)
) {
    val contentAlpha = if (enabled) 1f else 0.38f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(enabled = enabled) { onClick() }
            .padding(8.dp)
            .width(80.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tintColor.copy(alpha = contentAlpha),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray.copy(alpha = contentAlpha),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
