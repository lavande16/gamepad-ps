package com.armsx2.gamepad

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import org.json.JSONObject
import java.io.File

object PsUpgrade {
    fun migrate(context:Context,prefs:SharedPreferences) {
        val storage=Environment.getExternalStorageDirectory()
        val old=File(storage,"GamePad PS");val target=GamePadPsAssembler.root
        if(old.exists()) {
            check(!target.exists()){ "GamePadPS 与旧目录同时存在，请先处理目录冲突" }
            check(old.renameTo(target)){ "无法将 GamePad PS 更名为 GamePadPS" }
        }
        fun paths(s:String)=s.replace("/GamePad PS","/GamePadPS")
            .replace("primary:GamePad PS","primary:GamePadPS").replace("primary%3AGamePad%20PS","primary%3AGamePadPS")
            .replace("/GamePad%20PS","/GamePadPS").replace("\\/GamePad PS","\\/GamePadPS")
        val edit=prefs.edit()
        prefs.all.forEach { (key,value)->
            if(value is String && paths(value)!=value)edit.putString(key,paths(value))
        }
        check(edit.commit())
        val roots=listOf(context.filesDir,context.getExternalFilesDir(null),File(target,"Settings"))
        roots.filterNotNull().filter{it.isDirectory}.forEach {base->
            base.walkTopDown().maxDepth(4).filter{it.isFile && it.length()<2_000_000 && it.extension in setOf("json","ini","xml")}.forEach {file->
                val before=file.readText();val after=paths(before);if(before!=after)file.writeText(after)
            }
        }
        if(!prefs.getBoolean("gamepad.ps.globalDefaults.v2",false)) {
            val values=JSONObject(context.assets.open("gamepad-ps/armsx2-defaults.json").bufferedReader().use{it.readText()}).getJSONObject("global")
            val keys=listOf("upscaleFloat","lsfgEnabled","lsfgMultiplier","lsfgDllPath","osdShowMessages","achievementsNotifications","achievementsLeaderboardNotifications","achievementsOverlays","achievementsLbOverlays","achievementsSoundEffects")
            val global=JSONObject(prefs.getString("config.global","{}")!!)
            keys.forEach {key->global.put(key,if(key=="lsfgDllPath")File(context.filesDir,"lsfg/Lossless.dll").absolutePath else values.get(key))}
            check(prefs.edit().putString("config.global",global.toString()).putBoolean("gamepad.ps.globalDefaults.v2",true).commit())
            val external=File(target,"Settings/armsx2-defaults.json")
            if(external.isFile) {
                val d=JSONObject(external.readText());d.put("rootDirectoryName","GamePadPS")
                keys.forEach {d.getJSONObject("global").put(it,values.get(it))};external.writeText(d.toString(2))
            }
        }
        // Apply only the newly requested display defaults once; keep all other manual choices.
        if(!prefs.getBoolean("gamepad.ps.displayDefaults.v1",false)) {
            val global=JSONObject(prefs.getString("config.global","{}")!!)
            global.put("aspectRatio",0).put("orientation",1)
            check(prefs.edit().putString("config.global",global.toString())
                .putBoolean("gamepad.ps.displayDefaults.v1",true).commit())
            val external=File(target,"Settings/armsx2-defaults.json")
            if(external.isFile) {
                val d=JSONObject(external.readText())
                d.getJSONObject("global").put("aspectRatio",0).put("orientation",1)
                external.writeText(d.toString(2))
            }
        }
        // One-time user-requested unification. Future manual per-game changes remain intact.
        if(!prefs.getBoolean("gamepad.ps.unified.v1",false)) {
            val update=prefs.edit()
            prefs.all.keys.filter {it.startsWith("config.game.")}.forEach{update.remove(it)}
            check(update.putBoolean("gamepad.ps.unified.v1",true).commit())
            val external=File(target,"Settings/armsx2-defaults.json")
            if(external.isFile) {
                val d=JSONObject(external.readText());d.put("games",JSONObject());external.writeText(d.toString(2))
            }
        }
        if(!prefs.getBoolean("gamepad.ps.nativeUnified.v1",false)) {
            // Native per-game INIs also override globals; retire them in this requested reset.
            listOf(context.filesDir,context.getExternalFilesDir(null)).filterNotNull().forEach {base->
                File(base,"gamesettings").listFiles()?.filter{it.isFile && it.extension.equals("ini",true)}?.forEach {
                    check(it.delete()){ "无法清除旧的游戏专属设置：${it.name}" }
                }
                val mirror=File(base,"armsx2-settings.json")
                if(mirror.isFile)mirror.writeText(JSONObject().put("global",JSONObject(prefs.getString("config.global","{}")!!)).toString())
            }
            check(prefs.edit().putBoolean("gamepad.ps.nativeUnified.v1",true).commit())
        }
        if(!prefs.getBoolean("gamepad.ps.forcedDefaults.v041",false)) {
            val update=prefs.edit()
            update.putString("config.global",PsRestoredDefaults.force(JSONObject(prefs.getString("config.global","{}")!!)).toString())
            prefs.all.filterKeys{it.startsWith("config.game.")}.forEach {(key,value)->
                if(value is String)update.putString(key,PsRestoredDefaults.force(JSONObject(value)).toString())
            }
            check(update.commit())
            val external=File(target,"Settings/armsx2-defaults.json")
            if(external.isFile)external.writeText(PsRestoredDefaults.forceDocument(JSONObject(external.readText())).toString(2))
            listOf(context.filesDir,context.getExternalFilesDir(null)).filterNotNull().forEach {base->
                File(base,"gamesettings").listFiles()?.filter{it.isFile && it.extension.equals("ini",true)}?.forEach {ini->
                    ini.writeText(PsRestoredDefaults.forceGameIni(ini.readText()))
                }
                val mirror=File(base,"armsx2-settings.json")
                if(mirror.isFile)mirror.writeText(PsRestoredDefaults.forceDocument(JSONObject(mirror.readText())).toString())
            }
            // Mark only after every configuration layer has been updated. Later manual edits remain valid.
            check(prefs.edit().putBoolean("gamepad.ps.forcedDefaults.v041",true).commit())
        }

    }
}
