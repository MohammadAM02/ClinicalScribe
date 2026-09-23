package com.clinicalscribe.app.download

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class ModelDownloadStatus {
    data object Idle : ModelDownloadStatus()
    data class Downloading(
        val fileName: String,
        val fileIndex: Int,
        val fileCount: Int,
        val fileProgress: Float, // 0..1, this file only
        val overallProgress: Float, // 0..1, across all files
    ) : ModelDownloadStatus()
    data object Done : ModelDownloadStatus()
    data class Error(val message: String) : ModelDownloadStatus()
}

/** Process-wide download progress, updated by ModelDownloadService and observed by the UI. */
object ModelDownloadState {
    private val _status = MutableStateFlow<ModelDownloadStatus>(ModelDownloadStatus.Idle)
    val status: StateFlow<ModelDownloadStatus> = _status.asStateFlow()

    fun update(newStatus: ModelDownloadStatus) {
        _status.value = newStatus
    }
}
