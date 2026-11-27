package com.optimphone.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class AiSettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_settings)
        prefs = getSharedPreferences("ai_settings", Context.MODE_PRIVATE)

        val baseUrl = findViewById<EditText>(R.id.inputBaseUrl)
        val apiKey = findViewById<EditText>(R.id.inputApiKey)
        val model = findViewById<EditText>(R.id.inputModel)

        baseUrl.setText(prefs.getString("base_url", "https://api.mistral.ai/v1"))
        apiKey.setText(prefs.getString("api_key", ""))
        model.setText(prefs.getString("model", "mistral-small-latest"))

        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            prefs.edit()
                .putString("base_url", baseUrl.text.toString().trim())
                .putString("api_key", apiKey.text.toString().trim())
                .putString("model", model.text.toString().trim())
                .apply()
            Toast.makeText(this, R.string.ai_saved, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
