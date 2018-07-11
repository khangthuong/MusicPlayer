package com.example.khang.musicplayer;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import 	android.support.v7.app.NotificationCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.khang.musicplayer.util.MusicPlayer;

import java.io.IOException;

import static com.example.khang.musicplayer.MainActivity.isRepeatAll;
import static com.example.khang.musicplayer.MainActivity.isShuffleOn;
import static com.example.khang.musicplayer.util.MusicPlayer.ACTION_PAUSE;
import static com.example.khang.musicplayer.util.MusicPlayer.ACTION_PLAY;
import static com.example.khang.musicplayer.util.MusicPlayer.ACTION_PLAY_SHUFFLE;
import static com.example.khang.musicplayer.util.MusicPlayer.ACTION_PLAY_TO_END_SONG_LIST;
import static com.example.khang.musicplayer.util.MusicPlayer.NOTIFICATION_ID;
import static com.example.khang.musicplayer.util.MusicPlayer.NOTIFICATION_NEXT;
import static com.example.khang.musicplayer.util.MusicPlayer.NOTIFICATION_PAUSE;
import static com.example.khang.musicplayer.util.MusicPlayer.NOTIFICATION_PLAY;
import static com.example.khang.musicplayer.util.MusicPlayer.NOTIFICATION_PREVIOUS;
import static com.example.khang.musicplayer.util.MusicPlayer.NOTIFICATION_STOP;
import static com.example.khang.musicplayer.util.MusicPlayer.PLAY_NEW_AUDIO;
import static com.example.khang.musicplayer.util.MusicPlayer.milliSecondsToTimer;

/**
 * Created by sev_user on 9/20/2017.
 */

