package io.bitfountain.ashishpatel.RecentList.io.bitfountain.ashishpatel.RecentList.RecentList;

import java.util.List;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Query;


/**
 * Created by ashishpatel on 2016-11-16.
 * We use the retrofit library which automatically uses GSON for the queries
 */

public interface SoundCloudService {

    String CLIENT_ID = "ef3a47ab5b2575aa7dc363cc903bdffd";

    @GET("/tracks?client_id="+CLIENT_ID)
    public void searchSongs(@Query("q") String query, Callback<List<Track>> cb); //In Soundcloud it says to use "q" as the parameter

    @GET("/tracks?client_id="+CLIENT_ID)
    public void getRecentSongs(@Query("created_at[from]") String date, Callback<List<Track>> cb); //In Soundcloud it says to use "q" as the parameter


}
