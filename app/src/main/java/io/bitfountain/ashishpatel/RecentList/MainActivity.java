package io.bitfountain.ashishpatel.RecentList;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;


import com.google.android.gms.common.api.GoogleApiClient;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.net.ssl.SSLHandshakeException;

import io.bitfountain.ashishpatel.RecentList.io.bitfountain.ashishpatel.RecentList.RecentList.SoundCloud;
//import io.bitfountain.apatel.sounddroid.R;
import io.bitfountain.ashishpatel.RecentList.io.bitfountain.ashishpatel.RecentList.RecentList.SoundCloudService;
import io.bitfountain.ashishpatel.RecentList.io.bitfountain.ashishpatel.RecentList.RecentList.Track;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    private static final String TAG = "MainActivity";

    private TracksAdapter mAdapter;
    private List<Track> mTracks;

    private GoogleApiClient mClient;
    private TextView mSelectedTitle;
    private ImageView mSelectedThumbnail;
    private MediaPlayer mMediaPlayer;
    private ImageView mPlayerStateButon;
    private SearchView mSearchView;
    private List<Track> mPreviousTracks;

    public MainActivity() {
        super();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                toggleSongState();
            }
        });
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mPlayerStateButon.setImageResource(R.drawable.ic_play);
            }
        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.player_toolbar);
        mSelectedTitle = (TextView) findViewById(R.id.selected_title);
        mSelectedThumbnail = (ImageView) findViewById(R.id.selected_thumbnail);

        mPlayerStateButon =  (ImageView)findViewById(R.id.player_state);
        mPlayerStateButon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSongState();

            }
        });

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.songslist);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mTracks = new ArrayList<Track>();
        mAdapter = new TracksAdapter(this, mTracks);
        mAdapter.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Track selectedTrack = mTracks.get(position);

                mSelectedTitle.setText(selectedTrack.getTitle());
                Picasso.with(MainActivity.this).load(selectedTrack.getAvatarURL()).into(mSelectedThumbnail);

                if(mMediaPlayer.isPlaying()){
                    mMediaPlayer.stop();
                    mMediaPlayer.reset();
                }

                try {
                    mMediaPlayer.setDataSource(selectedTrack.getStreamURL()+"?client_id="+SoundCloudService.CLIENT_ID);
                    mMediaPlayer.prepareAsync();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        recyclerView.setAdapter(mAdapter);


        SoundCloudService service = SoundCloud.getService();   //This was done for boiler plate and provides performance
        service.getRecentSongs(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()), new Callback<List<Track>>() {
            @Override
            public void success(List<Track> tracks, Response response) {
               updateTracks(tracks);
            }

            @Override
            public void failure(RetrofitError error) {
                try {
                    throw (error.getCause());
                } catch (UnknownHostException e) {
                    System.out.println("No internet connection");
                } catch (SSLHandshakeException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                } catch (Throwable e) {
                    e.printStackTrace();
                }

            }
        });


    }

    private void updateTracks(List<Track> tracks){
        mTracks.clear();
        mTracks.addAll(tracks);
        Log.d(TAG, "Track 1 avatar url is " + mTracks.get(0).getAvatarURL());
        mAdapter.notifyDataSetChanged();

    }

    private void toggleSongState() {
        if(mMediaPlayer.isPlaying()){
            mMediaPlayer.pause();
            mPlayerStateButon.setImageResource(R.drawable.ic_play);

        }else{
            mMediaPlayer.start();
            mPlayerStateButon.setImageResource(R.drawable.ic_pause);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(mMediaPlayer != null){
            if(mMediaPlayer.isPlaying()){
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {

        mSearchView.clearFocus();
        SoundCloud.getService().searchSongs(query, new Callback<List<Track>>() {
            @Override
            public void success(List<Track> tracks, Response response) {
                updateTracks(tracks);
            }

            @Override
            public void failure(RetrofitError error) {

            }
        });
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mSearchView = (SearchView)menu.findItem(R.id.search_view).getActionView();
        mSearchView.setOnQueryTextListener(this);
        MenuItemCompat.setOnActionExpandListener(menu.findItem(R.id.search_view), new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                mPreviousTracks = new ArrayList<Track>(mTracks);
                return false;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                updateTracks(mPreviousTracks);
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.search_view) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public static final class R {
        public static final class anim {
            public static final int abc_fade_in=0x7f050000;
            public static final int abc_fade_out=0x7f050001;
            public static final int abc_grow_fade_in_from_bottom=0x7f050002;
            public static final int abc_popup_enter=0x7f050003;
            public static final int abc_popup_exit=0x7f050004;
            public static final int abc_shrink_fade_out_from_bottom=0x7f050005;
            public static final int abc_slide_in_bottom=0x7f050006;
            public static final int abc_slide_in_top=0x7f050007;
            public static final int abc_slide_out_bottom=0x7f050008;
            public static final int abc_slide_out_top=0x7f050009;
            public static final int fab_in=0x7f05000a;
            public static final int fab_out=0x7f05000b;
            public static final int snackbar_in=0x7f05000c;
            public static final int snackbar_out=0x7f05000d;
        }
        public static final class attr {
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionBarDivider=0x7f01009a;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionBarItemBackground=0x7f01009b;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionBarPopupTheme=0x7f010094;
            /** <p>May be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
    <p>May be one of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>wrap_content</code></td><td>0</td><td></td></tr>
    </table>
             */
            public static final int actionBarSize=0x7f010099;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionBarSplitStyle=0x7f010096;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionBarStyle=0x7f010095;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionBarTabBarStyle=0x7f010090;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionBarTabStyle=0x7f01008f;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionBarTabTextStyle=0x7f010091;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionBarTheme=0x7f010097;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionBarWidgetTheme=0x7f010098;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionButtonStyle=0x7f0100b4;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionDropDownStyle=0x7f0100b0;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionLayout=0x7f01004c;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionMenuTextAppearance=0x7f01009c;
            /** <p>May be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
    <p>May be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
             */
            public static final int actionMenuTextColor=0x7f01009d;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionModeBackground=0x7f0100a0;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionModeCloseButtonStyle=0x7f01009f;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionModeCloseDrawable=0x7f0100a2;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionModeCopyDrawable=0x7f0100a4;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionModeCutDrawable=0x7f0100a3;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionModeFindDrawable=0x7f0100a8;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionModePasteDrawable=0x7f0100a5;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionModePopupWindowStyle=0x7f0100aa;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionModeSelectAllDrawable=0x7f0100a6;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionModeShareDrawable=0x7f0100a7;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionModeSplitBackground=0x7f0100a1;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionModeStyle=0x7f01009e;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionModeWebSearchDrawable=0x7f0100a9;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionOverflowButtonStyle=0x7f010092;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int actionOverflowMenuStyle=0x7f010093;
            /** <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int actionProviderClass=0x7f01004e;
            /** <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int actionViewClass=0x7f01004d;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int activityChooserViewStyle=0x7f0100bc;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int alertDialogButtonGroupStyle=0x7f0100dd;
            /** <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int alertDialogCenterButtons=0x7f0100de;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int alertDialogStyle=0x7f0100dc;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int alertDialogTheme=0x7f0100df;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int autoCompleteTextViewStyle=0x7f0100e4;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int background=0x7f01000c;
            /** <p>May be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
    <p>May be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
             */
            public static final int backgroundSplit=0x7f01000e;
            /** <p>May be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
    <p>May be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
             */
            public static final int backgroundStacked=0x7f01000d;
            /** <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int backgroundTint=0x7f0100fd;
            /** <p>Must be one of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>src_over</code></td><td>3</td><td></td></tr>
    <tr><td><code>src_in</code></td><td>5</td><td></td></tr>
    <tr><td><code>src_atop</code></td><td>9</td><td></td></tr>
    <tr><td><code>multiply</code></td><td>14</td><td></td></tr>
    <tr><td><code>screen</code></td><td>15</td><td></td></tr>
    </table>
             */
            public static final int backgroundTintMode=0x7f0100fe;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int barSize=0x7f01003f;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int behavior_overlapTop=0x7f010058;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int borderWidth=0x7f010044;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int borderlessButtonStyle=0x7f0100b9;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int buttonBarButtonStyle=0x7f0100b6;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int buttonBarNegativeButtonStyle=0x7f0100e2;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int buttonBarNeutralButtonStyle=0x7f0100e3;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int buttonBarPositiveButtonStyle=0x7f0100e1;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int buttonBarStyle=0x7f0100b5;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int buttonPanelSideLayout=0x7f01001f;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int buttonStyle=0x7f0100e5;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int buttonStyleSmall=0x7f0100e6;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int checkboxStyle=0x7f0100e7;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int checkedTextViewStyle=0x7f0100e8;
            /** <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int circleCrop=0x7f01004a;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int closeIcon=0x7f01005d;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int closeItemLayout=0x7f01001c;
            /** <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int collapseContentDescription=0x7f0100f7;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int collapseIcon=0x7f0100f6;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int collapsedTitleTextAppearance=0x7f01002f;
            /** <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int color=0x7f010039;
            /** <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int colorAccent=0x7f0100d6;
            /** <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int colorButtonNormal=0x7f0100da;
            /** <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int colorControlActivated=0x7f0100d8;
            /** <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int colorControlHighlight=0x7f0100d9;
            /** <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int colorControlNormal=0x7f0100d7;
            /** <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int colorPrimary=0x7f0100d4;
            /** <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int colorPrimaryDark=0x7f0100d5;
            /** <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int colorSwitchThumbNormal=0x7f0100db;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int commitIcon=0x7f010062;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int contentInsetEnd=0x7f010017;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int contentInsetLeft=0x7f010018;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int contentInsetRight=0x7f010019;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int contentInsetStart=0x7f010016;
            /** <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int contentScrim=0x7f010030;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int customNavigationLayout=0x7f01000f;
            /** <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int defaultQueryHint=0x7f01005c;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int dialogPreferredPadding=0x7f0100ae;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int dialogTheme=0x7f0100ad;
            /** <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int disableChildrenWhenDisabled=0x7f01006a;
            /** <p>Must be one or more (separated by '|') of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>none</code></td><td>0</td><td></td></tr>
    <tr><td><code>useLogo</code></td><td>0x1</td><td></td></tr>
    <tr><td><code>showHome</code></td><td>0x2</td><td></td></tr>
    <tr><td><code>homeAsUp</code></td><td>0x4</td><td></td></tr>
    <tr><td><code>showTitle</code></td><td>0x8</td><td></td></tr>
    <tr><td><code>showCustom</code></td><td>0x10</td><td></td></tr>
    <tr><td><code>disableHome</code></td><td>0x20</td><td></td></tr>
    </table>
             */
            public static final int displayOptions=0x7f010005;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int divider=0x7f01000b;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int dividerHorizontal=0x7f0100bb;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int dividerPadding=0x7f010047;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int dividerVertical=0x7f0100ba;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int drawableSize=0x7f01003b;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int drawerArrowStyle=0x7f010000;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int dropDownListViewStyle=0x7f0100cc;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int dropdownListPreferredItemHeight=0x7f0100b1;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int editTextBackground=0x7f0100c2;
            /** <p>May be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
    <p>May be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
             */
            public static final int editTextColor=0x7f0100c1;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int editTextStyle=0x7f0100e9;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int elevation=0x7f01001a;
            /** <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int errorEnabled=0x7f010083;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int errorTextAppearance=0x7f010084;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int expandActivityOverflowButtonDrawable=0x7f01001e;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int expandedTitleMargin=0x7f010029;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int expandedTitleMarginBottom=0x7f01002d;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int expandedTitleMarginEnd=0x7f01002c;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int expandedTitleMarginStart=0x7f01002a;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int expandedTitleMarginTop=0x7f01002b;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int expandedTitleTextAppearance=0x7f01002e;
            /** <p>Must be one of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>normal</code></td><td>0</td><td></td></tr>
    <tr><td><code>mini</code></td><td>1</td><td></td></tr>
    </table>
             */
            public static final int fabSize=0x7f010042;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int gapBetweenBars=0x7f01003c;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int goIcon=0x7f01005e;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int headerLayout=0x7f010054;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int height=0x7f010001;
            /** <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int hideOnContentScroll=0x7f010015;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int hintTextAppearance=0x7f010082;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int homeAsUpIndicator=0x7f0100b3;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int homeLayout=0x7f010010;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int icon=0x7f010009;
            /** <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int iconifiedByDefault=0x7f01005a;
            /** <p>Must be a floating point value, such as "<code>1.2</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int imageAspectRatio=0x7f010049;
            /** <p>Must be one of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>none</code></td><td>0</td><td></td></tr>
    <tr><td><code>adjust_width</code></td><td>1</td><td></td></tr>
    <tr><td><code>adjust_height</code></td><td>2</td><td></td></tr>
    </table>
             */
            public static final int imageAspectRatioAdjust=0x7f010048;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int indeterminateProgressStyle=0x7f010012;
            /** <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int initialActivityCount=0x7f01001d;
            /** <p>May be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
    <p>May be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
             */
            public static final int insetForeground=0x7f010057;
            /** <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int isLightTheme=0x7f010002;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int itemBackground=0x7f010053;
            /** <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int itemIconTint=0x7f010051;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int itemPadding=0x7f010014;
            /** <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int itemTextColor=0x7f010052;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int keylines=0x7f010033;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int layout=0x7f010059;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int layout_anchor=0x7f010036;
            /** <p>Must be one or more (separated by '|') of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>top</code></td><td>0x30</td><td></td></tr>
    <tr><td><code>bottom</code></td><td>0x50</td><td></td></tr>
    <tr><td><code>left</code></td><td>0x03</td><td></td></tr>
    <tr><td><code>right</code></td><td>0x05</td><td></td></tr>
    <tr><td><code>center_vertical</code></td><td>0x10</td><td></td></tr>
    <tr><td><code>fill_vertical</code></td><td>0x70</td><td></td></tr>
    <tr><td><code>center_horizontal</code></td><td>0x01</td><td></td></tr>
    <tr><td><code>fill_horizontal</code></td><td>0x07</td><td></td></tr>
    <tr><td><code>center</code></td><td>0x11</td><td></td></tr>
    <tr><td><code>fill</code></td><td>0x77</td><td></td></tr>
    <tr><td><code>clip_vertical</code></td><td>0x80</td><td></td></tr>
    <tr><td><code>clip_horizontal</code></td><td>0x08</td><td></td></tr>
    <tr><td><code>start</code></td><td>0x00800003</td><td></td></tr>
    <tr><td><code>end</code></td><td>0x00800005</td><td></td></tr>
    </table>
             */
            public static final int layout_anchorGravity=0x7f010038;
            /** <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int layout_behavior=0x7f010035;
            /** <p>Must be one of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>none</code></td><td>0</td><td></td></tr>
    <tr><td><code>pin</code></td><td>1</td><td></td></tr>
    <tr><td><code>parallax</code></td><td>2</td><td></td></tr>
    </table>
             */
            public static final int layout_collapseMode=0x7f010027;
            /** <p>Must be a floating point value, such as "<code>1.2</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int layout_collapseParallaxMultiplier=0x7f010028;
            /** <p>Must be an integer value, such as "<code>100</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int layout_keyline=0x7f010037;
            /** <p>Must be one or more (separated by '|') of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>scroll</code></td><td>0x1</td><td></td></tr>
    <tr><td><code>exitUntilCollapsed</code></td><td>0x2</td><td></td></tr>
    <tr><td><code>enterAlways</code></td><td>0x4</td><td></td></tr>
    <tr><td><code>enterAlwaysCollapsed</code></td><td>0x8</td><td></td></tr>
    </table>
             */
            public static final int layout_scrollFlags=0x7f010024;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int layout_scrollInterpolator=0x7f010025;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int listChoiceBackgroundIndicator=0x7f0100d3;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int listDividerAlertDialog=0x7f0100af;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int listItemLayout=0x7f010023;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int listLayout=0x7f010020;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int listPopupWindowStyle=0x7f0100cd;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int listPreferredItemHeight=0x7f0100c7;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int listPreferredItemHeightLarge=0x7f0100c9;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int listPreferredItemHeightSmall=0x7f0100c8;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int listPreferredItemPaddingLeft=0x7f0100ca;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int listPreferredItemPaddingRight=0x7f0100cb;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int logo=0x7f01000a;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int maxActionInlineWidth=0x7f010066;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int maxButtonHeight=0x7f0100f5;
            /** <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int measureWithLargestChild=0x7f010045;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int menu=0x7f010050;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int middleBarArrowSize=0x7f01003e;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int multiChoiceItemLayout=0x7f010021;
            /** <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int navigationContentDescription=0x7f0100f9;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int navigationIcon=0x7f0100f8;
            /** <p>Must be one of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>normal</code></td><td>0</td><td></td></tr>
    <tr><td><code>listMode</code></td><td>1</td><td></td></tr>
    <tr><td><code>tabMode</code></td><td>2</td><td></td></tr>
    </table>
             */
            public static final int navigationMode=0x7f010004;
            /** <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int overlapAnchor=0x7f010055;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int paddingEnd=0x7f0100fb;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int paddingStart=0x7f0100fa;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int panelBackground=0x7f0100d0;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int panelMenuListTheme=0x7f0100d2;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int panelMenuListWidth=0x7f0100d1;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int popupMenuStyle=0x7f0100bf;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int popupPromptView=0x7f010069;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int popupTheme=0x7f01001b;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int popupWindowStyle=0x7f0100c0;
            /** <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int preserveIconSpacing=0x7f01004f;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int pressedTranslationZ=0x7f010043;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int progressBarPadding=0x7f010013;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int progressBarStyle=0x7f010011;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int prompt=0x7f010067;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int queryBackground=0x7f010064;
            /** <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int queryHint=0x7f01005b;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int radioButtonStyle=0x7f0100ea;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int ratingBarStyle=0x7f0100eb;
            /** <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int rippleColor=0x7f010041;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int searchHintIcon=0x7f010060;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int searchIcon=0x7f01005f;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int searchViewStyle=0x7f0100c6;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int selectableItemBackground=0x7f0100b7;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int selectableItemBackgroundBorderless=0x7f0100b8;
            /** <p>Must be one or more (separated by '|') of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>never</code></td><td>0</td><td></td></tr>
    <tr><td><code>ifRoom</code></td><td>1</td><td></td></tr>
    <tr><td><code>always</code></td><td>2</td><td></td></tr>
    <tr><td><code>withText</code></td><td>4</td><td></td></tr>
    <tr><td><code>collapseActionView</code></td><td>8</td><td></td></tr>
    </table>
             */
            public static final int showAsAction=0x7f01004b;
            /** <p>Must be one or more (separated by '|') of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>none</code></td><td>0</td><td></td></tr>
    <tr><td><code>beginning</code></td><td>1</td><td></td></tr>
    <tr><td><code>middle</code></td><td>2</td><td></td></tr>
    <tr><td><code>end</code></td><td>4</td><td></td></tr>
    </table>
             */
            public static final int showDividers=0x7f010046;
            /** <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int showText=0x7f010071;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int singleChoiceItemLayout=0x7f010022;
            /** <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int spinBars=0x7f01003a;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int spinnerDropDownItemStyle=0x7f0100b2;
            /** <p>Must be one of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>dialog</code></td><td>0</td><td></td></tr>
    <tr><td><code>dropdown</code></td><td>1</td><td></td></tr>
    </table>
             */
            public static final int spinnerMode=0x7f010068;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int spinnerStyle=0x7f0100ec;
            /** <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int splitTrack=0x7f010070;
            /** <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int state_above_anchor=0x7f010056;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int statusBarBackground=0x7f010034;
            /** <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int statusBarScrim=0x7f010031;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int submitBackground=0x7f010065;
            /** <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int subtitle=0x7f010006;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int subtitleTextAppearance=0x7f0100ef;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int subtitleTextStyle=0x7f010008;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int suggestionRowLayout=0x7f010063;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int switchMinWidth=0x7f01006e;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int switchPadding=0x7f01006f;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int switchStyle=0x7f0100ed;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int switchTextAppearance=0x7f01006d;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int tabBackground=0x7f010075;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int tabContentStart=0x7f010074;
            /** <p>Must be one of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>fill</code></td><td>0</td><td></td></tr>
    <tr><td><code>center</code></td><td>1</td><td></td></tr>
    </table>
             */
            public static final int tabGravity=0x7f010077;
            /** <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int tabIndicatorColor=0x7f010072;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int tabIndicatorHeight=0x7f010073;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int tabMaxWidth=0x7f010079;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int tabMinWidth=0x7f010078;
            /** <p>Must be one of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>scrollable</code></td><td>0</td><td></td></tr>
    <tr><td><code>fixed</code></td><td>1</td><td></td></tr>
    </table>
             */
            public static final int tabMode=0x7f010076;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int tabPadding=0x7f010081;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int tabPaddingBottom=0x7f010080;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int tabPaddingEnd=0x7f01007f;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int tabPaddingStart=0x7f01007d;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int tabPaddingTop=0x7f01007e;
            /** <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int tabSelectedTextColor=0x7f01007c;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int tabTextAppearance=0x7f01007a;
            /** <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int tabTextColor=0x7f01007b;
            /** <p>May be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
    <p>May be a boolean value, either "<code>true</code>" or "<code>false</code>".
             */
            public static final int textAllCaps=0x7f010026;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int textAppearanceLargePopupMenu=0x7f0100ab;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int textAppearanceListItem=0x7f0100ce;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int textAppearanceListItemSmall=0x7f0100cf;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int textAppearanceSearchResultSubtitle=0x7f0100c4;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int textAppearanceSearchResultTitle=0x7f0100c3;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int textAppearanceSmallPopupMenu=0x7f0100ac;
            /** <p>May be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
    <p>May be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
             */
            public static final int textColorAlertDialogListItem=0x7f0100e0;
            /** <p>May be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
    <p>May be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
             */
            public static final int textColorSearchUrl=0x7f0100c5;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int theme=0x7f0100fc;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int thickness=0x7f010040;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int thumbTextPadding=0x7f01006c;
            /** <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int title=0x7f010003;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int titleMarginBottom=0x7f0100f4;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int titleMarginEnd=0x7f0100f2;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int titleMarginStart=0x7f0100f1;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int titleMarginTop=0x7f0100f3;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int titleMargins=0x7f0100f0;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int titleTextAppearance=0x7f0100ee;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int titleTextStyle=0x7f010007;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int toolbarId=0x7f010032;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int toolbarNavigationButtonStyle=0x7f0100be;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int toolbarStyle=0x7f0100bd;
            /** <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int topBottomBarArrowSize=0x7f01003d;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int track=0x7f01006b;
            /** <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
             */
            public static final int voiceIcon=0x7f010061;
            /** <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int windowActionBar=0x7f010085;
            /** <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int windowActionBarOverlay=0x7f010087;
            /** <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int windowActionModeOverlay=0x7f010088;
            /** <p>May be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>May be a fractional value, which is a floating point number appended with either % or %p, such as "<code>14.5%</code>".
    The % suffix always means a percentage of the base size; the optional %p suffix provides a size relative to
    some parent container.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int windowFixedHeightMajor=0x7f01008c;
            /** <p>May be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>May be a fractional value, which is a floating point number appended with either % or %p, such as "<code>14.5%</code>".
    The % suffix always means a percentage of the base size; the optional %p suffix provides a size relative to
    some parent container.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int windowFixedHeightMinor=0x7f01008a;
            /** <p>May be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>May be a fractional value, which is a floating point number appended with either % or %p, such as "<code>14.5%</code>".
    The % suffix always means a percentage of the base size; the optional %p suffix provides a size relative to
    some parent container.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int windowFixedWidthMajor=0x7f010089;
            /** <p>May be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>May be a fractional value, which is a floating point number appended with either % or %p, such as "<code>14.5%</code>".
    The % suffix always means a percentage of the base size; the optional %p suffix provides a size relative to
    some parent container.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int windowFixedWidthMinor=0x7f01008b;
            /** <p>May be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>May be a fractional value, which is a floating point number appended with either % or %p, such as "<code>14.5%</code>".
    The % suffix always means a percentage of the base size; the optional %p suffix provides a size relative to
    some parent container.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int windowMinWidthMajor=0x7f01008d;
            /** <p>May be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>May be a fractional value, which is a floating point number appended with either % or %p, such as "<code>14.5%</code>".
    The % suffix always means a percentage of the base size; the optional %p suffix provides a size relative to
    some parent container.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int windowMinWidthMinor=0x7f01008e;
            /** <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
             */
            public static final int windowNoTitle=0x7f010086;
        }
        public static final class bool {
            public static final int abc_action_bar_embed_tabs=0x7f090002;
            public static final int abc_action_bar_embed_tabs_pre_jb=0x7f090000;
            public static final int abc_action_bar_expanded_action_views_exclusive=0x7f090003;
            public static final int abc_config_actionMenuItemAllCaps=0x7f090004;
            public static final int abc_config_allowActionMenuItemTextWithIcon=0x7f090001;
            public static final int abc_config_closeDialogWhenTouchOutside=0x7f090005;
            public static final int abc_config_showMenuShortcutsWhenKeyboardPresent=0x7f090006;
        }
        public static final class color {
            public static final int abc_background_cache_hint_selector_material_dark=0x7f0b004b;
            public static final int abc_background_cache_hint_selector_material_light=0x7f0b004c;
            public static final int abc_input_method_navigation_guard=0x7f0b0000;
            public static final int abc_primary_text_disable_only_material_dark=0x7f0b004d;
            public static final int abc_primary_text_disable_only_material_light=0x7f0b004e;
            public static final int abc_primary_text_material_dark=0x7f0b004f;
            public static final int abc_primary_text_material_light=0x7f0b0050;
            public static final int abc_search_url_text=0x7f0b0051;
            public static final int abc_search_url_text_normal=0x7f0b0001;
            public static final int abc_search_url_text_pressed=0x7f0b0002;
            public static final int abc_search_url_text_selected=0x7f0b0003;
            public static final int abc_secondary_text_material_dark=0x7f0b0052;
            public static final int abc_secondary_text_material_light=0x7f0b0053;
            public static final int accent_material_dark=0x7f0b0004;
            public static final int accent_material_light=0x7f0b0005;
            public static final int background_floating_material_dark=0x7f0b0006;
            public static final int background_floating_material_light=0x7f0b0007;
            public static final int background_material_dark=0x7f0b0008;
            public static final int background_material_light=0x7f0b0009;
            public static final int bright_foreground_disabled_material_dark=0x7f0b000a;
            public static final int bright_foreground_disabled_material_light=0x7f0b000b;
            public static final int bright_foreground_inverse_material_dark=0x7f0b000c;
            public static final int bright_foreground_inverse_material_light=0x7f0b000d;
            public static final int bright_foreground_material_dark=0x7f0b000e;
            public static final int bright_foreground_material_light=0x7f0b000f;
            public static final int button_material_dark=0x7f0b0010;
            public static final int button_material_light=0x7f0b0011;
            public static final int colorAccent=0x7f0b0012;
            public static final int colorPrimary=0x7f0b0013;
            public static final int colorPrimaryDark=0x7f0b0014;
            public static final int common_action_bar_splitter=0x7f0b0015;
            public static final int common_signin_btn_dark_text_default=0x7f0b0016;
            public static final int common_signin_btn_dark_text_disabled=0x7f0b0017;
            public static final int common_signin_btn_dark_text_focused=0x7f0b0018;
            public static final int common_signin_btn_dark_text_pressed=0x7f0b0019;
            public static final int common_signin_btn_default_background=0x7f0b001a;
            public static final int common_signin_btn_light_text_default=0x7f0b001b;
            public static final int common_signin_btn_light_text_disabled=0x7f0b001c;
            public static final int common_signin_btn_light_text_focused=0x7f0b001d;
            public static final int common_signin_btn_light_text_pressed=0x7f0b001e;
            public static final int common_signin_btn_text_dark=0x7f0b0054;
            public static final int common_signin_btn_text_light=0x7f0b0055;
            public static final int dim_foreground_disabled_material_dark=0x7f0b001f;
            public static final int dim_foreground_disabled_material_light=0x7f0b0020;
            public static final int dim_foreground_material_dark=0x7f0b0021;
            public static final int dim_foreground_material_light=0x7f0b0022;
            public static final int error_color=0x7f0b0023;
            public static final int fab_stroke_end_inner_color=0x7f0b0024;
            public static final int fab_stroke_end_outer_color=0x7f0b0025;
            public static final int fab_stroke_top_inner_color=0x7f0b0026;
            public static final int fab_stroke_top_outer_color=0x7f0b0027;
            public static final int highlighted_text_material_dark=0x7f0b0028;
            public static final int highlighted_text_material_light=0x7f0b0029;
            public static final int hint_foreground_material_dark=0x7f0b002a;
            public static final int hint_foreground_material_light=0x7f0b002b;
            public static final int link_text_material_dark=0x7f0b002c;
            public static final int link_text_material_light=0x7f0b002d;
            public static final int material_blue_grey_800=0x7f0b002e;
            public static final int material_blue_grey_900=0x7f0b002f;
            public static final int material_blue_grey_950=0x7f0b0030;
            public static final int material_deep_teal_200=0x7f0b0031;
            public static final int material_deep_teal_500=0x7f0b0032;
            public static final int primary_color=0x7f0b0033;
            public static final int primary_dark_material_dark=0x7f0b0034;
            public static final int primary_dark_material_light=0x7f0b0035;
            public static final int primary_material_dark=0x7f0b0036;
            public static final int primary_material_light=0x7f0b0037;
            public static final int primary_text_default_material_dark=0x7f0b0038;
            public static final int primary_text_default_material_light=0x7f0b0039;
            public static final int primary_text_disabled_material_dark=0x7f0b003a;
            public static final int primary_text_disabled_material_light=0x7f0b003b;
            public static final int ripple_material_dark=0x7f0b003c;
            public static final int ripple_material_light=0x7f0b003d;
            public static final int secondary_color=0x7f0b003e;
            public static final int secondary_text_default_material_dark=0x7f0b003f;
            public static final int secondary_text_default_material_light=0x7f0b0040;
            public static final int secondary_text_disabled_material_dark=0x7f0b0041;
            public static final int secondary_text_disabled_material_light=0x7f0b0042;
            public static final int shadow_end_color=0x7f0b0043;
            public static final int shadow_mid_color=0x7f0b0044;
            public static final int shadow_start_color=0x7f0b0045;
            public static final int snackbar_background_color=0x7f0b0046;
            public static final int switch_thumb_disabled_material_dark=0x7f0b0047;
            public static final int switch_thumb_disabled_material_light=0x7f0b0048;
            public static final int switch_thumb_material_dark=0x7f0b0056;
            public static final int switch_thumb_material_light=0x7f0b0057;
            public static final int switch_thumb_normal_material_dark=0x7f0b0049;
            public static final int switch_thumb_normal_material_light=0x7f0b004a;
        }
        public static final class dimen {
            public static final int abc_action_bar_content_inset_material=0x7f07000c;
            public static final int abc_action_bar_default_height_material=0x7f070001;
            public static final int abc_action_bar_default_padding_material=0x7f070002;
            public static final int abc_action_bar_icon_vertical_padding_material=0x7f070019;
            public static final int abc_action_bar_navigation_padding_start_material=0x7f07000d;
            public static final int abc_action_bar_overflow_padding_end_material=0x7f07000e;
            public static final int abc_action_bar_overflow_padding_start_material=0x7f07001a;
            public static final int abc_action_bar_progress_bar_size=0x7f070003;
            public static final int abc_action_bar_stacked_max_height=0x7f07001b;
            public static final int abc_action_bar_stacked_tab_max_width=0x7f07001c;
            public static final int abc_action_bar_subtitle_bottom_margin_material=0x7f07001d;
            public static final int abc_action_bar_subtitle_top_margin_material=0x7f07001e;
            public static final int abc_action_button_min_height_material=0x7f07001f;
            public static final int abc_action_button_min_width_material=0x7f070020;
            public static final int abc_action_button_min_width_overflow_material=0x7f070021;
            public static final int abc_alert_dialog_button_bar_height=0x7f070000;
            public static final int abc_button_inset_horizontal_material=0x7f070022;
            public static final int abc_button_inset_vertical_material=0x7f070023;
            public static final int abc_button_padding_horizontal_material=0x7f070024;
            public static final int abc_button_padding_vertical_material=0x7f070025;
            public static final int abc_config_prefDialogWidth=0x7f070006;
            public static final int abc_control_corner_material=0x7f070026;
            public static final int abc_control_inset_material=0x7f070027;
            public static final int abc_control_padding_material=0x7f070028;
            public static final int abc_dialog_list_padding_vertical_material=0x7f070029;
            public static final int abc_dialog_min_width_major=0x7f07002a;
            public static final int abc_dialog_min_width_minor=0x7f07002b;
            public static final int abc_dialog_padding_material=0x7f07002c;
            public static final int abc_dialog_padding_top_material=0x7f07002d;
            public static final int abc_disabled_alpha_material_dark=0x7f07002e;
            public static final int abc_disabled_alpha_material_light=0x7f07002f;
            public static final int abc_dropdownitem_icon_width=0x7f070030;
            public static final int abc_dropdownitem_text_padding_left=0x7f070031;
            public static final int abc_dropdownitem_text_padding_right=0x7f070032;
            public static final int abc_edit_text_inset_bottom_material=0x7f070033;
            public static final int abc_edit_text_inset_horizontal_material=0x7f070034;
            public static final int abc_edit_text_inset_top_material=0x7f070035;
            public static final int abc_floating_window_z=0x7f070036;
            public static final int abc_list_item_padding_horizontal_material=0x7f070037;
            public static final int abc_panel_menu_list_width=0x7f070038;
            public static final int abc_search_view_preferred_width=0x7f070039;
            public static final int abc_search_view_text_min_width=0x7f070007;
            public static final int abc_switch_padding=0x7f070016;
            public static final int abc_text_size_body_1_material=0x7f07003a;
            public static final int abc_text_size_body_2_material=0x7f07003b;
            public static final int abc_text_size_button_material=0x7f07003c;
            public static final int abc_text_size_caption_material=0x7f07003d;
            public static final int abc_text_size_display_1_material=0x7f07003e;
            public static final int abc_text_size_display_2_material=0x7f07003f;
            public static final int abc_text_size_display_3_material=0x7f070040;
            public static final int abc_text_size_display_4_material=0x7f070041;
            public static final int abc_text_size_headline_material=0x7f070042;
            public static final int abc_text_size_large_material=0x7f070043;
            public static final int abc_text_size_medium_material=0x7f070044;
            public static final int abc_text_size_menu_material=0x7f070045;
            public static final int abc_text_size_small_material=0x7f070046;
            public static final int abc_text_size_subhead_material=0x7f070047;
            public static final int abc_text_size_subtitle_material_toolbar=0x7f070004;
            public static final int abc_text_size_title_material=0x7f070048;
            public static final int abc_text_size_title_material_toolbar=0x7f070005;
            public static final int activity_horizontal_margin=0x7f070018;
            public static final int activity_vertical_margin=0x7f070049;
            public static final int appbar_elevation=0x7f07004a;
            public static final int avatar_padding=0x7f07004b;
            public static final int avatar_size=0x7f07004c;
            public static final int dialog_fixed_height_major=0x7f070008;
            public static final int dialog_fixed_height_minor=0x7f070009;
            public static final int dialog_fixed_width_major=0x7f07000a;
            public static final int dialog_fixed_width_minor=0x7f07000b;
            public static final int disabled_alpha_material_dark=0x7f07004d;
            public static final int disabled_alpha_material_light=0x7f07004e;
            public static final int fab_border_width=0x7f07004f;
            public static final int fab_content_size=0x7f070050;
            public static final int fab_elevation=0x7f070051;
            public static final int fab_margin=0x7f070052;
            public static final int fab_size_mini=0x7f070053;
            public static final int fab_size_normal=0x7f070054;
            public static final int fab_translation_z_pressed=0x7f070055;
            public static final int navigation_elevation=0x7f070056;
            public static final int navigation_icon_padding=0x7f070057;
            public static final int navigation_icon_size=0x7f070058;
            public static final int navigation_max_width=0x7f070059;
            public static final int navigation_padding_bottom=0x7f07005a;
            public static final int navigation_padding_top_default=0x7f070017;
            public static final int navigation_separator_vertical_padding=0x7f07005b;
            public static final int notification_large_icon_height=0x7f07005c;
            public static final int notification_large_icon_width=0x7f07005d;
            public static final int notification_subtext_size=0x7f07005e;
            public static final int player_toolbar=0x7f07005f;
            public static final int row_height=0x7f070060;
            public static final int snackbar_action_inline_max_width=0x7f07000f;
            public static final int snackbar_background_corner_radius=0x7f070010;
            public static final int snackbar_elevation=0x7f070061;
            public static final int snackbar_extra_spacing_horizontal=0x7f070011;
            public static final int snackbar_max_width=0x7f070012;
            public static final int snackbar_min_width=0x7f070013;
            public static final int snackbar_padding_horizontal=0x7f070062;
            public static final int snackbar_padding_vertical=0x7f070063;
            public static final int snackbar_padding_vertical_2lines=0x7f070014;
            public static final int snackbar_text_size=0x7f070064;
            public static final int tab_max_width=0x7f070065;
            public static final int tab_min_width=0x7f070015;
            public static final int vertical_inner_margin=0x7f070066;
            public static final int vertical_large_margin=0x7f070067;
            public static final int vertical_small_margin=0x7f070068;
        }
        public static final class drawable {
            public static final int abc_ab_share_pack_mtrl_alpha=0x7f020000;
            public static final int abc_btn_borderless_material=0x7f020001;
            public static final int abc_btn_check_material=0x7f020002;
            public static final int abc_btn_check_to_on_mtrl_000=0x7f020003;
            public static final int abc_btn_check_to_on_mtrl_015=0x7f020004;
            public static final int abc_btn_default_mtrl_shape=0x7f020005;
            public static final int abc_btn_radio_material=0x7f020006;
            public static final int abc_btn_radio_to_on_mtrl_000=0x7f020007;
            public static final int abc_btn_radio_to_on_mtrl_015=0x7f020008;
            public static final int abc_btn_rating_star_off_mtrl_alpha=0x7f020009;
            public static final int abc_btn_rating_star_on_mtrl_alpha=0x7f02000a;
            public static final int abc_btn_switch_to_on_mtrl_00001=0x7f02000b;
            public static final int abc_btn_switch_to_on_mtrl_00012=0x7f02000c;
            public static final int abc_cab_background_internal_bg=0x7f02000d;
            public static final int abc_cab_background_top_material=0x7f02000e;
            public static final int abc_cab_background_top_mtrl_alpha=0x7f02000f;
            public static final int abc_dialog_material_background_dark=0x7f020010;
            public static final int abc_dialog_material_background_light=0x7f020011;
            public static final int abc_edit_text_material=0x7f020012;
            public static final int abc_ic_ab_back_mtrl_am_alpha=0x7f020013;
            public static final int abc_ic_clear_mtrl_alpha=0x7f020014;
            public static final int abc_ic_commit_search_api_mtrl_alpha=0x7f020015;
            public static final int abc_ic_go_search_api_mtrl_alpha=0x7f020016;
            public static final int abc_ic_menu_copy_mtrl_am_alpha=0x7f020017;
            public static final int abc_ic_menu_cut_mtrl_alpha=0x7f020018;
            public static final int abc_ic_menu_moreoverflow_mtrl_alpha=0x7f020019;
            public static final int abc_ic_menu_paste_mtrl_am_alpha=0x7f02001a;
            public static final int abc_ic_menu_selectall_mtrl_alpha=0x7f02001b;
            public static final int abc_ic_menu_share_mtrl_alpha=0x7f02001c;
            public static final int abc_ic_search_api_mtrl_alpha=0x7f02001d;
            public static final int abc_ic_voice_search_api_mtrl_alpha=0x7f02001e;
            public static final int abc_item_background_holo_dark=0x7f02001f;
            public static final int abc_item_background_holo_light=0x7f020020;
            public static final int abc_list_divider_mtrl_alpha=0x7f020021;
            public static final int abc_list_focused_holo=0x7f020022;
            public static final int abc_list_longpressed_holo=0x7f020023;
            public static final int abc_list_pressed_holo_dark=0x7f020024;
            public static final int abc_list_pressed_holo_light=0x7f020025;
            public static final int abc_list_selector_background_transition_holo_dark=0x7f020026;
            public static final int abc_list_selector_background_transition_holo_light=0x7f020027;
            public static final int abc_list_selector_disabled_holo_dark=0x7f020028;
            public static final int abc_list_selector_disabled_holo_light=0x7f020029;
            public static final int abc_list_selector_holo_dark=0x7f02002a;
            public static final int abc_list_selector_holo_light=0x7f02002b;
            public static final int abc_menu_hardkey_panel_mtrl_mult=0x7f02002c;
            public static final int abc_popup_background_mtrl_mult=0x7f02002d;
            public static final int abc_ratingbar_full_material=0x7f02002e;
            public static final int abc_spinner_mtrl_am_alpha=0x7f02002f;
            public static final int abc_spinner_textfield_background_material=0x7f020030;
            public static final int abc_switch_thumb_material=0x7f020031;
            public static final int abc_switch_track_mtrl_alpha=0x7f020032;
            public static final int abc_tab_indicator_material=0x7f020033;
            public static final int abc_tab_indicator_mtrl_alpha=0x7f020034;
            public static final int abc_text_cursor_mtrl_alpha=0x7f020035;
            public static final int abc_textfield_activated_mtrl_alpha=0x7f020036;
            public static final int abc_textfield_default_mtrl_alpha=0x7f020037;
            public static final int abc_textfield_search_activated_mtrl_alpha=0x7f020038;
            public static final int abc_textfield_search_default_mtrl_alpha=0x7f020039;
            public static final int abc_textfield_search_material=0x7f02003a;
            public static final int common_full_open_on_phone=0x7f02003b;
            public static final int common_ic_googleplayservices=0x7f02003c;
            public static final int fab_background=0x7f02003d;
            public static final int ic_pause=0x7f02003e;
            public static final int ic_play=0x7f02003f;
            public static final int ic_search=0x7f020040;
            public static final int notification_template_icon_bg=0x7f020042;
            public static final int snackbar_background=0x7f020041;
        }
        public static final class id {
            public static final int action0=0x7f0c0070;
            public static final int action_bar=0x7f0c0059;
            public static final int action_bar_activity_content=0x7f0c0000;
            public static final int action_bar_container=0x7f0c0058;
            public static final int action_bar_root=0x7f0c0054;
            public static final int action_bar_spinner=0x7f0c0001;
            public static final int action_bar_subtitle=0x7f0c003d;
            public static final int action_bar_title=0x7f0c003c;
            public static final int action_context_bar=0x7f0c005a;
            public static final int action_divider=0x7f0c0074;
            public static final int action_menu_divider=0x7f0c0002;
            public static final int action_menu_presenter=0x7f0c0003;
            public static final int action_mode_bar=0x7f0c0056;
            public static final int action_mode_bar_stub=0x7f0c0055;
            public static final int action_mode_close_button=0x7f0c003e;
            public static final int activity_chooser_view_content=0x7f0c003f;
            public static final int adjust_height=0x7f0c002b;
            public static final int adjust_width=0x7f0c002c;
            public static final int alertTitle=0x7f0c0049;
            public static final int always=0x7f0c002d;
            public static final int beginning=0x7f0c0029;
            public static final int bottom=0x7f0c001a;
            public static final int buttonPanel=0x7f0c004f;
            public static final int cancel_action=0x7f0c0071;
            public static final int center=0x7f0c001b;
            public static final int center_horizontal=0x7f0c001c;
            public static final int center_vertical=0x7f0c001d;
            public static final int checkbox=0x7f0c0051;
            public static final int chronometer=0x7f0c0077;
            public static final int clip_horizontal=0x7f0c001e;
            public static final int clip_vertical=0x7f0c001f;
            public static final int collapseActionView=0x7f0c002e;
            public static final int contentPanel=0x7f0c004a;
            public static final int custom=0x7f0c004e;
            public static final int customPanel=0x7f0c004d;
            public static final int decor_content_parent=0x7f0c0057;
            public static final int default_activity_button=0x7f0c0042;
            public static final int dialog=0x7f0c0032;
            public static final int disableHome=0x7f0c000d;
            public static final int dropdown=0x7f0c0033;
            public static final int edit_query=0x7f0c005b;
            public static final int end=0x7f0c0020;
            public static final int end_padder=0x7f0c007c;
            public static final int enterAlways=0x7f0c0014;
            public static final int enterAlwaysCollapsed=0x7f0c0015;
            public static final int exitUntilCollapsed=0x7f0c0016;
            public static final int expand_activities_button=0x7f0c0040;
            public static final int expanded_menu=0x7f0c0050;
            public static final int fill=0x7f0c0021;
            public static final int fill_horizontal=0x7f0c0022;
            public static final int fill_vertical=0x7f0c0023;
            public static final int fixed=0x7f0c0034;
            public static final int home=0x7f0c0004;
            public static final int homeAsUp=0x7f0c000e;
            public static final int icon=0x7f0c0044;
            public static final int ifRoom=0x7f0c002f;
            public static final int image=0x7f0c0041;
            public static final int info=0x7f0c007b;
            public static final int left=0x7f0c0024;
            public static final int line1=0x7f0c0075;
            public static final int line3=0x7f0c0079;
            public static final int listMode=0x7f0c000a;
            public static final int list_item=0x7f0c0043;
            public static final int media_actions=0x7f0c0073;
            public static final int middle=0x7f0c002a;
            public static final int mini=0x7f0c0028;
            public static final int multiply=0x7f0c0037;
            public static final int never=0x7f0c0030;
            public static final int none=0x7f0c000f;
            public static final int normal=0x7f0c000b;
            public static final int parallax=0x7f0c0018;
            public static final int parentPanel=0x7f0c0046;
            public static final int pin=0x7f0c0019;
            public static final int player_state=0x7f0c006d;
            public static final int player_toolbar=0x7f0c006a;
            public static final int progress_circular=0x7f0c0005;
            public static final int progress_horizontal=0x7f0c0006;
            public static final int radio=0x7f0c0053;
            public static final int right=0x7f0c0025;
            public static final int screen=0x7f0c0038;
            public static final int scroll=0x7f0c0017;
            public static final int scrollView=0x7f0c004b;
            public static final int scrollable=0x7f0c0035;
            public static final int search_badge=0x7f0c005d;
            public static final int search_bar=0x7f0c005c;
            public static final int search_button=0x7f0c005e;
            public static final int search_close_btn=0x7f0c0063;
            public static final int search_edit_frame=0x7f0c005f;
            public static final int search_go_btn=0x7f0c0065;
            public static final int search_mag_icon=0x7f0c0060;
            public static final int search_plate=0x7f0c0061;
            public static final int search_src_text=0x7f0c0062;
            public static final int search_view=0x7f0c007f;
            public static final int search_voice_btn=0x7f0c0066;
            public static final int select_dialog_listview=0x7f0c0067;
            public static final int selected_thumbnail=0x7f0c006b;
            public static final int selected_title=0x7f0c006c;
            public static final int shortcut=0x7f0c0052;
            public static final int showCustom=0x7f0c0010;
            public static final int showHome=0x7f0c0011;
            public static final int showTitle=0x7f0c0012;
            public static final int snackbar_action=0x7f0c006f;
            public static final int snackbar_text=0x7f0c006e;
            public static final int songslist=0x7f0c0069;
            public static final int split_action_bar=0x7f0c0007;
            public static final int src_atop=0x7f0c0039;
            public static final int src_in=0x7f0c003a;
            public static final int src_over=0x7f0c003b;
            public static final int start=0x7f0c0026;
            public static final int status_bar_latest_event_content=0x7f0c0072;
            public static final int submit_area=0x7f0c0064;
            public static final int tabMode=0x7f0c000c;
            public static final int text=0x7f0c007a;
            public static final int text2=0x7f0c0078;
            public static final int textSpacerNoButtons=0x7f0c004c;
            public static final int time=0x7f0c0076;
            public static final int title=0x7f0c0045;
            public static final int title_template=0x7f0c0048;
            public static final int toolbar=0x7f0c0068;
            public static final int top=0x7f0c0027;
            public static final int topPanel=0x7f0c0047;
            public static final int track_thumbnail=0x7f0c007d;
            public static final int track_title=0x7f0c007e;
            public static final int up=0x7f0c0008;
            public static final int useLogo=0x7f0c0013;
            public static final int view_offset_helper=0x7f0c0009;
            public static final int withText=0x7f0c0031;
            public static final int wrap_content=0x7f0c0036;
        }
        public static final class integer {
            public static final int abc_config_activityDefaultDur=0x7f0a0002;
            public static final int abc_config_activityShortDur=0x7f0a0003;
            public static final int abc_max_action_buttons=0x7f0a0000;
            public static final int cancel_button_image_alpha=0x7f0a0004;
            public static final int google_play_services_version=0x7f0a0005;
            public static final int snackbar_text_max_lines=0x7f0a0001;
            public static final int status_bar_notification_info_maxnum=0x7f0a0006;
        }
        public static final class layout {
            public static final int abc_action_bar_title_item=0x7f040000;
            public static final int abc_action_bar_up_container=0x7f040001;
            public static final int abc_action_bar_view_list_nav_layout=0x7f040002;
            public static final int abc_action_menu_item_layout=0x7f040003;
            public static final int abc_action_menu_layout=0x7f040004;
            public static final int abc_action_mode_bar=0x7f040005;
            public static final int abc_action_mode_close_item_material=0x7f040006;
            public static final int abc_activity_chooser_view=0x7f040007;
            public static final int abc_activity_chooser_view_list_item=0x7f040008;
            public static final int abc_alert_dialog_material=0x7f040009;
            public static final int abc_dialog_title_material=0x7f04000a;
            public static final int abc_expanded_menu_layout=0x7f04000b;
            public static final int abc_list_menu_item_checkbox=0x7f04000c;
            public static final int abc_list_menu_item_icon=0x7f04000d;
            public static final int abc_list_menu_item_layout=0x7f04000e;
            public static final int abc_list_menu_item_radio=0x7f04000f;
            public static final int abc_popup_menu_item_layout=0x7f040010;
            public static final int abc_screen_content_include=0x7f040011;
            public static final int abc_screen_simple=0x7f040012;
            public static final int abc_screen_simple_overlay_action_mode=0x7f040013;
            public static final int abc_screen_toolbar=0x7f040014;
            public static final int abc_search_dropdown_item_icons_2line=0x7f040015;
            public static final int abc_search_view=0x7f040016;
            public static final int abc_select_dialog_material=0x7f040017;
            public static final int abc_simple_dropdown_hint=0x7f040018;
            public static final int activity_main=0x7f040019;
            public static final int content_main=0x7f04001a;
            public static final int design_navigation_item=0x7f04001b;
            public static final int design_navigation_item_header=0x7f04001c;
            public static final int design_navigation_item_separator=0x7f04001d;
            public static final int design_navigation_item_subheader=0x7f04001e;
            public static final int design_navigation_menu=0x7f04001f;
            public static final int layout_snackbar=0x7f040020;
            public static final int layout_snackbar_include=0x7f040021;
            public static final int layout_tab_icon=0x7f040022;
            public static final int layout_tab_text=0x7f040023;
            public static final int notification_media_action=0x7f040024;
            public static final int notification_media_cancel_action=0x7f040025;
            public static final int notification_template_big_media=0x7f040026;
            public static final int notification_template_big_media_narrow=0x7f040027;
            public static final int notification_template_lines=0x7f040028;
            public static final int notification_template_media=0x7f040029;
            public static final int notification_template_part_chronometer=0x7f04002a;
            public static final int notification_template_part_time=0x7f04002b;
            public static final int select_dialog_item_material=0x7f04002c;
            public static final int select_dialog_multichoice_material=0x7f04002d;
            public static final int select_dialog_singlechoice_material=0x7f04002e;
            public static final int support_simple_spinner_dropdown_item=0x7f04002f;
            public static final int track_row=0x7f040030;
        }
        public static final class menu {
            public static final int menu_main=0x7f0d0000;
        }
        public static final class mipmap {
            public static final int ic_launcher=0x7f030000;
            public static final int ic_play=0x7f030001;
        }
        public static final class string {
            public static final int abc_action_bar_home_description=0x7f060000;
            public static final int abc_action_bar_home_description_format=0x7f06002b;
            public static final int abc_action_bar_home_subtitle_description_format=0x7f06002c;
            public static final int abc_action_bar_up_description=0x7f060001;
            public static final int abc_action_menu_overflow_description=0x7f060002;
            public static final int abc_action_mode_done=0x7f060003;
            public static final int abc_activity_chooser_view_see_all=0x7f060004;
            public static final int abc_activitychooserview_choose_application=0x7f060005;
            public static final int abc_search_hint=0x7f06002d;
            public static final int abc_searchview_description_clear=0x7f060006;
            public static final int abc_searchview_description_query=0x7f060007;
            public static final int abc_searchview_description_search=0x7f060008;
            public static final int abc_searchview_description_submit=0x7f060009;
            public static final int abc_searchview_description_voice=0x7f06000a;
            public static final int abc_shareactionprovider_share_with=0x7f06000b;
            public static final int abc_shareactionprovider_share_with_application=0x7f06000c;
            public static final int abc_toolbar_collapse_description=0x7f06002e;
            public static final int action_settings=0x7f06002f;
            public static final int app_name=0x7f060030;
            public static final int appbar_scrolling_view_behavior=0x7f060031;
            public static final int auth_google_play_services_client_facebook_display_name=0x7f060032;
            public static final int auth_google_play_services_client_google_display_name=0x7f060033;
            public static final int common_android_wear_notification_needs_update_text=0x7f06000d;
            public static final int common_android_wear_update_text=0x7f06000e;
            public static final int common_android_wear_update_title=0x7f06000f;
            public static final int common_google_play_services_api_unavailable_text=0x7f060010;
            public static final int common_google_play_services_enable_button=0x7f060011;
            public static final int common_google_play_services_enable_text=0x7f060012;
            public static final int common_google_play_services_enable_title=0x7f060013;
            public static final int common_google_play_services_error_notification_requested_by_msg=0x7f060014;
            public static final int common_google_play_services_install_button=0x7f060015;
            public static final int common_google_play_services_install_text_phone=0x7f060016;
            public static final int common_google_play_services_install_text_tablet=0x7f060017;
            public static final int common_google_play_services_install_title=0x7f060018;
            public static final int common_google_play_services_invalid_account_text=0x7f060019;
            public static final int common_google_play_services_invalid_account_title=0x7f06001a;
            public static final int common_google_play_services_needs_enabling_title=0x7f06001b;
            public static final int common_google_play_services_network_error_text=0x7f06001c;
            public static final int common_google_play_services_network_error_title=0x7f06001d;
            public static final int common_google_play_services_notification_needs_update_title=0x7f06001e;
            public static final int common_google_play_services_notification_ticker=0x7f06001f;
            public static final int common_google_play_services_sign_in_failed_text=0x7f060020;
            public static final int common_google_play_services_sign_in_failed_title=0x7f060021;
            public static final int common_google_play_services_unknown_issue=0x7f060022;
            public static final int common_google_play_services_unsupported_text=0x7f060023;
            public static final int common_google_play_services_unsupported_title=0x7f060024;
            public static final int common_google_play_services_update_button=0x7f060025;
            public static final int common_google_play_services_update_text=0x7f060026;
            public static final int common_google_play_services_update_title=0x7f060027;
            public static final int common_google_play_services_updating_text=0x7f060028;
            public static final int common_google_play_services_updating_title=0x7f060029;
            public static final int common_open_on_phone=0x7f06002a;
            public static final int search=0x7f060034;
            public static final int status_bar_notification_info_overflow=0x7f060035;
        }
        public static final class style {
            public static final int AlertDialog_AppCompat=0x7f080075;
            public static final int AlertDialog_AppCompat_Light=0x7f080076;
            public static final int Animation_AppCompat_Dialog=0x7f080077;
            public static final int Animation_AppCompat_DropDownUp=0x7f080078;
            public static final int AppTheme=0x7f080079;
            public static final int AppTheme_AppBarOverlay=0x7f08007a;
            public static final int AppTheme_NoActionBar=0x7f08002f;
            public static final int AppTheme_PopupOverlay=0x7f08007b;
            public static final int Base_AlertDialog_AppCompat=0x7f08007c;
            public static final int Base_AlertDialog_AppCompat_Light=0x7f08007d;
            public static final int Base_Animation_AppCompat_Dialog=0x7f08007e;
            public static final int Base_Animation_AppCompat_DropDownUp=0x7f08007f;
            public static final int Base_DialogWindowTitle_AppCompat=0x7f080080;
            public static final int Base_DialogWindowTitleBackground_AppCompat=0x7f080081;
            public static final int Base_TextAppearance_AppCompat=0x7f080030;
            public static final int Base_TextAppearance_AppCompat_Body1=0x7f080031;
            public static final int Base_TextAppearance_AppCompat_Body2=0x7f080032;
            public static final int Base_TextAppearance_AppCompat_Button=0x7f080019;
            public static final int Base_TextAppearance_AppCompat_Caption=0x7f080033;
            public static final int Base_TextAppearance_AppCompat_Display1=0x7f080034;
            public static final int Base_TextAppearance_AppCompat_Display2=0x7f080035;
            public static final int Base_TextAppearance_AppCompat_Display3=0x7f080036;
            public static final int Base_TextAppearance_AppCompat_Display4=0x7f080037;
            public static final int Base_TextAppearance_AppCompat_Headline=0x7f080038;
            public static final int Base_TextAppearance_AppCompat_Inverse=0x7f080004;
            public static final int Base_TextAppearance_AppCompat_Large=0x7f080039;
            public static final int Base_TextAppearance_AppCompat_Large_Inverse=0x7f080005;
            public static final int Base_TextAppearance_AppCompat_Light_Widget_PopupMenu_Large=0x7f08003a;
            public static final int Base_TextAppearance_AppCompat_Light_Widget_PopupMenu_Small=0x7f08003b;
            public static final int Base_TextAppearance_AppCompat_Medium=0x7f08003c;
            public static final int Base_TextAppearance_AppCompat_Medium_Inverse=0x7f080006;
            public static final int Base_TextAppearance_AppCompat_Menu=0x7f08003d;
            public static final int Base_TextAppearance_AppCompat_SearchResult=0x7f080082;
            public static final int Base_TextAppearance_AppCompat_SearchResult_Subtitle=0x7f08003e;
            public static final int Base_TextAppearance_AppCompat_SearchResult_Title=0x7f08003f;
            public static final int Base_TextAppearance_AppCompat_Small=0x7f080040;
            public static final int Base_TextAppearance_AppCompat_Small_Inverse=0x7f080007;
            public static final int Base_TextAppearance_AppCompat_Subhead=0x7f080041;
            public static final int Base_TextAppearance_AppCompat_Subhead_Inverse=0x7f080008;
            public static final int Base_TextAppearance_AppCompat_Title=0x7f080042;
            public static final int Base_TextAppearance_AppCompat_Title_Inverse=0x7f080009;
            public static final int Base_TextAppearance_AppCompat_Widget_ActionBar_Menu=0x7f080043;
            public static final int Base_TextAppearance_AppCompat_Widget_ActionBar_Subtitle=0x7f080044;
            public static final int Base_TextAppearance_AppCompat_Widget_ActionBar_Subtitle_Inverse=0x7f080045;
            public static final int Base_TextAppearance_AppCompat_Widget_ActionBar_Title=0x7f080046;
            public static final int Base_TextAppearance_AppCompat_Widget_ActionBar_Title_Inverse=0x7f080047;
            public static final int Base_TextAppearance_AppCompat_Widget_ActionMode_Subtitle=0x7f080048;
            public static final int Base_TextAppearance_AppCompat_Widget_ActionMode_Title=0x7f080049;
            public static final int Base_TextAppearance_AppCompat_Widget_DropDownItem=0x7f080083;
            public static final int Base_TextAppearance_AppCompat_Widget_PopupMenu_Large=0x7f08004a;
            public static final int Base_TextAppearance_AppCompat_Widget_PopupMenu_Small=0x7f08004b;
            public static final int Base_TextAppearance_AppCompat_Widget_Switch=0x7f08004c;
            public static final int Base_TextAppearance_AppCompat_Widget_TextView_SpinnerItem=0x7f08004d;
            public static final int Base_TextAppearance_Widget_AppCompat_ExpandedMenu_Item=0x7f080084;
            public static final int Base_TextAppearance_Widget_AppCompat_Toolbar_Subtitle=0x7f08004e;
            public static final int Base_TextAppearance_Widget_AppCompat_Toolbar_Title=0x7f08004f;
            public static final int Base_Theme_AppCompat=0x7f080050;
            public static final int Base_Theme_AppCompat_CompactMenu=0x7f080085;
            public static final int Base_Theme_AppCompat_Dialog=0x7f08000a;
            public static final int Base_Theme_AppCompat_Dialog_Alert=0x7f080086;
            public static final int Base_Theme_AppCompat_Dialog_FixedSize=0x7f080087;
            public static final int Base_Theme_AppCompat_Dialog_MinWidth=0x7f080088;
            public static final int Base_Theme_AppCompat_DialogWhenLarge=0x7f080002;
            public static final int Base_Theme_AppCompat_Light=0x7f080051;
            public static final int Base_Theme_AppCompat_Light_DarkActionBar=0x7f080089;
            public static final int Base_Theme_AppCompat_Light_Dialog=0x7f08000b;
            public static final int Base_Theme_AppCompat_Light_Dialog_Alert=0x7f08008a;
            public static final int Base_Theme_AppCompat_Light_Dialog_FixedSize=0x7f08008b;
            public static final int Base_Theme_AppCompat_Light_Dialog_MinWidth=0x7f08008c;
            public static final int Base_Theme_AppCompat_Light_DialogWhenLarge=0x7f080003;
            public static final int Base_ThemeOverlay_AppCompat=0x7f08008d;
            public static final int Base_ThemeOverlay_AppCompat_ActionBar=0x7f08008e;
            public static final int Base_ThemeOverlay_AppCompat_Dark=0x7f08008f;
            public static final int Base_ThemeOverlay_AppCompat_Dark_ActionBar=0x7f080090;
            public static final int Base_ThemeOverlay_AppCompat_Light=0x7f080091;
            public static final int Base_V11_Theme_AppCompat_Dialog=0x7f08000c;
            public static final int Base_V11_Theme_AppCompat_Light_Dialog=0x7f08000d;
            public static final int Base_V12_Widget_AppCompat_AutoCompleteTextView=0x7f080015;
            public static final int Base_V12_Widget_AppCompat_EditText=0x7f080016;
            public static final int Base_V21_Theme_AppCompat=0x7f080052;
            public static final int Base_V21_Theme_AppCompat_Dialog=0x7f080053;
            public static final int Base_V21_Theme_AppCompat_Light=0x7f080054;
            public static final int Base_V21_Theme_AppCompat_Light_Dialog=0x7f080055;
            public static final int Base_V7_Theme_AppCompat=0x7f080092;
            public static final int Base_V7_Theme_AppCompat_Dialog=0x7f080093;
            public static final int Base_V7_Theme_AppCompat_Light=0x7f080094;
            public static final int Base_V7_Theme_AppCompat_Light_Dialog=0x7f080095;
            public static final int Base_V7_Widget_AppCompat_AutoCompleteTextView=0x7f080096;
            public static final int Base_V7_Widget_AppCompat_EditText=0x7f080097;
            public static final int Base_Widget_AppCompat_ActionBar=0x7f080098;
            public static final int Base_Widget_AppCompat_ActionBar_Solid=0x7f080099;
            public static final int Base_Widget_AppCompat_ActionBar_TabBar=0x7f08009a;
            public static final int Base_Widget_AppCompat_ActionBar_TabText=0x7f080056;
            public static final int Base_Widget_AppCompat_ActionBar_TabView=0x7f080057;
            public static final int Base_Widget_AppCompat_ActionButton=0x7f080058;
            public static final int Base_Widget_AppCompat_ActionButton_CloseMode=0x7f080059;
            public static final int Base_Widget_AppCompat_ActionButton_Overflow=0x7f08005a;
            public static final int Base_Widget_AppCompat_ActionMode=0x7f08009b;
            public static final int Base_Widget_AppCompat_ActivityChooserView=0x7f08009c;
            public static final int Base_Widget_AppCompat_AutoCompleteTextView=0x7f080017;
            public static final int Base_Widget_AppCompat_Button=0x7f08005b;
            public static final int Base_Widget_AppCompat_Button_Borderless=0x7f08005c;
            public static final int Base_Widget_AppCompat_Button_Borderless_Colored=0x7f08005d;
            public static final int Base_Widget_AppCompat_Button_ButtonBar_AlertDialog=0x7f08009d;
            public static final int Base_Widget_AppCompat_Button_Small=0x7f08005e;
            public static final int Base_Widget_AppCompat_ButtonBar=0x7f08005f;
            public static final int Base_Widget_AppCompat_ButtonBar_AlertDialog=0x7f08009e;
            public static final int Base_Widget_AppCompat_CompoundButton_CheckBox=0x7f080060;
            public static final int Base_Widget_AppCompat_CompoundButton_RadioButton=0x7f080061;
            public static final int Base_Widget_AppCompat_CompoundButton_Switch=0x7f08009f;
            public static final int Base_Widget_AppCompat_DrawerArrowToggle=0x7f080000;
            public static final int Base_Widget_AppCompat_DrawerArrowToggle_Common=0x7f0800a0;
            public static final int Base_Widget_AppCompat_DropDownItem_Spinner=0x7f080062;
            public static final int Base_Widget_AppCompat_EditText=0x7f080018;
            public static final int Base_Widget_AppCompat_Light_ActionBar=0x7f0800a1;
            public static final int Base_Widget_AppCompat_Light_ActionBar_Solid=0x7f0800a2;
            public static final int Base_Widget_AppCompat_Light_ActionBar_TabBar=0x7f0800a3;
            public static final int Base_Widget_AppCompat_Light_ActionBar_TabText=0x7f080063;
            public static final int Base_Widget_AppCompat_Light_ActionBar_TabText_Inverse=0x7f080064;
            public static final int Base_Widget_AppCompat_Light_ActionBar_TabView=0x7f080065;
            public static final int Base_Widget_AppCompat_Light_PopupMenu=0x7f080066;
            public static final int Base_Widget_AppCompat_Light_PopupMenu_Overflow=0x7f080067;
            public static final int Base_Widget_AppCompat_ListPopupWindow=0x7f080068;
            public static final int Base_Widget_AppCompat_ListView=0x7f080069;
            public static final int Base_Widget_AppCompat_ListView_DropDown=0x7f08006a;
            public static final int Base_Widget_AppCompat_ListView_Menu=0x7f08006b;
            public static final int Base_Widget_AppCompat_PopupMenu=0x7f08006c;
            public static final int Base_Widget_AppCompat_PopupMenu_Overflow=0x7f08006d;
            public static final int Base_Widget_AppCompat_PopupWindow=0x7f0800a4;
            public static final int Base_Widget_AppCompat_ProgressBar=0x7f08000e;
            public static final int Base_Widget_AppCompat_ProgressBar_Horizontal=0x7f08000f;
            public static final int Base_Widget_AppCompat_RatingBar=0x7f08006e;
            public static final int Base_Widget_AppCompat_SearchView=0x7f0800a5;
            public static final int Base_Widget_AppCompat_SearchView_ActionBar=0x7f0800a6;
            public static final int Base_Widget_AppCompat_Spinner=0x7f080010;
            public static final int Base_Widget_AppCompat_Spinner_DropDown_ActionBar=0x7f08006f;
            public static final int Base_Widget_AppCompat_Spinner_Underlined=0x7f080070;
            public static final int Base_Widget_AppCompat_TextView_SpinnerItem=0x7f080071;
            public static final int Base_Widget_AppCompat_Toolbar=0x7f0800a7;
            public static final int Base_Widget_AppCompat_Toolbar_Button_Navigation=0x7f080072;
            public static final int Base_Widget_Design_TabLayout=0x7f0800a8;
            public static final int Platform_AppCompat=0x7f080011;
            public static final int Platform_AppCompat_Light=0x7f080012;
            public static final int Platform_ThemeOverlay_AppCompat_Dark=0x7f080073;
            public static final int Platform_ThemeOverlay_AppCompat_Light=0x7f080074;
            public static final int Platform_V11_AppCompat=0x7f080013;
            public static final int Platform_V11_AppCompat_Light=0x7f080014;
            public static final int Platform_V14_AppCompat=0x7f08001a;
            public static final int Platform_V14_AppCompat_Light=0x7f08001b;
            public static final int RtlOverlay_DialogWindowTitle_AppCompat=0x7f080021;
            public static final int RtlOverlay_Widget_AppCompat_ActionBar_TitleItem=0x7f080022;
            public static final int RtlOverlay_Widget_AppCompat_ActionButton_Overflow=0x7f080023;
            public static final int RtlOverlay_Widget_AppCompat_DialogTitle_Icon=0x7f080024;
            public static final int RtlOverlay_Widget_AppCompat_PopupMenuItem=0x7f080025;
            public static final int RtlOverlay_Widget_AppCompat_PopupMenuItem_InternalGroup=0x7f080026;
            public static final int RtlOverlay_Widget_AppCompat_PopupMenuItem_Text=0x7f080027;
            public static final int RtlOverlay_Widget_AppCompat_Search_DropDown=0x7f080028;
            public static final int RtlOverlay_Widget_AppCompat_Search_DropDown_Icon1=0x7f080029;
            public static final int RtlOverlay_Widget_AppCompat_Search_DropDown_Icon2=0x7f08002a;
            public static final int RtlOverlay_Widget_AppCompat_Search_DropDown_Query=0x7f08002b;
            public static final int RtlOverlay_Widget_AppCompat_Search_DropDown_Text=0x7f08002c;
            public static final int RtlOverlay_Widget_AppCompat_SearchView_MagIcon=0x7f08002d;
            public static final int RtlOverlay_Widget_AppCompat_Toolbar_Button_Navigation=0x7f08002e;
            public static final int TextAppearance_AppCompat=0x7f0800a9;
            public static final int TextAppearance_AppCompat_Body1=0x7f0800aa;
            public static final int TextAppearance_AppCompat_Body2=0x7f0800ab;
            public static final int TextAppearance_AppCompat_Button=0x7f0800ac;
            public static final int TextAppearance_AppCompat_Caption=0x7f0800ad;
            public static final int TextAppearance_AppCompat_Display1=0x7f0800ae;
            public static final int TextAppearance_AppCompat_Display2=0x7f0800af;
            public static final int TextAppearance_AppCompat_Display3=0x7f0800b0;
            public static final int TextAppearance_AppCompat_Display4=0x7f0800b1;
            public static final int TextAppearance_AppCompat_Headline=0x7f0800b2;
            public static final int TextAppearance_AppCompat_Inverse=0x7f0800b3;
            public static final int TextAppearance_AppCompat_Large=0x7f0800b4;
            public static final int TextAppearance_AppCompat_Large_Inverse=0x7f0800b5;
            public static final int TextAppearance_AppCompat_Light_SearchResult_Subtitle=0x7f0800b6;
            public static final int TextAppearance_AppCompat_Light_SearchResult_Title=0x7f0800b7;
            public static final int TextAppearance_AppCompat_Light_Widget_PopupMenu_Large=0x7f0800b8;
            public static final int TextAppearance_AppCompat_Light_Widget_PopupMenu_Small=0x7f0800b9;
            public static final int TextAppearance_AppCompat_Medium=0x7f0800ba;
            public static final int TextAppearance_AppCompat_Medium_Inverse=0x7f0800bb;
            public static final int TextAppearance_AppCompat_Menu=0x7f0800bc;
            public static final int TextAppearance_AppCompat_SearchResult_Subtitle=0x7f0800bd;
            public static final int TextAppearance_AppCompat_SearchResult_Title=0x7f0800be;
            public static final int TextAppearance_AppCompat_Small=0x7f0800bf;
            public static final int TextAppearance_AppCompat_Small_Inverse=0x7f0800c0;
            public static final int TextAppearance_AppCompat_Subhead=0x7f0800c1;
            public static final int TextAppearance_AppCompat_Subhead_Inverse=0x7f0800c2;
            public static final int TextAppearance_AppCompat_Title=0x7f0800c3;
            public static final int TextAppearance_AppCompat_Title_Inverse=0x7f0800c4;
            public static final int TextAppearance_AppCompat_Widget_ActionBar_Menu=0x7f0800c5;
            public static final int TextAppearance_AppCompat_Widget_ActionBar_Subtitle=0x7f0800c6;
            public static final int TextAppearance_AppCompat_Widget_ActionBar_Subtitle_Inverse=0x7f0800c7;
            public static final int TextAppearance_AppCompat_Widget_ActionBar_Title=0x7f0800c8;
            public static final int TextAppearance_AppCompat_Widget_ActionBar_Title_Inverse=0x7f0800c9;
            public static final int TextAppearance_AppCompat_Widget_ActionMode_Subtitle=0x7f0800ca;
            public static final int TextAppearance_AppCompat_Widget_ActionMode_Subtitle_Inverse=0x7f0800cb;
            public static final int TextAppearance_AppCompat_Widget_ActionMode_Title=0x7f0800cc;
            public static final int TextAppearance_AppCompat_Widget_ActionMode_Title_Inverse=0x7f0800cd;
            public static final int TextAppearance_AppCompat_Widget_DropDownItem=0x7f0800ce;
            public static final int TextAppearance_AppCompat_Widget_PopupMenu_Large=0x7f0800cf;
            public static final int TextAppearance_AppCompat_Widget_PopupMenu_Small=0x7f0800d0;
            public static final int TextAppearance_AppCompat_Widget_Switch=0x7f0800d1;
            public static final int TextAppearance_AppCompat_Widget_TextView_SpinnerItem=0x7f0800d2;
            public static final int TextAppearance_Design_CollapsingToolbar_Expanded=0x7f0800d3;
            public static final int TextAppearance_Design_Error=0x7f0800d4;
            public static final int TextAppearance_Design_Hint=0x7f0800d5;
            public static final int TextAppearance_Design_Snackbar_Action=0x7f0800d6;
            public static final int TextAppearance_Design_Snackbar_Message=0x7f0800d7;
            public static final int TextAppearance_Design_Tab=0x7f0800d8;
            public static final int TextAppearance_StatusBar_EventContent=0x7f08001c;
            public static final int TextAppearance_StatusBar_EventContent_Info=0x7f08001d;
            public static final int TextAppearance_StatusBar_EventContent_Line2=0x7f08001e;
            public static final int TextAppearance_StatusBar_EventContent_Time=0x7f08001f;
            public static final int TextAppearance_StatusBar_EventContent_Title=0x7f080020;
            public static final int TextAppearance_Widget_AppCompat_ExpandedMenu_Item=0x7f0800d9;
            public static final int TextAppearance_Widget_AppCompat_Toolbar_Subtitle=0x7f0800da;
            public static final int TextAppearance_Widget_AppCompat_Toolbar_Title=0x7f0800db;
            public static final int Theme_AppCompat=0x7f0800dc;
            public static final int Theme_AppCompat_CompactMenu=0x7f0800dd;
            public static final int Theme_AppCompat_Dialog=0x7f0800de;
            public static final int Theme_AppCompat_Dialog_Alert=0x7f0800df;
            public static final int Theme_AppCompat_Dialog_MinWidth=0x7f0800e0;
            public static final int Theme_AppCompat_DialogWhenLarge=0x7f0800e1;
            public static final int Theme_AppCompat_Light=0x7f0800e2;
            public static final int Theme_AppCompat_Light_DarkActionBar=0x7f0800e3;
            public static final int Theme_AppCompat_Light_Dialog=0x7f0800e4;
            public static final int Theme_AppCompat_Light_Dialog_Alert=0x7f0800e5;
            public static final int Theme_AppCompat_Light_Dialog_MinWidth=0x7f0800e6;
            public static final int Theme_AppCompat_Light_DialogWhenLarge=0x7f0800e7;
            public static final int Theme_AppCompat_Light_NoActionBar=0x7f0800e8;
            public static final int Theme_AppCompat_NoActionBar=0x7f0800e9;
            public static final int ThemeOverlay_AppCompat=0x7f0800ea;
            public static final int ThemeOverlay_AppCompat_ActionBar=0x7f0800eb;
            public static final int ThemeOverlay_AppCompat_Dark=0x7f0800ec;
            public static final int ThemeOverlay_AppCompat_Dark_ActionBar=0x7f0800ed;
            public static final int ThemeOverlay_AppCompat_Light=0x7f0800ee;
            public static final int Widget_AppCompat_ActionBar=0x7f0800ef;
            public static final int Widget_AppCompat_ActionBar_Solid=0x7f0800f0;
            public static final int Widget_AppCompat_ActionBar_TabBar=0x7f0800f1;
            public static final int Widget_AppCompat_ActionBar_TabText=0x7f0800f2;
            public static final int Widget_AppCompat_ActionBar_TabView=0x7f0800f3;
            public static final int Widget_AppCompat_ActionButton=0x7f0800f4;
            public static final int Widget_AppCompat_ActionButton_CloseMode=0x7f0800f5;
            public static final int Widget_AppCompat_ActionButton_Overflow=0x7f0800f6;
            public static final int Widget_AppCompat_ActionMode=0x7f0800f7;
            public static final int Widget_AppCompat_ActivityChooserView=0x7f0800f8;
            public static final int Widget_AppCompat_AutoCompleteTextView=0x7f0800f9;
            public static final int Widget_AppCompat_Button=0x7f0800fa;
            public static final int Widget_AppCompat_Button_Borderless=0x7f0800fb;
            public static final int Widget_AppCompat_Button_Borderless_Colored=0x7f0800fc;
            public static final int Widget_AppCompat_Button_ButtonBar_AlertDialog=0x7f0800fd;
            public static final int Widget_AppCompat_Button_Small=0x7f0800fe;
            public static final int Widget_AppCompat_ButtonBar=0x7f0800ff;
            public static final int Widget_AppCompat_ButtonBar_AlertDialog=0x7f080100;
            public static final int Widget_AppCompat_CompoundButton_CheckBox=0x7f080101;
            public static final int Widget_AppCompat_CompoundButton_RadioButton=0x7f080102;
            public static final int Widget_AppCompat_CompoundButton_Switch=0x7f080103;
            public static final int Widget_AppCompat_DrawerArrowToggle=0x7f080104;
            public static final int Widget_AppCompat_DropDownItem_Spinner=0x7f080105;
            public static final int Widget_AppCompat_EditText=0x7f080106;
            public static final int Widget_AppCompat_Light_ActionBar=0x7f080107;
            public static final int Widget_AppCompat_Light_ActionBar_Solid=0x7f080108;
            public static final int Widget_AppCompat_Light_ActionBar_Solid_Inverse=0x7f080109;
            public static final int Widget_AppCompat_Light_ActionBar_TabBar=0x7f08010a;
            public static final int Widget_AppCompat_Light_ActionBar_TabBar_Inverse=0x7f08010b;
            public static final int Widget_AppCompat_Light_ActionBar_TabText=0x7f08010c;
            public static final int Widget_AppCompat_Light_ActionBar_TabText_Inverse=0x7f08010d;
            public static final int Widget_AppCompat_Light_ActionBar_TabView=0x7f08010e;
            public static final int Widget_AppCompat_Light_ActionBar_TabView_Inverse=0x7f08010f;
            public static final int Widget_AppCompat_Light_ActionButton=0x7f080110;
            public static final int Widget_AppCompat_Light_ActionButton_CloseMode=0x7f080111;
            public static final int Widget_AppCompat_Light_ActionButton_Overflow=0x7f080112;
            public static final int Widget_AppCompat_Light_ActionMode_Inverse=0x7f080113;
            public static final int Widget_AppCompat_Light_ActivityChooserView=0x7f080114;
            public static final int Widget_AppCompat_Light_AutoCompleteTextView=0x7f080115;
            public static final int Widget_AppCompat_Light_DropDownItem_Spinner=0x7f080116;
            public static final int Widget_AppCompat_Light_ListPopupWindow=0x7f080117;
            public static final int Widget_AppCompat_Light_ListView_DropDown=0x7f080118;
            public static final int Widget_AppCompat_Light_PopupMenu=0x7f080119;
            public static final int Widget_AppCompat_Light_PopupMenu_Overflow=0x7f08011a;
            public static final int Widget_AppCompat_Light_SearchView=0x7f08011b;
            public static final int Widget_AppCompat_Light_Spinner_DropDown_ActionBar=0x7f08011c;
            public static final int Widget_AppCompat_ListPopupWindow=0x7f08011d;
            public static final int Widget_AppCompat_ListView=0x7f08011e;
            public static final int Widget_AppCompat_ListView_DropDown=0x7f08011f;
            public static final int Widget_AppCompat_ListView_Menu=0x7f080120;
            public static final int Widget_AppCompat_PopupMenu=0x7f080121;
            public static final int Widget_AppCompat_PopupMenu_Overflow=0x7f080122;
            public static final int Widget_AppCompat_PopupWindow=0x7f080123;
            public static final int Widget_AppCompat_ProgressBar=0x7f080124;
            public static final int Widget_AppCompat_ProgressBar_Horizontal=0x7f080125;
            public static final int Widget_AppCompat_RatingBar=0x7f080126;
            public static final int Widget_AppCompat_SearchView=0x7f080127;
            public static final int Widget_AppCompat_SearchView_ActionBar=0x7f080128;
            public static final int Widget_AppCompat_Spinner=0x7f080129;
            public static final int Widget_AppCompat_Spinner_DropDown=0x7f08012a;
            public static final int Widget_AppCompat_Spinner_DropDown_ActionBar=0x7f08012b;
            public static final int Widget_AppCompat_Spinner_Underlined=0x7f08012c;
            public static final int Widget_AppCompat_TextView_SpinnerItem=0x7f08012d;
            public static final int Widget_AppCompat_Toolbar=0x7f08012e;
            public static final int Widget_AppCompat_Toolbar_Button_Navigation=0x7f08012f;
            public static final int Widget_Design_AppBarLayout=0x7f080130;
            public static final int Widget_Design_CollapsingToolbar=0x7f080131;
            public static final int Widget_Design_CoordinatorLayout=0x7f080132;
            public static final int Widget_Design_FloatingActionButton=0x7f080133;
            public static final int Widget_Design_NavigationView=0x7f080134;
            public static final int Widget_Design_ScrimInsetsFrameLayout=0x7f080135;
            public static final int Widget_Design_Snackbar=0x7f080136;
            public static final int Widget_Design_TabLayout=0x7f080001;
            public static final int Widget_Design_TextInputLayout=0x7f080137;
        }
        public static final class styleable {
            /** Attributes that can be used with a ActionBar.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #ActionBar_background io.bitfountain.ashishpatel.sounddroid:background}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionBar_backgroundSplit io.bitfountain.ashishpatel.sounddroid:backgroundSplit}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionBar_backgroundStacked io.bitfountain.ashishpatel.sounddroid:backgroundStacked}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionBar_contentInsetEnd io.bitfountain.ashishpatel.sounddroid:contentInsetEnd}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionBar_contentInsetLeft io.bitfountain.ashishpatel.sounddroid:contentInsetLeft}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionBar_contentInsetRight io.bitfountain.ashishpatel.sounddroid:contentInsetRight}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionBar_contentInsetStart io.bitfountain.ashishpatel.sounddroid:contentInsetStart}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionBar_customNavigationLayout io.bitfountain.ashishpatel.sounddroid:customNavigationLayout}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionBar_displayOptions io.bitfountain.ashishpatel.sounddroid:displayOptions}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionBar_divider io.bitfountain.ashishpatel.sounddroid:divider}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionBar_elevation io.bitfountain.ashishpatel.sounddroid:elevation}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionBar_height io.bitfountain.ashishpatel.sounddroid:height}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionBar_hideOnContentScroll io.bitfountain.ashishpatel.sounddroid:hideOnContentScroll}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionBar_homeAsUpIndicator io.bitfountain.ashishpatel.sounddroid:homeAsUpIndicator}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionBar_homeLayout io.bitfountain.ashishpatel.sounddroid:homeLayout}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionBar_icon io.bitfountain.ashishpatel.sounddroid:icon}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionBar_indeterminateProgressStyle io.bitfountain.ashishpatel.sounddroid:indeterminateProgressStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionBar_itemPadding io.bitfountain.ashishpatel.sounddroid:itemPadding}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionBar_logo io.bitfountain.ashishpatel.sounddroid:logo}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionBar_navigationMode io.bitfountain.ashishpatel.sounddroid:navigationMode}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionBar_popupTheme io.bitfountain.ashishpatel.sounddroid:popupTheme}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionBar_progressBarPadding io.bitfountain.ashishpatel.sounddroid:progressBarPadding}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionBar_progressBarStyle io.bitfountain.ashishpatel.sounddroid:progressBarStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionBar_subtitle io.bitfountain.ashishpatel.sounddroid:subtitle}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionBar_subtitleTextStyle io.bitfountain.ashishpatel.sounddroid:subtitleTextStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionBar_title io.bitfountain.ashishpatel.sounddroid:title}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionBar_titleTextStyle io.bitfountain.ashishpatel.sounddroid:titleTextStyle}</code></td><td></td></tr>
               </table>
               @see #ActionBar_background
               @see #ActionBar_backgroundSplit
               @see #ActionBar_backgroundStacked
               @see #ActionBar_contentInsetEnd
               @see #ActionBar_contentInsetLeft
               @see #ActionBar_contentInsetRight
               @see #ActionBar_contentInsetStart
               @see #ActionBar_customNavigationLayout
               @see #ActionBar_displayOptions
               @see #ActionBar_divider
               @see #ActionBar_elevation
               @see #ActionBar_height
               @see #ActionBar_hideOnContentScroll
               @see #ActionBar_homeAsUpIndicator
               @see #ActionBar_homeLayout
               @see #ActionBar_icon
               @see #ActionBar_indeterminateProgressStyle
               @see #ActionBar_itemPadding
               @see #ActionBar_logo
               @see #ActionBar_navigationMode
               @see #ActionBar_popupTheme
               @see #ActionBar_progressBarPadding
               @see #ActionBar_progressBarStyle
               @see #ActionBar_subtitle
               @see #ActionBar_subtitleTextStyle
               @see #ActionBar_title
               @see #ActionBar_titleTextStyle
             */
            public static final int[] ActionBar = {
                0x7f010001, 0x7f010003, 0x7f010004, 0x7f010005,
                0x7f010006, 0x7f010007, 0x7f010008, 0x7f010009,
                0x7f01000a, 0x7f01000b, 0x7f01000c, 0x7f01000d,
                0x7f01000e, 0x7f01000f, 0x7f010010, 0x7f010011,
                0x7f010012, 0x7f010013, 0x7f010014, 0x7f010015,
                0x7f010016, 0x7f010017, 0x7f010018, 0x7f010019,
                0x7f01001a, 0x7f01001b, 0x7f0100b3
            };
            /**
              <p>This symbol is the offset where the {@link attr#background}
              attribute's value can be found in the {@link #ActionBar} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:background
            */
            public static final int ActionBar_background = 10;
            /**
              <p>This symbol is the offset where the {@link attr#backgroundSplit}
              attribute's value can be found in the {@link #ActionBar} array.


              <p>May be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
    <p>May be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:backgroundSplit
            */
            public static final int ActionBar_backgroundSplit = 12;
            /**
              <p>This symbol is the offset where the {@link attr#backgroundStacked}
              attribute's value can be found in the {@link #ActionBar} array.


              <p>May be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
    <p>May be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:backgroundStacked
            */
            public static final int ActionBar_backgroundStacked = 11;
            /**
              <p>This symbol is the offset where the {@link attr#contentInsetEnd}
              attribute's value can be found in the {@link #ActionBar} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:contentInsetEnd
            */
            public static final int ActionBar_contentInsetEnd = 21;
            /**
              <p>This symbol is the offset where the {@link attr#contentInsetLeft}
              attribute's value can be found in the {@link #ActionBar} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:contentInsetLeft
            */
            public static final int ActionBar_contentInsetLeft = 22;
            /**
              <p>This symbol is the offset where the {@link attr#contentInsetRight}
              attribute's value can be found in the {@link #ActionBar} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:contentInsetRight
            */
            public static final int ActionBar_contentInsetRight = 23;
            /**
              <p>This symbol is the offset where the {@link attr#contentInsetStart}
              attribute's value can be found in the {@link #ActionBar} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:contentInsetStart
            */
            public static final int ActionBar_contentInsetStart = 20;
            /**
              <p>This symbol is the offset where the {@link attr#customNavigationLayout}
              attribute's value can be found in the {@link #ActionBar} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:customNavigationLayout
            */
            public static final int ActionBar_customNavigationLayout = 13;
            /**
              <p>This symbol is the offset where the {@link attr#displayOptions}
              attribute's value can be found in the {@link #ActionBar} array.


              <p>Must be one or more (separated by '|') of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>none</code></td><td>0</td><td></td></tr>
    <tr><td><code>useLogo</code></td><td>0x1</td><td></td></tr>
    <tr><td><code>showHome</code></td><td>0x2</td><td></td></tr>
    <tr><td><code>homeAsUp</code></td><td>0x4</td><td></td></tr>
    <tr><td><code>showTitle</code></td><td>0x8</td><td></td></tr>
    <tr><td><code>showCustom</code></td><td>0x10</td><td></td></tr>
    <tr><td><code>disableHome</code></td><td>0x20</td><td></td></tr>
    </table>
              @attr name io.bitfountain.ashishpatel.sounddroid:displayOptions
            */
            public static final int ActionBar_displayOptions = 3;
            /**
              <p>This symbol is the offset where the {@link attr#divider}
              attribute's value can be found in the {@link #ActionBar} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:divider
            */
            public static final int ActionBar_divider = 9;
            /**
              <p>This symbol is the offset where the {@link attr#elevation}
              attribute's value can be found in the {@link #ActionBar} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:elevation
            */
            public static final int ActionBar_elevation = 24;
            /**
              <p>This symbol is the offset where the {@link attr#height}
              attribute's value can be found in the {@link #ActionBar} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:height
            */
            public static final int ActionBar_height = 0;
            /**
              <p>This symbol is the offset where the {@link attr#hideOnContentScroll}
              attribute's value can be found in the {@link #ActionBar} array.


              <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:hideOnContentScroll
            */
            public static final int ActionBar_hideOnContentScroll = 19;
            /**
              <p>This symbol is the offset where the {@link attr#homeAsUpIndicator}
              attribute's value can be found in the {@link #ActionBar} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:homeAsUpIndicator
            */
            public static final int ActionBar_homeAsUpIndicator = 26;
            /**
              <p>This symbol is the offset where the {@link attr#homeLayout}
              attribute's value can be found in the {@link #ActionBar} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:homeLayout
            */
            public static final int ActionBar_homeLayout = 14;
            /**
              <p>This symbol is the offset where the {@link attr#icon}
              attribute's value can be found in the {@link #ActionBar} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:icon
            */
            public static final int ActionBar_icon = 7;
            /**
              <p>This symbol is the offset where the {@link attr#indeterminateProgressStyle}
              attribute's value can be found in the {@link #ActionBar} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:indeterminateProgressStyle
            */
            public static final int ActionBar_indeterminateProgressStyle = 16;
            /**
              <p>This symbol is the offset where the {@link attr#itemPadding}
              attribute's value can be found in the {@link #ActionBar} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:itemPadding
            */
            public static final int ActionBar_itemPadding = 18;
            /**
              <p>This symbol is the offset where the {@link attr#logo}
              attribute's value can be found in the {@link #ActionBar} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:logo
            */
            public static final int ActionBar_logo = 8;
            /**
              <p>This symbol is the offset where the {@link attr#navigationMode}
              attribute's value can be found in the {@link #ActionBar} array.


              <p>Must be one of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>normal</code></td><td>0</td><td></td></tr>
    <tr><td><code>listMode</code></td><td>1</td><td></td></tr>
    <tr><td><code>tabMode</code></td><td>2</td><td></td></tr>
    </table>
              @attr name io.bitfountain.ashishpatel.sounddroid:navigationMode
            */
            public static final int ActionBar_navigationMode = 2;
            /**
              <p>This symbol is the offset where the {@link attr#popupTheme}
              attribute's value can be found in the {@link #ActionBar} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:popupTheme
            */
            public static final int ActionBar_popupTheme = 25;
            /**
              <p>This symbol is the offset where the {@link attr#progressBarPadding}
              attribute's value can be found in the {@link #ActionBar} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:progressBarPadding
            */
            public static final int ActionBar_progressBarPadding = 17;
            /**
              <p>This symbol is the offset where the {@link attr#progressBarStyle}
              attribute's value can be found in the {@link #ActionBar} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:progressBarStyle
            */
            public static final int ActionBar_progressBarStyle = 15;
            /**
              <p>This symbol is the offset where the {@link attr#subtitle}
              attribute's value can be found in the {@link #ActionBar} array.


              <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:subtitle
            */
            public static final int ActionBar_subtitle = 4;
            /**
              <p>This symbol is the offset where the {@link attr#subtitleTextStyle}
              attribute's value can be found in the {@link #ActionBar} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:subtitleTextStyle
            */
            public static final int ActionBar_subtitleTextStyle = 6;
            /**
              <p>This symbol is the offset where the {@link attr#title}
              attribute's value can be found in the {@link #ActionBar} array.


              <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:title
            */
            public static final int ActionBar_title = 1;
            /**
              <p>This symbol is the offset where the {@link attr#titleTextStyle}
              attribute's value can be found in the {@link #ActionBar} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:titleTextStyle
            */
            public static final int ActionBar_titleTextStyle = 5;
            /** Attributes that can be used with a ActionBarLayout.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #ActionBarLayout_android_layout_gravity android:layout_gravity}</code></td><td></td></tr>
               </table>
               @see #ActionBarLayout_android_layout_gravity
             */
            public static final int[] ActionBarLayout = {
                0x010100b3
            };
            /**
              <p>This symbol is the offset where the {@link android.R.attr#layout_gravity}
              attribute's value can be found in the {@link #ActionBarLayout} array.
              @attr name android:layout_gravity
            */
            public static final int ActionBarLayout_android_layout_gravity = 0;
            /** Attributes that can be used with a ActionMenuItemView.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #ActionMenuItemView_android_minWidth android:minWidth}</code></td><td></td></tr>
               </table>
               @see #ActionMenuItemView_android_minWidth
             */
            public static final int[] ActionMenuItemView = {
                0x0101013f
            };
            /**
              <p>This symbol is the offset where the {@link android.R.attr#minWidth}
              attribute's value can be found in the {@link #ActionMenuItemView} array.
              @attr name android:minWidth
            */
            public static final int ActionMenuItemView_android_minWidth = 0;
            /** Attributes that can be used with a ActionMenuView.
             */
            public static final int[] ActionMenuView = {

            };
            /** Attributes that can be used with a ActionMode.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #ActionMode_background io.bitfountain.ashishpatel.sounddroid:background}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionMode_backgroundSplit io.bitfountain.ashishpatel.sounddroid:backgroundSplit}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionMode_closeItemLayout io.bitfountain.ashishpatel.sounddroid:closeItemLayout}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionMode_height io.bitfountain.ashishpatel.sounddroid:height}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionMode_subtitleTextStyle io.bitfountain.ashishpatel.sounddroid:subtitleTextStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #ActionMode_titleTextStyle io.bitfountain.ashishpatel.sounddroid:titleTextStyle}</code></td><td></td></tr>
               </table>
               @see #ActionMode_background
               @see #ActionMode_backgroundSplit
               @see #ActionMode_closeItemLayout
               @see #ActionMode_height
               @see #ActionMode_subtitleTextStyle
               @see #ActionMode_titleTextStyle
             */
            public static final int[] ActionMode = {
                0x7f010001, 0x7f010007, 0x7f010008, 0x7f01000c,
                0x7f01000e, 0x7f01001c
            };
            /**
              <p>This symbol is the offset where the {@link attr#background}
              attribute's value can be found in the {@link #ActionMode} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:background
            */
            public static final int ActionMode_background = 3;
            /**
              <p>This symbol is the offset where the {@link attr#backgroundSplit}
              attribute's value can be found in the {@link #ActionMode} array.


              <p>May be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
    <p>May be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:backgroundSplit
            */
            public static final int ActionMode_backgroundSplit = 4;
            /**
              <p>This symbol is the offset where the {@link attr#closeItemLayout}
              attribute's value can be found in the {@link #ActionMode} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:closeItemLayout
            */
            public static final int ActionMode_closeItemLayout = 5;
            /**
              <p>This symbol is the offset where the {@link attr#height}
              attribute's value can be found in the {@link #ActionMode} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:height
            */
            public static final int ActionMode_height = 0;
            /**
              <p>This symbol is the offset where the {@link attr#subtitleTextStyle}
              attribute's value can be found in the {@link #ActionMode} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:subtitleTextStyle
            */
            public static final int ActionMode_subtitleTextStyle = 2;
            /**
              <p>This symbol is the offset where the {@link attr#titleTextStyle}
              attribute's value can be found in the {@link #ActionMode} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:titleTextStyle
            */
            public static final int ActionMode_titleTextStyle = 1;
            /** Attributes that can be used with a ActivityChooserView.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #ActivityChooserView_expandActivityOverflowButtonDrawable io.bitfountain.ashishpatel.sounddroid:expandActivityOverflowButtonDrawable}</code></td><td></td></tr>
               <tr><td><code>{@link #ActivityChooserView_initialActivityCount io.bitfountain.ashishpatel.sounddroid:initialActivityCount}</code></td><td></td></tr>
               </table>
               @see #ActivityChooserView_expandActivityOverflowButtonDrawable
               @see #ActivityChooserView_initialActivityCount
             */
            public static final int[] ActivityChooserView = {
                0x7f01001d, 0x7f01001e
            };
            /**
              <p>This symbol is the offset where the {@link attr#expandActivityOverflowButtonDrawable}
              attribute's value can be found in the {@link #ActivityChooserView} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:expandActivityOverflowButtonDrawable
            */
            public static final int ActivityChooserView_expandActivityOverflowButtonDrawable = 1;
            /**
              <p>This symbol is the offset where the {@link attr#initialActivityCount}
              attribute's value can be found in the {@link #ActivityChooserView} array.


              <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:initialActivityCount
            */
            public static final int ActivityChooserView_initialActivityCount = 0;
            /** Attributes that can be used with a AlertDialog.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #AlertDialog_android_layout android:layout}</code></td><td></td></tr>
               <tr><td><code>{@link #AlertDialog_buttonPanelSideLayout io.bitfountain.ashishpatel.sounddroid:buttonPanelSideLayout}</code></td><td></td></tr>
               <tr><td><code>{@link #AlertDialog_listItemLayout io.bitfountain.ashishpatel.sounddroid:listItemLayout}</code></td><td></td></tr>
               <tr><td><code>{@link #AlertDialog_listLayout io.bitfountain.ashishpatel.sounddroid:listLayout}</code></td><td></td></tr>
               <tr><td><code>{@link #AlertDialog_multiChoiceItemLayout io.bitfountain.ashishpatel.sounddroid:multiChoiceItemLayout}</code></td><td></td></tr>
               <tr><td><code>{@link #AlertDialog_singleChoiceItemLayout io.bitfountain.ashishpatel.sounddroid:singleChoiceItemLayout}</code></td><td></td></tr>
               </table>
               @see #AlertDialog_android_layout
               @see #AlertDialog_buttonPanelSideLayout
               @see #AlertDialog_listItemLayout
               @see #AlertDialog_listLayout
               @see #AlertDialog_multiChoiceItemLayout
               @see #AlertDialog_singleChoiceItemLayout
             */
            public static final int[] AlertDialog = {
                0x010100f2, 0x7f01001f, 0x7f010020, 0x7f010021,
                0x7f010022, 0x7f010023
            };
            /**
              <p>This symbol is the offset where the {@link android.R.attr#layout}
              attribute's value can be found in the {@link #AlertDialog} array.
              @attr name android:layout
            */
            public static final int AlertDialog_android_layout = 0;
            /**
              <p>This symbol is the offset where the {@link attr#buttonPanelSideLayout}
              attribute's value can be found in the {@link #AlertDialog} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:buttonPanelSideLayout
            */
            public static final int AlertDialog_buttonPanelSideLayout = 1;
            /**
              <p>This symbol is the offset where the {@link attr#listItemLayout}
              attribute's value can be found in the {@link #AlertDialog} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:listItemLayout
            */
            public static final int AlertDialog_listItemLayout = 5;
            /**
              <p>This symbol is the offset where the {@link attr#listLayout}
              attribute's value can be found in the {@link #AlertDialog} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:listLayout
            */
            public static final int AlertDialog_listLayout = 2;
            /**
              <p>This symbol is the offset where the {@link attr#multiChoiceItemLayout}
              attribute's value can be found in the {@link #AlertDialog} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:multiChoiceItemLayout
            */
            public static final int AlertDialog_multiChoiceItemLayout = 3;
            /**
              <p>This symbol is the offset where the {@link attr#singleChoiceItemLayout}
              attribute's value can be found in the {@link #AlertDialog} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:singleChoiceItemLayout
            */
            public static final int AlertDialog_singleChoiceItemLayout = 4;
            /** Attributes that can be used with a AppBarLayout.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #AppBarLayout_android_background android:background}</code></td><td></td></tr>
               <tr><td><code>{@link #AppBarLayout_elevation io.bitfountain.ashishpatel.sounddroid:elevation}</code></td><td></td></tr>
               </table>
               @see #AppBarLayout_android_background
               @see #AppBarLayout_elevation
             */
            public static final int[] AppBarLayout = {
                0x010100d4, 0x7f01001a
            };
            /**
              <p>This symbol is the offset where the {@link android.R.attr#background}
              attribute's value can be found in the {@link #AppBarLayout} array.
              @attr name android:background
            */
            public static final int AppBarLayout_android_background = 0;
            /**
              <p>This symbol is the offset where the {@link attr#elevation}
              attribute's value can be found in the {@link #AppBarLayout} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:elevation
            */
            public static final int AppBarLayout_elevation = 1;
            /** Attributes that can be used with a AppBarLayout_LayoutParams.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #AppBarLayout_LayoutParams_layout_scrollFlags io.bitfountain.ashishpatel.sounddroid:layout_scrollFlags}</code></td><td></td></tr>
               <tr><td><code>{@link #AppBarLayout_LayoutParams_layout_scrollInterpolator io.bitfountain.ashishpatel.sounddroid:layout_scrollInterpolator}</code></td><td></td></tr>
               </table>
               @see #AppBarLayout_LayoutParams_layout_scrollFlags
               @see #AppBarLayout_LayoutParams_layout_scrollInterpolator
             */
            public static final int[] AppBarLayout_LayoutParams = {
                0x7f010024, 0x7f010025
            };
            /**
              <p>This symbol is the offset where the {@link attr#layout_scrollFlags}
              attribute's value can be found in the {@link #AppBarLayout_LayoutParams} array.


              <p>Must be one or more (separated by '|') of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>scroll</code></td><td>0x1</td><td></td></tr>
    <tr><td><code>exitUntilCollapsed</code></td><td>0x2</td><td></td></tr>
    <tr><td><code>enterAlways</code></td><td>0x4</td><td></td></tr>
    <tr><td><code>enterAlwaysCollapsed</code></td><td>0x8</td><td></td></tr>
    </table>
              @attr name io.bitfountain.ashishpatel.sounddroid:layout_scrollFlags
            */
            public static final int AppBarLayout_LayoutParams_layout_scrollFlags = 0;
            /**
              <p>This symbol is the offset where the {@link attr#layout_scrollInterpolator}
              attribute's value can be found in the {@link #AppBarLayout_LayoutParams} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:layout_scrollInterpolator
            */
            public static final int AppBarLayout_LayoutParams_layout_scrollInterpolator = 1;
            /** Attributes that can be used with a AppCompatTextView.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #AppCompatTextView_android_textAppearance android:textAppearance}</code></td><td></td></tr>
               <tr><td><code>{@link #AppCompatTextView_textAllCaps io.bitfountain.ashishpatel.sounddroid:textAllCaps}</code></td><td></td></tr>
               </table>
               @see #AppCompatTextView_android_textAppearance
               @see #AppCompatTextView_textAllCaps
             */
            public static final int[] AppCompatTextView = {
                0x01010034, 0x7f010026
            };
            /**
              <p>This symbol is the offset where the {@link android.R.attr#textAppearance}
              attribute's value can be found in the {@link #AppCompatTextView} array.
              @attr name android:textAppearance
            */
            public static final int AppCompatTextView_android_textAppearance = 0;
            /**
              <p>This symbol is the offset where the {@link attr#textAllCaps}
              attribute's value can be found in the {@link #AppCompatTextView} array.


              <p>May be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
    <p>May be a boolean value, either "<code>true</code>" or "<code>false</code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:textAllCaps
            */
            public static final int AppCompatTextView_textAllCaps = 1;
            /** Attributes that can be used with a CollapsingAppBarLayout_LayoutParams.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #CollapsingAppBarLayout_LayoutParams_layout_collapseMode io.bitfountain.ashishpatel.sounddroid:layout_collapseMode}</code></td><td></td></tr>
               <tr><td><code>{@link #CollapsingAppBarLayout_LayoutParams_layout_collapseParallaxMultiplier io.bitfountain.ashishpatel.sounddroid:layout_collapseParallaxMultiplier}</code></td><td></td></tr>
               </table>
               @see #CollapsingAppBarLayout_LayoutParams_layout_collapseMode
               @see #CollapsingAppBarLayout_LayoutParams_layout_collapseParallaxMultiplier
             */
            public static final int[] CollapsingAppBarLayout_LayoutParams = {
                0x7f010027, 0x7f010028
            };
            /**
              <p>This symbol is the offset where the {@link attr#layout_collapseMode}
              attribute's value can be found in the {@link #CollapsingAppBarLayout_LayoutParams} array.


              <p>Must be one of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>none</code></td><td>0</td><td></td></tr>
    <tr><td><code>pin</code></td><td>1</td><td></td></tr>
    <tr><td><code>parallax</code></td><td>2</td><td></td></tr>
    </table>
              @attr name io.bitfountain.ashishpatel.sounddroid:layout_collapseMode
            */
            public static final int CollapsingAppBarLayout_LayoutParams_layout_collapseMode = 0;
            /**
              <p>This symbol is the offset where the {@link attr#layout_collapseParallaxMultiplier}
              attribute's value can be found in the {@link #CollapsingAppBarLayout_LayoutParams} array.


              <p>Must be a floating point value, such as "<code>1.2</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:layout_collapseParallaxMultiplier
            */
            public static final int CollapsingAppBarLayout_LayoutParams_layout_collapseParallaxMultiplier = 1;
            /** Attributes that can be used with a CollapsingToolbarLayout.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #CollapsingToolbarLayout_collapsedTitleTextAppearance io.bitfountain.ashishpatel.sounddroid:collapsedTitleTextAppearance}</code></td><td></td></tr>
               <tr><td><code>{@link #CollapsingToolbarLayout_contentScrim io.bitfountain.ashishpatel.sounddroid:contentScrim}</code></td><td></td></tr>
               <tr><td><code>{@link #CollapsingToolbarLayout_expandedTitleMargin io.bitfountain.ashishpatel.sounddroid:expandedTitleMargin}</code></td><td></td></tr>
               <tr><td><code>{@link #CollapsingToolbarLayout_expandedTitleMarginBottom io.bitfountain.ashishpatel.sounddroid:expandedTitleMarginBottom}</code></td><td></td></tr>
               <tr><td><code>{@link #CollapsingToolbarLayout_expandedTitleMarginEnd io.bitfountain.ashishpatel.sounddroid:expandedTitleMarginEnd}</code></td><td></td></tr>
               <tr><td><code>{@link #CollapsingToolbarLayout_expandedTitleMarginStart io.bitfountain.ashishpatel.sounddroid:expandedTitleMarginStart}</code></td><td></td></tr>
               <tr><td><code>{@link #CollapsingToolbarLayout_expandedTitleMarginTop io.bitfountain.ashishpatel.sounddroid:expandedTitleMarginTop}</code></td><td></td></tr>
               <tr><td><code>{@link #CollapsingToolbarLayout_expandedTitleTextAppearance io.bitfountain.ashishpatel.sounddroid:expandedTitleTextAppearance}</code></td><td></td></tr>
               <tr><td><code>{@link #CollapsingToolbarLayout_statusBarScrim io.bitfountain.ashishpatel.sounddroid:statusBarScrim}</code></td><td></td></tr>
               <tr><td><code>{@link #CollapsingToolbarLayout_toolbarId io.bitfountain.ashishpatel.sounddroid:toolbarId}</code></td><td></td></tr>
               </table>
               @see #CollapsingToolbarLayout_collapsedTitleTextAppearance
               @see #CollapsingToolbarLayout_contentScrim
               @see #CollapsingToolbarLayout_expandedTitleMargin
               @see #CollapsingToolbarLayout_expandedTitleMarginBottom
               @see #CollapsingToolbarLayout_expandedTitleMarginEnd
               @see #CollapsingToolbarLayout_expandedTitleMarginStart
               @see #CollapsingToolbarLayout_expandedTitleMarginTop
               @see #CollapsingToolbarLayout_expandedTitleTextAppearance
               @see #CollapsingToolbarLayout_statusBarScrim
               @see #CollapsingToolbarLayout_toolbarId
             */
            public static final int[] CollapsingToolbarLayout = {
                0x7f010029, 0x7f01002a, 0x7f01002b, 0x7f01002c,
                0x7f01002d, 0x7f01002e, 0x7f01002f, 0x7f010030,
                0x7f010031, 0x7f010032
            };
            /**
              <p>This symbol is the offset where the {@link attr#collapsedTitleTextAppearance}
              attribute's value can be found in the {@link #CollapsingToolbarLayout} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:collapsedTitleTextAppearance
            */
            public static final int CollapsingToolbarLayout_collapsedTitleTextAppearance = 6;
            /**
              <p>This symbol is the offset where the {@link attr#contentScrim}
              attribute's value can be found in the {@link #CollapsingToolbarLayout} array.


              <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:contentScrim
            */
            public static final int CollapsingToolbarLayout_contentScrim = 7;
            /**
              <p>This symbol is the offset where the {@link attr#expandedTitleMargin}
              attribute's value can be found in the {@link #CollapsingToolbarLayout} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:expandedTitleMargin
            */
            public static final int CollapsingToolbarLayout_expandedTitleMargin = 0;
            /**
              <p>This symbol is the offset where the {@link attr#expandedTitleMarginBottom}
              attribute's value can be found in the {@link #CollapsingToolbarLayout} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:expandedTitleMarginBottom
            */
            public static final int CollapsingToolbarLayout_expandedTitleMarginBottom = 4;
            /**
              <p>This symbol is the offset where the {@link attr#expandedTitleMarginEnd}
              attribute's value can be found in the {@link #CollapsingToolbarLayout} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:expandedTitleMarginEnd
            */
            public static final int CollapsingToolbarLayout_expandedTitleMarginEnd = 3;
            /**
              <p>This symbol is the offset where the {@link attr#expandedTitleMarginStart}
              attribute's value can be found in the {@link #CollapsingToolbarLayout} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:expandedTitleMarginStart
            */
            public static final int CollapsingToolbarLayout_expandedTitleMarginStart = 1;
            /**
              <p>This symbol is the offset where the {@link attr#expandedTitleMarginTop}
              attribute's value can be found in the {@link #CollapsingToolbarLayout} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:expandedTitleMarginTop
            */
            public static final int CollapsingToolbarLayout_expandedTitleMarginTop = 2;
            /**
              <p>This symbol is the offset where the {@link attr#expandedTitleTextAppearance}
              attribute's value can be found in the {@link #CollapsingToolbarLayout} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:expandedTitleTextAppearance
            */
            public static final int CollapsingToolbarLayout_expandedTitleTextAppearance = 5;
            /**
              <p>This symbol is the offset where the {@link attr#statusBarScrim}
              attribute's value can be found in the {@link #CollapsingToolbarLayout} array.


              <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:statusBarScrim
            */
            public static final int CollapsingToolbarLayout_statusBarScrim = 8;
            /**
              <p>This symbol is the offset where the {@link attr#toolbarId}
              attribute's value can be found in the {@link #CollapsingToolbarLayout} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:toolbarId
            */
            public static final int CollapsingToolbarLayout_toolbarId = 9;
            /** Attributes that can be used with a CoordinatorLayout.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #CoordinatorLayout_keylines io.bitfountain.ashishpatel.sounddroid:keylines}</code></td><td></td></tr>
               <tr><td><code>{@link #CoordinatorLayout_statusBarBackground io.bitfountain.ashishpatel.sounddroid:statusBarBackground}</code></td><td></td></tr>
               </table>
               @see #CoordinatorLayout_keylines
               @see #CoordinatorLayout_statusBarBackground
             */
            public static final int[] CoordinatorLayout = {
                0x7f010033, 0x7f010034
            };
            /**
              <p>This symbol is the offset where the {@link attr#keylines}
              attribute's value can be found in the {@link #CoordinatorLayout} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:keylines
            */
            public static final int CoordinatorLayout_keylines = 0;
            /**
              <p>This symbol is the offset where the {@link attr#statusBarBackground}
              attribute's value can be found in the {@link #CoordinatorLayout} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:statusBarBackground
            */
            public static final int CoordinatorLayout_statusBarBackground = 1;
            /** Attributes that can be used with a CoordinatorLayout_LayoutParams.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #CoordinatorLayout_LayoutParams_android_layout_gravity android:layout_gravity}</code></td><td></td></tr>
               <tr><td><code>{@link #CoordinatorLayout_LayoutParams_layout_anchor io.bitfountain.ashishpatel.sounddroid:layout_anchor}</code></td><td></td></tr>
               <tr><td><code>{@link #CoordinatorLayout_LayoutParams_layout_anchorGravity io.bitfountain.ashishpatel.sounddroid:layout_anchorGravity}</code></td><td></td></tr>
               <tr><td><code>{@link #CoordinatorLayout_LayoutParams_layout_behavior io.bitfountain.ashishpatel.sounddroid:layout_behavior}</code></td><td></td></tr>
               <tr><td><code>{@link #CoordinatorLayout_LayoutParams_layout_keyline io.bitfountain.ashishpatel.sounddroid:layout_keyline}</code></td><td></td></tr>
               </table>
               @see #CoordinatorLayout_LayoutParams_android_layout_gravity
               @see #CoordinatorLayout_LayoutParams_layout_anchor
               @see #CoordinatorLayout_LayoutParams_layout_anchorGravity
               @see #CoordinatorLayout_LayoutParams_layout_behavior
               @see #CoordinatorLayout_LayoutParams_layout_keyline
             */
            public static final int[] CoordinatorLayout_LayoutParams = {
                0x010100b3, 0x7f010035, 0x7f010036, 0x7f010037,
                0x7f010038
            };
            /**
              <p>This symbol is the offset where the {@link android.R.attr#layout_gravity}
              attribute's value can be found in the {@link #CoordinatorLayout_LayoutParams} array.
              @attr name android:layout_gravity
            */
            public static final int CoordinatorLayout_LayoutParams_android_layout_gravity = 0;
            /**
              <p>This symbol is the offset where the {@link attr#layout_anchor}
              attribute's value can be found in the {@link #CoordinatorLayout_LayoutParams} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:layout_anchor
            */
            public static final int CoordinatorLayout_LayoutParams_layout_anchor = 2;
            /**
              <p>This symbol is the offset where the {@link attr#layout_anchorGravity}
              attribute's value can be found in the {@link #CoordinatorLayout_LayoutParams} array.


              <p>Must be one or more (separated by '|') of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>top</code></td><td>0x30</td><td></td></tr>
    <tr><td><code>bottom</code></td><td>0x50</td><td></td></tr>
    <tr><td><code>left</code></td><td>0x03</td><td></td></tr>
    <tr><td><code>right</code></td><td>0x05</td><td></td></tr>
    <tr><td><code>center_vertical</code></td><td>0x10</td><td></td></tr>
    <tr><td><code>fill_vertical</code></td><td>0x70</td><td></td></tr>
    <tr><td><code>center_horizontal</code></td><td>0x01</td><td></td></tr>
    <tr><td><code>fill_horizontal</code></td><td>0x07</td><td></td></tr>
    <tr><td><code>center</code></td><td>0x11</td><td></td></tr>
    <tr><td><code>fill</code></td><td>0x77</td><td></td></tr>
    <tr><td><code>clip_vertical</code></td><td>0x80</td><td></td></tr>
    <tr><td><code>clip_horizontal</code></td><td>0x08</td><td></td></tr>
    <tr><td><code>start</code></td><td>0x00800003</td><td></td></tr>
    <tr><td><code>end</code></td><td>0x00800005</td><td></td></tr>
    </table>
              @attr name io.bitfountain.ashishpatel.sounddroid:layout_anchorGravity
            */
            public static final int CoordinatorLayout_LayoutParams_layout_anchorGravity = 4;
            /**
              <p>This symbol is the offset where the {@link attr#layout_behavior}
              attribute's value can be found in the {@link #CoordinatorLayout_LayoutParams} array.


              <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:layout_behavior
            */
            public static final int CoordinatorLayout_LayoutParams_layout_behavior = 1;
            /**
              <p>This symbol is the offset where the {@link attr#layout_keyline}
              attribute's value can be found in the {@link #CoordinatorLayout_LayoutParams} array.


              <p>Must be an integer value, such as "<code>100</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:layout_keyline
            */
            public static final int CoordinatorLayout_LayoutParams_layout_keyline = 3;
            /** Attributes that can be used with a DrawerArrowToggle.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #DrawerArrowToggle_barSize io.bitfountain.ashishpatel.sounddroid:barSize}</code></td><td></td></tr>
               <tr><td><code>{@link #DrawerArrowToggle_color io.bitfountain.ashishpatel.sounddroid:color}</code></td><td></td></tr>
               <tr><td><code>{@link #DrawerArrowToggle_drawableSize io.bitfountain.ashishpatel.sounddroid:drawableSize}</code></td><td></td></tr>
               <tr><td><code>{@link #DrawerArrowToggle_gapBetweenBars io.bitfountain.ashishpatel.sounddroid:gapBetweenBars}</code></td><td></td></tr>
               <tr><td><code>{@link #DrawerArrowToggle_middleBarArrowSize io.bitfountain.ashishpatel.sounddroid:middleBarArrowSize}</code></td><td></td></tr>
               <tr><td><code>{@link #DrawerArrowToggle_spinBars io.bitfountain.ashishpatel.sounddroid:spinBars}</code></td><td></td></tr>
               <tr><td><code>{@link #DrawerArrowToggle_thickness io.bitfountain.ashishpatel.sounddroid:thickness}</code></td><td></td></tr>
               <tr><td><code>{@link #DrawerArrowToggle_topBottomBarArrowSize io.bitfountain.ashishpatel.sounddroid:topBottomBarArrowSize}</code></td><td></td></tr>
               </table>
               @see #DrawerArrowToggle_barSize
               @see #DrawerArrowToggle_color
               @see #DrawerArrowToggle_drawableSize
               @see #DrawerArrowToggle_gapBetweenBars
               @see #DrawerArrowToggle_middleBarArrowSize
               @see #DrawerArrowToggle_spinBars
               @see #DrawerArrowToggle_thickness
               @see #DrawerArrowToggle_topBottomBarArrowSize
             */
            public static final int[] DrawerArrowToggle = {
                0x7f010039, 0x7f01003a, 0x7f01003b, 0x7f01003c,
                0x7f01003d, 0x7f01003e, 0x7f01003f, 0x7f010040
            };
            /**
              <p>This symbol is the offset where the {@link attr#barSize}
              attribute's value can be found in the {@link #DrawerArrowToggle} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:barSize
            */
            public static final int DrawerArrowToggle_barSize = 6;
            /**
              <p>This symbol is the offset where the {@link attr#color}
              attribute's value can be found in the {@link #DrawerArrowToggle} array.


              <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:color
            */
            public static final int DrawerArrowToggle_color = 0;
            /**
              <p>This symbol is the offset where the {@link attr#drawableSize}
              attribute's value can be found in the {@link #DrawerArrowToggle} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:drawableSize
            */
            public static final int DrawerArrowToggle_drawableSize = 2;
            /**
              <p>This symbol is the offset where the {@link attr#gapBetweenBars}
              attribute's value can be found in the {@link #DrawerArrowToggle} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:gapBetweenBars
            */
            public static final int DrawerArrowToggle_gapBetweenBars = 3;
            /**
              <p>This symbol is the offset where the {@link attr#middleBarArrowSize}
              attribute's value can be found in the {@link #DrawerArrowToggle} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:middleBarArrowSize
            */
            public static final int DrawerArrowToggle_middleBarArrowSize = 5;
            /**
              <p>This symbol is the offset where the {@link attr#spinBars}
              attribute's value can be found in the {@link #DrawerArrowToggle} array.


              <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:spinBars
            */
            public static final int DrawerArrowToggle_spinBars = 1;
            /**
              <p>This symbol is the offset where the {@link attr#thickness}
              attribute's value can be found in the {@link #DrawerArrowToggle} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:thickness
            */
            public static final int DrawerArrowToggle_thickness = 7;
            /**
              <p>This symbol is the offset where the {@link attr#topBottomBarArrowSize}
              attribute's value can be found in the {@link #DrawerArrowToggle} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:topBottomBarArrowSize
            */
            public static final int DrawerArrowToggle_topBottomBarArrowSize = 4;
            /** Attributes that can be used with a FloatingActionButton.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #FloatingActionButton_android_background android:background}</code></td><td></td></tr>
               <tr><td><code>{@link #FloatingActionButton_backgroundTint io.bitfountain.ashishpatel.sounddroid:backgroundTint}</code></td><td></td></tr>
               <tr><td><code>{@link #FloatingActionButton_backgroundTintMode io.bitfountain.ashishpatel.sounddroid:backgroundTintMode}</code></td><td></td></tr>
               <tr><td><code>{@link #FloatingActionButton_borderWidth io.bitfountain.ashishpatel.sounddroid:borderWidth}</code></td><td></td></tr>
               <tr><td><code>{@link #FloatingActionButton_elevation io.bitfountain.ashishpatel.sounddroid:elevation}</code></td><td></td></tr>
               <tr><td><code>{@link #FloatingActionButton_fabSize io.bitfountain.ashishpatel.sounddroid:fabSize}</code></td><td></td></tr>
               <tr><td><code>{@link #FloatingActionButton_pressedTranslationZ io.bitfountain.ashishpatel.sounddroid:pressedTranslationZ}</code></td><td></td></tr>
               <tr><td><code>{@link #FloatingActionButton_rippleColor io.bitfountain.ashishpatel.sounddroid:rippleColor}</code></td><td></td></tr>
               </table>
               @see #FloatingActionButton_android_background
               @see #FloatingActionButton_backgroundTint
               @see #FloatingActionButton_backgroundTintMode
               @see #FloatingActionButton_borderWidth
               @see #FloatingActionButton_elevation
               @see #FloatingActionButton_fabSize
               @see #FloatingActionButton_pressedTranslationZ
               @see #FloatingActionButton_rippleColor
             */
            public static final int[] FloatingActionButton = {
                0x010100d4, 0x7f01001a, 0x7f010041, 0x7f010042,
                0x7f010043, 0x7f010044, 0x7f0100fd, 0x7f0100fe
            };
            /**
              <p>This symbol is the offset where the {@link android.R.attr#background}
              attribute's value can be found in the {@link #FloatingActionButton} array.
              @attr name android:background
            */
            public static final int FloatingActionButton_android_background = 0;
            /**
              <p>This symbol is the offset where the {@link attr#backgroundTint}
              attribute's value can be found in the {@link #FloatingActionButton} array.


              <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:backgroundTint
            */
            public static final int FloatingActionButton_backgroundTint = 6;
            /**
              <p>This symbol is the offset where the {@link attr#backgroundTintMode}
              attribute's value can be found in the {@link #FloatingActionButton} array.


              <p>Must be one of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>src_over</code></td><td>3</td><td></td></tr>
    <tr><td><code>src_in</code></td><td>5</td><td></td></tr>
    <tr><td><code>src_atop</code></td><td>9</td><td></td></tr>
    <tr><td><code>multiply</code></td><td>14</td><td></td></tr>
    <tr><td><code>screen</code></td><td>15</td><td></td></tr>
    </table>
              @attr name io.bitfountain.ashishpatel.sounddroid:backgroundTintMode
            */
            public static final int FloatingActionButton_backgroundTintMode = 7;
            /**
              <p>This symbol is the offset where the {@link attr#borderWidth}
              attribute's value can be found in the {@link #FloatingActionButton} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:borderWidth
            */
            public static final int FloatingActionButton_borderWidth = 5;
            /**
              <p>This symbol is the offset where the {@link attr#elevation}
              attribute's value can be found in the {@link #FloatingActionButton} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:elevation
            */
            public static final int FloatingActionButton_elevation = 1;
            /**
              <p>This symbol is the offset where the {@link attr#fabSize}
              attribute's value can be found in the {@link #FloatingActionButton} array.


              <p>Must be one of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>normal</code></td><td>0</td><td></td></tr>
    <tr><td><code>mini</code></td><td>1</td><td></td></tr>
    </table>
              @attr name io.bitfountain.ashishpatel.sounddroid:fabSize
            */
            public static final int FloatingActionButton_fabSize = 3;
            /**
              <p>This symbol is the offset where the {@link attr#pressedTranslationZ}
              attribute's value can be found in the {@link #FloatingActionButton} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:pressedTranslationZ
            */
            public static final int FloatingActionButton_pressedTranslationZ = 4;
            /**
              <p>This symbol is the offset where the {@link attr#rippleColor}
              attribute's value can be found in the {@link #FloatingActionButton} array.


              <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:rippleColor
            */
            public static final int FloatingActionButton_rippleColor = 2;
            /** Attributes that can be used with a LinearLayoutCompat.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #LinearLayoutCompat_android_baselineAligned android:baselineAligned}</code></td><td></td></tr>
               <tr><td><code>{@link #LinearLayoutCompat_android_baselineAlignedChildIndex android:baselineAlignedChildIndex}</code></td><td></td></tr>
               <tr><td><code>{@link #LinearLayoutCompat_android_gravity android:gravity}</code></td><td></td></tr>
               <tr><td><code>{@link #LinearLayoutCompat_android_orientation android:orientation}</code></td><td></td></tr>
               <tr><td><code>{@link #LinearLayoutCompat_android_weightSum android:weightSum}</code></td><td></td></tr>
               <tr><td><code>{@link #LinearLayoutCompat_divider io.bitfountain.ashishpatel.sounddroid:divider}</code></td><td></td></tr>
               <tr><td><code>{@link #LinearLayoutCompat_dividerPadding io.bitfountain.ashishpatel.sounddroid:dividerPadding}</code></td><td></td></tr>
               <tr><td><code>{@link #LinearLayoutCompat_measureWithLargestChild io.bitfountain.ashishpatel.sounddroid:measureWithLargestChild}</code></td><td></td></tr>
               <tr><td><code>{@link #LinearLayoutCompat_showDividers io.bitfountain.ashishpatel.sounddroid:showDividers}</code></td><td></td></tr>
               </table>
               @see #LinearLayoutCompat_android_baselineAligned
               @see #LinearLayoutCompat_android_baselineAlignedChildIndex
               @see #LinearLayoutCompat_android_gravity
               @see #LinearLayoutCompat_android_orientation
               @see #LinearLayoutCompat_android_weightSum
               @see #LinearLayoutCompat_divider
               @see #LinearLayoutCompat_dividerPadding
               @see #LinearLayoutCompat_measureWithLargestChild
               @see #LinearLayoutCompat_showDividers
             */
            public static final int[] LinearLayoutCompat = {
                0x010100af, 0x010100c4, 0x01010126, 0x01010127,
                0x01010128, 0x7f01000b, 0x7f010045, 0x7f010046,
                0x7f010047
            };
            /**
              <p>This symbol is the offset where the {@link android.R.attr#baselineAligned}
              attribute's value can be found in the {@link #LinearLayoutCompat} array.
              @attr name android:baselineAligned
            */
            public static final int LinearLayoutCompat_android_baselineAligned = 2;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#baselineAlignedChildIndex}
              attribute's value can be found in the {@link #LinearLayoutCompat} array.
              @attr name android:baselineAlignedChildIndex
            */
            public static final int LinearLayoutCompat_android_baselineAlignedChildIndex = 3;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#gravity}
              attribute's value can be found in the {@link #LinearLayoutCompat} array.
              @attr name android:gravity
            */
            public static final int LinearLayoutCompat_android_gravity = 0;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#orientation}
              attribute's value can be found in the {@link #LinearLayoutCompat} array.
              @attr name android:orientation
            */
            public static final int LinearLayoutCompat_android_orientation = 1;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#weightSum}
              attribute's value can be found in the {@link #LinearLayoutCompat} array.
              @attr name android:weightSum
            */
            public static final int LinearLayoutCompat_android_weightSum = 4;
            /**
              <p>This symbol is the offset where the {@link attr#divider}
              attribute's value can be found in the {@link #LinearLayoutCompat} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:divider
            */
            public static final int LinearLayoutCompat_divider = 5;
            /**
              <p>This symbol is the offset where the {@link attr#dividerPadding}
              attribute's value can be found in the {@link #LinearLayoutCompat} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:dividerPadding
            */
            public static final int LinearLayoutCompat_dividerPadding = 8;
            /**
              <p>This symbol is the offset where the {@link attr#measureWithLargestChild}
              attribute's value can be found in the {@link #LinearLayoutCompat} array.


              <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:measureWithLargestChild
            */
            public static final int LinearLayoutCompat_measureWithLargestChild = 6;
            /**
              <p>This symbol is the offset where the {@link attr#showDividers}
              attribute's value can be found in the {@link #LinearLayoutCompat} array.


              <p>Must be one or more (separated by '|') of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>none</code></td><td>0</td><td></td></tr>
    <tr><td><code>beginning</code></td><td>1</td><td></td></tr>
    <tr><td><code>middle</code></td><td>2</td><td></td></tr>
    <tr><td><code>end</code></td><td>4</td><td></td></tr>
    </table>
              @attr name io.bitfountain.ashishpatel.sounddroid:showDividers
            */
            public static final int LinearLayoutCompat_showDividers = 7;
            /** Attributes that can be used with a LinearLayoutCompat_Layout.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #LinearLayoutCompat_Layout_android_layout_gravity android:layout_gravity}</code></td><td></td></tr>
               <tr><td><code>{@link #LinearLayoutCompat_Layout_android_layout_height android:layout_height}</code></td><td></td></tr>
               <tr><td><code>{@link #LinearLayoutCompat_Layout_android_layout_weight android:layout_weight}</code></td><td></td></tr>
               <tr><td><code>{@link #LinearLayoutCompat_Layout_android_layout_width android:layout_width}</code></td><td></td></tr>
               </table>
               @see #LinearLayoutCompat_Layout_android_layout_gravity
               @see #LinearLayoutCompat_Layout_android_layout_height
               @see #LinearLayoutCompat_Layout_android_layout_weight
               @see #LinearLayoutCompat_Layout_android_layout_width
             */
            public static final int[] LinearLayoutCompat_Layout = {
                0x010100b3, 0x010100f4, 0x010100f5, 0x01010181
            };
            /**
              <p>This symbol is the offset where the {@link android.R.attr#layout_gravity}
              attribute's value can be found in the {@link #LinearLayoutCompat_Layout} array.
              @attr name android:layout_gravity
            */
            public static final int LinearLayoutCompat_Layout_android_layout_gravity = 0;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#layout_height}
              attribute's value can be found in the {@link #LinearLayoutCompat_Layout} array.
              @attr name android:layout_height
            */
            public static final int LinearLayoutCompat_Layout_android_layout_height = 2;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#layout_weight}
              attribute's value can be found in the {@link #LinearLayoutCompat_Layout} array.
              @attr name android:layout_weight
            */
            public static final int LinearLayoutCompat_Layout_android_layout_weight = 3;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#layout_width}
              attribute's value can be found in the {@link #LinearLayoutCompat_Layout} array.
              @attr name android:layout_width
            */
            public static final int LinearLayoutCompat_Layout_android_layout_width = 1;
            /** Attributes that can be used with a ListPopupWindow.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #ListPopupWindow_android_dropDownHorizontalOffset android:dropDownHorizontalOffset}</code></td><td></td></tr>
               <tr><td><code>{@link #ListPopupWindow_android_dropDownVerticalOffset android:dropDownVerticalOffset}</code></td><td></td></tr>
               </table>
               @see #ListPopupWindow_android_dropDownHorizontalOffset
               @see #ListPopupWindow_android_dropDownVerticalOffset
             */
            public static final int[] ListPopupWindow = {
                0x010102ac, 0x010102ad
            };
            /**
              <p>This symbol is the offset where the {@link android.R.attr#dropDownHorizontalOffset}
              attribute's value can be found in the {@link #ListPopupWindow} array.
              @attr name android:dropDownHorizontalOffset
            */
            public static final int ListPopupWindow_android_dropDownHorizontalOffset = 0;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#dropDownVerticalOffset}
              attribute's value can be found in the {@link #ListPopupWindow} array.
              @attr name android:dropDownVerticalOffset
            */
            public static final int ListPopupWindow_android_dropDownVerticalOffset = 1;
            /** Attributes that can be used with a LoadingImageView.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #LoadingImageView_circleCrop io.bitfountain.ashishpatel.sounddroid:circleCrop}</code></td><td></td></tr>
               <tr><td><code>{@link #LoadingImageView_imageAspectRatio io.bitfountain.ashishpatel.sounddroid:imageAspectRatio}</code></td><td></td></tr>
               <tr><td><code>{@link #LoadingImageView_imageAspectRatioAdjust io.bitfountain.ashishpatel.sounddroid:imageAspectRatioAdjust}</code></td><td></td></tr>
               </table>
               @see #LoadingImageView_circleCrop
               @see #LoadingImageView_imageAspectRatio
               @see #LoadingImageView_imageAspectRatioAdjust
             */
            public static final int[] LoadingImageView = {
                0x7f010048, 0x7f010049, 0x7f01004a
            };
            /**
              <p>This symbol is the offset where the {@link attr#circleCrop}
              attribute's value can be found in the {@link #LoadingImageView} array.


              <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:circleCrop
            */
            public static final int LoadingImageView_circleCrop = 2;
            /**
              <p>This symbol is the offset where the {@link attr#imageAspectRatio}
              attribute's value can be found in the {@link #LoadingImageView} array.


              <p>Must be a floating point value, such as "<code>1.2</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:imageAspectRatio
            */
            public static final int LoadingImageView_imageAspectRatio = 1;
            /**
              <p>This symbol is the offset where the {@link attr#imageAspectRatioAdjust}
              attribute's value can be found in the {@link #LoadingImageView} array.


              <p>Must be one of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>none</code></td><td>0</td><td></td></tr>
    <tr><td><code>adjust_width</code></td><td>1</td><td></td></tr>
    <tr><td><code>adjust_height</code></td><td>2</td><td></td></tr>
    </table>
              @attr name io.bitfountain.ashishpatel.sounddroid:imageAspectRatioAdjust
            */
            public static final int LoadingImageView_imageAspectRatioAdjust = 0;
            /** Attributes that can be used with a MenuGroup.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #MenuGroup_android_checkableBehavior android:checkableBehavior}</code></td><td></td></tr>
               <tr><td><code>{@link #MenuGroup_android_enabled android:enabled}</code></td><td></td></tr>
               <tr><td><code>{@link #MenuGroup_android_id android:id}</code></td><td></td></tr>
               <tr><td><code>{@link #MenuGroup_android_menuCategory android:menuCategory}</code></td><td></td></tr>
               <tr><td><code>{@link #MenuGroup_android_orderInCategory android:orderInCategory}</code></td><td></td></tr>
               <tr><td><code>{@link #MenuGroup_android_visible android:visible}</code></td><td></td></tr>
               </table>
               @see #MenuGroup_android_checkableBehavior
               @see #MenuGroup_android_enabled
               @see #MenuGroup_android_id
               @see #MenuGroup_android_menuCategory
               @see #MenuGroup_android_orderInCategory
               @see #MenuGroup_android_visible
             */
            public static final int[] MenuGroup = {
                0x0101000e, 0x010100d0, 0x01010194, 0x010101de,
                0x010101df, 0x010101e0
            };
            /**
              <p>This symbol is the offset where the {@link android.R.attr#checkableBehavior}
              attribute's value can be found in the {@link #MenuGroup} array.
              @attr name android:checkableBehavior
            */
            public static final int MenuGroup_android_checkableBehavior = 5;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#enabled}
              attribute's value can be found in the {@link #MenuGroup} array.
              @attr name android:enabled
            */
            public static final int MenuGroup_android_enabled = 0;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#id}
              attribute's value can be found in the {@link #MenuGroup} array.
              @attr name android:id
            */
            public static final int MenuGroup_android_id = 1;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#menuCategory}
              attribute's value can be found in the {@link #MenuGroup} array.
              @attr name android:menuCategory
            */
            public static final int MenuGroup_android_menuCategory = 3;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#orderInCategory}
              attribute's value can be found in the {@link #MenuGroup} array.
              @attr name android:orderInCategory
            */
            public static final int MenuGroup_android_orderInCategory = 4;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#visible}
              attribute's value can be found in the {@link #MenuGroup} array.
              @attr name android:visible
            */
            public static final int MenuGroup_android_visible = 2;
            /** Attributes that can be used with a MenuItem.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #MenuItem_actionLayout io.bitfountain.ashishpatel.sounddroid:actionLayout}</code></td><td></td></tr>
               <tr><td><code>{@link #MenuItem_actionProviderClass io.bitfountain.ashishpatel.sounddroid:actionProviderClass}</code></td><td></td></tr>
               <tr><td><code>{@link #MenuItem_actionViewClass io.bitfountain.ashishpatel.sounddroid:actionViewClass}</code></td><td></td></tr>
               <tr><td><code>{@link #MenuItem_android_alphabeticShortcut android:alphabeticShortcut}</code></td><td></td></tr>
               <tr><td><code>{@link #MenuItem_android_checkable android:checkable}</code></td><td></td></tr>
               <tr><td><code>{@link #MenuItem_android_checked android:checked}</code></td><td></td></tr>
               <tr><td><code>{@link #MenuItem_android_enabled android:enabled}</code></td><td></td></tr>
               <tr><td><code>{@link #MenuItem_android_icon android:icon}</code></td><td></td></tr>
               <tr><td><code>{@link #MenuItem_android_id android:id}</code></td><td></td></tr>
               <tr><td><code>{@link #MenuItem_android_menuCategory android:menuCategory}</code></td><td></td></tr>
               <tr><td><code>{@link #MenuItem_android_numericShortcut android:numericShortcut}</code></td><td></td></tr>
               <tr><td><code>{@link #MenuItem_android_onClick android:onClick}</code></td><td></td></tr>
               <tr><td><code>{@link #MenuItem_android_orderInCategory android:orderInCategory}</code></td><td></td></tr>
               <tr><td><code>{@link #MenuItem_android_title android:title}</code></td><td></td></tr>
               <tr><td><code>{@link #MenuItem_android_titleCondensed android:titleCondensed}</code></td><td></td></tr>
               <tr><td><code>{@link #MenuItem_android_visible android:visible}</code></td><td></td></tr>
               <tr><td><code>{@link #MenuItem_showAsAction io.bitfountain.ashishpatel.sounddroid:showAsAction}</code></td><td></td></tr>
               </table>
               @see #MenuItem_actionLayout
               @see #MenuItem_actionProviderClass
               @see #MenuItem_actionViewClass
               @see #MenuItem_android_alphabeticShortcut
               @see #MenuItem_android_checkable
               @see #MenuItem_android_checked
               @see #MenuItem_android_enabled
               @see #MenuItem_android_icon
               @see #MenuItem_android_id
               @see #MenuItem_android_menuCategory
               @see #MenuItem_android_numericShortcut
               @see #MenuItem_android_onClick
               @see #MenuItem_android_orderInCategory
               @see #MenuItem_android_title
               @see #MenuItem_android_titleCondensed
               @see #MenuItem_android_visible
               @see #MenuItem_showAsAction
             */
            public static final int[] MenuItem = {
                0x01010002, 0x0101000e, 0x010100d0, 0x01010106,
                0x01010194, 0x010101de, 0x010101df, 0x010101e1,
                0x010101e2, 0x010101e3, 0x010101e4, 0x010101e5,
                0x0101026f, 0x7f01004b, 0x7f01004c, 0x7f01004d,
                0x7f01004e
            };
            /**
              <p>This symbol is the offset where the {@link attr#actionLayout}
              attribute's value can be found in the {@link #MenuItem} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionLayout
            */
            public static final int MenuItem_actionLayout = 14;
            /**
              <p>This symbol is the offset where the {@link attr#actionProviderClass}
              attribute's value can be found in the {@link #MenuItem} array.


              <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:actionProviderClass
            */
            public static final int MenuItem_actionProviderClass = 16;
            /**
              <p>This symbol is the offset where the {@link attr#actionViewClass}
              attribute's value can be found in the {@link #MenuItem} array.


              <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:actionViewClass
            */
            public static final int MenuItem_actionViewClass = 15;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#alphabeticShortcut}
              attribute's value can be found in the {@link #MenuItem} array.
              @attr name android:alphabeticShortcut
            */
            public static final int MenuItem_android_alphabeticShortcut = 9;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#checkable}
              attribute's value can be found in the {@link #MenuItem} array.
              @attr name android:checkable
            */
            public static final int MenuItem_android_checkable = 11;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#checked}
              attribute's value can be found in the {@link #MenuItem} array.
              @attr name android:checked
            */
            public static final int MenuItem_android_checked = 3;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#enabled}
              attribute's value can be found in the {@link #MenuItem} array.
              @attr name android:enabled
            */
            public static final int MenuItem_android_enabled = 1;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#icon}
              attribute's value can be found in the {@link #MenuItem} array.
              @attr name android:icon
            */
            public static final int MenuItem_android_icon = 0;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#id}
              attribute's value can be found in the {@link #MenuItem} array.
              @attr name android:id
            */
            public static final int MenuItem_android_id = 2;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#menuCategory}
              attribute's value can be found in the {@link #MenuItem} array.
              @attr name android:menuCategory
            */
            public static final int MenuItem_android_menuCategory = 5;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#numericShortcut}
              attribute's value can be found in the {@link #MenuItem} array.
              @attr name android:numericShortcut
            */
            public static final int MenuItem_android_numericShortcut = 10;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#onClick}
              attribute's value can be found in the {@link #MenuItem} array.
              @attr name android:onClick
            */
            public static final int MenuItem_android_onClick = 12;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#orderInCategory}
              attribute's value can be found in the {@link #MenuItem} array.
              @attr name android:orderInCategory
            */
            public static final int MenuItem_android_orderInCategory = 6;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#title}
              attribute's value can be found in the {@link #MenuItem} array.
              @attr name android:title
            */
            public static final int MenuItem_android_title = 7;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#titleCondensed}
              attribute's value can be found in the {@link #MenuItem} array.
              @attr name android:titleCondensed
            */
            public static final int MenuItem_android_titleCondensed = 8;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#visible}
              attribute's value can be found in the {@link #MenuItem} array.
              @attr name android:visible
            */
            public static final int MenuItem_android_visible = 4;
            /**
              <p>This symbol is the offset where the {@link attr#showAsAction}
              attribute's value can be found in the {@link #MenuItem} array.


              <p>Must be one or more (separated by '|') of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>never</code></td><td>0</td><td></td></tr>
    <tr><td><code>ifRoom</code></td><td>1</td><td></td></tr>
    <tr><td><code>always</code></td><td>2</td><td></td></tr>
    <tr><td><code>withText</code></td><td>4</td><td></td></tr>
    <tr><td><code>collapseActionView</code></td><td>8</td><td></td></tr>
    </table>
              @attr name io.bitfountain.ashishpatel.sounddroid:showAsAction
            */
            public static final int MenuItem_showAsAction = 13;
            /** Attributes that can be used with a MenuView.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #MenuView_android_headerBackground android:headerBackground}</code></td><td></td></tr>
               <tr><td><code>{@link #MenuView_android_horizontalDivider android:horizontalDivider}</code></td><td></td></tr>
               <tr><td><code>{@link #MenuView_android_itemBackground android:itemBackground}</code></td><td></td></tr>
               <tr><td><code>{@link #MenuView_android_itemIconDisabledAlpha android:itemIconDisabledAlpha}</code></td><td></td></tr>
               <tr><td><code>{@link #MenuView_android_itemTextAppearance android:itemTextAppearance}</code></td><td></td></tr>
               <tr><td><code>{@link #MenuView_android_verticalDivider android:verticalDivider}</code></td><td></td></tr>
               <tr><td><code>{@link #MenuView_android_windowAnimationStyle android:windowAnimationStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #MenuView_preserveIconSpacing io.bitfountain.ashishpatel.sounddroid:preserveIconSpacing}</code></td><td></td></tr>
               </table>
               @see #MenuView_android_headerBackground
               @see #MenuView_android_horizontalDivider
               @see #MenuView_android_itemBackground
               @see #MenuView_android_itemIconDisabledAlpha
               @see #MenuView_android_itemTextAppearance
               @see #MenuView_android_verticalDivider
               @see #MenuView_android_windowAnimationStyle
               @see #MenuView_preserveIconSpacing
             */
            public static final int[] MenuView = {
                0x010100ae, 0x0101012c, 0x0101012d, 0x0101012e,
                0x0101012f, 0x01010130, 0x01010131, 0x7f01004f
            };
            /**
              <p>This symbol is the offset where the {@link android.R.attr#headerBackground}
              attribute's value can be found in the {@link #MenuView} array.
              @attr name android:headerBackground
            */
            public static final int MenuView_android_headerBackground = 4;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#horizontalDivider}
              attribute's value can be found in the {@link #MenuView} array.
              @attr name android:horizontalDivider
            */
            public static final int MenuView_android_horizontalDivider = 2;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#itemBackground}
              attribute's value can be found in the {@link #MenuView} array.
              @attr name android:itemBackground
            */
            public static final int MenuView_android_itemBackground = 5;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#itemIconDisabledAlpha}
              attribute's value can be found in the {@link #MenuView} array.
              @attr name android:itemIconDisabledAlpha
            */
            public static final int MenuView_android_itemIconDisabledAlpha = 6;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#itemTextAppearance}
              attribute's value can be found in the {@link #MenuView} array.
              @attr name android:itemTextAppearance
            */
            public static final int MenuView_android_itemTextAppearance = 1;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#verticalDivider}
              attribute's value can be found in the {@link #MenuView} array.
              @attr name android:verticalDivider
            */
            public static final int MenuView_android_verticalDivider = 3;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#windowAnimationStyle}
              attribute's value can be found in the {@link #MenuView} array.
              @attr name android:windowAnimationStyle
            */
            public static final int MenuView_android_windowAnimationStyle = 0;
            /**
              <p>This symbol is the offset where the {@link attr#preserveIconSpacing}
              attribute's value can be found in the {@link #MenuView} array.


              <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:preserveIconSpacing
            */
            public static final int MenuView_preserveIconSpacing = 7;
            /** Attributes that can be used with a NavigationView.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #NavigationView_android_background android:background}</code></td><td></td></tr>
               <tr><td><code>{@link #NavigationView_android_fitsSystemWindows android:fitsSystemWindows}</code></td><td></td></tr>
               <tr><td><code>{@link #NavigationView_android_maxWidth android:maxWidth}</code></td><td></td></tr>
               <tr><td><code>{@link #NavigationView_elevation io.bitfountain.ashishpatel.sounddroid:elevation}</code></td><td></td></tr>
               <tr><td><code>{@link #NavigationView_headerLayout io.bitfountain.ashishpatel.sounddroid:headerLayout}</code></td><td></td></tr>
               <tr><td><code>{@link #NavigationView_itemBackground io.bitfountain.ashishpatel.sounddroid:itemBackground}</code></td><td></td></tr>
               <tr><td><code>{@link #NavigationView_itemIconTint io.bitfountain.ashishpatel.sounddroid:itemIconTint}</code></td><td></td></tr>
               <tr><td><code>{@link #NavigationView_itemTextColor io.bitfountain.ashishpatel.sounddroid:itemTextColor}</code></td><td></td></tr>
               <tr><td><code>{@link #NavigationView_menu io.bitfountain.ashishpatel.sounddroid:menu}</code></td><td></td></tr>
               </table>
               @see #NavigationView_android_background
               @see #NavigationView_android_fitsSystemWindows
               @see #NavigationView_android_maxWidth
               @see #NavigationView_elevation
               @see #NavigationView_headerLayout
               @see #NavigationView_itemBackground
               @see #NavigationView_itemIconTint
               @see #NavigationView_itemTextColor
               @see #NavigationView_menu
             */
            public static final int[] NavigationView = {
                0x010100d4, 0x010100dd, 0x0101011f, 0x7f01001a,
                0x7f010050, 0x7f010051, 0x7f010052, 0x7f010053,
                0x7f010054
            };
            /**
              <p>This symbol is the offset where the {@link android.R.attr#background}
              attribute's value can be found in the {@link #NavigationView} array.
              @attr name android:background
            */
            public static final int NavigationView_android_background = 0;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#fitsSystemWindows}
              attribute's value can be found in the {@link #NavigationView} array.
              @attr name android:fitsSystemWindows
            */
            public static final int NavigationView_android_fitsSystemWindows = 1;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#maxWidth}
              attribute's value can be found in the {@link #NavigationView} array.
              @attr name android:maxWidth
            */
            public static final int NavigationView_android_maxWidth = 2;
            /**
              <p>This symbol is the offset where the {@link attr#elevation}
              attribute's value can be found in the {@link #NavigationView} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:elevation
            */
            public static final int NavigationView_elevation = 3;
            /**
              <p>This symbol is the offset where the {@link attr#headerLayout}
              attribute's value can be found in the {@link #NavigationView} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:headerLayout
            */
            public static final int NavigationView_headerLayout = 8;
            /**
              <p>This symbol is the offset where the {@link attr#itemBackground}
              attribute's value can be found in the {@link #NavigationView} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:itemBackground
            */
            public static final int NavigationView_itemBackground = 7;
            /**
              <p>This symbol is the offset where the {@link attr#itemIconTint}
              attribute's value can be found in the {@link #NavigationView} array.


              <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:itemIconTint
            */
            public static final int NavigationView_itemIconTint = 5;
            /**
              <p>This symbol is the offset where the {@link attr#itemTextColor}
              attribute's value can be found in the {@link #NavigationView} array.


              <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:itemTextColor
            */
            public static final int NavigationView_itemTextColor = 6;
            /**
              <p>This symbol is the offset where the {@link attr#menu}
              attribute's value can be found in the {@link #NavigationView} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:menu
            */
            public static final int NavigationView_menu = 4;
            /** Attributes that can be used with a PopupWindow.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #PopupWindow_android_popupBackground android:popupBackground}</code></td><td></td></tr>
               <tr><td><code>{@link #PopupWindow_overlapAnchor io.bitfountain.ashishpatel.sounddroid:overlapAnchor}</code></td><td></td></tr>
               </table>
               @see #PopupWindow_android_popupBackground
               @see #PopupWindow_overlapAnchor
             */
            public static final int[] PopupWindow = {
                0x01010176, 0x7f010055
            };
            /**
              <p>This symbol is the offset where the {@link android.R.attr#popupBackground}
              attribute's value can be found in the {@link #PopupWindow} array.
              @attr name android:popupBackground
            */
            public static final int PopupWindow_android_popupBackground = 0;
            /**
              <p>This symbol is the offset where the {@link attr#overlapAnchor}
              attribute's value can be found in the {@link #PopupWindow} array.


              <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:overlapAnchor
            */
            public static final int PopupWindow_overlapAnchor = 1;
            /** Attributes that can be used with a PopupWindowBackgroundState.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #PopupWindowBackgroundState_state_above_anchor io.bitfountain.ashishpatel.sounddroid:state_above_anchor}</code></td><td></td></tr>
               </table>
               @see #PopupWindowBackgroundState_state_above_anchor
             */
            public static final int[] PopupWindowBackgroundState = {
                0x7f010056
            };
            /**
              <p>This symbol is the offset where the {@link attr#state_above_anchor}
              attribute's value can be found in the {@link #PopupWindowBackgroundState} array.


              <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:state_above_anchor
            */
            public static final int PopupWindowBackgroundState_state_above_anchor = 0;
            /** Attributes that can be used with a ScrimInsetsFrameLayout.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #ScrimInsetsFrameLayout_insetForeground io.bitfountain.ashishpatel.sounddroid:insetForeground}</code></td><td></td></tr>
               </table>
               @see #ScrimInsetsFrameLayout_insetForeground
             */
            public static final int[] ScrimInsetsFrameLayout = {
                0x7f010057
            };
            /**
              <p>This symbol is the offset where the {@link attr#insetForeground}
              attribute's value can be found in the {@link #ScrimInsetsFrameLayout} array.


              <p>May be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
    <p>May be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:insetForeground
            */
            public static final int ScrimInsetsFrameLayout_insetForeground = 0;
            /** Attributes that can be used with a ScrollingViewBehavior_Params.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #ScrollingViewBehavior_Params_behavior_overlapTop io.bitfountain.ashishpatel.sounddroid:behavior_overlapTop}</code></td><td></td></tr>
               </table>
               @see #ScrollingViewBehavior_Params_behavior_overlapTop
             */
            public static final int[] ScrollingViewBehavior_Params = {
                0x7f010058
            };
            /**
              <p>This symbol is the offset where the {@link attr#behavior_overlapTop}
              attribute's value can be found in the {@link #ScrollingViewBehavior_Params} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:behavior_overlapTop
            */
            public static final int ScrollingViewBehavior_Params_behavior_overlapTop = 0;
            /** Attributes that can be used with a SearchView.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #SearchView_android_focusable android:focusable}</code></td><td></td></tr>
               <tr><td><code>{@link #SearchView_android_imeOptions android:imeOptions}</code></td><td></td></tr>
               <tr><td><code>{@link #SearchView_android_inputType android:inputType}</code></td><td></td></tr>
               <tr><td><code>{@link #SearchView_android_maxWidth android:maxWidth}</code></td><td></td></tr>
               <tr><td><code>{@link #SearchView_closeIcon io.bitfountain.ashishpatel.sounddroid:closeIcon}</code></td><td></td></tr>
               <tr><td><code>{@link #SearchView_commitIcon io.bitfountain.ashishpatel.sounddroid:commitIcon}</code></td><td></td></tr>
               <tr><td><code>{@link #SearchView_defaultQueryHint io.bitfountain.ashishpatel.sounddroid:defaultQueryHint}</code></td><td></td></tr>
               <tr><td><code>{@link #SearchView_goIcon io.bitfountain.ashishpatel.sounddroid:goIcon}</code></td><td></td></tr>
               <tr><td><code>{@link #SearchView_iconifiedByDefault io.bitfountain.ashishpatel.sounddroid:iconifiedByDefault}</code></td><td></td></tr>
               <tr><td><code>{@link #SearchView_layout io.bitfountain.ashishpatel.sounddroid:layout}</code></td><td></td></tr>
               <tr><td><code>{@link #SearchView_queryBackground io.bitfountain.ashishpatel.sounddroid:queryBackground}</code></td><td></td></tr>
               <tr><td><code>{@link #SearchView_queryHint io.bitfountain.ashishpatel.sounddroid:queryHint}</code></td><td></td></tr>
               <tr><td><code>{@link #SearchView_searchHintIcon io.bitfountain.ashishpatel.sounddroid:searchHintIcon}</code></td><td></td></tr>
               <tr><td><code>{@link #SearchView_searchIcon io.bitfountain.ashishpatel.sounddroid:searchIcon}</code></td><td></td></tr>
               <tr><td><code>{@link #SearchView_submitBackground io.bitfountain.ashishpatel.sounddroid:submitBackground}</code></td><td></td></tr>
               <tr><td><code>{@link #SearchView_suggestionRowLayout io.bitfountain.ashishpatel.sounddroid:suggestionRowLayout}</code></td><td></td></tr>
               <tr><td><code>{@link #SearchView_voiceIcon io.bitfountain.ashishpatel.sounddroid:voiceIcon}</code></td><td></td></tr>
               </table>
               @see #SearchView_android_focusable
               @see #SearchView_android_imeOptions
               @see #SearchView_android_inputType
               @see #SearchView_android_maxWidth
               @see #SearchView_closeIcon
               @see #SearchView_commitIcon
               @see #SearchView_defaultQueryHint
               @see #SearchView_goIcon
               @see #SearchView_iconifiedByDefault
               @see #SearchView_layout
               @see #SearchView_queryBackground
               @see #SearchView_queryHint
               @see #SearchView_searchHintIcon
               @see #SearchView_searchIcon
               @see #SearchView_submitBackground
               @see #SearchView_suggestionRowLayout
               @see #SearchView_voiceIcon
             */
            public static final int[] SearchView = {
                0x010100da, 0x0101011f, 0x01010220, 0x01010264,
                0x7f010059, 0x7f01005a, 0x7f01005b, 0x7f01005c,
                0x7f01005d, 0x7f01005e, 0x7f01005f, 0x7f010060,
                0x7f010061, 0x7f010062, 0x7f010063, 0x7f010064,
                0x7f010065
            };
            /**
              <p>This symbol is the offset where the {@link android.R.attr#focusable}
              attribute's value can be found in the {@link #SearchView} array.
              @attr name android:focusable
            */
            public static final int SearchView_android_focusable = 0;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#imeOptions}
              attribute's value can be found in the {@link #SearchView} array.
              @attr name android:imeOptions
            */
            public static final int SearchView_android_imeOptions = 3;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#inputType}
              attribute's value can be found in the {@link #SearchView} array.
              @attr name android:inputType
            */
            public static final int SearchView_android_inputType = 2;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#maxWidth}
              attribute's value can be found in the {@link #SearchView} array.
              @attr name android:maxWidth
            */
            public static final int SearchView_android_maxWidth = 1;
            /**
              <p>This symbol is the offset where the {@link attr#closeIcon}
              attribute's value can be found in the {@link #SearchView} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:closeIcon
            */
            public static final int SearchView_closeIcon = 8;
            /**
              <p>This symbol is the offset where the {@link attr#commitIcon}
              attribute's value can be found in the {@link #SearchView} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:commitIcon
            */
            public static final int SearchView_commitIcon = 13;
            /**
              <p>This symbol is the offset where the {@link attr#defaultQueryHint}
              attribute's value can be found in the {@link #SearchView} array.


              <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:defaultQueryHint
            */
            public static final int SearchView_defaultQueryHint = 7;
            /**
              <p>This symbol is the offset where the {@link attr#goIcon}
              attribute's value can be found in the {@link #SearchView} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:goIcon
            */
            public static final int SearchView_goIcon = 9;
            /**
              <p>This symbol is the offset where the {@link attr#iconifiedByDefault}
              attribute's value can be found in the {@link #SearchView} array.


              <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:iconifiedByDefault
            */
            public static final int SearchView_iconifiedByDefault = 5;
            /**
              <p>This symbol is the offset where the {@link attr#layout}
              attribute's value can be found in the {@link #SearchView} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:layout
            */
            public static final int SearchView_layout = 4;
            /**
              <p>This symbol is the offset where the {@link attr#queryBackground}
              attribute's value can be found in the {@link #SearchView} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:queryBackground
            */
            public static final int SearchView_queryBackground = 15;
            /**
              <p>This symbol is the offset where the {@link attr#queryHint}
              attribute's value can be found in the {@link #SearchView} array.


              <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:queryHint
            */
            public static final int SearchView_queryHint = 6;
            /**
              <p>This symbol is the offset where the {@link attr#searchHintIcon}
              attribute's value can be found in the {@link #SearchView} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:searchHintIcon
            */
            public static final int SearchView_searchHintIcon = 11;
            /**
              <p>This symbol is the offset where the {@link attr#searchIcon}
              attribute's value can be found in the {@link #SearchView} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:searchIcon
            */
            public static final int SearchView_searchIcon = 10;
            /**
              <p>This symbol is the offset where the {@link attr#submitBackground}
              attribute's value can be found in the {@link #SearchView} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:submitBackground
            */
            public static final int SearchView_submitBackground = 16;
            /**
              <p>This symbol is the offset where the {@link attr#suggestionRowLayout}
              attribute's value can be found in the {@link #SearchView} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:suggestionRowLayout
            */
            public static final int SearchView_suggestionRowLayout = 14;
            /**
              <p>This symbol is the offset where the {@link attr#voiceIcon}
              attribute's value can be found in the {@link #SearchView} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:voiceIcon
            */
            public static final int SearchView_voiceIcon = 12;
            /** Attributes that can be used with a SnackbarLayout.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #SnackbarLayout_android_maxWidth android:maxWidth}</code></td><td></td></tr>
               <tr><td><code>{@link #SnackbarLayout_elevation io.bitfountain.ashishpatel.sounddroid:elevation}</code></td><td></td></tr>
               <tr><td><code>{@link #SnackbarLayout_maxActionInlineWidth io.bitfountain.ashishpatel.sounddroid:maxActionInlineWidth}</code></td><td></td></tr>
               </table>
               @see #SnackbarLayout_android_maxWidth
               @see #SnackbarLayout_elevation
               @see #SnackbarLayout_maxActionInlineWidth
             */
            public static final int[] SnackbarLayout = {
                0x0101011f, 0x7f01001a, 0x7f010066
            };
            /**
              <p>This symbol is the offset where the {@link android.R.attr#maxWidth}
              attribute's value can be found in the {@link #SnackbarLayout} array.
              @attr name android:maxWidth
            */
            public static final int SnackbarLayout_android_maxWidth = 0;
            /**
              <p>This symbol is the offset where the {@link attr#elevation}
              attribute's value can be found in the {@link #SnackbarLayout} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:elevation
            */
            public static final int SnackbarLayout_elevation = 1;
            /**
              <p>This symbol is the offset where the {@link attr#maxActionInlineWidth}
              attribute's value can be found in the {@link #SnackbarLayout} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:maxActionInlineWidth
            */
            public static final int SnackbarLayout_maxActionInlineWidth = 2;
            /** Attributes that can be used with a Spinner.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #Spinner_android_background android:background}</code></td><td></td></tr>
               <tr><td><code>{@link #Spinner_android_dropDownHorizontalOffset android:dropDownHorizontalOffset}</code></td><td></td></tr>
               <tr><td><code>{@link #Spinner_android_dropDownSelector android:dropDownSelector}</code></td><td></td></tr>
               <tr><td><code>{@link #Spinner_android_dropDownVerticalOffset android:dropDownVerticalOffset}</code></td><td></td></tr>
               <tr><td><code>{@link #Spinner_android_dropDownWidth android:dropDownWidth}</code></td><td></td></tr>
               <tr><td><code>{@link #Spinner_android_gravity android:gravity}</code></td><td></td></tr>
               <tr><td><code>{@link #Spinner_android_popupBackground android:popupBackground}</code></td><td></td></tr>
               <tr><td><code>{@link #Spinner_disableChildrenWhenDisabled io.bitfountain.ashishpatel.sounddroid:disableChildrenWhenDisabled}</code></td><td></td></tr>
               <tr><td><code>{@link #Spinner_popupPromptView io.bitfountain.ashishpatel.sounddroid:popupPromptView}</code></td><td></td></tr>
               <tr><td><code>{@link #Spinner_prompt io.bitfountain.ashishpatel.sounddroid:prompt}</code></td><td></td></tr>
               <tr><td><code>{@link #Spinner_spinnerMode io.bitfountain.ashishpatel.sounddroid:spinnerMode}</code></td><td></td></tr>
               </table>
               @see #Spinner_android_background
               @see #Spinner_android_dropDownHorizontalOffset
               @see #Spinner_android_dropDownSelector
               @see #Spinner_android_dropDownVerticalOffset
               @see #Spinner_android_dropDownWidth
               @see #Spinner_android_gravity
               @see #Spinner_android_popupBackground
               @see #Spinner_disableChildrenWhenDisabled
               @see #Spinner_popupPromptView
               @see #Spinner_prompt
               @see #Spinner_spinnerMode
             */
            public static final int[] Spinner = {
                0x010100af, 0x010100d4, 0x01010175, 0x01010176,
                0x01010262, 0x010102ac, 0x010102ad, 0x7f010067,
                0x7f010068, 0x7f010069, 0x7f01006a
            };
            /**
              <p>This symbol is the offset where the {@link android.R.attr#background}
              attribute's value can be found in the {@link #Spinner} array.
              @attr name android:background
            */
            public static final int Spinner_android_background = 1;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#dropDownHorizontalOffset}
              attribute's value can be found in the {@link #Spinner} array.
              @attr name android:dropDownHorizontalOffset
            */
            public static final int Spinner_android_dropDownHorizontalOffset = 5;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#dropDownSelector}
              attribute's value can be found in the {@link #Spinner} array.
              @attr name android:dropDownSelector
            */
            public static final int Spinner_android_dropDownSelector = 2;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#dropDownVerticalOffset}
              attribute's value can be found in the {@link #Spinner} array.
              @attr name android:dropDownVerticalOffset
            */
            public static final int Spinner_android_dropDownVerticalOffset = 6;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#dropDownWidth}
              attribute's value can be found in the {@link #Spinner} array.
              @attr name android:dropDownWidth
            */
            public static final int Spinner_android_dropDownWidth = 4;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#gravity}
              attribute's value can be found in the {@link #Spinner} array.
              @attr name android:gravity
            */
            public static final int Spinner_android_gravity = 0;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#popupBackground}
              attribute's value can be found in the {@link #Spinner} array.
              @attr name android:popupBackground
            */
            public static final int Spinner_android_popupBackground = 3;
            /**
              <p>This symbol is the offset where the {@link attr#disableChildrenWhenDisabled}
              attribute's value can be found in the {@link #Spinner} array.


              <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:disableChildrenWhenDisabled
            */
            public static final int Spinner_disableChildrenWhenDisabled = 10;
            /**
              <p>This symbol is the offset where the {@link attr#popupPromptView}
              attribute's value can be found in the {@link #Spinner} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:popupPromptView
            */
            public static final int Spinner_popupPromptView = 9;
            /**
              <p>This symbol is the offset where the {@link attr#prompt}
              attribute's value can be found in the {@link #Spinner} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:prompt
            */
            public static final int Spinner_prompt = 7;
            /**
              <p>This symbol is the offset where the {@link attr#spinnerMode}
              attribute's value can be found in the {@link #Spinner} array.


              <p>Must be one of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>dialog</code></td><td>0</td><td></td></tr>
    <tr><td><code>dropdown</code></td><td>1</td><td></td></tr>
    </table>
              @attr name io.bitfountain.ashishpatel.sounddroid:spinnerMode
            */
            public static final int Spinner_spinnerMode = 8;
            /** Attributes that can be used with a SwitchCompat.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #SwitchCompat_android_textOff android:textOff}</code></td><td></td></tr>
               <tr><td><code>{@link #SwitchCompat_android_textOn android:textOn}</code></td><td></td></tr>
               <tr><td><code>{@link #SwitchCompat_android_thumb android:thumb}</code></td><td></td></tr>
               <tr><td><code>{@link #SwitchCompat_showText io.bitfountain.ashishpatel.sounddroid:showText}</code></td><td></td></tr>
               <tr><td><code>{@link #SwitchCompat_splitTrack io.bitfountain.ashishpatel.sounddroid:splitTrack}</code></td><td></td></tr>
               <tr><td><code>{@link #SwitchCompat_switchMinWidth io.bitfountain.ashishpatel.sounddroid:switchMinWidth}</code></td><td></td></tr>
               <tr><td><code>{@link #SwitchCompat_switchPadding io.bitfountain.ashishpatel.sounddroid:switchPadding}</code></td><td></td></tr>
               <tr><td><code>{@link #SwitchCompat_switchTextAppearance io.bitfountain.ashishpatel.sounddroid:switchTextAppearance}</code></td><td></td></tr>
               <tr><td><code>{@link #SwitchCompat_thumbTextPadding io.bitfountain.ashishpatel.sounddroid:thumbTextPadding}</code></td><td></td></tr>
               <tr><td><code>{@link #SwitchCompat_track io.bitfountain.ashishpatel.sounddroid:track}</code></td><td></td></tr>
               </table>
               @see #SwitchCompat_android_textOff
               @see #SwitchCompat_android_textOn
               @see #SwitchCompat_android_thumb
               @see #SwitchCompat_showText
               @see #SwitchCompat_splitTrack
               @see #SwitchCompat_switchMinWidth
               @see #SwitchCompat_switchPadding
               @see #SwitchCompat_switchTextAppearance
               @see #SwitchCompat_thumbTextPadding
               @see #SwitchCompat_track
             */
            public static final int[] SwitchCompat = {
                0x01010124, 0x01010125, 0x01010142, 0x7f01006b,
                0x7f01006c, 0x7f01006d, 0x7f01006e, 0x7f01006f,
                0x7f010070, 0x7f010071
            };
            /**
              <p>This symbol is the offset where the {@link android.R.attr#textOff}
              attribute's value can be found in the {@link #SwitchCompat} array.
              @attr name android:textOff
            */
            public static final int SwitchCompat_android_textOff = 1;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#textOn}
              attribute's value can be found in the {@link #SwitchCompat} array.
              @attr name android:textOn
            */
            public static final int SwitchCompat_android_textOn = 0;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#thumb}
              attribute's value can be found in the {@link #SwitchCompat} array.
              @attr name android:thumb
            */
            public static final int SwitchCompat_android_thumb = 2;
            /**
              <p>This symbol is the offset where the {@link attr#showText}
              attribute's value can be found in the {@link #SwitchCompat} array.


              <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:showText
            */
            public static final int SwitchCompat_showText = 9;
            /**
              <p>This symbol is the offset where the {@link attr#splitTrack}
              attribute's value can be found in the {@link #SwitchCompat} array.


              <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:splitTrack
            */
            public static final int SwitchCompat_splitTrack = 8;
            /**
              <p>This symbol is the offset where the {@link attr#switchMinWidth}
              attribute's value can be found in the {@link #SwitchCompat} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:switchMinWidth
            */
            public static final int SwitchCompat_switchMinWidth = 6;
            /**
              <p>This symbol is the offset where the {@link attr#switchPadding}
              attribute's value can be found in the {@link #SwitchCompat} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:switchPadding
            */
            public static final int SwitchCompat_switchPadding = 7;
            /**
              <p>This symbol is the offset where the {@link attr#switchTextAppearance}
              attribute's value can be found in the {@link #SwitchCompat} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:switchTextAppearance
            */
            public static final int SwitchCompat_switchTextAppearance = 5;
            /**
              <p>This symbol is the offset where the {@link attr#thumbTextPadding}
              attribute's value can be found in the {@link #SwitchCompat} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:thumbTextPadding
            */
            public static final int SwitchCompat_thumbTextPadding = 4;
            /**
              <p>This symbol is the offset where the {@link attr#track}
              attribute's value can be found in the {@link #SwitchCompat} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:track
            */
            public static final int SwitchCompat_track = 3;
            /** Attributes that can be used with a TabLayout.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #TabLayout_tabBackground io.bitfountain.ashishpatel.sounddroid:tabBackground}</code></td><td></td></tr>
               <tr><td><code>{@link #TabLayout_tabContentStart io.bitfountain.ashishpatel.sounddroid:tabContentStart}</code></td><td></td></tr>
               <tr><td><code>{@link #TabLayout_tabGravity io.bitfountain.ashishpatel.sounddroid:tabGravity}</code></td><td></td></tr>
               <tr><td><code>{@link #TabLayout_tabIndicatorColor io.bitfountain.ashishpatel.sounddroid:tabIndicatorColor}</code></td><td></td></tr>
               <tr><td><code>{@link #TabLayout_tabIndicatorHeight io.bitfountain.ashishpatel.sounddroid:tabIndicatorHeight}</code></td><td></td></tr>
               <tr><td><code>{@link #TabLayout_tabMaxWidth io.bitfountain.ashishpatel.sounddroid:tabMaxWidth}</code></td><td></td></tr>
               <tr><td><code>{@link #TabLayout_tabMinWidth io.bitfountain.ashishpatel.sounddroid:tabMinWidth}</code></td><td></td></tr>
               <tr><td><code>{@link #TabLayout_tabMode io.bitfountain.ashishpatel.sounddroid:tabMode}</code></td><td></td></tr>
               <tr><td><code>{@link #TabLayout_tabPadding io.bitfountain.ashishpatel.sounddroid:tabPadding}</code></td><td></td></tr>
               <tr><td><code>{@link #TabLayout_tabPaddingBottom io.bitfountain.ashishpatel.sounddroid:tabPaddingBottom}</code></td><td></td></tr>
               <tr><td><code>{@link #TabLayout_tabPaddingEnd io.bitfountain.ashishpatel.sounddroid:tabPaddingEnd}</code></td><td></td></tr>
               <tr><td><code>{@link #TabLayout_tabPaddingStart io.bitfountain.ashishpatel.sounddroid:tabPaddingStart}</code></td><td></td></tr>
               <tr><td><code>{@link #TabLayout_tabPaddingTop io.bitfountain.ashishpatel.sounddroid:tabPaddingTop}</code></td><td></td></tr>
               <tr><td><code>{@link #TabLayout_tabSelectedTextColor io.bitfountain.ashishpatel.sounddroid:tabSelectedTextColor}</code></td><td></td></tr>
               <tr><td><code>{@link #TabLayout_tabTextAppearance io.bitfountain.ashishpatel.sounddroid:tabTextAppearance}</code></td><td></td></tr>
               <tr><td><code>{@link #TabLayout_tabTextColor io.bitfountain.ashishpatel.sounddroid:tabTextColor}</code></td><td></td></tr>
               </table>
               @see #TabLayout_tabBackground
               @see #TabLayout_tabContentStart
               @see #TabLayout_tabGravity
               @see #TabLayout_tabIndicatorColor
               @see #TabLayout_tabIndicatorHeight
               @see #TabLayout_tabMaxWidth
               @see #TabLayout_tabMinWidth
               @see #TabLayout_tabMode
               @see #TabLayout_tabPadding
               @see #TabLayout_tabPaddingBottom
               @see #TabLayout_tabPaddingEnd
               @see #TabLayout_tabPaddingStart
               @see #TabLayout_tabPaddingTop
               @see #TabLayout_tabSelectedTextColor
               @see #TabLayout_tabTextAppearance
               @see #TabLayout_tabTextColor
             */
            public static final int[] TabLayout = {
                0x7f010072, 0x7f010073, 0x7f010074, 0x7f010075,
                0x7f010076, 0x7f010077, 0x7f010078, 0x7f010079,
                0x7f01007a, 0x7f01007b, 0x7f01007c, 0x7f01007d,
                0x7f01007e, 0x7f01007f, 0x7f010080, 0x7f010081
            };
            /**
              <p>This symbol is the offset where the {@link attr#tabBackground}
              attribute's value can be found in the {@link #TabLayout} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:tabBackground
            */
            public static final int TabLayout_tabBackground = 3;
            /**
              <p>This symbol is the offset where the {@link attr#tabContentStart}
              attribute's value can be found in the {@link #TabLayout} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:tabContentStart
            */
            public static final int TabLayout_tabContentStart = 2;
            /**
              <p>This symbol is the offset where the {@link attr#tabGravity}
              attribute's value can be found in the {@link #TabLayout} array.


              <p>Must be one of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>fill</code></td><td>0</td><td></td></tr>
    <tr><td><code>center</code></td><td>1</td><td></td></tr>
    </table>
              @attr name io.bitfountain.ashishpatel.sounddroid:tabGravity
            */
            public static final int TabLayout_tabGravity = 5;
            /**
              <p>This symbol is the offset where the {@link attr#tabIndicatorColor}
              attribute's value can be found in the {@link #TabLayout} array.


              <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:tabIndicatorColor
            */
            public static final int TabLayout_tabIndicatorColor = 0;
            /**
              <p>This symbol is the offset where the {@link attr#tabIndicatorHeight}
              attribute's value can be found in the {@link #TabLayout} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:tabIndicatorHeight
            */
            public static final int TabLayout_tabIndicatorHeight = 1;
            /**
              <p>This symbol is the offset where the {@link attr#tabMaxWidth}
              attribute's value can be found in the {@link #TabLayout} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:tabMaxWidth
            */
            public static final int TabLayout_tabMaxWidth = 7;
            /**
              <p>This symbol is the offset where the {@link attr#tabMinWidth}
              attribute's value can be found in the {@link #TabLayout} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:tabMinWidth
            */
            public static final int TabLayout_tabMinWidth = 6;
            /**
              <p>This symbol is the offset where the {@link attr#tabMode}
              attribute's value can be found in the {@link #TabLayout} array.


              <p>Must be one of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>scrollable</code></td><td>0</td><td></td></tr>
    <tr><td><code>fixed</code></td><td>1</td><td></td></tr>
    </table>
              @attr name io.bitfountain.ashishpatel.sounddroid:tabMode
            */
            public static final int TabLayout_tabMode = 4;
            /**
              <p>This symbol is the offset where the {@link attr#tabPadding}
              attribute's value can be found in the {@link #TabLayout} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:tabPadding
            */
            public static final int TabLayout_tabPadding = 15;
            /**
              <p>This symbol is the offset where the {@link attr#tabPaddingBottom}
              attribute's value can be found in the {@link #TabLayout} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:tabPaddingBottom
            */
            public static final int TabLayout_tabPaddingBottom = 14;
            /**
              <p>This symbol is the offset where the {@link attr#tabPaddingEnd}
              attribute's value can be found in the {@link #TabLayout} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:tabPaddingEnd
            */
            public static final int TabLayout_tabPaddingEnd = 13;
            /**
              <p>This symbol is the offset where the {@link attr#tabPaddingStart}
              attribute's value can be found in the {@link #TabLayout} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:tabPaddingStart
            */
            public static final int TabLayout_tabPaddingStart = 11;
            /**
              <p>This symbol is the offset where the {@link attr#tabPaddingTop}
              attribute's value can be found in the {@link #TabLayout} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:tabPaddingTop
            */
            public static final int TabLayout_tabPaddingTop = 12;
            /**
              <p>This symbol is the offset where the {@link attr#tabSelectedTextColor}
              attribute's value can be found in the {@link #TabLayout} array.


              <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:tabSelectedTextColor
            */
            public static final int TabLayout_tabSelectedTextColor = 10;
            /**
              <p>This symbol is the offset where the {@link attr#tabTextAppearance}
              attribute's value can be found in the {@link #TabLayout} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:tabTextAppearance
            */
            public static final int TabLayout_tabTextAppearance = 8;
            /**
              <p>This symbol is the offset where the {@link attr#tabTextColor}
              attribute's value can be found in the {@link #TabLayout} array.


              <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:tabTextColor
            */
            public static final int TabLayout_tabTextColor = 9;
            /** Attributes that can be used with a TextAppearance.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #TextAppearance_android_textColor android:textColor}</code></td><td></td></tr>
               <tr><td><code>{@link #TextAppearance_android_textSize android:textSize}</code></td><td></td></tr>
               <tr><td><code>{@link #TextAppearance_android_textStyle android:textStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #TextAppearance_android_typeface android:typeface}</code></td><td></td></tr>
               <tr><td><code>{@link #TextAppearance_textAllCaps io.bitfountain.ashishpatel.sounddroid:textAllCaps}</code></td><td></td></tr>
               </table>
               @see #TextAppearance_android_textColor
               @see #TextAppearance_android_textSize
               @see #TextAppearance_android_textStyle
               @see #TextAppearance_android_typeface
               @see #TextAppearance_textAllCaps
             */
            public static final int[] TextAppearance = {
                0x01010095, 0x01010096, 0x01010097, 0x01010098,
                0x7f010026
            };
            /**
              <p>This symbol is the offset where the {@link android.R.attr#textColor}
              attribute's value can be found in the {@link #TextAppearance} array.
              @attr name android:textColor
            */
            public static final int TextAppearance_android_textColor = 3;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#textSize}
              attribute's value can be found in the {@link #TextAppearance} array.
              @attr name android:textSize
            */
            public static final int TextAppearance_android_textSize = 0;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#textStyle}
              attribute's value can be found in the {@link #TextAppearance} array.
              @attr name android:textStyle
            */
            public static final int TextAppearance_android_textStyle = 2;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#typeface}
              attribute's value can be found in the {@link #TextAppearance} array.
              @attr name android:typeface
            */
            public static final int TextAppearance_android_typeface = 1;
            /**
              <p>This symbol is the offset where the {@link attr#textAllCaps}
              attribute's value can be found in the {@link #TextAppearance} array.


              <p>May be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
    <p>May be a boolean value, either "<code>true</code>" or "<code>false</code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:textAllCaps
            */
            public static final int TextAppearance_textAllCaps = 4;
            /** Attributes that can be used with a TextInputLayout.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #TextInputLayout_android_hint android:hint}</code></td><td></td></tr>
               <tr><td><code>{@link #TextInputLayout_errorEnabled io.bitfountain.ashishpatel.sounddroid:errorEnabled}</code></td><td></td></tr>
               <tr><td><code>{@link #TextInputLayout_errorTextAppearance io.bitfountain.ashishpatel.sounddroid:errorTextAppearance}</code></td><td></td></tr>
               <tr><td><code>{@link #TextInputLayout_hintTextAppearance io.bitfountain.ashishpatel.sounddroid:hintTextAppearance}</code></td><td></td></tr>
               </table>
               @see #TextInputLayout_android_hint
               @see #TextInputLayout_errorEnabled
               @see #TextInputLayout_errorTextAppearance
               @see #TextInputLayout_hintTextAppearance
             */
            public static final int[] TextInputLayout = {
                0x01010150, 0x7f010082, 0x7f010083, 0x7f010084
            };
            /**
              <p>This symbol is the offset where the {@link android.R.attr#hint}
              attribute's value can be found in the {@link #TextInputLayout} array.
              @attr name android:hint
            */
            public static final int TextInputLayout_android_hint = 0;
            /**
              <p>This symbol is the offset where the {@link attr#errorEnabled}
              attribute's value can be found in the {@link #TextInputLayout} array.


              <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:errorEnabled
            */
            public static final int TextInputLayout_errorEnabled = 2;
            /**
              <p>This symbol is the offset where the {@link attr#errorTextAppearance}
              attribute's value can be found in the {@link #TextInputLayout} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:errorTextAppearance
            */
            public static final int TextInputLayout_errorTextAppearance = 3;
            /**
              <p>This symbol is the offset where the {@link attr#hintTextAppearance}
              attribute's value can be found in the {@link #TextInputLayout} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:hintTextAppearance
            */
            public static final int TextInputLayout_hintTextAppearance = 1;
            /** Attributes that can be used with a Theme.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #Theme_actionBarDivider io.bitfountain.ashishpatel.sounddroid:actionBarDivider}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionBarItemBackground io.bitfountain.ashishpatel.sounddroid:actionBarItemBackground}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionBarPopupTheme io.bitfountain.ashishpatel.sounddroid:actionBarPopupTheme}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionBarSize io.bitfountain.ashishpatel.sounddroid:actionBarSize}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionBarSplitStyle io.bitfountain.ashishpatel.sounddroid:actionBarSplitStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionBarStyle io.bitfountain.ashishpatel.sounddroid:actionBarStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionBarTabBarStyle io.bitfountain.ashishpatel.sounddroid:actionBarTabBarStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionBarTabStyle io.bitfountain.ashishpatel.sounddroid:actionBarTabStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionBarTabTextStyle io.bitfountain.ashishpatel.sounddroid:actionBarTabTextStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionBarTheme io.bitfountain.ashishpatel.sounddroid:actionBarTheme}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionBarWidgetTheme io.bitfountain.ashishpatel.sounddroid:actionBarWidgetTheme}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionButtonStyle io.bitfountain.ashishpatel.sounddroid:actionButtonStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionDropDownStyle io.bitfountain.ashishpatel.sounddroid:actionDropDownStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionMenuTextAppearance io.bitfountain.ashishpatel.sounddroid:actionMenuTextAppearance}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionMenuTextColor io.bitfountain.ashishpatel.sounddroid:actionMenuTextColor}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionModeBackground io.bitfountain.ashishpatel.sounddroid:actionModeBackground}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionModeCloseButtonStyle io.bitfountain.ashishpatel.sounddroid:actionModeCloseButtonStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionModeCloseDrawable io.bitfountain.ashishpatel.sounddroid:actionModeCloseDrawable}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionModeCopyDrawable io.bitfountain.ashishpatel.sounddroid:actionModeCopyDrawable}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionModeCutDrawable io.bitfountain.ashishpatel.sounddroid:actionModeCutDrawable}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionModeFindDrawable io.bitfountain.ashishpatel.sounddroid:actionModeFindDrawable}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionModePasteDrawable io.bitfountain.ashishpatel.sounddroid:actionModePasteDrawable}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionModePopupWindowStyle io.bitfountain.ashishpatel.sounddroid:actionModePopupWindowStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionModeSelectAllDrawable io.bitfountain.ashishpatel.sounddroid:actionModeSelectAllDrawable}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionModeShareDrawable io.bitfountain.ashishpatel.sounddroid:actionModeShareDrawable}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionModeSplitBackground io.bitfountain.ashishpatel.sounddroid:actionModeSplitBackground}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionModeStyle io.bitfountain.ashishpatel.sounddroid:actionModeStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionModeWebSearchDrawable io.bitfountain.ashishpatel.sounddroid:actionModeWebSearchDrawable}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionOverflowButtonStyle io.bitfountain.ashishpatel.sounddroid:actionOverflowButtonStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_actionOverflowMenuStyle io.bitfountain.ashishpatel.sounddroid:actionOverflowMenuStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_activityChooserViewStyle io.bitfountain.ashishpatel.sounddroid:activityChooserViewStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_alertDialogButtonGroupStyle io.bitfountain.ashishpatel.sounddroid:alertDialogButtonGroupStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_alertDialogCenterButtons io.bitfountain.ashishpatel.sounddroid:alertDialogCenterButtons}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_alertDialogStyle io.bitfountain.ashishpatel.sounddroid:alertDialogStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_alertDialogTheme io.bitfountain.ashishpatel.sounddroid:alertDialogTheme}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_android_windowAnimationStyle android:windowAnimationStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_android_windowIsFloating android:windowIsFloating}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_autoCompleteTextViewStyle io.bitfountain.ashishpatel.sounddroid:autoCompleteTextViewStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_borderlessButtonStyle io.bitfountain.ashishpatel.sounddroid:borderlessButtonStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_buttonBarButtonStyle io.bitfountain.ashishpatel.sounddroid:buttonBarButtonStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_buttonBarNegativeButtonStyle io.bitfountain.ashishpatel.sounddroid:buttonBarNegativeButtonStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_buttonBarNeutralButtonStyle io.bitfountain.ashishpatel.sounddroid:buttonBarNeutralButtonStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_buttonBarPositiveButtonStyle io.bitfountain.ashishpatel.sounddroid:buttonBarPositiveButtonStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_buttonBarStyle io.bitfountain.ashishpatel.sounddroid:buttonBarStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_buttonStyle io.bitfountain.ashishpatel.sounddroid:buttonStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_buttonStyleSmall io.bitfountain.ashishpatel.sounddroid:buttonStyleSmall}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_checkboxStyle io.bitfountain.ashishpatel.sounddroid:checkboxStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_checkedTextViewStyle io.bitfountain.ashishpatel.sounddroid:checkedTextViewStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_colorAccent io.bitfountain.ashishpatel.sounddroid:colorAccent}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_colorButtonNormal io.bitfountain.ashishpatel.sounddroid:colorButtonNormal}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_colorControlActivated io.bitfountain.ashishpatel.sounddroid:colorControlActivated}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_colorControlHighlight io.bitfountain.ashishpatel.sounddroid:colorControlHighlight}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_colorControlNormal io.bitfountain.ashishpatel.sounddroid:colorControlNormal}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_colorPrimary io.bitfountain.ashishpatel.sounddroid:colorPrimary}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_colorPrimaryDark io.bitfountain.ashishpatel.sounddroid:colorPrimaryDark}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_colorSwitchThumbNormal io.bitfountain.ashishpatel.sounddroid:colorSwitchThumbNormal}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_dialogPreferredPadding io.bitfountain.ashishpatel.sounddroid:dialogPreferredPadding}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_dialogTheme io.bitfountain.ashishpatel.sounddroid:dialogTheme}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_dividerHorizontal io.bitfountain.ashishpatel.sounddroid:dividerHorizontal}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_dividerVertical io.bitfountain.ashishpatel.sounddroid:dividerVertical}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_dropDownListViewStyle io.bitfountain.ashishpatel.sounddroid:dropDownListViewStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_dropdownListPreferredItemHeight io.bitfountain.ashishpatel.sounddroid:dropdownListPreferredItemHeight}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_editTextBackground io.bitfountain.ashishpatel.sounddroid:editTextBackground}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_editTextColor io.bitfountain.ashishpatel.sounddroid:editTextColor}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_editTextStyle io.bitfountain.ashishpatel.sounddroid:editTextStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_homeAsUpIndicator io.bitfountain.ashishpatel.sounddroid:homeAsUpIndicator}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_listChoiceBackgroundIndicator io.bitfountain.ashishpatel.sounddroid:listChoiceBackgroundIndicator}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_listDividerAlertDialog io.bitfountain.ashishpatel.sounddroid:listDividerAlertDialog}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_listPopupWindowStyle io.bitfountain.ashishpatel.sounddroid:listPopupWindowStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_listPreferredItemHeight io.bitfountain.ashishpatel.sounddroid:listPreferredItemHeight}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_listPreferredItemHeightLarge io.bitfountain.ashishpatel.sounddroid:listPreferredItemHeightLarge}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_listPreferredItemHeightSmall io.bitfountain.ashishpatel.sounddroid:listPreferredItemHeightSmall}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_listPreferredItemPaddingLeft io.bitfountain.ashishpatel.sounddroid:listPreferredItemPaddingLeft}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_listPreferredItemPaddingRight io.bitfountain.ashishpatel.sounddroid:listPreferredItemPaddingRight}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_panelBackground io.bitfountain.ashishpatel.sounddroid:panelBackground}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_panelMenuListTheme io.bitfountain.ashishpatel.sounddroid:panelMenuListTheme}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_panelMenuListWidth io.bitfountain.ashishpatel.sounddroid:panelMenuListWidth}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_popupMenuStyle io.bitfountain.ashishpatel.sounddroid:popupMenuStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_popupWindowStyle io.bitfountain.ashishpatel.sounddroid:popupWindowStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_radioButtonStyle io.bitfountain.ashishpatel.sounddroid:radioButtonStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_ratingBarStyle io.bitfountain.ashishpatel.sounddroid:ratingBarStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_searchViewStyle io.bitfountain.ashishpatel.sounddroid:searchViewStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_selectableItemBackground io.bitfountain.ashishpatel.sounddroid:selectableItemBackground}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_selectableItemBackgroundBorderless io.bitfountain.ashishpatel.sounddroid:selectableItemBackgroundBorderless}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_spinnerDropDownItemStyle io.bitfountain.ashishpatel.sounddroid:spinnerDropDownItemStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_spinnerStyle io.bitfountain.ashishpatel.sounddroid:spinnerStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_switchStyle io.bitfountain.ashishpatel.sounddroid:switchStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_textAppearanceLargePopupMenu io.bitfountain.ashishpatel.sounddroid:textAppearanceLargePopupMenu}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_textAppearanceListItem io.bitfountain.ashishpatel.sounddroid:textAppearanceListItem}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_textAppearanceListItemSmall io.bitfountain.ashishpatel.sounddroid:textAppearanceListItemSmall}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_textAppearanceSearchResultSubtitle io.bitfountain.ashishpatel.sounddroid:textAppearanceSearchResultSubtitle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_textAppearanceSearchResultTitle io.bitfountain.ashishpatel.sounddroid:textAppearanceSearchResultTitle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_textAppearanceSmallPopupMenu io.bitfountain.ashishpatel.sounddroid:textAppearanceSmallPopupMenu}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_textColorAlertDialogListItem io.bitfountain.ashishpatel.sounddroid:textColorAlertDialogListItem}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_textColorSearchUrl io.bitfountain.ashishpatel.sounddroid:textColorSearchUrl}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_toolbarNavigationButtonStyle io.bitfountain.ashishpatel.sounddroid:toolbarNavigationButtonStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_toolbarStyle io.bitfountain.ashishpatel.sounddroid:toolbarStyle}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_windowActionBar io.bitfountain.ashishpatel.sounddroid:windowActionBar}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_windowActionBarOverlay io.bitfountain.ashishpatel.sounddroid:windowActionBarOverlay}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_windowActionModeOverlay io.bitfountain.ashishpatel.sounddroid:windowActionModeOverlay}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_windowFixedHeightMajor io.bitfountain.ashishpatel.sounddroid:windowFixedHeightMajor}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_windowFixedHeightMinor io.bitfountain.ashishpatel.sounddroid:windowFixedHeightMinor}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_windowFixedWidthMajor io.bitfountain.ashishpatel.sounddroid:windowFixedWidthMajor}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_windowFixedWidthMinor io.bitfountain.ashishpatel.sounddroid:windowFixedWidthMinor}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_windowMinWidthMajor io.bitfountain.ashishpatel.sounddroid:windowMinWidthMajor}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_windowMinWidthMinor io.bitfountain.ashishpatel.sounddroid:windowMinWidthMinor}</code></td><td></td></tr>
               <tr><td><code>{@link #Theme_windowNoTitle io.bitfountain.ashishpatel.sounddroid:windowNoTitle}</code></td><td></td></tr>
               </table>
               @see #Theme_actionBarDivider
               @see #Theme_actionBarItemBackground
               @see #Theme_actionBarPopupTheme
               @see #Theme_actionBarSize
               @see #Theme_actionBarSplitStyle
               @see #Theme_actionBarStyle
               @see #Theme_actionBarTabBarStyle
               @see #Theme_actionBarTabStyle
               @see #Theme_actionBarTabTextStyle
               @see #Theme_actionBarTheme
               @see #Theme_actionBarWidgetTheme
               @see #Theme_actionButtonStyle
               @see #Theme_actionDropDownStyle
               @see #Theme_actionMenuTextAppearance
               @see #Theme_actionMenuTextColor
               @see #Theme_actionModeBackground
               @see #Theme_actionModeCloseButtonStyle
               @see #Theme_actionModeCloseDrawable
               @see #Theme_actionModeCopyDrawable
               @see #Theme_actionModeCutDrawable
               @see #Theme_actionModeFindDrawable
               @see #Theme_actionModePasteDrawable
               @see #Theme_actionModePopupWindowStyle
               @see #Theme_actionModeSelectAllDrawable
               @see #Theme_actionModeShareDrawable
               @see #Theme_actionModeSplitBackground
               @see #Theme_actionModeStyle
               @see #Theme_actionModeWebSearchDrawable
               @see #Theme_actionOverflowButtonStyle
               @see #Theme_actionOverflowMenuStyle
               @see #Theme_activityChooserViewStyle
               @see #Theme_alertDialogButtonGroupStyle
               @see #Theme_alertDialogCenterButtons
               @see #Theme_alertDialogStyle
               @see #Theme_alertDialogTheme
               @see #Theme_android_windowAnimationStyle
               @see #Theme_android_windowIsFloating
               @see #Theme_autoCompleteTextViewStyle
               @see #Theme_borderlessButtonStyle
               @see #Theme_buttonBarButtonStyle
               @see #Theme_buttonBarNegativeButtonStyle
               @see #Theme_buttonBarNeutralButtonStyle
               @see #Theme_buttonBarPositiveButtonStyle
               @see #Theme_buttonBarStyle
               @see #Theme_buttonStyle
               @see #Theme_buttonStyleSmall
               @see #Theme_checkboxStyle
               @see #Theme_checkedTextViewStyle
               @see #Theme_colorAccent
               @see #Theme_colorButtonNormal
               @see #Theme_colorControlActivated
               @see #Theme_colorControlHighlight
               @see #Theme_colorControlNormal
               @see #Theme_colorPrimary
               @see #Theme_colorPrimaryDark
               @see #Theme_colorSwitchThumbNormal
               @see #Theme_dialogPreferredPadding
               @see #Theme_dialogTheme
               @see #Theme_dividerHorizontal
               @see #Theme_dividerVertical
               @see #Theme_dropDownListViewStyle
               @see #Theme_dropdownListPreferredItemHeight
               @see #Theme_editTextBackground
               @see #Theme_editTextColor
               @see #Theme_editTextStyle
               @see #Theme_homeAsUpIndicator
               @see #Theme_listChoiceBackgroundIndicator
               @see #Theme_listDividerAlertDialog
               @see #Theme_listPopupWindowStyle
               @see #Theme_listPreferredItemHeight
               @see #Theme_listPreferredItemHeightLarge
               @see #Theme_listPreferredItemHeightSmall
               @see #Theme_listPreferredItemPaddingLeft
               @see #Theme_listPreferredItemPaddingRight
               @see #Theme_panelBackground
               @see #Theme_panelMenuListTheme
               @see #Theme_panelMenuListWidth
               @see #Theme_popupMenuStyle
               @see #Theme_popupWindowStyle
               @see #Theme_radioButtonStyle
               @see #Theme_ratingBarStyle
               @see #Theme_searchViewStyle
               @see #Theme_selectableItemBackground
               @see #Theme_selectableItemBackgroundBorderless
               @see #Theme_spinnerDropDownItemStyle
               @see #Theme_spinnerStyle
               @see #Theme_switchStyle
               @see #Theme_textAppearanceLargePopupMenu
               @see #Theme_textAppearanceListItem
               @see #Theme_textAppearanceListItemSmall
               @see #Theme_textAppearanceSearchResultSubtitle
               @see #Theme_textAppearanceSearchResultTitle
               @see #Theme_textAppearanceSmallPopupMenu
               @see #Theme_textColorAlertDialogListItem
               @see #Theme_textColorSearchUrl
               @see #Theme_toolbarNavigationButtonStyle
               @see #Theme_toolbarStyle
               @see #Theme_windowActionBar
               @see #Theme_windowActionBarOverlay
               @see #Theme_windowActionModeOverlay
               @see #Theme_windowFixedHeightMajor
               @see #Theme_windowFixedHeightMinor
               @see #Theme_windowFixedWidthMajor
               @see #Theme_windowFixedWidthMinor
               @see #Theme_windowMinWidthMajor
               @see #Theme_windowMinWidthMinor
               @see #Theme_windowNoTitle
             */
            public static final int[] Theme = {
                0x01010057, 0x010100ae, 0x7f010085, 0x7f010086,
                0x7f010087, 0x7f010088, 0x7f010089, 0x7f01008a,
                0x7f01008b, 0x7f01008c, 0x7f01008d, 0x7f01008e,
                0x7f01008f, 0x7f010090, 0x7f010091, 0x7f010092,
                0x7f010093, 0x7f010094, 0x7f010095, 0x7f010096,
                0x7f010097, 0x7f010098, 0x7f010099, 0x7f01009a,
                0x7f01009b, 0x7f01009c, 0x7f01009d, 0x7f01009e,
                0x7f01009f, 0x7f0100a0, 0x7f0100a1, 0x7f0100a2,
                0x7f0100a3, 0x7f0100a4, 0x7f0100a5, 0x7f0100a6,
                0x7f0100a7, 0x7f0100a8, 0x7f0100a9, 0x7f0100aa,
                0x7f0100ab, 0x7f0100ac, 0x7f0100ad, 0x7f0100ae,
                0x7f0100af, 0x7f0100b0, 0x7f0100b1, 0x7f0100b2,
                0x7f0100b3, 0x7f0100b4, 0x7f0100b5, 0x7f0100b6,
                0x7f0100b7, 0x7f0100b8, 0x7f0100b9, 0x7f0100ba,
                0x7f0100bb, 0x7f0100bc, 0x7f0100bd, 0x7f0100be,
                0x7f0100bf, 0x7f0100c0, 0x7f0100c1, 0x7f0100c2,
                0x7f0100c3, 0x7f0100c4, 0x7f0100c5, 0x7f0100c6,
                0x7f0100c7, 0x7f0100c8, 0x7f0100c9, 0x7f0100ca,
                0x7f0100cb, 0x7f0100cc, 0x7f0100cd, 0x7f0100ce,
                0x7f0100cf, 0x7f0100d0, 0x7f0100d1, 0x7f0100d2,
                0x7f0100d3, 0x7f0100d4, 0x7f0100d5, 0x7f0100d6,
                0x7f0100d7, 0x7f0100d8, 0x7f0100d9, 0x7f0100da,
                0x7f0100db, 0x7f0100dc, 0x7f0100dd, 0x7f0100de,
                0x7f0100df, 0x7f0100e0, 0x7f0100e1, 0x7f0100e2,
                0x7f0100e3, 0x7f0100e4, 0x7f0100e5, 0x7f0100e6,
                0x7f0100e7, 0x7f0100e8, 0x7f0100e9, 0x7f0100ea,
                0x7f0100eb, 0x7f0100ec, 0x7f0100ed
            };
            /**
              <p>This symbol is the offset where the {@link attr#actionBarDivider}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionBarDivider
            */
            public static final int Theme_actionBarDivider = 23;
            /**
              <p>This symbol is the offset where the {@link attr#actionBarItemBackground}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionBarItemBackground
            */
            public static final int Theme_actionBarItemBackground = 24;
            /**
              <p>This symbol is the offset where the {@link attr#actionBarPopupTheme}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionBarPopupTheme
            */
            public static final int Theme_actionBarPopupTheme = 17;
            /**
              <p>This symbol is the offset where the {@link attr#actionBarSize}
              attribute's value can be found in the {@link #Theme} array.


              <p>May be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
    <p>May be one of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>wrap_content</code></td><td>0</td><td></td></tr>
    </table>
              @attr name io.bitfountain.ashishpatel.sounddroid:actionBarSize
            */
            public static final int Theme_actionBarSize = 22;
            /**
              <p>This symbol is the offset where the {@link attr#actionBarSplitStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionBarSplitStyle
            */
            public static final int Theme_actionBarSplitStyle = 19;
            /**
              <p>This symbol is the offset where the {@link attr#actionBarStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionBarStyle
            */
            public static final int Theme_actionBarStyle = 18;
            /**
              <p>This symbol is the offset where the {@link attr#actionBarTabBarStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionBarTabBarStyle
            */
            public static final int Theme_actionBarTabBarStyle = 13;
            /**
              <p>This symbol is the offset where the {@link attr#actionBarTabStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionBarTabStyle
            */
            public static final int Theme_actionBarTabStyle = 12;
            /**
              <p>This symbol is the offset where the {@link attr#actionBarTabTextStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionBarTabTextStyle
            */
            public static final int Theme_actionBarTabTextStyle = 14;
            /**
              <p>This symbol is the offset where the {@link attr#actionBarTheme}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionBarTheme
            */
            public static final int Theme_actionBarTheme = 20;
            /**
              <p>This symbol is the offset where the {@link attr#actionBarWidgetTheme}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionBarWidgetTheme
            */
            public static final int Theme_actionBarWidgetTheme = 21;
            /**
              <p>This symbol is the offset where the {@link attr#actionButtonStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionButtonStyle
            */
            public static final int Theme_actionButtonStyle = 49;
            /**
              <p>This symbol is the offset where the {@link attr#actionDropDownStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionDropDownStyle
            */
            public static final int Theme_actionDropDownStyle = 45;
            /**
              <p>This symbol is the offset where the {@link attr#actionMenuTextAppearance}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionMenuTextAppearance
            */
            public static final int Theme_actionMenuTextAppearance = 25;
            /**
              <p>This symbol is the offset where the {@link attr#actionMenuTextColor}
              attribute's value can be found in the {@link #Theme} array.


              <p>May be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
    <p>May be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionMenuTextColor
            */
            public static final int Theme_actionMenuTextColor = 26;
            /**
              <p>This symbol is the offset where the {@link attr#actionModeBackground}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionModeBackground
            */
            public static final int Theme_actionModeBackground = 29;
            /**
              <p>This symbol is the offset where the {@link attr#actionModeCloseButtonStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionModeCloseButtonStyle
            */
            public static final int Theme_actionModeCloseButtonStyle = 28;
            /**
              <p>This symbol is the offset where the {@link attr#actionModeCloseDrawable}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionModeCloseDrawable
            */
            public static final int Theme_actionModeCloseDrawable = 31;
            /**
              <p>This symbol is the offset where the {@link attr#actionModeCopyDrawable}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionModeCopyDrawable
            */
            public static final int Theme_actionModeCopyDrawable = 33;
            /**
              <p>This symbol is the offset where the {@link attr#actionModeCutDrawable}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionModeCutDrawable
            */
            public static final int Theme_actionModeCutDrawable = 32;
            /**
              <p>This symbol is the offset where the {@link attr#actionModeFindDrawable}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionModeFindDrawable
            */
            public static final int Theme_actionModeFindDrawable = 37;
            /**
              <p>This symbol is the offset where the {@link attr#actionModePasteDrawable}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionModePasteDrawable
            */
            public static final int Theme_actionModePasteDrawable = 34;
            /**
              <p>This symbol is the offset where the {@link attr#actionModePopupWindowStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionModePopupWindowStyle
            */
            public static final int Theme_actionModePopupWindowStyle = 39;
            /**
              <p>This symbol is the offset where the {@link attr#actionModeSelectAllDrawable}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionModeSelectAllDrawable
            */
            public static final int Theme_actionModeSelectAllDrawable = 35;
            /**
              <p>This symbol is the offset where the {@link attr#actionModeShareDrawable}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionModeShareDrawable
            */
            public static final int Theme_actionModeShareDrawable = 36;
            /**
              <p>This symbol is the offset where the {@link attr#actionModeSplitBackground}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionModeSplitBackground
            */
            public static final int Theme_actionModeSplitBackground = 30;
            /**
              <p>This symbol is the offset where the {@link attr#actionModeStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionModeStyle
            */
            public static final int Theme_actionModeStyle = 27;
            /**
              <p>This symbol is the offset where the {@link attr#actionModeWebSearchDrawable}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionModeWebSearchDrawable
            */
            public static final int Theme_actionModeWebSearchDrawable = 38;
            /**
              <p>This symbol is the offset where the {@link attr#actionOverflowButtonStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionOverflowButtonStyle
            */
            public static final int Theme_actionOverflowButtonStyle = 15;
            /**
              <p>This symbol is the offset where the {@link attr#actionOverflowMenuStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:actionOverflowMenuStyle
            */
            public static final int Theme_actionOverflowMenuStyle = 16;
            /**
              <p>This symbol is the offset where the {@link attr#activityChooserViewStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:activityChooserViewStyle
            */
            public static final int Theme_activityChooserViewStyle = 57;
            /**
              <p>This symbol is the offset where the {@link attr#alertDialogButtonGroupStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:alertDialogButtonGroupStyle
            */
            public static final int Theme_alertDialogButtonGroupStyle = 90;
            /**
              <p>This symbol is the offset where the {@link attr#alertDialogCenterButtons}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:alertDialogCenterButtons
            */
            public static final int Theme_alertDialogCenterButtons = 91;
            /**
              <p>This symbol is the offset where the {@link attr#alertDialogStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:alertDialogStyle
            */
            public static final int Theme_alertDialogStyle = 89;
            /**
              <p>This symbol is the offset where the {@link attr#alertDialogTheme}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:alertDialogTheme
            */
            public static final int Theme_alertDialogTheme = 92;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#windowAnimationStyle}
              attribute's value can be found in the {@link #Theme} array.
              @attr name android:windowAnimationStyle
            */
            public static final int Theme_android_windowAnimationStyle = 1;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#windowIsFloating}
              attribute's value can be found in the {@link #Theme} array.
              @attr name android:windowIsFloating
            */
            public static final int Theme_android_windowIsFloating = 0;
            /**
              <p>This symbol is the offset where the {@link attr#autoCompleteTextViewStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:autoCompleteTextViewStyle
            */
            public static final int Theme_autoCompleteTextViewStyle = 97;
            /**
              <p>This symbol is the offset where the {@link attr#borderlessButtonStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:borderlessButtonStyle
            */
            public static final int Theme_borderlessButtonStyle = 54;
            /**
              <p>This symbol is the offset where the {@link attr#buttonBarButtonStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:buttonBarButtonStyle
            */
            public static final int Theme_buttonBarButtonStyle = 51;
            /**
              <p>This symbol is the offset where the {@link attr#buttonBarNegativeButtonStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:buttonBarNegativeButtonStyle
            */
            public static final int Theme_buttonBarNegativeButtonStyle = 95;
            /**
              <p>This symbol is the offset where the {@link attr#buttonBarNeutralButtonStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:buttonBarNeutralButtonStyle
            */
            public static final int Theme_buttonBarNeutralButtonStyle = 96;
            /**
              <p>This symbol is the offset where the {@link attr#buttonBarPositiveButtonStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:buttonBarPositiveButtonStyle
            */
            public static final int Theme_buttonBarPositiveButtonStyle = 94;
            /**
              <p>This symbol is the offset where the {@link attr#buttonBarStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:buttonBarStyle
            */
            public static final int Theme_buttonBarStyle = 50;
            /**
              <p>This symbol is the offset where the {@link attr#buttonStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:buttonStyle
            */
            public static final int Theme_buttonStyle = 98;
            /**
              <p>This symbol is the offset where the {@link attr#buttonStyleSmall}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:buttonStyleSmall
            */
            public static final int Theme_buttonStyleSmall = 99;
            /**
              <p>This symbol is the offset where the {@link attr#checkboxStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:checkboxStyle
            */
            public static final int Theme_checkboxStyle = 100;
            /**
              <p>This symbol is the offset where the {@link attr#checkedTextViewStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:checkedTextViewStyle
            */
            public static final int Theme_checkedTextViewStyle = 101;
            /**
              <p>This symbol is the offset where the {@link attr#colorAccent}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:colorAccent
            */
            public static final int Theme_colorAccent = 83;
            /**
              <p>This symbol is the offset where the {@link attr#colorButtonNormal}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:colorButtonNormal
            */
            public static final int Theme_colorButtonNormal = 87;
            /**
              <p>This symbol is the offset where the {@link attr#colorControlActivated}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:colorControlActivated
            */
            public static final int Theme_colorControlActivated = 85;
            /**
              <p>This symbol is the offset where the {@link attr#colorControlHighlight}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:colorControlHighlight
            */
            public static final int Theme_colorControlHighlight = 86;
            /**
              <p>This symbol is the offset where the {@link attr#colorControlNormal}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:colorControlNormal
            */
            public static final int Theme_colorControlNormal = 84;
            /**
              <p>This symbol is the offset where the {@link attr#colorPrimary}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:colorPrimary
            */
            public static final int Theme_colorPrimary = 81;
            /**
              <p>This symbol is the offset where the {@link attr#colorPrimaryDark}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:colorPrimaryDark
            */
            public static final int Theme_colorPrimaryDark = 82;
            /**
              <p>This symbol is the offset where the {@link attr#colorSwitchThumbNormal}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:colorSwitchThumbNormal
            */
            public static final int Theme_colorSwitchThumbNormal = 88;
            /**
              <p>This symbol is the offset where the {@link attr#dialogPreferredPadding}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:dialogPreferredPadding
            */
            public static final int Theme_dialogPreferredPadding = 43;
            /**
              <p>This symbol is the offset where the {@link attr#dialogTheme}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:dialogTheme
            */
            public static final int Theme_dialogTheme = 42;
            /**
              <p>This symbol is the offset where the {@link attr#dividerHorizontal}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:dividerHorizontal
            */
            public static final int Theme_dividerHorizontal = 56;
            /**
              <p>This symbol is the offset where the {@link attr#dividerVertical}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:dividerVertical
            */
            public static final int Theme_dividerVertical = 55;
            /**
              <p>This symbol is the offset where the {@link attr#dropDownListViewStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:dropDownListViewStyle
            */
            public static final int Theme_dropDownListViewStyle = 73;
            /**
              <p>This symbol is the offset where the {@link attr#dropdownListPreferredItemHeight}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:dropdownListPreferredItemHeight
            */
            public static final int Theme_dropdownListPreferredItemHeight = 46;
            /**
              <p>This symbol is the offset where the {@link attr#editTextBackground}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:editTextBackground
            */
            public static final int Theme_editTextBackground = 63;
            /**
              <p>This symbol is the offset where the {@link attr#editTextColor}
              attribute's value can be found in the {@link #Theme} array.


              <p>May be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
    <p>May be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:editTextColor
            */
            public static final int Theme_editTextColor = 62;
            /**
              <p>This symbol is the offset where the {@link attr#editTextStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:editTextStyle
            */
            public static final int Theme_editTextStyle = 102;
            /**
              <p>This symbol is the offset where the {@link attr#homeAsUpIndicator}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:homeAsUpIndicator
            */
            public static final int Theme_homeAsUpIndicator = 48;
            /**
              <p>This symbol is the offset where the {@link attr#listChoiceBackgroundIndicator}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:listChoiceBackgroundIndicator
            */
            public static final int Theme_listChoiceBackgroundIndicator = 80;
            /**
              <p>This symbol is the offset where the {@link attr#listDividerAlertDialog}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:listDividerAlertDialog
            */
            public static final int Theme_listDividerAlertDialog = 44;
            /**
              <p>This symbol is the offset where the {@link attr#listPopupWindowStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:listPopupWindowStyle
            */
            public static final int Theme_listPopupWindowStyle = 74;
            /**
              <p>This symbol is the offset where the {@link attr#listPreferredItemHeight}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:listPreferredItemHeight
            */
            public static final int Theme_listPreferredItemHeight = 68;
            /**
              <p>This symbol is the offset where the {@link attr#listPreferredItemHeightLarge}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:listPreferredItemHeightLarge
            */
            public static final int Theme_listPreferredItemHeightLarge = 70;
            /**
              <p>This symbol is the offset where the {@link attr#listPreferredItemHeightSmall}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:listPreferredItemHeightSmall
            */
            public static final int Theme_listPreferredItemHeightSmall = 69;
            /**
              <p>This symbol is the offset where the {@link attr#listPreferredItemPaddingLeft}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:listPreferredItemPaddingLeft
            */
            public static final int Theme_listPreferredItemPaddingLeft = 71;
            /**
              <p>This symbol is the offset where the {@link attr#listPreferredItemPaddingRight}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:listPreferredItemPaddingRight
            */
            public static final int Theme_listPreferredItemPaddingRight = 72;
            /**
              <p>This symbol is the offset where the {@link attr#panelBackground}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:panelBackground
            */
            public static final int Theme_panelBackground = 77;
            /**
              <p>This symbol is the offset where the {@link attr#panelMenuListTheme}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:panelMenuListTheme
            */
            public static final int Theme_panelMenuListTheme = 79;
            /**
              <p>This symbol is the offset where the {@link attr#panelMenuListWidth}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:panelMenuListWidth
            */
            public static final int Theme_panelMenuListWidth = 78;
            /**
              <p>This symbol is the offset where the {@link attr#popupMenuStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:popupMenuStyle
            */
            public static final int Theme_popupMenuStyle = 60;
            /**
              <p>This symbol is the offset where the {@link attr#popupWindowStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:popupWindowStyle
            */
            public static final int Theme_popupWindowStyle = 61;
            /**
              <p>This symbol is the offset where the {@link attr#radioButtonStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:radioButtonStyle
            */
            public static final int Theme_radioButtonStyle = 103;
            /**
              <p>This symbol is the offset where the {@link attr#ratingBarStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:ratingBarStyle
            */
            public static final int Theme_ratingBarStyle = 104;
            /**
              <p>This symbol is the offset where the {@link attr#searchViewStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:searchViewStyle
            */
            public static final int Theme_searchViewStyle = 67;
            /**
              <p>This symbol is the offset where the {@link attr#selectableItemBackground}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:selectableItemBackground
            */
            public static final int Theme_selectableItemBackground = 52;
            /**
              <p>This symbol is the offset where the {@link attr#selectableItemBackgroundBorderless}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:selectableItemBackgroundBorderless
            */
            public static final int Theme_selectableItemBackgroundBorderless = 53;
            /**
              <p>This symbol is the offset where the {@link attr#spinnerDropDownItemStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:spinnerDropDownItemStyle
            */
            public static final int Theme_spinnerDropDownItemStyle = 47;
            /**
              <p>This symbol is the offset where the {@link attr#spinnerStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:spinnerStyle
            */
            public static final int Theme_spinnerStyle = 105;
            /**
              <p>This symbol is the offset where the {@link attr#switchStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:switchStyle
            */
            public static final int Theme_switchStyle = 106;
            /**
              <p>This symbol is the offset where the {@link attr#textAppearanceLargePopupMenu}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:textAppearanceLargePopupMenu
            */
            public static final int Theme_textAppearanceLargePopupMenu = 40;
            /**
              <p>This symbol is the offset where the {@link attr#textAppearanceListItem}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:textAppearanceListItem
            */
            public static final int Theme_textAppearanceListItem = 75;
            /**
              <p>This symbol is the offset where the {@link attr#textAppearanceListItemSmall}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:textAppearanceListItemSmall
            */
            public static final int Theme_textAppearanceListItemSmall = 76;
            /**
              <p>This symbol is the offset where the {@link attr#textAppearanceSearchResultSubtitle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:textAppearanceSearchResultSubtitle
            */
            public static final int Theme_textAppearanceSearchResultSubtitle = 65;
            /**
              <p>This symbol is the offset where the {@link attr#textAppearanceSearchResultTitle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:textAppearanceSearchResultTitle
            */
            public static final int Theme_textAppearanceSearchResultTitle = 64;
            /**
              <p>This symbol is the offset where the {@link attr#textAppearanceSmallPopupMenu}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:textAppearanceSmallPopupMenu
            */
            public static final int Theme_textAppearanceSmallPopupMenu = 41;
            /**
              <p>This symbol is the offset where the {@link attr#textColorAlertDialogListItem}
              attribute's value can be found in the {@link #Theme} array.


              <p>May be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
    <p>May be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:textColorAlertDialogListItem
            */
            public static final int Theme_textColorAlertDialogListItem = 93;
            /**
              <p>This symbol is the offset where the {@link attr#textColorSearchUrl}
              attribute's value can be found in the {@link #Theme} array.


              <p>May be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
    <p>May be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:textColorSearchUrl
            */
            public static final int Theme_textColorSearchUrl = 66;
            /**
              <p>This symbol is the offset where the {@link attr#toolbarNavigationButtonStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:toolbarNavigationButtonStyle
            */
            public static final int Theme_toolbarNavigationButtonStyle = 59;
            /**
              <p>This symbol is the offset where the {@link attr#toolbarStyle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:toolbarStyle
            */
            public static final int Theme_toolbarStyle = 58;
            /**
              <p>This symbol is the offset where the {@link attr#windowActionBar}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:windowActionBar
            */
            public static final int Theme_windowActionBar = 2;
            /**
              <p>This symbol is the offset where the {@link attr#windowActionBarOverlay}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:windowActionBarOverlay
            */
            public static final int Theme_windowActionBarOverlay = 4;
            /**
              <p>This symbol is the offset where the {@link attr#windowActionModeOverlay}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:windowActionModeOverlay
            */
            public static final int Theme_windowActionModeOverlay = 5;
            /**
              <p>This symbol is the offset where the {@link attr#windowFixedHeightMajor}
              attribute's value can be found in the {@link #Theme} array.


              <p>May be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>May be a fractional value, which is a floating point number appended with either % or %p, such as "<code>14.5%</code>".
    The % suffix always means a percentage of the base size; the optional %p suffix provides a size relative to
    some parent container.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:windowFixedHeightMajor
            */
            public static final int Theme_windowFixedHeightMajor = 9;
            /**
              <p>This symbol is the offset where the {@link attr#windowFixedHeightMinor}
              attribute's value can be found in the {@link #Theme} array.


              <p>May be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>May be a fractional value, which is a floating point number appended with either % or %p, such as "<code>14.5%</code>".
    The % suffix always means a percentage of the base size; the optional %p suffix provides a size relative to
    some parent container.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:windowFixedHeightMinor
            */
            public static final int Theme_windowFixedHeightMinor = 7;
            /**
              <p>This symbol is the offset where the {@link attr#windowFixedWidthMajor}
              attribute's value can be found in the {@link #Theme} array.


              <p>May be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>May be a fractional value, which is a floating point number appended with either % or %p, such as "<code>14.5%</code>".
    The % suffix always means a percentage of the base size; the optional %p suffix provides a size relative to
    some parent container.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:windowFixedWidthMajor
            */
            public static final int Theme_windowFixedWidthMajor = 6;
            /**
              <p>This symbol is the offset where the {@link attr#windowFixedWidthMinor}
              attribute's value can be found in the {@link #Theme} array.


              <p>May be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>May be a fractional value, which is a floating point number appended with either % or %p, such as "<code>14.5%</code>".
    The % suffix always means a percentage of the base size; the optional %p suffix provides a size relative to
    some parent container.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:windowFixedWidthMinor
            */
            public static final int Theme_windowFixedWidthMinor = 8;
            /**
              <p>This symbol is the offset where the {@link attr#windowMinWidthMajor}
              attribute's value can be found in the {@link #Theme} array.


              <p>May be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>May be a fractional value, which is a floating point number appended with either % or %p, such as "<code>14.5%</code>".
    The % suffix always means a percentage of the base size; the optional %p suffix provides a size relative to
    some parent container.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:windowMinWidthMajor
            */
            public static final int Theme_windowMinWidthMajor = 10;
            /**
              <p>This symbol is the offset where the {@link attr#windowMinWidthMinor}
              attribute's value can be found in the {@link #Theme} array.


              <p>May be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>May be a fractional value, which is a floating point number appended with either % or %p, such as "<code>14.5%</code>".
    The % suffix always means a percentage of the base size; the optional %p suffix provides a size relative to
    some parent container.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:windowMinWidthMinor
            */
            public static final int Theme_windowMinWidthMinor = 11;
            /**
              <p>This symbol is the offset where the {@link attr#windowNoTitle}
              attribute's value can be found in the {@link #Theme} array.


              <p>Must be a boolean value, either "<code>true</code>" or "<code>false</code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:windowNoTitle
            */
            public static final int Theme_windowNoTitle = 3;
            /** Attributes that can be used with a Toolbar.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #Toolbar_android_gravity android:gravity}</code></td><td></td></tr>
               <tr><td><code>{@link #Toolbar_android_minHeight android:minHeight}</code></td><td></td></tr>
               <tr><td><code>{@link #Toolbar_collapseContentDescription io.bitfountain.ashishpatel.sounddroid:collapseContentDescription}</code></td><td></td></tr>
               <tr><td><code>{@link #Toolbar_collapseIcon io.bitfountain.ashishpatel.sounddroid:collapseIcon}</code></td><td></td></tr>
               <tr><td><code>{@link #Toolbar_contentInsetEnd io.bitfountain.ashishpatel.sounddroid:contentInsetEnd}</code></td><td></td></tr>
               <tr><td><code>{@link #Toolbar_contentInsetLeft io.bitfountain.ashishpatel.sounddroid:contentInsetLeft}</code></td><td></td></tr>
               <tr><td><code>{@link #Toolbar_contentInsetRight io.bitfountain.ashishpatel.sounddroid:contentInsetRight}</code></td><td></td></tr>
               <tr><td><code>{@link #Toolbar_contentInsetStart io.bitfountain.ashishpatel.sounddroid:contentInsetStart}</code></td><td></td></tr>
               <tr><td><code>{@link #Toolbar_maxButtonHeight io.bitfountain.ashishpatel.sounddroid:maxButtonHeight}</code></td><td></td></tr>
               <tr><td><code>{@link #Toolbar_navigationContentDescription io.bitfountain.ashishpatel.sounddroid:navigationContentDescription}</code></td><td></td></tr>
               <tr><td><code>{@link #Toolbar_navigationIcon io.bitfountain.ashishpatel.sounddroid:navigationIcon}</code></td><td></td></tr>
               <tr><td><code>{@link #Toolbar_popupTheme io.bitfountain.ashishpatel.sounddroid:popupTheme}</code></td><td></td></tr>
               <tr><td><code>{@link #Toolbar_subtitle io.bitfountain.ashishpatel.sounddroid:subtitle}</code></td><td></td></tr>
               <tr><td><code>{@link #Toolbar_subtitleTextAppearance io.bitfountain.ashishpatel.sounddroid:subtitleTextAppearance}</code></td><td></td></tr>
               <tr><td><code>{@link #Toolbar_title io.bitfountain.ashishpatel.sounddroid:title}</code></td><td></td></tr>
               <tr><td><code>{@link #Toolbar_titleMarginBottom io.bitfountain.ashishpatel.sounddroid:titleMarginBottom}</code></td><td></td></tr>
               <tr><td><code>{@link #Toolbar_titleMarginEnd io.bitfountain.ashishpatel.sounddroid:titleMarginEnd}</code></td><td></td></tr>
               <tr><td><code>{@link #Toolbar_titleMarginStart io.bitfountain.ashishpatel.sounddroid:titleMarginStart}</code></td><td></td></tr>
               <tr><td><code>{@link #Toolbar_titleMarginTop io.bitfountain.ashishpatel.sounddroid:titleMarginTop}</code></td><td></td></tr>
               <tr><td><code>{@link #Toolbar_titleMargins io.bitfountain.ashishpatel.sounddroid:titleMargins}</code></td><td></td></tr>
               <tr><td><code>{@link #Toolbar_titleTextAppearance io.bitfountain.ashishpatel.sounddroid:titleTextAppearance}</code></td><td></td></tr>
               </table>
               @see #Toolbar_android_gravity
               @see #Toolbar_android_minHeight
               @see #Toolbar_collapseContentDescription
               @see #Toolbar_collapseIcon
               @see #Toolbar_contentInsetEnd
               @see #Toolbar_contentInsetLeft
               @see #Toolbar_contentInsetRight
               @see #Toolbar_contentInsetStart
               @see #Toolbar_maxButtonHeight
               @see #Toolbar_navigationContentDescription
               @see #Toolbar_navigationIcon
               @see #Toolbar_popupTheme
               @see #Toolbar_subtitle
               @see #Toolbar_subtitleTextAppearance
               @see #Toolbar_title
               @see #Toolbar_titleMarginBottom
               @see #Toolbar_titleMarginEnd
               @see #Toolbar_titleMarginStart
               @see #Toolbar_titleMarginTop
               @see #Toolbar_titleMargins
               @see #Toolbar_titleTextAppearance
             */
            public static final int[] Toolbar = {
                0x010100af, 0x01010140, 0x7f010003, 0x7f010006,
                0x7f010016, 0x7f010017, 0x7f010018, 0x7f010019,
                0x7f01001b, 0x7f0100ee, 0x7f0100ef, 0x7f0100f0,
                0x7f0100f1, 0x7f0100f2, 0x7f0100f3, 0x7f0100f4,
                0x7f0100f5, 0x7f0100f6, 0x7f0100f7, 0x7f0100f8,
                0x7f0100f9
            };
            /**
              <p>This symbol is the offset where the {@link android.R.attr#gravity}
              attribute's value can be found in the {@link #Toolbar} array.
              @attr name android:gravity
            */
            public static final int Toolbar_android_gravity = 0;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#minHeight}
              attribute's value can be found in the {@link #Toolbar} array.
              @attr name android:minHeight
            */
            public static final int Toolbar_android_minHeight = 1;
            /**
              <p>This symbol is the offset where the {@link attr#collapseContentDescription}
              attribute's value can be found in the {@link #Toolbar} array.


              <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:collapseContentDescription
            */
            public static final int Toolbar_collapseContentDescription = 18;
            /**
              <p>This symbol is the offset where the {@link attr#collapseIcon}
              attribute's value can be found in the {@link #Toolbar} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:collapseIcon
            */
            public static final int Toolbar_collapseIcon = 17;
            /**
              <p>This symbol is the offset where the {@link attr#contentInsetEnd}
              attribute's value can be found in the {@link #Toolbar} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:contentInsetEnd
            */
            public static final int Toolbar_contentInsetEnd = 5;
            /**
              <p>This symbol is the offset where the {@link attr#contentInsetLeft}
              attribute's value can be found in the {@link #Toolbar} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:contentInsetLeft
            */
            public static final int Toolbar_contentInsetLeft = 6;
            /**
              <p>This symbol is the offset where the {@link attr#contentInsetRight}
              attribute's value can be found in the {@link #Toolbar} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:contentInsetRight
            */
            public static final int Toolbar_contentInsetRight = 7;
            /**
              <p>This symbol is the offset where the {@link attr#contentInsetStart}
              attribute's value can be found in the {@link #Toolbar} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:contentInsetStart
            */
            public static final int Toolbar_contentInsetStart = 4;
            /**
              <p>This symbol is the offset where the {@link attr#maxButtonHeight}
              attribute's value can be found in the {@link #Toolbar} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:maxButtonHeight
            */
            public static final int Toolbar_maxButtonHeight = 16;
            /**
              <p>This symbol is the offset where the {@link attr#navigationContentDescription}
              attribute's value can be found in the {@link #Toolbar} array.


              <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:navigationContentDescription
            */
            public static final int Toolbar_navigationContentDescription = 20;
            /**
              <p>This symbol is the offset where the {@link attr#navigationIcon}
              attribute's value can be found in the {@link #Toolbar} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:navigationIcon
            */
            public static final int Toolbar_navigationIcon = 19;
            /**
              <p>This symbol is the offset where the {@link attr#popupTheme}
              attribute's value can be found in the {@link #Toolbar} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:popupTheme
            */
            public static final int Toolbar_popupTheme = 8;
            /**
              <p>This symbol is the offset where the {@link attr#subtitle}
              attribute's value can be found in the {@link #Toolbar} array.


              <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:subtitle
            */
            public static final int Toolbar_subtitle = 3;
            /**
              <p>This symbol is the offset where the {@link attr#subtitleTextAppearance}
              attribute's value can be found in the {@link #Toolbar} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:subtitleTextAppearance
            */
            public static final int Toolbar_subtitleTextAppearance = 10;
            /**
              <p>This symbol is the offset where the {@link attr#title}
              attribute's value can be found in the {@link #Toolbar} array.


              <p>Must be a string value, using '\\;' to escape characters such as '\\n' or '\\uxxxx' for a unicode character.
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:title
            */
            public static final int Toolbar_title = 2;
            /**
              <p>This symbol is the offset where the {@link attr#titleMarginBottom}
              attribute's value can be found in the {@link #Toolbar} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:titleMarginBottom
            */
            public static final int Toolbar_titleMarginBottom = 15;
            /**
              <p>This symbol is the offset where the {@link attr#titleMarginEnd}
              attribute's value can be found in the {@link #Toolbar} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:titleMarginEnd
            */
            public static final int Toolbar_titleMarginEnd = 13;
            /**
              <p>This symbol is the offset where the {@link attr#titleMarginStart}
              attribute's value can be found in the {@link #Toolbar} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:titleMarginStart
            */
            public static final int Toolbar_titleMarginStart = 12;
            /**
              <p>This symbol is the offset where the {@link attr#titleMarginTop}
              attribute's value can be found in the {@link #Toolbar} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:titleMarginTop
            */
            public static final int Toolbar_titleMarginTop = 14;
            /**
              <p>This symbol is the offset where the {@link attr#titleMargins}
              attribute's value can be found in the {@link #Toolbar} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:titleMargins
            */
            public static final int Toolbar_titleMargins = 11;
            /**
              <p>This symbol is the offset where the {@link attr#titleTextAppearance}
              attribute's value can be found in the {@link #Toolbar} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:titleTextAppearance
            */
            public static final int Toolbar_titleTextAppearance = 9;
            /** Attributes that can be used with a View.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #View_android_focusable android:focusable}</code></td><td></td></tr>
               <tr><td><code>{@link #View_android_theme android:theme}</code></td><td></td></tr>
               <tr><td><code>{@link #View_backgroundTint io.bitfountain.ashishpatel.sounddroid:backgroundTint}</code></td><td></td></tr>
               <tr><td><code>{@link #View_backgroundTintMode io.bitfountain.ashishpatel.sounddroid:backgroundTintMode}</code></td><td></td></tr>
               <tr><td><code>{@link #View_paddingEnd io.bitfountain.ashishpatel.sounddroid:paddingEnd}</code></td><td></td></tr>
               <tr><td><code>{@link #View_paddingStart io.bitfountain.ashishpatel.sounddroid:paddingStart}</code></td><td></td></tr>
               <tr><td><code>{@link #View_theme io.bitfountain.ashishpatel.sounddroid:theme}</code></td><td></td></tr>
               </table>
               @see #View_android_focusable
               @see #View_android_theme
               @see #View_backgroundTint
               @see #View_backgroundTintMode
               @see #View_paddingEnd
               @see #View_paddingStart
               @see #View_theme
             */
            public static final int[] View = {
                0x01010000, 0x010100da, 0x7f0100fa, 0x7f0100fb,
                0x7f0100fc, 0x7f0100fd, 0x7f0100fe
            };
            /**
              <p>This symbol is the offset where the {@link android.R.attr#focusable}
              attribute's value can be found in the {@link #View} array.
              @attr name android:focusable
            */
            public static final int View_android_focusable = 1;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#theme}
              attribute's value can be found in the {@link #View} array.
              @attr name android:theme
            */
            public static final int View_android_theme = 0;
            /**
              <p>This symbol is the offset where the {@link attr#backgroundTint}
              attribute's value can be found in the {@link #View} array.


              <p>Must be a color value, in the form of "<code>#<i>rgb</i></code>", "<code>#<i>argb</i></code>",
    "<code>#<i>rrggbb</i></code>", or "<code>#<i>aarrggbb</i></code>".
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:backgroundTint
            */
            public static final int View_backgroundTint = 5;
            /**
              <p>This symbol is the offset where the {@link attr#backgroundTintMode}
              attribute's value can be found in the {@link #View} array.


              <p>Must be one of the following constant values.</p>
    <table>
    <colgroup align="left" />
    <colgroup align="left" />
    <colgroup align="left" />
    <tr><th>Constant</th><th>Value</th><th>Description</th></tr>
    <tr><td><code>src_over</code></td><td>3</td><td></td></tr>
    <tr><td><code>src_in</code></td><td>5</td><td></td></tr>
    <tr><td><code>src_atop</code></td><td>9</td><td></td></tr>
    <tr><td><code>multiply</code></td><td>14</td><td></td></tr>
    <tr><td><code>screen</code></td><td>15</td><td></td></tr>
    </table>
              @attr name io.bitfountain.ashishpatel.sounddroid:backgroundTintMode
            */
            public static final int View_backgroundTintMode = 6;
            /**
              <p>This symbol is the offset where the {@link attr#paddingEnd}
              attribute's value can be found in the {@link #View} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:paddingEnd
            */
            public static final int View_paddingEnd = 3;
            /**
              <p>This symbol is the offset where the {@link attr#paddingStart}
              attribute's value can be found in the {@link #View} array.


              <p>Must be a dimension value, which is a floating point number appended with a unit such as "<code>14.5sp</code>".
    Available units are: px (pixels), dp (density-independent pixels), sp (scaled pixels based on preferred font size),
    in (inches), mm (millimeters).
    <p>This may also be a reference to a resource (in the form
    "<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>") or
    theme attribute (in the form
    "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>")
    containing a value of this type.
              @attr name io.bitfountain.ashishpatel.sounddroid:paddingStart
            */
            public static final int View_paddingStart = 2;
            /**
              <p>This symbol is the offset where the {@link attr#theme}
              attribute's value can be found in the {@link #View} array.


              <p>Must be a reference to another resource, in the form "<code>@[+][<i>package</i>:]<i>type</i>:<i>name</i></code>"
    or to a theme attribute in the form "<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>".
              @attr name io.bitfountain.ashishpatel.sounddroid:theme
            */
            public static final int View_theme = 4;
            /** Attributes that can be used with a ViewStubCompat.
               <p>Includes the following attributes:</p>
               <table>
               <colgroup align="left" />
               <colgroup align="left" />
               <tr><th>Attribute</th><th>Description</th></tr>
               <tr><td><code>{@link #ViewStubCompat_android_id android:id}</code></td><td></td></tr>
               <tr><td><code>{@link #ViewStubCompat_android_inflatedId android:inflatedId}</code></td><td></td></tr>
               <tr><td><code>{@link #ViewStubCompat_android_layout android:layout}</code></td><td></td></tr>
               </table>
               @see #ViewStubCompat_android_id
               @see #ViewStubCompat_android_inflatedId
               @see #ViewStubCompat_android_layout
             */
            public static final int[] ViewStubCompat = {
                0x010100d0, 0x010100f2, 0x010100f3
            };
            /**
              <p>This symbol is the offset where the {@link android.R.attr#id}
              attribute's value can be found in the {@link #ViewStubCompat} array.
              @attr name android:id
            */
            public static final int ViewStubCompat_android_id = 0;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#inflatedId}
              attribute's value can be found in the {@link #ViewStubCompat} array.
              @attr name android:inflatedId
            */
            public static final int ViewStubCompat_android_inflatedId = 2;
            /**
              <p>This symbol is the offset where the {@link android.R.attr#layout}
              attribute's value can be found in the {@link #ViewStubCompat} array.
              @attr name android:layout
            */
            public static final int ViewStubCompat_android_layout = 1;
        };
    }
}
