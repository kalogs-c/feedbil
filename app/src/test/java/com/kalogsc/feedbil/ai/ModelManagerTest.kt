package com.kalogsc.feedbil.ai

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ModelManagerTest {

    private lateinit var directory: File

    @Before
    fun setUp() {
        directory = Files.createTempDirectory("feedbil-model-test").toFile()
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun manager(body: String = CONTENT, code: Int = 200) = ModelManager(
        filesDir = directory,
        httpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message(if (code == 200) "OK" else "Error")
                    .body(body.toByteArray().toResponseBody("application/octet-stream".toMediaType()))
                    .build()
            }
            .build(),
        modelUrl = "https://example.com/model.litertlm",
        expectedBytes = CONTENT.toByteArray().size.toLong(),
    )

    @Test
    fun `downloads the file into app storage and reports progress`() = runBlocking {
        val manager = manager()

        manager.download()

        assertEquals(CONTENT, manager.modelFile.readText())
        assertTrue(manager.isAvailable())
        assertEquals(ModelState.Ready, manager.state.first())
    }

    @Test
    fun `a truncated download is rejected and leaves no file behind`() = runBlocking {
        val manager = manager(body = "short")

        manager.download()

        val state = manager.state.first()
        assertTrue(state is ModelState.Failed)
        assertFalse(manager.modelFile.exists())
        assertFalse(File(directory, "${manager.modelFile.name}.part").exists())
    }

    @Test
    fun `an http error is reported without keeping a partial file`() = runBlocking {
        val manager = manager(code = 503)

        manager.download()

        assertTrue(manager.state.first() is ModelState.Failed)
        assertFalse(manager.modelFile.exists())
    }

    @Test
    fun `a valid file appearing after a failure recovers the ready state`() = runBlocking {
        val manager = manager(code = 503)
        manager.download()
        assertTrue(manager.state.first() is ModelState.Failed)
        manager.modelFile.writeText(CONTENT)

        manager.download()

        assertEquals(ModelState.Ready, manager.state.first())
    }

    @Test
    fun `an existing file of the expected size is treated as ready`() = runBlocking {
        val manager = manager()
        manager.modelFile.writeText(CONTENT)

        val restarted = manager()

        assertTrue(restarted.isAvailable())
        assertEquals(ModelState.Ready, withTimeout(1_000) { restarted.state.first() })
    }

    private companion object {
        const val CONTENT = "a-small-fake-model-file"
    }
}
