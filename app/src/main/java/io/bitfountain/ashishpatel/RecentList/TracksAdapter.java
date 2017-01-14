package io.bitfountain.ashishpatel.RecentList;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import io.bitfountain.ashishpatel.RecentList.io.bitfountain.ashishpatel.RecentList.RecentList.Track;
//import io.bitfountain.apatel.sounddroid.R;
import io.bitfountain.ashishpatel.sounddroid.R;

/**
 * Created by ashishpatel on 2016-11-17.
 */

public class TracksAdapter extends RecyclerView.Adapter<TracksAdapter.ViewHolder> {

    //Cache the view in ViewHolder
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView titleTextView;
        private final ImageView thumbImageView;

        ViewHolder(View v) {

            super(v);
            titleTextView = (TextView) v.findViewById(R.id.track_title);
            thumbImageView = (ImageView)v.findViewById(R.id.track_thumbnail);
            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if(mOnItemClickListener != null){
                mOnItemClickListener.onItemClick(null,v,getPosition(),0);
            }
        }
    }

    private List<Track> mTracks;
    private Context mContext;
    private AdapterView.OnItemClickListener mOnItemClickListener;

    TracksAdapter(Context context, List<Track> tracks){

        mTracks = tracks;
        mContext = context;

    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    @Override
    public int getItemCount() {

        return mTracks.size();
    }

    //Compiles the ViewHolder data here
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {
        Track track = mTracks.get(i);
        viewHolder.titleTextView.setText(track.getTitle());
        Picasso.with(mContext).load(track.getAvatarURL()).into(viewHolder.thumbImageView);

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.track_row, viewGroup, false);

        return new ViewHolder(v);

    }


}
