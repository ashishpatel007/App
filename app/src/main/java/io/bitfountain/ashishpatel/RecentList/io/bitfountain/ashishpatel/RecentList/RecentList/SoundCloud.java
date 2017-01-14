package io.bitfountain.ashishpatel.RecentList.io.bitfountain.ashishpatel.RecentList.RecentList;

import retrofit.RestAdapter;

/**
 * Created by ashishpatel on 2016-11-17.
 */

public class SoundCloud {

    private static final String API_URL = "http://api.soundcloud.com";

    private static final RestAdapter REST_ADAPTER = new RestAdapter.Builder()
            .setLogLevel(RestAdapter.LogLevel.FULL)
            .setEndpoint("http://api.soundcloud.com")
            .build();

    private static final SoundCloudService SERVICE = REST_ADAPTER.create(SoundCloudService.class);

    public static SoundCloudService getService(){
        return SERVICE;

    }
}

