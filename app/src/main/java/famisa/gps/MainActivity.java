package famisa.gps;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private Button button;
    private Button stopbutton;
    private Button deleteDB;
    private Button viewDB;
    private Button socketData;
    private Button gpsZero;
    private Button startServer;

    private TextView serverStatus;
    ServerSocket serverSocket;

    private TextView textView;
    private EditText hora;
    private  EditText minuto;
    Location loc;
    private LocationManager locationManager;
    private LocationListener listener;
    private Location location;

    private int totalMinutos = 1;
    private int lapsoSegundos = 20;
    private SocketServerThread socketServerThread;
    SQLiteDatabase mydatabase;

    AlertDialog.Builder dlgAlert;
    boolean zeroStart = false;

    FileOutputStream fos ;
    Calendar cal = Calendar.getInstance();

    private double zeroLatitude;
    private double zeroLongitude;
    private double LatitudeCorrection = 0;
    private double LongitudeCorrection = 0;

    private double CurrentLatitude = 0;
    private double CurrentLongitude = 0;

    // NSD Manager, discovery listener code here
    private static int SocketServerPORT = 8080;
    private static String REQUEST_CONNECT_CLIENT = "request-zero";
    private static final String TAG = "MainActivity";
    private static String hostAddress;

    private static int hour;
    private static int minute;
    private static int second;

    AlarmManager am;

    /*
        0 - Inactive
        1 - On client-side set textview value of response.
     */
    private int stat = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        createDB();

        textView = (TextView) findViewById(R.id.textView);
        button = (Button) findViewById(R.id.button);
        stopbutton = (Button) findViewById(R.id.stopbutton);
        hora = (EditText) findViewById(R.id.hora);
        minuto = (EditText) findViewById(R.id.minuto);
        viewDB = (Button) findViewById(R.id.readDB);
        deleteDB = (Button) findViewById(R.id.deleteDB);
        socketData = (Button) findViewById(R.id.zero);
        gpsZero = (Button) findViewById(R.id.gps_p0);
        startServer = (Button) findViewById(R.id.startServer);
        serverStatus = (TextView) findViewById(R.id.serverStatus);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                textView.setText("");
                textView.append("n " + location.getLongitude() + " " + location.getLatitude());
//                try {
//                    new send(String.valueOf(location.getLongitude()), String.valueOf(location.getLatitude())).execute();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
                locationManager.removeUpdates(listener);
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(i);
            }
        };

        deleteDB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Eliminar la base de datos")
                        .setMessage("Deseas vaciar los datos de la base de datos")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                deleteDatabase("gpsreldb");
                                mydatabase = openOrCreateDatabase("gpsreldb", MODE_PRIVATE,null);
//                                mydatabase.execSQL("DROP TABLE GPS");
                                mydatabase.execSQL("CREATE TABLE IF NOT EXISTS GPS(id INTEGER PRIMARY KEY AUTOINCREMENT,latitude DOUBLE, longitude DOUBLE, fechahora DATETIME);");
                                mydatabase.execSQL("CREATE TABLE IF NOT EXISTS SERVER(ip VARCHAR(18));");
                                mydatabase.execSQL("CREATE TABLE IF NOT EXISTS ZERO(latitude DOUBLE, longitude DOUBLE);");
                                mydatabase.execSQL("CREATE TABLE IF NOT EXISTS GPSCORRECTION(id INTEGER PRIMARY KEY AUTOINCREMENT,latitude DOUBLE, longitude DOUBLE, newlatitude DOUBLE DEFAULT  NULL, newlongitude DOUBLE DEFAULT NULL, fechahora DATETIME);");
                                mydatabase.execSQL("INSERT INTO SERVER(ip) VALUES('');");
                                Toast.makeText(MainActivity.this, "La base de datos fue eliminada con exito.", Toast.LENGTH_SHORT).show();
                            }})
                        .setNegativeButton(android.R.string.no, null).show();
            }
        });

        viewDB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, readDB.class);
                startActivity(i);
            }
        });

        socketData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, socketdata.class);
                startActivity(i);
            }
        });

//        socketServerThread = new SocketServerThread();
//        socketServerThread.start();
        Thread socketServerThread = new Thread(new SocketServerThread());
        socketServerThread.start();
