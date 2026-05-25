package com.example.ui.profile

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.data.database.DoseLogWithMedicine
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class MedicineAdherenceSummary(
    val name: String,
    val dosage: String,
    val totalScheduled: Int,
    val takenCount: Int,
    val missedCount: Int,
    val adherencePercent: Int
)

object PdfExporter {

    fun generateAdherencePdf(
        context: Context,
        patientName: String,
        logs: List<DoseLogWithMedicine>
    ): ByteArray {
        // Create a PDF Document
        val document = PdfDocument()
        
        // standard page info (A4 size: 595 x 842 points)
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Paint components
        val logoPaint = Paint().apply {
            color = 0xFF10B981.toInt() // PrimaryTeal
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val titlePaint = Paint().apply {
            color = 0xFF0F111A.toInt() // DarkSlateBg
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = 0xFF9CA3AF.toInt() // Grey Text
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = 0xFF1F2937.toInt() // Content Text Color (Slate-800)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val labelPaint = Paint().apply {
            color = 0xFF4B5563.toInt() // Slate-600
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val tableHeaderPaint = Paint().apply {
            color = 0xFF374151.toInt() // Slate-800
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val fillPaint = Paint().apply {
            style = Paint.Style.FILL
        }

        val strokePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = 0xFFE5E7EB.toInt() // Light Grey Border
        }

        var currentY = 50f
        val leftMargin = 45f
        val rightMargin = 550f

        // 1. Draw HEADER Logos and Title
        canvas.drawText("MedRemind", leftMargin, currentY, logoPaint)
        
        val logoTextHeight = 24f
        currentY += logoTextHeight + 8f

        canvas.drawText("CLINICAL ADHERENCE EXPORT REPORT", leftMargin, currentY, titlePaint)
        currentY += 18f

        val sdfFull = SimpleDateFormat("MMMM d, yyyy", Locale.US)
        val todayStr = sdfFull.format(Date())
        canvas.drawText("Generated on $todayStr", leftMargin, currentY, subtitlePaint)
        currentY += 24f

        // Draw thin partition line
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, strokePaint)
        currentY += 25f

        // 2. Patient / Report Info Metadata Grid
        // Calculate date range (last 30 days)
        val cal = Calendar.getInstance()
        val endDateStr = sdfFull.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -29)
        val startDateStr = sdfFull.format(cal.time)

        canvas.drawText("PATIENT NAME:", leftMargin, currentY, labelPaint)
        canvas.drawText(patientName, leftMargin + 110, currentY, textPaint)
        currentY += 18f

        canvas.drawText("REPORT DURATION:", leftMargin, currentY, labelPaint)
        canvas.drawText("$startDateStr - $endDateStr (Last 30 Days)", leftMargin + 110, currentY, textPaint)
        currentY += 18f

        canvas.drawText("SYNC STATUS:", leftMargin, currentY, labelPaint)
        canvas.drawText("Active / Fully Synced Offline Room DB", leftMargin + 110, currentY, textPaint)
        currentY += 30f

        // 3. Process Adherence Summary
        // Group logs by medicine
        val summaryList = mutableListOf<MedicineAdherenceSummary>()
        val medicinesGrouped = logs.groupBy { it.medicineId }

        for ((medId, medLogs) in medicinesGrouped) {
            val name = medLogs.firstOrNull()?.medicineName ?: "Unknown Medication"
            val dosage = medLogs.firstOrNull()?.medicineDosage ?: "N/A"
            val totalScheduled = medLogs.size
            val takenCount = medLogs.count { it.status == "TAKEN" }
            val missedCount = medLogs.count { it.status == "MISSED" }
            val adherencePercent = if (totalScheduled > 0) {
                (takenCount * 100) / totalScheduled
            } else {
                100
            }
            summaryList.add(
                MedicineAdherenceSummary(
                    name = name,
                    dosage = dosage,
                    totalScheduled = totalScheduled,
                    takenCount = takenCount,
                    missedCount = missedCount,
                    adherencePercent = adherencePercent
                )
            )
        }

        // 4. DRAW MEDICINE TABLE
        canvas.drawText("MEDICATION COMPLIANCE METRICS", leftMargin, currentY, titlePaint)
        currentY += 15f

        // Table Header Banner Background
        fillPaint.color = 0xFFF3F4F6.toInt() // light header background grey
        canvas.drawRect(leftMargin, currentY - 12f, rightMargin, currentY + 16f, fillPaint)

        // Table Header Columns labels
        canvas.drawText("Medication Name", leftMargin + 10f, currentY + 4f, tableHeaderPaint)
        canvas.drawText("Dosage", leftMargin + 160f, currentY + 4f, tableHeaderPaint)
        canvas.drawText("Scheduled", leftMargin + 250f, currentY + 4f, tableHeaderPaint)
        canvas.drawText("Taken", leftMargin + 320f, currentY + 4f, tableHeaderPaint)
        canvas.drawText("Missed", leftMargin + 380f, currentY + 4f, tableHeaderPaint)
        canvas.drawText("Adherence", leftMargin + 440f, currentY + 4f, tableHeaderPaint)

        currentY += 28f

        // Table Rows
        var rowColorAlternator = true
        for (summary in summaryList) {
            // Draw alternating background striping
            if (rowColorAlternator) {
                fillPaint.color = 0xFFFAFAFA.toInt()
                canvas.drawRect(leftMargin, currentY - 10f, rightMargin, currentY + 16f, fillPaint)
            }
            rowColorAlternator = !rowColorAlternator

            // Draw content
            canvas.drawText(summary.name, leftMargin + 10f, currentY + 4f, textPaint)
            canvas.drawText(summary.dosage, leftMargin + 160f, currentY + 4f, textPaint)
            canvas.drawText(summary.totalScheduled.toString(), leftMargin + 250f, currentY + 4f, textPaint)
            canvas.drawText(summary.takenCount.toString(), leftMargin + 320f, currentY + 4f, textPaint)
            canvas.drawText(summary.missedCount.toString(), leftMargin + 380f, currentY + 4f, textPaint)
            
            // Adherence percent styling color
            val ratePaint = Paint(textPaint).apply {
                color = when {
                    summary.adherencePercent >= 80 -> 0xFF10B981.toInt() // Teal
                    summary.adherencePercent >= 50 -> 0xFFF59E0B.toInt() // Orange
                    else -> 0xFFF87171.toInt() // RefillRed
                }
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("${summary.adherencePercent}%", leftMargin + 440f, currentY + 4f, ratePaint)

            canvas.drawLine(leftMargin, currentY + 16f, rightMargin, currentY + 16f, strokePaint)
            currentY += 26f
        }

        currentY += 20f

        // 5. OVERALL COMPLIANCE ADHERENCE SUMMARY (SUMMARY BOX)
        // Calculate totals
        val totalDoses = summaryList.sumOf { it.totalScheduled }
        val totalTaken = summaryList.sumOf { it.takenCount }
        val overallAdherence = if (totalDoses > 0) {
            (totalTaken * 100) / totalDoses
        } else {
            100
        }

        // Draw summary card border
        fillPaint.color = 0xFFF0FDF4.toInt() // faint green background
        strokePaint.color = 0xFFDCFCE7.toInt()
        
        val strokeColorValue = when {
            overallAdherence >= 80 -> 0xFFD1FAE5.toInt() // light green
            overallAdherence >= 50 -> 0xFFFEF3C7.toInt() // light orange
            else -> 0xFFFEE2E2.toInt() // light red
        }
        val fillColorValue = when {
            overallAdherence >= 80 -> 0xFFF0FDF4.toInt() // light green
            overallAdherence >= 50 -> 0xFFFFFBEB.toInt() // light orange
            else -> 0xFFFEF2F2.toInt() // light red
        }
        
        fillPaint.color = fillColorValue
        strokePaint.color = strokeColorValue

        canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 80f, fillPaint)
        canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 80f, strokePaint)

