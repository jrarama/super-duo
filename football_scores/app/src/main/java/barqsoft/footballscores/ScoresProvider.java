package barqsoft.footballscores;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

/**
 * Created by yehya khaled on 2/25/2015.
 */
public class ScoresProvider extends ContentProvider {
    private static final String LOG_TAG = ScoresProvider.class.getSimpleName();

    private static ScoresDBHelper mOpenHelper;
    private static final int MATCHES = 100;
    private static final int MATCHES_WITH_LEAGUE = 101;
    private static final int MATCHES_WITH_ID = 102;
    private static final int MATCHES_WITH_DATE = 103;
    private static final int SEASONS = 200;
    private static final int TEAMS = 300;
    private static final int TEAM_WITH_ID = 301;

    private UriMatcher muriMatcher = buildUriMatcher();
    private static final SQLiteQueryBuilder scoreQueryBuilder = new SQLiteQueryBuilder();
    private static final String SCORES_BY_LEAGUE = DatabaseContract.ScoresTable.LEAGUE_COL + " = ?";

    private static final String SCORES_BY_DATE =
            DatabaseContract.ScoresTable.DATE_COL + " LIKE ?";
    private static final String SCORES_BY_ID =
            DatabaseContract.ScoresTable.MATCH_ID + " = ?";
    private static final String TEAM_BY_ID =
            DatabaseContract.TeamsTable._ID + " = ?";

    static {
        scoreQueryBuilder.setTables(
                DatabaseContract.SCORES_TABLE + " INNER JOIN "
                + DatabaseContract.SEASONS_TABLE + " ON "
                + DatabaseContract.SCORES_TABLE
                + "." + DatabaseContract.ScoresTable.LEAGUE_COL
                + " = " + DatabaseContract.SEASONS_TABLE
                + "." + DatabaseContract.SeasonsTable.SEASON_ID_COLUMN
        );
    }

    static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = DatabaseContract.BASE_CONTENT_URI.toString();
        matcher.addURI(authority, null, MATCHES);
        matcher.addURI(authority, "league", MATCHES_WITH_LEAGUE);
        matcher.addURI(authority, "id", MATCHES_WITH_ID);
        matcher.addURI(authority, "date", MATCHES_WITH_DATE);
        matcher.addURI(authority, "seasons", SEASONS);
        matcher.addURI(authority, "teams", TEAMS);
        matcher.addURI(authority, "team", TEAM_WITH_ID);
        return matcher;
    }

    private int matchUri(Uri uri) {
        String link = uri.toString();
        if (link.contentEquals(DatabaseContract.BASE_CONTENT_URI.toString())) {
            return MATCHES;
        } else if (link.contentEquals(DatabaseContract.ScoresTable.buildScoreWithDate().toString())) {
            return MATCHES_WITH_DATE;
        } else if (link.contentEquals(DatabaseContract.ScoresTable.buildScoreWithId().toString())) {
            return MATCHES_WITH_ID;
        } else if (link.contentEquals(DatabaseContract.ScoresTable.buildScoreWithLeague().toString())) {
            return MATCHES_WITH_LEAGUE;
        } else if (link.contentEquals(DatabaseContract.SeasonsTable.buildSeasonsPath().toString())) {
            return SEASONS;
        } else if (link.contentEquals(DatabaseContract.TeamsTable.buildTeamsPath().toString())) {
            return TEAMS;
        } else if (link.contentEquals(DatabaseContract.TeamsTable.buildTeamsPathWithId().toString())) {
            return TEAM_WITH_ID;
        }
        return -1;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new ScoresDBHelper(getContext());
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        final int match = muriMatcher.match(uri);
        switch (match) {
            case MATCHES:
                return DatabaseContract.ScoresTable.CONTENT_TYPE;
            case MATCHES_WITH_LEAGUE:
                return DatabaseContract.ScoresTable.CONTENT_TYPE;
            case MATCHES_WITH_ID:
                return DatabaseContract.ScoresTable.CONTENT_ITEM_TYPE;
            case MATCHES_WITH_DATE:
                return DatabaseContract.ScoresTable.CONTENT_TYPE;
            case SEASONS:
                return DatabaseContract.SeasonsTable.CONTENT_TYPE;
            case TEAMS:
                return DatabaseContract.TeamsTable.CONTENT_TYPE;
            case TEAM_WITH_ID:
                return DatabaseContract.TeamsTable.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri : " + uri);
        }
    }

    private Cursor scoreQuery(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return scoreQueryBuilder.query(
            mOpenHelper.getReadableDatabase(),
            projection, selection, selectionArgs, null, null, sortOrder
        );
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor retCursor;
        Log.d(LOG_TAG, uri.getPathSegments().toString());
        int match = matchUri(uri);

        switch (match) {
            case MATCHES:
                retCursor = scoreQuery(projection, null, null, sortOrder);
                break;
            case MATCHES_WITH_DATE:
                retCursor = scoreQuery(projection, SCORES_BY_DATE, selectionArgs, sortOrder);
                break;
            case MATCHES_WITH_ID:
                retCursor = scoreQuery(projection, SCORES_BY_ID, selectionArgs, sortOrder);
                break;
            case MATCHES_WITH_LEAGUE:
                retCursor = scoreQuery(projection, SCORES_BY_LEAGUE, selectionArgs, sortOrder);
                break;
            case SEASONS:
                retCursor = mOpenHelper.getReadableDatabase().query(
                        DatabaseContract.SEASONS_TABLE,
                        projection, selection, selectionArgs, null, null, sortOrder
                );
                break;
            case TEAMS:
                retCursor = mOpenHelper.getReadableDatabase().query(
                        DatabaseContract.TEAMS_TABLE,
                        projection, selection, selectionArgs, null, null, sortOrder
                );
                break;
            case TEAM_WITH_ID:
                retCursor = mOpenHelper.getReadableDatabase().query(
                        DatabaseContract.TEAMS_TABLE,
                        projection, TEAM_BY_ID, selectionArgs, null, null, sortOrder
                );
                break;
            default:
                throw new UnsupportedOperationException("Unknown Uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int returnCount = 0;
        int match = matchUri(uri);

        Log.d(LOG_TAG, "Bulk Insert type: " + match);
        switch (match) {
            case MATCHES:
            case SEASONS:
            case TEAMS:
                db.beginTransaction();
                try {
                    String table;
                    switch (match) {
                        case MATCHES: table = DatabaseContract.SCORES_TABLE; break;
                        case SEASONS: table = DatabaseContract.SEASONS_TABLE; break;
                        case TEAMS: table = DatabaseContract.TEAMS_TABLE; break;
                        default:
                            throw new UnsupportedOperationException("Unknown Uri: " + uri);
                    }

                    for (ContentValues value : values) {
                        long _id = db.insertWithOnConflict(table, null, value,
                                SQLiteDatabase.CONFLICT_REPLACE);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;

            default:
                return super.bulkInsert(uri, values);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = matchUri(uri);
        int rowsDeleted;
        // this makes delete all rows return the number of rows deleted
        if ( null == selection ) selection = "1";
        switch (match) {
            case MATCHES:
                rowsDeleted = db.delete(DatabaseContract.SCORES_TABLE, selection, selectionArgs);
                break;
            case SEASONS:
                rowsDeleted = db.delete(DatabaseContract.SEASONS_TABLE, selection, selectionArgs);
                break;
            case TEAMS:
                rowsDeleted = db.delete(DatabaseContract.TEAMS_TABLE, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Because a null deletes all rows
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }
}
