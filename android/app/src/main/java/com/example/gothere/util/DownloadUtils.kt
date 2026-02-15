package com.example.gothere.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment

object DownloadUtils {
    fun suggestedFileName(url: String, fallback: String = "document.pdf"): String {
        val last = Uri.parse(url).lastPathSegment ?: return fallback
        return if (last.contains('.')) last else "$last.pdf"
    }

    /** Enqueue a download; file lands in Downloads (public on <29, app-specific on 29+). */
    fun downloadPdf(context: Context, url: String, fileName: String? = null): Long {
        val name = fileName ?: suggestedFileName(url)
        val req = DownloadManager.Request(Uri.parse(url)).apply {
            setMimeType("application/pdf")
            setTitle(name)
            setDescription("Downloading…")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, name)
            } else {
                @Suppress("DEPRECATION")
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
            }
        }
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(req)
    }
}
