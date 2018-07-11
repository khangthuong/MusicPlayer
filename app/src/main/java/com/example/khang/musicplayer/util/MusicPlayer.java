package com.example.khang.musicplayer.util;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.example.khang.musicplayer.playlist.MusicPlayerProvider;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;

/**
 * Created by sev_user on 9/21/2017.
 */

public class MusicPlayer {
    public static final String PLAY_NEW_AUDIO = "com.example.khang.musicplayer.PLAY_NEW_AUDIO";
    public static final String ACTION_PLAY = "com.example.khang.musicplayer.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.example.khang.musicplayer.ACTION_PAUSE";
    public static final String ACTION_PLAY_SHUFFLE = "com.example.khang.musicplayer.ACTION_PLAY_SHUFFLE";
    public static final String ACTION_PLAY_TO_END_SONG_LIST = "com.example.khang.musicplayer.ACTION_PLAY_TO_END_SONG_LIST";

    public static final String NOTIFICATION_PLAY = "com.example.khang.musicplayer.NOTIFICATION_PLAY";
    public static final String NOTIFICATION_PAUSE = "com.example.khang.musicplayer.NOTIFICATION_PAUSE";
    public static final String NOTIFICATION_PREVIOUS = "com.example.khang.musicplayer.NOTIFICATION_PREVIOUS";
    public static final String NOTIFICATION_NEXT = "com.example.khang.musicplayer.NOTIFICATION_NEXT";
    public static final String NOTIFICATION_STOP = "com.example.khang.musicplayer.NOTIFICATION_STOP";
    public static final int NOTIFICATION_ID = 101;

    public static String milliSecondsToTimer(long milliseconds) {
        String finalTimerString = "";
        String secondsString = "";

        // Convert total duration into time
        int hours = (int) (milliseconds / (1000 * 60 * 60));
        int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);
        // Add hours if there
        if (hours > 0) {
            finalTimerString = hours + ":";
        }

        // Prepending 0 to seconds if it is one digit
        if (seconds < 10) {
            secondsString = "0" + seconds;
        } else {
            secondsString = "" + seconds;
        }

        finalTimerString = finalTimerString + minutes + ":" + secondsString;

