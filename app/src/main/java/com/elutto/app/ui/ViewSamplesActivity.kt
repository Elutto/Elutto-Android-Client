package com.elutto.app.ui

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.Menu
import android.view.MenuItem
import android.view.View

import com.elutto.app.R
import com.elutto.app.base.BaseActivity

/**
 * Activity demonstrates some GUI functionalities from the Android support library.
 */
class ViewSamplesActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_samples)
        setupToolbar()

        val btnFab = findViewById<View>(R.id.fab)
        btnFab.setOnClickListener { view -> Snackbar.make(view, "Hello Snackbar!", Snackbar.LENGTH_LONG).setAction("Action", null).show() }
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
        return R.id.nav_samples
    }

    override fun providesActivityToolbar(): Boolean {
        return true
    }
}
