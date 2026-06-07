package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.ShopProfile
import com.example.data.Transaction
import com.example.data.TransactionItem
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DocumentExporter {

    fun exportReportToPdf(
        context: Context,
        profile: ShopProfile,
        transactions: List<Transaction>,
        reportTitle: String
    ): File? {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 standard size
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas
            
            val paint = Paint()
            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = 12f
                isAntiAlias = true
            }
            
            val titlePaint = Paint().apply {
                color = Color.rgb(26, 82, 118) // Deep Blue Navy
                textSize = 18f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 10f
                isAntiAlias = true
            }

            val headerPaint = Paint().apply {
                color = Color.rgb(240, 243, 244)
                isAntiAlias = true
            }

            val tableHeaderPaint = Paint().apply {
                color = Color.rgb(44, 62, 80)
                textSize = 10f
                isFakeBoldText = true
                isAntiAlias = true
            }

            // Draw header
            canvas.drawRect(0f, 0f, 595f, 100f, Paint().apply { color = Color.rgb(235, 245, 251) })
            canvas.drawText(profile.shopName, 30f, 40f, titlePaint)
            canvas.drawText("${profile.address} | Telp: ${profile.phone}", 30f, 60f, subtitlePaint)
            canvas.drawText("Laporan POS Real-time - $reportTitle", 30f, 80f, Paint().apply {
                color = Color.rgb(33, 150, 243)
                textSize = 13f
                isFakeBoldText = true
                isAntiAlias = true
            })
            
            // Draw metadata info
            val dateStr = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")).format(Date())
            canvas.drawText("Dicetak pada: $dateStr", 30f, 130f, subtitlePaint)
            canvas.drawText("Total Transaksi: ${transactions.size}", 30f, 147f, textPaint)
            
            val totalSales = transactions.filter { it.status == "PAID" }.sumOf { it.totalAmount }
            val formattedSales = String.format(Locale("id", "ID"), "Rp %,.0f", totalSales)
            canvas.drawText("Total Pendapatan: $formattedSales", 300f, 147f, Paint().apply {
                color = Color.rgb(46, 125, 50)
                textSize = 12f
                isFakeBoldText = true
                isAntiAlias = true
            })

            // Draw Table list
            var yPosition = 190f
            
            // Table columns headers background
            canvas.drawRect(30f, yPosition, 565f, yPosition + 25f, Paint().apply { color = Color.rgb(44, 62, 80) })
            
            // Text color for table heading is changed to WHITE for legibility
            tableHeaderPaint.color = Color.WHITE
            canvas.drawText("Inisial Invoice", 40f, yPosition + 17f, tableHeaderPaint)
            canvas.drawText("Waktu", 160f, yPosition + 17f, tableHeaderPaint)
            canvas.drawText("Staff Kasir", 290f, yPosition + 17f, tableHeaderPaint)
            canvas.drawText("Metode", 410f, yPosition + 17f, tableHeaderPaint)
            canvas.drawText("Total (IDR)", 480f, yPosition + 17f, tableHeaderPaint)
            
            yPosition += 25f
            
            textPaint.textSize = 10f
            transactions.forEachIndexed { idx, trx ->
                if (yPosition > 800f) {
                    // Quick stop list if overflow A4 page height
                    return@forEachIndexed
                }
                
                // Zebra styling background
                if (idx % 2 == 0) {
                    canvas.drawRect(30f, yPosition, 565f, yPosition + 20f, headerPaint)
                }
                
                val statusPaint = Paint().apply {
                    color = if (trx.status == "PAID") Color.rgb(46, 125, 50) else Color.RED
                    textSize = 10f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                
                canvas.drawText(trx.invoiceNo, 40f, yPosition + 14f, textPaint)
                canvas.drawText(trx.getFormattedDate("dd/MM/yy HH:mm"), 160f, yPosition + 14f, textPaint)
                canvas.drawText(trx.cashierName.take(18), 290f, yPosition + 14f, textPaint)
                canvas.drawText(trx.paymentMethod, 410f, yPosition + 14f, textPaint)
                
                val priceVal = String.format(Locale("id", "ID"), "Rp %,.0f", trx.totalAmount)
                canvas.drawText(priceVal, 480f, yPosition + 14f, if (trx.status == "PAID") textPaint else statusPaint)
                
                yPosition += 20f
            }

            // Draw Footer
            canvas.drawRect(30f, 810f, 565f, 812f, Paint().apply { color = Color.LTGRAY })
            canvas.drawText("QuickPOS System - Laporan Elektronik untuk Pengarsipan Legal", 30f, 825f, subtitlePaint)
            
            pdfDocument.finishPage(page)
            
            // Save file in app's external files directory
            val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (directory != null && !directory.exists()) {
                directory.mkdirs()
            }
            
            val filename = "LaporanPOS_${reportTitle.replace(" ", "")}_${System.currentTimeMillis()}.pdf"
            val file = File(directory, filename)
            val fileOutputStream = FileOutputStream(file)
            pdfDocument.writeTo(fileOutputStream)
            pdfDocument.close()
            fileOutputStream.close()
            
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun exportReportToExcel(
        context: Context,
        profile: ShopProfile,
        transactions: List<Transaction>,
        reportTitle: String
    ): File? {
        try {
            val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (directory != null && !directory.exists()) {
                directory.mkdirs()
            }
            
            val filename = "LaporanPOS_${reportTitle.replace(" ", "")}_${System.currentTimeMillis()}.csv"
            val file = File(directory, filename)
            val writer = FileWriter(file)
            
            // Excel-compatible Tabular data format with UTF-8 BOM
            writer.write("\uFEFF") // UTF-8 BOM to ensure Indonesian characters are loaded correctly
            writer.write("LAPORAN POS SALES REPORT\n")
            writer.write("Toko;${profile.shopName}\n")
            writer.write("Alamat;${profile.address}\n")
            writer.write("Telepon;${profile.phone}\n")
            writer.write("Periode;${reportTitle}\n")
            writer.write("Tanggal Cetak;${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US).format(Date())}\n\n")
            
            // Table Heading
            writer.write("No;Invoice ID;Waktu Transaksi;Nama Kasir;Metode Pembayaran;Status;Pajak (IDR);Service (IDR);Total Penjualan (IDR)\n")
            
            transactions.forEachIndexed { index, trx ->
                val line = String.format(
                    Locale("id", "ID"),
                    "%d;%s;%s;%s;%s;%s;%.0f;%.0f;%.0f\n",
                    index + 1,
                    trx.invoiceNo,
                    trx.getFormattedDate("yyyy-MM-dd HH:mm"),
                    trx.cashierName,
                    trx.paymentMethod,
                    trx.status,
                    trx.taxProcessed,
                    trx.serviceChargeProcessed,
                    trx.totalAmount
                )
                writer.write(line)
            }
            
            // Total Row
            val totalSales = transactions.filter { it.status == "PAID" }.sumOf { it.totalAmount }
            writer.write("\n;;;;;;TOTAL APPROVED SALES;;${String.format(Locale("id", "ID"), "%.0f", totalSales)}\n")
            
            writer.flush()
            writer.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun openFile(context: Context, file: File, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Tidak ada aplikasi untuk membuka file ini: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Bagikan Laporan")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Tidak dapat membagikan file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportReceiptToPdf(
        context: Context,
        invoice: Transaction,
        items: List<TransactionItem>,
        profile: ShopProfile
    ): File? {
        try {
            val pdfDocument = PdfDocument()
            
            // Calculate a dynamic height based on the number of items
            val calculatedHeight = 280 + (items.size * 28) + 180
            val pageInfo = PdfDocument.PageInfo.Builder(300, calculatedHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas
            
            val paint = Paint().apply {
                color = Color.BLACK
                isAntiAlias = true
            }
            
            val monoPaintText = Paint().apply {
                color = Color.BLACK
                textSize = 9f
                typeface = android.graphics.Typeface.MONOSPACE
                isAntiAlias = true
            }

            val monoPaintBold = Paint().apply {
                color = Color.BLACK
                textSize = 10f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                isAntiAlias = true
            }

            val monoTitlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 12f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            val centerMonoPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 8f
                typeface = android.graphics.Typeface.MONOSPACE
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            // Draw header
            var y = 30f
            canvas.drawText(profile.shopName.uppercase(), 150f, y, monoTitlePaint)
            y += 15f
            canvas.drawText(profile.address, 150f, y, centerMonoPaint)
            y += 12f
            canvas.drawText("Telp: ${profile.phone}", 150f, y, centerMonoPaint)
            y += 15f
            
            // Divider line
            canvas.drawText("-".repeat(34), 150f, y, centerMonoPaint)
            y += 12f

            // Invoice details
            canvas.drawText("No: ${invoice.invoiceNo}", 20f, y, monoPaintText)
            y += 12f
            canvas.drawText("Tanggal: ${invoice.getFormattedDate()}", 20f, y, monoPaintText)
            y += 12f
            canvas.drawText("Kasir : ${invoice.cashierName}", 20f, y, monoPaintText)
            y += 15f

            // Divider line
            canvas.drawText("-".repeat(34), 150f, y, centerMonoPaint)
            y += 15f

            // Items list lines
            items.forEach { item ->
                canvas.drawText(item.productName, 20f, y, monoPaintBold)
                y += 12f
                val qtyPrice = String.format(Locale("id", "ID"), "%d x Rp %,.0f", item.quantity, item.price)
                val itemTotal = String.format(Locale("id", "ID"), "Rp %,.0f", item.subtotal)
                canvas.drawText(qtyPrice, 20f, y, monoPaintText)
                canvas.drawText(itemTotal, 280f - monoPaintText.measureText(itemTotal), y, monoPaintText)
                y += 16f
            }

            // Divider line
            canvas.drawText("-".repeat(34), 150f, y, centerMonoPaint)
            y += 15f

            // Calculation fields
            val sub = items.sumOf { it.subtotal }
            val priceStr = { label: String, amount: Double ->
                canvas.drawText(label, 20f, y, monoPaintText)
                val totalStr = String.format(Locale("id", "ID"), "Rp %,.0f", amount)
                canvas.drawText(totalStr, 280f - monoPaintText.measureText(totalStr), y, monoPaintText)
                y += 12f
            }

            priceStr("SUBTOTAL :", sub)
            priceStr("PAJAK (10%) :", invoice.taxProcessed)
            priceStr("SERVICE :", invoice.serviceChargeProcessed)
            
            y += 4f
            canvas.drawText("TOTAL :", 20f, y, monoPaintBold)
            val totalText = String.format(Locale("id", "ID"), "Rp %,.0f", invoice.totalAmount)
            canvas.drawText(totalText, 280f - monoPaintBold.measureText(totalText), y, monoPaintBold)
            y += 15f

            // Divider
            canvas.drawText("-".repeat(34), 150f, y, centerMonoPaint)
            y += 15f

            // Payment method
            canvas.drawText("BAYAR via ${invoice.paymentMethod.uppercase()}", 20f, y, monoPaintBold)
            canvas.drawText("LUNAS", 280f - monoPaintBold.measureText("LUNAS"), y, Paint().apply {
                color = Color.rgb(46, 125, 50)
                textSize = 10f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                isAntiAlias = true
            })
            y += 20f

            // Footer Greeting
            canvas.drawText("TERIMA KASIH ATAS KUNJUNGAN ANDA", 150f, y, centerMonoPaint)
            y += 10f
            canvas.drawText("Aplikasi Kasir POS oleh QuickPOS", 150f, y, centerMonoPaint)
            
            // Draw simulated barcode
            y += 15f
            val barcodePaint = Paint().apply {
                color = Color.BLACK
                strokeWidth = 2f
            }
            var startX = 60f
            for (i in 0..40) {
                val lineW = if (i % 3 == 0) 3f else if (i % 4 == 0) 1f else 1.5f
                barcodePaint.strokeWidth = lineW
                canvas.drawLine(startX, y, startX, y + 15f, barcodePaint)
                startX += lineW + 1.5f
            }

            pdfDocument.finishPage(page)

            // Save PDF
            val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (directory != null && !directory.exists()) {
                directory.mkdirs()
            }
            val filename = "Struk_${invoice.invoiceNo}.pdf"
            val file = File(directory, filename)
            val fileOutputStream = FileOutputStream(file)
            pdfDocument.writeTo(fileOutputStream)
            pdfDocument.close()
            fileOutputStream.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun printPdfFile(context: Context, file: File, jobName: String) {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val adapter = PdfPrintAdapter(file.absolutePath)
            printManager.print(jobName, adapter, PrintAttributes.Builder().build())
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal memicu cetak: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyImageToInternalStorage(context: Context, uri: android.net.Uri): String? {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val imagesDir = File(context.filesDir, "product_images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }
            val filename = "prod_img_${System.currentTimeMillis()}.jpg"
            val file = File(imagesDir, filename)
            val outputStream = FileOutputStream(file)
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            inputStream.close()
            outputStream.close()
            return file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}

class PdfPrintAdapter(private val filePath: String) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }
        val pdi = PrintDocumentInfo.Builder("Struk_Belanja.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .build()
        callback?.onLayoutFinished(pdi, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        var input: FileInputStream? = null
        var output: FileOutputStream? = null
        try {
            input = FileInputStream(filePath)
            output = FileOutputStream(destination?.fileDescriptor)
            val buf = ByteArray(16384)
            var bytesRead: Int
            while (input.read(buf).also { bytesRead = it } >= 0) {
                output.write(buf, 0, bytesRead)
            }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback?.onWriteFailed(e.toString())
        } finally {
            try {
                input?.close()
                output?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
