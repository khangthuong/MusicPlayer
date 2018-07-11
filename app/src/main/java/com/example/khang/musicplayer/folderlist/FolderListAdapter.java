package com.example.khang.musicplayer.folderlist;

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
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.khang.musicplayer.R;
import com.example.khang.musicplayer.songlist.SongListAdapter;
import com.example.khang.musicplayer.util.ImageLoader;
import com.example.khang.musicplayer.util.MusicPlayer;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import static com.example.khang.musicplayer.MyLoaderCallBack.sArtworkUri;

/**
 * Created by sev_user on 10/16/2017.
 */

public class FolderListAdapter extends ArrayAdapter {

    private Context mContext;
    private FragmentActivity activity;
    private ImageLoader mImageLoader;
    private ArrayList<String> folderList = null;

    public FolderListAdapter(Context context, final FragmentActivity activity, ArrayList<String> objects, int id) {
        super(context, id, objects);
        mContext = context;
        this.activity = activity;
        folderList = new ArrayList<String>();
        folderList = objects;
        mImageLoader = new ImageLoader(activity, MusicPlayer.getListPreferredItemHeight(activity)) {
            @Override
            protected Bitmap processBitmap(Object data) {
                return MusicPlayer.loadAlbumThumbnail((Uri)data, getImageSize(), activity);
            }
        };

        // Set a placeholder loading image for the image loader
        mImageLoader.setLoadingImage(R.drawable.ic_folder);

        // Add a cache to the image loader
        mImageLoader.addImageCache(activity.getSupportFragmentManager(), 0.5f);
    }

    private static class ViewHolder {
        private TextView folder, path;
        private int color;
        private View divider;
        private ImageView folderThumb;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int rowId = R.layout.row;
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = View.inflate(mContext, rowId, null);
            viewHolder.folder = (TextView) convertView.findViewById(R.id.row_song_name);
            viewHolder.path = (TextView) convertView.findViewById(R.id.row_song_singer);
            viewHolder.folderThumb = (ImageView) convertView.findViewById(R.id.row_thumb);
            viewHolder.color = ContextCompat.getColor(mContext, R.color.colorPrimaryDark);
            viewHolder.divider = (View)convertView.findViewById(R.id.list_item_divider) ;
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)convertView.getTag();
        }
        String pathStr = folderList.get(position);
        viewHolder.path.setText(pathStr);
        int j = 0;
        for (int i = pathStr.length()-1; i > -1; i--) {
            if (pathStr.charAt(i-1) == '/') {
                j = i;
                break;
            }
        }
        String folderStr = pathStr.substring(j, pathStr.length()-1);
        viewHolder.folder.setText(folderStr);
        mImageLoader.loadImage(Uri.parse("android.resource://com.example.khang.musicplayer/drawable/ic_folder.png"),
                viewHolder.folderThumb);
        return convertView;
    }

    @Override
    public int getCount() {
        return folderList.size();
    }

    public void setArrayList(ArrayList<String> list) {
        folderList = list;
    }

    public ArrayList<String> getArrayList() {
        return folderList;
    }
}
