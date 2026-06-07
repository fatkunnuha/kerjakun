package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.CartItem
import com.example.data.Product
import com.example.data.ShopProfile
import com.example.data.Transaction
import com.example.data.TransactionItem
import com.example.data.User
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                POSApp()
            }
        }
    }
}

@Composable
fun POSApp(viewModel: POSViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentScreen != Screen.LOGIN) {
                POSBottomNavBar(
                    currentScreen = currentScreen,
                    currentUser = currentUser,
                    onNavigate = { screen ->
                        // Role-Based Access Control logic
                        if (currentUser?.role != "Admin" && (screen == Screen.LAPORAN || screen == Screen.SETTINGS)) {
                            Toast.makeText(context, "Akses Terbatas! Hanya untuk Admin / Owner", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.navigateTo(screen)
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.LOGIN -> LoginScreen(viewModel)
                Screen.DASHBOARD -> DashboardScreen(viewModel)
                Screen.KASIR -> KasirScreen(viewModel)
                Screen.LAPORAN -> LaporanScreen(viewModel)
                Screen.PRODUK -> ProdukScreen(viewModel)
                Screen.SETTINGS -> SettingsScreen(viewModel)
            }
        }
    }
}

/**
 * Modern Material 3 Bottom Navigation Bar
 */
@Composable
fun POSBottomNavBar(
    currentScreen: Screen,
    currentUser: User?,
    onNavigate: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("main_bottom_navigation")
    ) {
        val items = listOf(
            NavigationItem("Dashboard", Screen.DASHBOARD, Icons.Default.Dashboard, Icons.Outlined.Dashboard),
            NavigationItem("Kasir", Screen.KASIR, Icons.Default.LocalMall, Icons.Outlined.LocalMall),
            NavigationItem("Laporan", Screen.LAPORAN, Icons.Default.Analytics, Icons.Outlined.Analytics),
            NavigationItem("Inventaris", Screen.PRODUK, Icons.Default.Inventory2, Icons.Outlined.Inventory2),
            NavigationItem("Toko", Screen.SETTINGS, Icons.Default.Storefront, Icons.Outlined.Storefront)
        )

        items.forEach { item ->
            val isSelected = currentScreen == item.screen
            val isRestricted = currentUser?.role != "Admin" && (item.screen == Screen.LAPORAN || item.screen == Screen.SETTINGS)

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.screen) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title,
                        tint = if (isRestricted) Color.LightGray else if (isSelected) Color(0xFF0B4C8C) else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp,
                        color = if (isRestricted) Color.LightGray else if (isSelected) Color(0xFF0B4C8C) else Color.Gray
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFFEDF2F7)
                ),
                modifier = Modifier.testTag("nav_tab_${item.title.lowercase()}")
            )
        }
    }
}

