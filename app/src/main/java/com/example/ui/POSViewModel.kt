package com.example.ui

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.*
import com.example.util.DocumentExporter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class POSViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "quickpos_db"
    ).fallbackToDestructiveMigration().build()

    val repository = PosRepository(db)

    // Current Screen
    private val _currentScreen = MutableStateFlow(Screen.LOGIN)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Current logged-in Session
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Cart State
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    // Filter States
    val searchQuery = MutableStateFlow("")
    val categoryFilter = MutableStateFlow("Semua")

    // Active Checkout State
    val checkoutPaymentMethod = MutableStateFlow("Tunai")
    
    // Automatic Receipt Printing State
    private val prefs = getApplication<Application>().getSharedPreferences("quickpos_prefs", Context.MODE_PRIVATE)
    val autoPrintEnabled = MutableStateFlow(prefs.getBoolean("auto_print", true))

    fun setAutoPrintEnabled(enabled: Boolean) {
        autoPrintEnabled.value = enabled
        prefs.edit().putBoolean("auto_print", enabled).apply()
    }

    // Toggle for Item View Layout (True = Grid, False = List/Horizontal view)
    val isGridView = MutableStateFlow(prefs.getBoolean("is_grid_view", true))

    fun setGridViewEnabled(enabled: Boolean) {
        isGridView.value = enabled
        prefs.edit().putBoolean("is_grid_view", enabled).apply()
    }
    
    // Receipt Preview State
    val showCheckoutReceipt = MutableStateFlow(false)
    val receiptInvoice = MutableStateFlow<Transaction?>(null)
    val receiptItems = MutableStateFlow<List<TransactionItem>>(emptyList())

    // Shop Profile Info
    val shopProfile: StateFlow<ShopProfile> = repository.shopProfile
        .map { it ?: ShopProfile() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShopProfile())

    // Products list
    val allProducts: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Computed filtered products for Cashier Selling
    val filteredProducts: StateFlow<List<Product>> = combine(
        allProducts,
        searchQuery,
        categoryFilter
    ) { products, query, cat ->
        products.filter { p ->
            val matchQuery = p.name.contains(query, ignoreCase = true) || p.barcode.contains(query)
            val matchCategory = cat == "Semua" || p.category.equals(cat, ignoreCase = true)
            matchQuery && matchCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All transaction histories lists
    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Low stock product lists (alerts widget state)
    val lowStockProducts: StateFlow<List<Product>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Report Interval State ("Harian", "Bulanan", "Tahunan")
    val reportType = MutableStateFlow("Harian")

    // Export variables
    private val _exportFileStatus = MutableStateFlow<File?>(null)
    val exportFileStatus: StateFlow<File?> = _exportFileStatus.asStateFlow()
    
    val lastExportedPdf = MutableStateFlow<File?>(null)
    val lastExportedExcel = MutableStateFlow<File?>(null)
    val showExportDialog = MutableStateFlow(false)

    // Dialog state for updating/adding product
    val editingProduct = MutableStateFlow<Product?>(null)

    private val _allTransactionItems = MutableStateFlow<List<TransactionItem>>(emptyList())
    val allTransactionItems = _allTransactionItems.asStateFlow()

    fun loadAllTransactionItems() {
        viewModelScope.launch {
            _allTransactionItems.value = repository.getAllTransactionItems()
        }
    }

    init {
        // Automatically seed mock data if first launch
        viewModelScope.launch {
            repository.seedMockData()
            repository.getOrInitializeProfile()
            
            // Automatically reload the transaction items whenever transactions change
            launch {
                repository.allTransactions.collect {
                    loadAllTransactionItems()
                }
            }
            
            // Check if there is a saved session
            val savedUsername = prefs.getString("logged_in_username", null)
            if (savedUsername != null) {
                val user = repository.getUserByUsername(savedUsername)
                if (user != null) {
                    _currentUser.value = user
                    _currentScreen.value = Screen.DASHBOARD
                }
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun login(username: String, pin: String, context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val user = repository.authenticateUser(username, pin)
            if (user != null) {
                _currentUser.value = user
                _currentScreen.value = Screen.DASHBOARD
                prefs.edit().putString("logged_in_username", username).apply()
                onSuccess()
            } else {
                Toast.makeText(context, "Username / PIN salah!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun logout() {
        prefs.edit().remove("logged_in_username").apply()
        _currentUser.value = null
        _cart.value = emptyList()
        _currentScreen.value = Screen.LOGIN
    }

    // User Account Management flows & methods
    val allUsers: StateFlow<List<User>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveUser(user: User) {
        viewModelScope.launch {
            repository.registerUser(user)
        }
    }

    fun deleteUser(username: String) {
        viewModelScope.launch {
            repository.deleteUser(username)
        }
    }

    // Cart operations
    fun addToCart(product: Product) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == product.id }
        
        // Check stock levels
        val currentQtyInCart = if (index != -1) currentList[index].quantity else 0
        if (currentQtyInCart >= product.stock) {
            // Cannot add more than in stock
            return
        }

        if (index != -1) {
            currentList[index] = currentList[index].copy(quantity = currentQtyInCart + 1)
        } else {
            currentList.add(CartItem(product, 1))
        }
        _cart.value = currentList
    }

    fun decreaseQuantity(product: Product) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            val qty = currentList[index].quantity
            if (qty > 1) {
                currentList[index] = currentList[index].copy(quantity = qty - 1)
            } else {
                currentList.removeAt(index)
            }
            _cart.value = currentList
        }
    }

    fun removeFromCart(product: Product) {
        _cart.value = _cart.value.filter { it.product.id != product.id }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    // Summing helpers
    fun getCartSubtotal(): Double {
        return _cart.value.sumOf { 
            val effectivePrice = it.product.price * (1.0 - it.product.discountPercent / 100.0)
            effectivePrice * it.quantity 
        }
    }

    fun getCartTaxAmount(profile: ShopProfile): Double {
        return getCartSubtotal() * (profile.taxRate / 100.0)
    }

    fun getCartTotal(profile: ShopProfile): Double {
        return getCartSubtotal() + getCartTaxAmount(profile) + profile.serviceCharge
    }

    // Barcode Scanning Simulation
    fun scanBarcode(barcode: String, context: Context): Boolean {
        var found = false
        viewModelScope.launch {
            val product = repository.getProductByBarcode(barcode)
            if (product != null) {
                if (product.stock > 0) {
                    addToCart(product)
                    found = true
                    Toast.makeText(context, "Berhasil scan: ${product.name}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Stok produk ${product.name} habis!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Barcode tidak terdaftar: $barcode", Toast.LENGTH_SHORT).show()
            }
        }
        return found
    }

    // Checkout processing
    fun executeCheckout(context: Context) {
        val currentCart = _cart.value
        if (currentCart.isEmpty()) {
            Toast.makeText(context, "Keranjang belanja kosong", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            val p = shopProfile.value
            val subtotal = getCartSubtotal()
            val taxAmount = getCartTaxAmount(p)
            val serviceAmount = p.serviceCharge
            val grandTotal = getCartTotal(p)
            
            // Random dynamic invoice number to match screenshots
            val randomNum = (1000..9999).random() 
            val invoiceNo = "TRX-$randomNum"
            
            val trxName = _currentUser.value?.name ?: "Kasir Utama"
            val transactionResult = repository.checkout(
                invoiceNo = invoiceNo,
                cashierName = trxName,
                cartItems = currentCart,
                paymentMethod = checkoutPaymentMethod.value,
                taxAmount = taxAmount,
                serviceAmount = serviceAmount,
                grandTotal = grandTotal
            )

            if (transactionResult != null) {
                receiptInvoice.value = transactionResult
                // Fetch transactional line items
                val itemsList = repository.getItemsForTransaction(transactionResult.id)
                receiptItems.value = itemsList
                showCheckoutReceipt.value = true
                clearCart()
                
                if (autoPrintEnabled.value) {
                    Toast.makeText(context, "Transaksi $invoiceNo Berhasil! [Otomatis] Mencetak struk...", Toast.LENGTH_LONG).show()
                    val pdfFile = com.example.util.DocumentExporter.exportReceiptToPdf(context, transactionResult, itemsList, p)
                    if (pdfFile != null) {
                        com.example.util.DocumentExporter.printPdfFile(context, pdfFile, "Struk_${transactionResult.invoiceNo}")
                    }
                } else {
                    Toast.makeText(context, "Transaksi $invoiceNo Berhasil Disimpan!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, "Terjadi kesalahan dalam checkout", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // PDF and Excel exports wrapper
    fun triggerReportExport(context: Context, type: String) {
        viewModelScope.launch {
            val p = shopProfile.value
            val trxs = getFilteredTransactionsForPeriod(type)
            
            val pdfFile = DocumentExporter.exportReportToPdf(context, p, trxs, type)
            val excelFile = DocumentExporter.exportReportToExcel(context, p, trxs, type)
            
            if (pdfFile != null && excelFile != null) {
                lastExportedPdf.value = pdfFile
                lastExportedExcel.value = excelFile
                showExportDialog.value = true
                Toast.makeText(
                    context, 
                    "Selesai! Laporan berhasil di-export ke PDF & Excel.", 
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(context, "Gagal mengexport file laporan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun clearPdfStatus() {
        _exportFileStatus.value = null
    }

    // Analytics: Fetch transactions of selected periods
    fun getFilteredTransactionsForPeriod(period: String): List<Transaction> {
        val allTrxs = allTransactions.value
        val now = System.currentTimeMillis()
        val calNow = Calendar.getInstance().apply { timeInMillis = now }

        return allTrxs.filter { trx ->
            val calTrx = Calendar.getInstance().apply { timeInMillis = trx.timestamp }
            when (period) {
                "Harian" -> {
                    calTrx.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                    calTrx.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)
                }
                "Bulanan" -> {
                    calTrx.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                    calTrx.get(Calendar.MONTH) == calNow.get(Calendar.MONTH)
                }
                "Tahunan" -> {
                    calTrx.get(Calendar.YEAR) == calNow.get(Calendar.YEAR)
                }
                else -> true
            }
        }
    }

    // Total earnings metrics for report state
    fun getSalesSumForPeriod(period: String): Double {
        return getFilteredTransactionsForPeriod(period)
            .filter { it.status == "PAID" }
            .sumOf { it.totalAmount }
    }

    fun getTransactionsCountForPeriod(period: String): Int {
        return getFilteredTransactionsForPeriod(period).size
    }

    // Product Inventory CRUD Actions
    fun createOrUpdateProduct(
        name: String,
        barcode: String,
        price: Double,
        stock: Int,
        minStock: Int,
        category: String,
        imageUrl: String,
        discountPercent: Double = 0.0
    ) {
        viewModelScope.launch {
            val active = editingProduct.value
            if (active != null) {
                val updatedProduct = active.copy(
                    name = name,
                    barcode = barcode,
                    price = price,
                    stock = stock,
                    minStock = minStock,
                    category = category,
                    imageUrl = imageUrl,
                    discountPercent = discountPercent
                )
                repository.saveProduct(updatedProduct)
            } else {
                val newProduct = Product(
                    name = name,
                    barcode = barcode,
                    price = price,
                    stock = stock,
                    minStock = minStock,
                    category = category,
                    imageUrl = imageUrl,
                    discountPercent = discountPercent
                )
                repository.saveProduct(newProduct)
            }
            editingProduct.value = null
        }
    }

    fun deleteSelectedProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product.id)
            editingProduct.value = null
        }
    }

    // Settings Profile updates
    fun saveShopProfile(
        name: String,
        owner: String,
        phone: String,
        address: String,
        tax: Double,
        service: Double,
        logoUrl: String
    ) {
        viewModelScope.launch {
            val updated = shopProfile.value.copy(
                shopName = name,
                ownerName = owner,
                phone = phone,
                address = address,
                taxRate = tax,
                serviceCharge = service,
                logoUrl = logoUrl
            )
            repository.updateShopProfile(updated)
        }
    }
}

enum class Screen {
    LOGIN,
    DASHBOARD,
    KASIR,
    LAPORAN,
    PRODUK,
    SETTINGS
}
