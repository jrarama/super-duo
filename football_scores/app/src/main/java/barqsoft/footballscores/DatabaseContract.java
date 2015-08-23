package barqsoft.footballscores;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by yehya khaled on 2/25/2015.
 */
public class DatabaseContract {
    public static final String SCORES_TABLE = "ScoresTable";
    public static final String SEASONS_TABLE = "SeasonsTable";
    public static final String TEAMS_TABLE = "TeamsTable";

    public static final class ScoresTable implements BaseColumns {
        //Table data
        public static final String LEAGUE_COL = "league";
        public static final String DATE_COL = "date";
        public static final String TIME_COL = "time";
        public static final String HOME_COL = "home";
        public static final String AWAY_COL = "away";
        public static final String HOME_GOALS_COL = "home_goals";
        public static final String AWAY_GOALS_COL = "away_goals";
        public static final String MATCH_ID = "match_id";
        public static final String MATCH_DAY = "match_day";

        public static final String PATH = "scores";

        //Types
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH;

        public static Uri buildScoreWithLeague() {
            return BASE_CONTENT_URI.buildUpon().appendPath("league").build();
        }

        public static Uri buildScoreWithId() {
            return BASE_CONTENT_URI.buildUpon().appendPath("id").build();
        }

        public static Uri buildScoreWithDate() {
            return BASE_CONTENT_URI.buildUpon().appendPath("date").build();
        }
    }

    public static final class SeasonsTable implements BaseColumns {
        public static final String SEASON_ID_COLUMN = "season_id";
        public static final String CAPTION_COLUMN = "caption";
        public static final String LEAGUE_COLUMN = "league";

        public static final String PATH = "seasons";

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH;

        public static Uri buildSeasonsPath() {
            return BASE_CONTENT_URI.buildUpon().appendPath(PATH).build();
        }
    }

    public static final class TeamsTable implements BaseColumns {
        public static final String NAME_COLUMN = "name";
        public static final String CREST_URL_COLUMN = "crest_url";

        public static final String PATH = "teams";

        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH;

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH;

        public static Uri buildTeamsPath() {
            return BASE_CONTENT_URI.buildUpon().appendPath(PATH).build();
        }

        public static Uri buildTeamsPathWithId() {
            return BASE_CONTENT_URI.buildUpon().appendPath("team").build();
        }
    }

    //URI data
    public static final String CONTENT_AUTHORITY = "barqsoft.footballscores";

    public static Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
}
