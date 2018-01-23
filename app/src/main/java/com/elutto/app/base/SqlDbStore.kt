package com.elutto.app.base

import com.elutto.app.util.LogUtil.logI
import com.elutto.app.util.LogUtil.makeLogTag
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.elutto.app.util.LogUtil.logE
import org.threeten.bp.LocalDateTime

class SqlDbStore(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION)
{
    override fun onCreate(db: SQLiteDatabase)
    {
        // SQLite cannot execute multi-statements. We split with delimiter.
        for (statement in DATABASE_CREATE.split(";"))
        {
            if (!statement.isBlank()) {
                logI(TAG, "onCreate: " + statement)
                db.execSQL(statement)
            }
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVer: Int, newVer: Int)
    {
        // SQLite cannot execute multi-statements. We split with delimiter.
        for (statement in DATABASE_DROP.split(";"))
        {
            if (!statement.isBlank()) {
                logI(TAG, "onUpgrade: " + statement)
                db.execSQL(statement)
            }
        }
        for (statement in DATABASE_CREATE.split(";"))
        {
            if (!statement.isBlank()) {
                logI(TAG, "onUpgrade: " + statement)
                db.execSQL(statement)
            }
        }
    }

    /**
     * Translates DB datetime to LocalDateTime
     */
    fun fromLiteDatetime(liteTm: String?): LocalDateTime? {
        return if (liteTm.isNullOrEmpty()) null else LocalDateTime.parse(liteTm)
    }

    /**
     * Wraps SQLite connection.
     */
    class ManagedConnection(private val dbConn: SQLiteDatabase) : AutoCloseable
    {
        override fun close() {
            // Much confusion re. when/if to close the database. Majority SO posts imply closing
            // shouldn't be done in user-code at all. To back them up, when I try to close promptly
            // and multiple threads are involved, the app sometimes crashes with errors like:
            // "The connection pool has been closed but there are still 1 connections in use."

            // Intentionally left open...
            // dbConn.close()
        }

        fun query(sqlBody: String, vararg params: String?): Cursor {
            logI(TAG, "query: "+sqlBody+" ["+params.joinToString(",")+"]")
            return dbConn.rawQuery(sqlBody, params)
        }

        fun modify(sqlBody: String, vararg params: Any?) {
            val stmt = dbConn.compileStatement( sqlBody )
            params.forEachIndexed { ix, param ->
               if (param == null) {
                   stmt.bindNull(ix+1)
               } else {
                   stmt.bindString(ix+1, param.toString())
               }
            }
            logI(TAG, "modify: "+sqlBody+" ["+params.joinToString(",")+"]")
            stmt.execute()
        }
    }

    /**
     *  Produce a read-only DB connection suitable for SELECTs.
     */
    fun readable(block: ManagedConnection.() -> Unit)
    {
        ManagedConnection(readableDatabase).use {
            try {
                block(it)
            }
            catch (e: Exception) {
                logE(TAG, "DB reading failed: "+e.toString())
                throw e
            }
        }
    }

    /**
     *  Produce a writable DB connection suitable for INSERTs and UPDATEs.
     *  All statements are executed in the context of a transaction.
     */
    fun writable(block: ManagedConnection.() -> Unit)
    {
        val db = writableDatabase
        ManagedConnection(db).use {
            try {
                db.beginTransaction()
                block(it)
                db.setTransactionSuccessful()
            }
            catch (e: Exception) {
                logE(TAG, "DB writing failed: "+e.toString())
                throw e
            } finally {
                db.endTransaction()
            }
        }
    }

    companion object
    {
        // Beginning of the Elutto Epoch. A point in time before any Elutto activity.
        // We use this value whenever we need a "zero time".
        val ELUTTO_EPOCH = LocalDateTime.of(2004, 3, 9, 8, 0)!!

        private val DATABASE_NAME: String = "elutto-lite"

        // Database version
        private val DATABASE_VERSION: Int = 1

        private val TAG = makeLogTag(SqlDbStore::class.java)

        // SQL statements
        val SQL_USER_PROFILE_RETRIEVE = """
select
    username,
    first_name,
    last_name,
    middle_name,
    avatar,
    circle
from user_info usr
where user_id = ?
"""

        val SQL_USER_PROFILE_UPDATE = """
replace into user_info(
    user_id,
    username,
    first_name,
    last_name,
    middle_name,
    avatar,
    circle
)
values(?,?,?,?,?,?,?)
"""

        val SQL_CHANNELS_PARTICIPANTS_RETRIEVE = """
select
    channel_id,
    participant_user_id
from channel_participant
"""

        val SQL_CHANNELS_EVENTS_DECLARATIONS_RETRIEVE = """
select
    event_id,
    user_id,
    decl_resp,
    decl_tm
from channel_event_declaration
"""

        val SQL_CHANNELS_EVENTS_RETRIEVE = """
select
    channel_id,
    event_id,
    author_user_id,
    accepted_count,
    denied_count,
    max_allowed_users,
    min_required_users,
    rsvp_expiry_tm,
    rsvp_start_tm,
    title,
    underlying_event_location,
    underlying_event_start_tm,
    icon
from channel_event
"""

        val SQL_CHANNELS_SUMMARIES_RETRIEVE = """
select
    chnl.channel_id,
    chnl.create_tm,
    chnl.is_public,
    chnl.msg_count,
    chnl.my_notify_settings_mask,
    chnl.my_privileges_mask,
    chnl.my_rank integer,
    chnl.primary_user_id,
    chnl.theme_color,
    chnl.theme_image,
    chnl.title,
    user_last_peek.data_value as last_peek_time,
    chnl.last_msg_id text,
    chnl.last_msg_author_id,
    chnl.last_msg_received_tm,
    chnl.last_msg_text,
    chnl.last_msg_image
from channel_info chnl
    left join channel_data user_last_peek
        on user_last_peek.channel_id = chnl.channel_id
        and user_last_peek.data_name = 'CHANNEL_LAST_PEEK_TIME'
"""

        val SQL_CHANNEL_UPDATE = """
replace into channel_info(
    channel_id,
    create_tm,
    msg_count,
    is_public,
    my_notify_settings_mask,
    my_privileges_mask,
    my_rank,
    primary_user_id,
    theme_color,
    theme_image,
    title,
    last_msg_id,
    last_msg_author_id,
    last_msg_received_tm,
    last_msg_text,
    last_msg_image)
values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
"""

        val SQL_CHANNEL_PARTICIPANTS_DROP = """
delete from channel_participant
where channel_id = ?
"""

        val SQL_CHANNEL_EVENTS_DECLARATIONS_DROP = """
delete from channel_event_declaration
where channel_id = ?
"""

        val SQL_CHANNEL_EVENTS_DROP = """
delete from channel_event
where channel_id = ?
"""

        val SQL_CHANNEL_PARTICIPANT_ADD = """
insert into channel_participant(
    channel_id,
    participant_user_id,
    join_tm,
    user_rank,
    user_privileges_mask)
values(?,?,?,?,?)
"""

        val SQL_CHANNEL_EVENT_ADD = """
insert into channel_event(
    event_id,
    channel_id,
    author_user_id,
    accepted_count,
    denied_count,
    max_allowed_users,
    min_required_users,
    rsvp_expiry_tm,
    rsvp_start_tm,
    title,
    underlying_event_location,
    underlying_event_start_tm,
    icon)
values(?,?,?,?,?,?,?,?,?,?,?,?,?)
"""

        val SQL_CHANNEL_EVENT_DECLARATION_ADD = """
insert into channel_event_declaration(
    event_id,
    channel_id,
    user_id,
    decl_resp,
    decl_tm)
values(?,?,?,?,?)
"""

        // Database upgrade SQL statement
        private val DATABASE_DROP: String = """
drop table activity_log;
drop table pending_action;
drop table channel_message;
drop table channel_event_declaration;
drop table channel_event;
drop table channel_participant;
drop table user_info;
drop table channel_info;
"""

        // Database schema
        private val DATABASE_CREATE: String = """
create table user_info (
    user_id text primary key,
    username text not null,
    first_name text not null,
    last_name text not null,
    middle_name text null,
    avatar text null,
    circle text null
);

create table channel_info (
    channel_id text primary key,
    create_tm text not null,
    msg_count integer not null,
    is_public integer not null,
    my_notify_settings_mask integer not null,
    my_privileges_mask integer not null,
    my_rank integer not null,
    primary_user_id text not null,
    theme_color text null,
    theme_image text null,
    title text null,
    -- most recent message
    last_msg_id text null,
    last_msg_author_id text null,
    last_msg_received_tm text null,
    last_msg_text text null,
    last_msg_image text null
);

-- stores local application data pertaining to the channel
create table channel_data(
    channel_id text not null,
    -- Attributes that we are tracking per channel:
    -- * CHANNEL_LAST_PEEK_TIME # last time our user viewed the channel
    data_name text not null,
    data_value text null,
    primary key (channel_id, data_name)
);

create table channel_participant (
    channel_id text not null,
    participant_user_id text not null,
    join_tm text not null,
    user_rank integer not null,
    user_privileges_mask integer not null,
    primary key (channel_id, participant_user_id),
    foreign key (participant_user_id) references user_info(user_id)
);

create table channel_event (
    event_id text primary key,
    channel_id text not null,
    author_user_id text not null,
    accepted_count integer not null,
    denied_count integer not null,
    max_allowed_users integer not null,
    min_required_users integer not null,
    rsvp_expiry_tm text not null,
    rsvp_start_tm text not null,
    title text not null,
    underlying_event_location text null,
    underlying_event_start_tm text null,
    icon text null,
    foreign key (channel_id) references channel_info(channel_id),
    foreign key (author_user_id) references user_info(user_id)
);

create table channel_event_declaration (
    event_id text not null,
    channel_id text not null,
    user_id text not null,
    decl_resp text not null,
    decl_tm text not null,
    primary key (event_id, user_id),
    foreign key (event_id) references channel_event(event_id),
    foreign key (channel_id) references channel_info(channel_id),
    foreign key (user_id) references user_info(user_id)
);

create table channel_message (
    message_id text primary key,
    channel_id text not null,
    author_user_id text not null,
    server_received_tm text null, -- time when server received the message
    msg_text text null,
    msg_image text null,
    foreign key (channel_id) references channel_info(channel_id),
    foreign key (author_user_id) references user_info(user_id)
);

create table pending_action (
    client_action_id text primary key,
    queued_tm text not null, -- time action was queued up
    -- Action context:
    --  MSG_NEW_1: message that starts a new conversation
    --  MSG_1: message in the existing conversation
    --  EVENT_NEW_1: event that starts a new channel
    --  EVENT_1: event in the existing channel
    context text not null,
    -- context-specific parameters
    data_0 text null,
    data_1 text null,
    data_2 text null,
    data_3 text null,
    data_4 text null,
    data_5 text null,
    data_6 text null,
    data_7 text null,
    data_8 text null,
    data_9 text null
);

-- Log of major apiChannelEvents that occurred recently. For example, the client
-- throttles server-requests based on recent activity.
create table activity_log (
    activity_tm text not null,
    activity_kind text not null, -- CHANNELS_SYNC
    status text not null, -- OK, ERR
    resource text null,
    message text null
);
"""
    }
}