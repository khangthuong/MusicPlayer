package com.example.khang.musicplayer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.khang.musicplayer.playlist.MusicPlayerProvider;
import com.example.khang.musicplayer.util.ImageLoader;
import com.example.khang.musicplayer.util.MusicPlayer;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;

import static com.example.khang.musicplayer.MyLoaderCallBack.sArtworkUri;
import static com.example.khang.musicplayer.util.MusicPlayer.ACTION_PLAY_SHUFFLE;
import static com.example.khang.musicplayer.util.MusicPlayer.NOTIFICATION_NEXT;
import static com.example.khang.musicplayer.util.MusicPlayer.NOTIFICATION_PLAY;
import static com.example.khang.musicplayer.util.MusicPlayer.NOTIFICATION_PREVIOUS;
import static com.example.khang.musicplayer.util.MusicPlayer.NOTIFICATION_STOP;

/**
 * Created by sev_user on 9/19/2017.
 */

public class PlayingLayout implements OnClickListener {
    private Activity activity;
    private FragmentManager fragmentManager;
    private ImageView imgThub, imgPlayPause;
    LinearLayout nextBtn, pausePlayBtn, prevBtn;
    private TextView songName, songSinger, currentTime, totalTime;
    private SeekBar seekBar;
    private String mediaFile = null;
    int songPos = 0;
    private Cursor c = null;
    private ImageLoader mImageLoader;
    private OnPlayListener onPlayListener;

    public PlayingLayout(Activity mContext, FragmentManager fragmentManager) {
        this.activity = mContext;
        onPlayListener = (OnPlayListener) mContext;
        mImageLoader = new ImageLoader(activity, 150) {
            @Override
            protected Bitmap processBitmap(Object data) {
                return loadAlbumThumbnail((Uri) data, getImageSize());
            }
        };

        // Set a placeholder loading image for the image loader
        mImageLoader.setLoadingImage(R.drawable.ic_music_file);

        // Add a cache to the image loader
        mImageLoader.addImageCache(fragmentManager, 0.5f);
        register_notificationActionReceiver();
    }

    public void initView() {
        imgThub = (ImageView) (activity.findViewById(R.id.playing_layout_thumb));
        imgPlayPause = (ImageView) (activity.findViewById(R.id.imgPlayPause)) ;
        nextBtn = (LinearLayout) activity.findViewById(R.id.next_btn);
        pausePlayBtn = (LinearLayout) (activity.findViewById(R.id.play_pause_btn));
        prevBtn = (LinearLayout) (activity.findViewById(R.id.prev_btn));
        nextBtn.setOnClickListener(this);
        pausePlayBtn.setOnClickListener(this);
        prevBtn.setOnClickListener(this);
        songName = (TextView) (activity.findViewById(R.id.song_name_txt));
        songSinger = (TextView) (activity.findViewById(R.id.song_singer));
        currentTime = (TextView) (activity.findViewById(R.id.playing_layout_current_time_txt));
        totalTime = (TextView) (activity.findViewById(R.id.playing_layout_total_time_txt));
        seekBar = (SeekBar) (activity.findViewById(R.id.media_seek_bar));
    }

    public SeekBar getSeekBar() {
        return  seekBar;
    }

    public TextView getCurrentTimeTxt() {
        return currentTime;
    }

    public void setCursor(Cursor c) {
        this.c = c;
    }

    public void setMediafile(String str, int pos) {
        mediaFile = str;
        songPos = pos;
    }

    public void setData(String from) {
        String songNameStr;
        String songSingerStr;
        Long albumID;
        Uri uri;
        String totalTimeStr;
        String currentTimeStr;

        if (from.equals("playlist")) {
            songNameStr = c.getString(MusicPlayerProvider.COLUMN_SONG_NAME);
            songSingerStr = c.getString(MusicPlayerProvider.COLUMN_SONG_ARTIST);
            albumID = c.getLong(MusicPlayerProvider.COLUMN_ALBUM_ID);
            uri = ContentUris.withAppendedId(sArtworkUri, albumID);
            totalTimeStr = MusicPlayer.milliSecondsToTimer(c.getLong(MusicPlayerProvider.COLUMN_SONG_DURATION));
            currentTimeStr = "0:00";
        } else {
            songNameStr = c.getString(MusicPlayer.SongListQuery.TITLE);
            songSingerStr = c.getString(MusicPlayer.SongListQuery.ARTIST);
            albumID = c.getLong(MusicPlayer.SongListQuery.ALBUM_ID);
            uri = ContentUris.withAppendedId(sArtworkUri, albumID);
            totalTimeStr = MusicPlayer.milliSecondsToTimer(c.getLong(MusicPlayer.SongListQuery.DURATION));
            currentTimeStr = "0:00";
        }

        if (imgThub != null && uri != null) {
            mImageLoader.loadImage(uri, imgThub);
        }

        if (songName != null && songNameStr != null) {
            songName.setText(songNameStr.toString());
        }

        if (songSinger != null && songSingerStr != null) {
            songSinger.setText(songSingerStr.toString());
        }

        if (currentTime != null && currentTimeStr != null) {
            currentTime.setText(currentTimeStr.toString());
        }

        if (totalTime != null && totalTimeStr != null) {
            totalTime.setText(totalTimeStr.toString());
        }
    }

