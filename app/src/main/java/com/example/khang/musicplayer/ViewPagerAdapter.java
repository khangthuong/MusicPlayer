package com.example.khang.musicplayer;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.example.khang.musicplayer.folderlist.FolderListFragment;
import com.example.khang.musicplayer.playlist.PlayListFragment;
import com.example.khang.musicplayer.songlist.SongListFragment;

/**
 * Created by sev_user on 9/18/2017.
 */

public class ViewPagerAdapter extends FragmentStatePagerAdapter {
    private Context mContext;

    public ViewPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        mContext = context;
    }
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                SongListFragment songListFragment = new SongListFragment();
                return  songListFragment;
            case 1:
                FolderListFragment folderListFragment = new FolderListFragment();
                return folderListFragment;
            case 2:
                PlayListFragment playListFragment = new PlayListFragment();
                return playListFragment;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle (int position) {
        switch (position) {
            case 0:
                return mContext.getResources().getString(R.string.tab_title_song_list);
            case 1:
                return mContext.getResources().getString(R.string.tab_title_folder_list);
            case 2:
                return mContext.getResources().getString(R.string.tab_title_playlist);
            default:
                return "";
        }
    }
}
