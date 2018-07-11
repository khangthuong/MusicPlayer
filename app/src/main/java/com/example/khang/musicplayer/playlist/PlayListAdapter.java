package com.example.khang.musicplayer.playlist;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.khang.musicplayer.R;
import com.example.khang.musicplayer.songlist.SongListAdapter;
import com.example.khang.musicplayer.util.ImageLoader;
import com.example.khang.musicplayer.util.MusicPlayer;

/**
 * Created by sev_user on 10/19/2017.
 */

public class PlayListAdapter extends CursorAdapter {

    private Context mContext = null;
    private FragmentActivity activity;

    public PlayListAdapter(Context context, final FragmentActivity activity, Cursor c, int flag) {
        super(context, null, 0);
        mContext = context;
        this.activity = activity;

    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        int rowId = R.layout.row_playlist;
        final View convertView = View.inflate(mContext, rowId, null);
        ViewHolder viewHolder = new ViewHolder();
        viewHolder.playListName = (TextView)convertView.findViewById(R.id.playlist_name);
        viewHolder.totalSong = (TextView)convertView.findViewById(R.id.playlist_total_song);
        convertView.setTag(viewHolder);
        return convertView;
    }

    @Override
    public void bindView(View view, Context context, Cursor c) {
        ViewHolder viewHolder = (ViewHolder)view.getTag();

        if (viewHolder == null) {
            return;
        }

        String playListName = c.getString(MusicPlayerProvider.COLUMN_NAME);
        String totalSong = c.getString(MusicPlayerProvider.COLUMN_TOTAL_SONG);
        viewHolder.playListName.setText(playListName);
        viewHolder.totalSong.setText(totalSong + " songs");
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
        private TextView playListName, totalSong;
    }
}
