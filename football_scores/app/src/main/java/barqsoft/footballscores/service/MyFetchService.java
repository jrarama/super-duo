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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.DatabaseContract.SeasonsTable;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilies;

import static barqsoft.footballscores.DatabaseContract.*;

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
                    //if there is no data, call the function on dummy data
                    //this is expected behavior during the off season.
                    processScoresData(getString(R.string.dummy_data), getApplicationContext(), false);
                    return;
                }

                processScoresData(json, getApplicationContext(), true);
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
                teamValues.put(TeamsTable._ID, id);
                teamValues.put(TeamsTable.NAME_COLUMN, name);
                teamValues.put(TeamsTable.CREST_URL_COLUMN, crestUrl);
                Log.d(LOG_TAG, "Team Id: " + id + ", Name: " + name
                        + ", CrestUrl: " + crestUrl);
                values.add(teamValues);
            }

            ContentValues[] contentValues = new ContentValues[values.size()];
            values.toArray(contentValues);
            int insertedData = context.getContentResolver().bulkInsert(
                    TeamsTable.buildTeamsPath(), contentValues);

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
                seasonValues.put(SeasonsTable.SEASON_ID_COLUMN, seasonId);
                seasonValues.put(SeasonsTable.CAPTION_COLUMN, caption);
                seasonValues.put(SeasonsTable.LEAGUE_COLUMN, league);

                Log.d(LOG_TAG, "Seasons Id: " + seasonId + ", League: " + league
                        + ", Caption: " + caption);
                values.add(seasonValues);

                getTeams(context, seasonId);
            }

            ContentValues[] contentValues = new ContentValues[values.size()];
            values.toArray(contentValues);
            int insertedData = context.getContentResolver().bulkInsert(
                    SeasonsTable.buildSeasonsPath(), contentValues);

            Log.d(LOG_TAG, "Succesfully Inserted " + insertedData + " data.");

        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error parsing JSON data", e);
        }
    }


    private void processScoresData(String json, Context context, boolean isReal) {
        //JSON data
        if (!isReal) {
            insertFakeData(context);
        }

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

                if (isReal && !leagueMap.containsKey(league)) {
                    Log.d(LOG_TAG, "League " + league + " not in wanted list");
                    continue;
                }

                String matchId = getIdFromUrl(links.getJSONObject(SELF).getString(HREF));
                if(!isReal){
                    //This if statement changes the match ID of the dummy data so that it all goes into the database
                    matchId=matchId+Integer.toString(i);
                }

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

                    if(!isReal){
                        //This if statement changes the dummy data's date to match our current date range.
                        Date fragmentdate = new Date(System.currentTimeMillis()+((i-2)*86400000));
                        SimpleDateFormat mformat = new SimpleDateFormat("yyyy-MM-dd");
                        matchDate=mformat.format(fragmentdate);
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error parsing date", e);
                }
                String home = match.getString(HOME_TEAM);
                String away = match.getString(AWAY_TEAM);
                String homeGoals = match.getJSONObject(RESULT).getString(HOME_GOALS);
                String awayGoals = match.getJSONObject(RESULT).getString(AWAY_GOALS);
                String matchDay = match.getString(MATCH_DAY);
                ContentValues matchValues = new ContentValues();
                matchValues.put(ScoresTable.MATCH_ID, matchId);
                matchValues.put(ScoresTable.DATE_COL, matchDate);
                matchValues.put(ScoresTable.TIME_COL, matchTime);
                matchValues.put(ScoresTable.HOME_COL, home);
                matchValues.put(ScoresTable.AWAY_COL, away);
                matchValues.put(ScoresTable.HOME_TEAM_COL, homeTeam);
                matchValues.put(ScoresTable.AWAY_TEAM_COL, awayTeam);
                matchValues.put(ScoresTable.HOME_GOALS_COL, homeGoals);
                matchValues.put(ScoresTable.AWAY_GOALS_COL, awayGoals);
                matchValues.put(ScoresTable.LEAGUE_COL, league);
                matchValues.put(ScoresTable.MATCH_DAY, matchDay);

                Log.d(LOG_TAG, "MatchID: " + matchId + ", League: " + league
                    + ", Match Day: " + matchDay + ", Date: "+ matchDate + ", Time: " + matchTime
                    + ", Home: " + homeTeam + " " + home + ", Away: " + awayTeam + " "  +away
                    + ", HomeGoals: " + homeGoals + ", Away Goals: " + awayGoals);

                values.add(matchValues);
            }

            ContentValues[] contentValues = new ContentValues[values.size()];
            values.toArray(contentValues);

            int insertedData = context.getContentResolver().bulkInsert(
                    BASE_CONTENT_URI, contentValues);

            Log.d(LOG_TAG, "Succesfully Inserted " + insertedData + " data.");
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    private void insertFakeData(Context context) {
        ContentValues seasonValues = new ContentValues();
        seasonValues.put(SeasonsTable.SEASON_ID_COLUMN, 357);
        seasonValues.put(SeasonsTable.CAPTION_COLUMN, "TESTING SEASON");
        seasonValues.put(SeasonsTable.LEAGUE_COLUMN, "TS");

        int insertedData = context.getContentResolver().bulkInsert(
                SeasonsTable.buildSeasonsPath(), new ContentValues[] {seasonValues});

        Log.d(LOG_TAG, "Succesfully Inserted " + insertedData + " fake seasons.");

        ContentValues homeTeam = new ContentValues();
        homeTeam.put(TeamsTable._ID, 263);
        homeTeam.put(TeamsTable.NAME_COLUMN, "Test Home Team");
        homeTeam.put(TeamsTable.CREST_URL_COLUMN, "https://upload.wikimedia.org/wikipedia/en/2/2e/Deportivo_Alaves_logo.svg");

        ContentValues awayTeam = new ContentValues();
        awayTeam.put(TeamsTable._ID, 275);
        awayTeam.put(TeamsTable.NAME_COLUMN, "Test Home Team");
        awayTeam.put(TeamsTable.CREST_URL_COLUMN, "https://upload.wikimedia.org/wikipedia/en/2/20/UD_Las_Palmas_logo.svg");

        insertedData = context.getContentResolver().bulkInsert(
                TeamsTable.buildTeamsPath(), new ContentValues[] {homeTeam, awayTeam});

        Log.d(LOG_TAG, "Succesfully Inserted " + insertedData + " fake teams.");

    }
}