//        if(socketServerThread.socket.isClosed())
//        {
//            serverStatus.setText("Server Status: Online");
//        } else {
//            serverStatus.setText("Server Status: Offline");
//        }
        zeroStart = true;
        scheduleGPS();
        stopGPS();
        //configure_button();
        zeroStart = false;
        startGPSZero();
    }

    public void createDB()
    {
        mydatabase = openOrCreateDatabase("gpsreldb", MODE_PRIVATE,null);
        mydatabase.execSQL("CREATE TABLE IF NOT EXISTS GPS(id INTEGER PRIMARY KEY AUTOINCREMENT,latitude DOUBLE, longitude DOUBLE, fechahora DATETIME);");
        mydatabase.execSQL("CREATE TABLE IF NOT EXISTS SERVER(ip VARCHAR(18));");
        mydatabase.execSQL("CREATE TABLE IF NOT EXISTS ZERO(latitude DOUBLE, longitude DOUBLE);");
        mydatabase.execSQL("CREATE TABLE IF NOT EXISTS GPSCORRECTION(id INTEGER PRIMARY KEY AUTOINCREMENT,latitude DOUBLE, longitude DOUBLE, newlatitude DOUBLE DEFAULT  NULL, newlongitude DOUBLE DEFAULT NULL, fechahora DATETIME);");
        mydatabase.execSQL("INSERT INTO SERVER(ip) VALUES('');");
        mydatabase.close();
    }

    public void insertDB()
    {
        mydatabase.execSQL("INSERT INTO GPS VALUES(" + location.getLatitude() + "," + location.getLongitude() + ", strftime('%Y-%m-%d %H-%M-%f','now'));");
    }

    public void setCoordinates(String longitude, String latitude)
    {
        textView.setText("");
        textView.append("n " + longitude + " " + latitude);
    }

    public void startGPSZero()
    {
        gpsZero.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mydatabase = openOrCreateDatabase("gpsreldb", MODE_PRIVATE,null);
                Cursor c = mydatabase.rawQuery("SELECT ip FROM SERVER", null);
                if (c.moveToFirst()){
                        hostAddress = c.getString(0);
                }
                mydatabase.close();
                if(pingIP(hostAddress)) {
                    zeroStart = true;
                    REQUEST_CONNECT_CLIENT = "request-zero";
                    JSONObject jsonData = new JSONObject();
                    try {
                        jsonData.put("request", REQUEST_CONNECT_CLIENT);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e(TAG, "can't put request");
                        return;
                    }
                    new SocketServerTask().execute(jsonData);
                } else
                {
                    Toast.makeText(MainActivity.this, "No fue posible establecer una conexión con la ip: " + hostAddress, Toast.LENGTH_SHORT).show(); //Ping doesnt work
                }
            }
        });
    }

    private boolean pingIP(String ip)
    {
        System.out.println("executeCommand");
        Runtime runtime = Runtime.getRuntime();
        try
        {
            Process  mIpAddrProcess = runtime.exec("/system/bin/ping -c 1 " + ip);
            int mExitValue = mIpAddrProcess.waitFor();
            System.out.println(" mExitValue "+mExitValue);
            if(mExitValue==0)
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        catch (InterruptedException ignore)
        {
            ignore.printStackTrace();
            System.out.println(" Exception:"+ignore);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.out.println(" Exception:"+e);
        }
        return false;
    }

    public void getCoordinates()
    {
        Toast.makeText(MainActivity.this,"asdf", Toast.LENGTH_LONG).show();
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //   ActivityCompat#requestPermissions
            //   here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
//                locationManager.requestLocationUpdates("gps", 30 * 60 * 1000, 0, listener);
        locationManager.requestLocationUpdates("gps", 0, 0, listener);
    }

    public void scheduleGPS()
    {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                int pruebas = (totalMinutos * 60 / lapsoSegundos);
                int pruebas = 1;
                int horaPrueba = Integer.valueOf(hora.getText().toString());
                int minutoPrueba = Integer.valueOf(minuto.getText().toString());
                int segundoPrueba = 0;

                Calendar gpsExec = Calendar.getInstance();
                AlarmManager [] am = new AlarmManager[pruebas];

                gpsExec.set(Calendar.HOUR_OF_DAY, horaPrueba);
                gpsExec.set(Calendar.MINUTE, minutoPrueba);
                gpsExec.set(Calendar.SECOND, segundoPrueba);
                gpsExec.set(Calendar.MILLISECOND, 0);
                Toast.makeText(MainActivity.this, "Hora: " + horaPrueba + "\nMinuto: " + minutoPrueba + "\nSegundo: " + segundoPrueba + "\nCurrent:" + System.currentTimeMillis() + "\ngpsExec: " + gpsExec.getTimeInMillis(), Toast.LENGTH_LONG).show();

                Intent intentAlarm = new Intent(MainActivity.this, SilenceBroadcastReceiver.class);

                am[0] = (AlarmManager) MainActivity.this.getSystemService(ALARM_SERVICE);
                am[0].set(AlarmManager.RTC_WAKEUP, gpsExec.getTimeInMillis(), PendingIntent.getBroadcast(MainActivity.this, 0, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));

//                for(int i = 1; i < pruebas; i++) {
//                    //create new calendar instance
//
//
//                    segundoPrueba += 10;
//                    if(segundoPrueba == 60) {
//                        segundoPrueba = 0;
//                        minutoPrueba++;
//                        if(minutoPrueba == 60) {
//                            minutoPrueba = 0;
//                            if(horaPrueba == 23)
//                                horaPrueba = 0;
//                            else
//                                horaPrueba++;
//                        }
//                    }
//                    //set the time to midnight tonight
//                    gpsExec.set(Calendar.HOUR_OF_DAY, horaPrueba);
//                    gpsExec.set(Calendar.MINUTE, minutoPrueba);
//                    gpsExec.set(Calendar.SECOND, segundoPrueba);
//                    gpsExec.set(Calendar.MILLISECOND, 0);
////                    long millis = (gpsExec.getTimeInMillis() - System.currentTimeMillis());
//
//                    am[i] = (AlarmManager) MainActivity.this.getSystemService(ALARM_SERVICE);
//                    //create a pending intent to be called at midnight
////                PendingIntent gpsPI = PendingIntent.getService(MainActivity.this, 0, new Intent("famisa.gps.SilenceBroadcastReceiver"), PendingIntent.FLAG_UPDATE_CURRENT);
//
//                    //schedule time for pending intent, and set the interval to day so that this event will repeat at the selected time every day
//                    am[i].set(AlarmManager.RTC_WAKEUP, gpsExec.getTimeInMillis(), PendingIntent.getBroadcast(MainActivity.this, i+100, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));
//                    //am.setRepeating(AlarmManager.RTC_WAKEUP, gpsExec.getTimeInMillis(), AlarmManager.INTERVAL_DAY, gpsPI);
//                }
            }
        });
    }

    public void stopGPS()
    {
        stopbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //get Alarm manager instance
                AlarmManager am = (AlarmManager) MainActivity.this.getSystemService(ALARM_SERVICE);
                //build intent for midnight
                PendingIntent gpsPI = PendingIntent.getService(MainActivity.this, 0, new Intent("famisa.gps.SilenceBroadcastReceiver"), PendingIntent.FLAG_UPDATE_CURRENT);
                //cancel it
                am.cancel(gpsPI);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case 10:
                configure_button();
                break;
            default:
                break;
        }
    }

    void configure_button() {
        // first check for permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET}
                        , 10);
            }
            return;
        }
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //   ActivityCompat#requestPermissions
                    //   here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
            }
        });
        // this code won'textView execute IF permissions are not allowed, because in the line above there is return statement.
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(hora.getText().equals("") || minuto.getText().equals(""))
                {
                    Toast.makeText(MainActivity.this, "Ingresa datos de inicio de fecha y hora", Toast.LENGTH_LONG).show();
                }
                //noinspection MissingPermission
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //   ActivityCompat#requestPermissions
                    //   here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
