package com.example.khang.musicplayer.playlist;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.example.khang.musicplayer.MyLoaderCallBack;
import com.example.khang.musicplayer.OnFragmentInteractionListener;
import com.example.khang.musicplayer.R;
import com.example.khang.musicplayer.songlist.SongListAdapter;
import com.example.khang.musicplayer.util.MusicPlayer;
import com.google.android.gms.plus.PlusOneButton;

import static android.app.Activity.RESULT_OK;

public class PlayListFragment extends Fragment implements MyLoaderCallBack.OnLoaderFinishListener{
    private static final String TAG = "PlayListFragment";
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private PlusOneButton mPlusOneButton;

    private OnFragmentInteractionListener mListener;

    FloatingActionButton addSongBtn;
    LinearLayout playListNameLayout, playListDataLayout;
    private EditText userInputNewPlaylist;
    private TextInputLayout mTextInputLayoutId;
    private ListView listView1, listView2;
    private SongListAdapter songListAdapter;
    private PlayListAdapter playListAdapter;
    private MyLoaderCallBack myLoaderCallBack;
    public static final String PLAYLIST_ID = "playlist_id_name";
    public static final String NUMBERSONGINPLAYLIST = "number_song_in_playlist";
    public static final int REQUEST_CODE_START_ACTIVITY_SONG_PICKER = 1;
    public static final int REQUEST_CODE_START_ACTIVITY_SONG_PICKER_FROM_PLAYLIST = 2;
    private int totalPlaylsit = 0;
    private int playListID = -1;
    private int numberSongInPlayList = 0;
    private View footerView;
    private SharedPreferences mPref = null;
    private SharedPreferences.Editor mEditor = null;
    //private static final String PLAYLIST_ID = "playlist_id";

    public PlayListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PlayListFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PlayListFragment newInstance(String param1, String param2) {
        Log.d(TAG, "newInstance()");
        PlayListFragment fragment = new PlayListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_play_list, container, false);
        listView1 = (ListView)view.findViewById(R.id.list_view_playlsit_name);
        listView2 = (ListView)view.findViewById(R.id.list_view_playlist_data);
        addSongBtn = (FloatingActionButton)view.findViewById(R.id.add_btn);
        playListNameLayout = (LinearLayout)view.findViewById(R.id.playlist_name_layout);
        playListDataLayout = (LinearLayout)view.findViewById(R.id.playlist_data_layout);
        listView1.setDivider(null);
        listView2.setDivider(null);

        listView1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                playListNameLayout.setVisibility(View.GONE);
                playListDataLayout.setVisibility(View.VISIBLE);

                songListAdapter = new SongListAdapter(getContext(), getActivity(), null, 1);
                listView2.setAdapter(songListAdapter);
                Cursor c = playListAdapter.getCursor();
                Bundle b = new Bundle();
                c.moveToPosition(position);
                playListID = c.getInt(MusicPlayerProvider.COLUMN_INDEX);
                b.putInt(PLAYLIST_ID, playListID);

                mPref = getActivity().getSharedPreferences(PLAYLIST_ID, Context.MODE_PRIVATE);
                mEditor = mPref.edit();
                mEditor.putInt(PLAYLIST_ID, playListID);
                mEditor.apply();

