package com.example.ui

import java.io.File
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.CartItem
import com.example.data.Product
import com.example.data.ShopProfile
import com.example.data.Transaction
import com.example.data.TransactionItem
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Custom Barcode Generator for Receipts
 */
@Composable
fun MockBarcodeDraw(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(48.dp)) {
        val barCount = 42
        val randomWidths = listOf(2f, 4f, 1f, 3f, 5f, 2f)
        var currentX = 0f
        var ind = 0
        while (currentX < size.width) {
            val width = randomWidths[ind % randomWidths.size] * 3f
            val space = randomWidths[(ind + 1) % randomWidths.size] * 2f
            if (ind % 2 == 0) {
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(currentX, 0f),
                    size = Size(width, size.height)
                )
            }
            currentX += width + space
            ind++
        }
    }
}

/**
 * QRIS Generator canvas mockup
 */
@Composable
fun MockQrisDraw(amount: Double, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(190.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw QR layout border corners
            val cellSize = size.width / 16f
            
            // Outer squares typical of QR codes
            fun drawFinder(x: Float, y: Float) {
                drawRect(Color.Black, Offset(x, y), Size(cellSize * 5, cellSize * 5))
                drawRect(Color.White, Offset(x + cellSize, y + cellSize), Size(cellSize * 3, cellSize * 3))
                drawRect(Color.Black, Offset(x + cellSize * 1.5f, y + cellSize * 1.5f), Size(cellSize * 2, cellSize * 2))
            }
            
            drawFinder(0f, 0f)
            drawFinder(size.width - cellSize * 5, 0f)
            drawFinder(0f, size.height - cellSize * 5)
            
            // Fill pseudo random dots inside other pixels
            val rng = Random((amount * 100).toLong())
            for (i in 0..15) {
                for (j in 0..15) {
                    // Skip finder patterns
                    if (i < 5 && j < 5) continue
                    if (i > 10 && j < 5) continue
                    if (i < 5 && j > 10) continue
                    
                    if (rng.nextBoolean()) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(i * cellSize, j * cellSize),
                            size = Size(cellSize, cellSize)
                        )
                    }
                }
            }
            
            // Draw tiny QRIS logo icon badge in middle
            val logoSize = cellSize * 4f
            val logoLeft = size.width / 2f - logoSize / 2f
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(logoLeft, logoLeft),
                size = Size(logoSize, logoSize),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(logoLeft + 3f, logoLeft + 3f),
                size = Size(logoSize - 6f, logoSize - 6f)
            )
        }
        Text(
            text = "QRIS",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            modifier = Modifier
                .background(Color(0xFF0F172A), RoundedCornerShape(2.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

/**
 * Beautiful weekly bar chart representing Rp 1.120k, 2.500k, etc
 */
@Composable
fun WeeklyUsageChart(modifier: Modifier = Modifier) {
    val weekDays = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
    val heightsPercent = listOf(0.45f, 0.32f, 0.81f, 0.65f, 0.95f, 0.38f, 0.18f)
    val weekValuesStr = listOf("1.2M", "850k", "2.1M", "1.7M", "2.5M", "1.0M", "450k")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        weekDays.forEachIndexed { idx, day ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                // Label numerical value above column
                Text(
                    text = weekValuesStr[idx],
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Purple40,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                // Rounded column shape background
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(115.dp)
                        .background(Color(0xFFEDF2F7), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction = heightsPercent[idx])
                            .background(
                                color = if (idx == 4) Color(0xFF0B4C8C) else Color(0xFF1E3A8A).copy(alpha = 0.82f),
                                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                            )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = day,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * High-fidelity animated dynamic trend line chart vectors drawing inside Canvas
 */
@Composable
fun MiniSalesTrendChart(transactions: List<Transaction>, modifier: Modifier = Modifier) {
    val hourlyTotals = remember(transactions) {
        // Accumulate transaction counts/amounts for visual representation
        val buckets = DoubleArray(6) { 0.0 }
        
        // Populate standard sample outputs
        buckets[0] = 1200000.0 // 08:00
        buckets[1] = 2450000.0 // 10:00
        buckets[2] = 4120000.0 // 12:00
        buckets[3] = 1900000.0 // 14:00
        buckets[4] = 3100000.0 // 16:00
        buckets[5] = 4900000.0 // 18:00

        // Blend real values dynamically if we have new sales
        val paidTrxs = transactions.filter { it.status == "PAID" }
        if (paidTrxs.isNotEmpty()) {
            buckets[5] += paidTrxs.take(3).sumOf { it.totalAmount } / 2.0
            buckets[4] += paidTrxs.takeLast(2).sumOf { it.totalAmount } / 3.0
        }
        buckets
    }

    Canvas(modifier = modifier.fillMaxWidth().height(150.dp)) {
        val width = size.width
        val height = size.height
        val maxVal = (hourlyTotals.maxOrNull() ?: 1.0) * 1.15f
        
        val points = mutableListOf<Offset>()
        val stepX = width / (hourlyTotals.size - 1)
        
        hourlyTotals.forEachIndexed { idx, value ->
            val x = idx * stepX
            val y = height - ((value / maxVal) * height).toFloat()
            points.add(Offset(x, y))
        }

        // Horizontal Grid guidelines
        for (i in 1..3) {
            val hY = height * (i / 4f)
            drawLine(
                color = Color.LightGray.copy(alpha = 0.4f),
                start = Offset(0f, hY),
                end = Offset(width, hY),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        }

        // Draw Area Fill gradient underneath curves
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, height)
            points.forEach { points ->
                lineTo(points.x, points.y)
            }
            lineTo(width, height)
            close()
        }
        drawPath(
            path = path,
            color = Color(0xFF0B4C8C).copy(alpha = 0.08f)
        )

        // Draw primary spline trend vectors line
        for (i in 0 until points.size - 1) {
            drawLine(
                color = Color(0xFF0B4C8C),
                start = points[i],
                end = points[i + 1],
                strokeWidth = 5f
            )
        }

        // Draw dots and coordinate bubbles on vertices
        points.forEachIndexed { index, pt ->
            // outer halo bubble
            drawCircle(
                color = Color(0xFF0B4C8C).copy(alpha = 0.25f),
                radius = 12f,
                center = pt
            )
            // inner solid core
            drawCircle(
                color = Color(0xFF0B4C8C),
                radius = 6f,
                center = pt
            )
        }
    }
}

/**
 * Category carousel select tabs
 */
@Composable
fun CategoryTabsRow(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf("Semua", "Makanan", "Minuman", "Lainnya")
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { cat ->
            val isActive = selectedCategory == cat
            Button(
                onClick = { onCategorySelected(cat) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) Color(0xFF0B4C8C) else Color(0xFFEDF2F7),
                    contentColor = if (isActive) Color.White else Color(0xFF4A5568)
                ),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isActive) 2.dp else 0.dp),
                modifier = Modifier.testTag("category_tab_${cat.lowercase()}")
            ) {
                Text(
                    text = cat,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun BusinessLogoView(
    logoUrl: String,
    modifier: Modifier = Modifier
) {
    val isCustomImage = logoUrl.startsWith("content://") || logoUrl.startsWith("file://") || logoUrl.startsWith("/") || logoUrl.contains("/")
    
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFFEFF6FF)),
        contentAlignment = Alignment.Center
    ) {
        if (isCustomImage) {
            var isError by remember { mutableStateOf(false) }
            if (!isError) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = "Logo Usaha",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onError = { isError = true }
                )
            } else {
                DefaultBusinessLogoFallback(logoUrl)
            }
        } else {
            DefaultBusinessLogoFallback(logoUrl)
        }
    }
}

@Composable
private fun DefaultBusinessLogoFallback(logoUrl: String) {
    val (icon, bg, tint) = remember(logoUrl) {
        when {
            logoUrl == "preset_coffee" -> Triple(Icons.Default.Coffee, Color(0xFFFFECE0), Color(0xFFB45309))
            logoUrl == "preset_resto" -> Triple(Icons.Default.Restaurant, Color(0xFFE2F9EC), Color(0xFF10B981))
            logoUrl == "preset_store" -> Triple(Icons.Default.Storefront, Color(0xFFECF3FF), Color(0xFF0B4C8C))
            logoUrl == "preset_bakery" -> Triple(Icons.Default.Cookie, Color(0xFFFFF7E2), Color(0xFFD97706))
            logoUrl == "preset_clothing" -> Triple(Icons.Default.Checkroom, Color(0xFFFFF1FA), Color(0xFFDB2777))
            else -> Triple(Icons.Default.Storefront, Color(0xFFF1F5F9), Color(0xFF475569))
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Logo Toko",
            tint = tint,
            modifier = Modifier.fillMaxSize(0.55f)
        )
    }
}

/**
 * Fallback illustration image generator fallback or custom image loader
 */
@Composable
fun ProductIconFallback(imageUrl: String, category: String, modifier: Modifier = Modifier) {
    val isCustomImage = imageUrl.startsWith("content://") || imageUrl.startsWith("file://") || imageUrl.startsWith("/") || imageUrl.contains("/")
    
    if (isCustomImage) {
        var isError by remember { mutableStateOf(false) }
        if (!isError) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier.fillMaxSize(),
                onError = { isError = true }
            )
        } else {
            DefaultIconFallback(imageUrl, category, modifier)
        }
    } else {
        DefaultIconFallback(imageUrl, category, modifier)
    }
}

