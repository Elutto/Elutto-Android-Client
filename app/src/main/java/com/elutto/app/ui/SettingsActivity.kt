package com.elutto.app.ui

import android.os.Bundle
import android.preference.PreferenceFragment
import android.support.v7.app.ActionBar
import android.view.Menu
import android.view.MenuItem

import com.elutto.app.R
import com.elutto.app.base.BaseActivity

/**
 * This Activity provides several settings. Activity contains [PreferenceFragment] as inner class.
 */
class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setupToolbar()
    }

    private fun setupToolbar() {
        val ab = actionBarToolbar
        ab.setHomeAsUpIndicator(R.drawable.ic_menu)
        ab.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.sample_actions, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                openDrawer()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun getSelfNavDrawerItem(): Int {
        return R.id.nav_settings
    }

    override fun providesActivityToolbar(): Boolean {
        return true
    }

    class SettingsFragment : PreferenceFragment() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.settings_prefs)
        }
    }
}