                myLoaderCallBack = new MyLoaderCallBack(getContext(), songListAdapter, null, null, null, null);
                getLoaderManager().restartLoader(MusicPlayer.PlayListDataQuery.QUERY_ID, b, myLoaderCallBack);
            }
        });

        listView2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Cursor c = songListAdapter.getCursor();
                if (mListener != null) {
                    mListener.onItemClick(c, position, "playlist", playListID);
                }
            }
        });

        addSongBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playListNameLayout.getVisibility() == View.VISIBLE) {
                    createNewPlaylistDialog();
                    userInputNewPlaylist.requestFocus();
                }

                if (playListDataLayout.getVisibility() == View.VISIBLE) {
                    numberSongInPlayList = songListAdapter.getCount();
                    Intent intent = new Intent();
                    intent.setClass(getContext(), SongPickerActivity.class);
                    intent.putExtra(NUMBERSONGINPLAYLIST, numberSongInPlayList);
                    intent.putExtra(PLAYLIST_ID, playListID);
                    startActivityForResult(intent, REQUEST_CODE_START_ACTIVITY_SONG_PICKER_FROM_PLAYLIST);
                }
            }
        });

        LayoutInflater inflaterFooter = LayoutInflater.from(getContext());
        footerView = inflaterFooter.inflate(getResources().getLayout(R.layout.footer_view_playlist), null);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated()");
        super.onActivityCreated(savedInstanceState);
        playListAdapter = new PlayListAdapter(getContext(), getActivity(), null, 0);
        listView1.setAdapter(playListAdapter);
        myLoaderCallBack = new MyLoaderCallBack(getContext(), null, null, playListAdapter, null, null);
        myLoaderCallBack.setOnLoaderFinishListener(this);
        getLoaderManager().initLoader(MusicPlayer.PlayListNameQuery.QUERY_ID, null, myLoaderCallBack);

    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
    }

    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "onAttach()");
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        Log.d(TAG, "onDetach()");
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_START_ACTIVITY_SONG_PICKER && resultCode == Activity.RESULT_OK) {
            myLoaderCallBack = new MyLoaderCallBack(getContext(), null, null, playListAdapter, null, null);
            myLoaderCallBack.setOnLoaderFinishListener(this);
            getLoaderManager().restartLoader(MusicPlayer.PlayListNameQuery.QUERY_ID, null, myLoaderCallBack);
            Log.d(TAG, "onActivityResult(): REQUEST_CODE_START_ACTIVITY_SONG_PICKER ");
        }

        if (requestCode == REQUEST_CODE_START_ACTIVITY_SONG_PICKER_FROM_PLAYLIST && resultCode == Activity.RESULT_OK) {
            playListNameLayout.setVisibility(View.GONE);
            playListDataLayout.setVisibility(View.VISIBLE);

            Bundle b = new Bundle();
            b.putInt(PLAYLIST_ID, playListID);

            myLoaderCallBack = new MyLoaderCallBack(getContext(), songListAdapter, null, null, null, null);
            getLoaderManager().restartLoader(MusicPlayer.PlayListDataQuery.QUERY_ID, b, myLoaderCallBack);
            Log.d(TAG, "onActivityResult(): REQUEST_CODE_START_ACTIVITY_SONG_PICKER_FROM_PLAYLIST ");
        }
    }

    private void createNewPlaylistDialog() {
        Log.d(TAG, "createNewPlaylistDialog()");
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        View view = layoutInflater.inflate(R.layout.new_playlist_dialog, null);
        userInputNewPlaylist = (EditText)view.findViewById(R.id.editTextDialogUserInput);
        userInputNewPlaylist.requestFocus();
        mTextInputLayoutId = (TextInputLayout) view.findViewById(R.id.input_name);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.MyCustomThemeForDialog);
        builder.setTitle("New Playlist");
        builder.setView(view);
        builder.setPositiveButton("CREATE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String str = userInputNewPlaylist.getText().toString();
                ContentValues values = new ContentValues();
                values.put(MusicPlayerProvider.TAG_NAME, str);
                values.put(MusicPlayerProvider.TAG_TOTAL_SONG, 0);
                getContext().getContentResolver().insert(MusicPlayerProvider.CONTENT_URI, values);

                totalPlaylsit = totalPlaylsit+1;
                mTextInputLayoutId.setEnabled(false);
                Intent intent = new Intent();
                intent.putExtra(PLAYLIST_ID, totalPlaylsit);
                intent.setClass(getContext(), SongPickerActivity.class);
                startActivityForResult(intent, REQUEST_CODE_START_ACTIVITY_SONG_PICKER);

            }
        });
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.create();
        builder.show();
    }

    @Override
    public void onLoadFinished(Cursor c) {
        Log.d(TAG, "onLoadFinished()");
        totalPlaylsit = c.getCount();
        footerView.setClickable(false);
        if (listView1.getFooterViewsCount() > 0) {
            listView1.removeFooterView(footerView);
        }
        listView1.addFooterView(footerView, null, false);
        TextView textView = (TextView) footerView.findViewById(R.id.txt_total_playlist);
        textView.setText(totalPlaylsit + " Playlist");
    }

    public boolean setListViewVisibility() {
        Log.d(TAG, "setListViewVisibility()");
        if (playListDataLayout == null || playListNameLayout == null) {
            return true;
        }

        if (playListNameLayout.getVisibility() == View.VISIBLE) {
            return true;
        } else {
            playListDataLayout.setVisibility(View.GONE);
            playListNameLayout.setVisibility(View.VISIBLE);
            myLoaderCallBack = new MyLoaderCallBack(getContext(), null, null, playListAdapter, null, null);
            myLoaderCallBack.setOnLoaderFinishListener(this);
            getLoaderManager().restartLoader(MusicPlayer.PlayListNameQuery.QUERY_ID, null, myLoaderCallBack);
            return false;
        }
    }
}
