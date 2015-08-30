package barqsoft.footballscores;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import barqsoft.footballscores.DatabaseContract.ScoresTable;

import static barqsoft.footballscores.DatabaseContract.*;

/**
 * Created by yehya khaled on 2/25/2015.
 */
public class ScoresDBHelper extends SQLiteOpenHelper
{
    public static final String DATABASE_NAME = "Scores.db";
    private static final int DATABASE_VERSION = 3;
    public ScoresDBHelper(Context context)
    {
        super(context,DATABASE_NAME,null,DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        final String CreateScoresTable = "CREATE TABLE " + SCORES_TABLE + " ("
                + ScoresTable._ID + " INTEGER PRIMARY KEY,"
                + ScoresTable.DATE_COL + " TEXT NOT NULL,"
                + ScoresTable.TIME_COL + " INTEGER NOT NULL,"
                + ScoresTable.HOME_COL + " TEXT NOT NULL,"
                + ScoresTable.AWAY_COL + " TEXT NOT NULL,"
                + ScoresTable.HOME_TEAM_COL + " INTEGER NOT NULL,"
                + ScoresTable.AWAY_TEAM_COL + " INTEGER NOT NULL,"
                + ScoresTable.LEAGUE_COL + " INTEGER NOT NULL,"
                + ScoresTable.HOME_GOALS_COL + " TEXT NOT NULL,"
                + ScoresTable.AWAY_GOALS_COL + " TEXT NOT NULL,"
                + ScoresTable.MATCH_ID + " INTEGER NOT NULL,"
                + ScoresTable.MATCH_DAY + " INTEGER NOT NULL,"
                + " UNIQUE ("+ ScoresTable.MATCH_ID+") ON CONFLICT REPLACE"
                + " );";

        final String CreateSeasonsTable = "CREATE TABLE " + SEASONS_TABLE + " ("
                + SeasonsTable._ID + " INTEGER PRIMARY KEY,"
                + SeasonsTable.SEASON_ID_COLUMN + " INTEGER NOT NULL,"
                + SeasonsTable.CAPTION_COLUMN + " TEXT NOT NULL,"
                + SeasonsTable.LEAGUE_COLUMN + " TEXT NOT NULL,"
                + " UNIQUE ("+ SeasonsTable.LEAGUE_COLUMN+") ON CONFLICT REPLACE"
                + " );";

        final String CreateTeamsTable = "CREATE TABLE " + TEAMS_TABLE + " ("
                + TeamsTable._ID + " INTEGER PRIMARY KEY,"
                + TeamsTable.NAME_COLUMN + " TEXT NOT NULL,"
                + TeamsTable.CREST_URL_COLUMN + " TEXT NOT NULL"
                + " );";

        db.execSQL(CreateScoresTable);
        db.execSQL(CreateSeasonsTable);
        db.execSQL(CreateTeamsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        //Remove old values when upgrading.
        db.execSQL("DROP TABLE IF EXISTS " + SCORES_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + SEASONS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + TEAMS_TABLE);
    }
}
