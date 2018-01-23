package com.elutto.app.base

import android.app.Service
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.support.text.emoji.widget.EmojiTextView
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.elutto.app.event.ChannelsRefreshDone
import com.elutto.app.event.IoEvent
import com.elutto.app.event.IoEventContext
import com.elutto.app.event.UserProfileUpdated
import com.elutto.app.model.*
import com.elutto.app.util.LogUtil.logE
import com.elutto.app.util.LogUtil.logI
import com.elutto.app.util.LogUtil.makeLogTag
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.jakewharton.threetenabp.AndroidThreeTen
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import okhttp3.*
import org.aaronhe.threetengson.ThreeTenGsonAdapter
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.coroutines.experimental.bg
import org.threeten.bp.Duration
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap


class Elutto (context: Context)
{
    // JSON parser
    var gson: Gson

    // Application database
    val db = SqlDbStore(context)

    // Application layout inflater
    val inflater = context.getSystemService(Service.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    // Application resources
    val resources : Resources = context.resources

    // my own user ID
    val myUserId = "10"

    // generator of icons from text
    private val iconify = TextIconGenerator()

    init {
        // Initialize timezone data
        AndroidThreeTen.init(context)
        val builder = GsonBuilder()
        this.gson = ThreeTenGsonAdapter.registerAll(builder).create()
    }

    /**
     * Creates an avatar from a user profile
     */
    fun makeAvatar(user: UserProfile) : AvatarHolder {
        // use the first component of a user profile which isn't empty
        val component = arrayOf(user.firstName, user.username, user.lastName, user.userId).find { it.isNotBlank() }
        return AvatarHolder(user.userId, user.avatar, iconify.getIconRound(component!!.take(1), user.userId), null)
    }

    /**
     * Creates an avatar from a URI string
     */
    fun makeAvatar(uri: String) : AvatarHolder {
        var res : AvatarHolder? = null
        if (uri.startsWith("http")) {
            res = AvatarHolder(uri, uri, null, null)
        }
        else if (uri.startsWith("emoji://")) {
            val emojiCode = uri.substring(8)
            if (emojiCode.isNotEmpty()) {
                res = AvatarHolder(uri, null, null, emojiCode)
            }
        }

        if (res == null)
            throw IllegalArgumentException("Cannot derive avatar from URI: $uri")

        return res
    }

    /**
     * Pulls channel information from the server.
     * Upon success, updates DB and posts an event to let the models refresh their views.
     */
    fun pullChannelsFromServer() {
        // TODO: throttle network calls
        eluttoGet(urlEluttoBase+"/channels", object:Callback {
            override fun onFailure(call: Call?, e: IOException?) {
                EventBus.getDefault().post(IoEvent(getUtcNow(), IoEventContext.CHANNELS_SYNC, false, e.toString()))
                logE(TAG, e.toString())
            }

            override fun onResponse(call: Call?, response: Response?) {
                if (response?.isSuccessful == true)
                {
                    EventBus.getDefault().post(IoEvent(getUtcNow(), IoEventContext.CHANNELS_SYNC, true))
                    response.body().use {
                        val channels = App.elutto.gson.fromJson(it!!.charStream(), Array<ApiChannelSummary>::class.java)
                        logI(TAG, "Channels parsed: "+channels.size)
                        // store in database
                        db.writable {
                            channels.forEach { chl ->
                                val mostRecentMsg: ApiChannelMessage? = chl.recentMsg.getOrNull(0)
                                modify(SqlDbStore.SQL_CHANNEL_UPDATE,
                                       chl.channelId,
                                       chl.createTm,
                                       chl.msgCount,
                                        if (chl.isPublic) "1" else "0",
                                       channelNotifySettingsToMask(chl.myNotifySettings),
                                       channelPrivilegesToMask(chl.myChannelPrivileges),
                                       chl.myChannelRank,
                                       chl.primaryUserId,
                                       chl.themeColor,
                                       chl.themeImage,
                                       chl.title,
                                       mostRecentMsg?.msgId,
                                       mostRecentMsg?.authorId,
                                       mostRecentMsg?.receivedTm,
                                       mostRecentMsg?.msgText,
                                       mostRecentMsg?.msgImage)

                                // Delete existing channel participants in preparation for
                                // inserting the latest list of participants. This is needed
                                // in case someone was banned and no longer belongs to the channel.
                                modify(SqlDbStore.SQL_CHANNEL_PARTICIPANTS_DROP, chl.channelId)
                                chl.participants.forEach { party ->
                                    modify(SqlDbStore.SQL_CHANNEL_PARTICIPANT_ADD,
                                        chl.channelId,
                                        party.userId,
                                        party.channelJoinTm,
                                        party.userRank,
                                        channelPrivilegesToMask(party.channelPrivileges))
                                } // loop: participants

                                // Similarly, replace channel events in preparation for
                                // inserting the latest list of events.
                                modify(SqlDbStore.SQL_CHANNEL_EVENTS_DECLARATIONS_DROP, chl.channelId)
                                modify(SqlDbStore.SQL_CHANNEL_EVENTS_DROP, chl.channelId)

                                chl.events.forEach { ev ->
                                    modify(SqlDbStore.SQL_CHANNEL_EVENT_ADD,
                                            ev.eventId,
                                            chl.channelId,
                                            ev.authorUserId,
                                            ev.acceptedCount,
                                            ev.deniedCount,
                                            ev.maxAllowedUsers,
                                            ev.minRequiredUsers,
                                            ev.rsvpExpiryTm,
                                            ev.rsvpStartTm,
                                            ev.title,
                                            ev.underlyingEventLocation,
                                            ev.underlyingEventStartTm,
                                            ev.icon)

                                    // Save event declarations
                                    ev.declarations.forEach { ev_decl ->
                                        modify(SqlDbStore.SQL_CHANNEL_EVENT_DECLARATION_ADD,
                                                ev.eventId,
                                                chl.channelId,
                                                ev_decl.userId,
                                                ev_decl.declResponse,
                                                ev_decl.declTime)
                                    }
                                } // loop: events
                            } // loop: channels
                        }
                        // let views know to update
                        EventBus.getDefault().post(ChannelsRefreshDone(true))
                    }
                }
                else {
                    EventBus.getDefault().post(
                        IoEvent(getUtcNow(), IoEventContext.CHANNELS_SYNC, false, eluttoError(response)))
                }
            }
        })
    }

    /**
     *  Concatenate participant names, excluding ourselves.
     */
    fun getUsernames(participants: List<UserProfile>, delim: String) : String? {
        if (participants.isEmpty())
            return null

        var sb = StringBuilder()
        participants.forEach {
            if (myUserId != it.userId) {
                if (sb.isNotBlank()) {
                    sb.append(delim)
                }
                sb.append(it.firstName)
            }
        }
        return sb.toString()
    }

    /**
     * Resolves a user profile from cache, DB or remote server.
     */
    private fun resolveUserProfile(userId: String): UserProfile
    {
        // look up in memory
        var resolvedUser = userCache.get( userId )

        // look up in database
        if (resolvedUser == null) {
            db.readable {
                query(SqlDbStore.SQL_USER_PROFILE_RETRIEVE, userId).use {
                    if (it.moveToNext()) {
                        val justResolvedUser = UserProfile(
                            userId = userId,
                            username = it.getString(0),
                            firstName = it.getString(1),
                            lastName = it.getString(2),
                            middleName = it.getString(3),
                            avatar = it.getString(4),
                            circle = it.getString(5))

                        userCache.put(userId, justResolvedUser)
                        resolvedUser = justResolvedUser
                    }
                }
            }
        }

        // User not found in database nor memory.
        // Return placeholder user info, request user from server
        if (resolvedUser == null) {
            resolvedUser = UserProfile(
                userId = userId,
                username = "?",
                firstName = "?",
                lastName = "",
                middleName = null,
                avatar = null,
                circle = null)

            // don't fetch if call already in progress
            if (!userPendingResolve.containsKey(userId)) {

                // mark we are working on this user
                userPendingResolve.put(userId, true)

                eluttoGet(urlEluttoBase + "/users/$userId", object : Callback {
                    override fun onFailure(call: Call?, e: IOException?) {
                        userPendingResolve.remove(userId)
                        logE(TAG, e.toString())
                    }

                    override fun onResponse(call: Call?, response: Response?) {
                        userPendingResolve.remove(userId)
                        if (response?.isSuccessful == true) {
                            response.body().use {
                                val apiUserProfile = App.elutto.gson.fromJson(it!!.charStream(), ApiUserProfile::class.java)

                                val justResolvedUser = UserProfile(
                                        userId = userId,
                                        username = apiUserProfile.username,
                                        firstName = apiUserProfile.firstName,
                                        lastName = apiUserProfile.lastName,
                                        middleName = apiUserProfile.middleName,
                                        avatar = apiUserProfile.avatar,
                                        circle = apiUserProfile.circle)

                                // add to in-memory cache
                                userCache.put(userId, justResolvedUser)

                                // store in database
                                db.writable {
                                    modify(SqlDbStore.SQL_USER_PROFILE_UPDATE,
                                           justResolvedUser.userId,
                                           justResolvedUser.username,
                                           justResolvedUser.firstName,
                                           justResolvedUser.lastName,
                                           justResolvedUser.middleName,
                                           justResolvedUser.avatar,
                                           justResolvedUser.circle)
                                }
                                // let views know to update if nothing else is pending
                                if (userPendingResolve.isEmpty())
                                    EventBus.getDefault().post(UserProfileUpdated())
                            }
                        }
                    }
                })
            }
        }
        return resolvedUser!!
    }

    /**
     * Makes a View from an Avatar Holder.
     */
    fun makeAvatarView(context: Context, layout: ViewGroup.LayoutParams, avatar: AvatarHolder): View {
        var res: View
        // first check if dealing with a remote image
        if (avatar.url != null) {
            val glide = Glide.with(context)
            val imgView = ImageView(context)
            imgView.layoutParams = layout
            val bld = glide.load(avatar.url).asBitmap()
            if (avatar.drawable != null) {
                bld.placeholder(avatar.drawable)
            }
            bld.fitCenter()
               .into(object : BitmapImageViewTarget(imgView) {
                    override fun setResource(resource: Bitmap) {
                        val circularBitmapDrawable = RoundedBitmapDrawableFactory.create(resources, resource)
                        circularBitmapDrawable.isCircular = true
                        imgView.setImageDrawable(circularBitmapDrawable)
                    }
                })
            res = imgView

        } // a drawable?
        else if(avatar.drawable != null) {
            val imgView = ImageView(context)
            imgView.layoutParams = layout
            imgView.setImageDrawable(avatar.drawable)
            res = imgView
        } // an emoji?
        else if (avatar.emoji != null) {
            val txtView = EmojiTextView(context)
            txtView.layoutParams = layout
            txtView.text = avatar.emoji
            txtView.setTextSize(TypedValue.COMPLEX_UNIT_PX, layout.height.toFloat()*0.7f)
            res = txtView
        }
        else {
            throw IllegalArgumentException("Avatar not a URL, drawable nor emoji")
        }
        return res
    }

    /**
     * Retrieves channel info from the local store.
     */
    fun getChannels(onDone: (List<ChannelSummary>) -> Unit)
    {
        // Use a background thread and call onDone when result ready
        async(UI)
        {
            val res = bg {
                val items = mutableListOf<ChannelSummary>()
                db.readable {
                    val participants = mutableMapOf<String, MutableList<UserProfile>>()
                    query(SqlDbStore.SQL_CHANNELS_PARTICIPANTS_RETRIEVE).use {
                        while (it.moveToNext()) {
                            val channelId = it.getString(0)
                            val participantUserId = it.getString(1)
                            val participant = resolveUserProfile(participantUserId)

                            // add participant to the channel's list of participants
                            participants.getOrPut(channelId, { mutableListOf() }).add(participant)
                        }
                    }

                    // maps event IDs -> user participation declarations
                    val channelEventDeclarations = mutableMapOf<String, MutableList<ChannelEventDeclaration>>()
                    query(SqlDbStore.SQL_CHANNELS_EVENTS_DECLARATIONS_RETRIEVE).use {
                        while (it.moveToNext()) {
                            val eventId = it.getString(0)
                            val eventDecl = ChannelEventDeclaration(
                                    user = resolveUserProfile(it.getString(1)),
                                    declResponse = it.getString(2),
                                    declTm = db.fromLiteDatetime(it.getString(3))!!)

                            // add declaration to the event
                            channelEventDeclarations.getOrPut(eventId, { mutableListOf() }).add(eventDecl)
                        }
                    }

                    // maps channel IDs -> event
                    val channelEvents = mutableMapOf<String, MutableList<ChannelEvent>>()
                    query(SqlDbStore.SQL_CHANNELS_EVENTS_RETRIEVE).use {
                        while (it.moveToNext()) {
                            val channelId = it.getString(0)
                            val eventId = it.getString(1)
                            val channelEvent = ChannelEvent(
                                    eventId = eventId,
                                    authorUser = resolveUserProfile(it.getString(2)),
                                    acceptedCount = it.getInt(3),
                                    deniedCount = it.getInt(4),
                                    maxAllowedUsers = it.getInt(5),
                                    minRequiredUsers = it.getInt(6),
                                    rsvpExpiryTm = db.fromLiteDatetime(it.getString(7))!!,
                                    rsvpStartTm = db.fromLiteDatetime(it.getString(8))!!,
                                    title = it.getString(9),
                                    underlyingEventLocation = it.getString(10),
                                    underlyingEventStartTm = db.fromLiteDatetime(it.getString(11)),
                                    icon = it.getString(12),
                                    declarations = channelEventDeclarations[eventId] ?: emptyList())

                            // add declarations to the event's list of declarations
                            channelEvents.getOrPut(channelId, { mutableListOf() }).add(channelEvent)
                        }
                    }

                    query(SqlDbStore.SQL_CHANNELS_SUMMARIES_RETRIEVE).use { cur ->
                        while (cur.moveToNext()) {
                            val channelId = cur.getString(0)
                            // if the last msg ID is not null, we create the message instance
                            val lastMsg = cur.getString(12)?.let { lastMsgId ->

                                val authorId = cur.getString(13)
                                ChannelMessage(
                                        msgId = lastMsgId,
                                        author = resolveUserProfile(authorId),
                                        msgServerReceivedTm = db.fromLiteDatetime(cur.getString(14))!!,
                                        msgText = cur.getString(15),
                                        msgImage = cur.getString(16) )
                            }

                            val channelPrimaryUserId = cur.getString(7)
                            val item = ChannelSummary(
                                    channelId = channelId,
                                    createTm = db.fromLiteDatetime(cur.getString(1))!!,
                                    isPublic = cur.getInt(2) != 0,
                                    msgCount = cur.getInt(3),
                                    myChannelNotifySettings = cur.getInt(4),
                                    myChannelPrivileges = cur.getInt(5),
                                    myChannelRank = cur.getInt(6),
                                    primaryUser = resolveUserProfile(channelPrimaryUserId),
                                    themeColor = cur.getString(8),
                                    themeImage = cur.getString(9),
                                    title = cur.getString(10),
                                    lastPeekTm = db.fromLiteDatetime(cur.getString(11)),
                                    lastMessage = lastMsg,
                                    participants = participants[channelId] ?: emptyList(),
                                    events = channelEvents[channelId] ?: emptyList()
                            )
                            items.add(item)
                        }
                    } // close cursor
                }
                return@bg items // result from our background-lambda
            }
            // pass the result from the closure via the UI thread.
            onDone(res.await())
        }
    }

    /**
     *  Sends a GET request (with credentials) to Elutto servers.
     */
    private fun eluttoGet(url: String, callback: Callback)
    {
        logI(TAG, "[GET] "+url)
        val request = Request.Builder()
                .header("Accept", "application/json")
                .url(url)

        if (eluttoAuthToken != null) {
            request.header("Authorization", "Bearer "+eluttoAuthToken)
        }

        httpElutto.newCall(request.build()).enqueue(callback)
    }

    /**
     * Extracts error text from the HTTP/JSON response.
     */
    private fun eluttoError(response: Response?): String
    {
        if (response == null)
            return "Empty response"

        try {
            response.body().use {
                val el = JsonParser().parse(it!!.charStream()).asJsonObject
                return el.getAsJsonPrimitive("message").asString
            }
        } catch (e: Exception) {
            return e.toString()
        }
    }

    /**
     * Translates an array of channel privileges to a bitmask.
     */
    private fun channelPrivilegesToMask(privileges: List<String>): Int
    {
        var mask = 0
        privileges.forEach { v ->
            mask += when (v)
            {
                "KICK" -> 0x40
                "THEME" -> 0x20
                "TITLE" -> 0x10
                "INVITE" -> 0x8
                "WRITE" -> 0x04
                "SPECTATE" -> 0x02
                else -> 0
            }
        }
        return mask
    }

    /**
     * Translates an array of channel notification flags to a bitmask.
     */
    private fun channelNotifySettingsToMask(notifySettings: List<String>): Int
    {
        var mask = 0
        notifySettings.forEach { v ->
            mask += when (v)
            {
                "EVNT_LFCE" -> 0x08
                "EVNT_DECL" -> 0x04
                "CHNL_MBR" -> 0x02
                "CHNL_MSG" -> 0x01
                else -> 0
            }
        }
        return mask
    }

    // Types of avatars:
    //  * a user photo from an external repository
    //  * a generated icon based on user name
    //  * an emoji
    data class AvatarHolder(
        val srcId: String,
        val url: String?,
        val drawable: Drawable?,
        val emoji: String?)

    companion object {

        private val TAG = makeLogTag(Elutto::class.java)
        private val utcZoneId = ZoneId.of("UTC")

        /**
         * Current time in UTC zone.
         */
        fun getUtcNow(): LocalDateTime {
            return LocalDateTime.now(utcZoneId)
        }

        /** Convert Unzoned UTC time to local-zoned instance. */
        private fun fromUtcToLocal(dt: LocalDateTime) : ZonedDateTime {
            val dtUtc = dt.atZone(utcZoneId)
            // from UTC to system timezone
            return dtUtc.withZoneSameInstant(ZoneId.systemDefault())
        }

        /**
         *  Shortest possible time left.
         */
        fun shortRemainingTime(dt: LocalDateTime?) : String {
            if (dt == null) return ""

            val curTime = getUtcNow()
            val duration = Duration.between(curTime, dt)

            val hoursLeft = duration.toHours()

            // less than hour, show minutes
            if (hoursLeft < 1) {
                val minutesLeft = duration.toMinutes()
                return if (minutesLeft < 1) "" else minutesLeft.toString()+"m"
            }

            val daysLeft = duration.toDays()
            // less than a day, show hours
            if (daysLeft < 1) {
                return hoursLeft.toString()+"h"
            }

            return daysLeft.toString()+"d"
        }

        /**
         *  Shortest possible and localized date-time formatter.
         */
        fun shortLocalizedTime(dt: LocalDateTime?) : String {
            if (dt == null) return ""

            val curTime = getUtcNow()
            val duration = Duration.between(dt, curTime)

            // from UTC to system timezone
            val dtLoc = fromUtcToLocal(dt)

            // if today, show: time
            if (duration.toDays() < 1) {
                return dtLocFmtTime.format(dtLoc)
            }
            // if this year, show: month, day
            if (dt.year == curTime.year) {
                return dtLocFmtMonthDay.format(dtLoc)
            }
            // else show: year, month
            return dtLocFmtYearMonth.format(dtLoc)
        }

        /**
         *  Either sets the TextView or hides it.
         */
        fun setCollapsableTextView(textView: TextView, text: String?) {
            if (text.isNullOrBlank()) {
                textView.text = ""
                textView.visibility = View.GONE
            } else {
                textView.text = text
                textView.visibility = View.VISIBLE
            }
        }

        private val dtLocFmtTime = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        private val dtLocFmtMonthDay = DateTimeFormatter.ofPattern("MMM d")
        private val dtLocFmtYearMonth = DateTimeFormatter.ofPattern("YY MMM")
    }

    private val userPendingResolve = ConcurrentHashMap<String, Boolean>()
    private val userCache = ConcurrentHashMap<String, UserProfile>()
    private val ctx = context
    private val httpElutto = OkHttpClient()
    private var eluttoAuthToken: String? = "rDoaxngg8atFUI_pbLKw5ZCgKyJkFvVnszopmDVF1yN3MThOooWSgMHpja8rDNzGFv8lzMUHry7H_3hWw-R_IPry2YNes4AW8vnuYYtSGmXa4QclE7ll7WEsxJMJiCpXVdeqesO-x1jY3Bs1xw4E63SpT0DJ0m4Ist5Mm1DGtaN_ATcRJhbBxFG545qdm7IEOq6xwGaoBGt6H4ZhW-oBKqvpBDDkp_28cNAQnm3dZ_MPJP3R8TSxa-_kjxalQ7YSm32uUe-Cig-yNGZP7UVWtI6qD7nPZaDVUCbc4Oq5v1fwij5v0Ky0J7SXP3VsfkTjYALuhYvsMYqsHTjiuaEz-eUWJcLhxpHLF8NN-PY3Apei2WT5FsTt2tgBy-hunkXFtKMsbZyJdV2bzctB7flFBbNTMN5_lJcLzg0wIabjQKSgTO9dVd2x79lRHhcxFYm6"
    private val urlEluttoBase = "http://192.168.0.21:5000"
}