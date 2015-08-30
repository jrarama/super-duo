package barqsoft.footballscores;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by yehya khaled on 3/3/2015.
 */
public class Utilies
{
    public static final String CHAMPIONS_LEAGUE = "CL";

    public static String getLeagueCaption(String league) {
        if (league == null) return null;

        return league.replaceAll("^\\d\\. |\\d+/\\d+$", "");
    }

    public static String getMatchDay(int match_day, String league)
    {
        if(league.equals(CHAMPIONS_LEAGUE))
        {
            if (match_day <= 6)
            {
                return "Group Stages, Matchday : 6";
            }
            else if(match_day == 7 || match_day == 8)
            {
                return "First Knockout round";
            }
            else if(match_day == 9 || match_day == 10)
            {
                return "QuarterFinal";
            }
            else if(match_day == 11 || match_day == 12)
            {
                return "SemiFinal";
            }
            else
            {
                return "Final";
            }
        }
        else
        {
            return "Matchday : " + String.valueOf(match_day);
        }
    }

    public static String getScores(int home_goals,int awaygoals)
    {
        if(home_goals < 0 && awaygoals < 0)
        {
            return " - ";
        }
        else
        {
            return String.valueOf(home_goals) + " - " + String.valueOf(awaygoals);
        }
    }

    public static int getTeamCrestByTeamName (String teamname)
    {
        if (teamname==null){return R.drawable.no_icon;}
        switch (teamname)
        {
            case "Arsenal London FC" : return R.drawable.arsenal;
            case "Manchester United FC" : return R.drawable.manchester_united;
            case "Swansea City" : return R.drawable.swansea_city_afc;
            case "Leicester City" : return R.drawable.leicester_city_fc_hd_logo;
            case "Everton FC" : return R.drawable.everton_fc_logo1;
            case "West Ham United FC" : return R.drawable.west_ham;
            case "Tottenham Hotspur FC" : return R.drawable.tottenham_hotspur;
            case "West Bromwich Albion" : return R.drawable.west_bromwich_albion_hd_logo;
            case "Sunderland AFC" : return R.drawable.sunderland;
            case "Stoke City FC" : return R.drawable.stoke_city;
            default: return R.drawable.no_icon;
        }
    }

    @Nullable
    public static String fetchUrl(Context context, Uri uri, String logTag) throws IOException {
        return fetchUrl(context, uri, logTag, false);
    }

    @Nullable
    public static String fetchUrl(Context context, Uri uri, String logTag, boolean noToken) throws IOException {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        String JSON_data = null;

        // Opening Connection
        try {
            URL fetch = new URL(uri.toString());
            connection = (HttpURLConnection) fetch.openConnection();
            connection.setRequestMethod("GET");

            if (!noToken) {
                String apiToken = context.getResources().getString(R.string.football_apikey);
                connection.addRequestProperty("X-Auth-Token", apiToken);
            }
            connection.connect();

            // Read the input stream into a String
            InputStream inputStream = connection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                Log.e(logTag, "Input stream is null");
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
                Log.d(logTag, "Data received is empty");
                return null;
            }

            JSON_data = buffer.toString();

            Log.d(logTag, "Fetch Result: \n" + JSON_data);

            return JSON_data;
        } catch (IOException e) {
            Log.e(logTag, "Error fetching data", e);

            throw e;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(logTag, "Error Closing Stream");
                }
            }
        }
    }

}
