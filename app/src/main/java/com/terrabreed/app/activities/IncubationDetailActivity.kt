package com.terrabreed.app.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.terrabreed.app.databinding.ActivityIncubationDetailBinding

class IncubationDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIncubationDetailBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncubationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Detail Inkubasi"
    }
    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
