package barqsoft.footballscores.service;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import barqsoft.footballscores.DatabaseContract;

/**
 * Created by yehya khaled on 3/2/2015.
 */
public class MyFetchService extends IntentService {
    public static final String LOG_TAG = MyFetchService.class.getSimpleName();

    private static final Uri API_BASE_URI = Uri.parse("http://api.football-data.org/alpha/");
    private final String FIXTURES = "fixtures";
    private final String SOCCER_SEASONS = "soccerseasons";
    final String LINKS = "_links";
    final String SELF = "self";
    private final String ID_MATCHER = "(.*/)(\\d+)/?";

    private Map<String, String> leagueMap = new HashMap<>();
    private List<String> wantedLeagues = Arrays.asList(
        "SA", // Serie A
        "PL", // Premiere League
        "CL", // Champions-League
        "PD", // Primera Division
        "BL1", // Bundesliga 1
        "BL2" // Bundesliga 2
    );

    public static final String NEXT = "n";
    public static final String PAST = "p";

    public MyFetchService() {
        super("MyFetchService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Context context = getApplicationContext();

        getSeasons(context);
        getScores(context, NEXT, 2);
        getScores(context, PAST, 2);
    }

    @Nullable
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

    public void getSeasons(Context context) {
        Uri uri = API_BASE_URI.buildUpon().appendPath(SOCCER_SEASONS).build();
        Log.d(LOG_TAG, "Seasons URI: " + uri.toString());

        try {
            String json = fetchUrl(uri);
            processSeasonsData(json, context);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error getting data from server.", e);
        }
    }

    public void getScores(Context context, String pastOrNext, int days) {
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

                processScoresdata(json, context);
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

    private void processSeasonsData(String json, Context context) {
        final String HREF = "href";
        final String CAPTION = "caption";
        final String LEAGUE = "league";

        try {
            JSONArray seasons = new JSONArray(json);
            ArrayList<ContentValues> values = new ArrayList<>();
            leagueMap.clear();
            for (int i= 0; i< seasons.length(); i ++) {
                JSONObject season = seasons.getJSONObject(i);

                String seasonId = season.getJSONObject(LINKS)
                        .getJSONObject(SELF).getString(HREF);
                seasonId = seasonId.replaceAll(ID_MATCHER, "$2");
                String caption = season.getString(CAPTION);
                String league = season.getString(LEAGUE);

                if (!wantedLeagues.contains(league)) {
                    Log.d(LOG_TAG, "League " + league + " not in wanted list");
                    continue;
                }

                leagueMap.put(seasonId, league);
                ContentValues seasonValues = new ContentValues();
                seasonValues.put(DatabaseContract.SeasonsTable.SEASON_ID_COLUMN, seasonId);
                seasonValues.put(DatabaseContract.SeasonsTable.CAPTION_COLUMN, caption);
                seasonValues.put(DatabaseContract.SeasonsTable.LEAGUE_COLUMN, league);

                Log.d(LOG_TAG, "Seasons Id: " + seasonId + ", League: " + league
                        + ", Caption: " + caption);
                values.add(seasonValues);
            }

            ContentValues[] contentValues = new ContentValues[values.size()];
            values.toArray(contentValues);
            int insertedData = context.getContentResolver().bulkInsert(
                    DatabaseContract.SeasonsTable.buildSeasonsPath(), contentValues);

            Log.d(LOG_TAG, "Succesfully Inserted " + insertedData + " data.");

        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error parsing JSON data", e);
        }
    }

    private void processScoresdata(String json, Context context) {
        //JSON data

        final String SOCCER_SEASON = "soccerseason";
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
            JSONArray matches = new JSONObject(json).getJSONArray(FIXTURES);

            //ContentValues to be inserted
            ArrayList<ContentValues> values = new ArrayList<>();
            for (int i = 0; i < matches.length(); i++) {
                JSONObject match = matches.getJSONObject(i);

                league = match.getJSONObject(LINKS).getJSONObject(SOCCER_SEASON).getString("href");
                league = league.replaceAll(ID_MATCHER, "$2");

                if (!leagueMap.containsKey(league)) {
                    Log.d(LOG_TAG, "League " + league + " not in wanted list");
                    continue;
                }

                matchId = match.getJSONObject(LINKS).getJSONObject(SELF).getString("href");
                matchId = matchId.replaceAll(ID_MATCHER, "$2");

                matchDate = match.getString(MATCH_DATE);
                matchTime = matchDate.substring(matchDate.indexOf("T") + 1, matchDate.indexOf("Z"));
                matchDate = matchDate.substring(0, matchDate.indexOf("T"));
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss");
                dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                try {
                    Date parsedDate = dateFormat.parse(matchDate + matchTime);
                    SimpleDateFormat newDateFormat = new SimpleDateFormat("yyyy-MM-dd:HH:mm");
                    newDateFormat.setTimeZone(TimeZone.getDefault());
                    matchDate = newDateFormat.format(parsedDate);
                    matchTime = matchDate.substring(matchDate.indexOf(":") + 1);
                    matchDate = matchDate.substring(0, matchDate.indexOf(":"));
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error parsing date", e);
                }
                home = match.getString(HOME_TEAM);
                away = match.getString(AWAY_TEAM);
                homeGoals = match.getJSONObject(RESULT).getString(HOME_GOALS);
                awayGoals = match.getJSONObject(RESULT).getString(AWAY_GOALS);
                matchDay = match.getString(MATCH_DAY);
                ContentValues matchValues = new ContentValues();
                matchValues.put(DatabaseContract.ScoresTable.MATCH_ID, matchId);
                matchValues.put(DatabaseContract.ScoresTable.DATE_COL, matchDate);
                matchValues.put(DatabaseContract.ScoresTable.TIME_COL, matchTime);
                matchValues.put(DatabaseContract.ScoresTable.HOME_COL, home);
                matchValues.put(DatabaseContract.ScoresTable.AWAY_COL, away);
                matchValues.put(DatabaseContract.ScoresTable.HOME_GOALS_COL, homeGoals);
                matchValues.put(DatabaseContract.ScoresTable.AWAY_GOALS_COL, awayGoals);
                matchValues.put(DatabaseContract.ScoresTable.LEAGUE_COL, league);
                matchValues.put(DatabaseContract.ScoresTable.MATCH_DAY, matchDay);

                Log.d(LOG_TAG, "MatchID: " + matchId + ", League: " + league
                    + ", Match Day: " + matchDay + ", Date: "+ matchDate + ", Time: " + matchTime
                    + ", Home: " + home + ", Away: " + away + ", HomeGoals: " + homeGoals
                    + ", Away Goals: " + awayGoals);

                values.add(matchValues);
            }

            ContentValues[] contentValues = new ContentValues[values.size()];
            values.toArray(contentValues);

            int insertedData = context.getContentResolver().bulkInsert(
                    DatabaseContract.BASE_CONTENT_URI, contentValues);

            Log.d(LOG_TAG, "Succesfully Inserted " + insertedData + " data.");
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }
}

