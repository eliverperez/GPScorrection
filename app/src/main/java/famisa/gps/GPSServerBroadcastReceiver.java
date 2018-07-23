package famisa.gps;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

/**
 * Created by Eliver on 30/06/18.
 */

public class GPSServerBroadcastReceiver extends BroadcastReceiver {

    private LocationManager locationManager;
    private LocationListener listener;
    MainActivity mainActivity;

    SQLiteDatabase mydatabase;
    File file;
    FileOutputStream fOut;
    OutputStreamWriter myOutWriter;
    Intent i;

    public  GPSServerBroadcastReceiver(MainActivity mainActivity)
    {
        this.mainActivity = mainActivity;
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        Toast.makeText(context, "GPS SERVER BROADCAST RECEIVER", Toast.LENGTH_LONG).show();
//        mainActivity = (MainActivity) context;
        mainActivity = new MainActivity();
        i = new Intent(context, sqlite.class);

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                i.putExtra("latitude", location.getLatitude());
                i.putExtra("longitude", location.getLongitude());
                mainActivity.getCorrection(location.getLatitude(), location.getLongitude());
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
                context.startActivity(i);
            }
        };
//        if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //   ActivityCompat#requestPermissions
//            //   here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
//                locationManager.requestLocationUpdates("gps", 30 * 60 * 1000, 0, listener);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates("gps", 0, 0, listener);
    }
}
