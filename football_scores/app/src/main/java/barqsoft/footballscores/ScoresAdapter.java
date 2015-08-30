package barqsoft.footballscores;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.bumptech.glide.GenericRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.StreamEncoder;
import com.bumptech.glide.load.resource.file.FileToStreamDecoder;
import com.caverock.androidsvg.SVG;

import java.io.InputStream;

import barqsoft.footballscores.svg.SvgDecoder;
import barqsoft.footballscores.svg.SvgDrawableTranscoder;
import barqsoft.footballscores.svg.SvgSoftwareLayerSetter;

/**
 * Created by yehya khaled on 2/26/2015.
 */
public class ScoresAdapter extends CursorAdapter {
    private static final String LOG_TAG = ScoresAdapter.class.getSimpleName();

    public static final int COL_DATE = 1;
    public static final int COL_MATCHTIME = 2;
    public static final int COL_HOME = 3;
    public static final int COL_AWAY = 4;
    public static final int COL_LEAGUE = 7;
    public static final int COL_HOME_GOALS = 8;
    public static final int COL_AWAY_GOALS = 9;

    public static final int COL_ID = 10;
    public static final int COL_MATCHDAY = 11;
    public static final int COL_LEAGUE_CAPTION = 14;
    public static final int COL_LEAGUE_CODE = 15;

    public static final int COL_HOME_CREST = 18;
    public static final int COL_AWAY_CREST = 21;
    private double detailMatchId = 0;

    private ViewHolder mHolder;
    private GenericRequestBuilder<Uri, InputStream, SVG, PictureDrawable> requestBuilder;

    public double getDetailMatchId() {
        return detailMatchId;
    }

    public void setDetailMatchId(double detailMatchId) {
        this.detailMatchId = detailMatchId;
    }

    private String FOOTBALL_SCORES_HASHTAG = "#Football_Scores";

    public ScoresAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View mItem = LayoutInflater.from(context).inflate(R.layout.scores_list_item, parent, false);
        mHolder = new ViewHolder(mItem);
        mItem.setTag(mHolder);

        requestBuilder = Glide.with(context)
                .using(Glide.buildStreamModelLoader(Uri.class, context), InputStream.class)
                .from(Uri.class)
                .as(SVG.class)
                .transcode(new SvgDrawableTranscoder(), PictureDrawable.class)
                .sourceEncoder(new StreamEncoder())
                .cacheDecoder(new FileToStreamDecoder<SVG>(new SvgDecoder()))
                .decoder(new SvgDecoder())
                .placeholder(R.drawable.no_icon)
                .error(R.drawable.no_icon)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .listener(new SvgSoftwareLayerSetter<Uri>());

        mHolder.home_crest.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        mHolder.away_crest.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        Log.d(LOG_TAG, "new View inflated");
        return mItem;
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        String home = cursor.getString(COL_HOME);
        String away = cursor.getString(COL_AWAY);

        mHolder.home_name.setText(home);
        mHolder.away_name.setText(away);
        mHolder.date.setText(cursor.getString(COL_MATCHTIME));
        mHolder.score.setText(Utilies.getScores(cursor.getInt(COL_HOME_GOALS), cursor.getInt(COL_AWAY_GOALS)));
        mHolder.match_id = cursor.getDouble(COL_ID);

        String homeCrest = cursor.getString(COL_HOME_CREST);
        String awayCrest = cursor.getString(COL_AWAY_CREST);

        Log.d(LOG_TAG, "Home Crest: " + homeCrest);
        Log.d(LOG_TAG, "Away Crest: " + awayCrest);

        if (homeCrest.endsWith(".svg")) {
            requestBuilder.load(Uri.parse(homeCrest)).into(mHolder.home_crest);
        } else {
            mHolder.home_crest.setImageResource(Utilies.getTeamCrestByTeamName(cursor.getString(COL_HOME)));
        }

        if (awayCrest.endsWith(".svg")) {
            requestBuilder.load(Uri.parse(awayCrest)).into(mHolder.away_crest);
        } else {
            mHolder.away_crest.setImageResource(Utilies.getTeamCrestByTeamName(cursor.getString(COL_AWAY)));
        }

        LayoutInflater vi = (LayoutInflater) context.getApplicationContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = vi.inflate(R.layout.detail_fragment, null);
        ViewGroup container = (ViewGroup) view.findViewById(R.id.details_fragment_container);
        if (mHolder.match_id == detailMatchId) {
            //Log.v(FetchScoreTask.LOG_TAG,"will insert extraView");

            container.addView(v, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT
                    , ViewGroup.LayoutParams.MATCH_PARENT));
            TextView match_day = (TextView) v.findViewById(R.id.matchday_textview);
            match_day.setText(Utilies.getMatchDay(cursor.getInt(COL_MATCHDAY),
                    cursor.getString(COL_LEAGUE_CODE)));
            TextView league = (TextView) v.findViewById(R.id.league_textview);

            String caption = Utilies.getLeagueCaption(cursor.getString(COL_LEAGUE_CAPTION));
            league.setText(caption);
            Button share_button = (Button) v.findViewById(R.id.share_button);
            share_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //add Share Action
                    context.startActivity(createShareForecastIntent(mHolder.home_name.getText() + " "
                            + mHolder.score.getText() + " " + mHolder.away_name.getText() + " "));
                }
            });
        } else {
            container.removeAllViews();
        }

    }

    public Intent createShareForecastIntent(String ShareText) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, ShareText + FOOTBALL_SCORES_HASHTAG);
        return shareIntent;
    }

}
