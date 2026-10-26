// SPDX-License-Identifier: GPL-3.0-or-later
package com.armsx2.gamepad

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import com.armsx2.config.Settings
import kr.co.iefriends.pcsx2.NativeApp
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object GamePadPsAssembler {
    val root: File get() = File(Environment.getExternalStorageDirectory(), "GamePadPS")
    data class Game(val file: File, val directory: File, val serial: String)
    data class Result(val ready: Boolean, val games: Int, val messages: List<String>)
    @Volatile var readyThisProcess = false
        private set
    @Volatile private var availableLsfg = false
    @Volatile private var games: List<Game> = emptyList()

    fun gameForPath(path: String): Game? = runCatching {
        val file = File(if (path.startsWith("file:")) Uri.parse(path).path!! else path).canonicalFile
        games.firstOrNull { it.file.canonicalFile == file }
    }.getOrNull()

    fun accepts(file: File): Boolean = !MaterialFiles.inside(file, root) ||
        games.any { it.file.canonicalFile == file.canonicalFile }

    fun lsfgAvailable(): Boolean = availableLsfg

    fun cardsForPath(path: String): File? = gameForPath(path)?.let { MaterialFiles.childDirectory(it.directory, "Memcards") }

    /** Only a launch-time availability mask: missing optional material never rewrites user prefs. */
    fun runtimeSettings(settings: Settings): Settings = if (availableLsfg) settings else
        settings.copy(graphics = settings.graphics.copy(lsfgEnabled = false))

    fun prepareGame(path: String) {
        val game = gameForPath(path) ?: return
        val cards = MaterialFiles.childDirectory(game.directory, "Memcards").apply { mkdirs() }
        val states = MaterialFiles.childDirectory(game.directory, "Sstates").apply { mkdirs() }
        check(NativeApp.preparePortableGameFolders(cards.absolutePath, states.absolutePath)) {
            "当前游戏尚未停止，无法切换存档目录"
        }
    }

    @Synchronized
    fun assemble(context: Context, prefs: SharedPreferences, progress: (Int, String) -> Unit): Result {
        readyThisProcess = false
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()
        val state = GameFoldAssemblyState.load(context)
        val report = JSONObject().put("schemaVersion", 1).put("root", root.absolutePath)
        fun finish(): Result {
            val ok = errors.isEmpty()
            readyThisProcess = ok
            report.put("ready", ok).put("games", JSONArray(games.map { JSONObject()
                .put("path", it.file.absolutePath).put("serial", it.serial)
                .put("memcards", MaterialFiles.childDirectory(it.directory, "Memcards").absolutePath)
                .put("sstates", MaterialFiles.childDirectory(it.directory, "Sstates").absolutePath) }))
                .put("lsfgAvailable", availableLsfg).put("warnings", JSONArray(warnings))
                .put("errors", JSONArray(errors)).put("completedAt", System.currentTimeMillis())
            val output = File(context.filesDir, "gamepad-ps/assembly-report.json")
            output.parentFile!!.mkdirs(); output.writeText(report.toString(2))
            if (root.isDirectory) runCatching {
                File(root, "Settings").mkdirs()
                val target = File(root, "Settings/assembly-report.json")
                val temporary = File(target.parentFile, ".assembly-report.tmp")
                temporary.writeText(report.toString(2)); check(temporary.renameTo(target))
            }
            if (ok) state.put("assembly.version", "3")
            return Result(ok, games.size, errors + warnings)
        }
        if (!Environment.isExternalStorageManager()) {
            errors += "请授权读取内部存储中的 GamePadPS 文件夹"
            games = emptyList(); return finish()
        }
        PsUpgrade.migrate(context,prefs)
        root.mkdirs()
        listOf("BIOS", "LSFG", "game", "Settings").forEach { MaterialFiles.childDirectory(root, it).mkdirs() }
        progress(10, "检查 BIOS")
        val data = context.getExternalFilesDir(null) ?: context.filesDir
        val biosFolder = File(data, "bios").apply { mkdirs() }
        fun validBios(file: File): Boolean = runCatching {
            file.isFile && file.length() in 512_000L..8_388_608L &&
                NativeApp.getBiosInfoFromFd(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).detachFd()) != null
        }.getOrDefault(false)
        val biosCandidates = MaterialFiles.files(MaterialFiles.childDirectory(root, "BIOS"))
            .filter { validBios(it) }.sortedBy { if (it.name.equals("SCPH-10000.BIN", true)) "" else it.name }
        val chosen = prefs.getString("bios", null)?.let(::File)?.takeIf(::validBios)
        var bios = chosen
        if (bios == null) {
            val source = biosCandidates.firstOrNull()
            if (source == null) errors += "BIOS 文件夹中没有可用的 PS2 BIOS"
            else {
                var target = File(biosFolder, source.name)
                if (target.exists() && MaterialFiles.sha256(target) != MaterialFiles.sha256(source))
                    target = File(biosFolder, "${source.nameWithoutExtension}-${MaterialFiles.sha256(source).take(12)}.bin")
                MaterialFiles.copyMissing(source, target)
                for (extension in listOf("mec", "nvm")) {
                    val sidecar = File(source.parentFile, source.nameWithoutExtension + ".$extension")
                    if (sidecar.isFile) MaterialFiles.copyMissing(sidecar, File(target.parentFile, target.nameWithoutExtension + ".$extension"))
                }
                bios = target
            }
        } else if (biosCandidates.isEmpty()) warnings += "BIOS 材料缺失，继续使用已导入的 BIOS"
        if (bios != null) {
            prefs.edit().putString("bios", bios.absolutePath).putString("biosDir", bios.parent).commit()
            report.put("bios", bios.absolutePath)
        }

        progress(25, "检查插帧文件")
        val lsfgSource = MaterialFiles.files(MaterialFiles.childDirectory(root, "LSFG"))
            .firstOrNull { it.name.equals("Lossless.dll", true) }
        val privateDll = File(context.filesDir, "lsfg/Lossless.dll")
        availableLsfg = lsfgSource != null && MaterialFiles.lossless(lsfgSource)
        if (availableLsfg) {
            MaterialFiles.copyMissing(lsfgSource!!, privateDll)
            availableLsfg = MaterialFiles.lossless(privateDll)
            if (availableLsfg && MaterialFiles.sha256(lsfgSource) != MaterialFiles.sha256(privateDll))
                warnings += "已保留现有插帧文件，新文件未覆盖导入"
        }
        if (!availableLsfg) warnings += "未找到可用的 Lossless.dll，暂时关闭插帧，游戏仍可运行"
        report.put("dll", privateDll.absolutePath)

        progress(40, "识别游戏")
        val gameRoot = MaterialFiles.childDirectory(root, "game")
        val discovered = mutableListOf<Game>()
        val invalid = JSONArray()
        PsGameFiles.discover(gameRoot).forEach { image ->
            val file=image.file;val directory=image.directory
            val signature=MaterialFiles.fingerprint(file)
            val key="game.${file.relativeTo(root).invariantSeparatorsPath}"
            val cached=state.get(key)?.let {runCatching{JSONObject(it)}.getOrNull()}
            val serial=if(cached?.optString("fingerprint")==signature)cached.optString("serial") else
                runCatching {NativeApp.getGameSerialFromFd(ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY).detachFd())}.getOrNull()
            if(!serial.isNullOrBlank()) {
                discovered+=Game(file,directory,serial.substringAfter(':'))
                if(cached?.optString("fingerprint")!=signature)state.put(key,JSONObject().put("fingerprint",signature).put("serial",serial).toString())
            }else invalid.put(file.relativeTo(root).path)
        }
        games = discovered
        report.put("rejectedGames", invalid)
        if (invalid.length() > 0) warnings += "已跳过 ${invalid.length()} 个无法识别的游戏文件"
        if (games.isEmpty()) errors += "请将 PS2 游戏放入 GamePadPS/game 文件夹"

        progress(65, "检查游戏存档")
        val imports = JSONArray()
        games.map { it.directory }.distinct().forEach { directory ->
            val cards = MaterialFiles.childDirectory(directory, "Memcards").apply { mkdirs() }
            MaterialFiles.childDirectory(directory, "Sstates").mkdirs()
            for (slot in 1..2) {
                val target = File(cards, "slot$slot.ps2")
                if (target.exists()) {
                    if (MaterialFiles.card(target) == MaterialFiles.Card.INVALID)
                        errors += "${directory.name} 的 slot$slot.ps2 无法读取；已保留原文件，请先备份后修复"
                    continue
                }
                val accepted = if (slot == 1) setOf("slot1.ps2", "mcd001.ps2", "mcd001.mcd", "memorycard1.ps2")
                    else setOf("slot2.ps2", "mcd002.ps2", "mcd002.mcd", "memorycard2.ps2")
                val candidates = MaterialFiles.files(cards).filter { it.name.lowercase() in accepted }
                val source = try { MaterialFiles.importCandidate(candidates) } catch (_: IllegalArgumentException) {
                    errors += "${directory.name} 存在多份不同记忆卡，请先备份并将要使用的卡命名为 slot$slot.ps2"
                    continue
                }
                if (source != null) {
                    MaterialFiles.copyMissing(source, target)
                    imports.put(JSONObject().put("source", source.absolutePath).put("target", target.absolutePath).put("sha256", MaterialFiles.sha256(target)))
                } else if (candidates.isNotEmpty()) {
                    errors += "${directory.name} 的待导入记忆卡损坏，已保留，未创建空卡替代"
                }
            }
        }
        report.put("imports", imports)

        progress(85, "应用初始设置")
        val bundled = JSONObject(context.assets.open("gamepad-ps/armsx2-defaults.json").bufferedReader().use { it.readText() })
        var defaults = bundled
        val external = File(MaterialFiles.childDirectory(root, "Settings"), "armsx2-defaults.json")
        if (external.isFile) {
            runCatching {
                check(external.length() <= 1_000_000)
                val candidate = JSONObject(external.readText())
                check(candidate.getInt("schemaVersion") == 1)
                val global = candidate.getJSONObject("global")
                global.keys().forEach { key ->
                    check(bundled.getJSONObject("global").has(key)) { "未知默认项 $key" }
                    val original = bundled.getJSONObject("global").get(key); val value = global.get(key)
                    check((original is Number && value is Number) || original.javaClass == value.javaClass) { "默认项类型错误 $key" }
                }
                candidate.optJSONObject("games")?.keys()?.forEach { serial ->
                    val values = candidate.getJSONObject("games").getJSONObject(serial)
                    check(serial.matches(Regex("[A-Za-z0-9_.-]{1,64}")))
                    values.keys().forEach { key ->
                        val original = bundled.getJSONObject("global").get(key)
                        val value = values.get(key)
                        check((original is Number && value is Number) || original.javaClass == value.javaClass)
                    }
                }
                defaults = candidate
            }.onFailure { warnings += "Settings 中的默认配置无效，已使用内置默认项" }
        }
        fun resolved(value: JSONObject): JSONObject = DefaultPaths.resolve(value, privateDll.absolutePath)
        val seed = resolved(bundled.getJSONObject("global"))
        val externalGlobal = resolved(defaults.getJSONObject("global"))
        externalGlobal.keys().forEach { seed.put(it, externalGlobal.get(it)) }
        seed.put("memoryCardSlot1Filename", "slot1.ps2").put("memoryCardSlot2Filename", "slot2.ps2")
        val existing = prefs.getString("config.global", null)
        val global = if (existing == null) seed else JSONObject(existing).apply {
            seed.keys().forEach { if (!has(it)) put(it, seed.get(it)) }
        }
        val edit = prefs.edit().putString("config.global", global.toString())
        // Upstream caches by folder URI alone. Material additions/removals inside the same
        // product folder must invalidate that cache, otherwise cold launch shows an old library.
        val librarySignature = games.sortedBy { it.file.absolutePath }.joinToString("\n") {
            "${it.file.absolutePath}|${it.serial}|${MaterialFiles.fingerprint(it.file)}"
        }
        val libraryChanged = state.get("library.signature") != librarySignature
        if (libraryChanged) edit.remove("gamesCacheKey").remove("gamesCacheDir").remove("gamesCache")
        val profiles = JSONObject(bundled.getJSONObject("games").toString())
        defaults.optJSONObject("games")?.let { extra -> extra.keys().forEach { serial ->
            val merged = profiles.optJSONObject(serial) ?: JSONObject()
            extra.getJSONObject(serial).keys().forEach { merged.put(it, extra.getJSONObject(serial).get(it)) }
            profiles.put(serial, merged)
        } }
        val seeded = mutableListOf<String>()
        profiles.keys().forEach { serial ->
            val key = "config.game.$serial"
            val legacySeed = serial == "SLPS-25837" && prefs.getBoolean("gamepad.ps.defaults.v1", false)
            if (!prefs.contains(key) && state.get("defaults.$serial") == null && !legacySeed && games.any { it.serial == serial })
                edit.putString(key, resolved(profiles.getJSONObject(serial)).toString())
            if (games.any { it.serial == serial }) seeded += serial
        }
        if (!prefs.contains("systemDir")) edit.putString("systemDir", data.absolutePath)
        val tree = DocumentsContract.buildTreeDocumentUri("com.android.externalstorage.documents", "primary:${gameRoot.relativeTo(Environment.getExternalStorageDirectory()).invariantSeparatorsPath}").toString()
        val oldDirs = runCatching { JSONArray(prefs.getString("romsDirs", "[]")) }.getOrDefault(JSONArray())
        val dirs = (0 until oldDirs.length()).map { oldDirs.getString(it) }.filter { raw ->
            val posix = com.armsx2.runtime.MainActivityRuntime.resolveTreeUriToPosix(raw) ?: raw
            !runCatching { MaterialFiles.inside(File(posix), gameRoot) }.getOrDefault(false)
        } + tree
        edit.putString("romsDirs", JSONArray(dirs.distinct()).toString())
        listOf("rendererUpscale", "orientationDriver", "outputScale", "osdDefaultOff", "osdScale65", "blendBasic",
            "folderReconcile", "lowLatencyOff", "affinityPerfCores", "achievementsToSettings").forEach { edit.putBoolean("config.migrated.$it", true) }
        if (errors.isEmpty()) edit.putBoolean("setupComplete", true)
        if (errors.isEmpty()) edit.putBoolean("gamepad.ps.defaults.v1", true)
        check(edit.commit())
        seeded.forEach { state.put("defaults.$it", "seeded") }
        if (libraryChanged) state.put("library.signature", librarySignature)
        val ini = File(data, "PCSX2-Android.ini")
        if (!ini.exists() && bios != null) {
            data.mkdirs()
            ini.writeText(context.assets.open("gamepad-ps/PCSX2-Android.template.ini").bufferedReader().use { it.readText() }
                .replace("\u0024{ROOT}/BIOS", bios.parent!!).replace("\u0024{ROOT}/LSFG/Lossless.dll", privateDll.absolutePath)
                .replace(Regex("(?m)^Slot1_Filename\\s*=.*$"), "Slot1_Filename = slot1.ps2")
                .replace(Regex("(?m)^Slot2_Filename\\s*=.*$"), "Slot2_Filename = slot2.ps2"))
        }
        progress(100, if (errors.isEmpty()) "游戏已准备好" else "需要补充材料")
        return finish()
    }
}
