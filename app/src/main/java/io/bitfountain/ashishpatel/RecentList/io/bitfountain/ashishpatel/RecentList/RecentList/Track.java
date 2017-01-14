package io.bitfountain.ashishpatel.RecentList.io.bitfountain.ashishpatel.RecentList.RecentList;

import com.google.gson.annotations.SerializedName;

/**
 * Created by ashishpatel on 2016-03-24.
 */
public class Track {
    @SerializedName("title")
    private String mTitle;

    @SerializedName("stream_url")
    private String mStreamURL;

    @SerializedName("id")
    private int mID;

    @SerializedName("artwork_url")
    private String artworkURL;


    public String getTitle() {
        return mTitle;
    }


    public int getID() {
        return mID;
    }

    public String getStreamURL(){ return mStreamURL; }

    public String getArtworkURL() {
        return artworkURL;
    }

    public String getAvatarURL() {
        String avatarURL = artworkURL;
        if (avatarURL != null) {
            avatarURL = artworkURL.replace("large", "tiny");
        }
        return avatarURL;
    }
}

