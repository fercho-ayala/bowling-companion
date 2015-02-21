package ca.josephroque.bowlingcompanion.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.melnykov.fab.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ca.josephroque.bowlingcompanion.Constants;
import ca.josephroque.bowlingcompanion.LeagueEventActivity;
import ca.josephroque.bowlingcompanion.R;
import ca.josephroque.bowlingcompanion.adapter.LeagueEventAdapter;
import ca.josephroque.bowlingcompanion.database.Contract.*;
import ca.josephroque.bowlingcompanion.database.DatabaseHelper;
import ca.josephroque.bowlingcompanion.dialog.NewLeagueEventDialog;

/**
 * Created by josephroque on 15-02-19.
 * <p/>
 * Location ca.josephroque.bowlingcompanion.fragments
 * in project Bowling Companion
 */
public class LeagueEventFragment extends Fragment
{

    private static final String TAG = "LeagueEventFragment";

    private RecyclerView mLeagueEventRecycler;
    private RecyclerView.Adapter mLeagueEventAdapter;

    private long mBowlerId = -1;
    private List<Long> mListLeagueEventIds;
    private List<String> mListLeagueEventNames;
    private List<Short> mListLeagueEventAverages;
    private List<Byte> mListLeagueEventNumberOfGames;

    public static LeagueEventFragment newInstance(boolean eventMode)
    {
        LeagueEventFragment leagueEventFragment = new LeagueEventFragment();
        Bundle args = new Bundle();
        args.putBoolean(Constants.EXTRA_EVENT_MODE, eventMode);
        leagueEventFragment.setArguments(args);

        return leagueEventFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstance)
    {
        View rootView = inflater.inflate(R.layout.fragment_leagues_events, container, false);

        mListLeagueEventIds = new ArrayList<>();
        mListLeagueEventNames = new ArrayList<>();
        mListLeagueEventAverages = new ArrayList<>();
        mListLeagueEventNumberOfGames = new ArrayList<>();

        mLeagueEventRecycler = (RecyclerView) rootView.findViewById(R.id.recyclerView_leagues_events);
        mLeagueEventRecycler.setHasFixedSize(true);

        RecyclerView.LayoutManager leagueLayoutManager = new LinearLayoutManager(getActivity());
        mLeagueEventRecycler.setLayoutManager(leagueLayoutManager);

        mLeagueEventAdapter = new LeagueEventAdapter(
                getActivity(),
                mListLeagueEventIds,
                mListLeagueEventNames,
                mListLeagueEventAverages,
                mListLeagueEventNumberOfGames,
                isEventMode());
        mLeagueEventRecycler.setAdapter(mLeagueEventAdapter);

        FloatingActionButton floatingActionButton = (FloatingActionButton)rootView.findViewById(R.id.fab_new_league_event);
        floatingActionButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                showNewLeagueOrEventDialog();
            }
        });

        return rootView;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        SharedPreferences preferences = getActivity().getSharedPreferences(Constants.PREFERENCES, Activity.MODE_PRIVATE);
        mBowlerId = preferences.getLong(Constants.PREFERENCE_ID_BOWLER, -1);

        mListLeagueEventIds.clear();
        mListLeagueEventNames.clear();
        mListLeagueEventAverages.clear();
        mListLeagueEventNumberOfGames.clear();

        new LoadLeaguesEventsTask().execute();
    }

    private void showNewLeagueOrEventDialog()
    {
        DialogFragment dialog = new NewLeagueEventDialog();
        Bundle bundle = new Bundle();
        bundle.putBoolean(Constants.EXTRA_EVENT_MODE, isEventMode());
        dialog.setArguments(bundle);
        dialog.show(getActivity().getSupportFragmentManager(), "NewLeagueEventDialogFragment");
    }

    private boolean isEventMode()
    {
        return getArguments().getBoolean(Constants.EXTRA_EVENT_MODE);
    }

    public void addNewLeagueOrEvent(String leagueName, byte numberOfGames)
    {
        boolean validInput = true;
        String invalidInputMessage = null;

        if (numberOfGames < 1 || numberOfGames > Constants.MAX_NUMBER_LEAGUE_GAMES)
        {
            validInput = false;
            invalidInputMessage = "The number of games must be between 1 and "
                    + Constants.MAX_NUMBER_LEAGUE_GAMES
                    + " (inclusive).";
        }
        else if (mListLeagueEventNames.contains(leagueName))
        {
            validInput = false;
            invalidInputMessage = "That name has already been used. You must choose another.";
        }

        if (!validInput)
        {
            AlertDialog.Builder invalidInputBuilder = new AlertDialog.Builder(getActivity());
            invalidInputBuilder.setMessage(invalidInputMessage)
                    .setCancelable(false)
                    .setPositiveButton(Constants.DIALOG_OKAY, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            dialog.dismiss();
                        }
                    })
                    .create()
                    .show();
            return;
        }

        new AddNewLeagueEventTask().execute(leagueName, numberOfGames);
    }

    private class LoadLeaguesEventsTask extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... params)
        {
            SQLiteDatabase database = DatabaseHelper.getInstance(getActivity()).getReadableDatabase();

            String rawLeagueEventQuery = "SELECT "
                    + LeagueEntry.TABLE_NAME + "." + LeagueEntry._ID + " AS lid, "
                    + LeagueEntry.COLUMN_NAME_LEAGUE_NAME + ", "
                    + LeagueEntry.COLUMN_NAME_NUMBER_OF_GAMES + ", "
                    + " AVG(" + GameEntry.COLUMN_NAME_GAME_FINAL_SCORE + ") AS avg"
                    + " FROM " + LeagueEntry.TABLE_NAME
                    + " LEFT JOIN " + GameEntry.TABLE_NAME
                    + " ON lid=" + GameEntry.COLUMN_NAME_LEAGUE_ID
                    + " WHERE " + LeagueEntry.COLUMN_NAME_BOWLER_ID + "=? AND " + LeagueEntry.COLUMN_NAME_IS_EVENT + "=?"
                    + " GROUP BY lid"
                    + " ORDER BY " + LeagueEntry.COLUMN_NAME_DATE_MODIFIED + " DESC";
            String[] rawLeagueEventArgs ={String.valueOf(mBowlerId), String.valueOf(isEventMode() ? 1:0)};

            Cursor leagueEventCursor = database.rawQuery(rawLeagueEventQuery, rawLeagueEventArgs);
            if (leagueEventCursor.moveToFirst())
            {
                while(!leagueEventCursor.isAfterLast())
                {
                    String leagueEventName = leagueEventCursor.getString(leagueEventCursor.getColumnIndex(LeagueEntry.COLUMN_NAME_LEAGUE_NAME));
                    long leagueEventId = leagueEventCursor.getLong(leagueEventCursor.getColumnIndex("lid"));
                    byte numberOfGames = (byte)leagueEventCursor.getInt(leagueEventCursor.getColumnIndex(LeagueEntry.COLUMN_NAME_NUMBER_OF_GAMES));
                    short leagueEventAverage = (short)leagueEventCursor.getInt(leagueEventCursor.getColumnIndex("avg"));
                    mListLeagueEventIds.add(leagueEventId);
                    mListLeagueEventNames.add(leagueEventName);
                    mListLeagueEventAverages.add(leagueEventAverage);
                    mListLeagueEventNumberOfGames.add(numberOfGames);

                    leagueEventCursor.moveToNext();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void param)
        {
            mLeagueEventAdapter.notifyDataSetChanged();
        }
    }

    private class AddNewLeagueEventTask extends AsyncTask<Object, Void, Void>
    {
        @Override
        protected Void doInBackground(Object... params)
        {
            String leagueName = params[0].toString();
            byte numberOfGames = (Byte)params[1];

            long newId = -1;
            SQLiteDatabase database = DatabaseHelper.getInstance(getActivity()).getWritableDatabase();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            ContentValues values = new ContentValues();
            values.put(LeagueEntry.COLUMN_NAME_LEAGUE_NAME, leagueName);
            values.put(LeagueEntry.COLUMN_NAME_DATE_MODIFIED, dateFormat.format(new Date()));
            values.put(LeagueEntry.COLUMN_NAME_BOWLER_ID, mBowlerId);
            values.put(LeagueEntry.COLUMN_NAME_NUMBER_OF_GAMES, numberOfGames);

            database.beginTransaction();
            try
            {
                newId = database.insert(LeagueEntry.TABLE_NAME, null, values);
                database.setTransactionSuccessful();
            }
            catch (Exception ex)
            {
                Log.w(TAG, "Error adding new league: " + ex.getMessage());
            }
            finally
            {
                database.endTransaction();
            }

            //Adds the league to the top of the list (it is the most recent)
            mListLeagueEventIds.add(0, newId);
            mListLeagueEventNames.add(0, leagueName);
            mListLeagueEventAverages.add(0, (short)0);
            mListLeagueEventNumberOfGames.add(0, numberOfGames);

            return null;
        }

        @Override
        protected void onPostExecute(Void param)
        {
            mLeagueEventAdapter.notifyItemInserted(0);
        }
    }
}
