// SPDX-License-Identifier: GPL-3.0-or-later
package com.armsx2.gamepad

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.armsx2.Main
import com.armsx2.R
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** PS2 backend for the directly reused NS assembly panel. The full NS shell follows in stage 4. */
class GamePadPsAssemblyActivity : ComponentActivity() {
    private var running = false
    private val permission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { route() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gamepad_ps_assembly)
        findViewById<View>(R.id.button_permission).setOnClickListener {
            permission.launch(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")))
        }
        findViewById<View>(R.id.button_retry).setOnClickListener { route() }
        route()
    }

    private fun route() {
        if (running) return
        val allowed = Environment.isExternalStorageManager()
        findViewById<View>(R.id.button_permission).visibility = if (allowed) View.GONE else View.VISIBLE
        findViewById<View>(R.id.button_retry).visibility = View.GONE
        if (!allowed) {
            findViewById<TextView>(R.id.text_assembly_title).text = "需要存储授权"
            findViewById<TextView>(R.id.text_assembly_detail).text = "授权后可自动读取 GamePadPS 中的游戏材料和存档。"
            return
        }
        running = true
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    GamePadPsAssembler.assemble(applicationContext, getSharedPreferences("ARMSX2", MODE_PRIVATE)) { percent, title ->
                        runOnUiThread {
                            findViewById<LinearProgressIndicator>(R.id.progress_assembly).setProgressCompat(percent, true)
                            findViewById<TextView>(R.id.text_progress_percent).text = "$percent%"
                            findViewById<TextView>(R.id.text_assembly_title).text = title
                        }
                    }
                }.getOrElse { GamePadPsAssembler.Result(false, 0, listOf(it.message ?: "材料读取失败，请重新检查")) }
            }
            running = false
            findViewById<TextView>(R.id.text_eyebrow).text = if (result.ready) "装配完成" else "检查游戏材料"
            findViewById<TextView>(R.id.text_assembly_detail).text = result.messages.joinToString("\n").ifBlank { "已准备 ${result.games} 款游戏" }
            if (result.ready) {
                if (result.messages.isNotEmpty()) com.armsx2.gamepad.PsNotices.makeText(
                    applicationContext, result.messages.first(), android.widget.Toast.LENGTH_LONG).show()
                startActivity(Intent(this@GamePadPsAssemblyActivity, Main::class.java).apply {
                    intent?.data?.let { data = it }
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                })
                finish()
            } else findViewById<View>(R.id.button_retry).visibility = View.VISIBLE
        }
    }
}
