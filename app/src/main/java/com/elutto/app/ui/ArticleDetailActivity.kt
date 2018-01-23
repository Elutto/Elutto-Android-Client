package com.elutto.app.ui

import android.os.Bundle

import com.elutto.app.R
import com.elutto.app.base.BaseActivity

/**
 * Simple wrapper for [ArticleDetailFragment]
 * This wrapper is only used in single pan mode (= on smartphones)
 */
class ArticleDetailActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        // Show the Up button in the action bar.
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        val fragment = ArticleDetailFragment.newInstance(intent.getStringExtra(ArticleDetailFragment.ARG_ITEM_ID))
        fragmentManager.beginTransaction().replace(R.id.article_detail_container, fragment).commit()
    }

    override fun providesActivityToolbar(): Boolean {
        return false
    }
}