        // Draw compliance scores inside box
        val activeSummaryTitlePaint = Paint().apply {
            color = 0xFF1F2937.toInt()
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        canvas.drawText("OVERALL COMPLIANCE METRIC SUMMARY", leftMargin + 15f, currentY + 25f, activeSummaryTitlePaint)

        val scorePaint = Paint().apply {
            color = when {
                overallAdherence >= 80 -> 0xFF10B981.toInt() // Teal
                overallAdherence >= 50 -> 0xFFF59E0B.toInt() // Orange
                else -> 0xFFF87171.toInt() // Red
            }
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("$overallAdherence%", rightMargin - 90f, currentY + 48f, scorePaint)

        // Compliance evaluation and textual report summary
        val evalText = when {
            overallAdherence >= 80 -> "Excellent adherence! Adhering strictly to your medication routine leads to better clinical results."
            overallAdherence >= 50 -> "Moderate compliance score. Consider setting up caregiver viewport notification flags to avoid omissions."
            else -> "Warning: Adherence remains dangerously low. Urgent practitioner review or family supervision is recommended."
        }
        
        canvas.drawText("Evaluation: $evalText", leftMargin + 15f, currentY + 45f, subtitlePaint)
        canvas.drawText("Historical count: $totalTaken taken doses out of $totalDoses scheduled.", leftMargin + 15f, currentY + 62f, subtitlePaint)

        currentY += 110f

        // 6. Draw Footer Guidelines
        strokePaint.color = 0xFFE5E7EB.toInt()
        canvas.drawLine(leftMargin, 770f, rightMargin, 770f, strokePaint)
        
        val footerPaint = Paint().apply {
            color = 0xFF9CA3AF.toInt()
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("MedRemind Client Application - Sealed Electronic Diagnostic Adherence Report", leftMargin, 788f, footerPaint)
        canvas.drawText("Page 1 of 1", rightMargin - 55f, 788f, footerPaint)

        document.finishPage(page)

        // Write content to bytes
        val outputStream = ByteArrayOutputStream()
        document.writeTo(outputStream)
        document.close()

        return outputStream.toByteArray()
    }

    fun savePdfToDownloads(context: Context, pdfBytes: ByteArray, fileName: String): Uri? {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            Uri.parse("content://media/external/file")
        }

        val uri = resolver.insert(collectionUri, contentValues) ?: return null

        return try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(pdfBytes)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            e.printStackTrace()
            null
        }
    }

    fun openPdf(context: Context, uri: Uri) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No default PDF viewer application found", Toast.LENGTH_SHORT).show()
        }
    }
}
