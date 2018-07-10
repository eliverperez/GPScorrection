package famisa.gps;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private Button button;
    private Button stopbutton;
    private Button deleteDB;
    private Button viewDB;

    private TextView textView;
    private EditText hora;
    private  EditText minuto;
    Location loc;
    private LocationManager locationManager;
    private LocationListener listener;
    private Location location;

    private int totalMinutos = 1;
    private int lapsoSegundos = 20;
    SQLiteDatabase mydatabase;

    AlertDialog.Builder dlgAlert;

    FileOutputStream fos ;

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
                                mydatabase = openOrCreateDatabase("gpsreldb", MODE_PRIVATE,null);
                                mydatabase.execSQL("DROP TABLE GPS");
                                mydatabase.execSQL("CREATE TABLE IF NOT EXISTS GPS(id INTEGER PRIMARY KEY AUTOINCREMENT,latitude DOUBLE, longitude DOUBLE, fechahora DATETIME);");
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

        scheduleGPS();
        stopGPS();
//        configure_button();
    }

    public void createDB()
    {
        mydatabase = openOrCreateDatabase("gpsreldb", MODE_PRIVATE,null);
        mydatabase.execSQL("CREATE TABLE IF NOT EXISTS GPS(id INTEGER PRIMARY KEY AUTOINCREMENT,latitude DOUBLE, longitude DOUBLE, fechahora DATETIME);");
    }

    public void insertDB()
    {
        mydatabase.execSQL("INSERT INTO GPS VALUES(" + location.getLatitude() + "," + location.getLongitude() +", strftime('%Y-%m-%d %H-%M-%f','now'));");
    }

    public void setCoordinates(String longitude, String latitude)
    {
        textView.setText("");
        textView.append("n " + longitude + " " + latitude);
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
                am[0].set(AlarmManager.RTC_WAKEUP, gpsExec.getTimeInMillis(), PendingIntent.getBroadcast(MainActivity.this, 100, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));

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

    public  void stopGPS()
    {
        stopbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //get Alarm manager instance
                AlarmManager am = (AlarmManager) MainActivity.this.getSystemService(ALARM_SERVICE);
                //build intent for midnight
                PendingIntent gpsPI = PendingIntent.getService(MainActivity.this, 0, new Intent("net.accella.sheduleexample.SilenceBroadcastReceiver"), PendingIntent.FLAG_UPDATE_CURRENT);
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
}