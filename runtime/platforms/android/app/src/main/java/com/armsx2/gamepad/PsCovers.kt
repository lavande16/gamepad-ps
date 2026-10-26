package com.armsx2.gamepad

import android.graphics.BitmapFactory
import android.widget.ImageView
import coil.load
import com.armsx2.GameInfo
import com.armsx2.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/** Artwork is keyed by the disc serial, never the translated title or patched CRC. */
object PsCovers {
    private val bitmaps=object:android.util.LruCache<String,android.graphics.Bitmap>(24*1024*1024) {
        override fun sizeOf(key:String,value:android.graphics.Bitmap)=value.byteCount
    }
    private val locks=ConcurrentHashMap<String,Any>()
    fun candidates(serial:String):List<String> {
        if (!serial.matches(Regex("[A-Z]{4}-[0-9]{5}"))) return emptyList()
        return listOf(
            "https://raw.githubusercontent.com/xlenore/ps2-covers/main/covers/default/$serial.jpg",
            "https://cdn.jsdelivr.net/gh/xlenore/ps2-covers@main/covers/default/$serial.jpg",
        )
    }
    private fun valid(file:File):Boolean = file.isFile && runCatching {
        val options=BitmapFactory.Options().apply{inJustDecodeBounds=true}
        BitmapFactory.decodeFile(file.path,options);options.outWidth>0&&options.outHeight>0
    }.getOrDefault(false)
    fun existing(game:GameInfo):File? {
        val path=if(game.uri.scheme=="file")game.uri.path else game.uri.toString()
        val directory=path?.let {GamePadPsAssembler.gameForPath(it)?.directory}
        val local=directory?.let {d->listOf("cover.jpg","cover.png","cover.webp").map{File(d,it)}.firstOrNull(::valid)}
        return local ?: game.serial?.let{File(GamePadPsAssembler.root,"Covers/$it.jpg").takeIf(::valid)}
    }
    suspend fun missing(game:GameInfo):File? = withContext(Dispatchers.IO) {
        existing(game)?.let{return@withContext it}
        val serial=game.serial ?:return@withContext null
        synchronized(locks.getOrPut(serial){Any()}) {
            existing(game)?.let{return@synchronized it}
            val target=File(GamePadPsAssembler.root,"Covers/$serial.jpg");target.parentFile!!.mkdirs()
            for(url in candidates(serial)) {
                val temp=File(target.parentFile,".$serial.download")
                try {
                    val connection=URL(url).openConnection() as HttpURLConnection
                    try {
                        connection.connectTimeout=6000;connection.readTimeout=8000;connection.setRequestProperty("User-Agent","GamePadPS/0.3.1")
                        check(connection.responseCode==200){"HTTP ${connection.responseCode}"}
                        connection.inputStream.use{input->temp.outputStream().use{out->
                            val buffer=ByteArray(16384);var total=0
                            while(true){val n=input.read(buffer);if(n<0)break;total+=n;check(total<=8*1024*1024);out.write(buffer,0,n)}
                        }}
                        check(valid(temp)){"Invalid image"};check(temp.renameTo(target));android.util.Log.i("GamePadPS-Cover","Cached $serial from $url")
                        return@synchronized target
                    }finally{connection.disconnect()}
                }catch(e:Exception){android.util.Log.w("GamePadPS-Cover","$serial: ${e.message}")}
                finally{temp.delete()}
            }
            null
        }
    }
    fun show(image:ImageView,game:GameInfo) {
        val file=existing(game)
        if(file==null){image.setImageResource(R.drawable.gamepad_ps_launcher_icon);return}
        val key="${file.absolutePath}:${file.length()}:${file.lastModified()}"
        val bitmap=bitmaps.get(key) ?: android.graphics.BitmapFactory.decodeFile(file.absolutePath)?.also{bitmaps.put(key,it)}
        if(bitmap!=null) image.setImageBitmap(bitmap) else image.setImageResource(R.drawable.gamepad_ps_launcher_icon)
    }
}
