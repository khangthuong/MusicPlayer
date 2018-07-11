package com.example.khang.musicplayer;

import android.database.Cursor;
import android.net.Uri;

/**
 * Created by sev_user on 9/21/2017.
 */
/**
 * This interface must be implemented by activities that contain this
 * fragment to allow an interaction in this fragment to be communicated
 * to the activity and potentially other fragments contained in that
 * activity.
 * <p>
 * See the Android Training lesson <a href=
 * "http://developer.android.com/training/basics/fragments/communicating.html"
 * >Communicating with Other Fragments</a> for more information.
 */
public interface OnFragmentInteractionListener {
    // TODO: Update argument type and name
    void onFragmentInteraction(Cursor c);
    void onItemClick(Cursor c, int pos, String from, int playListID);
}
