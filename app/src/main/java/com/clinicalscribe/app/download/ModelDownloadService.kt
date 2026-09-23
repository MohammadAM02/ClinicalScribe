package com.clinicalscribe.app.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.clinicalscribe.app.MainActivity
import com.clinicalscribe.app.R
import com.clinicalscribe.app.asr.HotwordsFileBuilder
import com.clinicalscribe.app.asr.ModelFiles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads the ~650MB Parakeet ASR model (4 files) from Hugging Face into
 * app-private storage, with a progress notification -- run as a foreground
 * service so it survives the user backgrounding the app mid-download.
 */
class ModelDownloadService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("Starting download…", 0))
        scope.launch { runDownload() }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun runDownload() {
        try {
            val files = ModelFiles.DOWNLOAD_FILES
            val dir = ModelFiles.modelDir(applicationContext)
            dir.mkdirs()

            for ((index, entry) in files.withIndex()) {
                val (fileName, url) = entry
                val dest = File(dir, fileName)
                if (dest.exists() && dest.length() > 0) {
                    // Left over from a previous, possibly interrupted, run.
                    ModelDownloadState.update(
                        ModelDownloadStatus.Downloading(fileName, index + 1, files.size, 1f, (index + 1f) / files.size),
                    )
                    continue
                }
                downloadFile(url, dest) { fileProgress ->
                    val overall = (index + fileProgress) / files.size
                    ModelDownloadState.update(
                        ModelDownloadStatus.Downloading(fileName, index + 1, files.size, fileProgress, overall),
                    )
                    updateNotification(fileName, overall)
                }
            }

            HotwordsFileBuilder.ensureBpeVocab(applicationContext)
            ModelDownloadState.update(ModelDownloadStatus.Done)
            notificationManager.notify(NOTIFICATION_ID, buildNotification("Speech model ready", 100, ongoing = false))
        } catch (e: Exception) {
            ModelDownloadState.update(ModelDownloadStatus.Error(e.message ?: "Download failed"))
            notificationManager.notify(
                NOTIFICATION_ID,
                buildNotification("Download failed: ${e.message}", 0, ongoing = false),
            )
        } finally {
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    /** Downloads to a .part file and only renames to the final name on success, so a half-downloaded
     *  file is never mistaken for a complete one if the app is killed mid-download. */
    private fun downloadFile(urlString: String, dest: File, onProgress: (Float) -> Unit) {
        val partFile = File(dest.parentFile, "${dest.name}.part")
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 30_000
                readTimeout = 30_000
            }
            connection.connect()
            val totalBytes = connection.contentLengthLong

            connection.inputStream.use { input ->
                partFile.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (totalBytes > 0) onProgress(downloaded.toFloat() / totalBytes)
                    }
                }
            }
            if (!partFile.renameTo(dest)) {
                throw IOException("Failed to finalize ${dest.name}")
            }
        } finally {
            connection?.disconnect()
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Model download", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(fileName: String, overallProgress: Float) {
        notificationManager.notify(
            NOTIFICATION_ID,
            buildNotification("Downloading $fileName…", (overallProgress * 100).toInt()),
        )
    }

    private fun buildNotification(text: String, progressPercent: Int, ongoing: Boolean = true): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_mic)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .setProgress(100, progressPercent, false)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "model_download"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, ModelDownloadService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
