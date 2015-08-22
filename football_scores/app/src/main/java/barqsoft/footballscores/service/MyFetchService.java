package barqsoft.footballscores.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

import barqsoft.footballscores.DatabaseContract;

/**
 * Created by yehya khaled on 3/2/2015.
 */
public class MyFetchService extends IntentService {
    public static final String LOG_TAG = MyFetchService.class.getSimpleName();

    private static final Uri API_BASE_URI = Uri.parse("http://api.football-data.org/alpha/");
    final String FIXTURES = "fixtures";
    final String SEASONS = "seasons";

    private static final String NEXT = "n";
    private static final String PAST = "p";


    public MyFetchService() {
        super("MyFetchService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        getData(NEXT, 2);
        getData(PAST, 2);
    }

    private String fetchUrl(Uri uri) throws IOException {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        String JSON_data = null;

        // Opening Connection
        try {
            URL fetch = new URL(uri.toString());
            connection = (HttpURLConnection) fetch.openConnection();
            connection.setRequestMethod("GET");
            connection.addRequestProperty("X-Auth-Token", "e136b7858d424b9da07c88f28b61989a");
            connection.connect();

            // Read the input stream into a String
            InputStream inputStream = connection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                Log.e(LOG_TAG, "Input stream is null");
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                Log.d(LOG_TAG, "Data received is empty");
                return null;
            }

            JSON_data = buffer.toString();

            Log.d(LOG_TAG, "Fetch Result: \n" + JSON_data);

            return JSON_data;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error fetching data", e);

            throw e;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error Closing Stream");
                }
            }
        }
    }

    private void getSeasons() {
        Uri uri = API_BASE_URI.buildUpon().appendPath(SEASONS).build();
        Log.d(LOG_TAG, "Seasons URI: " + uri.toString());

        try {
            String json = fetchUrl(uri);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error getting data from server.", e);
        }
    }

    private void getData(String pastOrNext, int days) {
        String timeFrame = pastOrNext + days;
        // Creating fetch URL
        final String QUERY_TIME_FRAME = "timeFrame"; // Time Frame parameter to determine days

        Uri uri = API_BASE_URI.buildUpon().appendPath(FIXTURES)
                .appendQueryParameter(QUERY_TIME_FRAME, timeFrame).build();

        Log.d(LOG_TAG, "Fixtures URI: " + uri.toString());


        try {
            String json = fetchUrl(uri);
            if (json != null) {
                // This bit is to check if the data contains any matches. If not, we call processJson on the dummy data
                JSONArray matches = new JSONObject(json).getJSONArray("fixtures");
                if (matches.length() == 0) {
                    Log.d(LOG_TAG, "No fixtures");
                    return;
                }

                processJSONdata(json, getApplicationContext(), true);
            } else {
                //Could not Connect
                Log.e(LOG_TAG, "No data received.");
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error parsing JSON result", e);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error getting data from server.", e);
        }
    }

    private void processJSONdata(String JSONdata, Context mContext, boolean isReal) {
        //JSON data
        final String SERIE_A = "357";
        final String PREMIER_LEGAUE = "354";
        final String CHAMPIONS_LEAGUE = "362";
        final String PRIMERA_DIVISION = "358";
        final String BUNDESLIGA = "351";
        final String SEASON_LINK = "http://api.football-data.org/alpha/soccerseasons/";
        final String MATCH_LINK = "http://api.football-data.org/alpha/fixtures/";
        final String FIXTURES = "fixtures";
        final String LINKS = "_links";
        final String SOCCER_SEASON = "soccerseason";
        final String SELF = "self";
        final String MATCH_DATE = "date";
        final String HOME_TEAM = "homeTeamName";
        final String AWAY_TEAM = "awayTeamName";
        final String RESULT = "result";
        final String HOME_GOALS = "goalsHomeTeam";
        final String AWAY_GOALS = "goalsAwayTeam";
        final String MATCH_DAY = "matchday";

        //Match data
        String league = null;
        String matchDate = null;
        String matchTime = null;
        String home = null;
        String away = null;
        String homeGoals = null;
        String awayGoals = null;
        String matchId = null;
        String matchDay = null;


        try {
            JSONArray matches = new JSONObject(JSONdata).getJSONArray(FIXTURES);

            //ContentValues to be inserted
            Vector<ContentValues> values = new Vector<ContentValues>(matches.length());
            for (int i = 0; i < matches.length(); i++) {
                JSONObject match = matches.getJSONObject(i);

                league = match.getJSONObject(LINKS).getJSONObject(SOCCER_SEASON).getString("href");
                league = league.replace(SEASON_LINK, "");

                if (league.equals(PREMIER_LEGAUE) ||
                        league.equals(SERIE_A) ||
                        league.equals(CHAMPIONS_LEAGUE) ||
                        league.equals(BUNDESLIGA) ||
                        league.equals(PRIMERA_DIVISION)) {
                    matchId = match.getJSONObject(LINKS).getJSONObject(SELF).
                            getString("href");
                    matchId = matchId.replace(MATCH_LINK, "");
                    if (!isReal) {
                        //This if statement changes the match ID of the dummy data so that it all goes into the database
                        matchId = matchId + Integer.toString(i);
                    }

                    matchDate = match.getString(MATCH_DATE);
                    matchTime = matchDate.substring(matchDate.indexOf("T") + 1, matchDate.indexOf("Z"));
                    matchDate = matchDate.substring(0, matchDate.indexOf("T"));
                    SimpleDateFormat match_date = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss");
                    match_date.setTimeZone(TimeZone.getTimeZone("UTC"));
                    try {
                        Date parseddate = match_date.parse(matchDate + matchTime);
                        SimpleDateFormat new_date = new SimpleDateFormat("yyyy-MM-dd:HH:mm");
                        new_date.setTimeZone(TimeZone.getDefault());
                        matchDate = new_date.format(parseddate);
                        matchTime = matchDate.substring(matchDate.indexOf(":") + 1);
                        matchDate = matchDate.substring(0, matchDate.indexOf(":"));

                        if (!isReal) {
                            //This if statement changes the dummy data's date to match our current date range.
                            Date fragmentdate = new Date(System.currentTimeMillis() + ((i - 2) * 86400000));
                            SimpleDateFormat mformat = new SimpleDateFormat("yyyy-MM-dd");
                            matchDate = mformat.format(fragmentdate);
                        }
                    } catch (Exception e) {
                        Log.d(LOG_TAG, "error here!");
                        Log.e(LOG_TAG, e.getMessage());
                    }
                    home = match.getString(HOME_TEAM);
                    away = match.getString(AWAY_TEAM);
                    homeGoals = match.getJSONObject(RESULT).getString(HOME_GOALS);
                    awayGoals = match.getJSONObject(RESULT).getString(AWAY_GOALS);
                    matchDay = match.getString(MATCH_DAY);
                    ContentValues match_values = new ContentValues();
                    match_values.put(DatabaseContract.ScoresTable.MATCH_ID, matchId);
                    match_values.put(DatabaseContract.ScoresTable.DATE_COL, matchDate);
                    match_values.put(DatabaseContract.ScoresTable.TIME_COL, matchTime);
                    match_values.put(DatabaseContract.ScoresTable.HOME_COL, home);
                    match_values.put(DatabaseContract.ScoresTable.AWAY_COL, away);
                    match_values.put(DatabaseContract.ScoresTable.HOME_GOALS_COL, homeGoals);
                    match_values.put(DatabaseContract.ScoresTable.AWAY_GOALS_COL, awayGoals);
                    match_values.put(DatabaseContract.ScoresTable.LEAGUE_COL, league);
                    match_values.put(DatabaseContract.ScoresTable.MATCH_DAY, matchDay);
                    //log spam

                    //Log.v(LOG_TAG,match_id);
                    //Log.v(LOG_TAG,mDate);
                    //Log.v(LOG_TAG,mTime);
                    //Log.v(LOG_TAG,Home);
                    //Log.v(LOG_TAG,Away);
                    //Log.v(LOG_TAG,Home_goals);
                    //Log.v(LOG_TAG,Away_goals);

                    values.add(match_values);
                }
            }
            int inserted_data = 0;
            ContentValues[] insert_data = new ContentValues[values.size()];
            values.toArray(insert_data);
            inserted_data = mContext.getContentResolver().bulkInsert(
                    DatabaseContract.BASE_CONTENT_URI, insert_data);

            //Log.v(LOG_TAG,"Succesfully Inserted : " + String.valueOf(inserted_data));
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage());
        }

    }
}

