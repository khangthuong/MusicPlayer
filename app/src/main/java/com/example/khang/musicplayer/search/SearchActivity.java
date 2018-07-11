package com.example.khang.musicplayer.search;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.khang.musicplayer.MyLoaderCallBack;
import com.example.khang.musicplayer.R;
import com.example.khang.musicplayer.util.MusicPlayer;

import static com.example.khang.musicplayer.playlist.SongPickerActivity.SEARCH_KEY;


public class SearchActivity extends AppCompatActivity {

    private final String TAG = "SearchActivity1";
    private AnimationDrawable animationDrawable;
    private Context context;
    private ListView listView;
    private SearchAdapter searchAdapter;
    private Menu menu;
    private MyLoaderCallBack myLoaderCallBack;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        context = this;
        CoordinatorLayout coordinatorLayout = (CoordinatorLayout)findViewById(R.id.search_layout_root);
        animationDrawable =(AnimationDrawable)coordinatorLayout.getBackground();
        animationDrawable.setEnterFadeDuration(5000);
        animationDrawable.setExitFadeDuration(2000);
        Window w = getWindow(); // in Activity's onCreate() for instance
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        listView = (ListView)findViewById(R.id.list_view_search_activity);
        listView.setDivider(null);
        searchAdapter = new SearchAdapter(context, this, null, 0);
        listView.setAdapter(searchAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
                Cursor c = searchAdapter.getCursor();
                c.moveToPosition(pos);
                int songID = c.getInt(MusicPlayer.SongListQuery.ID);
                Intent resultIntent = new Intent();
                resultIntent.putExtra("songID", songID);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
        myLoaderCallBack = new MyLoaderCallBack(context, null, null, null, null, searchAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search_activity, menu);
        this.menu = menu;

        MenuItem menuItem = menu.findItem(R.id.action_search_picker);
        SearchView searchView = (SearchView)menuItem.getActionView();
        TextView searchTextView = (TextView)searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        searchAdapter.setsearchTextView(searchTextView);
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
}
