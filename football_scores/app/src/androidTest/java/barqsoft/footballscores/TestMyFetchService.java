package barqsoft.footballscores;

import android.content.Context;
import android.content.Intent;
import android.test.AndroidTestCase;

import barqsoft.footballscores.service.MyFetchService;

/**
 * Created by joshua on 8/23/15.
 */
public class TestMyFetchService extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteAllRecords();
    }

    public void deleteAllRecords() {
        mContext.getContentResolver().delete(DatabaseContract.BASE_CONTENT_URI, null, null);
        mContext.getContentResolver().delete(DatabaseContract.SeasonsTable.buildSeasonsPath(), null, null);
    }

    public void testUpdateScores() {
        MyFetchService service = new MyFetchService();
        service.getSeasons(mContext);
        service.getScores(mContext, MyFetchService.PAST, 2);
    }
}
