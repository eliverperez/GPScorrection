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
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.text.Format;

public class socketdata extends AppCompatActivity {

    private EditText miIP;
    private EditText serverIP;

    private Button ping;
    private Button saveIP;

    SQLiteDatabase mydatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.socketdata);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        miIP = (EditText) findViewById(R.id.miIp_txt);
        serverIP = (EditText) findViewById(R.id.serverIp_txt);

        ping = (Button) findViewById(R.id.ping_btn);
        saveIP = (Button) findViewById(R.id.saveIP_btn);

        DatabaseHelper dbh = new DatabaseHelper(this);
        serverIP.setText(dbh.getIP());

//        miIP.setText(Utils.getIPAddress(true));
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String ipAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        miIP.setText(ipAddress);

        ping.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Google.com
                Toast.makeText(socketdata.this, "Haciendo ping a la ip: " + serverIP.getText(), Toast.LENGTH_SHORT).show(); //Ping works
                if (pingIP())
                {
                    //Boolean variable named network
                    Toast.makeText(socketdata.this, "Comunicación exitosa con la ip: " + serverIP.getText(), Toast.LENGTH_SHORT).show(); //Ping works
                }
                else
                {
                    Toast.makeText(socketdata.this, "No fue posible establecer una conexión con la ip: " + serverIP.getText(), Toast.LENGTH_SHORT).show(); //Ping doesnt work
                }
            }
        });

        saveIP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mydatabase = openOrCreateDatabase("gpsreldb", MODE_PRIVATE,null);
                mydatabase.execSQL("UPDATE SERVER SET ip = '" + serverIP.getText() + "';");
                Toast.makeText(socketdata.this, "La ip: " + serverIP.getText() + " ha sido almacenada.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean pingIP()
    {
        System.out.println("executeCommand");
        Runtime runtime = Runtime.getRuntime();
        try
        {
            Process  mIpAddrProcess = runtime.exec("/system/bin/ping -c 1 " + serverIP.getText());
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
}
