package com.example.khang.musicplayer.playlist;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.khang.musicplayer.MyLoaderCallBack;
import com.example.khang.musicplayer.R;
import com.example.khang.musicplayer.util.MusicPlayer;

import static com.example.khang.musicplayer.playlist.PlayListFragment.NUMBERSONGINPLAYLIST;
import static com.example.khang.musicplayer.playlist.PlayListFragment.PLAYLIST_ID;

public class SongPickerActivity extends AppCompatActivity {

    private final String TAG = "SongPickerActivity";
    private AnimationDrawable animationDrawable;
    private Context context;
    private SongPickerAdapter songPickerAdapter;
    private ListView listView;
    private MyLoaderCallBack myLoaderCallBack;
    private TextView mSelectionTitle;
    private CheckBox mSelectAll;
    private LinearLayout mSelectAllLayout;
    private int totalPlaylist;
    private int numberSongPicked = 0;
    private Menu menu;
    public static final String SEARCH_KEY = "search_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_picker);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        context = this;
        CoordinatorLayout coordinatorLayout = (CoordinatorLayout)findViewById(R.id.song_picker_root);
        animationDrawable =(AnimationDrawable)coordinatorLayout.getBackground();
        animationDrawable.setEnterFadeDuration(5000);
        animationDrawable.setExitFadeDuration(2000);
        Window w = getWindow(); // in Activity's onCreate() for instance
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        Intent intent = getIntent();
        totalPlaylist = intent.getIntExtra(PLAYLIST_ID, 0);
        numberSongPicked = intent.getIntExtra(NUMBERSONGINPLAYLIST, 0);

        mSelectAllLayout = (LinearLayout) findViewById(R.id.select_all_layout);
        mSelectionTitle = (TextView) findViewById(R.id.selection_title);
        mSelectAll = (CheckBox) findViewById(R.id.select_all_cb);
        mSelectAllLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (mSelectAll.isChecked()) {
                    mSelectAll.setChecked(false);
                    onSelectAllCheck(false);
                    mSelectionTitle.setText("");
                    setEnableAddMenu(false);
                    numberSongPicked = 0;
                } else {
                    mSelectAll.setChecked(true);
                    onSelectAllCheck(true);
                    setEnableAddMenu(true);
                    mSelectionTitle.setText(songPickerAdapter.getCount() + "");
                    numberSongPicked = songPickerAdapter.getCount();
                }

            }
        });

        listView = (ListView)findViewById(R.id.list_view_song_picker);
        listView.setDivider(null);
        songPickerAdapter = new SongPickerAdapter(context, this, null, 0);
        listView.setAdapter(songPickerAdapter);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        songPickerAdapter.setListView(listView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                if (!listView.isItemChecked(i)) {
                    listView.setItemChecked(i, false);
                    Log.d(TAG,"set check false");
                    numberSongPicked--;
                } else {
                    listView.setItemChecked(i, true);
                    Log.d(TAG,"set check true");
                    numberSongPicked++;
                }
                Log.d(TAG, "onItemClicked " + listView.isItemChecked(i) + " " + i);
                songPickerAdapter.notifyDataSetChanged();

                if (numberSongPicked <= 0) {
                    setEnableAddMenu(false);
                } else {
                    setEnableAddMenu(true);
                }

            }
        });
        myLoaderCallBack = new MyLoaderCallBack(context, null, null, null, songPickerAdapter, null);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_song_picker_activity, menu);
        this.menu = menu;
        setEnableAddMenu(false);

        MenuItem menuItem = menu.findItem(R.id.action_search_picker);
        SearchView searchView = (SearchView)menuItem.getActionView();
        TextView searchTextView = (TextView)searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        songPickerAdapter.setsearchTextView(searchTextView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Toast.makeText(context, query, Toast.LENGTH_SHORT).show();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Bundle b = new Bundle();
                b.putString(SEARCH_KEY, newText);
                getSupportLoaderManager().restartLoader(MusicPlayer.SongListQuery.QUERY_ID_FOR_SONG_PICKER_FILTER, b, myLoaderCallBack);
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Log.d(TAG, "onOptionsItemSelected()");
        int id = item.getItemId();
        if (id == R.id.action_add) {
            addSongToPlaylist();
            ContentValues values = new ContentValues();
            values.put(MusicPlayerProvider.TAG_TOTAL_SONG, numberSongPicked);
            getContentResolver().update(MusicPlayerProvider.CONTENT_URI, values,
                    MusicPlayerProvider.TAG_INDEX + " = " + totalPlaylist, null);
            Intent resultIntent = new Intent();
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        }

        //noinspection SimplifiableIfStatement
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        animationDrawable.start();
        getSupportLoaderManager().restartLoader(MusicPlayer.SongListQuery.QUERY_ID_FOR_SONG_PICKER, null, myLoaderCallBack);
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed()");
        finish();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        animationDrawable.stop();
    }

    private void onSelectAllCheck(boolean checked) {
        Log.d(TAG, "onSelectAllCheck(): checked=" + checked);
        if (listView == null) {
            return;
        }

        for (int i = 0; i < songPickerAdapter.getCursor().getCount(); ++i) {
            if (checked) {
                listView.setItemChecked(i, true);
            } else {
                listView.setItemChecked(i, false);
            }
        }
        songPickerAdapter.notifyDataSetChanged();
        listView.invalidate();
    }

    private void addSongToPlaylist() {
        Log.d(TAG, "addSongToPlaylist");
        Cursor c = songPickerAdapter.getCursor();
        for (int i = 0; i < songPickerAdapter.getCount(); ++i) {
            if (listView.isItemChecked(i)) {
                c.moveToPosition(i);
                ContentValues values = new ContentValues();
                values.put(MusicPlayerProvider.TAG_PLAYLSIT_ID, totalPlaylist);
                values.put(MusicPlayerProvider.TAG_SONG_PATH, c.getString(MusicPlayer.SongListQuery.DATA));
                values.put(MusicPlayerProvider.TAG_SONG_NAME, c.getString(MusicPlayer.SongListQuery.TITLE));
                values.put(MusicPlayerProvider.TAG_SONG_ARTIST, c.getString(MusicPlayer.SongListQuery.ARTIST));
                values.put(MusicPlayerProvider.TAG_SONG_DURATION, c.getLong(MusicPlayer.SongListQuery.DURATION));
                values.put(MusicPlayerProvider.TAG_SONG_ID, c.getLong(MusicPlayer.SongListQuery.ID));
                values.put(MusicPlayerProvider.TAG_ALBUM_ID, c.getLong(MusicPlayer.SongListQuery.ALBUM_ID));
                values.put(MusicPlayerProvider.TAG_ALBUM_KEY, c.getString(MusicPlayer.SongListQuery.ALBUM_KEY));
                getContentResolver().insert(MusicPlayerProvider.CONTENT_URI_DATA, values);
            }
        }
    }

    private void setEnableAddMenu(boolean enable) {
        Log.d(TAG, "setEnableAddMenu(): enable=" + enable);
        MenuItem addMenuItem = menu.findItem(R.id.action_add);
        addMenuItem.setEnabled(enable);
    }


}
