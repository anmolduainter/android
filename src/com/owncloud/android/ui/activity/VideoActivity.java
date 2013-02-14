/* ownCloud Android client application
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.MediaController;
import android.widget.VideoView;

import com.owncloud.android.AccountUtils;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.media.MediaService;

/**
 *  Activity implementing a basic video player.
 * 
 *  Used as an utility to preview video files contained in an ownCloud account.
 *  
 *  Currently, it always plays in landscape mode, full screen. When the playback ends,
 *  the activity is finished. 
 *  
 *  @author David A. Velasco
 */
public class VideoActivity extends Activity implements OnCompletionListener, OnPreparedListener, OnErrorListener {

    /** Key to receive an {@link OCFile} to play as an extra value in an {@link Intent} */
    public static final String EXTRA_FILE = "FILE";
    /** Key to receive the ownCloud {@link Account} where the file to play is saved as an extra value in an {@link Intent} */
    public static final String EXTRA_ACCOUNT = "ACCOUNT";
    
    private static final String TAG = null;

    private OCFile mFile;                       // video file to play
    private Account mAccount;                   // ownCloud account holding mFile
    private VideoView mVideoPlayer;             // view to play the file; both performs and show the playback
    private MediaController mMediaController;   // panel control used by the user to control the playback
          
    /** 
     *  Called when the activity is first created.
     *  
     *  Searches for an {@link OCFile} and ownCloud {@link Account} holding it in the starting {@link Intent}.
     *  
     *  The {@link Account} is unnecessary if the file is downloaded; else, the {@link Account} is used to 
     *  try to stream the remote file - TODO get the streaming works
     * 
     *  {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.video_layout);
    
        mFile = getIntent().getExtras().getParcelable(EXTRA_FILE);
        mAccount = getIntent().getExtras().getParcelable(EXTRA_ACCOUNT);
          
        mVideoPlayer = (VideoView) findViewById(R.id.videoPlayer);

        // set listeners to get more contol on the playback
        mVideoPlayer.setOnPreparedListener(this);
        mVideoPlayer.setOnCompletionListener(this);
        mVideoPlayer.setOnErrorListener(this);
          
        // keep the screen on while the playback is performed (prevents screen off by battery save)
        mVideoPlayer.setKeepScreenOn(true);
        
        if (mFile != null) {
            if (mFile.isDown()) {
                mVideoPlayer.setVideoPath(mFile.getStoragePath());
                
            } else if (mAccount != null) {
                String url = AccountUtils.constructFullURLForAccount(this, mAccount) + mFile.getRemotePath();
                mVideoPlayer.setVideoURI(Uri.parse(url));
                
            } else {
                onError(null, MediaService.OC_MEDIA_ERROR, R.string.media_err_no_account);
            }
            
            // create and prepare control panel for the user
            mMediaController = new MediaController(this);
            mMediaController.setMediaPlayer(mVideoPlayer);
            mMediaController.setAnchorView(mVideoPlayer);
            mVideoPlayer.setMediaController(mMediaController);
            
        } else {
            onError(null, MediaService.OC_MEDIA_ERROR, R.string.media_err_nothing_to_play);
        }
    }    
    
    
    /** 
     * Called when the file is ready to be played.
     * 
     * Just starts the playback.
     * 
     * @param   mp    {@link MediaPlayer} instance performing the playback.
     */
    @Override
    public void onPrepared(MediaPlayer vp) {
        mVideoPlayer.start();
        mMediaController.show(5000);  
    }
    
    
    /**
     * Called when the file is finished playing.
     *  
     * Finishes the activity.
     * 
     * @param   mp    {@link MediaPlayer} instance performing the playback.
     */
    @Override
    public void onCompletion(MediaPlayer  mp) {
        this.finish(); 
    }
    
    
    /**
     * Called when an error in playback occurs.
     * 
     * @param   mp      {@link MediaPlayer} instance performing the playback.
     * @param   what    Type of error
     * @param   extra   Extra code specific to the error
     */
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "Error in video playback, what = " + what + ", extra = " + extra);
        
        if (mMediaController != null) {
            mMediaController.hide();
        }
        
        if (mVideoPlayer.getWindowToken() != null) {
            String message = MediaService.getMessageForMediaError(this, what, extra);
            new AlertDialog.Builder(this)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.VideoView_error_button,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    VideoActivity.this.onCompletion(null);
                                }
                            })
                    .setCancelable(false)
                    .show();
        }
        return true;
    }
    
    
    /**  
     * Screen touches trigger the appearance of the control panel for a limited time.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean onTouchEvent (MotionEvent ev){ 
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mMediaController.show(MediaService.MEDIA_CONTROL_SHORT_LIFE);
            return true;        
        } else {
            return false;
        }
    }


}