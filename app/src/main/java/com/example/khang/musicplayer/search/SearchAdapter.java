package com.example.khang.musicplayer.search;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.khang.musicplayer.R;
import com.example.khang.musicplayer.util.ImageLoader;
import com.example.khang.musicplayer.util.MusicPlayer;

import static com.example.khang.musicplayer.MyLoaderCallBack.sArtworkUri;

/**
 * Created by sev_user on 12/7/2017.
 */

public class SearchAdapter extends CursorAdapter {
    private Context mContext;
    private FragmentActivity activity;
    private ImageLoader mImageLoader;
    private ListView mListView;
    private TextView searchTextView;

    public SearchAdapter(Context context, final FragmentActivity activity, Cursor c, int flag) {
        super(context, null, 0);
        mContext = context;
        this.activity = activity;
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
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        int rowId = R.layout.row;
        final View convertView = View.inflate(mContext, rowId, null);
        ViewHolder viewHolder = new ViewHolder();
        viewHolder.songName = (TextView) convertView.findViewById(R.id.row_song_name);
        viewHolder.songSinger = (TextView) convertView.findViewById(R.id.row_song_singer);
        viewHolder.thumb = (ImageView) convertView.findViewById(R.id.row_thumb);
        viewHolder.color = ContextCompat.getColor(mContext, R.color.primary_color_custom_dialog);
        convertView.setTag(viewHolder);
        return convertView;
    }

    @Override
    public void bindView(View view, Context context, Cursor c) {
        ViewHolder viewHolder = (ViewHolder)view.getTag();

        if (viewHolder == null) {
            return;
        }

        String songNameStr = c.getString(MusicPlayer.SongListQuery.TITLE);
        String songSingerStr = c.getString(MusicPlayer.SongListQuery.ARTIST);

        String keyWordString;
        if (searchTextView != null) {
            keyWordString = searchTextView.getText().toString();
        } else {
            keyWordString = "";
        }

        Spannable spanTitle = new SpannableString(songNameStr);
        String[] arrayKey = keyWordString.split(" ");
        int indexOf;
        int length;
        int addOffset = 0;

        for (int i = 0; i < arrayKey.length; i++) {
            indexOf = getSpannableTextIndex(songNameStr, arrayKey[i]);
            length = arrayKey[i].length();
            if (indexOf != -1) {
                int l = spanTitle.length();
                if (addOffset + length + indexOf <= l) {
                    spanTitle.setSpan(new ForegroundColorSpan(viewHolder.color),
                            indexOf + addOffset, length + indexOf + addOffset,
                            Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    songNameStr = songNameStr.substring(indexOf + length);
                    addOffset += length + indexOf;
                } else {
                    spanTitle.setSpan(new ForegroundColorSpan(viewHolder.color),
                            indexOf + addOffset, l - 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    songNameStr = songNameStr.substring(l - 1);
                    addOffset += l - 1;
                }
            }
        }

        viewHolder.songName.setText(spanTitle);

        spanTitle = new SpannableString(songSingerStr);
        arrayKey = keyWordString.split(" ");
        addOffset = 0;

        for (int i = 0; i < arrayKey.length; i++) {
            indexOf = getSpannableTextIndex(songSingerStr, arrayKey[i]);
            length = arrayKey[i].length();
            if (indexOf != -1) {
                int l = spanTitle.length();
                if (addOffset + length + indexOf <= l) {
                    spanTitle.setSpan(new ForegroundColorSpan(viewHolder.color),
                            indexOf + addOffset, length + indexOf + addOffset,
                            Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    songSingerStr = songSingerStr.substring(indexOf + length);
                    addOffset += length + indexOf;
                } else {
                    spanTitle.setSpan(new ForegroundColorSpan(viewHolder.color),
                            indexOf + addOffset, l - 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    songSingerStr = songSingerStr.substring(l - 1);
                    addOffset += l - 1;
                }
            }
        }

        viewHolder.songSinger.setText(spanTitle);

        int id = c.getInt(MusicPlayer.SongListQuery.ID);
        Long albumID = c.getLong(MusicPlayer.SongListQuery.ALBUM_ID);
        Uri uri = ContentUris.withAppendedId(sArtworkUri, albumID);
        mImageLoader.loadImage(uri, viewHolder.thumb);
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

    private static class ViewHolder {
        private TextView songName, songSinger;
        private int color;
        private View divider;
        private ImageView thumb;
    }

    private int getSpannableTextIndex(String title, String keyword) {
        return title.toLowerCase().indexOf(keyword.toLowerCase());
    }

    public void setListView(ListView listView) {
        mListView = listView;
    }

    public void setsearchTextView(TextView textView) {
        searchTextView = textView;
    }
}
