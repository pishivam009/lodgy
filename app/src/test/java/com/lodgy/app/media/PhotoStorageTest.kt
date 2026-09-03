package com.lodgy.app.media

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PhotoStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var filesDir: File
    private lateinit var cacheDir: File
    private val context: Context = mockk()
    private val contentResolver: ContentResolver = mockk()
    private lateinit var photoStorage: PhotoStorage

    @Before
    fun setUp() {
        filesDir = tempFolder.newFolder("files")
        cacheDir = tempFolder.newFolder("cache")
        every { context.filesDir } returns filesDir
        every { context.cacheDir } returns cacheDir
        every { context.contentResolver } returns contentResolver
        every { context.packageName } returns "com.lodgy.app"
        photoStorage = PhotoStorage(context)
    }

    @After
    fun tearDown() {
        unmockkStatic(FileProvider::class)
    }

    @Test
    fun `createCameraOutputUri asks FileProvider for a uri under the app's fileprovider authority`() {
        mockkStatic(FileProvider::class)
        val expectedUri: Uri = mockk()
        val authoritySlot = io.mockk.slot<String>()
        val fileSlot = io.mockk.slot<File>()
        every { FileProvider.getUriForFile(context, capture(authoritySlot), capture(fileSlot)) } returns expectedUri

        val result = photoStorage.createCameraOutputUri()

        assertTrue(result === expectedUri)
        assertTrue(authoritySlot.captured == "com.lodgy.app.fileprovider")
        assertTrue(fileSlot.captured.parentFile == cacheDir)
        assertTrue(fileSlot.captured.name.endsWith(".jpg"))
    }

    @Test
    fun `persist copies the source stream into app-private photos storage and returns its path`() = runTest {
        val sourceUri: Uri = mockk()
        val sourceBytes = byteArrayOf(1, 2, 3, 4, 5)
        every { contentResolver.openInputStream(sourceUri) } answers { sourceBytes.inputStream() }

        val path = photoStorage.persist(sourceUri)

        val savedFile = File(path)
        assertTrue(savedFile.exists())
        assertTrue(savedFile.parentFile == File(filesDir, "photos"))
        assertTrue(savedFile.name.endsWith(".jpg"))
        assertArrayEquals(sourceBytes, savedFile.readBytes())
    }

    @Test
    fun `persist tolerates a source stream that can't be opened`() = runTest {
        val sourceUri: Uri = mockk()
        every { contentResolver.openInputStream(sourceUri) } returns null

        val path = photoStorage.persist(sourceUri)

        assertTrue(path.isNotBlank())
        assertTrue(!File(path).exists())
    }
}
