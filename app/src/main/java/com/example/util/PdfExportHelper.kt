package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.data.model.OperationWeeklySummary
import com.example.data.model.ReportEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportHelper {

  private const val PAGE_WIDTH = 595 // Standard A4 width in points
  private const val PAGE_HEIGHT = 842 // Standard A4 height in points

  /**
   * Generates a comprehensive, illustrated PDF for an individual field report.
   * Includes all metadata, field metrics, input details, remarks, and attached photo evidence.
   */
  fun exportReportToPdf(context: Context, report: ReportEntity): File {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas

    // Background color: Clean crisp white page for printing with Dark Green & Dark Blue branding
    canvas.drawColor(Color.WHITE)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 1. HEADER BANNER (Dark Green & Dark Blue Gradient style)
    val headerRect = RectF(24f, 24f, PAGE_WIDTH - 24f, 96f)
    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(13, 35, 24) // Deep Dark Green (#0D2318)
    canvas.drawRoundRect(headerRect, 10f, 10f, paint)

    // Subtle dark blue decorative strip at bottom of header
    paint.color = Color.rgb(13, 30, 51) // Deep Dark Blue (#0D1E33)
    canvas.drawRoundRect(RectF(24f, 88f, PAGE_WIDTH - 24f, 96f), 0f, 0f, paint)

    // App Branding & Title
    paint.color = Color.WHITE
    paint.textSize = 17f
    paint.isFakeBoldText = true
    canvas.drawText("UPENJAnet SUGARCANE FIELD OPERATION REPORT", 40f, 56f, paint)

    paint.color = Color.rgb(180, 220, 195) // Light mint green
    paint.textSize = 9.5f
    paint.isFakeBoldText = false
    canvas.drawText("Cloud-Synchronized Plantation Records Management • Verified Operational Copy", 40f, 74f, paint)

    // 2. AUDIT & METADATA SECTION
    val metaRect = RectF(24f, 108f, PAGE_WIDTH - 24f, 176f)
    paint.color = Color.rgb(244, 248, 245)
    canvas.drawRoundRect(metaRect, 8f, 8f, paint)

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1f
    paint.color = Color.rgb(210, 228, 216)
    canvas.drawRoundRect(metaRect, 8f, 8f, paint)

    paint.style = Paint.Style.FILL
    paint.textSize = 10f

    // Column 1 (Left)
    paint.isFakeBoldText = true
    paint.color = Color.rgb(13, 35, 24)
    canvas.drawText("Report ID:", 40f, 128f, paint)
    canvas.drawText("Plantation Zone:", 40f, 146f, paint)
    canvas.drawText("Date of Operation:", 40f, 164f, paint)

    paint.isFakeBoldText = false
    paint.color = Color.rgb(30, 45, 35)
    canvas.drawText(report.id, 145f, 128f, paint)
    canvas.drawText(report.zone, 145f, 146f, paint)
    canvas.drawText(report.dateOfOperation, 145f, 164f, paint)

    // Column 2 (Right)
    paint.isFakeBoldText = true
    paint.color = Color.rgb(13, 35, 24)
    canvas.drawText("Status:", 320f, 128f, paint)
    canvas.drawText("Created By:", 320f, 146f, paint)
    canvas.drawText("Last Synchronized:", 320f, 164f, paint)

    paint.isFakeBoldText = false
    paint.color = Color.rgb(30, 45, 35)
    canvas.drawText(report.status.name, 430f, 128f, paint)
    canvas.drawText("${report.createdBy} (${report.createdByRole.name})", 430f, 146f, paint)
    val syncText = if (report.synced) "Synced with Cloud" else "Local Copy"
    canvas.drawText(syncText, 430f, 164f, paint)

    // 3. FIELD OPERATIONS TABLE
    val tableTop = 188f
    val tableBottom = 356f
    val tableRect = RectF(24f, tableTop, PAGE_WIDTH - 24f, tableBottom)
    paint.color = Color.rgb(255, 255, 255)
    canvas.drawRoundRect(tableRect, 8f, 8f, paint)

    paint.style = Paint.Style.STROKE
    paint.color = Color.rgb(200, 220, 208)
    canvas.drawRoundRect(tableRect, 8f, 8f, paint)
    paint.style = Paint.Style.FILL

    // Section Header in Table
    val tableHeaderRect = RectF(24f, tableTop, PAGE_WIDTH - 24f, tableTop + 24f)
    paint.color = Color.rgb(13, 30, 51) // Dark Blue header
    canvas.drawRoundRect(tableHeaderRect, 8f, 8f, paint)
    paint.color = Color.WHITE
    paint.textSize = 10.5f
    paint.isFakeBoldText = true
    canvas.drawText("OPERATIONAL METRICS & FIELD ALLOCATION", 36f, tableTop + 16f, paint)

    // Grid details
    var yPos = tableTop + 44f
    fun drawTableRow(label1: String, val1: String, label2: String, val2: String, highlightVal1: Boolean = false) {
      paint.isFakeBoldText = true
      paint.textSize = 10f
      paint.color = Color.rgb(60, 75, 65)
      canvas.drawText(label1, 36f, yPos, paint)

      if (highlightVal1) {
        paint.color = Color.rgb(18, 90, 42) // Dark green highlight
        paint.isFakeBoldText = true
        paint.textSize = 11f
      } else {
        paint.color = Color.rgb(20, 25, 22)
        paint.isFakeBoldText = false
        paint.textSize = 10f
      }
      canvas.drawText(val1, 160f, yPos, paint)

      paint.isFakeBoldText = true
      paint.textSize = 10f
      paint.color = Color.rgb(60, 75, 65)
      canvas.drawText(label2, 320f, yPos, paint)

      paint.isFakeBoldText = false
      paint.color = Color.rgb(20, 25, 22)
      canvas.drawText(val2, 440f, yPos, paint)

      // Light separator line
      paint.color = Color.rgb(235, 242, 238)
      canvas.drawLine(36f, yPos + 6f, PAGE_WIDTH - 36f, yPos + 6f, paint)
      yPos += 22f
    }

    drawTableRow("Operation Name:", report.operationName, "Block Number:", report.blockNumber)
    drawTableRow("Contractor / Sup:", report.contractorName, "Labourers Engaged:", "${report.noOfLabourers} Workers")
    drawTableRow("Area Covered (Acres):", "${report.areaCoveredHa} Ac", "Balance to Do (Acres):", "${report.balanceToBeDoneHa} Ac", highlightVal1 = true)
    drawTableRow("Agricultural Input:", report.inputName.ifBlank { "None Specified" }, "Ratoon Cutting Date:", report.ratoonDate.ifBlank { "N/A" })

    // Remark line
    paint.isFakeBoldText = true
    paint.textSize = 10f
    paint.color = Color.rgb(60, 75, 65)
    canvas.drawText("Field Comments / Remarks:", 36f, yPos, paint)
    paint.isFakeBoldText = false
    paint.color = Color.rgb(20, 25, 22)
    val remarkText = report.remark.ifBlank { "No additional comments or obstacles recorded." }
    canvas.drawText(remarkText.take(65), 180f, yPos, paint)

    // 3B. GRAPHICAL AREA ILLUSTRATION & COMPLETION DIAGRAM
    val diagramTop = 328f
    val diagramBottom = 386f
    val diagramRect = RectF(24f, diagramTop, PAGE_WIDTH - 24f, diagramBottom)
    paint.color = Color.rgb(243, 248, 245)
    canvas.drawRoundRect(diagramRect, 6f, 6f, paint)

    paint.style = Paint.Style.STROKE
    paint.color = Color.rgb(205, 225, 212)
    canvas.drawRoundRect(diagramRect, 6f, 6f, paint)
    paint.style = Paint.Style.FILL

    val totalBlockAcres = report.areaCoveredHa + report.balanceToBeDoneHa
    val completionPct = if (totalBlockAcres > 0) ((report.areaCoveredHa / totalBlockAcres) * 100).toInt() else 0

    paint.textSize = 9.5f
    paint.isFakeBoldText = true
    paint.color = Color.rgb(18, 55, 34)
    canvas.drawText("FIELD PROGRESS & AREA ILLUSTRATION:", 36f, diagramTop + 16f, paint)

    paint.textSize = 9.5f
    paint.isFakeBoldText = true
    paint.color = Color.rgb(13, 30, 51)
    canvas.drawText("$completionPct% Complete (Total Block: ${"%.1f".format(totalBlockAcres)} Ac)", PAGE_WIDTH - 215f, diagramTop + 16f, paint)

    // Two-tone progress bar illustration
    val barLeft = 36f
    val barRight = PAGE_WIDTH - 36f
    val barTop = diagramTop + 24f
    val barBottom = diagramTop + 37f
    val barWidth = barRight - barLeft

    // Background track (Balance - Dark Blue)
    val trackRect = RectF(barLeft, barTop, barRight, barBottom)
    paint.color = Color.rgb(15, 39, 68) // Deep Dark Blue
    canvas.drawRoundRect(trackRect, 4f, 4f, paint)

    // Filled track (Covered - Forest Green)
    val coveredWidth = (barWidth * (completionPct / 100f)).coerceIn(0f, barWidth)
    if (coveredWidth > 0f) {
      val coveredRect = RectF(barLeft, barTop, barLeft + coveredWidth, barBottom)
      paint.color = Color.rgb(46, 139, 87) // Cane Green
      canvas.drawRoundRect(coveredRect, 4f, 4f, paint)
    }

    // Legend
    paint.textSize = 8.5f
    paint.isFakeBoldText = false
    paint.color = Color.rgb(60, 80, 70)
    canvas.drawText("■ Green: Area Covered (${report.areaCoveredHa} Ac)     ■ Blue: Balance to be Done (${report.balanceToBeDoneHa} Ac)", 36f, diagramTop + 50f, paint)

    // 4. PHOTO EVIDENCE ILLUSTRATION
    val photoBoxTop = 394f
    val photoBoxBottom = 720f
    val photoBoxRect = RectF(24f, photoBoxTop, PAGE_WIDTH - 24f, photoBoxBottom)

    paint.color = Color.rgb(248, 250, 249)
    canvas.drawRoundRect(photoBoxRect, 8f, 8f, paint)

    paint.style = Paint.Style.STROKE
    paint.color = Color.rgb(200, 220, 208)
    canvas.drawRoundRect(photoBoxRect, 8f, 8f, paint)
    paint.style = Paint.Style.FILL

    // Header for Photo Box
    val photoHeaderRect = RectF(24f, photoBoxTop, PAGE_WIDTH - 24f, photoBoxTop + 24f)
    paint.color = Color.rgb(18, 55, 34) // Dark Forest Green
    canvas.drawRoundRect(photoHeaderRect, 8f, 8f, paint)
    paint.color = Color.WHITE
    paint.textSize = 10.5f
    paint.isFakeBoldText = true
    canvas.drawText("FIELD PHOTO EVIDENCE & VISUAL VERIFICATION", 36f, photoBoxTop + 16f, paint)

    var photoDrawn = false
    if (!report.photoUri.isNullOrBlank()) {
      try {
        val uri = Uri.parse(report.photoUri)
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        if (bitmap != null) {
          // Scale bitmap to fit nicely within photo area: width ~ 500, height ~ 280
          val maxTargetWidth = 500f
          val maxTargetHeight = 275f

          val scale = minOf(maxTargetWidth / bitmap.width.toFloat(), maxTargetHeight / bitmap.height.toFloat())
          val destWidth = bitmap.width * scale
          val destHeight = bitmap.height * scale

          val destLeft = 24f + ((PAGE_WIDTH - 48f) - destWidth) / 2f
          val destTop = photoBoxTop + 34f

          val destRect = RectF(destLeft, destTop, destLeft + destWidth, destTop + destHeight)
          canvas.drawBitmap(bitmap, null, destRect, null)

          // Border around photo
          paint.style = Paint.Style.STROKE
          paint.color = Color.rgb(180, 200, 190)
          paint.strokeWidth = 1f
          canvas.drawRoundRect(destRect, 4f, 4f, paint)
          paint.style = Paint.Style.FILL

          // Caption below image
          paint.textSize = 9.5f
          paint.color = Color.rgb(70, 90, 80)
          paint.isFakeBoldText = false
          val caption = "Visual evidence captured for ${report.zone}, Block ${report.blockNumber} (${report.operationName}). Submitted by ${report.createdBy}."
          canvas.drawText(caption, 36f, destTop + destHeight + 16f, paint)

          photoDrawn = true
        }
      } catch (e: Exception) {
        // Fallback to text box if bitmap decoding fails
      }
    }

    if (!photoDrawn) {
      // Placeholder illustration when no photo is attached
      paint.color = Color.rgb(120, 140, 130)
      paint.textSize = 12f
      paint.isFakeBoldText = false
      canvas.drawText("No photographic evidence was attached to this report at the time of submission.", 80f, photoBoxTop + 140f, paint)
      paint.textSize = 10f
      paint.color = Color.rgb(150, 170, 160)
      canvas.drawText("(Field photos can be attached by the Headman or Officer via the Edit Report screen)", 90f, photoBoxTop + 162f, paint)
    }

    // 5. OFFICIAL SIGN-OFF & STAMP FOOTER
    val footerTop = 730f
    paint.color = Color.rgb(100, 120, 110)
    paint.textSize = 9f
    paint.isFakeBoldText = true
    canvas.drawText("OFFICIAL CERTIFICATION & SIGN-OFF:", 36f, footerTop, paint)

    paint.style = Paint.Style.STROKE
    paint.color = Color.rgb(160, 180, 170)
    canvas.drawLine(36f, footerTop + 35f, 220f, footerTop + 35f, paint)
    canvas.drawLine(340f, footerTop + 35f, 520f, footerTop + 35f, paint)
    paint.style = Paint.Style.FILL

    paint.textSize = 8.5f
    paint.isFakeBoldText = false
    paint.color = Color.rgb(120, 135, 125)
    canvas.drawText("Field Supervisor / Headman Signature", 40f, footerTop + 48f, paint)
    canvas.drawText("Plantation General Manager Approval", 350f, footerTop + 48f, paint)

    // Document timestamp line
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    val timestamp = sdf.format(Date())
    canvas.drawText("UPENJAnet Digital Agricultural Operations System • Document generated on $timestamp", 36f, 810f, paint)

    pdfDocument.finishPage(page)

    // Save to cache directory
    val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
    val sanitizedId = report.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    val file = File(reportsDir, "UPENJA_Report_${sanitizedId}.pdf")
    val outputStream = FileOutputStream(file)
    pdfDocument.writeTo(outputStream)
    outputStream.flush()
    outputStream.close()
    pdfDocument.close()

    return file
  }

  /**
   * Generates a comprehensive Weekly Summary Operations PDF report.
   * Aggregates total area covered per operation (e.g. Stubble shaving 80Ac).
   */
  fun exportWeeklySummaryToPdf(
    context: Context,
    startDate: String,
    endDate: String,
    zoneFilter: String?,
    summaries: List<OperationWeeklySummary>,
  ): File {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas

    canvas.drawColor(Color.WHITE)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Header banner (Dark Green & Dark Blue)
    val headerRect = RectF(24f, 24f, PAGE_WIDTH - 24f, 96f)
    paint.color = Color.rgb(13, 30, 51) // Deep Dark Blue (#0D1E33)
    canvas.drawRoundRect(headerRect, 10f, 10f, paint)

    paint.color = Color.rgb(18, 55, 34) // Dark Forest Green strip
    canvas.drawRoundRect(RectF(24f, 88f, PAGE_WIDTH - 24f, 96f), 0f, 0f, paint)

    paint.color = Color.WHITE
    paint.textSize = 16f
    paint.isFakeBoldText = true
    canvas.drawText("UPENJAnet - WEEKLY OPERATIONS SUMMARY REPORT", 40f, 54f, paint)

    paint.color = Color.rgb(200, 230, 245)
    paint.textSize = 9.5f
    paint.isFakeBoldText = false
    val zoneLabel = if (zoneFilter.isNullOrBlank() || zoneFilter == "All Zones") "All Plantation Zones" else zoneFilter
    canvas.drawText("Period: From $startDate to $endDate  •  Zone: $zoneLabel", 40f, 74f, paint)

    // Grand Totals Summary Card
    val totalAcres = summaries.sumOf { it.totalAreaAcres }
    val totalLabourers = summaries.sumOf { it.totalLabourers }
    val totalRecords = summaries.sumOf { it.reportCount }

    val grandTotalRect = RectF(24f, 108f, PAGE_WIDTH - 24f, 172f)
    paint.color = Color.rgb(240, 247, 242)
    canvas.drawRoundRect(grandTotalRect, 8f, 8f, paint)

    paint.style = Paint.Style.STROKE
    paint.color = Color.rgb(180, 215, 195)
    canvas.drawRoundRect(grandTotalRect, 8f, 8f, paint)
    paint.style = Paint.Style.FILL

    paint.textSize = 10f
    paint.isFakeBoldText = true
    paint.color = Color.rgb(18, 55, 34)
    canvas.drawText("AGGREGATED PERIOD METRICS:", 40f, 126f, paint)

    paint.textSize = 18f
    val formattedGrandTotal = if (totalAcres % 1.0 == 0.0) "${totalAcres.toInt()}Ac" else "${"%.1f".format(totalAcres)}Ac"
    canvas.drawText(formattedGrandTotal, 40f, 154f, paint)

    paint.textSize = 9.5f
    paint.isFakeBoldText = false
    paint.color = Color.rgb(60, 80, 70)
    canvas.drawText("Grand Total Area Covered", 40f, 166f, paint)

    paint.textSize = 18f
    paint.isFakeBoldText = true
    paint.color = Color.rgb(13, 30, 51)
    canvas.drawText("$totalLabourers", 240f, 154f, paint)
    paint.textSize = 9.5f
    paint.isFakeBoldText = false
    paint.color = Color.rgb(60, 80, 70)
    canvas.drawText("Total Labourers Engaged", 240f, 166f, paint)

    paint.textSize = 18f
    paint.isFakeBoldText = true
    paint.color = Color.rgb(13, 30, 51)
    canvas.drawText("$totalRecords", 420f, 154f, paint)
    paint.textSize = 9.5f
    paint.isFakeBoldText = false
    paint.color = Color.rgb(60, 80, 70)
    canvas.drawText("Total Field Reports Filed", 420f, 166f, paint)

    // Detailed Operations Breakdown Table
    val tableTop = 186f
    paint.color = Color.rgb(13, 35, 24)
    paint.textSize = 11f
    paint.isFakeBoldText = true
    canvas.drawText("BREAKDOWN PER SPECIFIC OPERATION:", 26f, tableTop, paint)

    var currentY = tableTop + 14f

    summaries.forEachIndexed { idx, op ->
      val cardRect = RectF(24f, currentY, PAGE_WIDTH - 24f, currentY + 68f)
      paint.color = Color.rgb(250, 252, 251)
      canvas.drawRoundRect(cardRect, 6f, 6f, paint)

      paint.style = Paint.Style.STROKE
      paint.color = Color.rgb(220, 235, 226)
      canvas.drawRoundRect(cardRect, 6f, 6f, paint)
      paint.style = Paint.Style.FILL

      // Operation Title & Area Badge
      paint.textSize = 11.5f
      paint.isFakeBoldText = true
      paint.color = Color.rgb(13, 35, 24)
      canvas.drawText("${idx + 1}. ${op.operationName}", 36f, currentY + 20f, paint)

      // Area Badge in Dark Green
      val formattedArea = if (op.totalAreaAcres % 1.0 == 0.0) "${op.totalAreaAcres.toInt()}Ac" else "${"%.1f".format(op.totalAreaAcres)}Ac"
      paint.textSize = 13f
      paint.isFakeBoldText = true
      paint.color = Color.rgb(18, 90, 42)
      canvas.drawText("Area: $formattedArea", PAGE_WIDTH - 130f, currentY + 20f, paint)

      // Exact user format note
      paint.textSize = 9.5f
      paint.isFakeBoldText = true
      paint.color = Color.rgb(13, 30, 51)
      canvas.drawText("From $startDate to $endDate, Area Covered by ${op.operationName} is $formattedArea", 36f, currentY + 38f, paint)

      // Contractors and Blocks
      paint.textSize = 9f
      paint.isFakeBoldText = false
      paint.color = Color.rgb(80, 95, 85)
      val contractorStr = if (op.contractors.isNotEmpty()) "Contractor(s): ${op.contractors.joinToString(", ")}" else "No contractor assigned"
      val blocksStr = if (op.blocks.isNotEmpty()) "Blocks: ${op.blocks.joinToString(", ")}" else ""
      canvas.drawText("$contractorStr  •  $blocksStr  •  ${op.reportCount} record(s)  •  ${op.totalLabourers} Labourers", 36f, currentY + 54f, paint)

      currentY += 76f
    }

    // Sign off & Date at bottom
    val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
    paint.textSize = 8.5f
    paint.isFakeBoldText = false
    paint.color = Color.rgb(120, 135, 125)
    canvas.drawText("UPENJAnet Digital Agricultural Operations System • Report generated on $timestamp", 36f, 810f, paint)

    pdfDocument.finishPage(page)

    val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
    val file = File(reportsDir, "UPENJA_Weekly_Operations_Summary_${startDate.replace("/", "-")}_to_${endDate.replace("/", "-")}.pdf")
    val outputStream = FileOutputStream(file)
    pdfDocument.writeTo(outputStream)
    outputStream.flush()
    outputStream.close()
    pdfDocument.close()

    return file
  }

  /**
   * Triggers Android share sheet for a PDF file with read URI permission.
   */
  fun sharePdf(context: Context, pdfFile: File, title: String = "Share Report PDF") {
    val authority = "${context.packageName}.fileprovider"
    val uri = FileProvider.getUriForFile(context, authority, pdfFile)

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
      type = "application/pdf"
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_SUBJECT, pdfFile.name)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooser = Intent.createChooser(sendIntent, title).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
  }

  /**
   * Opens and views the PDF in an external PDF viewer or print service.
   */
  fun openPdf(context: Context, pdfFile: File) {
    val authority = "${context.packageName}.fileprovider"
    val uri = FileProvider.getUriForFile(context, authority, pdfFile)

    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, "application/pdf")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
      context.startActivity(viewIntent)
    } catch (e: Exception) {
      // If no direct PDF viewer is available, fallback to chooser
      sharePdf(context, pdfFile, "Open / Download PDF")
    }
  }

  /**
   * Directly exports and launches the share sheet for an individual report.
   */
  fun exportAndShareReport(context: Context, report: ReportEntity) {
    val file = exportReportToPdf(context, report)
    sharePdf(context, file, "Share Field Report PDF (${report.id})")
  }

  /**
   * Copies the generated PDF to the public Downloads folder for easy file access.
   */
  fun downloadPdf(context: Context, pdfFile: File): File {
    return try {
      val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
      if (!downloadsDir.exists()) downloadsDir.mkdirs()
      val dest = File(downloadsDir, pdfFile.name)
      pdfFile.copyTo(dest, overwrite = true)
      dest
    } catch (e: Exception) {
      pdfFile
    }
  }
}
