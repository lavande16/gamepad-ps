package com.armsx2.gamepad

import org.json.JSONObject

/** The four values explicitly requested for the V0.4.1 one-time global/game migration. */
internal object PsRestoredDefaults {
    fun force(json: JSONObject): JSONObject = json.apply {
        put("textureUpscale",2)
        put("precacheTextureReplacements",true)
        put("gsBackThreadMode",1)
        put("fpsLimit",60)
    }
    fun forceDocument(json: JSONObject): JSONObject = json.apply {
        optJSONObject("global")?.let(::force)
        optJSONObject("games")?.let {games->games.keys().forEach {key->games.optJSONObject(key)?.let(::force)}}
    }
    fun forceGameIni(source: String): String {
        val replacements=mapOf("TextureUpscale" to "2","PrecacheTextureReplacements" to "true","GSBackThreadMode" to "1")
        val lines=source.lines().toMutableList()
        val start=lines.indexOfFirst {it.trim()=="[EmuCore/GS]"}
        if(start<0)return source.trimEnd()+"\n\n[EmuCore/GS]\n"+replacements.entries.joinToString("\n"){"${it.key} = ${it.value}"}+"\n"
        val end=(start+1 until lines.size).firstOrNull {lines[it].trim().startsWith("[")} ?: lines.size
        val pending=replacements.toMutableMap()
        val result=lines.take(start+1).toMutableList()
        for(line in lines.subList(start+1,end)) {
            val key=line.substringBefore('=').trim()
            val value=replacements[key]
            if(value==null)result+=line
            else if(pending.remove(key)!=null)result+="$key = $value"
        }
        result+=pending.entries.map{"${it.key} = ${it.value}"}
        result+=lines.drop(end)
        return result.joinToString("\n").trimEnd()+"\n"
    }
}
