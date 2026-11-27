package com.optimphone.app

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.text.format.Formatter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class AiActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai)
        prefs = getSharedPreferences("ai_settings", Context.MODE_PRIVATE)

        findViewById<Button>(R.id.btnAiSettings).setOnClickListener {
            startActivity(Intent(this, AiSettingsActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnAnalyze).setOnClickListener { runAnalysis() }
        findViewById<MaterialButton>(R.id.btnTips).setOnClickListener {
            sendPrompt(getString(R.string.ai_tips_prompt), "Analyse générale…")
        }
    }

    private fun collectStats(): String {
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.totalBytes
        val free = stat.availableBytes
        val used = total - free
        val percent = if (total > 0) used * 100 / total else 0

        val mem = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        mem.getMemoryInfo(memInfo)
        val ramPercent = if (memInfo.totalMem > 0)
            (memInfo.totalMem - memInfo.availMem) * 100 / memInfo.totalMem else 0

        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val battery = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        val launchables = packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        ).map { it.activityInfo.packageName }.distinct()

        val installed = packageManager.getInstalledPackages(0)
            .mapNotNull { it.applicationInfo?.packageName }

        val systemApps = installed.count { name ->
            val info = try {
                packageManager.getApplicationInfo(name, 0)
            } catch (e: Exception) {
                null
            }
            info != null && (info.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
        }

        return buildString {
            appendLine("Stockage : ${Formatter.formatFileSize(this@AiActivity, used)} utilisés sur ${Formatter.formatFileSize(this@AiActivity, total)} ($percent %).")
            appendLine("RAM : $ramPercent % utilisée (${Formatter.formatFileSize(this@AiActivity, memInfo.availMem)} disponible).")
            appendLine("Batterie : $battery %.")
            appendLine("Applications installées : ${installed.size}, dont $systemApps applications système.")
            appendLine("Applications lancables : ${launchables.size}.")
            appendLine("Principales applications : ${launchables.take(40).joinToString(", ")}.")
            appendLine("Android SDK : ${android.os.Build.VERSION.SDK_INT}, appareil : ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}.")
        }
    }

    private fun runAnalysis() {
        val stats = collectStats()
        val prompt = "Voici des statistiques de mon téléphone Android :\n$stats\n" +
                "Analyse-les et donne-moi une liste concise et concrète d'actions pour " +
                "libérer de l'espace, améliorer la batterie et la fluidité. Réponds en français, " +
                "par points courts."
        sendPrompt(prompt, "Analyse du téléphone…")
    }

    private fun sendPrompt(prompt: String, placeholder: String) {
        val baseUrl = prefs.getString("base_url", "https://api.mistral.ai/v1")!!.trimEnd('/')
        val apiKey = prefs.getString("api_key", "")!!.trim()
        val model = prefs.getString("model", "mistral-small-latest")!!.trim()

        if (apiKey.isEmpty()) {
            Toast.makeText(this, getString(R.string.ai_need_key), Toast.LENGTH_LONG).show()
            startActivity(Intent(this, AiSettingsActivity::class.java))
            return
        }

        val output = findViewById<TextView>(R.id.aiOutput)
        val button = findViewById<MaterialButton>(R.id.btnAnalyze)
        output.text = placeholder
        button.isEnabled = false

        thread {
            var result: String
            try {
                val body = JSONObject().apply {
                    put("model", model)
                    put("messages", JSONArray().put(
                        JSONObject().put("role", "system")
                            .put("content", "Tu es un assistant d'optimisation pour téléphones Android. Réponds en français, de façon concise et pratique.")
                    ).put(
                        JSONObject().put("role", "user").put("content", prompt)
                    ))
                }
                val connection = URL("$baseUrl/chat/completions").openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 15000
                connection.readTimeout = 60000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
                connection.outputStream.use { it.write(body.toString().toByteArray()) }

                val stream = if (connection.responseCode in 200..299)
                    connection.inputStream else connection.errorStream
                val text = BufferedReader(InputStreamReader(stream)).use { it.readText() }
                if (connection.responseCode !in 200..299) {
                    result = getString(R.string.ai_error, connection.responseCode, text.take(300))
                } else {
                    result = JSONObject(text)
                        .getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("message").getString("content")
                }
                connection.disconnect()
            } catch (e: Exception) {
                result = getString(R.string.ai_error, "—", e.message ?: "erreur inconnue")
            }

            runOnUiThread {
                output.text = result
                button.isEnabled = true
            }
        }
    }
}
