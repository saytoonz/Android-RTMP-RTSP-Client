package com.sayt.android_rtmp_rtsp_client;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.pedro.encoder.input.decoder.AudioDecoderInterface;
import com.pedro.encoder.input.decoder.VideoDecoderInterface;
import com.pedro.encoder.input.video.CameraOpenException;
import com.pedro.rtmp.utils.ConnectCheckerRtmp;
import com.pedro.rtplibrary.rtmp.RtmpCamera1;
import com.sayt.android_rtmp_rtsp_client.utils.PathUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ConnectCheckerRtmp, AudioDecoderInterface, VideoDecoderInterface, SurfaceHolder.Callback, View.OnClickListener {
    private final String[] PERMISSIONS = {android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private final String[] PERMISSIONS_A_13 = {android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.CAMERA, Manifest.permission.POST_NOTIFICATIONS};


    private RtmpCamera1 rtmpCamera1;
    private Button button;
    private Button bRecord;

    private String currentDateAndTime = "";

    private File folder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        requestPermissions();
        folder = PathUtils.getRecordPath();


        SurfaceView surfaceView = findViewById(R.id.surfaceView);
        button = findViewById(R.id.button);
        button.setOnClickListener(this);
        bRecord = findViewById(R.id.record);
        bRecord.setOnClickListener(this);
        Button switchCamera = findViewById(R.id.switch_camera);
        switchCamera.setOnClickListener(this);
        rtmpCamera1 = new RtmpCamera1(surfaceView, this);
        rtmpCamera1.setReTries(10);
        surfaceView.getHolder().addCallback(this);
    }


    private void toggleAudio() {
        if (!rtmpCamera1.isAudioMuted()) {
//            item.setIcon(getResources().getDrawable(R.drawable.icon_microphone_off));
            rtmpCamera1.disableAudio();
        } else {
//            item.setIcon(getResources().getDrawable(R.drawable.icon_microphone));
            rtmpCamera1.enableAudio();
        }
    }


    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasPermissions(this)) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_A_13, 1);
            }
        } else {
            if (!hasPermissions(this)) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, 1);
            }
        }
    }

    private boolean hasPermissions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return hasPermissions(context, PERMISSIONS_A_13);
        } else {
            return hasPermissions(context, PERMISSIONS);
        }
    }

    private boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }


    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        rtmpCamera1.startPreview();
        if (!rtmpCamera1.isLanternEnabled()) {
            rtmpCamera1.switchCamera();
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        if (rtmpCamera1.isRecording()) {
            rtmpCamera1.stopRecord();
            PathUtils.updateGallery(this, folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
            bRecord.setText("Record");
            Toast.makeText(this, "file " + currentDateAndTime + ".mp4 saved in " + folder.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            currentDateAndTime = "";
        }
        if (rtmpCamera1.isStreaming()) {
            rtmpCamera1.stopStream();
            button.setText("Stop");
        }
        rtmpCamera1.stopPreview();
    }


    @Override
    public void onAuthErrorRtmp() {
        runOnUiThread(() -> {
            Toast.makeText(MainActivity.this, "Auth error", Toast.LENGTH_SHORT).show();
            rtmpCamera1.stopStream();
            button.setText("Start");
        });
    }

    @Override
    public void onAuthSuccessRtmp() {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Auth success", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onConnectionFailedRtmp(@NonNull String reason) {
        runOnUiThread(() -> {
            if (rtmpCamera1.reTry(5000, reason, null)) {
                Toast.makeText(MainActivity.this, "Retry", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Connection failed. " + reason, Toast.LENGTH_SHORT).show();
                rtmpCamera1.stopStream();
                button.setText("Start");
            }
        });
    }

    @Override
    public void onConnectionStartedRtmp(@NonNull String s) {

    }

    @Override
    public void onConnectionSuccessRtmp() {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Connection success", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDisconnectRtmp() {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onNewBitrateRtmp(long l) {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button:
                if (!rtmpCamera1.isStreaming()) {
                    if (rtmpCamera1.isRecording() || rtmpCamera1.prepareAudio() && rtmpCamera1.prepareVideo()) {
                        button.setText("Stop");
                        rtmpCamera1.startStream("rtmp://192.168.0.151:1939/live/test");
                    } else {
                        Toast.makeText(this, "Error preparing stream, This device cant do it", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    button.setText("Start");
                    rtmpCamera1.stopStream();
                }
                break;
            case R.id.switch_camera:
                try {
                    rtmpCamera1.switchCamera();
                } catch (CameraOpenException e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.record:
//                toggleAudio();
                if (!rtmpCamera1.isRecording()) {
                    try {
                        if (!folder.exists()) {
                            folder.mkdir();
                        }
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
                        currentDateAndTime = sdf.format(new Date());
                        if (!rtmpCamera1.isStreaming()) {
                            if (rtmpCamera1.prepareAudio() && rtmpCamera1.prepareVideo()) {
                                rtmpCamera1.startRecord(folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
                                bRecord.setText("Stop Rec");
                                Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Error preparing stream, This device cant do it", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            rtmpCamera1.startRecord(folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
                            bRecord.setText("Stop Rec");
                            Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        rtmpCamera1.stopRecord();
                        PathUtils.updateGallery(this, folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
                        bRecord.setText("Record");
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    rtmpCamera1.stopRecord();
                    PathUtils.updateGallery(this, folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
                    bRecord.setText("Record");
                    Toast.makeText(this, "file " + currentDateAndTime + ".mp4 saved in " + folder.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onVideoDecoderFinished() {

    }

    @Override
    public void onAudioDecoderFinished() {

    }
}