package db;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by Eliver on 06/09/17.
 */
public class send extends AsyncTask<String,Void,String> {

    private String lat;
    private String lon;
    private String ip = "http://famisa.com.mx/";
    private String location = "relgps/";
    private InputStream is = null;
    private StringBuilder sb = null;
    private static Context mContext;
    private String result = null;

    private int idDB;
    private String userDB;
    private String pwdDB;
    private int activeDB;
    private String nombre;

    public send(String lon, String lat) throws IOException {
        this.lat = lat;
        this.lon = lon;
    }

    protected void onPreExecute(){
//        pDialog = new ProgressDialog(
//                mContext);
//        pDialog.setMessage("Autenticando..");
//        pDialog.setIndeterminate(true);
//        pDialog.setCancelable(false);
//        pDialog.show();
    }

    @Override
    protected String doInBackground(String... arg0) {
        return sendDB();
    }

    @Override
    protected void onPostExecute(String result){
//        if(result.equals("1")) {
//            Autenticacion.loginPassed();
//        } else {
//            Autenticacion.alerta("Fallo la autenticación", "Error");
//        }
//        pDialog.dismiss();
    }

    private String sendDB() {
        ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
//        int encontrado = 0;
        try {
            int k = 1;
            HttpClient httpclient = new DefaultHttpClient();
            String url = this.ip + this.location + "gps.php";
            HttpPost httppost = new HttpPost(url);
            nameValuePairs.add(new BasicNameValuePair("LAT",this.lat));
            nameValuePairs.add(new BasicNameValuePair("LON", this.lon));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            is = entity.getContent();
            if(is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is,"iso-8859-1"),8);
                sb = new StringBuilder();
                sb.append(reader.readLine() + "\n");
                String line = null;
                while((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
                is.close();
                result = sb.toString();
                Log.e("log_tag", "Cadena JSon " + result);
            }
            if(!result.substring(0, 4).equalsIgnoreCase("null")) {
                JSONArray jArray = new JSONArray(result);
                if(jArray != null && jArray.length()>0) {
                    JSONObject json_data = jArray.getJSONObject(0);
//                    idDB = json_data.getInt("id");
//                    userDB = json_data.getString("usuario");
//                    pwdDB = json_data.getString("pass");
//                    activeDB = json_data.getInt("activo");
//                    encontrado = json_data.getInt("encontrado");
//                    nombre = json_data.getString("nombrecompleto");
                } else {
                    return "0";
                }
            } else {
                return "0";
            }
        } catch (Exception e) {
            Log.e("log_tag", "Error in http connection " + e.toString());
        } finally {
            return "0";
        }
    }
}
