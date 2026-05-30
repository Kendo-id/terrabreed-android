package com.terrabreed.app.activities

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.terrabreed.app.R
import com.terrabreed.app.api.ApiClient
import com.terrabreed.app.databinding.ActivitySettingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Pengaturan"
        loadSettings()
        binding.btnSave.setOnClickListener { saveSettings() }
        binding.btnTest.setOnClickListener { testConnection() }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("terrabreed_prefs", Context.MODE_PRIVATE)
        binding.etServerIp.setText(prefs.getString("server_ip", "10.10.1.1"))
        binding.etServerPort.setText(prefs.getString("server_port", ""))
        binding.etRefreshInterval.setText(prefs.getString("refresh_interval", "5"))
        binding.switchHttps.isChecked = prefs.getBoolean("use_https", true)
    }

    private fun saveSettings() {
        val ip       = binding.etServerIp.text.toString().trim()
        val port     = binding.etServerPort.text.toString().trim()
        val interval = binding.etRefreshInterval.text.toString().trim()
        val useHttps = binding.switchHttps.isChecked

        if (ip.isBlank()) { binding.etServerIp.error = "Wajib diisi"; return }

        getSharedPreferences("terrabreed_prefs", Context.MODE_PRIVATE).edit().apply {
            putString("server_ip", ip)
            putString("server_port", port)
            putString("refresh_interval", interval)
            putBoolean("use_https", useHttps)
            apply()
        }
        ApiClient.invalidate()
        Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
    }

    private fun testConnection() {
        saveSettings()
        binding.btnTest.isEnabled = false
        binding.btnTest.text = "Menguji..."
        lifecycleScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.getApi(this@SettingsActivity).getSensorLatest()
                }
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful) {
                        val s = resp.body()?.sensor
                        Toast.makeText(this@SettingsActivity,
                            "✅ Terhubung! Suhu: ${s?.temp}°C", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@SettingsActivity,
                            "❌ HTTP ${resp.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity,
                        "❌ Gagal: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.btnTest.isEnabled = true
                    binding.btnTest.text = getString(R.string.settings_test_connection)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