        // return timer string
        return finalTimerString;
    }

    public static Bitmap loadAlbumThumbnail(Uri uri, int imageSize, Context mContext) {
        AssetFileDescriptor afd = null;

        try {
            // Retrieves a file descriptor from the Contacts Provider. To learn more about this
            // feature, read the reference documentation for
            // ContentResolver#openAssetFileDescriptor.
            afd = mContext.getContentResolver().openAssetFileDescriptor(uri, "r");

            // Gets a FileDescriptor from the AssetFileDescriptor. A BitmapFactory object can
            // decode the contents of a file pointed to by a FileDescriptor into a Bitmap.
            FileDescriptor fileDescriptor = afd.getFileDescriptor();

            if (fileDescriptor != null) {
                // Decodes a Bitmap from the image pointed to by the FileDescriptor, and scales it
                // to the specified width and height
                return ImageLoader.decodeSampledBitmapFromDescriptor(
                        fileDescriptor, imageSize, imageSize);
            }
        } catch (FileNotFoundException e) {
            // If the file pointed to by the thumbnail URI doesn't exist, or the file can't be
            // opened in "read" mode, ContentResolver.openAssetFileDescriptor throws a
            // FileNotFoundException.

        } finally {
            // If an AssetFileDescriptor was returned, try to close it
            if (afd != null) {
                try {
                    afd.close();
                } catch (IOException e) {
                    // Closing a file descriptor might cause an IOException if the file is
                    // already closed. Nothing extra is needed to handle this.
                }
            }
        }

        // If the decoding failed, returns null
        return null;
    }

    public static int randInt(int min, int max) {

        // Usually this can be a field rather than a method variable
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomNum = rand.nextInt((max - min) + 1) + min;

        return randomNum;
    }

    public static int getListPreferredItemHeight(FragmentActivity activity) {
        final TypedValue typedValue = new TypedValue();

        // Resolve list item preferred height theme attribute into typedValue
        activity.getTheme().resolveAttribute(
                android.R.attr.listPreferredItemHeight, typedValue, true);

        // Create a new DisplayMetrics object
        final DisplayMetrics metrics = new android.util.DisplayMetrics();

        // Populate the DisplayMetrics
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        // Return theme value based on DisplayMetrics
        return (int) typedValue.getDimension(metrics);
    }

    public static class SongListQuery {
        public final static String PATH = "path";
        public final static int QUERY_ID = 0;
        public final static int QUERY_ID_FOLDER = 1;
        public final static int QUERY_ID_SPECIFIC_FOLDER = 2;
        public final static int QUERY_ID_SPECIFIC_FOLDER_FROM_SHAREPREFERENCE = 3;
        public final static int QUERY_ID_FOR_SONG_PICKER = 4;
        public final static int QUERY_ID_FOR_SONG_PICKER_FILTER = 45;
        public final static String[] PROJECTION = {
                MediaStore.Audio.AudioColumns._ID,
                MediaStore.Audio.AudioColumns.DATA,
                MediaStore.Audio.AudioColumns.TITLE,
                MediaStore.Audio.AudioColumns.ARTIST,
                MediaStore.Audio.AudioColumns.DURATION,
                MediaStore.Audio.AudioColumns.ALBUM_ID,
                MediaStore.Audio.AudioColumns.ALBUM_KEY,
                MediaStore.Audio.AudioColumns.DISPLAY_NAME
        };
        public final static String SELECTION = MediaStore.Audio.Media.IS_MUSIC + " !=" + 0;
        /*public final static String SELECTION_1 = MediaStore.Audio.Media.IS_MUSIC + " !=" + 0
        + " AND " + MediaStore.Audio.Media.DATA + " LIKE '" + PATH
        + "/" + ChiaSeNhac + "/%'";*/
        public final static String SORT_ORDER = MediaStore.Audio.AudioColumns.TITLE + " COLLATE LOCALIZED ASC";
        public final static String SORT_ORDER_1 = MediaStore.Audio.AudioColumns.DATA + " COLLATE LOCALIZED ASC";

        public final static int ID = 0;
        public final static int DATA = 1;
        public final static int TITLE = 2;
        public final static int ARTIST = 3;
        public final static int DURATION = 4;
        public final static int ALBUM_ID = 5;
        public final static int ALBUM_KEY = 6;
        public final static int DISPLAY_NAME = 7;
    }

    public static class PlayListNameQuery {
        public final static int QUERY_ID = 5;
        public final static String[] PROJECTION = {
                MusicPlayerProvider.TAG_INDEX,
                MusicPlayerProvider.TAG_NAME,
                MusicPlayerProvider.TAG_TOTAL_SONG
        };
        public final static String SORT_ORDER = MusicPlayerProvider.TAG_NAME + " COLLATE LOCALIZED ASC";
    }

    public static class PlayListDataQuery {
        public final static int QUERY_ID = 6;
        public final static String[] PROJECTION = {
                MusicPlayerProvider.TAG_INDEX_DATA,
                MusicPlayerProvider.TAG_PLAYLSIT_ID,
                MusicPlayerProvider.TAG_SONG_PATH,
                MusicPlayerProvider.TAG_SONG_NAME,
                MusicPlayerProvider.TAG_SONG_ARTIST,
                MusicPlayerProvider.TAG_SONG_DURATION,
                MusicPlayerProvider.TAG_SONG_ID,
                MusicPlayerProvider.TAG_ALBUM_ID

        };
        public final static String SORT_ORDER = MusicPlayerProvider.TAG_SONG_NAME + " COLLATE LOCALIZED ASC";
    }
}
