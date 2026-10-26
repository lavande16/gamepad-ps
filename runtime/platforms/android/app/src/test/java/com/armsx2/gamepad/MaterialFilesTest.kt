package com.armsx2.gamepad

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files

class MaterialFilesTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun existingSaveAlwaysWinsAndMissingTargetImportsExactly() {
        val source = temporary.newFile("import.ps2").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val target = temporary.newFile("current.ps2").apply { writeBytes(byteArrayOf(9, 8, 7)) }
        assertFalse(MaterialFiles.copyMissing(source, target))
        assertArrayEquals(byteArrayOf(9, 8, 7), target.readBytes())
        val missing = File(temporary.root, "GamePad PS/Memcards/slot1.ps2")
        assertTrue(MaterialFiles.copyMissing(source, missing))
        assertEquals(MaterialFiles.sha256(source), MaterialFiles.sha256(missing))
        source.writeText("newer material must not roll back runtime progress")
        assertFalse(MaterialFiles.copyMissing(source, missing))
        assertArrayEquals(byteArrayOf(1, 2, 3), missing.readBytes())
    }

    @Test fun cardRequiresValidSizeAndContentNotFilename() {
        val fake = temporary.newFile("slot1.ps2").apply { writeText("not a card") }
        assertEquals(MaterialFiles.Card.INVALID, MaterialFiles.card(fake))
        val card = temporary.newFile("unknown.bin")
        RandomAccessFile(card, "rw").use { it.setLength(8L * 1024 * 1024); it.write("Sony PS2 Memory Card Format".toByteArray()) }
        assertEquals(MaterialFiles.Card.FORMATTED, MaterialFiles.card(card))
        val blank = temporary.newFile("blank.ps2")
        blank.outputStream().use { out -> repeat(64) { out.write(ByteArray(128 * 1024) { -1 }) } }
        assertEquals(MaterialFiles.Card.BLANK, MaterialFiles.card(blank))
        RandomAccessFile(blank, "rw").use { it.seek(blank.length() - 1); it.write(0) }
        assertEquals(MaterialFiles.Card.INVALID, MaterialFiles.card(blank))
    }

    @Test fun externalSymlinkAndHiddenDirectoriesAreExcluded() {
        val root = temporary.newFolder("GamePad PS")
        val outside = temporary.newFolder("other")
        File(outside, "game.iso").writeText("external")
        Files.createSymbolicLink(File(root, "escape").toPath(), outside.toPath())
        File(root, ".hidden").mkdir(); File(root, ".hidden/game.iso").writeText("hidden")
        File(root, "ISO").mkdir(); File(root, "ISO/game.iso").writeText("owned")
        assertEquals(listOf("game.iso"), MaterialFiles.files(root, 3).map { it.name })
        assertFalse(MaterialFiles.inside(File(root, "escape/game.iso"), root))
        assertTrue(runCatching { MaterialFiles.childDirectory(root, "escape") }.isFailure)
    }

    @Test fun renamedDllIsRejectedAndHeaderChangeInvalidatesFingerprint() {
        val fake = temporary.newFile("Lossless.dll").apply { writeText("fake") }
        assertFalse(MaterialFiles.lossless(fake))
        val timestamp = fake.lastModified()
        val before = MaterialFiles.fingerprint(fake)
        fake.writeText("real"); fake.setLastModified(timestamp)
        assertNotEquals(before, MaterialFiles.fingerprint(fake))
    }

    @Test fun conflictingImportCardsNeverChooseArbitrarily() {
        val first = temporary.newFile("mcd001.ps2")
        RandomAccessFile(first, "rw").use { it.setLength(8L * 1024 * 1024); it.write("Sony PS2 Memory Card Format".toByteArray()) }
        val second = temporary.newFile("memorycard1.ps2")
        first.copyTo(second, overwrite=true)
        assertEquals(first, MaterialFiles.importCandidate(listOf(first, second)))
        RandomAccessFile(second, "rw").use { it.seek(1024); it.write(7) }
        assertTrue(runCatching { MaterialFiles.importCandidate(listOf(first, second)) }.isFailure)
        assertFalse(File(temporary.root, "slot1.ps2").exists())
    }
}
