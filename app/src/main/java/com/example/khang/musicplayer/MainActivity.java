package com.example.khang.musicplayer;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.khang.musicplayer.folderlist.FolderListFragment;
import com.example.khang.musicplayer.playlist.MusicPlayerProvider;
import com.example.khang.musicplayer.playlist.PlayListFragment;
import com.example.khang.musicplayer.search.SearchActivity;
import com.example.khang.musicplayer.util.MusicPlayer;

import static com.example.khang.musicplayer.MyLoaderCallBack.sArtworkUri;
import static com.example.khang.musicplayer.util.MusicPlayer.ACTION_PLAY;
import static com.example.khang.musicplayer.util.MusicPlayer.PLAY_NEW_AUDIO;

public class MainActivity extends AppCompatActivity implements
        OnFragmentInteractionListener,
        TabLayout.OnTabSelectedListener, ViewPager.OnPageChangeListener,
        MyLoaderCallBack.OnLoaderFinishListener,
        PlayingLayout.OnPlayListener {

    private final String TAG = "MainActivity1";
    private final int REQUEST_CODE_START_ACTIVITY_SONG_PICKER = 1003;
    private final int PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 1001;
    private final int PERMISSION_REQUEST_READ_PHONE_STATE = 1002;
    public static final String TAB_STATUS = "TabStatus";
    public static final String CURRENT_TAB = "CurrentTab";
    public static final String SONG_STATUS = "SongStatus";
    public static final String PLAY_FROM = "PlayFrom";
    public static final String ISREPEATALL = "isRepeatAll";
    public static final String ISPLAYSHUFFLE = "isPlayShuffle";
    public static final String PLAY_FROM_FOLDER = "PlayFromFolder";
    public static final String PLAY_FROM_PLAYLIST = "PlayFromPlaylist";
    public static final String CURRENT_SONG = "CurrentSong";
    private Intent playerIntent;
    private TabLayout.TabLayoutOnPageChangeListener mTabLayoutOnPageChangeListener;
    private ViewPagerAdapter viewPagerAdapter;
    private ViewPager viewPager;
    private TabLayout tabLayout;
    private AnimationDrawable animationDrawable;
    public static int mSelectedTab = 0;
    private SharedPreferences mPref = null;
    private SharedPreferences.Editor mEditor = null;
    private PlayingLayout playingLayout;
    private MediaPlayerService player;
    private boolean serviceBound = false;
    private Context context;
    private SeekBar seekBar;
    private TextView currentTimeTxt;
    public static boolean isRepeatAll = true;
    public static boolean isShuffleOn = false;
    public static String playFrom = "song_list";
    public static int playListID = -1;
    private int hasStoragePermission = -1;
    private int hasPhoneStatePermission = -1;
    public final static int MULTIPLE_PERMISSIONS = 1009;
    private String[] permissions = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        CoordinatorLayout coordinatorLayout = (CoordinatorLayout)findViewById(R.id.root);
        animationDrawable =(AnimationDrawable)coordinatorLayout.getBackground();
        animationDrawable.setEnterFadeDuration(5000);
        animationDrawable.setExitFadeDuration(2000);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Window w = getWindow(); // in Activity's onCreate() for instance
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        checkPermission();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("ServiceState", serviceBound);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem repeat = menu.findItem(R.id.action_repeat);
        repeat.setIcon(isRepeatAll == true ? R.drawable.ic_repeat_24dp_all : R.drawable.ic_repeat_one_24dp);
        MenuItem shuffle = menu.findItem(R.id.action_shuffle);
        shuffle.setIcon(isShuffleOn == true ? R.drawable.ic_shuffle_24dp : R.drawable.ic_shuffle_24dp_off);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_shuffle) {
            if (!isShuffleOn) {
                item.setIcon(R.drawable.ic_shuffle_24dp);
                isShuffleOn = true;
                Toast.makeText(getApplicationContext(), "Shuffle is On", Toast.LENGTH_LONG).show();
            } else {
                item.setIcon(R.drawable.ic_shuffle_24dp_off);
                isShuffleOn = false;
                Toast.makeText(getApplicationContext(), "Shuffle is Off", Toast.LENGTH_LONG).show();
            }
            mPref = getSharedPreferences(ISPLAYSHUFFLE, Context.MODE_PRIVATE);
            mEditor = mPref.edit();
            mEditor.putBoolean(ISPLAYSHUFFLE, isShuffleOn);
            return true;
        }

        if (id == R.id.action_repeat) {
            if (isRepeatAll) {
                item.setIcon(R.drawable.ic_repeat_one_24dp);
                isRepeatAll = false;
                Toast.makeText(getApplicationContext(), "Repeat Once", Toast.LENGTH_LONG).show();
            } else {
                item.setIcon(R.drawable.ic_repeat_24dp_all);
                isRepeatAll = true;
                Toast.makeText(getApplicationContext(), "Repeat All", Toast.LENGTH_LONG).show();
            }
            mPref = getSharedPreferences(ISREPEATALL, Context.MODE_PRIVATE);
            mEditor = mPref.edit();
            mEditor.putBoolean(ISREPEATALL, isRepeatAll);
            return true;
        }

        if (id == R.id.action_search) {
            Intent startSearchActivityIntent = new Intent(this, SearchActivity.class);
            startActivityForResult(startSearchActivityIntent, REQUEST_CODE_START_ACTIVITY_SONG_PICKER);
            return true;
        }

        if (id == R.id.action_about) {
            Intent startAboutActivityIntent = new Intent(this, AboutActivity.class);
            startActivity(startAboutActivityIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //activity starting
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "MainActivity - onStart - binding...");
        animationDrawable.start();
    }

    //activity stopping
    @Override
    protected void onStop() {
        super.onStop();
        animationDrawable.stop();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();


    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPress()");
        if (mSelectedTab == 0) {
            if (player != null &&
                    player.getMediaPlayingStatus() == MediaPlayerService.PlaybackStatus.PLAYING) {
                moveTaskToBack(true);
            } else {
                finish();
            }
        }

        boolean needFinishedActivity = true;
        if (viewPagerAdapter != null && ((mSelectedTab == 1) || mSelectedTab == 2)) {

            for (Fragment fg : getSupportFragmentManager().getFragments()) {
                if (mSelectedTab == 1 && fg instanceof FolderListFragment && (!fg.isDetached())) {
                    needFinishedActivity = ((FolderListFragment)fg).setListViewVisibility();
                    Log.d(TAG, "onBackPress() from FolderListFragment");
                    break;
                }

                if (mSelectedTab == 2 && fg instanceof PlayListFragment && (!fg.isDetached())) {
                    needFinishedActivity = ((PlayListFragment)fg).setListViewVisibility();
                    Log.d(TAG, "onBackPress() from PlayListFragment");
                    break;
                }
            }

        }

        if (needFinishedActivity) {
            if (player != null &&
                    player.getMediaPlayingStatus() == MediaPlayerService.PlaybackStatus.PLAYING) {
                moveTaskToBack(true);
            } else {
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        if (isFinishing()) {
            doUnbindService();
            stopService(playerIntent);
            serviceBound = false;
        }

        if (mPref != null) {
            mPref = null;
        }

        if (mEditor != null) {
            mEditor = null;
        }

        if (playingLayout != null) {
            playingLayout.unRegister_notificationActionReceiver();
        }
        isRepeatAll = true;
        isShuffleOn = false;
    }

    private void createTabs() {
        Log.d(TAG, "createTabs()");
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), this);
        viewPager = (ViewPager)findViewById(R.id.view_pager);
        tabLayout = (TabLayout)findViewById(R.id.tab_layout);
        viewPager.setAdapter(viewPagerAdapter);
        mTabLayoutOnPageChangeListener = new TabLayout.TabLayoutOnPageChangeListener(tabLayout);
        viewPager.addOnPageChangeListener(mTabLayoutOnPageChangeListener);
        viewPager.addOnPageChangeListener(this);
        for (int i = 0; i < 3; ++i) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            LinearLayout linearLayout = (LinearLayout) inflater
                    .inflate(R.layout.tab_indicator_holo, null);
            TextView textView = (TextView) linearLayout.findViewById(R.id.tab_text);
            if (viewPagerAdapter.getPageTitle(i) != null) {
                textView.setText(viewPagerAdapter.getPageTitle(i).toString().toUpperCase());
            }

            tabLayout.addTab(tabLayout.newTab().setText(viewPagerAdapter.getPageTitle(i).toString()).setCustomView(textView));
            ((ViewGroup) tabLayout.getTabAt(i).getCustomView().getParent())
                    .setBackground(ContextCompat.getDrawable(this, R.drawable.tab_widget_ripple));
        }
        tabLayout.addOnTabSelectedListener(this);
    }

    public static void setSelectedTab (int pos) {
        mSelectedTab = pos;
    }

    private void setTabSharedPreference() {
        mPref = getSharedPreferences(TAB_STATUS, Context.MODE_PRIVATE);

        int position = mPref.getInt(CURRENT_TAB, -1);
        if (position == -1) {
            position = 0;
        }
        TabLayout.Tab tb = tabLayout.getTabAt(position);
        onTabSelected(tb);
        setSelectedTab(position);
        viewPager.setCurrentItem(mSelectedTab);

        mPref = getSharedPreferences(SONG_STATUS, Context.MODE_PRIVATE);
        String from = mPref.getString(PLAY_FROM, null);
        if (from == null) {
            from = "song_list";
        }
        playFrom = from;
        mPref = getSharedPreferences(ISREPEATALL, Context.MODE_PRIVATE);
        isRepeatAll = mPref.getBoolean(ISREPEATALL, true);
        mPref = getSharedPreferences(ISPLAYSHUFFLE, Context.MODE_PRIVATE);
        isShuffleOn = mPref.getBoolean(ISPLAYSHUFFLE, false);
    }

    //Binding this Client to the AudioPlayer Service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;
            player.setSeekBar(seekBar);
            player.setCurrentTimeTxt(currentTimeTxt);
            Log.d(TAG, "Service Bound");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            player = null;
            serviceBound = false;
            Log.d(TAG, "Service UnBound");
        }
    };

    //    unbind from the service
    private void doUnbindService() {
        Log.d(TAG, "Unbinding...");
        unbindService(serviceConnection);
        serviceBound = false;
    }

    //    bind to the service
    private void doBindToService() {
        Log.d(TAG, "Binding...");
        if (!serviceBound) {
            Intent bindIntent = new Intent(this, MediaPlayerService.class);
            serviceBound = bindService(bindIntent, serviceConnection,
                    Context.BIND_AUTO_CREATE);
        }
    }

    private void playAudio(String media, String name, String singer,
                           String album, Long duration, String uri) {
        player.setSeekBar(seekBar);
        player.setCurrentTimeTxt(currentTimeTxt);
        Intent broadcastIntent = new Intent(PLAY_NEW_AUDIO);
        broadcastIntent.putExtra("new_media", media);
        broadcastIntent.putExtra("song_name", name);
        broadcastIntent.putExtra("song_singer", singer);
        broadcastIntent.putExtra("song_album", album);
        broadcastIntent.putExtra("song_thumbUri", uri.toString());
        broadcastIntent.putExtra("song_duration", duration);
        sendBroadcast(broadcastIntent);
    }

    @Override
    public void onFragmentInteraction(Cursor c) {
    }

    @Override
    public void onItemClick(Cursor c, int pos, String from, int playListID) {
        mPref = getSharedPreferences(SONG_STATUS, Context.MODE_PRIVATE);
        mEditor = mPref.edit();
        mEditor.putInt(SONG_STATUS, pos);
        mEditor.putString(PLAY_FROM, from);
        mEditor.putInt(PLAY_FROM_PLAYLIST, playListID);
        playFrom = from;
        this.playListID = playListID;
        mEditor.apply();

        playingLayout.setCursor(c);
        Log.d("MainActivity1", "onItemClick Save to Preferences: " + "songPos = " + pos + " playFrom = " + from + " playListID = " + playListID);
        String mediaFile;
        String songName;
        String songSinger;
        String songAlbum;
        Long albumID;
        long duration;
        Uri uri;

        if (from.equals("playlist")) {
            mediaFile = c.getString(MusicPlayerProvider.COLUMN_SONG_PATH);
            songName = c.getString(MusicPlayerProvider.COLUMN_SONG_NAME);
            songSinger = c.getString(MusicPlayerProvider.COLUMN_SONG_ARTIST);
            songAlbum = "khang";//c.getString(MusicPlayerProvider.COLUMN_ALBUM_KEY);
            albumID = c.getLong(MusicPlayerProvider.COLUMN_ALBUM_ID);
            duration = c.getLong(MusicPlayerProvider.COLUMN_SONG_DURATION);
            uri = ContentUris.withAppendedId(sArtworkUri, albumID);
        } else {
            mediaFile = c.getString(MusicPlayer.SongListQuery.DATA);
            songName = c.getString(MusicPlayer.SongListQuery.TITLE);
            songSinger = c.getString(MusicPlayer.SongListQuery.ARTIST);
            songAlbum = c.getString(MusicPlayer.SongListQuery.ALBUM_KEY);
            albumID = c.getLong(MusicPlayer.SongListQuery.ALBUM_ID);
            duration = c.getLong(MusicPlayer.SongListQuery.DURATION);
            uri = ContentUris.withAppendedId(sArtworkUri, albumID);
        }

        playingLayout.setMediafile(mediaFile, pos);
        playingLayout.setData(from);
        playAudio(mediaFile, songName, songSinger, songAlbum, duration, uri.toString());
        playingLayout.setPausePlayBtn(true);

    }

    @Override
    public void onLoadFinished(Cursor c) {
        mPref = getSharedPreferences(SONG_STATUS, Context.MODE_PRIVATE);
        int pos = mPref.getInt(SONG_STATUS, 0);
        String from = mPref.getString(PLAY_FROM, "song_list");
        c.moveToPosition(pos);
        playingLayout.setCursor(c);
        String mediaFile = null;
        Log.d("MainActivity1", "onLoadFinished Get from Preferences: " + "songPos = " + pos + " playFrom = " + from + " playListID = " + playListID);

        if (from.equals("playlist")) {
            mediaFile = c.getString(MusicPlayerProvider.COLUMN_SONG_PATH);
            seekBar.setMax((int) c.getLong(MusicPlayerProvider.COLUMN_SONG_DURATION));
        }

        if (from.equals("song_list") || from.equals("folder")) {
            mediaFile = c.getString(MusicPlayer.SongListQuery.DATA);
            seekBar.setMax((int) c.getLong(MusicPlayer.SongListQuery.DURATION));
        }

        playingLayout.setMediafile(mediaFile, pos);
        playingLayout.setData(playFrom);
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        setSelectedTab(tab.getPosition());
        ((TextView) tab.getCustomView()).setSelected(true);
        ((TextView) tab.getCustomView())
                .setTypeface(Typeface.create("sec-roboto-condensed", Typeface.BOLD));
        ((TextView) tab.getCustomView()).setTextColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        viewPager.setCurrentItem(tab.getPosition());

        mPref = getSharedPreferences(TAB_STATUS, Context.MODE_PRIVATE);
        mEditor = mPref.edit();
        mEditor.putInt(CURRENT_TAB, mSelectedTab);
        mEditor.apply();

    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        ((TextView) tab.getCustomView()).setSelected(false);
        ((TextView) tab.getCustomView())
                .setTypeface(Typeface.create("sec-roboto-condensed", Typeface.NORMAL));
        ((TextView) tab.getCustomView()).setTextColor(ContextCompat.getColor(this, R.color.colorWhite));

        if (tab.getPosition() == 1) {
            for (Fragment fg : getSupportFragmentManager().getFragments()) {
                if (fg instanceof FolderListFragment && (!fg.isDetached())) {
                    ((FolderListFragment)fg).setListViewVisibility();
                    break;
                }
            }
        }

        if (tab.getPosition() == 2) {
            for (Fragment fg : getSupportFragmentManager().getFragments()) {
                if (fg instanceof PlayListFragment && (!fg.isDetached())) {
                    ((PlayListFragment)fg).setListViewVisibility();
                    break;
                }
            }
        }
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onPlay(Cursor c, int pos) {
        String mediaFile;
        String songName;
        String songSinger;
        String songAlbum;
        Long albumID;
        long duration;
        Uri uri;

        if (playFrom.equals("playlist")) {
            mediaFile = c.getString(MusicPlayerProvider.COLUMN_SONG_PATH);
            songName = c.getString(MusicPlayerProvider.COLUMN_SONG_NAME);
            songSinger = c.getString(MusicPlayerProvider.COLUMN_SONG_ARTIST);
            songAlbum = "khang";
            albumID = c.getLong(MusicPlayerProvider.COLUMN_ALBUM_ID);
            duration = c.getLong(MusicPlayerProvider.COLUMN_SONG_DURATION);
            uri = ContentUris.withAppendedId(sArtworkUri, albumID);
        } else {
            mediaFile = c.getString(MusicPlayer.SongListQuery.DATA);
            songName = c.getString(MusicPlayer.SongListQuery.TITLE);
            songSinger = c.getString(MusicPlayer.SongListQuery.ARTIST);
            songAlbum = c.getString(MusicPlayer.SongListQuery.ALBUM_KEY);
            albumID = c.getLong(MusicPlayer.SongListQuery.ALBUM_ID);
            duration = c.getLong(MusicPlayer.SongListQuery.DURATION);
            uri = ContentUris.withAppendedId(sArtworkUri, albumID);
        }

        Intent broadcastIntent = new Intent(ACTION_PLAY);
        broadcastIntent.putExtra("new_media", mediaFile);
        broadcastIntent.putExtra("song_name", songName);
        broadcastIntent.putExtra("song_singer", songSinger);
        broadcastIntent.putExtra("song_album", songAlbum);
        broadcastIntent.putExtra("song_thumbUri", uri.toString());
        broadcastIntent.putExtra("song_duration", duration);

        sendBroadcast(broadcastIntent);
        if (player.getMediaPlayingStatus() == MediaPlayerService.PlaybackStatus.PLAYING) {
            playingLayout.setPausePlayBtn(false);
        } else {
            playingLayout.setPausePlayBtn(true);
        }
    }

    @Override
    public void onNext(Cursor c, int pos) {
        onItemClick(c, pos, playFrom, playListID);
    }

    @Override
    public void onPrev(Cursor c, int pos) {
        onItemClick(c, pos, playFrom, playListID);
    }

    @Override
    public void onStop (Cursor c, int pos) {
        playingLayout.setPausePlayBtn(false);
    }

    private void onPermissionGetGranted (){
        createTabs();
        setTabSharedPreference();
        playingLayout = new PlayingLayout(this, getSupportFragmentManager());
        playingLayout.initView();
        seekBar = playingLayout.getSeekBar();
        currentTimeTxt = playingLayout.getCurrentTimeTxt();

        playerIntent = new Intent(this, MediaPlayerService.class);
        startService(playerIntent);
        Log.d("MainActivity1", "onPermissionGetGranted Service start....");
        doBindToService();
    }
    private void checkPermission() {
        hasStoragePermission = checkSelfPermission(permissions[0]);
        hasPhoneStatePermission = checkSelfPermission(permissions[1]);

        if (hasStoragePermission != PackageManager.PERMISSION_GRANTED &&
                hasPhoneStatePermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(permissions, MULTIPLE_PERMISSIONS);
        } else if (hasPhoneStatePermission == PackageManager.PERMISSION_GRANTED &&
                hasStoragePermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{permissions[0]}, PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
        } else if (hasStoragePermission == PackageManager.PERMISSION_GRANTED &&
                hasPhoneStatePermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{permissions[1]}, PERMISSION_REQUEST_READ_PHONE_STATE);
        } else {
            onPermissionGetGranted();
        }
        /*if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(context, R.style.MyCustomThemeForDialog);
                dialog.setTitle("Permission Explain");
                dialog.setMessage("You have to provide READ EXTERNAL STORAGE permission");
                dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
                    }
                });
                dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                dialog.show();
            }
        } */
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        Log.d(TAG, "onRequestPermissionsResult()");
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hasStoragePermission = PackageManager.PERMISSION_GRANTED;
                }
                if (grantResults.length > 0 && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    hasPhoneStatePermission = PackageManager.PERMISSION_GRANTED;
                }
                break;
            case PERMISSION_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hasStoragePermission = PackageManager.PERMISSION_GRANTED;
                }
                break;
            case PERMISSION_REQUEST_READ_PHONE_STATE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hasPhoneStatePermission = PackageManager.PERMISSION_GRANTED;
                }
                break;
        }

        if (hasStoragePermission == PackageManager.PERMISSION_GRANTED &&
                hasPhoneStatePermission == PackageManager.PERMISSION_GRANTED) {
            onPermissionGetGranted();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_START_ACTIVITY_SONG_PICKER && resultCode == Activity.RESULT_OK) {
            int songID = data.getIntExtra("songID", 0);
            Log.d(TAG, "songID: " + songID);
            Cursor c = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    MusicPlayer.SongListQuery.PROJECTION,
                    MediaStore.Audio.AudioColumns._ID + " = " + songID,
                    null,
                    MusicPlayer.SongListQuery.SORT_ORDER);
            c.moveToFirst();
            onItemClick(c, 0, "song_list", -1);
        }
    }
}