data class NavigationItem(
    val title: String,
    val screen: Screen,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * 1. Login Screen layout with custom PIN keypad or staff list shortcuts
 */
@Composable
fun LoginScreen(viewModel: POSViewModel) {
    val context = LocalContext.current
    var usernameInput by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }
    var selectedRoleTab by remember { mutableStateOf("Budi Hartono (Owner)") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        
        // Brand Icon Logo Illustration mockup
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color(0xFF0B4C8C), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Storefront,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "QuickPOS Cashier Hub",
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            color = Color(0xFF0F172A),
            textAlign = TextAlign.Center
        )
        Text(
            text = "Sistem Kasir Pintar & Manajemen Inventaris Barang",
            color = Color.Gray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Quick login staff shortcut card (Makes it highly robust & accessible in a tap)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Pilih Profil Akun Staf:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Admin button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            usernameInput = "admin"
                            selectedRoleTab = "Budi Hartono (Owner)"
                        }
                        .background(
                            if (usernameInput == "admin") Color(0xFFF1F5F9) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFFFECE0), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎯", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Budi Hartono", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Role: Admin / Owner", fontSize = 11.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (usernameInput == "admin") {
                        Icon(Icons.Default.CheckCircle, "Selected", tint = Color(0xFF0B4C8C))
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))

                // Cashier button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            usernameInput = "kasir"
                            selectedRoleTab = "Siti Rahma (Staff)"
                        }
                        .background(
                            if (usernameInput == "kasir") Color(0xFFF1F5F9) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFE2F9EC), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💼", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Siti Rahma", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Role: Kasir Toko", fontSize = 11.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (usernameInput == "kasir") {
                        Icon(Icons.Default.CheckCircle, "Selected", tint = Color(0xFF0B4C8C))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Username Field (manual entry option)
        OutlinedTextField(
            value = usernameInput,
            onValueChange = { usernameInput = it.lowercase() },
            label = { Text("ID Staf / Username") },
            leadingIcon = { Icon(Icons.Default.Person, "ID") },
            shape = RoundedCornerShape(10.dp),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("username_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // PIN Security Field
        OutlinedTextField(
            value = pinInput,
            onValueChange = { pinInput = it },
            label = { Text("PIN Keamanan (Default: 1234)") },
            leadingIcon = { Icon(Icons.Default.Lock, "Lock") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            shape = RoundedCornerShape(10.dp),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("password_input")
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (usernameInput.isBlank()) {
                    Toast.makeText(context, "Silakan pilih staf atau isi username terlebih dahulu", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.login(usernameInput, pinInput, context) {}
                }
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B4C8C)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("login_button")
        ) {
            Text("Masuk ke Kasir", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

/**
 * 2. Dashboard Screen with Weekly bar metrics and low stock alerts
 */
@Composable
fun DashboardScreen(viewModel: POSViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val shopProfile by viewModel.shopProfile.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val lowStockList by viewModel.lowStockProducts.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Applet Header Panel with Profile Image representation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Mock avatar from profileImage layout
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFFE2E8F0), CircleShape)
                        .border(1.5.dp, Color(0xFF0B4C8C), CircleShape)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = if (currentUser?.role == "Admin") "🎯" else "💼", fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "QuickPOS",
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0B4C8C),
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Staf: ${currentUser?.name ?: "Kasir Utama"}",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Row of top right actions including notifications and logout
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Simple Notificationbell representing dynamic changes in stock triggers
                IconButton(
                    onClick = {
                        if (lowStockList.isNotEmpty()) {
                            Toast.makeText(context, "Peringatan! Ada ${lowStockList.size} barang menipis!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Belum ada peringatan stok baru", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Box {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            tint = Color(0xFF0B4C8C),
                            contentDescription = "Peringatan",
                            modifier = Modifier.size(26.dp)
                        )
                        if (lowStockList.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color.Red, CircleShape)
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }
                }

                // Logout button for all users (Admin & Cashier, but particularly vital for Cashier who has no settings tab)
                IconButton(
                    onClick = {
                        viewModel.logout()
                        Toast.makeText(context, "Sesi keluar berhasil", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("dashboard_logout_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        tint = Color(0xFFDC2626),
                        contentDescription = "Logout",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Custom Greetings & Shop details
        Text(
            text = "Selamat Datang!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF0F172A)
        )
        Text(
            text = "Berikut adalah ringkasan penjualan toko Anda hari ini.",
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Real-time Low Stock Warning block (Red header alert)
        if (lowStockList.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0)),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFEF4444), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            tint = Color.White,
                            contentDescription = "Stok Menipis",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Peringatan Stok Menipis!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF991B1B)
                        )
                        Text(
                            text = "Ada ${lowStockList.size} item produk dengan stok kritis di bawah ambang batas.",
                            fontSize = 11.sp,
                            color = Color(0xFFB91C1C)
                        )
                    }

                    Button(
                        onClick = { viewModel.navigateTo(Screen.PRODUK) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Kelola", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Metrics Card row values
        val revenueToday = viewModel.getSalesSumForPeriod("Harian")
        val countToday = viewModel.getTransactionsCountForPeriod("Harian")
        val formattedRevenueToday = String.format(Locale("id", "ID"), "Rp %,.0f", revenueToday)

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TOTAL PENJUALAN HARI INI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formattedRevenueToday,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0B4C8C)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, "Up", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+12.5% dari kemarin",
                            fontSize = 11.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color(0xFFEFF6FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        tint = Color(0xFF0B4C8C),
                        contentDescription = "Income",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("TRANSAKSI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("$countToday", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Transaksi Selesai", fontSize = 11.sp, color = Color.Gray)
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("PRODUK TERJUAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("32", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Unit Item", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        // Action Core CTA button "Mulai Transaksi"
        Button(
            onClick = { viewModel.navigateTo(Screen.KASIR) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(bottom = 16.dp)
                .testTag("start_transaction_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B4C8C)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.ShoppingCart, "Input")
                Text("Mulai Transaksi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        // Kinerja Mingguan Title & Chart
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kinerja Mingguan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A)
                    )
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opsi",
                        tint = Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                WeeklyUsageChart()
            }
        }

        // Transaction Logs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transaksi Terakhir",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "Lihat Semua",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B4C8C),
                modifier = Modifier.clickable {
                    if (currentUser?.role == "Admin") {
                        viewModel.navigateTo(Screen.LAPORAN)
                    } else {
                        Toast.makeText(context, "Akses Laporan Terbatas untuk Admin", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Render standard scroll list of logs
        val latestTrxs = transactions.take(4)
        if (latestTrxs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Belum ada transaksi tersimpan.", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            latestTrxs.forEach { trx ->
                val priceVal = String.format(Locale("id", "ID"), "Rp %,.0f", trx.totalAmount)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (trx.paymentMethod == "QRIS") Color(0xFFEDFDED) else Color(0xFFEFF6FF),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (trx.paymentMethod == "QRIS") Icons.Default.QrCodeScanner else Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = if (trx.paymentMethod == "QRIS") Color(0xFF10B981) else Color(0xFF0B4C8C),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "#${trx.invoiceNo}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "${trx.getFormattedDate("HH:mm")} • ${trx.paymentMethod}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Text(
                            text = priceVal,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (trx.status == "FAILED") Color.Gray else Color(0xFF0F172A)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3. Cashier selling screen (Grid products + Cart bottom sheets + QRIS Payments simulation with counter)
 */
@Composable
fun KasirScreen(viewModel: POSViewModel) {
    val search by viewModel.searchQuery.collectAsState()
    val category by viewModel.categoryFilter.collectAsState()
    val productsList by viewModel.filteredProducts.collectAsState()
    val cartItems by viewModel.cart.collectAsState()
    val shopInfo by viewModel.shopProfile.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    
    var showScanPopup by remember { mutableStateOf(false) }
    var showPaymentSheet by remember { mutableStateOf(false) }
    var showCartDialogDetail by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Checkout receipt popup details variables
    val receiptShown by viewModel.showCheckoutReceipt.collectAsState()
    val invoiceDetails by viewModel.receiptInvoice.collectAsState()
    val invoiceItems by viewModel.receiptItems.collectAsState()

    // 1. Scanner Camera Shutter overlay trigger
    if (showScanPopup) {
        MockBarcodeScannerDialog(
            onDismissRequest = { showScanPopup = false },
            onBarcodeDetected = { code ->
                viewModel.scanBarcode(code, context)
                showScanPopup = false
            }
        )
    }

    // 2. Receipt Dialog popup view
    if (receiptShown && invoiceDetails != null) {
        PrintedReceiptDialog(
            invoice = invoiceDetails!!,
            items = invoiceItems,
            profile = shopInfo,
            onDismiss = { viewModel.showCheckoutReceipt.value = false }
        )
    }

    // 2b. Export Success Dialog view
    val showExportDialog by viewModel.showExportDialog.collectAsState()
    val lastPdfFile by viewModel.lastExportedPdf.collectAsState()
    val lastExcelFile by viewModel.lastExportedExcel.collectAsState()

    if (showExportDialog) {
        ExportSuccessDialog(
            pdfFile = lastPdfFile,
            excelFile = lastExcelFile,
            onDismiss = { viewModel.showExportDialog.value = false }
        )
    }

    // 3. Payment Processing Action Dialog (QRIS / Cash change calculation)
    if (showPaymentSheet) {
        var inputCashString by remember { mutableStateOf("") }
        val grandTotalStr = String.format(Locale("id", "ID"), "Rp %,.0f", viewModel.getCartTotal(shopInfo))
        val paymentMethodSelected by viewModel.checkoutPaymentMethod.collectAsState()
        
        // Timer for QRIS dynamic payment countdown
        var qrisCountdownTimerVal by remember { mutableIntStateOf(120) }
        LaunchedEffect(showPaymentSheet, paymentMethodSelected) {
            if (paymentMethodSelected == "QRIS") {
                qrisCountdownTimerVal = 120
                while (qrisCountdownTimerVal > 0) {
                    delay(1000)
                    qrisCountdownTimerVal--
                }
            }
        }

        Dialog(onDismissRequest = { showPaymentSheet = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Selesaikan Pembayaran", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                        IconButton(onClick = { showPaymentSheet = false }) {
                            Icon(Icons.Default.Close, "Kembali", tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Total Tagihan:",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = grandTotalStr,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mode selections Row options
                    val methodsList = listOf(
                        PaymentMethodItem("Tunai", Icons.Default.Money, "Tunai"),
                        PaymentMethodItem("QRIS", Icons.Default.QrCode, "QRIS"),
                        PaymentMethodItem("Kartu", Icons.Default.CreditCard, "Kartu/Debit")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        methodsList.forEach { m ->
                            val isSelected = m.id == paymentMethodSelected
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        2.dp,
                                        if (isSelected) Color(0xFF0B4C8C) else Color(0xFFCBD5E1),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .background(
                                        if (isSelected) Color(0xFFEFF6FF) else Color.Transparent,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { viewModel.checkoutPaymentMethod.value = m.id }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = m.icon,
                                        contentDescription = m.label,
                                        tint = if (isSelected) Color(0xFF0B4C8C) else Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = m.label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF0B4C8C) else Color.DarkGray
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Payment modes details switcher
                    if (paymentMethodSelected == "Tunai") {
                        OutlinedTextField(
                            value = inputCashString,
                            onValueChange = { inputCashString = it },
                            label = { Text("Jumlah Uang Tunai Diterima") },
                            placeholder = { Text("Mulai mengetik nominal...") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cash_input_field")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Render suggestion pills
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(50000.0, 100000.0, 200000.0).forEach { cash ->
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                        .clickable { inputCashString = String.format(Locale.US, "%.0f", cash) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = String.format(Locale("id", "ID"), "Rp %,.0f", cash),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }

                        // Kembalian calculation
                        val inputNum = inputCashString.toDoubleOrNull() ?: 0.0
                        val changeLeft = inputNum - viewModel.getCartTotal(shopInfo)
                        if (changeLeft >=  0.0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFE2F9EC), RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Uang Kembalian:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF166534))
                                Text(
                                    text = String.format(Locale("id", "ID"), "Rp %,.0f", changeLeft),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = Color(0xFF166534)
                                )
                            }
                        }
                    } else if (paymentMethodSelected == "QRIS") {
                        // QRIS mockup with dynamic timer and merchant properties
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            MockQrisDraw(amount = viewModel.getCartTotal(shopInfo))
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val mm = qrisCountdownTimerVal / 60
                            val ss = qrisCountdownTimerVal % 60
                            Text(
                                text = "Kode QRIS Dinamis • Exp: ${String.format("%02d:%02d", mm, ss)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Text(
                                text = "Gateway Pihak Ketiga: QRIS MID-${shopInfo.qrisPaymentGatewayId}",
                                fontSize = 9.sp,
                                color = Color.LightGray
                            )
                        }
                    } else {
                        // Card debit message
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Geser / Masukkan Kartu Debit & Kredit pada Mesin EDC Bank Anda.",
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (paymentMethodSelected == "Tunai") {
                                val inVal = inputCashString.toDoubleOrNull() ?: 0.0
                                if (inVal < viewModel.getCartTotal(shopInfo)) {
                                    Toast.makeText(context, "Jumlah uang yang diberikan kurang!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                            }
                            viewModel.executeCheckout(context)
                            showPaymentSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("pay_confirm_button")
                    ) {
                        Text("Konfirmasi Pembayaran Selesai", fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                }
            }
        }
    }

    // 4. Cart List Details sheet drawer
    if (showCartDialogDetail) {
        Dialog(onDismissRequest = { showCartDialogDetail = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxHeight(0.8f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ringkasan Belanja", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                        IconButton(onClick = { showCartDialogDetail = false }) {
                            Icon(Icons.Default.Close, "Tutup", tint = Color.Gray)
                        }
                    }

                    Divider()

                    // Cart item layout lists
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 10.dp)
                    ) {
                        items(cartItems) { item ->
                            val hasDiscount = item.product.discountPercent > 0.0
                            val effectivePrice = if (hasDiscount) item.product.price * (1.0 - item.product.discountPercent / 100.0) else item.product.price
                            val unitTotal = String.format(Locale("id", "ID"), "Rp %,.0f", effectivePrice * item.quantity)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            String.format(Locale("id", "ID"), "%d x Rp %,.0f", item.quantity, effectivePrice),
                                            fontSize = 11.sp,
                                            color = Color.Black
                                        )
                                        if (hasDiscount) {
                                            Text(
                                                String.format(Locale("id", "ID"), "(Asli: Rp %,.0f)", item.product.price),
                                                style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.decreaseQuantity(item.product) },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                                    ) {
                                        Icon(Icons.Default.Remove, "Kurang", tint = Color.DarkGray, modifier = Modifier.size(16.dp))
                                    }
                                    
                                    Text("${item.quantity}", fontWeight = FontWeight.Bold, color = Color.Black)

                                    IconButton(
                                        onClick = { viewModel.addToCart(item.product) },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                                    ) {
                                        Icon(Icons.Default.Add, "Tambah", tint = Color.DarkGray, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            Divider(color = Color(0xFFF1F5F9))
                        }
                    }

                    // Taxes logic display
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Subtotal:", fontSize = 11.sp, color = Color.Gray)
                                Text(String.format(Locale("id", "ID"), "Rp %,.0f", viewModel.getCartSubtotal()), fontSize = 11.sp, color = Color.Black)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Pajak (${shopInfo.taxRate}%):", fontSize = 11.sp, color = Color.Gray)
                                Text(String.format(Locale("id", "ID"), "Rp %,.0f", viewModel.getCartTaxAmount(shopInfo)), fontSize = 11.sp, color = Color.Black)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Biaya Jasa:", fontSize = 11.sp, color = Color.Gray)
                                Text(String.format(Locale("id", "ID"), "Rp %,.0f", shopInfo.serviceCharge), fontSize = 11.sp, color = Color.Black)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            showCartDialogDetail = false
                            showPaymentSheet = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B4C8C)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Lanjutkan ke Pembayaran", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // MAIN SELLING WRAPPER LAYOUT
    Column(modifier = Modifier.fillMaxSize()) {
        // Applet Header with search query + scanner launcher triggers
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B4C8C))
                .padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kasir Penjualan",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { showScanPopup = true },
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                .testTag("launch_scanner_trigger")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                tint = Color.White,
                                contentDescription = "Scan"
                            )
                        }

                        IconButton(
                            onClick = {
                                viewModel.logout()
                                Toast.makeText(context, "Sesi keluar berhasil", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                .testTag("kasir_logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                tint = Color.White,
                                contentDescription = "Logout"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Custom Search Textbox
                OutlinedTextField(
                    value = search,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Cari menu, merk atau barcode...", color = Color.LightGray) },
                    leadingIcon = { Icon(Icons.Default.Search, "Cari", tint = Color.LightGray) },
                    trailingIcon = {
                        if (search.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, "Clear", tint = Color.White)
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.White,
                        unfocusedContainerColor = Color.White.copy(alpha = 0.15f),
                        focusedContainerColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("search_inventory_box")
                )
            }
        }

        // Category tabs and Layout Switcher controllers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.weight(1f)) {
                CategoryTabsRow(
                    selectedCategory = category,
                    onCategorySelected = { viewModel.categoryFilter.value = it }
                )
            }
            
            // Grid / List Layout controller
            Row(
                modifier = Modifier
                    .background(Color(0xFFEDF2F7), RoundedCornerShape(8.dp))
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = { viewModel.setGridViewEnabled(true) },
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = if (isGridView) Color.White else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Tampilan Kotak (Grid)",
                        tint = if (isGridView) Color(0xFF0B4C8C) else Color(0xFF718096),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.setGridViewEnabled(false) },
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = if (!isGridView) Color.White else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Tampilan Baris (List)",
                        tint = if (!isGridView) Color(0xFF0B4C8C) else Color(0xFF718096),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Products Catalog selection area
        if (productsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, "Empty", tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Produk tidak ditemukan.", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(productsList) { p ->
                        ProductGridCard(
                            product = p,
                            onAddToCart = { viewModel.addToCart(p) },
                            modifier = Modifier.testTag("product_grid_${p.name.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(productsList) { p ->
                        ProductListRow(
                            product = p,
                            onAddToCart = { viewModel.addToCart(p) },
                            modifier = Modifier.testTag("product_list_${p.name.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            }
        }

        // Floating Bottom shopping summary
        if (cartItems.isNotEmpty()) {
            BottomCartBar(
                cart = cartItems,
                onPayClicked = { showCartDialogDetail = true }
            )
        }
    }
}

data class PaymentMethodItem(
    val id: String,
    val icon: ImageVector,
    val label: String
)

/**
 * 4. Laporan / Reports Screen with intervals tabs and sales charts
 */
@Composable
fun LaporanScreen(viewModel: POSViewModel) {
    val reportPeriodSelected by viewModel.reportType.collectAsState()
    val allTransactionsList by viewModel.allTransactions.collectAsState()
    val allTransactionItemsList by viewModel.allTransactionItems.collectAsState()
    val context = LocalContext.current

    val finalPeriodListFiltered = remember(allTransactionsList, reportPeriodSelected) {
        viewModel.getFilteredTransactionsForPeriod(reportPeriodSelected)
    }

    val totalTokoRevenue = remember(finalPeriodListFiltered) {
        finalPeriodListFiltered.filter { it.status == "PAID" }.sumOf { it.totalAmount }
    }
    
    val transactionsCount = remember(finalPeriodListFiltered) {
        finalPeriodListFiltered.size
    }

    // Comprehensive financial components
    val totalTaxCollected = remember(finalPeriodListFiltered) {
        finalPeriodListFiltered.filter { it.status == "PAID" }.sumOf { it.taxProcessed }
    }

    val totalServiceCharge = remember(finalPeriodListFiltered) {
        finalPeriodListFiltered.filter { it.status == "PAID" }.sumOf { it.serviceChargeProcessed }
    }

    val totalNetRevenue = remember(totalTokoRevenue, totalTaxCollected, totalServiceCharge) {
        totalTokoRevenue - totalTaxCollected - totalServiceCharge
    }

    // Payment methods calculations
    val paymentBreakdown = remember(finalPeriodListFiltered) {
        val paidTrxs = finalPeriodListFiltered.filter { it.status == "PAID" }
        val tunaiSum = paidTrxs.filter { it.paymentMethod == "Tunai" }.sumOf { it.totalAmount }
        val qrisSum = paidTrxs.filter { it.paymentMethod == "QRIS" }.sumOf { it.totalAmount }
        val kartuSum = paidTrxs.filter { it.paymentMethod == "Kartu" }.sumOf { it.totalAmount }
        
        val tunaiCount = paidTrxs.count { it.paymentMethod == "Tunai" }
        val qrisCount = paidTrxs.count { it.paymentMethod == "QRIS" }
        val kartuCount = paidTrxs.count { it.paymentMethod == "Kartu" }
        
        Triple(
            Pair(tunaiSum, tunaiCount),
            Pair(qrisSum, qrisCount),
            Pair(kartuSum, kartuCount)
        )
    }

    // Dynamic Top Selling Menus
    val dynamicBestSellers = remember(allTransactionItemsList, finalPeriodListFiltered) {
        val paidTrxsIds = finalPeriodListFiltered.filter { it.status == "PAID" }.map { it.id }.toSet()
        val filteredLineItems = allTransactionItemsList.filter { it.transactionId in paidTrxsIds }
        
        filteredLineItems.groupBy { it.productName }
            .map { (name, list) ->
                val qty = list.sumOf { it.quantity }
                val sales = list.sumOf { it.subtotal }
                Triple(name, qty, sales)
            }
            .sortedByDescending { it.second }
    }

    // Dialog view state for Past Transaction Struk
    var selectedTrxForDetails by remember { mutableStateOf<Transaction?>(null) }
    var selectedTrxItems by remember { mutableStateOf<List<TransactionItem>>(emptyList()) }

    LaunchedEffect(selectedTrxForDetails) {
        selectedTrxForDetails?.let { trx ->
            selectedTrxItems = viewModel.repository.getItemsForTransaction(trx.id)
        }
    }

    val shopProfileState by viewModel.shopProfile.collectAsState()

    if (selectedTrxForDetails != null) {
        PrintedReceiptDialog(
            invoice = selectedTrxForDetails!!,
            items = selectedTrxItems,
            profile = shopProfileState,
            onDismiss = {
                selectedTrxForDetails = null
                selectedTrxItems = emptyList()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Laporan Analitik",
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            color = Color(0xFF0F172A)
        )
        Text(
            text = "Data statistik ringkasan dan performa jajaran penjualan toko.",
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Interval range options tabs (Harian, Bulanan, Tahunan)
        val periodOptions = listOf("Harian", "Bulanan", "Tahunan")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEDF2F7), RoundedCornerShape(10.dp))
                .padding(4.dp)
        ) {
            periodOptions.forEach { p ->
                val isSelected = p == reportPeriodSelected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) Color.White else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.reportType.value = p }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = p,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp,
                        color = if (isSelected) Color(0xFF0B4C8C) else Color.DarkGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Total earnings and telemetry stats card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL OMSET (${reportPeriodSelected.uppercase()})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale("id", "ID"), "Rp %,.0f", totalTokoRevenue),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0B4C8C)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFEDFDED), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MonetizationOn,
                            "Dompet",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Jumlah Transaksi", fontSize = 11.sp, color = Color.Gray)
                        Text("$transactionsCount", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Rata-Rata Transaksi", fontSize = 11.sp, color = Color.Gray)
                        val avg = if (transactionsCount > 0) totalTokoRevenue / transactionsCount else 0.0
                        Text(String.format(Locale("id", "ID"), "Rp %,.0f", avg), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Financial Details breakdown components (Omni-channel breakdown)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "A. Rincian Finansial Toko",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Komponen Pendapatan", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pendapatan Bersih", fontSize = 13.sp, color = Color.DarkGray)
                            Text(String.format(Locale("id", "ID"), "Rp %,.0f", totalNetRevenue), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pajak Terjual (PPN)", fontSize = 13.sp, color = Color.DarkGray)
                            Text(String.format(Locale("id", "ID"), "Rp %,.0f", totalTaxCollected), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Biaya Layanan Toko", fontSize = 13.sp, color = Color.DarkGray)
                            Text(String.format(Locale("id", "ID"), "Rp %,.0f", totalServiceCharge), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Card Cetak Laporan (Harian, Bulanan, Tahunan) - Replaces export/sharing
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFEFF6FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Cetak",
                            tint = Color(0xFF0B4C8C),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Cetak Laporan Fisik",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.Black
                        )
                        Text(
                            text = "Mencetak rincian penjualan & riwayat finansial langsung.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Daily Report Print Button
                Button(
                    onClick = {
                        val transactions = viewModel.getFilteredTransactionsForPeriod("Harian")
                        val file = com.example.util.DocumentExporter.exportReportToPdf(
                            context = context,
                            profile = shopProfileState,
                            transactions = transactions,
                            reportTitle = "Laporan Penjualan Harian"
                        )
                        if (file != null) {
                            Toast.makeText(context, "Mempersiapkan Cetak Laporan Harian...", Toast.LENGTH_SHORT).show()
                            com.example.util.DocumentExporter.printPdfFile(context, file, "Laporan_Harian")
                        } else {
                            Toast.makeText(context, "Gagal membuat Laporan Harian", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(Icons.Default.Print, "Cetak", modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cetak Laporan Harian", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Monthly Report Print Button
                Button(
                    onClick = {
                        val transactions = viewModel.getFilteredTransactionsForPeriod("Bulanan")
                        val file = com.example.util.DocumentExporter.exportReportToPdf(
                            context = context,
                            profile = shopProfileState,
                            transactions = transactions,
                            reportTitle = "Laporan Penjualan Bulanan"
                        )
                        if (file != null) {
                            Toast.makeText(context, "Mempersiapkan Cetak Laporan Bulanan...", Toast.LENGTH_SHORT).show()
                            com.example.util.DocumentExporter.printPdfFile(context, file, "Laporan_Bulanan")
                        } else {
                            Toast.makeText(context, "Gagal membuat Laporan Bulanan", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(Icons.Default.Print, "Cetak", modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cetak Laporan Bulanan", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Yearly Report Print Button
                Button(
                    onClick = {
                        val transactions = viewModel.getFilteredTransactionsForPeriod("Tahunan")
                        val file = com.example.util.DocumentExporter.exportReportToPdf(
                            context = context,
                            profile = shopProfileState,
                            transactions = transactions,
                            reportTitle = "Laporan Penjualan Tahunan"
                        )
                        if (file != null) {
                            Toast.makeText(context, "Mempersiapkan Cetak Laporan Tahunan...", Toast.LENGTH_SHORT).show()
                            com.example.util.DocumentExporter.printPdfFile(context, file, "Laporan_Tahunan")
                        } else {
                            Toast.makeText(context, "Gagal membuat Laporan Tahunan", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(Icons.Default.Print, "Cetak", modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cetak Laporan Tahunan", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Intermezzo: Breakdown of sales channels
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "B. Pembayaran Berdasarkan Metode",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(14.dp))

                val (tunai, qris, kartu) = paymentBreakdown
                val totalMethodsSum = (tunai.first + qris.first + kartu.first).coerceAtLeast(1.0)

                // 1. Tunai Channel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MonetizationOn, "Tunai", tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tunai / Cash", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                    }
                    Text(
                        text = "${tunai.second} Trx  •  ${String.format(Locale("id", "ID"), "Rp %,.0f", tunai.first)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                LinearProgressIndicator(
                    progress = (tunai.first / totalMethodsSum).toFloat(),
                    color = Color(0xFFD97706),
                    trackColor = Color(0xFFFEF3C7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 2. QRIS Channel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payment, "QRIS", tint = Color(0xFF0B4C8C), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("QRIS Digital", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                    }
                    Text(
                        text = "${qris.second} Trx  •  ${String.format(Locale("id", "ID"), "Rp %,.0f", qris.first)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                LinearProgressIndicator(
                    progress = (qris.first / totalMethodsSum).toFloat(),
                    color = Color(0xFF0B4C8C),
                    trackColor = Color(0xFFEFF6FF),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Kartu Channel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payment, "Kartu", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kartu Debit/Kredit", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                    }
                    Text(
                        text = "${kartu.second} Trx  •  ${String.format(Locale("id", "ID"), "Rp %,.0f", kartu.first)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                LinearProgressIndicator(
                    progress = (kartu.first / totalMethodsSum).toFloat(),
                    color = Color(0xFF10B981),
                    trackColor = Color(0xFFECFDF5),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Real-time Trend curve visualizer canvas
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Sales Trend Curve (${reportPeriodSelected})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                MiniSalesTrendChart(transactions = finalPeriodListFiltered)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Top Selling items dynamic layout list
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("C. Menu Terlaris (Berdasarkan Database)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(12.dp))

                if (dynamicBestSellers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada menu yang terjual pada periode ini.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    dynamicBestSellers.take(5).forEach { (itemName, qty, revenue) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFFFECE0), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Coffee, "Kopi", tint = Color(0xFFB45309), modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(itemName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                                    Text("$qty Unit Terjual", fontSize = 11.sp, color = Color.Gray)
                                }
                            }

                            Text(
                                String.format(Locale("id", "ID"), "Rp %,.0f", revenue),
                                color = Color(0xFF0B4C8C),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Divider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // D. Transaction list details Registry section
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "D. Riwayat Registrasi Transaksi Rinci",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )
                Text(
                    text = "Klik baris transaksi untuk melihat struk & membagikan ke WhatsApp.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (finalPeriodListFiltered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tidak ada transaksi pada periode ini.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                } else {
                    finalPeriodListFiltered.forEach { trx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTrxForDetails = trx }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = trx.invoiceNo,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0B4C8C)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = when (trx.paymentMethod) {
                                                    "QRIS" -> Color(0xFFEFF6FF)
                                                    "Tunai" -> Color(0xFFFEF3C7)
                                                    else -> Color(0xFFECFDF5)
                                                },
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = trx.paymentMethod,
                                            color = when (trx.paymentMethod) {
                                                "QRIS" -> Color(0xFF1E40AF)
                                                "Tunai" -> Color(0xFF92400E)
                                                else -> Color(0xFF065F46)
                                            },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Kasir: ${trx.cashierName} • ${trx.getFormattedDate()}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = String.format(Locale("id", "ID"), "Rp %,.0f", trx.totalAmount),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = "Detail",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Divider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Export Report Button triggers
        Button(
            onClick = { viewModel.triggerReportExport(context, reportPeriodSelected) },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B4C8C)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("export_report_button")
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DownloadForOffline, "Export")
                Text("Export Laporan Lengkap ke PDF & Excel", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * 5. Inventory Management Screen (CRUD of products database)
 */
@Composable
fun ProdukScreen(viewModel: POSViewModel) {
    val allProductsList by viewModel.allProducts.collectAsState()
    val editingProdState by viewModel.editingProduct.collectAsState()
    var showProductFormDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Register image picker launcher outside conditional components
    var selectedImageUriString by remember { mutableStateOf<String?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val filePath = com.example.util.DocumentExporter.copyImageToInternalStorage(context, uri)
            if (filePath != null) {
                selectedImageUriString = filePath
            } else {
                Toast.makeText(context, "Gagal memproses gambar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Dialog form for creating or editing product record
    if (showProductFormDialog || editingProdState != null) {
        var inputName by remember { mutableStateOf(editingProdState?.name ?: "") }
        var inputBarcode by remember { mutableStateOf(editingProdState?.barcode ?: "") }
        var inputPrice by remember { mutableStateOf(editingProdState?.price?.toString() ?: "") }
        var inputStock by remember { mutableStateOf(editingProdState?.stock?.toString() ?: "") }
        var inputMinStock by remember { mutableStateOf(editingProdState?.minStock?.toString() ?: "5") }
        var inputCategory by remember { mutableStateOf(editingProdState?.category ?: "Makanan") }
        var inputImageKey by remember { mutableStateOf(editingProdState?.imageUrl ?: "") }
        var inputDiscountPercent by remember { mutableStateOf(editingProdState?.discountPercent?.toString() ?: "0.0") }

        // Automatically update image field when a custom file is picked
        LaunchedEffect(selectedImageUriString) {
            selectedImageUriString?.let {
                inputImageKey = it
                selectedImageUriString = null
            }
        }

        Dialog(onDismissRequest = { 
            showProductFormDialog = false
            viewModel.editingProduct.value = null
        }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isEdit = editingProdState != null
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEdit) "Edit Produk" else "Tambah Produk Baru",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                        IconButton(onClick = { 
                            showProductFormDialog = false
                            viewModel.editingProduct.value = null
                        }) {
                            Icon(Icons.Default.Close, "Tutup", tint = Color.Gray)
                        }
                    }

                    Divider()

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Nama Barang") },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_name_input_field")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = inputBarcode,
                        onValueChange = { inputBarcode = it },
                        label = { Text("Kode SKU / Barcode") },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = inputPrice,
                            onValueChange = { inputPrice = it },
                            label = { Text("Harga (IDR)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = inputStock,
                            onValueChange = { inputStock = it },
                            label = { Text("Stok Awal") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = inputMinStock,
                            onValueChange = { inputMinStock = it },
                            label = { Text("Ambang Stok Kritis") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        // Category field dropdown mock representation
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = inputCategory,
                                onValueChange = { inputCategory = it },
                                label = { Text("Kategori (Makanan/Minuman/Lainnya)") },
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = inputDiscountPercent,
                        onValueChange = { inputDiscountPercent = it },
                        label = { Text("Diskon Item Produk (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // IMAGE SELECTOR AREA WITH FULL IMAGE LOADER & GALLERY TRIGGER
                    Text(
                        text = "Foto / Gambar Barang",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.Black,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Image Preview Box
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            ProductIconFallback(
                                imageUrl = inputImageKey,
                                category = inputCategory,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(14.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, "Gallery", modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pilih Foto", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (inputImageKey.contains("/")) "Menggunakan file foto lokal" else if (inputImageKey.isNotBlank()) "Preset: $inputImageKey" else "Belum ada foto (Default)",
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isEdit) {
                            Button(
                                onClick = {
                                    viewModel.deleteSelectedProduct(editingProdState!!)
                                    Toast.makeText(context, "Produk Dihapus", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Hapus", fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                val priceD = inputPrice.toDoubleOrNull() ?: 0.0
                                val stockI = inputStock.toIntOrNull() ?: 0
                                val minI = inputMinStock.toIntOrNull() ?: 5
                                val discountD = inputDiscountPercent.toDoubleOrNull() ?: 0.0
                                
                                if (inputName.isBlank() || inputBarcode.isBlank()) {
                                    Toast.makeText(context, "Nama dan Barcode wajib diisi!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                
                                viewModel.createOrUpdateProduct(
                                    inputName, inputBarcode, priceD, stockI, minI, inputCategory, inputImageKey, discountD
                                )
                                Toast.makeText(context, "Produk Berhasil Disimpan", Toast.LENGTH_SHORT).show()
                                showProductFormDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B4C8C)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Simpan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // MAIN LIST OF PRODUCTS PANEL
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    viewModel.editingProduct.value = null
                    showProductFormDialog = true 
                },
                containerColor = Color(0xFF0B4C8C),
                contentColor = Color.White,
                modifier = Modifier.testTag("add_product_fab")
            ) {
                Icon(Icons.Default.Add, "Tambah")
            }
        },
        containerColor = Color.Transparent
    ) { p ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(p)
                .padding(16.dp)
        ) {
            Text(
                text = "Manajemen Inventaris",
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "Kelola, pantau, dan selaraskan ketersediaan stok fisik barang.",
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Main items catalog scroll
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(allProductsList) { prod ->
                    val isLowStock = prod.stock <= prod.minStock
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.editingProduct.value = prod }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Falling back beautifully to vector illustrations
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                ProductIconFallback(
                                    imageUrl = prod.imageUrl,
                                    category = prod.category,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = prod.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.Black
                                )
                                Text(
                                    text = "SKU: ${prod.barcode}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Price
                                    Text(
                                        text = String.format(Locale("id", "ID"), "Rp %,.0f", prod.price),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0B4C8C)
                                    )
                                    // Stok
                                    Text(
                                        text = "Stok: ${prod.stock} Unit",
                                        fontSize = 12.sp,
                                        color = if (isLowStock) Color.Red else Color.DarkGray,
                                        fontWeight = if (isLowStock) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }

                            // Red or Green stock safety level warning tag
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isLowStock) Color(0xFFFFF0F0) else Color(0xFFE2F9EC),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isLowStock) "Low Stock" else "In Stock",
                                    color = if (isLowStock) Color.Red else Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 6. Settings Screen (Customizing logo/details + RBAC configuration display)
 */
@Composable
fun SettingsScreen(viewModel: POSViewModel) {
    val shopProfile by viewModel.shopProfile.collectAsState()
    val usersList by viewModel.allUsers.collectAsState()
    var showUserFormDialog by remember { mutableStateOf(false) }
    var editingUserRecord by remember { mutableStateOf<User?>(null) }
    val context = LocalContext.current

    var pName by remember(shopProfile) { mutableStateOf(shopProfile.shopName) }
    var pOwner by remember(shopProfile) { mutableStateOf(shopProfile.ownerName) }
    var pPhone by remember(shopProfile) { mutableStateOf(shopProfile.phone) }
    var pAddress by remember(shopProfile) { mutableStateOf(shopProfile.address) }
    var pTax by remember(shopProfile) { mutableStateOf(shopProfile.taxRate.toString()) }
    var pService by remember(shopProfile) { mutableStateOf(shopProfile.serviceCharge.toString()) }
    var pLogoUrl by remember(shopProfile) { mutableStateOf(shopProfile.logoUrl) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val filePath = com.example.util.DocumentExporter.copyImageToInternalStorage(context, uri)
            if (filePath != null) {
                pLogoUrl = filePath
            } else {
                Toast.makeText(context, "Gagal memproses gambar logo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Pengaturan Profil Usaha",
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            color = Color(0xFF0F172A)
        )
        Text(
            text = "Sesuaikan identitas, logo, alamat kelengkapan struk, dan perpajakan.",
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Main info fields form layout
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Identitas Toko & Logo", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(14.dp))

                // Logo Input & Selector Area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color(0xFFE2E8F0), CircleShape)
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (pLogoUrl.isNotEmpty()) {
                            com.example.ui.BusinessLogoView(
                                logoUrl = pLogoUrl,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = "Default Logo",
                                tint = Color.LightGray,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Logo Usaha",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Upload Button
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.Upload, "Upload", modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload Logo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (pLogoUrl.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { pLogoUrl = "" },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                ) {
                                    Text("Hapus", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Sample Preset Selector
                Text(
                    text = "Pilih Contoh Logo:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val presets = listOf(
                        Pair("preset_coffee", "Kopi"),
                        Pair("preset_resto", "Resto"),
                        Pair("preset_store", "Toko"),
                        Pair("preset_bakery", "Kue"),
                        Pair("preset_clothing", "Butik")
                    )
                    presets.forEach { preset ->
                        val isSelected = pLogoUrl == preset.first
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF0B4C8C) else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { pLogoUrl = preset.first }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                ) {
                                    com.example.ui.BusinessLogoView(logoUrl = preset.first, modifier = Modifier.fillMaxSize())
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = preset.second,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF0B4C8C) else Color.DarkGray
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFE2E8F0))
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = pName,
                    onValueChange = { pName = it },
                    label = { Text("Nama Toko / Usaha") },
                    leadingIcon = { Icon(Icons.Default.Store, "Toko") },
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shop_name_profile_field")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = pOwner,
                    onValueChange = { pOwner = it },
                    label = { Text("Nama Pemilik (Owner)") },
                    leadingIcon = { Icon(Icons.Default.Person, "Owner") },
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = pPhone,
                    onValueChange = { pPhone = it },
                    label = { Text("Nomor WhatsApp / Telp") },
                    leadingIcon = { Icon(Icons.Default.Phone, "Telp") },
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = pAddress,
                    onValueChange = { pAddress = it },
                    label = { Text("Alamat Lengkap Toko") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, "Alamat") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Financial config fields: tax rates & services charge fees
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Tarif & Biaya Tambahan", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = pTax,
                        onValueChange = { pTax = it },
                        label = { Text("PPN / Pajak (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = pService,
                        onValueChange = { pService = it },
                        label = { Text("Biaya Layanan (Flat)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Auto print setup card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Pengaturan Printer & Struk",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cetak Struk Otomatis",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                        Text(
                            text = "Secara otomatis mencetak struk belanja ke printer thermal setelah transaksi selesai.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    val autoPrint by viewModel.autoPrintEnabled.collectAsState()
                    Switch(
                        checked = autoPrint,
                        onCheckedChange = { viewModel.setAutoPrintEnabled(it) },
                        modifier = Modifier.testTag("auto_print_toggle")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // USER ACCOUNT CRUD MANAGEMENT INTERFACE
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kelola Akun Pengguna / Staff",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.Black
                        )
                        Text(
                            text = "Daftar staff yang memiliki hak akses login ke sistem POS.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Button(
                        onClick = {
                            editingUserRecord = null
                            showUserFormDialog = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B4C8C)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("+ Tambah", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                usersList.forEach { user ->
                    val isPrimaryAdmin = user.username.lowercase() == "admin"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // User initials badge
                            val initials = if (user.name.isNotBlank()) {
                                user.name.split(" ")
                                    .take(2)
                                    .mapNotNull { it.firstOrNull()?.toString() }
                                    .joinToString("")
                                    .uppercase()
                            } else {
                                "US"
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = if (user.role == "Admin") Color(0xFFEFF6FF) else Color(0xFFF1F5F9),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (user.role == "Admin") Color(0xFF0B4C8C) else Color(0xFF475569)
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = user.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.Black
                                    )
                                    // Role Badge
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = if (user.role == "Admin") Color(0xFFDBEAFE) else Color(0xFFF1F5F9),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = user.role,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (user.role == "Admin") Color(0xFF1E40AF) else Color(0xFF475569)
                                        )
                                    }
                                }
                                Text(
                                    text = "@${user.username} • PIN: ${user.pin}",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Edit modifier
                            IconButton(
                                onClick = {
                                    editingUserRecord = user
                                    showUserFormDialog = true
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit User",
                                    tint = Color(0xFF0B4C8C),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Delete modifier (disable for master account Admin to prevent zero login states)
                            IconButton(
                                onClick = {
                                    if (isPrimaryAdmin) {
                                        Toast.makeText(context, "Akun Master Admin tidak boleh dihapus!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.deleteUser(user.username)
                                        Toast.makeText(context, "Pengguna @${user.username} Dihapus", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(32.dp),
                                enabled = !isPrimaryAdmin
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus User",
                                    tint = if (isPrimaryAdmin) Color.LightGray else Color.Red,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Divider(color = Color(0xFFF1F5F9))
                }
            }
        }

        // USER ACCOUNT DIALOG FORM
        if (showUserFormDialog) {
            val isEditing = editingUserRecord != null
            var uUsername by remember { mutableStateOf(editingUserRecord?.username ?: "") }
            var uFullName by remember { mutableStateOf(editingUserRecord?.name ?: "") }
            var uPin by remember { mutableStateOf(editingUserRecord?.pin ?: "") }
            var uRole by remember { mutableStateOf(editingUserRecord?.role ?: "Cashier") }

            Dialog(onDismissRequest = { showUserFormDialog = false }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(18.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEditing) "Edit Akun Pengguna" else "Tambah Pengguna Baru",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                            IconButton(onClick = { showUserFormDialog = false }) {
                                Icon(Icons.Default.Close, "Tutup", tint = Color.Gray)
                            }
                        }

                        Divider()
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = uUsername,
                            onValueChange = { if (!isEditing) uUsername = it },
                            label = { Text("Username Login") },
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            enabled = !isEditing,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = uFullName,
                            onValueChange = { uFullName = it },
                            label = { Text("Nama Lengkap") },
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = uPin,
                            onValueChange = { uPin = it },
                            label = { Text("PIN Belanja (Numeric)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Role selection header
                        Text(
                            text = "Hak Akses Pengguna:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.Black,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Admin", "Cashier").forEach { roleName ->
                                val roleSel = uRole == roleName
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (roleSel) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
                                        .border(
                                            width = if (roleSel) 2.dp else 1.dp,
                                            color = if (roleSel) Color(0xFF0B4C8C) else Color(0xFFE2E8F0),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { uRole = roleName }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = roleName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (roleSel) Color(0xFF0B4C8C) else Color.DarkGray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showUserFormDialog = false },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Batal", fontWeight = FontWeight.Bold)
                            }
                            
                            Button(
                                onClick = {
                                    if (uUsername.isBlank() || uFullName.isBlank() || uPin.isBlank()) {
                                        Toast.makeText(context, "Semua kolom wajib diisi!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val newUser = User(
                                        username = uUsername,
                                        name = uFullName,
                                        pin = uPin,
                                        role = uRole
                                    )
                                    viewModel.saveUser(newUser)
                                    Toast.makeText(context, "Akun @$uUsername berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                    showUserFormDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B4C8C)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Text("Simpan", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val taxD = pTax.toDoubleOrNull() ?: 10.0
                val serviceD = pService.toDoubleOrNull() ?: 2000.0
                viewModel.saveShopProfile(
                    name = pName,
                    owner = pOwner,
                    phone = pPhone,
                    address = pAddress,
                    tax = taxD,
                    service = serviceD,
                    logoUrl = pLogoUrl
                )
                Toast.makeText(context, "Profil Toko Berhasil Diperbarui!", Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B4C8C)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_profile_button")
        ) {
            Text("Simpan Perubahan Pengaturan", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ABOUT DEVELOPER CARD
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth().testTag("about_developer_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Main Header Row with Dev Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFEFF6FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Tentang Pengembang",
                            tint = Color(0xFF0B4C8C),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Tentang Pengembang",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.Black
                        )
                        Text(
                            text = "Profil pengembang dan teknologi sistem POS.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(14.dp))

                // Profile Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Developer Initials Avatar
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0B4C8C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "FN",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Fatkun Nuha",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Senior Mobile & Full-Stack Developer",
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = Color(0xFF0B4C8C)
                        )
                        Text(
                            text = "fatkunnuha5@gmail.com",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Aplikasi QuickPOS ini dirancang dengan standar performa tinggi, " +
                           "menawarkan antarmuka modern Material 3, transaksi secepat kilat, " +
                           "serta sinkronisasi basis data Room yang handal dan offline-first.",
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Skills / Tech Badges
                Text(
                    text = "Teknologi Utama:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val row1 = listOf("Kotlin", "Jetpack Compose", "Room Database")
                    row1.forEach { tech ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tech,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val row2 = listOf("Material 3", "MVVM Layer", "Clean Code")
                    row2.forEach { tech ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tech,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Contact / Support Button
                Button(
                    onClick = {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                data = android.net.Uri.parse("mailto:fatkunnuha5@gmail.com")
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "Tanya QuickPOS Developer")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Tidak ada aplikasi email terpasang", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF), contentColor = Color(0xFF0B4C8C)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Hubungi Pengembang",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Hubungi Pengembang via Email",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Logout session option
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFECE0)),
            border = BorderStroke(1.dp, Color(0xFFFFCCAC)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Keluar dari Sesi Aktif", fontWeight = FontWeight.Bold, color = Color(0xFFC2410C), fontSize = 14.sp)
                    Text("Matikan konektivitas login sekarang.", color = Color(0xFFC2410C), fontSize = 11.sp)
                }

                Button(
                    onClick = { viewModel.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Logout", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
