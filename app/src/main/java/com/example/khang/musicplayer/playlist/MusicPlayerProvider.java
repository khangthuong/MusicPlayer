package com.example.khang.musicplayer.playlist;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

/**
 * Created by sev_user on 10/19/2017.
 */

public class MusicPlayerProvider extends ContentProvider {

    private static final String TAG = "MusicPlayerProvider";
    // Playlist database
    public static final String DATABASE_NAME = "playlist.db";
    public static final String TABLE_PLAYLIST_NAME = "playlistname";
    public static final String TABLE_PLAYLIST_DATA = "playlistdata";
    private SQLiteOpenHelper mOpenHelper;
    public static final String AUTHORITY = "com.example.khang.musicplayer";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + '/' + TABLE_PLAYLIST_NAME);
    public static final Uri CONTENT_URI_DATA = Uri.parse("content://" + AUTHORITY + '/' + TABLE_PLAYLIST_DATA);

    // Colum name - playlistname table
    public static final String TAG_INDEX = "_id";
    public static final String TAG_NAME = "name";
    public static final String TAG_TOTAL_SONG = "total_song";
    // column index - playlistname table
    public static final int COLUMN_INDEX = 0;
    public static final int COLUMN_NAME = 1;
    public static final int COLUMN_TOTAL_SONG = 2;

    // column name - playlist data
    public static final String TAG_INDEX_DATA = "_id";
    public static final String TAG_PLAYLSIT_ID = "_nameID";
    public static final String TAG_SONG_PATH = "path";
    public static final String TAG_SONG_NAME = "name";
    public static final String TAG_SONG_ARTIST = "artist";
    public static final String TAG_SONG_DURATION = "duaration";
    public static final String TAG_SONG_ID = "songID";
    public static final String TAG_ALBUM_ID = "AlbumID";
    public static final String TAG_ALBUM_KEY = "AlbumKEY";
    // column index - playlist data
    public static final int COLUMN_INDEX_DATA = 0;
    public static final int COLUMN_PLAYLIST_ID = 1;
    public static final int COLUMN_SONG_PATH = 2;
    public static final int COLUMN_SONG_NAME = 3;
    public static final int COLUMN_SONG_ARTIST = 4;
    public static final int COLUMN_SONG_DURATION = 5;
    public static final int COLUMN_SONG_ID = 6;
    public static final int COLUMN_ALBUM_ID = 7;
    public static final int COLUMN_ALBUM_KEY = 8;

    // playlist name database : create table query
    public static final String CREATE_TABLE_PLAYLIST_NAME = "CREATE TABLE IF NOT EXISTS " + TABLE_PLAYLIST_NAME
            + " (" + TAG_INDEX + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
            + TAG_NAME + " TEXT,"
            + TAG_TOTAL_SONG + " INTEGER NOT NULL DEFAULT 0)";

    // playlist data database : create table query
    public static final String CREATE_TABLE_PLAYLIST_DATA = "CREATE TABLE IF NOT EXISTS " + TABLE_PLAYLIST_DATA
            + " (" + TAG_INDEX_DATA + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
            + TAG_PLAYLSIT_ID + " INTEGER,"
            + TAG_SONG_PATH + " TEXT,"
            + TAG_SONG_NAME + " TEXT,"
            + TAG_SONG_ARTIST + " TEXT,"
            + TAG_SONG_DURATION + " LONG NOT NULL DEFAULT 0,"
            + TAG_SONG_ID + " LONG NOT NULL DEFAULT 0,"
            + TAG_ALBUM_ID + " LONG NOT NULL DEFAULT 0,"
            + TAG_ALBUM_KEY + " TEXT)";


    @Override
    public boolean onCreate() {
        final Context context = getContext();
        mOpenHelper = new DatabaseHelper(context);
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(args.table);
        SQLiteDatabase db;

        try {
            db = mOpenHelper.getWritableDatabase();
        } catch (Exception e) {
            db = mOpenHelper.getReadableDatabase();
        }

        Cursor result = qb.query(db, projection, args.where, args.args, null, null, sortOrder);

        if (result != null) {
            Context context = getContext();

            if (context != null) {
                result.setNotificationUri(context.getContentResolver(), uri);
            }
        }

        return result;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues initialValues) {
        SqlArguments args = new SqlArguments(uri);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final long rowId;

        try {
            rowId = db.insert(args.table, null, initialValues);
        } catch (Exception e) {
            throw e;
        }

        if (rowId <= 0) {
            return null;
        }

        Uri uriResult = ContentUris.withAppendedId(uri, rowId);

        return uriResult;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;

        try {
            count = db.update(args.table, values, args.where, args.args);
        } catch (Exception e) {
            throw e;
        }

        return count;
    }

    private static class SqlArguments {

        public final String table;

        public final String where;

        public final String[] args;

        private SqlArguments(Uri url, String where, String[] args) {
            if (url.getPathSegments().size() == 1) {
                table = url.getPathSegments().get(0);
                this.where = where;
                this.args = args;
            } else if (url.getPathSegments().size() != 2) {
                throw new IllegalArgumentException("Invalid URI: " + url);
            } else if (!TextUtils.isEmpty(where)) {
                throw new UnsupportedOperationException("WHERE clause not supported: " + url);
            } else {
                this.table = url.getPathSegments().get(0);
                this.where = "_id=" + ContentUris.parseId(url);
                this.args = null;
            }
        }

        private SqlArguments(Uri url) {
            if (url.getPathSegments().size() == 1) {
                table = url.getPathSegments().get(0);
                where = null;
                args = null;
            } else {
                throw new IllegalArgumentException("Invalid URI: " + url);
            }
        }
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        private DatabaseHelper (Context context) {
            super(context, DATABASE_NAME, null, 1);

        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL(CREATE_TABLE_PLAYLIST_NAME);
            sqLiteDatabase.execSQL(CREATE_TABLE_PLAYLIST_DATA);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

        }
    }

}
