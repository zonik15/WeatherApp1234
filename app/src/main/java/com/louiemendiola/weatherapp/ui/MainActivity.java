package com.louiemendiola.weatherapp.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.louiemendiola.weatherapp.R;
import com.louiemendiola.weatherapp.model.Weather;
import com.louiemendiola.weatherapp.utils.ServiceHandler;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


/**
 * Created by LSMendiola on 11/8/2019.
 */

public class MainActivity extends Activity {
    TextView txtCityName;
    TextView txtTime;
    TextView txtDescription;
    ImageView imgIcon;
    TextView txtTemperature;
    TextView txtFahrenheit;
    TextView txtCelcius;
    double longitude;
    double latitude;
    LocationManager lm;
    LocationListener locationListener;
    final int REQUEST_FINE_LOCATION = 1;
    final int GPS_ENABLED = 8;
    boolean gps_enabled;
    boolean network_enabled;
    ArrayList<Weather> weatherList;
    double temperature;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        try {
            isLocationPermissionGranted();
        } catch (Exception ex) {
        }
        txtCityName = (TextView) findViewById(R.id.txtCityName);
        txtTime = (TextView) findViewById(R.id.txtTime);
        txtDescription = (TextView) findViewById(R.id.txtDescription);
        imgIcon = (ImageView) findViewById(R.id.imgIcon);
        txtTemperature = (TextView) findViewById(R.id.txtTemperature);
        txtFahrenheit = (TextView) findViewById(R.id.txtFahrenheit);
        txtFahrenheit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtCelcius.setTextColor(Color.BLUE);
                txtFahrenheit.setTextColor(Color.BLACK);
                txtTemperature.setText(String.format("%.2f",kelvinToFahrenheit(temperature)));
            }
        });
        txtCelcius = (TextView) findViewById(R.id.txtCelcius);
        txtCelcius.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtCelcius.setTextColor(Color.BLACK);
                txtFahrenheit.setTextColor(Color.BLUE);
                txtTemperature.setText(String.format("%.2f",kelvinToCelcius(temperature)));
            }
        });
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {

        }
        if (isLocationPermissionGranted()) {
            if (!gps_enabled && !network_enabled) {
                showGPS();
            } else {
                getLocationCoordinates();
            }
        }


    }

    public boolean isLocationPermissionGranted() {
        Boolean check;
        boolean granted;
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                //already granted
                check = true;
                getLocationCoordinates();
            } else {
                //not yet granted
                System.out.println("==============y");
                System.out.println(longitude + "======2======" + latitude);
                ActivityCompat.requestPermissions(this, new String[]{
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        android.Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                }, REQUEST_FINE_LOCATION);

                check = false;
            }
            granted = checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
            return check;


        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v("TAG", "Permission is granted");
            return true;
        }


    }

    public void showGPS() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_gps);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(true);
        Button btnOkay = (Button) dialog.findViewById(R.id.btnOkay);
        btnOkay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), GPS_ENABLED);
                dialog.dismiss();
            }
        });
        dialog.show();


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GPS_ENABLED) {
            switch (requestCode) {
                case GPS_ENABLED:
                    getLocationCoordinates();
                    break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_FINE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (!gps_enabled && !network_enabled) {
                        showGPS();
                    } else {
                        getLocationCoordinates();
                    }


                } else {
                    ///not granted
                }
            }
            break;
        }
        return;

    }

    public void getLocationCoordinates() {
        longitude = 0;
        latitude = 0;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = getLastKnownLocation();
//                lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//

        System.out.println(location + "====location");
        if (location != null) {
            longitude = location.getLongitude();
            latitude = location.getLatitude();

        }

        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                longitude = location.getLongitude();
                latitude = location.getLatitude();
//                txtCityName.setText(longitude + "");
//                txtTime.setText(latitude + "");
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
        if (longitude == 0.0 && latitude == 0.0) {
            longitude = 121.0;
            latitude = 14.58333;
            Toast.makeText(this, "It seems that your in an area with low signal, I cannot find your location. But for now I will show you the forecast in Manila City. You can try again later.", Toast.LENGTH_LONG).show();
        }
            new getWeather().execute();

    }

    private Location getLastKnownLocation() {

        lm = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = lm.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {

            @SuppressLint("MissingPermission")
            Location l = lm.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }

    public class getWeather extends AsyncTask<Void, Void, ArrayList<Weather>> {
        ProgressDialog pDialog;
        String city;


        @Override
        protected void onPreExecute() {
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setCancelable(false);
            pDialog.setMessage("Please wait...");
            pDialog.show();
        }


        @Override
        protected ArrayList<Weather> doInBackground(Void... voids) {
            JSONArray jsonArray = null;

            weatherList = new ArrayList<Weather>();
            ServiceHandler sh = new ServiceHandler();
            String jsonStr = sh.makeServiceCall("http://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude + "&APPID=3137e7d3d9387c8e5a5c9dff9fb51f34", ServiceHandler.GET);
            if (jsonStr != null) {
                try {

                    JSONObject jsonObj = new JSONObject(jsonStr);
                    jsonArray = jsonObj.getJSONArray("weather");
                    JSONObject jsonObjectMain;
                    jsonObjectMain = jsonObj.getJSONObject("main");
                    temperature = jsonObjectMain.getDouble("temp");
                    city = jsonObj.getString("name");

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject c = jsonArray.getJSONObject(i);

                        int id = c.getInt("id");
                        String main = c.getString("main");
                        String description = c.getString("description");
                        String icon = c.getString("icon");


                        weatherList.add(new Weather(id, main, description, icon));

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Log.e("ServiceHandler", "Couldn't get any data from the url");
            }
            return weatherList;
        }

        @Override
        protected void onPostExecute(ArrayList<Weather> weathers) {
            if (pDialog.isShowing()) {
                pDialog.dismiss();
            }
            txtCityName.setText(city);
            String am_pm;
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR);
            int minutes = calendar.get(Calendar.MINUTE);
            int ampm = calendar.get(Calendar.AM_PM);
            if(ampm==0){
                am_pm = "AM";
            }else{
                am_pm = "PM";
            }
            int day = calendar.get(Calendar.DAY_OF_WEEK);
            switch (day){
                case Calendar.SUNDAY:
                    txtTime.setText("Sunday "+hour+":"+minutes+" "+am_pm);
                    break;
                case Calendar.MONDAY:
                    txtTime.setText("Monday "+hour+":"+minutes+" "+am_pm);
                    break;
                case Calendar.TUESDAY:
                    txtTime.setText("Tuesday "+hour+":"+minutes+" "+am_pm);
                    break;
                case Calendar.WEDNESDAY:
                    txtTime.setText("Wednesday "+hour+":"+minutes+" "+am_pm);
                    break;
                case Calendar.THURSDAY:
                    txtTime.setText("Thursday "+hour+":"+minutes+" "+am_pm);
                    break;
                case Calendar.FRIDAY:
                    txtTime.setText("Friday "+hour+":"+minutes+" "+am_pm);
                    break;
                case Calendar.SATURDAY:
                    txtTime.setText("Saturday "+hour+":"+minutes+" "+am_pm);
                    break;
            }

            txtDescription.setText(weathers.get(0).getDescription());
            txtTemperature.setText(String.format("%.2f",kelvinToFahrenheit(temperature)));

            Picasso.get().load("http://openweathermap.org/img/wn/"+weathers.get(0).getIcon()+"@2x.png").into(imgIcon);
        }
    }

    public static Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            // Log exception
            return null;
        }
    }
    public double kelvinToFahrenheit(double kelvin){
        double answer = 0.0;
        answer = ((kelvin * 9)/5)-459.67;
        return answer;
    }
    public double kelvinToCelcius(double kelvin){
        double answer = 0.0;
        answer = kelvin - 273.15;

        return answer;
    }
}
