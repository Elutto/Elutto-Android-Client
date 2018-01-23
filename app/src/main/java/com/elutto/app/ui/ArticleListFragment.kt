package com.elutto.app.ui

import android.app.ListFragment
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v4.widget.SwipeRefreshLayout
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.target.BitmapImageViewTarget

import com.elutto.app.R
import com.elutto.app.base.App
import com.elutto.app.base.Elutto
import com.elutto.app.event.*
import com.elutto.app.model.ChannelSummary
import org.greenrobot.eventbus.*

import com.elutto.app.util.LogUtil.logI
import com.elutto.app.util.LogUtil.makeLogTag
import org.apmem.tools.layouts.FlowLayout
import org.threeten.bp.LocalDateTime

/**
 * Shows a list of all available apiChannelEvents.
 */
class ArticleListFragment : ListFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listAdapter = ChannelListAdapter(activity)
        setHasOptionsMenu(true)
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)

        // show local data
        EventBus.getDefault().post(ChannelsRefreshDone(false))
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.channels_list_view, container, false)

        swipeToRefreshChannels = v.findViewById<View>(R.id.swipe_to_refresh_channels) as SwipeRefreshLayout
        swipeToRefreshChannels.setOnRefreshListener { App.elutto.pullChannelsFromServer() }

        val btnCreateEvent = v.findViewById<View>(R.id.btn_create_event)
        btnCreateEvent.setOnClickListener { view ->
            Snackbar.make(view, "Create event!", Snackbar.LENGTH_LONG).setAction("Action", null).show()
        }

        // ready to refresh channels
        App.elutto.pullChannelsFromServer()

        return v
    }

    /**
     * Called when an IO event occurs.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    @Suppress("UNUSED_PARAMETER")
    fun onMessageEvent(ev: IoEvent) {
        if (ev.context == IoEventContext.CHANNELS_SYNC) {
            swipeToRefreshChannels.isRefreshing = false
        }
    }

    /**
     * Called when refreshing of channel data completes.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    @Suppress("UNUSED_PARAMETER")
    fun onMessageEvent(event: ChannelsRefreshDone) {
        App.elutto.getChannels {
            // update the list adapter when query-results are available
            (listAdapter as ChannelListAdapter).setChannelData(it)
        }
    }

    /**
     * Called when a user profile was updated from the cloud.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    @Suppress("UNUSED_PARAMETER")
    fun onMessageEvent(event: UserProfileUpdated) {
        App.elutto.getChannels {
            // update the list adapter when query-results are available
            (listAdapter as ChannelListAdapter).setChannelData(it)
        }
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        super.onListItemClick(l, v, position, id)
        // notify about the selected list item
        EventBus.getDefault().post(ChannelDetailsRequested("1"))
    }

    /**
     * Shows avatars of channel participants (up to 6). If user-image is available,
     * uses it, otherwise generates an icon with a capital first-name letter.
     */
    private class ChannelSummaryAvatarAdapter(val context: Context, val glide: RequestManager, var channel: ChannelSummary, skipUserIds: List<String>) : BaseAdapter()
    {
        companion object {
            val MAX_AVATARS = 6
        }

        private val avatars = mutableListOf<Elutto.AvatarHolder>()

        init {
            // if channel has one participant (us), generate a single avatar
            if (channel.participants.size < 2) {
                if (channel.primaryUser.userId !in skipUserIds) {
                    avatars.add(App.elutto.makeAvatar(channel.primaryUser))
                }
            }
            else {
                for (participant in channel.participants) {
                    if (participant.userId !in skipUserIds) {
                        avatars.add(App.elutto.makeAvatar(participant))
                        if (avatars.size >= MAX_AVATARS)
                            break
                    }
                }
            }
        }

        override fun getCount(): Int {
            return avatars.size
        }

        override fun getItem(pos: Int): Any {
            return avatars[pos]
        }

        override fun getItemId(pos: Int): Long {
            return avatars[pos].srcId.hashCode().toLong()
        }

        override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View
        {
            // derive the height of an avatar from the number of avatars we show
            var avatarWidth = context.resources.getDimension(R.dimen.list_item_article_thumbnail).toDouble()
            var avatarHeight = avatarWidth

            // for 2 or less avatars, we shrink avatar by 20%
            // original size
            if (avatars.size <= 2) {
              avatarWidth *= 0.80
              avatarHeight = avatarWidth
            }
            else if (avatars.size > 2) { // shrink more than half to fit two columns
                avatarWidth *= 0.48
                avatarHeight = avatarWidth
            }

            val img: ImageView = (convertView as ImageView?) ?: ImageView(context)
            val imgLayout = FlowLayout.LayoutParams(avatarHeight.toInt(), avatarWidth.toInt())
            img.layoutParams = imgLayout

            val avatar = avatars[pos]

            if (avatar.url == null) {
                img.setImageDrawable(avatar.drawable)
            }
            else {
                glide.load(avatar.url)
                        .asBitmap()
                        .placeholder(avatar.drawable)
                        .fitCenter()
                        .into(object : BitmapImageViewTarget(img) {
                            override fun setResource(resource: Bitmap) {
                                val circularBitmapDrawable = RoundedBitmapDrawableFactory.create(App.elutto.resources, resource)
                                circularBitmapDrawable.isCircular = true
                                img.setImageDrawable(circularBitmapDrawable)
                            }
                        })
            }

            return img
        }
    }

    /**
     * Retrieves channel items from the local DB.
     */
    private class ChannelListAdapter(val context: Context) : BaseAdapter()
    {
        private val glide = Glide.with(context)
        private var channels = emptyList<ChannelSummary>()
        private val avatarWidth = context.resources.getDimension(R.dimen.list_item_article_thumbnail).toInt()

        /** Instead of doing a lookup up of elements of a listview
         * we cache previous search-results in this holder.
         */
        private data class ListViewItemHolder(
            val title: TextView,
            val avatar: ViewGroup,
            val eventInfoSection: View,
            val rsvpExpiry: TextView,
            val channelParticipantNames: TextView,
            val body: TextView,
            val messageAvatar: FlowLayout,
            val lastMsgTm: TextView,
            val msgCount: TextView,
            val thumbsUp: TextView,
            val thumbsDown: TextView
        )

        fun setChannelData(channels: List<ChannelSummary>) {
            this.channels = channels
            notifyDataSetChanged()
        }

        override fun getCount(): Int {
            return channels.size
        }

        override fun getItem(position: Int): Any {
            return channels[position]
        }

        override fun getItemId(position: Int): Long {
            return channels[position].hashCode().toLong()
        }

        override fun getView(position: Int, convertViewOpt: View?, container: ViewGroup): View
        {
            // lookup the item in the model
            val item = channels[position]

            val holder: ListViewItemHolder
            val convertView: View
            // we only inflate the list-item once per row
            if (convertViewOpt == null) {
                convertView = App.elutto.inflater.inflate(R.layout.channels_list_item, container, false)
                holder = ListViewItemHolder(
                        title = convertView.findViewById(R.id.channel_summary_title),
                        avatar = convertView.findViewById(R.id.channel_summary_avatar),
                        eventInfoSection = convertView.findViewById(R.id.channel_summary_event_info_section),
                        rsvpExpiry = convertView.findViewById(R.id.channel_summary_event_rsvp_time),
                        channelParticipantNames = convertView.findViewById(R.id.channel_summary_participant_names),
                        body = convertView.findViewById(R.id.channel_summary_body),
                        messageAvatar = convertView.findViewById(R.id.channel_summary_message_avatar),
                        lastMsgTm = convertView.findViewById(R.id.channel_summary_last_message_tm),
                        msgCount = convertView.findViewById(R.id.channel_summary_message_count),
                        thumbsUp = convertView.findViewById(R.id.channel_summary_thumbs_up_label),
                        thumbsDown = convertView.findViewById(R.id.channel_summary_thumbs_down_label))
                convertView.tag = holder
            }
            else {
                convertView = convertViewOpt
                holder = convertView.tag as ListViewItemHolder
                // clear up avatar placeholders
                holder.messageAvatar.removeAllViews()
                holder.avatar.removeAllViews()
            }

            // Derive title from the event name or the channel name.
            //
            var title = item.title
            var avatar : Elutto.AvatarHolder? = null
            var eventRsvpExpiry : LocalDateTime? = null

            if (item.events.isNotEmpty()) {
                val event = item.events.first()
                // override channel title with the event title
                title = event.title
                // derive avatar from the event icon or from the organizer
                avatar = event.icon?.let { App.elutto.makeAvatar(it) } ?: App.elutto.makeAvatar(event.authorUser)
                eventRsvpExpiry = event.rsvpExpiryTm
            }

            // if event didn't yield an avatar, derive it from the channel's
            // primary user
            if (title != null && avatar == null) {
                avatar = App.elutto.makeAvatar(item.primaryUser)
            }

            // ready to create an avatar
            if (avatar != null) {
                val avatarLayout = holder.avatar.layoutParams
                avatarLayout.height = avatarWidth
                holder.avatar.addView( App.elutto.makeAvatarView(context, avatarLayout, avatar) )
            }

            Elutto.setCollapsableTextView(holder.title, title)
            Elutto.setCollapsableTextView(holder.rsvpExpiry, Elutto.shortRemainingTime(eventRsvpExpiry))

            // On/off the event-info section depending if have an event.
            //
            holder.eventInfoSection.visibility = if (item.events.isNotEmpty()) View.VISIBLE else View.GONE

            // Event accepted/declined counts.
            //
            holder.thumbsUp.text = App.elutto.resources.getString(R.string.channels_list_label_thumbs_up, 7)
            holder.thumbsDown.text = App.elutto.resources.getString(R.string.channels_list_label_thumbs_down, 2)

            // Names of conversation participants
            //
            Elutto.setCollapsableTextView(holder.channelParticipantNames, App.elutto.getUsernames(item.participants, ", "))

            // Unseen messages counts.
            //
            holder.msgCount.text = item.msgCount.toString()

            // Last message in the conversation
            //
            // reset typeface of the msg body
            holder.body.setTypeface(null, Typeface.NORMAL)
            Elutto.setCollapsableTextView(holder.body, item.lastMessage?.msgText)

            // Show last message if available
            var msgAuthorAndTime: String? = null
            if (item.lastMessage != null)
            {
                // Last message timestamp.
                //
                var msgTm = Elutto.shortLocalizedTime(item.lastMessage.msgServerReceivedTm)
                msgAuthorAndTime = App.elutto.resources.getString(R.string.channels_list_message_credit, msgTm, item.lastMessage.author.firstName)

                // Highlight the last message if we haven't seen it yet
                if (item.lastPeekTm == null || item.lastPeekTm < item.lastMessage.msgServerReceivedTm) {
                    holder.body.setTypeface(null, Typeface.BOLD)
                }
            }

            Elutto.setCollapsableTextView(holder.lastMsgTm, msgAuthorAndTime)

            // Avatars of the users we are talking to, minus ourselves or the event organizer
            //
            val skipAvatarsOf = mutableListOf<String>();
            skipAvatarsOf.add(App.elutto.myUserId)
            avatar?.let {
                skipAvatarsOf.add(it.srcId)
            }
            val avatarModel = ChannelSummaryAvatarAdapter(context, glide, item, skipAvatarsOf)
            // if we have more than 2 avatars, we change the linear- to flow-layout
            if (avatarModel.count > 2) {
                holder.messageAvatar.orientation = LinearLayout.HORIZONTAL

            } else {
                holder.messageAvatar.orientation = LinearLayout.VERTICAL
            }
            for (i in 0 until avatarModel.count) {
                holder.messageAvatar.addView(avatarModel.getView(i, null, holder.messageAvatar))
            }

            return convertView
        }
    }

    companion object {
        private val TAG = makeLogTag(ArticleListFragment::class.java)
    }

    private lateinit var swipeToRefreshChannels: SwipeRefreshLayout
}
