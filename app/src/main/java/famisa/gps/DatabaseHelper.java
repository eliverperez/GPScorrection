package famisa.gps;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Eliver on 30/06/18.
 */

public class DatabaseHelper extends SQLiteOpenHelper {

    Location punto_cero = new Location("");
    Location posicionamiento = new Location("");


    public DatabaseHelper(Context context) {
        super(context, "gpsreldb", null, 1);
    }

    public List getAll() {
        List gpsDetailsList = new ArrayList();
        String selectQuery = "SELECT latitude, longitude, fechahora FROM GPS";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        int i = 0;
        float distance = 0;
        //if TABLE has rows
        if (cursor.moveToFirst()) {
            //Loop through the table rows
            do {
                if( i == 0 )
                {
                    i++;
                    punto_cero.setLatitude(cursor.getDouble(0));
                    punto_cero.setLongitude(cursor.getDouble(1));
                    distance = 0;
                } else {
                    posicionamiento.setLatitude(cursor.getDouble(0));
                    posicionamiento.setLongitude(cursor.getDouble(1));
                    distance = punto_cero.distanceTo(posicionamiento);
                }
                gpsDetailsList.add(cursor.getDouble(0) + ", " + cursor.getDouble(1) + ", " + cursor.getString(2) + ", " + distance + "mts");
            } while (cursor.moveToNext());
        }
        db.close();
        return gpsDetailsList;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}