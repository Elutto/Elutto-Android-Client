package com.elutto.app.ui

import android.app.Fragment
import android.os.Bundle
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import com.bumptech.glide.Glide

import com.elutto.app.R
import com.elutto.app.base.BaseActivity

/**
 * Shows the quote detail page.
 */
class ArticleDetailFragment : Fragment() {

    class DummyItem(val id: String, val photoId: Int, val title: String, val author: String, val content: String)

    private var quote: TextView? = null
    private var author: TextView? = null
    private var backdropImg: ImageView? = null
    private var collapsingToolbar: CollapsingToolbarLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments.containsKey(ARG_ITEM_ID)) {
            // load dummy item by using the passed item ID.
            // dummyItem = ApiChannelEventSummary.ITEM_MAP[arguments.getString(ARG_ITEM_ID)]
        }

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_article_detail, container, false)

        if (!(activity as BaseActivity).providesActivityToolbar()) {
            // No Toolbar present. Set include_toolbar:
            (activity as BaseActivity).setToolbar(rootView.findViewById<View>(R.id.toolbar) as Toolbar)
        }

        // bindings
        quote = rootView.findViewById<View>(R.id.quote) as TextView
        author = rootView.findViewById<View>(R.id.author) as TextView
        backdropImg = rootView.findViewById<View>(R.id.backdrop) as ImageView
        collapsingToolbar = rootView.findViewById<View>(R.id.collapsing_toolbar) as CollapsingToolbarLayout

        if (dummyItem != null) {
            loadBackdrop()
            collapsingToolbar!!.title = dummyItem!!.title
            author!!.text = dummyItem!!.author
            quote!!.text = dummyItem!!.content
        }

        return rootView
    }

    private fun loadBackdrop() {
        // Glide.with(this).load(dummyItem!!.photoId).centerCrop().into(backdropImg!!)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.sample_actions, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings ->
                // your logic
                return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {

        /**
         * The argument represents the dummy item ID of this fragment.
         */
        val ARG_ITEM_ID = "item_id"

        /**
         * The dummy content of this fragment.
         */
        val dummyItem: DummyItem = DummyItem("1", R.drawable.guest_avatar, "Quote #1", "Steve Jobs", "Focusing is about saying No.")

        fun newInstance(itemID: String): ArticleDetailFragment {
            val fragment = ArticleDetailFragment()
            val args = Bundle()
            args.putString(ArticleDetailFragment.ARG_ITEM_ID, itemID)
            fragment.arguments = args
            return fragment
        }
    }
}
