package com.terrabreed.app.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.terrabreed.app.MainActivity
import com.terrabreed.app.R
import com.terrabreed.app.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cek apakah sudah ada konfigurasi server
        val prefs = getSharedPreferences("terrabreed_prefs", Context.MODE_PRIVATE)
        val ip = prefs.getString("server_ip", "") ?: ""
        // Set default jika belum ada
        if (ip.isBlank()) {
            prefs.edit().putString("server_ip", "10.10.1.1").putBoolean("use_https", true).apply()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 1500)
    }

    private lateinit var binding: ActivitySplashBinding
}
