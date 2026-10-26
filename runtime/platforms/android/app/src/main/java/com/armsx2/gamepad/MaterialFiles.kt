// SPDX-License-Identifier: GPL-3.0-or-later
package com.armsx2.gamepad

import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files
import java.security.MessageDigest

/** PS2-specific file rules; no Android/native dependencies, so preservation can be unit tested. */
object MaterialFiles {
    fun inside(file: File, root: File): Boolean =
        file.canonicalFile.toPath().startsWith(root.canonicalFile.toPath())

    // Adapted from NS GameFoldPackage.childDirectory/filesWithExtensions. Do not follow links
    // out of the product folder, recurse into runtime saves, or infer a format from its suffix.
    fun childDirectory(parent: File, name: String): File {
        val candidate = parent.listFiles()?.firstOrNull { it.name.equals(name, true) } ?: File(parent, name)
        check(inside(candidate, parent)) { "目录超出游戏材料范围：$name" }
        check(!candidate.exists() || candidate.isDirectory) { "需要文件夹：$name" }
        return candidate
    }

    fun files(parent: File, depth: Int = 1): List<File> = if (!parent.isDirectory) emptyList() else
        parent.walkTopDown().maxDepth(depth).onEnter {
            inside(it, parent) && (it == parent || !it.name.startsWith('.'))
        }.filter { it.isFile && !it.name.startsWith('.') && inside(it, parent) }
            .sortedBy { it.name.lowercase() }.toList()

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(128 * 1024)
            while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** NS uses path/size/mtime fingerprints. Include a header hash, without hashing GB-size ISOs. */
    fun fingerprint(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update("${file.canonicalPath}\u0000${file.length()}\u0000${file.lastModified()}".toByteArray())
        file.inputStream().use { digest.update(it.readNBytes(65536)) }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** Copy through a sibling temporary file. Existing targets are never replaced. */
    fun copyMissing(source: File, target: File): Boolean {
        if (target.exists()) return false
        check(target.parentFile!!.isDirectory || target.parentFile!!.mkdirs())
        val temporary = File.createTempFile(".import-", ".part", target.parentFile)
        try {
            source.copyTo(temporary, overwrite = true)
            check(sha256(source) == sha256(temporary)) { "导入校验失败：${source.name}" }
            Files.move(temporary.toPath(), target.toPath()) // No REPLACE_EXISTING.
            return true
        } finally { temporary.delete() }
    }

    enum class Card { FORMATTED, BLANK, INVALID }
    fun importCandidate(candidates: List<File>): File? {
        val valid = candidates.filter { card(it) != Card.INVALID }
        require(valid.size <= 1 || valid.map(::sha256).distinct().size == 1) { "存在不同进度的待导入记忆卡" }
        return valid.firstOrNull()
    }

    fun card(file: File): Card {
        if (!file.isFile) return Card.INVALID
        val supported = listOf(8, 16, 32, 64, 128).flatMap { mb ->
            val bytes = mb * 1024L * 1024L; listOf(bytes, bytes / 512 * 528)
        }
        if (file.length() !in supported) return Card.INVALID
        file.inputStream().buffered().use { input ->
            val head = input.readNBytes(128)
            if (head.toString(Charsets.US_ASCII).startsWith("Sony PS2 Memory Card Format")) return Card.FORMATTED
            if (head.any { it != (-1).toByte() }) return Card.INVALID
            val buffer = ByteArray(128 * 1024)
            while (true) {
                val n = input.read(buffer); if (n < 0) break
                if ((0 until n).any { buffer[it] != (-1).toByte() }) return Card.INVALID
            }
        }
        return Card.BLANK
    }

    /** Reject renamed text/arbitrary DLLs before the native LSFG shader extractor sees them. */
    fun lossless(file: File): Boolean = runCatching {
        if (!file.isFile || file.length() !in 1_000_000L..64_000_000L) return false
        RandomAccessFile(file, "r").use { input ->
            if (input.readUnsignedShort() != 0x4d5a) return false
            input.seek(0x3c); val offset = Integer.reverseBytes(input.readInt()).toLong() and 0xffffffffL
            if (offset > file.length() - 6) return false
            input.seek(offset)
            if (input.readInt() != 0x50450000 || input.readUnsignedShort() != 0x6486) return false
        }
        val bytes = file.readBytes()
        (0 until bytes.size - 3).any { bytes[it] == 0x44.toByte() && bytes[it+1] == 0x58.toByte() && bytes[it+2] == 0x42.toByte() && bytes[it+3] == 0x43.toByte() }
    }.getOrDefault(false)
}
