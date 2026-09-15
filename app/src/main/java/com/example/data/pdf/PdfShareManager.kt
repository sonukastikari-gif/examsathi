package com.example.data.pdf

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Manages reliable PDF file access, opening, and sharing
 * using Android-supported FileProvider mechanisms.
 */
object PdfShareManager {

    /**
     * Obtains a secure content:// URI for the generated PDF file
     */
    fun getContentUri(context: Context, file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    /**
     * Opens the PDF scorecard using the system default PDF viewer.
     */
    fun openPdf(context: Context, file: File): Result<Unit> {
        return try {
            val uri = getContentUri(context, file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: ActivityNotFoundException) {
            // If no dedicated PDF viewer is installed, attempt fallback to chooser
            try {
                val uri = getContentUri(context, file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "Open scorecard with...").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                Result.success(Unit)
            } catch (fallbackEx: Exception) {
                Result.failure(Exception("No PDF viewer app found on device. PDF saved at: ${file.name}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Shares the PDF scorecard via messaging, email, Drive, or other apps.
     */
    fun sharePdf(context: Context, file: File, testTitle: String): Result<Unit> {
        return try {
            val uri = getContentUri(context, file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "ExamSathi Scorecard: $testTitle")
                putExtra(Intent.EXTRA_TEXT, "Here is my ExamSathi test scorecard and detailed performance analysis for $testTitle.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share Scorecard PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
