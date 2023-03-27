package com.noname.hidcam;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MyForegroundService extends Service implements LifecycleOwner {

    ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    MediaRecorder mRecorder;

    private static final long INTERVAL = 5 * 60 * 1000; // 15 minutes in milliseconds

    Timer timer;
    AlarmManager alarmManager;
    Intent alarmIntent;
    PendingIntent pendingIntent;

    private final ServiceLifecycleDispatcher mDispatcher = new ServiceLifecycleDispatcher(this);

    @Override
    public void onCreate() {
        mDispatcher.onServicePreSuperOnCreate();
        super.onCreate();
        timer = new Timer();

        }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        super.onStartCommand(intent, flags, startId);
        NotificationChannel channel = new NotificationChannel("my_channel_id", "My Channel Name", NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "my_channel_id")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Running")
                .setContentText("Running")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        int notificationId = 1;
        startForeground(notificationId, builder.build());

        Toast toast = Toast.makeText(getApplicationContext(), "Running", Toast.LENGTH_SHORT);
        toast.show();

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmIntent = new Intent(this, MyBroadcastReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,System.currentTimeMillis() + INTERVAL, pendingIntent);


        initCam();

        return START_STICKY;
    }

    private File createVideoFile(String position) throws IOException {
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("ddMMyyyy_hh-mm-ss", Locale.getDefault());

        String timeStamp = format.format(date);

        Log.e("myLog", timeStamp);

        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath());

        return File.createTempFile("VIDEO_" + position + "_" + timeStamp + "_no_number",".MP4", storageDir);
    }

    private void initCam(){
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder()
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .build();

                startMediaRecorder();

                preview.setSurfaceProvider(request -> request.provideSurface(mRecorder.getSurface(), getMainExecutor(),
                        result -> {}));


                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void startMediaRecorder() throws IOException {

        mRecorder = new MediaRecorder(this);

        mRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mRecorder.setOrientationHint(90);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mRecorder.setVideoSize(1920, 1080);
        mRecorder.setAudioEncodingBitRate(128000);
        mRecorder.setAudioSamplingRate(44100);

        mRecorder.setOutputFile(createVideoFile("backCam").getAbsolutePath());
        mRecorder.prepare();
        mRecorder.start();


        Log.e("myLog", "mRecorder.start()");

    }

    private void stopMediaRecorder(){
        if (mRecorder!=null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
            Log.e("myLog", "mRecorder.stop()");
            initCam();

        }
    }

    @Override
    public void onDestroy() {
        mDispatcher.onServicePreSuperOnDestroy();
        super.onDestroy();
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        alarmManager.cancel(pendingIntent);
        Log.e("myLog", "onDestroy ");
        Toast toast = Toast.makeText(getApplicationContext(),"Stopping", Toast.LENGTH_SHORT);
        toast.show();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mDispatcher.onServicePreSuperOnBind();
        return null;
    }

    @SuppressWarnings("deprecation")
    @CallSuper
    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        mDispatcher.onServicePreSuperOnStart();
        super.onStart(intent, startId);
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return mDispatcher.getLifecycle();

    }
    class UpdateTimeTask extends TimerTask {
        public void run() {
        stopMediaRecorder();
        }
    }
}
