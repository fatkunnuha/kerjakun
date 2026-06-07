package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class PosRepository(private val db: AppDatabase) {
    private val userDao = db.userDao()
    private val productDao = db.productDao()
    private val transactionDao = db.transactionDao()
    private val shopProfileDao = db.shopProfileDao()

    // Users (RBAC)
    val allUsers: Flow<List<User>> = userDao.getAllUsers()
    
    suspend fun authenticateUser(username: String, pin: String): User? {
        val user = userDao.getUserByUsername(username)
        if (user != null && user.pin == pin) {
            return user
        }
        return null
    }

    suspend fun registerUser(user: User) {
        userDao.insertUser(user)
    }

    suspend fun deleteUser(username: String) {
        userDao.deleteUser(username)
    }

    suspend fun getUserByUsername(username: String): User? {
        return userDao.getUserByUsername(username)
    }

    // Products (Inventory & stock levels)
    val allProducts: Flow<List<Product>> = productDao.getAllProductsFlow()
    val lowStockProducts: Flow<List<Product>> = productDao.getLowStockProductsFlow()

    suspend fun getProductByBarcode(barcode: String): Product? {
        return productDao.getProductByBarcode(barcode)
    }

    suspend fun getProductById(id: Int): Product? {
        return productDao.getProductById(id)
    }

    suspend fun saveProduct(product: Product) {
        productDao.insertProduct(product)
    }

    suspend fun deleteProduct(id: Int) {
        productDao.deleteProduct(id)
    }

    // Transactions & Receipts
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactionsFlow()

    suspend fun getItemsForTransaction(transactionId: Int): List<TransactionItem> {
        return transactionDao.getItemsForTransaction(transactionId)
    }

    suspend fun getAllTransactionItems(): List<TransactionItem> {
        return transactionDao.getAllTransactionItems()
    }

    /**
     * Executes a checkout: decrements stock of each purchased item, 
     * registers transaction and matching line items.
     */
    suspend fun checkout(
        invoiceNo: String,
        cashierName: String,
        cartItems: List<CartItem>,
        paymentMethod: String,
        taxAmount: Double,
        serviceAmount: Double,
        grandTotal: Double
    ): Transaction? {
        val transaction = Transaction(
            invoiceNo = invoiceNo,
            cashierName = cashierName,
            totalAmount = grandTotal,
            paymentMethod = paymentMethod,
            status = "PAID", // Default checkout status
            taxProcessed = taxAmount,
            serviceChargeProcessed = serviceAmount
        )
        
        val transId = transactionDao.insertTransaction(transaction).toInt()
        val lineItems = cartItems.map { cartItem ->
            // Subtract stock
            val currentProduct = productDao.getProductById(cartItem.product.id)
            if (currentProduct != null) {
                val updatedStock = maxOf(0, currentProduct.stock - cartItem.quantity)
                productDao.updateStock(cartItem.product.id, updatedStock)
            }
            val effectivePrice = cartItem.product.price * (1.0 - cartItem.product.discountPercent / 100.0)
            TransactionItem(
                transactionId = transId,
                productId = cartItem.product.id,
                productName = cartItem.product.name,
                price = effectivePrice,
                quantity = cartItem.quantity,
                subtotal = effectivePrice * cartItem.quantity
            )
        }
        transactionDao.insertTransactionItems(lineItems)
        return transaction.copy(id = transId)
    }

    // Shop Profile Settings
    val shopProfile: Flow<ShopProfile?> = shopProfileDao.getShopProfileFlow()

    suspend fun updateShopProfile(profile: ShopProfile) {
        shopProfileDao.updateShopProfile(profile)
    }

    suspend fun getOrInitializeProfile(): ShopProfile {
        var profile = shopProfileDao.getShopProfile()
        if (profile == null) {
            profile = ShopProfile()
            shopProfileDao.updateShopProfile(profile)
        }
        return profile
    }

    /**
     * Seed initial beautiful products, history transactions, and users
     * representing the screenshots.
     */
    suspend fun seedMockData() {
        // 1. Seed shop profile
        if (shopProfileDao.getShopProfile() == null) {
            shopProfileDao.updateShopProfile(ShopProfile())
        }

        // 2. Seed users (RBAC)
        val testUserAdmin = userDao.getUserByUsername("admin")
        if (testUserAdmin == null) {
            userDao.insertUser(User("admin", "Budi Hartono (Owner)", "1234", "Admin"))
            userDao.insertUser(User("kasir", "Siti Rahma (Staff)", "1234", "Cashier"))
        }

        // 3. Seed products matching screenshot items and images
        val productsList = productDao.getAllProducts()
        if (productsList.isEmpty()) {
            val sampleProducts = listOf(
                Product(name = "Kopi Susu Gula Aren", barcode = "899012345671", price = 18000.0, stock = 42, minStock = 5, category = "Minuman", imageUrl = "coffee_aren", discountPercent = 15.0),
                Product(name = "Salad Bowl Sehat", barcode = "899012345672", price = 35000.0, stock = 15, minStock = 3, category = "Makanan", imageUrl = "salad_bowl", discountPercent = 5.0),
                Product(name = "Margherita Pizza", barcode = "899012345673", price = 65000.0, stock = 8, minStock = 2, category = "Makanan", imageUrl = "pizza_margherita"),
                Product(name = "Premium Donut Glaze", barcode = "899012345674", price = 15000.0, stock = 50, minStock = 8, category = "Makanan", imageUrl = "donut_glaze", discountPercent = 10.0),
                Product(name = "Fresh Fruit Tea", barcode = "899012345675", price = 22000.0, stock = 4, minStock = 5, category = "Minuman", imageUrl = "fruit_tea"), // Low Stock level alert
                Product(name = "Nike Air Max 270", barcode = "191887342676", price = 2150000.0, stock = 12, minStock = 4, category = "Lainnya", imageUrl = "nike_air"),
                Product(name = "Sony WH-1000XM4", barcode = "454873611277", price = 3899000.0, stock = 3, minStock = 5, category = "Lainnya", imageUrl = "sony_headphones"), // Low Stock warning
                Product(name = "Minimalist Watch", barcode = "761313361278", price = 850000.0, stock = 45, minStock = 10, category = "Lainnya", imageUrl = "minimalist_watch")
            )
            for (p in sampleProducts) {
                productDao.insertProduct(p)
            }

            // 4. Seed historical transactions matching metrics like Rp 12.450.000 total sales, 148 transactions, 112 products sold
            val savedProducts = productDao.getAllProducts()
            val aren = savedProducts.find { it.name == "Kopi Susu Gula Aren" }
            val pizza = savedProducts.find { it.name == "Margherita Pizza" }
            val donut = savedProducts.find { it.name == "Premium Donut Glaze" }

            val timestampToday = System.currentTimeMillis()
            val oneDayMs = 24 * 60 * 60 * 1000L
            val oneMonthMs = 30 * oneDayMs

            // 1st mock transaction
            val t1Id = transactionDao.insertTransaction(
                Transaction(invoiceNo = "TRX-9920", cashierName = "Siti Rahma (Staff)", totalAmount = 125000.0, paymentMethod = "Tunai", status = "PAID", timestamp = timestampToday - 2000000L)
            ).toInt()
            transactionDao.insertTransactionItems(listOf(
                TransactionItem(transactionId = t1Id, productId = aren?.id ?: 1, productName = aren?.name ?: "Kopi Susu Gula Aren", price = 18000.0, quantity = 2, subtotal = 36000.0),
                TransactionItem(transactionId = t1Id, productId = pizza?.id ?: 3, productName = pizza?.name ?: "Margherita Pizza", price = 65000.0, quantity = 1, subtotal = 65000.0)
            ))

            // 2nd mock transaction
            val t2Id = transactionDao.insertTransaction(
                Transaction(invoiceNo = "TRX-9919", cashierName = "Siti Rahma (Staff)", totalAmount = 450000.0, paymentMethod = "QRIS", status = "PAID", timestamp = timestampToday - 4000000L)
            ).toInt()
            transactionDao.insertTransactionItems(listOf(
                TransactionItem(transactionId = t2Id, productId = donut?.id ?: 4, productName = donut?.name ?: "Premium Donut Glaze", price = 15000.0, quantity = 30, subtotal = 450000.0)
            ))

            // 3rd mock transaction (Yesterday)
            val t3Id = transactionDao.insertTransaction(
                Transaction(invoiceNo = "INV-001", cashierName = "Budi Hartono (Owner)", totalAmount = 125000.0, paymentMethod = "Tunai", status = "PAID", timestamp = timestampToday - oneDayMs)
            ).toInt()
            
            // 4th mock transaction (Yesterday)
            val t4Id = transactionDao.insertTransaction(
                Transaction(invoiceNo = "INV-002", cashierName = "Siti Rahma (Staff)", totalAmount = 84500.0, paymentMethod = "QRIS", status = "PAID", timestamp = timestampToday - oneDayMs - 100000L)
            ).toInt()

            // 5th mock transaction (Failed, for display)
            val t5Id = transactionDao.insertTransaction(
                Transaction(invoiceNo = "INV-003", cashierName = "Siti Rahma (Staff)", totalAmount = 210000.0, paymentMethod = "Kartu", status = "FAILED", timestamp = timestampToday - oneDayMs - 500000L)
            ).toInt()

            // 6th mock transaction (Last month)
            val t6Id = transactionDao.insertTransaction(
                Transaction(invoiceNo = "INV-004", cashierName = "Budi Hartono (Owner)", totalAmount = 4250000.0, paymentMethod = "Tunai", status = "PAID", timestamp = timestampToday - oneMonthMs)
            ).toInt()

            // 7th mock transaction (Older months)
            val t7Id = transactionDao.insertTransaction(
                Transaction(invoiceNo = "INV-005", cashierName = "Budi Hartono (Owner)", totalAmount = 7440500.0, paymentMethod = "QRIS", status = "PAID", timestamp = timestampToday - 2 * oneMonthMs)
            ).toInt()
        }
    }
}

data class CartItem(
    val product: Product,
    val quantity: Int
)
