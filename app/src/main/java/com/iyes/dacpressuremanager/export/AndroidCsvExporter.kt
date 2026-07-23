package com.iyes.dacpressuremanager.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.iyes.dacpressuremanager.R
import java.io.File

object AndroidCsvExporter {
    fun writeToUri(
        context: Context,
        uri: Uri,
        document: CsvDocument,
    ) {
        val stream = requireNotNull(context.contentResolver.openOutputStream(uri, "w")) {
            "The selected document cannot be opened for writing"
        }
        stream.use { it.write(document.bytes) }
    }

    fun share(
        context: Context,
        document: CsvDocument,
    ) {
        val exportDirectory = File(context.cacheDir, "history-exports").apply {
            mkdirs()
        }
        exportDirectory.listFiles()
            ?.filter { it.isFile && it.name != document.fileName }
            ?.forEach(File::delete)
        val exportFile = File(exportDirectory, document.fileName).apply {
            writeBytes(document.bytes)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            exportFile,
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, document.fileName)
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_csv_title))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(
                shareIntent,
                context.getString(R.string.share),
            ),
        )
    }
}