public class MediaPlayerService extends Service implements
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener,
        SeekBar.OnSeekBarChangeListener {

    private final String TAG = "MediaPlayerService";
    private final IBinder iBinder = new LocalBinder();
    private MediaPlayer mediaPlayer;
    private String mediaFile;
    private String songSinger;
    private String songName;
    private String songAlbum;
    private String songThumbUri;
    private long duaraion;
    private int resumePosition;
    private boolean isStopMedia = true;
    private boolean isMediaPlayerStart = false;
    private AudioManager audioManager;
    private AudioAttributes audioAttributes;
    private SeekBar seekBar;
    private TextView currentTimetxt;
    private Handler seekHandler;
    //Handle incoming phone calls
    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;
    //MediaSession
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        register_playNewAudio();
        registerBecomingNoisyReceiver();
        seekHandler = new Handler();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");
        return iBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");
        //Request audio focus
        if (requestAudioFocus() == false) {
            //Could not gain focus
            stopSelf();
        }

        callStateListener();

        if (mediaSessionManager == null) {
            try {
                initMediaSession();
                //initMediaPlayer();
            } catch (RemoteException e) {
                e.printStackTrace();
                stopSelf();
            }
        }

        //Handle Intent action from MediaSession.TransportControls
        handleIncomingActions(intent);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
        if (mediaPlayer != null) {
            if (!isStopMedia) {
                stopMedia();
            }
            mediaPlayer.release();
        }
        removeAudioFocus();
        //Disable the PhoneStateListener
        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        removeNotification();
        unregisterReceiver(playReceiver);
        unregisterReceiver(becomingNoisyReceiver);
        seekHandler.removeCallbacks(updateSeekBarAndTime);
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Log.d(TAG, "onCompletion(): " + "isRepeatOnce" + isRepeatAll + " isRepeatAll" + isRepeatAll
        + " isShuffleOn" + isShuffleOn);
        if (!isRepeatAll) {
            stopMedia();
            mediaPlayer.reset();
            resumePosition = 0;
            seekBar.setProgress(0);
            initMediaPlayer();
            updateMetaData();
            buildNotification(PlaybackStatus.PLAYING);
        } else if (isRepeatAll){
            if (isShuffleOn) {
                Intent broadcastIntent = new Intent(ACTION_PLAY_SHUFFLE);
                sendBroadcast(broadcastIntent);
            } else {
                transportControls.skipToNext();
            }
        } else {
            transportControls.skipToNext();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        //Invoked when there has been an error during an asynchronous operation
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d(TAG, "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d(TAG, "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d(TAG, "MEDIA ERROR UNKNOWN " + extra);
                break;
        }
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {

        if (seekBar == null) {
            return;
        }
        resumePosition = seekBar.getProgress();
        seekHandler.postDelayed(updateSeekBarAndTime, 1000);
        playMedia();
        Log.d(TAG, "onPrepared(): seekBar=" + seekBar + "resumePosition: " + resumePosition);
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
    }

    @Override
    public void onAudioFocusChange(int focusState) {
        //Invoked when the audio focus of the system is updated.
        Log.d(TAG, "onAudioFocusChange(): focusState = " + focusState);
        switch (focusState) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mediaPlayer == null) {
                    initMediaPlayer();
                }
                else if (!mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                }
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    private void initMediaPlayer() {
        Log.d(TAG, "initMediaPlayer()");
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        mediaPlayer = new MediaPlayer();
        //Set up MediaPlayer event listeners
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        //Reset so that the MediaPlayer is not pointing to another data source
        mediaPlayer.reset();
        audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        mediaPlayer.setAudioAttributes(audioAttributes);
        try {
            // Set the data source to the mediaFile location
            mediaPlayer.setDataSource(mediaFile);
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
        mediaPlayer.prepareAsync();
    }

    private void playMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
            isStopMedia = false;
            isMediaPlayerStart = true;
            Log.d(TAG, "playMedia() successful");
        }
    }

    private void stopMedia() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            seekHandler.removeCallbacks(updateSeekBarAndTime);
            mediaPlayer.stop();
            seekBar.setProgress(0);
            isStopMedia = true;
            isMediaPlayerStart = false;
            removeNotification();
            Log.d(TAG, "stopMedia() successful");
        }
    }

    private void pauseMedia() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            seekHandler.removeCallbacks(updateSeekBarAndTime);
            resumePosition = mediaPlayer.getCurrentPosition();
            isMediaPlayerStart = false;
            Log.d(TAG, "pauseMedia() successful");
        }
    }

    private void resumeMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(resumePosition);
            seekHandler.postDelayed(updateSeekBarAndTime, 1000);
            mediaPlayer.start();
            isStopMedia = false;
            isMediaPlayerStart = true;
            Log.d(TAG, "resumeMedia(): resumePosition=" + resumePosition);
        }
    }

    private boolean requestAudioFocus() {
        Log.d(TAG, "requestAudioFocus()");
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        /*audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setWillPauseWhenDucked(true)
                .build();

        int result = audioManager.requestAudioFocus(audioFocusRequest);*/
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //Focus gained
            return true;
        }
        //Could not gain focus
        return false;
    }

    private boolean removeAudioFocus() {
        Log.d(TAG, "removeAudioFocus()");
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                audioManager.abandonAudioFocus(this);
                //audioManager.abandonAudioFocusRequest(audioFocusRequest);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        resumePosition = i;
        currentTimetxt.setText(milliSecondsToTimer(i));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (mediaPlayer != null) {
            seekHandler.removeCallbacks(updateSeekBarAndTime);
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(resumePosition);
            seekHandler.postDelayed(updateSeekBarAndTime, 1000);
        }
    }

    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    private BroadcastReceiver playReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (mediaFile == null) {
                stopSelf();
            }
            try {
                //An audio file is passed to the service through putExtra();
                mediaFile = intent.getExtras().getString("new_media");
                songName = intent.getExtras().getString("song_name");
                songSinger = intent.getExtras().getString("song_singer");
                songAlbum = intent.getExtras().getString("song_album");
                songThumbUri = intent.getExtras().getString("song_thumbUri");
                duaraion = intent.getExtras().getLong("song_duration");
                seekBar.setMax((int)duaraion);
            } catch (NullPointerException e) {
                stopSelf();
            }

            if (action.equals(PLAY_NEW_AUDIO)) {
                Log.d(TAG, "playReceiver: play a new song");
                stopMedia();
                if (mediaPlayer != null) {
                    mediaPlayer.reset();
                }
                resumePosition = 0;
                seekBar.setProgress(0);
                initMediaPlayer();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);
            } else if (action.equals(ACTION_PLAY)) {
                Log.d(TAG, "playReceiver: play/pause a current song");
                if (mediaPlayer!= null && mediaPlayer.isPlaying()) {
                    pauseMedia();
                    buildNotification(PlaybackStatus.PAUSED);
                } else if (isStopMedia){
                    if (mediaPlayer != null) {
                        resumePosition = 0;
                        seekBar.setProgress(0);
                    }
                    initMediaPlayer();
                    updateMetaData();
                    buildNotification(PlaybackStatus.PLAYING);
                } else {
                    resumeMedia();
                    buildNotification(PlaybackStatus.PLAYING);
                }
            }
        }
    };

    private void register_playNewAudio() {
        //Register playNewMedia receiver
        Log.d(TAG, "register_playNewAudio()");
        IntentFilter filter = new IntentFilter();
        filter.addAction(PLAY_NEW_AUDIO);
        filter.addAction(ACTION_PLAY);
        filter.addAction(ACTION_PAUSE);
        registerReceiver(playReceiver, filter);
    }

    //Becoming noisy
    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //pause audio on ACTION_AUDIO_BECOMING_NOISY
            Log.d(TAG, "pause audio on ACTION_AUDIO_BECOMING_NOISY Receiver");
            pauseMedia();
            buildNotification(PlaybackStatus.PAUSED);
        }
    };

    private void registerBecomingNoisyReceiver() {
        //register after getting audio focus
        Log.d(TAG, "registerBecomingNoisyReceiver()");
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }

    //Handle incoming phone calls
    private void callStateListener() {
        // Get the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Starting listening for PhoneState changes
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                Log.d(TAG, "onCallStateChanged(): state=" + state);
                switch (state) {
                    //if at least one call exists or the phone is ringing
                    //pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mediaPlayer != null) {
                            pauseMedia();
                            ongoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Phone idle. Start playing.
                        if (mediaPlayer != null) {
                            if (ongoingCall) {
                                ongoingCall = false;
                                resumeMedia();
                            }
                        }
                        break;
                }
            }
        };
        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);
    }

    public void setSeekBar(SeekBar sk) {
        Log.d(TAG, "setSeekBar()");
        seekBar = sk;
        seekBar.setOnSeekBarChangeListener(this);
    }

    public void setCurrentTimeTxt(TextView txt) {
        Log.d(TAG, "setCurrentTimeTxt()");
        currentTimetxt = txt;
    }

    public PlaybackStatus getMediaPlayingStatus() {
        if (mediaPlayer != null && isMediaPlayerStart) {
            return PlaybackStatus.PLAYING;
        }
        return PlaybackStatus.PAUSED;
    }

    private Runnable updateSeekBarAndTime = new Runnable() {
        @Override
        public void run() {
            long currentTime = mediaPlayer.getCurrentPosition();
            currentTimetxt.setText(milliSecondsToTimer(currentTime));
            seekBar.setProgress((int)currentTime);
            seekHandler.postDelayed(this, 1000);
        }
    };

    private void initMediaSession () throws RemoteException {
        Log.d(TAG, "initMediaSession()");
        if (mediaSession != null) {
            return;
        }
        mediaSessionManager = (MediaSessionManager)getSystemService(MEDIA_SESSION_SERVICE);
        // Create a new media session
        mediaSession = new MediaSessionCompat(getApplicationContext(), "Music");
        // Get media session transport controls
        transportControls = mediaSession.getController().getTransportControls();
        // Set Mediaseeion to ready to receive media commands
        mediaSession.setActive(true);
        // Indicate that the media session hansles transport control commands
        // through its MediaSession.Callback
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // set mediaSession's Metadata
        //updateMetaData();

        // Attach Callback to receive MediaSession updates
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                Log.d(TAG, "mediaSession: onPlay()");
                super.onPlay();
                Intent broadcastIntent = new Intent(NOTIFICATION_PLAY);
                sendBroadcast(broadcastIntent);
            }

            @Override
            public void onPause() {
                Log.d(TAG, "mediaSession: onPause()");
                super.onPause();
                Intent broadcastIntent = new Intent(NOTIFICATION_PLAY);
                sendBroadcast(broadcastIntent);
            }

            @Override
            public void onSkipToNext() {
                Log.d(TAG, "mediaSession: onSkipToNext()");
                super.onSkipToNext();
                Intent broadcastIntent = new Intent(NOTIFICATION_NEXT);
                sendBroadcast(broadcastIntent);
                /*updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);*/
            }

            @Override
            public void onSkipToPrevious() {
                Log.d(TAG, "mediaSession: onSkipToPrevious()");
                super.onSkipToPrevious();
                Intent broadcastIntent = new Intent(NOTIFICATION_PREVIOUS);
                sendBroadcast(broadcastIntent);
                /*updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);*/
            }

            @Override
            public void onStop() {
                Log.d(TAG, "mediaSession: onStop()");
                super.onStop();
                stopMedia();
                Intent broadcastIntent = new Intent(NOTIFICATION_STOP);
                sendBroadcast(broadcastIntent);
            }

            @Override
            public void onSeekTo(long pos) {
                super.onSeekTo(pos);
            }
        });
    }

    public enum PlaybackStatus {
        PLAYING,
        PAUSED
    }

    private void buildNotification(PlaybackStatus playbackStatus) {
        Log.d(TAG, "buildNotification()");
        int notificationAction = android.R.drawable.ic_media_pause;//needs to be initialized
        PendingIntent play_pauseAction = null;

        //Build a new notification according to the current state of the MediaPlayer
        if (playbackStatus == PlaybackStatus.PLAYING) {
            notificationAction = R.drawable.ic_pause_30dp;
            //create the pause action
            play_pauseAction = playbackAction(1);
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            notificationAction = R.drawable.ic_play_arrow_30dp;
            //create the play action
            play_pauseAction = playbackAction(0);
        }

        Bitmap largeIcon = MusicPlayer.loadAlbumThumbnail(Uri.parse(songThumbUri), 150, getApplicationContext());

        Intent startIntent = new Intent(getApplicationContext(), MainActivity.class);
        if (playbackStatus == PlaybackStatus.PLAYING) {
            startIntent.putExtra("PlaybackStatus", "PLAYING");
        } else {
            startIntent.putExtra("PlaybackStatus", "PAUSE");
        }
        //startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 100, startIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Create a new Notification
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder)
                new NotificationCompat.Builder(getApplicationContext())
                .setWhen(System.currentTimeMillis())
                .setOngoing(true)
                //.setContentIntent(contentIntent)
                // Set the Notification style
                .setStyle(new NotificationCompat.MediaStyle()
                        // Attach our MediaSession token
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowCancelButton(true)
                        // Show our playback controls in the compact notification view.
                        .setShowActionsInCompactView(0, 1, 2, 3))
                // Set the Notification color
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                // Set the large and small icons
                .setLargeIcon(largeIcon)
                .setSmallIcon(R.drawable.ic_launcher)
                // Set Notification content information
                .setContentText(songSinger)
                .setContentTitle(songName)
                .setContentInfo(songAlbum)
                // Add playback actions
                .addAction(R.drawable.ic_skip_previous_30dp, "previous", playbackAction(3))
                .addAction(notificationAction, "pause", play_pauseAction)
                .addAction(R.drawable.ic_skip_next_30dp, "next", playbackAction(2))
                .addAction(R.drawable.ic_close_30dp, "next", playbackAction(4));

        //((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notificationBuilder.build());
        startForeground(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void removeNotification() {
        Log.d(TAG, "buildNotification()");
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
        stopForeground(true);
    }

    private PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent(this, MediaPlayerService.class);
        switch (actionNumber) {
            case 0:
                // Play
                playbackAction.setAction(NOTIFICATION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                // Pause
                playbackAction.setAction(NOTIFICATION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 2:
                // Next track
                playbackAction.setAction(NOTIFICATION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 3:
                // Previous track
                playbackAction.setAction(NOTIFICATION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 4:
                playbackAction.setAction(NOTIFICATION_STOP);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }

    private void updateMetaData() {
        Log.d(TAG, "updateMetaData()");
        Bitmap albumArt = MusicPlayer.loadAlbumThumbnail(Uri.parse(songThumbUri), 150, getApplicationContext()); //replace with medias albumArt
        // Update the current metadata
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, songSinger)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, songAlbum)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, songName)
                .build());
    }

    private void handleIncomingActions(Intent playbackAction) {
        if (playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(NOTIFICATION_PLAY)) {
            Log.d(TAG, "handleIncomingActions(): play");
            transportControls.play();
        } else if (actionString.equalsIgnoreCase(NOTIFICATION_PAUSE)) {
            Log.d(TAG, "handleIncomingActions(): pause");
            transportControls.pause();
        } else if (actionString.equalsIgnoreCase(NOTIFICATION_NEXT)) {
            Log.d(TAG, "handleIncomingActions(): next");
            transportControls.skipToNext();
        } else if (actionString.equalsIgnoreCase(NOTIFICATION_PREVIOUS)) {
            Log.d(TAG, "handleIncomingActions(): prev");
            transportControls.skipToPrevious();
        } else if (actionString.equalsIgnoreCase(NOTIFICATION_STOP)) {
            Log.d(TAG, "handleIncomingActions(): stop");
            transportControls.stop();
        }
    }
}



