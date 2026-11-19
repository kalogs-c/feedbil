package com.kalogsc.feedbil.ai

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

/** Observable state of the on-device model file. */
sealed interface ModelState {
    data object NotDownloaded : ModelState
    data class Downloading(val bytes: Long, val total: Long) : ModelState
    data object Ready : ModelState
    data class Failed(val detail: String?) : ModelState
}

/**
 * Downloads the model from the public Hugging Face file into app storage.
 *
 * The file is large, so this uses a dedicated long-timeout client (the feed
 * client's 20 s call timeout would abort it). Nothing is stored in assets or
 * the APK. The expected size is checked before the path is handed to the
 * engine, and a partial file is never left behind.
 */
open class ModelManager(
    filesDir: File,
    private val httpClient: OkHttpClient,
    private val modelUrl: String = MODEL_URL,
    private val expectedBytes: Long = EXPECTED_BYTES,
) {
    val modelFile = File(filesDir, MODEL_FILE_NAME)
    val modelPath: String get() = modelFile.path

    private val _state = MutableStateFlow(if (isAvailable()) ModelState.Ready else ModelState.NotDownloaded)
    open val state: StateFlow<ModelState> = _state.asStateFlow()

    init {
        // A truncated or replaced file must never be given to the engine.
        if (modelFile.exists() && !isAvailable()) modelFile.delete()
    }

    fun isAvailable(): Boolean = modelFile.isFile && modelFile.length() == expectedBytes

    open suspend fun download() {
        if (_state.value is ModelState.Downloading || isAvailable()) return
        _state.value = ModelState.Downloading(0, EXPECTED_BYTES)
        val partial = File(modelFile.parentFile, "$MODEL_FILE_NAME.part")

        withContext(Dispatchers.IO) {
            try {
                partial.delete()
                val request = Request.Builder().url(modelUrl).get().build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                    val body = response.body ?: throw IOException("Empty response")
                    val total = body.contentLength().takeIf { it > 0 } ?: expectedBytes
                    partial.outputStream().use { output ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var bytes = 0L
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                                bytes += read
                                _state.value = ModelState.Downloading(bytes, total)
                            }
                        }
                    }
                }
                if (partial.length() != expectedBytes) {
                    throw IOException("Expected $expectedBytes bytes, got ${partial.length()}")
                }
                if (modelFile.exists()) modelFile.delete()
                if (!partial.renameTo(modelFile)) throw IOException("Could not store the model file")
                _state.value = ModelState.Ready
            } catch (cancellation: CancellationException) {
                partial.delete()
                _state.value = ModelState.NotDownloaded
                throw cancellation
            } catch (error: Exception) {
                partial.delete()
                _state.value = ModelState.Failed(error.message?.take(160))
            }
        }
    }

    private companion object {
        const val MODEL_FILE_NAME = "gemma-4-E2B-it.litertlm"
        const val MODEL_URL =
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/" +
                "resolve/main/gemma-4-E2B-it.litertlm"
        const val EXPECTED_BYTES = 2_588_147_712L
        const val BUFFER_SIZE = 64 * 1024
    }
}
