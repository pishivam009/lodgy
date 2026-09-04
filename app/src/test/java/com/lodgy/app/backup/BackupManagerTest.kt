package com.lodgy.app.backup

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.lodgy.app.data.LodgyDatabase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var filesDir: File
    private lateinit var cacheDir: File
    private lateinit var dbFile: File
    private lateinit var photosDir: File
    private val context: Context = mockk()
    private val contentResolver: ContentResolver = mockk()
    private val database: LodgyDatabase = mockk(relaxed = true)

    private lateinit var backupManager: BackupManager

    @Before
    fun setUp() {
        filesDir = tempFolder.newFolder("files")
        cacheDir = tempFolder.newFolder("cache")
        photosDir = File(filesDir, "photos").apply { mkdirs() }
        dbFile = File(tempFolder.newFolder("db"), LodgyDatabase.DATABASE_NAME)
        dbFile.writeBytes(byteArrayOf(1, 2, 3, 4))

        every { context.filesDir } returns filesDir
        every { context.cacheDir } returns cacheDir
        every { context.getDatabasePath(LodgyDatabase.DATABASE_NAME) } returns dbFile
        every { context.contentResolver } returns contentResolver
        every { database.query("PRAGMA wal_checkpoint(FULL)", null) } returns mockk<Cursor>(relaxed = true)

        backupManager = BackupManager(context, database)
    }

    @Test
    fun `export bundles the db and every photo into a zip`() = runTest {
        File(photosDir, "a.jpg").writeBytes(byteArrayOf(10, 20))
        File(photosDir, "b.jpg").writeBytes(byteArrayOf(30, 40, 50))
        val destinationFile = File(tempFolder.root, "export.zip")
        val destinationUri: Uri = mockk()
        every { contentResolver.openOutputStream(destinationUri) } returns destinationFile.outputStream()

        val result = backupManager.export(destinationUri)

        assertTrue(result)
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(destinationFile.inputStream()).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                entries[entry.name] = zip.readBytes()
            }
        }
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), entries["lodgy.db"])
        assertArrayEquals(byteArrayOf(10, 20), entries["photos/a.jpg"])
        assertArrayEquals(byteArrayOf(30, 40, 50), entries["photos/b.jpg"])
    }

    @Test
    fun `export fails cleanly when the destination stream cannot be opened`() = runTest {
        val destinationUri: Uri = mockk()
        every { contentResolver.openOutputStream(destinationUri) } returns null

        assertFalse(backupManager.export(destinationUri))
    }

    private fun zipFile(build: ZipOutputStream.() -> Unit): File {
        val file = File(tempFolder.root, "import-${System.nanoTime()}.zip")
        ZipOutputStream(file.outputStream()).use { it.build() }
        return file
    }

    @Test
    fun `stageImport extracts db and photos and reports success`() = runTest {
        val zip = zipFile {
            putNextEntry(ZipEntry("lodgy.db"))
            write(byteArrayOf(9, 8, 7))
            closeEntry()
            putNextEntry(ZipEntry("photos/a.jpg"))
            write(byteArrayOf(1, 1))
            closeEntry()
        }
        val sourceUri: Uri = mockk()
        every { contentResolver.openInputStream(sourceUri) } returns zip.inputStream()

        val (result, stagingDir) = backupManager.stageImport(sourceUri)

        assertEquals(ImportResult.Success, result)
        assertTrue(stagingDir != null)
        assertArrayEquals(byteArrayOf(9, 8, 7), File(stagingDir, "lodgy.db").readBytes())
        assertArrayEquals(byteArrayOf(1, 1), File(stagingDir, "photos/a.jpg").readBytes())
    }

    @Test
    fun `stageImport rejects photo entries that try to escape the photos directory`() = runTest {
        val zip = zipFile {
            putNextEntry(ZipEntry("lodgy.db"))
            write(byteArrayOf(9))
            closeEntry()
            putNextEntry(ZipEntry("photos/../../evil.jpg"))
            write(byteArrayOf(6, 6, 6))
            closeEntry()
        }
        val sourceUri: Uri = mockk()
        every { contentResolver.openInputStream(sourceUri) } returns zip.inputStream()

        val (result, stagingDir) = backupManager.stageImport(sourceUri)

        assertEquals(ImportResult.Success, result)
        requireNotNull(stagingDir)
        val escaped = File(stagingDir.parentFile, "evil.jpg")
        assertFalse(escaped.exists())
        val photosDirStaged = File(stagingDir, "photos")
        assertTrue(photosDirStaged.listFiles()?.isEmpty() != false)
    }

    @Test
    fun `stageImport reports NotALodgyBackup when there is no db entry`() = runTest {
        val zip = zipFile {
            putNextEntry(ZipEntry("photos/a.jpg"))
            write(byteArrayOf(1))
            closeEntry()
        }
        val sourceUri: Uri = mockk()
        every { contentResolver.openInputStream(sourceUri) } returns zip.inputStream()

        val (result, stagingDir) = backupManager.stageImport(sourceUri)

        assertEquals(ImportResult.NotALodgyBackup, result)
        assertNull(stagingDir)
    }

    @Test
    fun `stageImport fails cleanly when the source stream cannot be opened`() = runTest {
        val sourceUri: Uri = mockk()
        every { contentResolver.openInputStream(sourceUri) } returns null

        val (result, stagingDir) = backupManager.stageImport(sourceUri)

        assertTrue(result is ImportResult.Failed)
        assertNull(stagingDir)
    }

    @Test
    fun `applyStaged overwrites the live db and photos then cleans up staging`() = runTest {
        File(photosDir, "stale.jpg").writeBytes(byteArrayOf(0))

        val stagingDir = tempFolder.newFolder("staging")
        File(stagingDir, "lodgy.db").writeBytes(byteArrayOf(5, 5, 5))
        val stagedPhotos = File(stagingDir, "photos").apply { mkdirs() }
        File(stagedPhotos, "fresh.jpg").writeBytes(byteArrayOf(7, 7))

        backupManager.applyStaged(stagingDir)

        assertArrayEquals(byteArrayOf(5, 5, 5), dbFile.readBytes())
        assertFalse(File(photosDir, "stale.jpg").exists())
        assertArrayEquals(byteArrayOf(7, 7), File(photosDir, "fresh.jpg").readBytes())
        assertFalse(stagingDir.exists())
    }

    /** The regression guard for LODGY-78. Every other export test creates photosDir in setUp, which
     *  is why a successful-but-reported-failed export went unnoticed: listFiles() returns null only
     *  when the directory is absent, which is the normal state until the first photo is saved. */
    @Test
    fun `export succeeds and says so when the photos directory has never been created`() = runTest {
        photosDir.deleteRecursively()
        assertFalse(photosDir.exists())
        val destinationFile = File(tempFolder.root, "nophotos.zip")
        val destinationUri: Uri = mockk()
        every { contentResolver.openOutputStream(destinationUri) } returns destinationFile.outputStream()

        val result = backupManager.export(destinationUri)

        assertTrue(result)
        val entries = mutableListOf<String>()
        ZipInputStream(destinationFile.inputStream()).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entries += it.name }
        }
        assertEquals(listOf("lodgy.db"), entries)
    }

    @Test
    fun `export succeeds when the photos directory exists but is empty`() = runTest {
        val destinationFile = File(tempFolder.root, "emptyphotos.zip")
        val destinationUri: Uri = mockk()
        every { contentResolver.openOutputStream(destinationUri) } returns destinationFile.outputStream()

        assertTrue(backupManager.export(destinationUri))
    }
}
