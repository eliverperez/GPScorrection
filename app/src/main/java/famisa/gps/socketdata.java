package famisa.gps;

import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetAddress;

public class socketdata extends AppCompatActivity {

    private EditText miIP;
    private EditText serverIP;

    private Button ping;
    private Button saveIP;

    SQLiteDatabase mydatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_db);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        miIP = (EditText) findViewById(R.id.miIp_txt);
        serverIP = (EditText) findViewById(R.id.serverIp_txt);

        ping = (Button) findViewById(R.id.ping_btn);
        saveIP = (Button) findViewById(R.id.saveIP_btn);

        DatabaseHelper dbh = new DatabaseHelper(this);
        serverIP.setText(dbh.getIP());

        miIP.setText(Utils.getIPAddress(true));

        ping.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Google.com
                try {
                    if (InetAddress.getByAddress("173.194.35.133".getBytes()).isReachable(1000)==true)
                    {
                        //Boolean variable named network
                        Toast.makeText(socketdata.this, "Comunicación exitosa con la ip: " + serverIP.getText(), Toast.LENGTH_SHORT).show(); //Ping works
                    }
                    else
                    {
                        Toast.makeText(socketdata.this, "No fue posible establecer una conexión con la ip: " + serverIP.getText(), Toast.LENGTH_SHORT).show(); //Ping doesnt work
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        saveIP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mydatabase = openOrCreateDatabase("gpsreldb", MODE_PRIVATE,null);
                mydatabase.execSQL("INSERT INTO SERVER(ip) VALUES('" + serverIP.getText() + "');");
                Toast.makeText(socketdata.this, "La ip: " + serverIP.getText() + " ha sido almacenada.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