    public void setPausePlayBtn (boolean play) {
        if (play) {
            imgPlayPause.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_pause_btn));
        } else {
            imgPlayPause.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_play_btn));
        }
    }

    @Nullable
    private Bitmap loadAlbumThumbnail(Uri uri, int imageSize) {
        AssetFileDescriptor afd = null;

        try {
            // Retrieves a file descriptor from the Contacts Provider. To learn more about this
            // feature, read the reference documentation for
            // ContentResolver#openAssetFileDescriptor.
            afd = activity.getContentResolver().openAssetFileDescriptor(uri, "r");

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

    @Override
    public void onClick(View view) {
        if (view == null) {
            return;
        }

        if(c.isClosed()) {
            if (MainActivity.playFrom.equals("song_list")) {
                c = activity.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        MusicPlayer.SongListQuery.PROJECTION,
                        MusicPlayer.SongListQuery.SELECTION,
                        null,
                        MusicPlayer.SongListQuery.SORT_ORDER);
            }

            if (MainActivity.playFrom.equals("playlist")) {
                c = activity.getContentResolver().query(MusicPlayerProvider.CONTENT_URI_DATA,
                        MusicPlayer.PlayListDataQuery.PROJECTION,
                        MusicPlayerProvider.TAG_PLAYLSIT_ID + " = " + MainActivity.playListID ,
                        null,
                        MusicPlayer.PlayListDataQuery.SORT_ORDER);
            }
        }

        switch (view.getId()) {
            case R.id.next_btn:

                songPos++;
                if (songPos == c.getCount()) {
                    songPos = 0;
                }
                c.moveToPosition(songPos);
                if (MainActivity.playFrom.equals("playlist")) {
                    seekBar.setMax((int)c.getLong(MusicPlayerProvider.COLUMN_SONG_DURATION));
                } else {
                    seekBar.setMax((int) c.getLong(MusicPlayer.SongListQuery.DURATION));
                }
                /*setCursor(c);
                setData();
                setMediafile(c.getString(MusicPlayer.SongListQuery.DATA), songPos);
                broadcastIntent.setAction(PLAY_NEW_AUDIO);
                broadcastIntent.putExtra("new_media", mediaFile);
                activity.sendBroadcast(broadcastIntent);*/
                onPlayListener.onNext(c, songPos);
                break;

            case R.id.prev_btn:
                songPos--;
                if (songPos < 0) {
                    songPos = c.getCount()-1;
                }
                c.moveToPosition(songPos);
                if (MainActivity.playFrom.equals("playlist")) {
                    seekBar.setMax((int)c.getLong(MusicPlayerProvider.COLUMN_SONG_DURATION));
                } else {
                    seekBar.setMax((int) c.getLong(MusicPlayer.SongListQuery.DURATION));
                }
                /*setCursor(c);
                setData();
                setMediafile(c.getString(MusicPlayer.SongListQuery.DATA), songPos);
                broadcastIntent.setAction(PLAY_NEW_AUDIO);
                broadcastIntent.putExtra("new_media", mediaFile);
                activity.sendBroadcast(broadcastIntent);*/
                onPlayListener.onPrev(c, songPos);
                break;

            case R.id.play_pause_btn:

                c.moveToPosition(songPos);
                onPlayListener.onPlay(c, songPos);
                break;
        }
    }

    private void playShuffle() {
        if (c == null) {
            return;
        }

        songPos = MusicPlayer.randInt(0, c.getCount()-1);
        c.moveToPosition(songPos);
        if (MainActivity.playFrom.equals("playlist")) {
            seekBar.setMax((int)c.getLong(MusicPlayerProvider.COLUMN_SONG_DURATION));
        } else {
            seekBar.setMax((int) c.getLong(MusicPlayer.SongListQuery.DURATION));
        }
        onPlayListener.onNext(c, songPos);
    }

    public interface OnPlayListener {
        void onPlay (Cursor c, int pos);
        void onNext (Cursor c, int pos);
        void onPrev (Cursor c, int pos);
        void onStop (Cursor c, int pos);
    }

    private BroadcastReceiver notificationActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(NOTIFICATION_NEXT)) {
                nextBtn.performClick();
            } else if (action.equals(NOTIFICATION_PREVIOUS)) {
                prevBtn.performClick();
            } else if (action.equals(NOTIFICATION_PLAY)) {
                pausePlayBtn.performClick();
            } else if (action.equals(NOTIFICATION_STOP)) {
                onPlayListener.onStop(c, songPos);
            } else if (action.equals(ACTION_PLAY_SHUFFLE)) {
                playShuffle();
            }
        }
    };

    private void register_notificationActionReceiver() {
        //Register playNewMedia receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(NOTIFICATION_PLAY);
        filter.addAction(NOTIFICATION_NEXT);
        filter.addAction(NOTIFICATION_PREVIOUS);
        filter.addAction(NOTIFICATION_STOP);
        filter.addAction(ACTION_PLAY_SHUFFLE);
        activity.registerReceiver(notificationActionReceiver, filter);
    }

    public void unRegister_notificationActionReceiver() {
        activity.unregisterReceiver(notificationActionReceiver);
    }
}
