package com.terrabreed.app

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationBarView
import com.terrabreed.app.activities.AIVoiceCallActivity
import com.terrabreed.app.activities.SettingsActivity
import com.terrabreed.app.databinding.ActivityMainBinding
import com.terrabreed.app.fragments.AiChatFragment
import com.terrabreed.app.fragments.ControlFragment
import com.terrabreed.app.fragments.DashboardFragment
import com.terrabreed.app.fragments.IncubationFragment

class MainActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {

    private lateinit var binding: ActivityMainBinding

    private val dashboardFragment  = DashboardFragment()
    private val controlFragment    = ControlFragment()
    private val aiChatFragment     = AiChatFragment()
    private val incubationFragment = IncubationFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = ""

        if (savedInstanceState == null) {
            loadFragment(dashboardFragment)
        }

        binding.bottomNav.setOnItemSelectedListener(this)

        binding.fabVoice.setOnClickListener {
            startActivity(Intent(this, AIVoiceCallActivity::class.java))
        }

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
                else -> false
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nav_dashboard  -> { loadFragment(dashboardFragment);  setToolbarTitle("TerraBreed 🥚"); true }
            R.id.nav_control    -> { loadFragment(controlFragment);    setToolbarTitle("Kontrol ⚙️"); true }
            R.id.nav_ai_chat    -> { loadFragment(aiChatFragment);     setToolbarTitle(""); true }
            R.id.nav_incubation -> { loadFragment(incubationFragment); setToolbarTitle("Inkubasi 🐣"); true }
            else -> false
        }
    }

    private fun setToolbarTitle(title: String) {
        supportActionBar?.title = title
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
