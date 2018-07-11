package com.example.khang.musicplayer.songlist;

import android.content.ContentUris;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.khang.musicplayer.R;
import com.example.khang.musicplayer.playlist.MusicPlayerProvider;
import com.example.khang.musicplayer.util.ImageLoader;
import com.example.khang.musicplayer.util.MusicPlayer;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;

import static com.example.khang.musicplayer.MyLoaderCallBack.sArtworkUri;

/**
 * Created by sev_user on 9/19/2017.
 */

public class SongListAdapter extends CursorAdapter {

    private Context mContext;
    private FragmentActivity activity;
    private ImageLoader mImageLoader;
    private int flag = 0;

    public SongListAdapter(Context context, final FragmentActivity activity, Cursor c, int flag) {
        super(context, null, 0);
        mContext = context;
        this.activity = activity;
        this.flag = flag;
        mImageLoader = new ImageLoader(activity, MusicPlayer.getListPreferredItemHeight(activity)) {
            @Override
            protected Bitmap processBitmap(Object data) {
                return MusicPlayer.loadAlbumThumbnail((Uri)data, getImageSize(), activity);
            }
        };

        // Set a placeholder loading image for the image loader
        mImageLoader.setLoadingImage(R.drawable.ic_music_file);

        // Add a cache to the image loader
        mImageLoader.addImageCache(activity.getSupportFragmentManager(), 0.5f);
    }

    private static class ViewHolder {
        private TextView songName, songSinger;
        private int color;
        private View divider;
        private ImageView thumb;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        int rowId = R.layout.row;
        final View convertView = View.inflate(mContext, rowId, null);
        ViewHolder viewHolder = new ViewHolder();
        viewHolder.songName = (TextView) convertView.findViewById(R.id.row_song_name);
        viewHolder.songSinger = (TextView) convertView.findViewById(R.id.row_song_singer);
        viewHolder.thumb = (ImageView) convertView.findViewById(R.id.row_thumb);
        viewHolder.color = ContextCompat.getColor(mContext, R.color.colorPrimaryDark);
        convertView.setTag(viewHolder);
        return convertView;
    }

    @Override
    public void bindView(View view, Context context, Cursor c) {
        ViewHolder viewHolder = (ViewHolder)view.getTag();

        if (viewHolder == null) {
            return;
        }

        if (flag == 0) {
            String songNameStr = c.getString(MusicPlayer.SongListQuery.TITLE);
            String songSingerStr = c.getString(MusicPlayer.SongListQuery.ARTIST);
            viewHolder.songName.setText(songNameStr.toString());
            viewHolder.songSinger.setText(songSingerStr);

            Long albumID = c.getLong(MusicPlayer.SongListQuery.ALBUM_ID);
            Uri uri = ContentUris.withAppendedId(sArtworkUri, albumID);
            mImageLoader.loadImage(uri, viewHolder.thumb);
        } else { //flag ==1
            String songNameStr = c.getString(MusicPlayerProvider.COLUMN_SONG_NAME);
            String songSingerStr = c.getString(MusicPlayerProvider.COLUMN_SONG_ARTIST);
            viewHolder.songName.setText(songNameStr.toString());
            viewHolder.songSinger.setText(songSingerStr);

            Long albumID = c.getLong(MusicPlayerProvider.COLUMN_ALBUM_ID);
            Uri uri = ContentUris.withAppendedId(sArtworkUri, albumID);
            mImageLoader.loadImage(uri, viewHolder.thumb);
        }

    }

    @Override
    public int getCount() {
        if (getCursor() == null) {
            return 0;
        }
        return super.getCount();
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        return super.swapCursor(newCursor);
    }

}


