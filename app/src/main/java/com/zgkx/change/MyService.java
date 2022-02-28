package com.zgkx.change;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

public class MyService extends Service {
    private static final String TAG = "MyServiceInfo";

    private final int PID =11;

    private AssistServiceConnection mConnection;

    @Override

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override

    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MyService: onCreate()");
        setForeground();
    }

    @Override

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "MyService: onStartCommand()");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override

    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MyService: onDestroy()");
    }

    public void setForeground() {
        // sdk < 18 , 直接调用startForeground即可,不会在通知栏创建通知
        if (Build.VERSION.SDK_INT < 18) {
            this.startForeground(PID, getNotification());
            return;
        }

        if (null == mConnection) {
            mConnection = new AssistServiceConnection();
        }
        this.bindService(new Intent(this, AssistService.class), mConnection,
                Service.BIND_AUTO_CREATE);
    }

    private class AssistServiceConnection implements ServiceConnection {
        @Override

        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "MyService: onServiceDisconnected");
        }

        @Override

        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.d(TAG, "MyService: onServiceConnected");
            Service assistService = ((AssistService.LocalBinder) binder)
                    .getService();
            MyService.this.startForeground(PID, getNotification());
            assistService.startForeground(PID, getNotification());
            assistService.stopForeground(true);
            MyService.this.unbindService(mConnection);
            mConnection = null;
        }
    }

    private Notification getNotification() {
        // 定义一个notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel("notification_id", "notification_name", NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(channel);
        Notification.Builder builder = new Notification.Builder(this.getApplicationContext()); //获取一个Notification构造器
        builder.setChannelId("notification_id");
        Intent nfIntent = new Intent(this, MainActivity.class);
        builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, 0)) // 设置PendingIntent
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher)) // 设置下拉列表中的图标(大图标)
                .setContentTitle(getResources().getString(R.string.app_name)) // 设置下拉列表里的标题
                .setSmallIcon(R.mipmap.ic_launcher) // 设置状态栏内的小图标
                .setContentText("test dialog") // 设置上下文内容
                .setWhen(System.currentTimeMillis()); // 设置该通知发生的时间
        Notification notification = builder.build(); // 获取构建好的Notification
        notification.defaults = Notification.DEFAULT_SOUND; //设置为默认的声音
        return notification;

    }

    //再看那个辅助消除通知的Service的代码，非常的简单：
    public static class AssistService extends Service {

        private static final String TAG = "AssistService";
        public class LocalBinder extends Binder {
            public AssistService getService() {
                return AssistService.this;
            }
        }
        @Override
        public IBinder onBind(Intent intent) {
            Log.d(TAG, "AssistService: onBind()");
            return new LocalBinder();
        }
        @Override
        public void onDestroy() {
            super.onDestroy();
            Log.d(TAG, "AssistService: onDestroy()");
        }
    }
}