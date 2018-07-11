package com.example.khang.musicplayer.folderlist;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.khang.musicplayer.MainActivity;
import com.example.khang.musicplayer.MyLoaderCallBack;
import com.example.khang.musicplayer.OnFragmentInteractionListener;
import com.example.khang.musicplayer.R;
import com.example.khang.musicplayer.playlist.MusicPlayerProvider;
import com.example.khang.musicplayer.songlist.SongListAdapter;
import com.example.khang.musicplayer.util.MusicPlayer;
import com.google.android.gms.plus.PlusOneButton;

import java.util.ArrayList;

import static com.example.khang.musicplayer.playlist.PlayListFragment.PLAYLIST_ID;

public class FolderListFragment extends Fragment {
    private static final String TAG = "FolderListFragment";
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private ListView listView;
    private ListView listView1;
    private FolderListAdapter folderListAdapter;
    SongListAdapter songListAdapter;
    private MyLoaderCallBack myLoaderCallBack;
    private String pathStr = null;
    private SharedPreferences mPref = null;
    private SharedPreferences.Editor mEditor = null;
    private static final String SONG_PATH = "song_path";

    private OnFragmentInteractionListener mListener;

    public FolderListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FolderListFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static FolderListFragment newInstance(String param1, String param2) {
        Log.d(TAG, "newInstance()");
        FolderListFragment fragment = new FolderListFragment();
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
        View view = inflater.inflate(R.layout.fragment_folder_list, container, false);
        listView = (ListView)view.findViewById(R.id.folder_list);
        listView1 = (ListView)view.findViewById(R.id.songs_of_folder_list);
        listView.setDivider(null);
        listView1.setDivider(null);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                listView.setVisibility(View.GONE);
                listView1.setVisibility(View.VISIBLE);

                ArrayList<String> folderList = folderListAdapter.getArrayList();
                pathStr = folderList.get(position);

                songListAdapter = new SongListAdapter(getContext(), getActivity(), null, 0);
                listView1.setAdapter(songListAdapter);
                Bundle b = new Bundle();
                b.putString(MusicPlayer.SongListQuery.PATH, pathStr);
                myLoaderCallBack = new MyLoaderCallBack(getContext(), songListAdapter, folderListAdapter, null, null, null);
                getLoaderManager().restartLoader(MusicPlayer.SongListQuery.QUERY_ID_SPECIFIC_FOLDER, b, myLoaderCallBack);
            }
        });
        listView1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor c = songListAdapter.getCursor();
                if (mListener != null) {
                    mListener.onItemClick(c, position, "folder", -1);
                    mPref = getActivity().getSharedPreferences(SONG_PATH, Context.MODE_PRIVATE);
                    mEditor = mPref.edit();
                    mEditor.putString(SONG_PATH, pathStr);
                    mEditor.apply();
                }
            }
        });
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated()");
        super.onActivityCreated(savedInstanceState);
        ArrayList<String> list = new ArrayList<>();
        folderListAdapter = new FolderListAdapter(getContext(), getActivity(), list, R.layout.row);
        listView.setAdapter(folderListAdapter);
        myLoaderCallBack = new MyLoaderCallBack(getContext(), null, folderListAdapter, null, null, null);
        getLoaderManager().initLoader(MusicPlayer.SongListQuery.QUERY_ID_FOLDER, null, myLoaderCallBack);

        mPref = getActivity().getSharedPreferences(SONG_PATH, Context.MODE_PRIVATE);
        pathStr = mPref.getString(SONG_PATH, null);
        songListAdapter = new SongListAdapter(getContext(), getActivity(), null, 0);
        if (pathStr != null && MainActivity.playFrom.equals("folder")) {
            Bundle b = new Bundle();
            b.putString(MusicPlayer.SongListQuery.PATH, pathStr);
            myLoaderCallBack = new MyLoaderCallBack(getContext(), songListAdapter, folderListAdapter, null, null, null);
            getLoaderManager().restartLoader(MusicPlayer.SongListQuery.QUERY_ID_SPECIFIC_FOLDER_FROM_SHAREPREFERENCE, b, myLoaderCallBack);
        }

        if (MainActivity.playFrom.equals("song_list")) {
            myLoaderCallBack = new MyLoaderCallBack(getContext(), songListAdapter, null, null, null, null);
            getLoaderManager().restartLoader(MusicPlayer.SongListQuery.QUERY_ID, null, myLoaderCallBack);
        }

        if (MainActivity.playFrom.equals("playlist")) {
            mPref = getActivity().getSharedPreferences(PLAYLIST_ID, Context.MODE_PRIVATE);
            int id = mPref.getInt(PLAYLIST_ID, -1);
            if (id >=0) {
                Bundle b = new Bundle();
                b.putInt(PLAYLIST_ID, id);
                myLoaderCallBack = new MyLoaderCallBack(getContext(), songListAdapter, null, null, null, null);
                getLoaderManager().restartLoader(MusicPlayer.PlayListDataQuery.QUERY_ID, b, myLoaderCallBack);
            }
        }
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

    public boolean setListViewVisibility() {
        Log.d(TAG, "setListViewVisibility()");
        if (listView1 == null || listView == null) {
            return true;
        }

        if (listView.getVisibility() == View.VISIBLE) {
           return true;
        } else {
            listView1.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
            return false;
        }
    }
}
