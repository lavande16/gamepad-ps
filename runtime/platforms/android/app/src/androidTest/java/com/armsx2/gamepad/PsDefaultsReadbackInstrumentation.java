package com.armsx2.gamepad;

import android.app.Instrumentation;
import android.os.Bundle;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.io.File;
import java.nio.file.Files;

/** Read-only, same-signature probe: verifies only the four requested fields without device root. */
public final class PsDefaultsReadbackInstrumentation extends Instrumentation {
    private final String[] keys={"textureUpscale","precacheTextureReplacements","gsBackThreadMode","fpsLimit"};
    @Override public void onCreate(Bundle arguments) {super.onCreate(arguments);start();}
    private JSONObject fields(String source) throws Exception {
        JSONObject from=new JSONObject(source),out=new JSONObject();
        for(String key:keys)if(from.has(key))out.put(key,from.get(key));
        return out;
    }
    @Override public void onStart() {
        Bundle result=new Bundle();
        try {
            SharedPreferences p=getTargetContext().getSharedPreferences("ARMSX2",0);
            JSONObject prefs=new JSONObject();
            prefs.put("config.global",fields(p.getString("config.global","{}")).toString());
            for(String key:p.getAll().keySet())if(key.startsWith("config.game."))prefs.put(key,fields(p.getString(key,"{}")).toString());
            prefs.put("gamepad.ps.forcedDefaults.v041",p.getBoolean("gamepad.ps.forcedDefaults.v041",false));
            JSONObject nativeFiles=new JSONObject();
            for(File base:new File[]{getTargetContext().getFilesDir(),getTargetContext().getExternalFilesDir(null)}) {
                if(base==null)continue;
                File[] files=new File(base,"gamesettings").listFiles();if(files==null)continue;
                for(File f:files)if(f.isFile()&&f.getName().endsWith(".ini")) {
                    StringBuilder value=new StringBuilder();
                    for(String line:Files.readAllLines(f.toPath()))if(line.matches("\\s*(TextureUpscale|PrecacheTextureReplacements|GSBackThreadMode)\\s*=.*"))value.append(line).append('\n');
                    nativeFiles.put(base.getName()+"/"+f.getName(),value.toString());
                }
            }
            JSONObject report=new JSONObject().put("prefs",prefs).put("nativeGameIni",nativeFiles);
            result.putString("stream", "PS_DEFAULTS_JSON="+report.toString()+"\n");finish(-1,result);
        } catch(Throwable t) {result.putString("stream",android.util.Log.getStackTraceString(t));finish(0,result);}
    }
}
