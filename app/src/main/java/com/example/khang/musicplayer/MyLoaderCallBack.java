package com.example.khang.musicplayer;

import android.content.Context;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import com.example.khang.musicplayer.folderlist.FolderListAdapter;
import com.example.khang.musicplayer.playlist.MusicPlayerProvider;
import com.example.khang.musicplayer.playlist.PlayListAdapter;
import com.example.khang.musicplayer.playlist.PlayListFragment;
import com.example.khang.musicplayer.playlist.SongPickerActivity;
import com.example.khang.musicplayer.playlist.SongPickerAdapter;
import com.example.khang.musicplayer.search.SearchAdapter;
import com.example.khang.musicplayer.songlist.SongListAdapter;
import com.example.khang.musicplayer.util.MusicPlayer;

import java.util.ArrayList;

/**
 * Created by sev_user on 9/19/2017.
 */

public class MyLoaderCallBack implements LoaderManager.LoaderCallbacks<Cursor> {

    private Uri contentUri = null;
    private Context mContext = null;
    public static final Uri sArtworkUri = Uri
            .parse("content://media/external/audio/albumart");
    private SongListAdapter songListAdapter;
    private FolderListAdapter folderListAdapter;
    private PlayListAdapter playListAdapter;
    private SongPickerAdapter songPickerAdapter;
    private SearchAdapter searchAdapter;
    private OnLoaderFinishListener onLoaderFinishListener;
    private OnLoaderFinishListener onLoaderFinishListener1;
    private ArrayList<String>folderList = null;

