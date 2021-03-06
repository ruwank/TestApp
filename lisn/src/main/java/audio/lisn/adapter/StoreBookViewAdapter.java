/*
 * Copyright (C) 2015 Antonio Leiva
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package audio.lisn.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.List;

import audio.lisn.R;
import audio.lisn.model.AudioBook;
import audio.lisn.util.AppUtils;
import audio.lisn.util.ConnectionDetector;
import audio.lisn.util.CustomTypeFace;

public class StoreBookViewAdapter extends RecyclerView.Adapter<StoreBookViewAdapter.ViewHolder> implements Runnable {

    private List<AudioBook> items;
    private StoreBookSelectListener listener;
    MediaPlayer mediaPlayer = null;
    ConnectionDetector connectionDetector;
    AudioBook selectedAudioBook;
    private boolean isPlayingPreview,isLoadingPreview;
    int selectedBookIndex;
    private Context context;


    String leftTime;

    public StoreBookViewAdapter(Context context, List<AudioBook> items) {
        this.items = items;
        this.context=context;
        connectionDetector = new ConnectionDetector(context);

    }

    public void setStoreBookSelectListener(StoreBookSelectListener onItemClickListener) {
        this.listener = onItemClickListener;
    }

    @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.store_book_view, parent, false);


        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                releaseMediaPlayer();

                if (listener != null) {

                    new Handler().postDelayed(new Runnable() {
                        @Override public void run() {
                            listener.onStoreBookSelect(v,(AudioBook) v.getTag(),AudioBook.SelectedAction.ACTION_DETAIL);
                        }
                    }, 200);
                }
            }
        });


        return new ViewHolder(view);
    }



    @Override public void onBindViewHolder(final ViewHolder holder, int position) {
        AudioBook book = items.get(position);
        selectedBookIndex=position;

        if((isLoadingPreview || isPlayingPreview) && selectedAudioBook.getBook_id().equalsIgnoreCase(book.getBook_id()) ){
            holder.previewLayout.setVisibility(View.VISIBLE);
            holder.playButton.setImageResource(R.drawable.btn_play_pause);

            if(isPlayingPreview){
                holder.spinner.setVisibility(View.INVISIBLE);
                holder.previewLabel.setText("Preview");
                holder.timeLabel.setText(leftTime);

            }else{
                holder.spinner.setVisibility(View.VISIBLE);
                holder.previewLabel.setText("Loading...");
                holder.timeLabel.setText("");
            }
        }else{
            holder.previewLayout.setVisibility(View.GONE);
            holder.playButton.setImageResource(R.drawable.btn_play_start);
        }
        if(book.getLanguageCode()== AudioBook.LanguageCode.LAN_SI){
            holder.title.setTypeface(CustomTypeFace.getSinhalaTypeFace(holder.title.getContext()));
            holder.author.setTypeface(CustomTypeFace.getSinhalaTypeFace(holder.author.getContext()));
        }else{
            holder.title.setTypeface(CustomTypeFace.getEnglishTypeFace(holder.title.getContext()));
            holder.author.setTypeface(CustomTypeFace.getEnglishTypeFace(holder.author.getContext()));
        }
        holder.title.setText(book.getTitle());
        holder.author.setText(book.getAuthor());
        String priceText="Free";
        if( Float.parseFloat(book.getPrice())>0 ){
            priceText="LKR "+book.getPrice();
        }
        holder.price.setText(priceText);
        if(Float.parseFloat(book.getRate())>-1){
            holder.ratingBar.setRating(Float.parseFloat(book.getRate()));
        }
        holder.ratingBar.setIsIndicator(true);
        holder.thumbNail.setImageBitmap(null);
        Picasso.with(holder.thumbNail.getContext())
                .load(book.getCover_image())
                .placeholder(R.drawable.audiobook_placeholder)
                .into(holder.thumbNail);

        holder.optionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                popupMenu.inflate(R.menu.store_book_menu);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        AudioBook audioBook = items.get(selectedBookIndex);

                        switch (item.getItemId()) {
                            case R.id.action_preview:
                                playButtonPressed((AudioBook)holder.itemView.getTag());
                                break;
                            case R.id.action_purchase:
                                releaseMediaPlayer();
                                if (listener != null) {
                                    new Handler().postDelayed(new Runnable() {
                                        @Override public void run() {
                                            listener.onStoreBookSelect(holder.itemView,(AudioBook) holder.itemView.getTag(),AudioBook.SelectedAction.ACTION_DETAIL);
                                        }
                                    }, 200);
                                }
                                break;
                            case R.id.action_detail:
                                releaseMediaPlayer();
                                if (listener != null) {
                                    new Handler().postDelayed(new Runnable() {
                                        @Override public void run() {
                                            listener.onStoreBookSelect(holder.itemView,(AudioBook) holder.itemView.getTag(),AudioBook.SelectedAction.ACTION_DETAIL);
                                        }
                                    }, 200);
                                }
                                break;
                            case R.id.action_play:
                                releaseMediaPlayer();
                                if (listener != null) {
                                    new Handler().postDelayed(new Runnable() {
                                        @Override public void run() {
                                            listener.onStoreBookSelect(holder.itemView,(AudioBook) holder.itemView.getTag(),AudioBook.SelectedAction.ACTION_PLAY);
                                        }
                                    }, 200);
                                }
                                break;
                            default:
                                break;

                        }

                        return true;
                    }
                });
                popupMenu.show();



            }
        });

        holder.playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playButtonPressed((AudioBook)holder.itemView.getTag());

            }
        });
        holder.itemView.setTag(book);
    }

    @Override public int getItemCount() {
        return items.size();
    }


    protected static class ViewHolder extends RecyclerView.ViewHolder {

        public ImageView thumbNail;
        public TextView title, author,price;
        public RatingBar ratingBar;
        public ImageButton optionButton,playButton;
        public RelativeLayout previewLayout;
        public TextView previewLabel,timeLabel;
        public ProgressBar spinner;



        public ViewHolder(View itemView) {
            super(itemView);

            thumbNail=(ImageView) itemView
                    .findViewById(R.id.book_cover_thumbnail);
            title= (TextView) itemView.findViewById(R.id.book_title);
            author= (TextView) itemView.findViewById(R.id.book_author);
            price= (TextView) itemView.findViewById(R.id.book_price);
            ratingBar=(RatingBar)itemView.findViewById(R.id.rating_bar);
            optionButton=(ImageButton)itemView.findViewById(R.id.btn_action);
            playButton=(ImageButton)itemView.findViewById(R.id.playButton);
            previewLayout=(RelativeLayout)itemView.findViewById(R.id.preview_layout);
            previewLabel=(TextView)itemView.findViewById(R.id.preview_label);
            timeLabel=(TextView)itemView.findViewById(R.id.time_label);
            spinner = (ProgressBar)itemView.findViewById(R.id.progressBar);



        }
    }
    private void playButtonPressed(AudioBook audioBook){
        if (audioBook.getPreview_audio() !=null && (audioBook.getPreview_audio().length()>0)) {
            boolean stopPlayer = false;
            if(selectedAudioBook != null){
                if((isLoadingPreview || isPlayingPreview ) && (audioBook.getBook_id().equalsIgnoreCase(selectedAudioBook.getBook_id()))){
                    stopPlayer=true;
                }
            }
            selectedAudioBook=audioBook;
            if(stopPlayer){
                if(mediaPlayer.isPlaying()){
                    mediaPlayer.stop();
                    new Thread(this).interrupt();
                }

                mediaPlayer.reset();
                isPlayingPreview=false;
                isLoadingPreview=false;

            }else{
                playPreview();
            }

            // AppController.getInstance().playPreviewFile(audioBook.getPreview_audio());
        }else{
            if(selectedAudioBook != null && isPlayingPreview){
                if(mediaPlayer.isPlaying()){
                    mediaPlayer.stop();
                    new Thread(this).interrupt();
                }

                mediaPlayer.reset();
                isPlayingPreview=false;
                isLoadingPreview=false;

            }
        }
        notifyDataSetChanged();
    }
    @Override
    public void run() {
        int currentPosition = 0;//
        while (mediaPlayer != null && mediaPlayer.isPlaying() && currentPosition < mediaPlayer.getDuration()) {
            try {
                Thread.sleep(1000);
                currentPosition = mediaPlayer.getCurrentPosition();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            updateTimer();
        }
    }

    private void updateTimer() {
        int currentPosition = mediaPlayer.getCurrentPosition();
        int totalDuration =mediaPlayer.getDuration();
        leftTime= AppUtils.milliSecondsToTimer(totalDuration - currentPosition);
        // Get a handler that can be used to post to the main thread
        Handler mainHandler = new Handler(context.getMainLooper());

        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();

            } // This is your code
        };
        mainHandler.post(timerRunnable);
    }
    private void playPreview( ) {
        isLoadingPreview=true;
        isPlayingPreview=false;

        if (connectionDetector.isConnectingToInternet()) {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
            }
            if(mediaPlayer.isPlaying()){
                mediaPlayer.stop();
                new Thread(this).interrupt();
            }

            mediaPlayer.reset();

            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                mediaPlayer.setDataSource(selectedAudioBook.getPreview_audio());
            }catch (IOException e) {
                Log.v("playPreview", "IOException" + e.getMessage());

                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    isPlayingPreview=true;
                    isLoadingPreview=false;
                    startTimer();
                    mp.start();
                    notifyDataSetChanged();
                }
            });
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                public boolean onError(MediaPlayer mp, int what, int extra) {

                    return false;
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    isPlayingPreview=false;
                    isLoadingPreview=false;
                    stopTimer();
                    notifyDataSetChanged();
                }
            });
            mediaPlayer.prepareAsync(); // prepare async to not block main


        } else {

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("No Internet Connection").setPositiveButton(
                    "OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }

    }

    private void startTimer(){
        new Thread(this).start();
    }
    private void stopTimer(){
        new Thread(this).interrupt();
    }

    private void releaseMediaPlayer(){
        if (mediaPlayer != null){
            if(mediaPlayer.isPlaying())
                mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer=null;

        }
    }
    public interface StoreBookSelectListener
    {
        public void onStoreBookSelect(View view, AudioBook audioBook,AudioBook.SelectedAction btnIndex);
    }


}
