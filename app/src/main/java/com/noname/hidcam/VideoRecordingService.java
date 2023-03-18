package com.noname.hidcam;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.view.Surface;

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

    private CameraManager mCameraManager = null;
    private final int CAMERA_BACK = 0;
    private final int CAMERA_FRONT = 1;
    MediaRecorder mMediaRecorder;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.e(LOG_TAG, "onCreate()");

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = cameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification());


        // Start video recording
        //startVideoRecording(getApplicationContext());
        startRecording(getApplicationContext());

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
        if (isRecording) {
            return;
        }
        isRecording = true;
        handlerThread = new HandlerThread("VideoRecordingThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        try {
            mMediaRecorder = new MediaRecorder(context);

            setUpMediaRecorder();

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Log.e(LOG_TAG, "NO checkSelfPermission");

                return;
            }
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    try {
                        List<Surface> surfaces = new ArrayList<>();

                        // Add surface for camera preview
                        SurfaceTexture surfaceTexture = new SurfaceTexture(0);
                        Surface previewSurface = new Surface(surfaceTexture);
                        surfaces.add(previewSurface);

                        // Add surface for MediaRecorder
                        Surface recorderSurface = mMediaRecorder.getSurface();
                        surfaces.add(recorderSurface);


                        // Start a capture session
                        cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                captureSession = session;
                                try {
                                    CaptureRequest.Builder previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                    previewRequestBuilder.addTarget(previewSurface);

                                    CaptureRequest.Builder recordingRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                                    recordingRequestBuilder.addTarget(recorderSurface);

                                    // Start preview
                                    captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, handler);

                                    // Start recording
                                    mMediaRecorder.start();
                                    Log.e(LOG_TAG, "mMediaRecorder.start()");

                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                Log.e(LOG_TAG, "Capture session configuration failed");
                            }
                        }, handler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    cameraDevice.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    cameraDevice.close();
                    cameraDevice = null;
                }
            }, handler);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
    }


    private void setUpMediaRecorder() throws IOException {

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        //CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
        //mMediaRecorder.setVideoFrameRate(profile.videoFrameRate);
        //mMediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
       // mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264); //contained inside MP4
        //mMediaRecorder.setVideoSize(640, 480); //width 640, height 480
        //mMediaRecorder.setVideoFrameRate(30);  //30 FPS
        //mMediaRecorder.setVideoEncodingBitRate(3000000);
        //mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);

        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        //mMediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);
        //mMediaRecorder.setAudioSamplingRate(profile.audioSampleRate);
        mMediaRecorder.setOutputFile(createVideoFile("front").getAbsolutePath());

        try {
            mMediaRecorder.prepare();
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
        cameraDevice.close();
        cameraDevice = null;
        captureSession.close();
        captureSession = null;
        handlerThread.quitSafely();
        handlerThread = null;
        handler = null;
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