//                locationManager.requestLocationUpdates("gps", 30 * 60 * 1000, 0, listener);
                locationManager.requestLocationUpdates("gps", 0, 0, listener);
            }
        });
    }

    public void saveCoordinates(double latitude, double longitude)
    {
        mydatabase = openOrCreateDatabase("gpsreldb", MODE_PRIVATE,null);
        mydatabase.execSQL("INSERT INTO GPSCORRECTION (latitude, longitude, fechahora) VALUES(" + latitude + "," + longitude + ", strftime('%Y-%m-%d %H-%M-%f','now'));");
        mydatabase.close();
    }

    private class SocketServerTask extends AsyncTask<JSONObject, Void, Void> {

        private JSONObject jsonData;
        private boolean success;
        private JSONObject jsondata;
        private String response;

        @Override
        protected Void doInBackground(JSONObject... params) {
            if(zeroStart) {
                Socket socket = null;
                DataInputStream dataInputStream = null;
                DataOutputStream dataOutputStream = null;
                jsonData = params[0];

                try {
                    // Create a new Socket instance and connect to host
                    //textView.setText( "Trying to connecto to: " + hostAddress + ": \n" + SocketServerPORT);
                    socket = new Socket(hostAddress, SocketServerPORT);

                    dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    dataInputStream = new DataInputStream(socket.getInputStream());

                    // transfer JSONObject as String to the server
                    dataOutputStream.writeUTF(jsonData.toString());
                    Log.i(TAG, "waiting for response from host");

                    // Thread will wait till server replies
                    response = dataInputStream.readUTF();
                    jsondata = new JSONObject(response);

                    String request = jsonData.getString("request");
                    if (request.equals("request-zero")) {
                        if(jsondata.getString("message") != null) {
                            success = true;
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        textView.setText(jsondata.getString("message"));
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        } else {
                            success = false;
                        }
                    } else if (request.equals("send-correction")) {
                        if(jsondata.getString("request") != null) {
                            success = true;
                            scheduleGPSCorrectionServer(jsondata.getInt("hour"), jsondata.getInt("minute"), jsondata.getInt("second"));
                        } else {
                            success = false;
                        }
                    } else if (request.equals("send-zero")) {
                        success = true;
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        textView.setText("SERVER\nCorrection set to start at:\n " + jsondata.getInt("hour") + ":" + jsondata.getInt("minute") + ":" + jsondata.getInt("second"));
                                        scheduleGPSCorrectionServer(jsondata.getInt("hour"), jsondata.getInt("minute"), jsondata.getInt("second"));
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    success = false;
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {

                    // close socket
                    if (socket != null) {
                        try {
                            Log.i(TAG, "closing the socket");
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    // close input stream
                    if (dataInputStream != null) {
                        try {
                            dataInputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    // close output stream
                    if (dataOutputStream != null) {
                        try {
                            dataOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (success) {
                Toast.makeText(MainActivity.this, "Connection Established", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Unable to connect", Toast.LENGTH_SHORT).show();
            }
            switch (stat)
            {
                case 1:
                    textView.setText(response);
                    break;
            }
        }
    }

    private void scheduleGPSCorrection(int hora, int minuto, int segundo)
    {
        this.hour = hora;
        this.minute = minuto;
        this.second = segundo;
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(textView.getText().toString() + "\nRUNNING CORRECTION");

                Calendar gpsExec = Calendar.getInstance();

                gpsExec.set(Calendar.HOUR_OF_DAY, hour);
                gpsExec.set(Calendar.MINUTE, minute);
                gpsExec.set(Calendar.SECOND, second);
                gpsExec.set(Calendar.MILLISECOND, 0);

                Intent intentAlarm = new Intent(MainActivity.this, GPSBroadcastReceiver.class);

                am = (AlarmManager) MainActivity.this.getSystemService(ALARM_SERVICE);
                am.set(AlarmManager.RTC_WAKEUP, gpsExec.getTimeInMillis(), PendingIntent.getBroadcast(MainActivity.this, 100, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));

                Toast.makeText(MainActivity.this, "Hora: " + hour + "\nMinuto: " + minute + "\nSegundo: " + second + "\nCurrent:" + System.currentTimeMillis() + "\ngpsExec: " + gpsExec.getTimeInMillis(), Toast.LENGTH_LONG).show();

                if(am != null)
                {
                    Toast.makeText(MainActivity.this, "ALARMA EN FUNCIONAMIENTO", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void scheduleGPSCorrectionServer(int hora, int minuto, int segundo)
    {
        this.hour = hora;
        this.minute = minuto;
        this.second = segundo;
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(textView.getText().toString() + "\nRUNNING CORRECTION");

                Calendar gpsExec = Calendar.getInstance();

                gpsExec.set(Calendar.HOUR_OF_DAY, hour);
                gpsExec.set(Calendar.MINUTE, minute);
                gpsExec.set(Calendar.SECOND, second);
                gpsExec.set(Calendar.MILLISECOND, 0);

                Intent intentAlarm = new Intent(MainActivity.this, GPSServerBroadcastReceiver.class);

                am = (AlarmManager) MainActivity.this.getSystemService(ALARM_SERVICE);
                am.set(AlarmManager.RTC_WAKEUP, gpsExec.getTimeInMillis(), PendingIntent.getBroadcast(MainActivity.this, 100, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));

                Toast.makeText(MainActivity.this, "Hora: " + hour + "\nMinuto: " + minute + "\nSegundo: " + second + "\nCurrent:" + System.currentTimeMillis() + "\ngpsExec: " + gpsExec.getTimeInMillis(), Toast.LENGTH_LONG).show();

            }
        });
    }

    public void getCorrection(double latitude, double longitude)
    {
        mydatabase = openOrCreateDatabase("gpsreldb", MODE_PRIVATE,null);
        Cursor c = mydatabase.rawQuery("SELECT latitude, longitude FROM ZERO", null);
        if (c.moveToFirst()){
            do {
                // Passing values
                zeroLatitude = c.getDouble(0);
                zeroLongitude = c.getDouble(1);
            } while(c.moveToNext());
        }
        mydatabase.close();
        LatitudeCorrection = zeroLatitude - latitude;
        LongitudeCorrection = zeroLongitude - longitude;
        REQUEST_CONNECT_CLIENT = "send-correction";
        JSONObject jsonData = new JSONObject();
        try {
            jsonData.put("request", REQUEST_CONNECT_CLIENT);
            jsonData.put("latitude", LatitudeCorrection);
            jsonData.put("longitude", LongitudeCorrection);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "can't put request");
            return;
        }
        new SocketServerTask().execute(jsonData);
    }

    private class SocketServerThread extends Thread {
        Socket socket = null;
        JSONObject jsonData = new JSONObject();

        public boolean getStatus()
        {
            return socket.isConnected();
        }

        @Override
        public void run() {
//            if(zeroStart) {
                DataInputStream dataInputStream = null;
                DataOutputStream dataOutputStream = null;

                try {
                    Log.i(TAG, "Creating server socket");
                    serverSocket = new ServerSocket(SocketServerPORT);

                    while (true) {
                        socket = serverSocket.accept();
                        dataInputStream = new DataInputStream(socket.getInputStream());
                        dataOutputStream = new DataOutputStream(socket.getOutputStream());

                        String messageFromClient, messageToClient, request;

                        //If no message sent from client, this code will block the program
                        messageFromClient = dataInputStream.readUTF();

                        final JSONObject jsondata;
                        jsondata = new JSONObject(messageFromClient);

                        try {
                            request = jsondata.getString("request");

                            if (request.equals("request-zero")) {
                                listener = new LocationListener() {
                                    @Override
                                    public void onLocationChanged(Location location) {
                                        textView.setText("Origin is now Ready.");
                                        locationManager.removeUpdates(listener);
                                        zeroLatitude = location.getLatitude();
                                        zeroLongitude = location.getLongitude();
                                        mydatabase = openOrCreateDatabase("gpsreldb", MODE_PRIVATE, null);
                                        mydatabase.execSQL("DELETE FROM ZERO;");
                                        mydatabase.execSQL("INSERT INTO ZERO(latitude, longitude) VALUES(" + zeroLatitude + ", " + zeroLongitude + ");");
                                        mydatabase.close();
                                        REQUEST_CONNECT_CLIENT = "send-zero";
                                        JSONObject jsonData = new JSONObject();
                                        try {
                                            jsonData.put("request", REQUEST_CONNECT_CLIENT);
                                            jsonData.put("get-zero", 1);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                            Log.e(TAG, "can't put request");
                                            return;
                                        }
                                        new SocketServerTask().execute(jsonData);
                                    }

                                    @Override
                                    public void onStatusChanged(String s, int i, Bundle bundle) {

                                    }

                                    @Override
                                    public void onProviderEnabled(String s) {

                                    }

                                    @Override
                                    public void onProviderDisabled(String s) {

                                        Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                        startActivity(i);
                                    }
                                };
                                MainActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //noinspection MissingPermission
                                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                            // TODO: Consider calling
                                            //   ActivityCompat#requestPermissions
                                            //   here to request the missing permissions, and then overriding
                                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                            //                                          int[] grantResults)
                                            // to handle the case where the user grants the permission. See the documentation
                                            // for ActivityCompat#requestPermissions for more details.
                                            return;
                                        }
                                        locationManager.requestLocationUpdates("gps", 0, 0, listener);
                                    }
                                });
                                JSONObject jsonResponse = new JSONObject();
                                jsonResponse.put("request", "request-zero");
                                jsonResponse.put("message", "Awaiting Origin Reference is Ready...");
                                dataOutputStream.writeUTF(jsonResponse.toString());
                            } else if (request.equals("request-correction")) {
                                scheduleGPSCorrectionServer(jsondata.getInt("hour"), jsondata.getInt("minute"), jsondata.getInt("second"));
                            } else if (request.equals("send-zero")) {
                                cal = Calendar.getInstance();
                                hour = cal.get(Calendar.HOUR_OF_DAY);
                                minute = cal.get(Calendar.MINUTE);
                                second = cal.get(Calendar.SECOND);
                                if (second > 15) {
                                    second = 0;
                                    minute += 1;
                                    if (minute == 60) {
                                        hour++;
                                        minute = 0;
                                    }
                                    if (hour == 24)
                                        hour = 0;
                                } else
                                    second = 30;
                                REQUEST_CONNECT_CLIENT = "request-correction";
                                jsonData = new JSONObject();
                                try {
                                    jsonData.put("request", REQUEST_CONNECT_CLIENT);
                                    jsonData.put("hour", hour);
                                    jsonData.put("minute", minute);
                                    jsonData.put("second", second);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    Log.e(TAG, "can't put request");
                                    return;
                                }
                                dataOutputStream.writeUTF(jsonData.toString());
                                MainActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            textView.setText("CLIENT\nCorrection set to start at:\n " + jsonData.getInt("hour") + ":" + jsonData.getInt("minute") + ":" + jsonData.getInt("second"));
                                            scheduleGPSCorrection(hour, minute, second);
                                            Toast.makeText(MainActivity.this, "GPS correction about to start", Toast.LENGTH_SHORT).show();
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            } else if (request.equals("send-correction")) {
                                mydatabase = openOrCreateDatabase("gpsreldb", MODE_PRIVATE, null);
                                mydatabase.execSQL("UPDATE GPSCORRECTION SET newlatitude = " + (CurrentLatitude - jsondata.getDouble("latitude")) + ", newlongitude = " + (CurrentLongitude - jsondata.getDouble("longitude")) + " WHERE id = (SELECT MAX(id) FROM table)");
                                mydatabase.close();
                                hour = cal.HOUR_OF_DAY;
                                minute = cal.MINUTE;
                                second = cal.SECOND;
                                if (second > 20) {
                                    second = 0;
                                    minute += 1;
                                    if (minute == 60) {
                                        hour++;
                                        minute = 0;
                                    }
                                    if (hour == 24)
                                        hour = 0;
                                } else
                                    second = 30;
                                REQUEST_CONNECT_CLIENT = "request-correction";
                                try {
                                    jsonData.put("request", REQUEST_CONNECT_CLIENT);
                                    jsonData.put("hour", hour);
                                    jsonData.put("minute", minute);
                                    jsonData.put("second", second);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    Log.e(TAG, "can't put request");
                                    return;
                                }
                                dataOutputStream.writeUTF(jsonData.toString());
                                scheduleGPSCorrection(hour, minute, second);
                                MainActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            textView.setText("Correction set to start at:\n " + jsonData.getInt("hour") + ":" + jsonData.getInt("minute") + ":" + jsonData.getInt("second"));
                                            Toast.makeText(MainActivity.this, "GPS correction about to start again", Toast.LENGTH_SHORT).show();
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e(TAG, "Unable to get request");
                            dataOutputStream.flush();
                        }
                    }

                } catch (IOException e) {
                    Log.e(TAG, "IOException");
                    e.printStackTrace();
                } catch (JSONException e) {
                    Log.e(TAG, "JSONException");
                    e.printStackTrace();
                } catch (Exception e) {
                    Log.e(TAG, "Server Exception");
                    e.printStackTrace();
                }
//                finally {
//                    if (socket != null) {
//                        try {
//                            socket.close();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//
//                    if (dataInputStream != null) {
//                        try {
//                            dataInputStream.close();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//
//                    if (dataOutputStream != null) {
//                        try {
//                            dataOutputStream.close();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            }

        }

    }
}