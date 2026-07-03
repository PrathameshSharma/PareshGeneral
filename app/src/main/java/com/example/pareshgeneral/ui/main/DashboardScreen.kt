package com.example.pareshgeneral.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pareshgeneral.R
import com.example.pareshgeneral.data.RentalRepository
import com.example.pareshgeneral.theme.PareshGeneralTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { RentalRepository(context) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    DashboardScreenContent(
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        onLogout = onLogout,
        onSettingsClick = { showSettingsDialog = true }
    ) {
        when (selectedTab) {
            0 -> RentItScreen(repository = repository)
            1 -> RentedOutScreen(repository = repository)
            2 -> AnalyticsScreen(repository = repository)
        }
    }

    if (showSettingsDialog) {
        var sheetUrl by remember { mutableStateOf(repository.getGoogleSheetUrl()) }

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Sync Configuration") },
            text = {
                Column {
                    Text("Enter your Google Sheets Web App URL for real-time Excel sync:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.padding(8.dp))
                    OutlinedTextField(
                        value = sheetUrl,
                        onValueChange = { sheetUrl = it },
                        label = { Text("Web App URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.saveGoogleSheetUrl(sheetUrl.trim())
                        showSettingsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A0E17))
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Cancel", color = Color(0xFF4A0E17))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreenContent(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onLogout: () -> Unit,
    onSettingsClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White
            ) {
                // Drawer Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF4A0E17))
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                ) {
                    Column {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "Shop Logo",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .padding(4.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Paresh General",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Bridal Jewellery On Rent",
                            color = Color(0xFFD4AF37),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Navigation Items
                NavigationDrawerItem(
                    label = { Text("RentIt Form", fontWeight = FontWeight.SemiBold) },
                    selected = selectedTab == 0,
                    onClick = {
                        onTabSelected(0)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Refresh, contentDescription = "RentIt") },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0xFF4A0E17).copy(alpha = 0.1f),
                        selectedIconColor = Color(0xFF4A0E17),
                        selectedTextColor = Color(0xFF4A0E17),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.DarkGray
                    ),
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(modifier = Modifier.height(4.dp))

                NavigationDrawerItem(
                    label = { Text("RentedOut Log", fontWeight = FontWeight.SemiBold) },
                    selected = selectedTab == 1,
                    onClick = {
                        onTabSelected(1)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.List, contentDescription = "RentedOut") },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0xFF4A0E17).copy(alpha = 0.1f),
                        selectedIconColor = Color(0xFF4A0E17),
                        selectedTextColor = Color(0xFF4A0E17),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.DarkGray
                    ),
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(modifier = Modifier.height(4.dp))

                NavigationDrawerItem(
                    label = { Text("Business Analytics", fontWeight = FontWeight.SemiBold) },
                    selected = selectedTab == 2,
                    onClick = {
                        onTabSelected(2)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Analytics") },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0xFF4A0E17).copy(alpha = 0.1f),
                        selectedIconColor = Color(0xFF4A0E17),
                        selectedTextColor = Color(0xFF4A0E17),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.DarkGray
                    ),
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Navigation Menu", tint = Color.White)
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.app_logo),
                                contentDescription = "Shop Logo",
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                            )
                            Text("Paresh General", color = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF4A0E17)
                    ),
                    actions = {
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "Google Sheet Settings", tint = Color.White)
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.White)
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Screen content area
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    PareshGeneralTheme {
        DashboardScreenContent(
            selectedTab = 0,
            onTabSelected = {},
            onLogout = {},
            onSettingsClick = {}
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Dashboard Content Preview")
            }
        }
    }
}
