package com.clinicalscribe.app.ime

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.clinicalscribe.app.AppGraph
import com.clinicalscribe.app.MainActivity
import com.clinicalscribe.app.PermissionTrampolineActivity
import com.clinicalscribe.app.R
import com.clinicalscribe.app.asr.AudioRecorder
import com.clinicalscribe.app.asr.ModelFiles
import com.clinicalscribe.app.data.TextPostProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "DictationIME"

class DictationInputMethodService : InputMethodService() {

    private enum class MicState { IDLE, LISTENING, PROCESSING }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var micState = MicState.IDLE
    private var audioRecorder: AudioRecorder? = null

    private var micButton: FrameLayout? = null
    private var statusText: TextView? = null

    override fun onCreate() {
        super.onCreate()
        AppGraph.init(applicationContext)
    }

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.ime_view, null)

        statusText = view.findViewById(R.id.status_text)
        micButton = view.findViewById(R.id.btn_mic)
        micButton?.setOnClickListener { onMicTapped() }

        view.findViewById<ImageButton>(R.id.btn_backspace).setOnClickListener {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }
        view.findViewById<ImageButton>(R.id.btn_switch_ime).setOnClickListener {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        }

        setMicState(MicState.IDLE)
        return view
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        // Don't leave the mic open (or a stale recording buffered) if the user
        // switches away mid-dictation.
        if (micState == MicState.LISTENING) {
            audioRecorder?.cancel()
            audioRecorder = null
            setMicState(MicState.IDLE)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        audioRecorder?.cancel()
        super.onDestroy()
    }

    private fun onMicTapped() {
        when (micState) {
            MicState.IDLE -> startListening()
            MicState.LISTENING -> stopListeningAndTranscribe()
            MicState.PROCESSING -> { /* ignore taps while transcribing */ }
        }
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            statusText?.setText(R.string.mic_permission_needed)
            startActivity(
                Intent(this, PermissionTrampolineActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        }

        if (!ModelFiles.isModelReady(applicationContext)) {
            statusText?.setText(R.string.mic_model_missing)
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        }

        val recorder = AudioRecorder { rms ->
            // Cheap "listening" feedback: nudge the status text opacity/scale could go here;
            // kept as a no-op hook for now to avoid touching views off the recorder thread.
        }
        audioRecorder = recorder
        try {
            recorder.start()
            setMicState(MicState.LISTENING)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            audioRecorder = null
            setMicState(MicState.IDLE)
        }
    }

    private fun stopListeningAndTranscribe() {
        val recorder = audioRecorder ?: return
        audioRecorder = null
        setMicState(MicState.PROCESSING)

        val samples = recorder.stop()
        val targetConnection = currentInputConnection

        serviceScope.launch {
            val finalText = try {
                withContext(Dispatchers.Default) {
                    AppGraph.asrEngine.ensureReady(
                        vocabulary = AppGraph.vocabulary.items.value,
                        enhancedRecognition = AppGraph.preferences.enhancedRecognition,
                    )
                    val raw = AppGraph.asrEngine.transcribe(samples)
                    TextPostProcessor.apply(
                        text = raw,
                        dictionary = AppGraph.dictionary.items.value,
                        snippets = AppGraph.snippets.items.value,
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Transcription failed", e)
                null
            }

            withContext(Dispatchers.Main) {
                if (!finalText.isNullOrBlank()) {
                    targetConnection?.commitText("$finalText ", 1)
                }
                setMicState(MicState.IDLE)
            }
        }
    }

    private fun setMicState(state: MicState) {
        micState = state
        val bg = when (state) {
            MicState.IDLE -> R.drawable.bg_mic_idle
            MicState.LISTENING -> R.drawable.bg_mic_listening
            MicState.PROCESSING -> R.drawable.bg_mic_processing
        }
        micButton?.setBackgroundResource(bg)
        val statusRes = when (state) {
            MicState.IDLE -> R.string.mic_tap_to_speak
            MicState.LISTENING -> R.string.mic_listening
            MicState.PROCESSING -> R.string.mic_processing
        }
        statusText?.setText(statusRes)
    }
}
