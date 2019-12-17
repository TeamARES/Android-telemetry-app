package com.example.rovercontroller;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements IVLCVout.Callback {

    public final static String TAG = "MainActivity";
    private ImageButton up, left, right, down;
    private String mFilePath;
    private SurfaceView mSurface;
    private SurfaceHolder holder;
    private LibVLC libVLC;
    private MediaPlayer mediaPlayer = null;
    private int mVideoWidth;
    private int mVideoHeight;
    private String ipAdd;
    private Client client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        up = findViewById(R.id.arrow_up);
        left = findViewById(R.id.arrow_left);
        right = findViewById(R.id.arrow_right);
        down = findViewById(R.id.arrow_down);

        EnterIPAddress();
    }

    private void EnterIPAddress() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("IP ADDRESS");
        LayoutInflater inflater = getLayoutInflater();
        final View view = inflater.inflate(R.layout.ipadd_layout, null);
        builder.setView(view)
        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                EditText text = view.findViewById(R.id.ipadd_et);
                ipAdd = text.getText().toString();
                setUp();
            }
        }).create();
        builder.show();
    }

    private void setUp() {

        client = new Client(ipAdd);
        client.OpenConnection();

        up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                client.UpDownClick(1);
            }
        });

        left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                client.LeftRightClick(1);
            }
        });

        right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                client.LeftRightClick(-1);
            }
        });

        down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                client.UpDownClick(-1);
            }
        });

        mFilePath = "rtmp://"+ ipAdd + "/live/";
        mSurface = findViewById(R.id.surface);
        holder = mSurface.getHolder();

        createPlayer(mFilePath);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setSize(mVideoWidth, mVideoHeight);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
        client.CloseConnection();
    }

    private void setSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        if (mVideoWidth * mVideoHeight <= 1)
            return;
        if (holder == null || mSurface == null)
            return;
        int w = getWindow().getDecorView().getWidth();
        int h = getWindow().getDecorView().getHeight();
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (w > h && isPortrait || w < h && !isPortrait) {
            int i = w;
            w = h;
            h = i;
        }
        float videoAR = (float) mVideoWidth / (float) mVideoHeight;
        float screenAR = (float) w / (float) h;
        if (screenAR < videoAR)
            h = (int) (w / videoAR);
        else
            w = (int) (h * videoAR);
        holder.setFixedSize(mVideoWidth, mVideoHeight);
        ViewGroup.LayoutParams lp = mSurface.getLayoutParams();
        lp.width = w;
        lp.height = h;
        mSurface.setLayoutParams(lp);
        mSurface.invalidate();
    }

    private void createPlayer(String media) {
        releasePlayer();
        try {
            if (media.length() > 0) {
                Toast.makeText(this, media, Toast.LENGTH_SHORT).show();
            }

            // Create LibVLC
            ArrayList<String> options = new ArrayList<>();

            options.add("--aout=opensles");
            options.add("--audio-time-stretch");
            options.add("-vvv");
            libVLC = new LibVLC(this, options);
            holder.setKeepScreenOn(true);

            // Creating media player
            mediaPlayer = new MediaPlayer(libVLC);
            mediaPlayer.setEventListener(mPlayerListener);

            // Setting up video output
            final IVLCVout vout = mediaPlayer.getVLCVout();
            vout.setVideoView(mSurface);
            vout.addCallback(this);
            vout.attachViews();

            Media m = new Media(libVLC, Uri.parse(media));
            mediaPlayer.setMedia(m);
            mediaPlayer.play();
        } catch (Exception e) {
            Toast.makeText(this, "Error in creating player!", Toast
                    .LENGTH_SHORT).show();
        }
    }

    private MediaPlayer.EventListener mPlayerListener = new MyPlayerListener(this);

    private static class MyPlayerListener implements MediaPlayer.EventListener {
        private WeakReference<MainActivity> mOwner;

        public MyPlayerListener(MainActivity owner) {
            mOwner = new WeakReference<MainActivity>(owner);
        }

        @Override
        public void onEvent(MediaPlayer.Event event) {
            MainActivity player = mOwner.get();
            switch (event.type) {
                case MediaPlayer.Event.EndReached:
                    Log.d(TAG, "MediaPlayerEndReached");
                    player.releasePlayer();
                    break;
                case MediaPlayer.Event.Playing:
                case MediaPlayer.Event.Paused:
                case MediaPlayer.Event.Stopped:
                default:
                    break;
            }
        }
    }

    private void releasePlayer() {
        if (libVLC == null) {
            return;
        }
        mediaPlayer.stop();
        final IVLCVout vout = mediaPlayer.getVLCVout();
        vout.removeCallback(this);
        vout.detachViews();
        holder = null;
        libVLC.release();
        libVLC = null;

        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    @Override
    public void onNewLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        if (width * height == 0)
            return;
        // store video size
        mVideoWidth = width;
        mVideoHeight = height;
        setSize(mVideoWidth, mVideoHeight);
    }

    @Override
    public void onSurfacesCreated(IVLCVout vlcVout) {

    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vlcVout) {

    }

    @Override
    public void onHardwareAccelerationError(IVLCVout vlcVout) {
        Log.e(TAG, "Error with hardware acceleration");
        this.releasePlayer();
        Toast.makeText(this, "Error with hardware acceleration", Toast.LENGTH_LONG).show();
    }

}