@Composable
private fun DefaultIconFallback(imageUrl: String, category: String, modifier: Modifier = Modifier) {
    val (icon, bg) = remember(imageUrl, category) {
        when {
            imageUrl == "coffee_aren" -> Pair(Icons.Default.Coffee, Color(0xFFFFECE0))
            imageUrl == "salad_bowl" -> Pair(Icons.Default.Restaurant, Color(0xFFE2F9EC))
            imageUrl == "pizza_margherita" -> Pair(Icons.Default.LocalPizza, Color(0xFFFFF0F1))
            imageUrl == "donut_glaze" -> Pair(Icons.Default.Cookie, Color(0xFFFFF7E2))
            imageUrl == "fruit_tea" -> Pair(Icons.Default.LocalDrink, Color(0xFFECF3FF))
            imageUrl == "nike_air" -> Pair(Icons.Default.Checkroom, Color(0xFFFFF1FA))
            imageUrl == "sony_headphones" -> Pair(Icons.Default.Headphones, Color(0xFFEDECFD))
            imageUrl == "minimalist_watch" -> Pair(Icons.Default.Watch, Color(0xFFFFFBEB))
            category == "Makanan" -> Pair(Icons.Default.Restaurant, Color(0xFFE2F9EC))
            category == "Minuman" -> Pair(Icons.Default.Coffee, Color(0xFFFFECE0))
            else -> Pair(Icons.Default.Category, Color(0xFFF2F4F7))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Image(
            imageVector = icon,
            contentDescription = null,
            colorFilter = ColorFilter.tint(Color(0xFF2C3E50)),
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Single Grid Item Card displaying catalog items
 */
@Composable
fun ProductGridCard(
    product: Product,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOutOfStock = product.stock <= 0
    val isLowStock = product.stock <= product.minStock && product.stock > 0
    
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clickable(enabled = !isOutOfStock) { onAddToCart() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp)
            ) {
                // Falling back beautifully using material symbols vector graphics matching mockup
                ProductIconFallback(
                    imageUrl = product.imageUrl,
                    category = product.category,
                    modifier = Modifier.fillMaxSize()
                )

                // Stock badges tags
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = when {
                                    isOutOfStock -> Color(0xFFEF4444)
                                    isLowStock -> Color(0xFFF59E0B)
                                    else -> Color(0xFF10B981)
                                },
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when {
                                isOutOfStock -> "Stok Habis"
                                isLowStock -> "Sisa ${product.stock}"
                                else -> "${product.stock} Unit"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Price and Details Labels
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = DarkCharcoal
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = product.category,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val hasDiscount = product.discountPercent > 0.0
                        val finalPrice = if (hasDiscount) product.price * (1.0 - product.discountPercent / 100.0) else product.price
                        val formattedPrice = String.format(Locale("id", "ID"), "Rp %,.0f", finalPrice)
                        
                        Text(
                            text = formattedPrice,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (hasDiscount) Color(0xFFE11D48) else Color(0xFF0B4C8C), // Rose/Red for discounts!
                            fontSize = 13.sp
                        )
                        
                        if (hasDiscount) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = String.format(Locale("id", "ID"), "Rp %,.0f", product.price),
                                    style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = String.format(Locale("id", "ID"), "-%.0f%%", product.discountPercent),
                                    color = Color(0xFFE11D48),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Rounded green '+' add button
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                color = if (isOutOfStock) Color.LightGray else Color(0xFF10B981),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambahkan",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single List Row Item Card displaying catalog items in a horizontal linear style
 */
@Composable
fun ProductListRow(
    product: Product,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOutOfStock = product.stock <= 0
    val isLowStock = product.stock <= product.minStock && product.stock > 0
    
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clickable(enabled = !isOutOfStock) { onAddToCart() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side: Product Image / Icon representation
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF1F5F9))
            ) {
                ProductIconFallback(
                    imageUrl = product.imageUrl,
                    category = product.category,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Middle: Name, Category, Stock level
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = DarkCharcoal
                )
                
                Text(
                    text = product.category,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Fine-tuned Stock status indicator pill
                Box(
                    modifier = Modifier
                        .background(
                            color = when {
                                isOutOfStock -> Color(0xFFFEF2F2)
                                isLowStock -> Color(0xFFFFFBEB)
                                else -> Color(0xFFECFDF5)
                            },
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = when {
                            isOutOfStock -> "Stok Habis"
                            isLowStock -> "Sisa ${product.stock} Unit!"
                            else -> "Tersedia: ${product.stock} Unit"
                        },
                        color = when {
                            isOutOfStock -> Color(0xFFEF4444)
                            isLowStock -> Color(0xFFD97706)
                            else -> Color(0xFF10B981)
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Price & Quick Action button
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                val hasDiscount = product.discountPercent > 0.0
                val finalPrice = if (hasDiscount) product.price * (1.0 - product.discountPercent / 100.0) else product.price
                val formattedPrice = String.format(Locale("id", "ID"), "Rp %,.0f", finalPrice)
                Text(
                    text = formattedPrice,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (hasDiscount) Color(0xFFE11D48) else Color(0xFF0B4C8C),
                    fontSize = 14.sp
                )
                
                if (hasDiscount) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = String.format(Locale("id", "ID"), "Rp %,.0f", product.price),
                            style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                        Text(
                            text = String.format(Locale("id", "ID"), "-%.0f%%", product.discountPercent),
                            color = Color(0xFFE11D48),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Rounded green '+' add button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            color = if (isOutOfStock) Color.LightGray else Color(0xFF10B981),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambahkan",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Custom Floating Shopping Cart drawer sheet
 */
@Composable
fun BottomCartBar(
    cart: List<CartItem>,
    onPayClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (cart.isEmpty()) return

    val totalCount = cart.sumOf { it.quantity }
    val totalPrice = cart.sumOf { it.product.price * it.quantity }
    val formattedPrice = String.format(Locale("id", "ID"), "Rp %,.0f", totalPrice)

    Surface(
        color = Color(0xFF0B4C8C),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        shadowElevation = TokenElevations.LargeElevation,
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "$totalCount Item",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Text(
                    text = formattedPrice,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Button(
                onClick = onPayClicked,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                modifier = Modifier.testTag("submit_checkout_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Bayar",
                        color = Color(0xFF0B4C8C),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = "Bayar",
                        tint = Color(0xFF0B4C8C),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Quick Barcode Scanner Shutter viewport overlay mockup dialog
 */
@Composable
fun MockBarcodeScannerDialog(
    onDismissRequest: () -> Unit,
    onBarcodeDetected: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Barcode Scanner POS",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, "Tutup", tint = Color.LightGray)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Simulated camera viewfinder box
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Running scanning background design lines
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Drawing corner focus brackets mockup
                        val len = 30f
                        val stroke = 6f
                        // Top Left
                        drawLine(Color.Green, Offset(0f, 0f), Offset(len, 0f), strokeWidth = stroke)
                        drawLine(Color.Green, Offset(0f, 0f), Offset(0f, len), strokeWidth = stroke)
                        // Top Right
                        drawLine(Color.Green, Offset(size.width, 0f), Offset(size.width - len, 0f), strokeWidth = stroke)
                        drawLine(Color.Green, Offset(size.width, 0f), Offset(size.width, len), strokeWidth = stroke)
                        // Bottom Left
                        drawLine(Color.Green, Offset(0f, size.height), Offset(len, size.height), strokeWidth = stroke)
                        drawLine(Color.Green, Offset(0f, size.height), Offset(0f, size.height - len), strokeWidth = stroke)
                        // Bottom Right
                        drawLine(Color.Green, Offset(size.width, size.height), Offset(size.width - len, size.height), strokeWidth = stroke)
                        drawLine(Color.Green, Offset(size.width, size.height), Offset(size.width, size.height - len), strokeWidth = stroke)
                    }

                    // A bright glowing animated horizontal laser scanning line across the screen!
                    Divider(
                        color = Color.Red,
                        thickness = 3.dp,
                        modifier = Modifier.fillMaxWidth().align(Alignment.Center)
                    )
                    
                    Text(
                        text = "Arahkan Kamera ke Barcode",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = "Klik jalan pintas di bawah untuk mendeteksi barang otomatis:",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // List of preset fast items barcodes for simulation check
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { onBarcodeDetected("899012345671") }, // Kopi Aren
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C3E50)),
                        modifier = Modifier.testTag("scan_mock_kopi")
                    ) {
                        Text("Kopi Aren", fontSize = 11.sp, color = Color.White)
                    }
                    Button(
                        onClick = { onBarcodeDetected("899012345672") }, // Salad Bowl
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C3E50)),
                        modifier = Modifier.testTag("scan_mock_salad")
                    ) {
                        Text("Salad", fontSize = 11.sp, color = Color.White)
                    }
                    Button(
                        onClick = { onBarcodeDetected("899012345674") }, // Donut Glaze
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C3E50)),
                        modifier = Modifier.testTag("scan_mock_donut")
                    ) {
                        Text("Donat", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Custom Thermal Receipt layout simulation matching Image 3 with perfect scrollable receipt!
 */
@Composable
fun PrintedReceiptDialog(
    invoice: Transaction,
    items: List<TransactionItem>,
    profile: ShopProfile,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Simulasi Cetak Struk",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 15.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Tutup", tint = Color.Gray)
                    }
                }

                Divider(color = Color.LightGray, thickness = 1.dp)
                
                // Receipt Area (Thermal styling paper background)
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(vertical = 12.dp)
                        .background(Color(0xFFFAF9F6), RoundedCornerShape(8.dp)) // warm ivory receipt background
                        .border(1.dp, Color.LightGray.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Business Logo
                        if (profile.logoUrl.isNotEmpty()) {
                            BusinessLogoView(
                                logoUrl = profile.logoUrl,
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .size(48.dp)
                            )
                        }

                        // Shop Name heading
                        Text(
                            text = profile.shopName.uppercase(),
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black
                        )
                        Text(
                            text = profile.address,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "Telp: ${profile.phone}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.DarkGray
                        )
                        
                        Text(
                            text = "-".repeat(34),
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        // Invoice metadata
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "No: ${invoice.invoiceNo}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Black
                            )
                            Text(
                                text = "Tanggal: ${invoice.getFormattedDate()}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Black
                            )
                            Text(
                                text = "Kasir : ${invoice.cashierName}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Black
                            )
                        }

                        Text(
                            text = "-".repeat(34),
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        // Cart items list lines
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items.forEach { item ->
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = item.productName,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val qtyPrice = String.format(Locale("id", "ID"), "%d x Rp %,.0f", item.quantity, item.price)
                                        val itemTotal = String.format(Locale("id", "ID"), "Rp %,.0f", item.subtotal)
                                        Text(
                                            text = qtyPrice,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.DarkGray
                                        )
                                        Text(
                                            text = itemTotal,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "-".repeat(34),
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        // Calculations subtotal tax etc
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val sub = items.sumOf { it.subtotal }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("SUBTOTAL :", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.DarkGray)
                                Text(String.format(Locale("id", "ID"), "Rp %,.0f", sub), fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("PAJAK (10%) :", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.DarkGray)
                                Text(String.format(Locale("id", "ID"), "Rp %,.0f", invoice.taxProcessed), fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("SERVICE :", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.DarkGray)
                                Text(String.format(Locale("id", "ID"), "Rp %,.0f", invoice.serviceChargeProcessed), fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("TOTAL :", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text(String.format(Locale("id", "ID"), "Rp %,.0f", invoice.totalAmount), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }

                        Text(
                            text = "-".repeat(34),
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        // Payment Details line
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("BAYAR via ${invoice.paymentMethod.uppercase()}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text("LUNAS", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // Footer Greeting
                        Text(
                            text = "TERIMA KASIH ATAS KUNJUNGAN ANDA",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "Aplikasi Kasir POS oleh QuickPOS",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Fake barcode simulation at footer
                        MockBarcodeDraw(modifier = Modifier.width(180.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action printed button
                Button(
                    onClick = {
                        val pdfFile = com.example.util.DocumentExporter.exportReceiptToPdf(context, invoice, items, profile)
                        if (pdfFile != null) {
                            com.example.util.DocumentExporter.printPdfFile(context, pdfFile, "Struk_${invoice.invoiceNo}")
                        } else {
                            Toast.makeText(context, "Gagal membuat dokumen PDF struk", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B4C8C)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("print_action_button")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Print, "Cetak")
                        Text("Cetak Struk (PDF / Print)", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Secondary Action Row: Buka PDF and Bagikan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val pdfFile = com.example.util.DocumentExporter.exportReceiptToPdf(context, invoice, items, profile)
                            if (pdfFile != null) {
                                com.example.util.DocumentExporter.openFile(context, pdfFile, "application/pdf")
                            } else {
                                Toast.makeText(context, "Gagal membuka dokumen PDF", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Launch, "Buka", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buka PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val pdfFile = com.example.util.DocumentExporter.exportReceiptToPdf(context, invoice, items, profile)
                            if (pdfFile != null) {
                                com.example.util.DocumentExporter.shareFile(context, pdfFile, "application/pdf")
                            } else {
                                Toast.makeText(context, "Gagal membagikan dokumen PDF", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Share, "Bagikan", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bagikan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Double layer elevation tokens parameters
 */
object TokenElevations {
    val DefaultElevation = 4.dp
    val LargeElevation = 10.dp
}

/**
 * Interactive Dialog displayed upon successful export to PDF & Excel/CSV,
 * allowing instant opening and sharing of generated business records.
 */
@Composable
fun ExportSuccessDialog(
    pdfFile: File?,
    excelFile: File?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFFECFDF5), RoundedCornerShape(100.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Laporan Siap Diunduh",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "File Excel & PDF laporan usaha berhasil diekspor. Pilih opsi di bawah untuk membuka atau membagikan dokumen.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Section 1: PDF Document View & Share
                if (pdfFile != null) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = "PDF File",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Dokumen PDF Laporan",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.Black
                                        )
                                        Text(
                                            text = pdfFile.name.take(24) + "...",
                                            fontSize = 9.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { com.example.util.DocumentExporter.openFile(context, pdfFile, "application/pdf") },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Launch, "Buka", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Buka PDF", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { com.example.util.DocumentExporter.shareFile(context, pdfFile, "application/pdf") },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Share, "Bagikan", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Bagikan", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Section 2: Excel File View & Share
                if (excelFile != null) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = "Excel File",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Spreadsheet Excel (CSV)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.Black
                                        )
                                        Text(
                                            text = excelFile.name.take(24) + "...",
                                            fontSize = 9.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { com.example.util.DocumentExporter.openFile(context, excelFile, "text/comma-separated-values") },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Launch, "Buka", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Buka Excel", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { com.example.util.DocumentExporter.shareFile(context, excelFile, "text/comma-separated-values") },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Share, "Bagikan", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Bagikan", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Dismiss button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tutup", fontWeight = FontWeight.Bold, color = Color.Gray)
                }
            }
        }
    }
}
