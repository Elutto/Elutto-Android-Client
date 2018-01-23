package com.elutto.app.ui

import com.elutto.app.event.ChannelDetailsRequested
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ListView

import com.elutto.app.R
import com.elutto.app.base.BaseActivity
import com.elutto.app.util.LogUtil
import org.greenrobot.eventbus.*

/**
 * Lists all available quotes. This Activity supports a single pane (= smartphones) and a two pane mode (= large screens with >= 600dp width).
 */
class ListActivity : BaseActivity() {
    /**
     * Whether or not the activity is running on a device with a large screen
     */
    private var twoPaneMode: Boolean = false

    /**
     * Is the container present? If so, we are using the two-pane layout.
     *
     * @return true if the two pane layout is used.
     */
    private val isTwoPaneLayoutUsed: Boolean
        get() = findViewById<View>(R.id.article_detail_container) != null

    /**
     * Subscribe to event broker.
     */
    public override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    /**
     * Unsubscribe from event broker.
     */
    public override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        setupToolbar()

        if (isTwoPaneLayoutUsed) {
            twoPaneMode = true
            LogUtil.logD("TEST", "TWO PANE")
            enableActiveItemState()
        }

        if (savedInstanceState == null && twoPaneMode) {
            setupDetailFragment()
        }
    }

    /**
     * Called when an item has been selected
     *
     * @param event selected quote ID
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: ChannelDetailsRequested) {
        if (twoPaneMode) {
            // Show the quote detail information by replacing the DetailFragment via transaction.
            val fragment = ArticleDetailFragment.newInstance(event.itemId)
            fragmentManager.beginTransaction().replace(R.id.article_detail_container, fragment).commit()
        } else {
            // Start the detail activity in single pane mode.
            val detailIntent = Intent(this, ArticleDetailActivity::class.java)
            detailIntent.putExtra(ArticleDetailFragment.ARG_ITEM_ID, event.itemId)
            startActivity(detailIntent)
        }
    }

    private fun setupToolbar() {
        val ab = actionBarToolbar
        ab.setHomeAsUpIndicator(R.drawable.ic_menu)
        ab.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupDetailFragment() {
        val fragment = ArticleDetailFragment.newInstance("1")
        fragmentManager.beginTransaction().replace(R.id.article_detail_container, fragment).commit()
    }

    /**
     * Enables the functionality that selected items are automatically highlighted.
     */
    private fun enableActiveItemState() {
        val fragmentById = fragmentManager.findFragmentById(R.id.article_list) as ArticleListFragment
        fragmentById.listView.choiceMode = ListView.CHOICE_MODE_SINGLE
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
        return R.id.nav_quotes
    }

    override fun providesActivityToolbar(): Boolean {
        return true
    }
}