    public MyLoaderCallBack (Context context,
                             SongListAdapter songListAdapter,
                             FolderListAdapter folderListAdapter,
                             PlayListAdapter playListAdapter,
                             SongPickerAdapter songPickerAdapter,
                             SearchAdapter searchAdapter) {
        mContext = context;
        this.songListAdapter = songListAdapter;
        this.folderListAdapter = folderListAdapter;
        this.playListAdapter = playListAdapter;
        this.songPickerAdapter = songPickerAdapter;
        this.searchAdapter = searchAdapter;
        folderList = new ArrayList<>();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        if (mContext instanceof OnLoaderFinishListener) {
            onLoaderFinishListener = (OnLoaderFinishListener) mContext;
        }

        switch (id) {

            case MusicPlayer.SongListQuery.QUERY_ID:
            case MusicPlayer.SongListQuery.QUERY_ID_FOR_SONG_PICKER:

                return new CursorLoader(mContext,
                        contentUri,
                        MusicPlayer.SongListQuery.PROJECTION,
                        MusicPlayer.SongListQuery.SELECTION,
                        null,
                        MusicPlayer.SongListQuery.SORT_ORDER);

            case MusicPlayer.SongListQuery.QUERY_ID_FOLDER:

                return new CursorLoader(mContext,
                        contentUri,
                        MusicPlayer.SongListQuery.PROJECTION,
                        MusicPlayer.SongListQuery.SELECTION,
                        null,
                        MusicPlayer.SongListQuery.SORT_ORDER_1);

            case MusicPlayer.SongListQuery.QUERY_ID_SPECIFIC_FOLDER:
            case MusicPlayer.SongListQuery.QUERY_ID_SPECIFIC_FOLDER_FROM_SHAREPREFERENCE:

                String pathStr = bundle.getString(MusicPlayer.SongListQuery.PATH);
                return new CursorLoader(mContext,
                        contentUri,
                        MusicPlayer.SongListQuery.PROJECTION,
                        MediaStore.Audio.Media.IS_MUSIC + " !=" + 0
                                + " AND " + MediaStore.Audio.Media.DATA + " LIKE '" + pathStr + "%'",
                        null,
                        MusicPlayer.SongListQuery.SORT_ORDER);

            case MusicPlayer.PlayListNameQuery.QUERY_ID:

                return new CursorLoader(mContext,
                        MusicPlayerProvider.CONTENT_URI,
                        MusicPlayer.PlayListNameQuery.PROJECTION,
                        null,
                        null,
                        MusicPlayer.PlayListNameQuery.SORT_ORDER);

            case MusicPlayer.PlayListDataQuery.QUERY_ID:

                int playListID = bundle.getInt(PlayListFragment.PLAYLIST_ID);
                return new CursorLoader(mContext,
                        MusicPlayerProvider.CONTENT_URI_DATA,
                        MusicPlayer.PlayListDataQuery.PROJECTION,
                        MusicPlayerProvider.TAG_PLAYLSIT_ID + " = " + playListID ,
                        null,
                        MusicPlayer.PlayListDataQuery.SORT_ORDER);

            case MusicPlayer.SongListQuery.QUERY_ID_FOR_SONG_PICKER_FILTER:

                String searchKey = bundle.getString(SongPickerActivity.SEARCH_KEY);
                return new CursorLoader(mContext,
                        contentUri,
                        MusicPlayer.SongListQuery.PROJECTION,
                        MediaStore.Audio.Media.IS_MUSIC + " !=" + 0
                                + " AND (" + MediaStore.Audio.Media.TITLE + " LIKE '" + "%" + searchKey + "%'"
                                + " OR " + MediaStore.Audio.Media.ARTIST + " LIKE '" + "%" + searchKey + "%')",
                        null,
                        MusicPlayer.SongListQuery.SORT_ORDER);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        switch (loader.getId()) {

            case MusicPlayer.SongListQuery.QUERY_ID:

                songListAdapter.swapCursor(c);
                if (MainActivity.playFrom.equals("song_list")) {
                    onLoaderFinishListener.onLoadFinished(c);
                }
                break;

            case MusicPlayer.SongListQuery.QUERY_ID_FOLDER:

                if (c.moveToFirst()) {
                    do {
                        String songNameStr = c.getString(MusicPlayer.SongListQuery.DISPLAY_NAME);
                        String data = c.getString(MusicPlayer.SongListQuery.DATA);
                        int len1 = songNameStr.length();
                        int len2 = data.length();

                        String path = data.substring(0, len2 - len1);
                        if (!isSamePath(path)) {
                            folderList.add(path);
                        }
                    } while (c.moveToNext());
                    folderListAdapter.setArrayList(folderList);
                    folderListAdapter.notifyDataSetChanged();
                }
                break;

            case MusicPlayer.SongListQuery.QUERY_ID_SPECIFIC_FOLDER:

                songListAdapter.swapCursor(c);
                break;

            case MusicPlayer.SongListQuery.QUERY_ID_SPECIFIC_FOLDER_FROM_SHAREPREFERENCE:

                songListAdapter.swapCursor(c);
                if (MainActivity.playFrom.equals("folder")) {
                    onLoaderFinishListener.onLoadFinished(c);
                }
                break;

            case MusicPlayer.PlayListNameQuery.QUERY_ID:
                playListAdapter.swapCursor(c);
                onLoaderFinishListener1.onLoadFinished(c);
                break;

            case MusicPlayer.PlayListDataQuery.QUERY_ID:
                songListAdapter.swapCursor(c);
                if (MainActivity.playFrom.equals("playlist")) {
                    onLoaderFinishListener.onLoadFinished(c);
                }
                break;

            case MusicPlayer.SongListQuery.QUERY_ID_FOR_SONG_PICKER:
            case MusicPlayer.SongListQuery.QUERY_ID_FOR_SONG_PICKER_FILTER:
                if (songPickerAdapter != null) {
                    songPickerAdapter.swapCursor(c);
                }

                if (searchAdapter != null) {
                    searchAdapter.swapCursor(c);
                }
                break;

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {

            case MusicPlayer.SongListQuery.QUERY_ID:
            case MusicPlayer.SongListQuery.QUERY_ID_SPECIFIC_FOLDER:
            case MusicPlayer.SongListQuery.QUERY_ID_SPECIFIC_FOLDER_FROM_SHAREPREFERENCE:
            case MusicPlayer.PlayListDataQuery.QUERY_ID:
                songListAdapter.swapCursor(null);
                break;
            case MusicPlayer.PlayListNameQuery.QUERY_ID:
                playListAdapter.swapCursor(null);
                break;
            case MusicPlayer.SongListQuery.QUERY_ID_FOR_SONG_PICKER:
            case MusicPlayer.SongListQuery.QUERY_ID_FOR_SONG_PICKER_FILTER:
                if (songPickerAdapter != null) {
                    songPickerAdapter.swapCursor(null);
                }

                if (searchAdapter != null) {
                    searchAdapter.swapCursor(null);
                }
                break;

        }
    }

    public interface OnLoaderFinishListener {
        void onLoadFinished(Cursor c);
    }

    public void setOnLoaderFinishListener(OnLoaderFinishListener onLoaderFinishListener) {
        this.onLoaderFinishListener1 = onLoaderFinishListener;
    }

    private boolean isSamePath(String path) {

        if (folderList.size() == 0) {
            return false;
        }

        for (String item : folderList) {
            if (path.equals(item)) {
                return true;
            }
        }
        return false;
    }
}
