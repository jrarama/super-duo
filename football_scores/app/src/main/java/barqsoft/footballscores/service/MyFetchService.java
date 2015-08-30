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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilies;

/**
 * Created by yehya khaled on 3/2/2015.
 */
public class MyFetchService extends IntentService {
    public static final String LOG_TAG = MyFetchService.class.getSimpleName();

    private static final Uri API_BASE_URI = Uri.parse("http://api.football-data.org/alpha/");

    private final String FIXTURES = "fixtures";
    private final String SOCCER_SEASONS = "soccerseasons";
    private final String TEAMS = "teams";
    final String LINKS = "_links";
    final String HREF = "href";
    final String SELF = "self";
    private final String ID_MATCHER = "(.*/)(\\d+)/?";

    private Map<String, String> leagueMap = new HashMap<>();
    private List<String> wantedLeagues = Arrays.asList(
        "SA", // Serie A
        "PL", // Premiere League
        "CL", // Champions-League
        "PD", // Primera Division
        "BL1" // Bundesliga 1
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

    public void getSeasons(Context context) {
        Uri uri = API_BASE_URI.buildUpon().appendPath(SOCCER_SEASONS).build();
        Log.d(LOG_TAG, "Seasons URI: " + uri.toString());

        try {
            String json = Utilies.fetchUrl(context, uri, LOG_TAG);
            processSeasonsData(json, context);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error getting data from server.", e);
        }
    }

    private String getIdFromUrl(String url) {
        return url.replaceAll(ID_MATCHER, "$2");
    }

    public void getScores(Context context, String pastOrNext, int days) {
        String timeFrame = pastOrNext + days;
        // Creating fetch URL
        final String QUERY_TIME_FRAME = "timeFrame"; // Time Frame parameter to determine days

        Uri uri = API_BASE_URI.buildUpon().appendPath(FIXTURES)
                .appendQueryParameter(QUERY_TIME_FRAME, timeFrame).build();

        Log.d(LOG_TAG, "Fixtures URI: " + uri.toString());


        try {
            String json = Utilies.fetchUrl(context, uri, LOG_TAG);
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

    public void getTeams(Context context, String seasonId) {
        Uri uri = API_BASE_URI.buildUpon().appendPath(SOCCER_SEASONS)
                .appendPath(seasonId).appendPath(TEAMS).build();

        Log.d(LOG_TAG, "TEAMS URI: " + uri.toString());
        String json;
        try {
            json = Utilies.fetchUrl(context, uri, LOG_TAG);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error getting data from server.", e);
            return;
        }

        final String NAME = "name";
        final String CREST_URL = "crestUrl";

        try {
            JSONObject teamsObj = new JSONObject(json);
            JSONArray teams = teamsObj.getJSONArray(TEAMS);

            if (teams == null || teams.length() == 0) {
                Log.d(LOG_TAG, "No teams for Season: " + seasonId);
                return;
            }

            List<ContentValues> values = new ArrayList<>();
            for (int i = 0; i < teams.length(); i++) {
                JSONObject team = teams.getJSONObject(i);

                String id = getIdFromUrl(team.getJSONObject(LINKS).getJSONObject(SELF).getString(HREF));
                String name = team.getString(NAME);
                String crestUrl = team.getString(CREST_URL);

                ContentValues teamValues = new ContentValues();
                teamValues.put(DatabaseContract.TeamsTable._ID, id);
                teamValues.put(DatabaseContract.TeamsTable.NAME_COLUMN, name);
                teamValues.put(DatabaseContract.TeamsTable.CREST_URL_COLUMN, crestUrl);
                Log.d(LOG_TAG, "Team Id: " + id + ", Name: " + name
                        + ", CrestUrl: " + crestUrl);
                values.add(teamValues);
            }

            ContentValues[] contentValues = new ContentValues[values.size()];
            values.toArray(contentValues);
            int insertedData = context.getContentResolver().bulkInsert(
                    DatabaseContract.TeamsTable.buildTeamsPath(), contentValues);

            Log.d(LOG_TAG, "Succesfully Inserted " + insertedData + " data.");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void processSeasonsData(String json, Context context) {
        final String CAPTION = "caption";
        final String LEAGUE = "league";

        try {
            JSONArray seasons = new JSONArray(json);
            ArrayList<ContentValues> values = new ArrayList<>();
            leagueMap.clear();
            for (int i= 0; i< seasons.length(); i ++) {
                JSONObject season = seasons.getJSONObject(i);

                String seasonId = getIdFromUrl(season.getJSONObject(LINKS)
                        .getJSONObject(SELF).getString(HREF));

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

                getTeams(context, seasonId);
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
        final String HOME_TEAM_LINK = "homeTeam";
        final String AWAY_TEAM_LINK = "awayTeam";
        final String HOME_TEAM = "homeTeamName";
        final String AWAY_TEAM = "awayTeamName";
        final String RESULT = "result";
        final String HOME_GOALS = "goalsHomeTeam";
        final String AWAY_GOALS = "goalsAwayTeam";
        final String MATCH_DAY = "matchday";

        try {
            JSONArray matches = new JSONObject(json).getJSONArray(FIXTURES);

            //ContentValues to be inserted
            ArrayList<ContentValues> values = new ArrayList<>();
            for (int i = 0; i < matches.length(); i++) {
                JSONObject match = matches.getJSONObject(i);

                JSONObject links = match.getJSONObject(LINKS);
                String league = getIdFromUrl(links.getJSONObject(SOCCER_SEASON).getString(HREF));

                if (!leagueMap.containsKey(league)) {
                    Log.d(LOG_TAG, "League " + league + " not in wanted list");
                    continue;
                }

                String matchId = getIdFromUrl(links.getJSONObject(SELF).getString(HREF));
                Integer homeTeam = Integer.parseInt(getIdFromUrl(links.getJSONObject(HOME_TEAM_LINK).getString(HREF)));
                Integer awayTeam = Integer.parseInt(getIdFromUrl(links.getJSONObject(AWAY_TEAM_LINK).getString(HREF)));

                String matchDate = match.getString(MATCH_DATE);
                String matchTime = matchDate.substring(matchDate.indexOf("T") + 1, matchDate.indexOf("Z"));
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
                String home = match.getString(HOME_TEAM);
                String away = match.getString(AWAY_TEAM);
                String homeGoals = match.getJSONObject(RESULT).getString(HOME_GOALS);
                String awayGoals = match.getJSONObject(RESULT).getString(AWAY_GOALS);
                String matchDay = match.getString(MATCH_DAY);
                ContentValues matchValues = new ContentValues();
                matchValues.put(DatabaseContract.ScoresTable.MATCH_ID, matchId);
                matchValues.put(DatabaseContract.ScoresTable.DATE_COL, matchDate);
                matchValues.put(DatabaseContract.ScoresTable.TIME_COL, matchTime);
                matchValues.put(DatabaseContract.ScoresTable.HOME_COL, home);
                matchValues.put(DatabaseContract.ScoresTable.AWAY_COL, away);
                matchValues.put(DatabaseContract.ScoresTable.HOME_TEAM_COL, homeTeam);
                matchValues.put(DatabaseContract.ScoresTable.AWAY_TEAM_COL, awayTeam);
                matchValues.put(DatabaseContract.ScoresTable.HOME_GOALS_COL, homeGoals);
                matchValues.put(DatabaseContract.ScoresTable.AWAY_GOALS_COL, awayGoals);
                matchValues.put(DatabaseContract.ScoresTable.LEAGUE_COL, league);
                matchValues.put(DatabaseContract.ScoresTable.MATCH_DAY, matchDay);

                Log.d(LOG_TAG, "MatchID: " + matchId + ", League: " + league
                    + ", Match Day: " + matchDay + ", Date: "+ matchDate + ", Time: " + matchTime
                    + ", Home: " + homeTeam + " " + home + ", Away: " + awayTeam + " "  +away
                    + ", HomeGoals: " + homeGoals + ", Away Goals: " + awayGoals);

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

