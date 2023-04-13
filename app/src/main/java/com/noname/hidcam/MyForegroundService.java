package com.noname.hidcam;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleService;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class MyForegroundService extends LifecycleService  {
//implements LifecycleOwner
    ListenableFuture<ProcessCameraProvider> cameraProviderFuture;


    private static final int INTERVAL = 1 * 60 * 1000; // 15 minutes in milliseconds

    VideoRecorder videoRecorder;

    //private final ServiceLifecycleDispatcher mDispatcher = new ServiceLifecycleDispatcher(this);


    @Override
    public void onCreate() {
        super.onCreate();
        //mDispatcher.onServicePreSuperOnCreate();
        videoRecorder = new VideoRecorder();

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



        initCam();

        return START_STICKY;
    }



    private void initCam(){
        cameraProviderFuture = ProcessCameraProvider.getInstance(getApplicationContext());
        //HandlerThread handlerThread = new HandlerThread("CameraHandlerThread");
        //handlerThread.start();
        //Handler handler = new Handler(handlerThread.getLooper());
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

                videoRecorder.startMediaRecorder(getApplicationContext());

                preview.setSurfaceProvider(request -> request.provideSurface(videoRecorder.mRecorder.getSurface(), getMainExecutor(), result -> {}));


                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(getApplicationContext()));
    }

//ContextCompat.getMainExecutor(getApplicationContext())



    @Override
    public void onDestroy() {
        //mDispatcher.onServicePreSuperOnDestroy();
        super.onDestroy();
        videoRecorder.stopMediaRecorder();
        Log.e("myLog", "onDestroy ");
        Toast toast = Toast.makeText(getApplicationContext(),"Stopping", Toast.LENGTH_SHORT);
        toast.show();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        //mDispatcher.onServicePreSuperOnBind();
        return null;
    }

    @SuppressWarnings("deprecation")
    @CallSuper
    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        //mDispatcher.onServicePreSuperOnStart();
        super.onStart(intent, startId);
    }

   // @NonNull
  //  @Override
  //  public Lifecycle getLifecycle() {
        //return mDispatcher.getLifecycle();

   // }

    public class VideoRecorder implements MediaRecorder.OnInfoListener {
        private MediaRecorder mRecorder;
        private final int MAX_RECORDING_TIME = 1 * 60 * 1000; // 15 minutes in milliseconds
        private final int MAX_FILE_SIZE = 100000000; // 100 MB in bytes


        private void startMediaRecorder(Context context) throws IOException {

            mRecorder = new MediaRecorder(context);


            mRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mRecorder.setOrientationHint(90);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mRecorder.setVideoSize(1920, 1080);
            mRecorder.setAudioEncodingBitRate(128000);
            mRecorder.setAudioSamplingRate(44100);
            mRecorder.setMaxFileSize(MAX_FILE_SIZE);

            mRecorder.setMaxDuration(MAX_RECORDING_TIME);
            mRecorder.setOnInfoListener(this);

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

            }
        }

        private File createVideoFile(String position) throws IOException {
            Date date = new Date();
            SimpleDateFormat format = new SimpleDateFormat("ddMMyyyy_hh-mm-ss", Locale.getDefault());

            String timeStamp = format.format(date);

            //Log.e("myLog", timeStamp);

            File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath());

            return File.createTempFile("VIDEO_" + position + "_" + timeStamp + "__",".MP4", storageDir);
        }

        @Override
        public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
                    what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                Log.e("myLog", "onInfo(MediaRecorder)");

                stopMediaRecorder();


                initCam();

                //try {
                //    startMediaRecorder(getApplicationContext());
               // } catch (IOException e) {
                //    throw new RuntimeException(e);
                //}


            }
        }
    }

}
