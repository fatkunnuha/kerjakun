package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "users")
data class User(
    @PrimaryKey val username: String,
    val name: String,
    val pin: String,
    val role: String, // "Admin" or "Cashier"
    val avatarUrl: String = ""
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val barcode: String,
    val price: Double,
    val stock: Int,
    val minStock: Int = 5, // Alert threshold when stock goes <= minStock
    val category: String, // "Makanan", "Minuman", "Lainnya"
    val imageUrl: String = "", // Placeholders or custom loaded images
    val createdAt: Long = System.currentTimeMillis(),
    val discountPercent: Double = 0.0 // Discount percentage (e.g. 10.0 for 10% discount)
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val invoiceNo: String,
    val cashierName: String,
    val totalAmount: Double,
    val paymentMethod: String, // "Tunai", "QRIS", "Kartu"
    val status: String, // "PAID", "FAILED", "PENDING"
    val timestamp: Long = System.currentTimeMillis(),
    val taxProcessed: Double = 0.0,
    val serviceChargeProcessed: Double = 0.0
) {
    fun getFormattedDate(pattern: String = "dd MMM yyyy, HH:mm"): String {
        return SimpleDateFormat(pattern, Locale("id", "ID")).format(Date(timestamp))
    }
}

@Entity(tableName = "transaction_items")
data class TransactionItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val transactionId: Int,
    val productId: Int,
    val productName: String,
    val price: Double,
    val quantity: Int,
    val subtotal: Double
)

@Entity(tableName = "shop_profile")
data class ShopProfile(
    @PrimaryKey val id: Int = 1,
    val shopName: String = "QuickPOS Cafe & Roastery",
    val ownerName: String = "Budi Hartono",
    val phone: String = "0812-3456-7890",
    val address: String = "Jl. Merdeka Barat No. 45, Jakarta Pusat",
    val logoUrl: String = "",
    val taxRate: Double = 10.0, // in %
    val serviceCharge: Double = 2000.0, // Fixed rupiah
    val qrisPaymentGatewayId: String = "MID_MOCK_QRIS_12389"
)
