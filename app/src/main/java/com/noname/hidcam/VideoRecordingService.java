package com.noname.hidcam;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VideoRecordingService extends Service {

    private static final int NOTIFICATION_ID = 1;
    private static final String LOG_TAG = "myLog";
    private CameraManager cameraManager;
    private String cameraId;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private boolean isRecording = false;
    HandlerThread handlerThread;
    Handler handler;

    private static Camera mServiceCamera;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;


    private CameraManager mCameraManager = null;
    private final int CAMERA_BACK = 0;
    private final int CAMERA_FRONT = 1;
    MediaRecorder mMediaRecorder;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(LOG_TAG, "onCreate()");


    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification());


        // Start video recording
        //startVideoRecording(getApplicationContext());
        try {
            setUpMediaRecorder();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Stop video recording
        stopRecording();
        super.onDestroy();

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startRecording(Context context) {

    }


    private void setUpMediaRecorder() throws IOException {
        mServiceCamera = Camera.open();

        mServiceCamera.unlock();

        mMediaRecorder = new MediaRecorder();

        mMediaRecorder.setCamera(mServiceCamera);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(640, 480);


       // mMediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);
        //mMediaRecorder.setAudioSamplingRate(profile.audioSampleRate);
        mMediaRecorder.setOutputFile(createVideoFile("front").getAbsolutePath());

        try {
            mMediaRecorder.prepare();
            mMediaRecorder.start();
            Log.i(LOG_TAG, " запустили медиа рекордер");

        } catch (Exception e) {
            Log.i(LOG_TAG, "не запустили медиа рекордер " + e);
        }


    }

    private void stopRecording() {
        if (!isRecording) {
            return;
        }
        isRecording = false;
        try {
            captureSession.stopRepeating();
            captureSession.abortCaptures();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        mMediaRecorder.stop();
        mMediaRecorder.reset();
        mMediaRecorder.release();
        mMediaRecorder = null;
        mServiceCamera.release();
        mServiceCamera = null;
        //mMediaRecorder.stop();
        //mMediaRecorder.reset();
        //mMediaRecorder.release();
        //mMediaRecorder.stop();
        stopForeground(true);
        stopSelf();


    }

    private File createVideoFile(String position) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());

        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath());

        //File storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);

        return File.createTempFile("VIDEO_" + position + "_" + timeStamp,".MP4", storageDir);
    }

    private Notification buildNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel_id")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Video Recording")
                .setContentText("Recording in progress")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        NotificationChannel channel = new NotificationChannel("channel_id", "Video Recording", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Video Recording in progress");
        channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        return builder.build();
    }
}