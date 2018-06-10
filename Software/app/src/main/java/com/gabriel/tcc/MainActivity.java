package com.gabriel.tcc;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
        implements SensorEventListener {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CONNECTION_BT = 2;
    private static final int MESSAGE_READ = 3;

    private static final int SMS_PERMISSION_CODE = 4;

    private static final int pressureNotificationID = 10;
    private static final int stepNotificationID = 11;

    int overpressureCount = 0;

    ConnectedThread connectedThread;
    Handler mHandler;
    StringBuilder bluetoothDataBuilder = new StringBuilder();

    Button btnCon, btnSet;

    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothDevice mBluetoothDevice = null;
    BluetoothSocket mBluetoothSocket = null;

    boolean connection = false;

    private static String connectionAddress = null;

    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    int sensorReading1;
    int sensorReading2;
    int sensorReading3;
    int sensorReading4;
    int sensorPercent1;
    int sensorPercent2;
    int sensorPercent3;
    int sensorPercent4;

    SensorManager sensorManager;
    TextView tv_stepCounter, tv_s1v, tv_s2v, tv_s3v, tv_s4v;
    ImageView im_s1,im_s2,im_s3,im_s4;
    boolean running = false;
    int stepCount;

    float low = 25;
    float mid = 50;
    float high = 75;
    int stepMax = 2000;
    String phoneNumber;
    int time;
    int stepDiff = 0;
    int stepsInterval;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Verify bluetooth availability and activate it
        if (mBluetoothAdapter == null){
            Toast.makeText(getApplicationContext(),"Bluetooth Unsupported. Closing App",
                    Toast.LENGTH_LONG).show();
            //finish();
        }else if(!mBluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        tv_stepCounter = findViewById(R.id.tv_stepCounter);
        tv_s1v = (TextView) findViewById(R.id.tv_s1v);
        tv_s2v = (TextView) findViewById(R.id.tv_s2v);
        tv_s3v = (TextView) findViewById(R.id.tv_s3v);
        tv_s4v = (TextView) findViewById(R.id.tv_s4v);
        im_s1 = (ImageView) findViewById(R.id.im_s1);
        im_s2 = (ImageView) findViewById(R.id.im_s2);
        im_s3 = (ImageView) findViewById(R.id.im_s3);
        im_s4 = (ImageView) findViewById(R.id.im_s4);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepCount = 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Preferences preferences = Preferences.getInstance();

        running = true;
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if(countSensor != null){
            sensorManager.registerListener(this,countSensor,SensorManager.SENSOR_DELAY_UI);
        }else{
            Toast.makeText(this, "Sensor not found!", Toast.LENGTH_SHORT).show();
        }
        // Connect/Disconnect Button
        btnCon = (Button) findViewById(R.id.btnCon);
        btnCon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(connection){
                    try{
                        mBluetoothSocket.close();
                        connection = false;
                        Toast.makeText(getApplicationContext(),"Disconnected",
                                Toast.LENGTH_LONG).show();
                        btnCon.setText("Connect");
                    }catch(IOException e){
                        Toast.makeText(getApplicationContext(),"Error disconnecting",
                                Toast.LENGTH_LONG).show();
                    }
                }else{
                    Intent openListIntent =new Intent(MainActivity.this,DeviceList.class);
                    startActivityForResult(openListIntent,REQUEST_CONNECTION_BT);
                }

            }
        });

        // Open Bluetooth Settings Button
        btnSet = (Button) findViewById(R.id.btnBTSet);
        btnSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(android.provider.Settings.
                                ACTION_BLUETOOTH_SETTINGS),0);
            }
        });

        low = Float.valueOf(preferences.getLow());
        mid = Float.valueOf(preferences.getMid());
        high = Float.valueOf(preferences.getHigh());
        stepMax = Integer.valueOf(preferences.getstepMax());
        phoneNumber = preferences.getphoneNumber();
        time = Integer.valueOf(preferences.getTime());
        stepsInterval = Integer.valueOf(preferences.getStepsInterval());

        final NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("Too Much Preasure!!!")
                        .setContentText("Be light on your feet");
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);

        final NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.

        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(msg.what == MESSAGE_READ){
                    String received = (String) msg.obj;
                    bluetoothDataBuilder.append(received);

                    int endMsg = bluetoothDataBuilder.indexOf("}");

                    if(endMsg > 0){
                        String completeData = bluetoothDataBuilder.substring(0,endMsg);
                        int msgLength = completeData.length();
                        if(bluetoothDataBuilder.charAt(0)=='{'){
                            String finalData = bluetoothDataBuilder.substring(1,msgLength);
                            //Log.d("Received", finalData);
                            sensorReading1 = Integer.parseInt(finalData.
                                    substring(finalData.indexOf(":")+1,finalData.indexOf(";")));
                            String aux = finalData.
                                    substring(finalData.indexOf(";")+1,finalData.length());
                            sensorReading2 = Integer.parseInt(aux.
                                    substring(aux.indexOf(":")+1,aux.indexOf(";")));
                            aux = aux.substring(aux.indexOf(";")+1,aux.length());
                            sensorReading3 = Integer.parseInt(aux.
                                    substring(aux.indexOf(":")+1,aux.indexOf(";")));
                            aux = aux.substring(aux.indexOf(";")+1,aux.length());
                            sensorReading4 = Integer.parseInt(aux.
                                    substring(aux.indexOf(":")+1,aux.indexOf(";")));
                            sensorPercent1 = sensorReading1*100/4095;
                            sensorPercent2 = sensorReading2*100/4095;
                            sensorPercent3 = sensorReading3*100/4095;
                            sensorPercent4 = sensorReading4*100/4095;
                            tv_s1v.setText(String.valueOf(sensorPercent1)+"%");
                            tv_s2v.setText(String.valueOf(sensorPercent2)+"%");
                            tv_s3v.setText(String.valueOf(sensorPercent3)+"%");
                            tv_s4v.setText(String.valueOf(sensorPercent4)+"%");
                            if(sensorPercent1 > 75 || sensorPercent2 > 75 ||
                                    sensorPercent3 > 75 || sensorPercent4 > 75){
                                overpressureCount++;
                            }
                            if(overpressureCount >=(2*time)){
                                mNotificationManager.notify(pressureNotificationID,
                                        mBuilder.build());
                                AudioManager manager = (AudioManager)
                                        getSystemService(Context.AUDIO_SERVICE);
                                manager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 100,
                                        AudioManager.FLAG_VIBRATE);
                                Uri alarmSound = RingtoneManager.
                                        getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                mBuilder.setSound(alarmSound);

                                if(!isSmsPermissionGranted()){
                                    requestReadAndSendSmsPermission();
                                }
                                sendSMS(phoneNumber,
                                        "Hey, check out how Gabriel is doing," +
                                                " he is putting too much pressure on his feet");

                                overpressureCount = 0;
                            }

                            if(sensorPercent1 <= low){
                                im_s1.setImageResource(R.drawable.circle_green);
                            }else if(sensorPercent1 <= mid){
                                im_s1.setImageResource(R.drawable.circle_lgreen);
                            }else if(sensorPercent1 <= high) {
                                im_s1.setImageResource(R.drawable.circle_pink);
                            }else{
                                im_s1.setImageResource(R.drawable.circle_red);
                            }

                            if(sensorPercent2 <= low){
                                im_s2.setImageResource(R.drawable.circle_green);
                            }else if(sensorPercent2 <= mid){
                                im_s2.setImageResource(R.drawable.circle_lgreen);
                            }else if(sensorPercent2 <= high) {
                                im_s2.setImageResource(R.drawable.circle_pink);
                            }else{
                                im_s2.setImageResource(R.drawable.circle_red);
                            }

                            if(sensorPercent3 <= low){
                                im_s3.setImageResource(R.drawable.circle_green);
                            }else if(sensorPercent3 <= mid){
                                im_s3.setImageResource(R.drawable.circle_lgreen);
                            }else if(sensorPercent3 <= high) {
                                im_s3.setImageResource(R.drawable.circle_pink);
                            }else{
                                im_s3.setImageResource(R.drawable.circle_red);
                            }

                            if(sensorPercent4 <= low){
                                im_s4.setImageResource(R.drawable.circle_green);
                            }else if(sensorPercent4 <= mid){
                                im_s4.setImageResource(R.drawable.circle_lgreen);
                            }else if(sensorPercent4 <= high) {
                                im_s4.setImageResource(R.drawable.circle_pink);
                            }else{
                                im_s4.setImageResource(R.drawable.circle_red);
                            }

                            Log.d("low", Float.toString(low));
                            Log.d("mid", Float.toString(mid));
                            Log.d("high", Float.toString(high));
                        }
                        bluetoothDataBuilder.delete(0,bluetoothDataBuilder.length());
                    }
                }
            }
        };
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_ENABLE_BT:
                if(resultCode == Activity.RESULT_OK){
                    Toast.makeText(getApplicationContext(),
                            "Bluetooth: ON", Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(getApplicationContext(),
                            "Bluetooth: OFF. Closing App", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            case REQUEST_CONNECTION_BT:
                if(resultCode == Activity.RESULT_OK){
                    connectionAddress = data.getExtras().getString(DeviceList.ConDeviceAddress);
                    mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(connectionAddress);

                    try{
                        mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                        mBluetoothSocket.connect();
                        connection = true;
                        connectedThread = new ConnectedThread(mBluetoothSocket);
                        connectedThread.start();
                        btnCon.setText("Disconect");
                        Toast.makeText(getApplicationContext(),
                                "Connected to " + connectionAddress, Toast.LENGTH_LONG).show();
                    }catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(),
                                "Error Connecting to: " + connectionAddress,
                                Toast.LENGTH_LONG).show();
                    }

                }else{
                    Toast.makeText(getApplicationContext(),
                            "Address: Fail", Toast.LENGTH_LONG).show();
                }
                break;


        }
    }



    @Override
    protected void onPause() {
        super.onPause();
        Log.d("low", Float.toString(low));
        Log.d("mid", Float.toString(mid));
        Log.d("high", Float.toString(high));
        running = false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        stepCount++;
        tv_stepCounter.setText(String.valueOf(stepCount));

        final NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("You are walking too much!!!")
                        .setContentText("Take a Rest...");
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);

        final NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.

        if(stepCount >stepMax){
            stepDiff++;
            if(stepDiff > stepsInterval) {
                mNotificationManager.notify(stepNotificationID, mBuilder.build());
                AudioManager manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                manager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 100,
                        AudioManager.FLAG_VIBRATE);
                Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                mBuilder.setSound(alarmSound);

                if (!isSmsPermissionGranted()) {
                    requestReadAndSendSmsPermission();
                }
                sendSMS(phoneNumber,
                        "Hey, check out how Gabriel is doing, he is walking too much!");
                stepDiff = 0;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    String bluetoothData = new String(buffer, 0,bytes);

                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, bluetoothData)
                            .sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String sendData) {
            byte[] buffer = sendData.getBytes();
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
            }
        }
    }
    public void sendSMS(String phoneNo, String msg) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null,
                    msg, null, null);
            Toast.makeText(getApplicationContext(), "Message Sent",
                    Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(),ex.getMessage().toString(),
                    Toast.LENGTH_LONG).show();
            ex.printStackTrace();
        }
    }

    public boolean isSmsPermissionGranted() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request runtime SMS permission
     */
    private void requestReadAndSendSmsPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.SEND_SMS)) {
            // You may display a non-blocking explanation here, read more in the documentation:
            // https://developer.android.com/training/permissions/requesting.html
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS},
                SMS_PERMISSION_CODE);
    }
}
