package com.michael.nyclean;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    public static final String TAG = "MapsActivity";

    private FusedLocationProviderClient mFusedLocationProviderClient;

    private GoogleMap mMap;
    private ListView mListOfBins;
    private ArrayList<HashMap<String, String>> locationsList;

    private ArrayList<RecycleBinLocation> mLocationsArray;
    private RecycleBinLocation[] mClosestLocations;

    private LatLng mMyLocation;

    private final DecimalFormat df = new DecimalFormat("#0.00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        if ( Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission( getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission( getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            mFusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            log("Lat: " + location.getLatitude() + " : " + "Lon: " + location.getLongitude());
                            mMyLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        }
                    });
        else
            log("COULD NOT GET LOC");

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        mListOfBins = (ListView) findViewById(R.id.list_of_bins);
        new GetBinLocations().execute(); // get the locations and populate the list
    }

    private void log(String msg){
        Log.i(TAG, msg);
    }

    private void findClosestPlaces(LatLng myLocation) {
        mClosestLocations = new RecycleBinLocation[mLocationsArray.size()];
        final double DEG_LENGTH = 68.5061739;

        for (int i = 0;i<mLocationsArray.size();i++){
            double currBinLat = mLocationsArray.get(i).getLatitude(),
                    currBinLong = mLocationsArray.get(i).getLongitude();

            double distX = myLocation.latitude - currBinLat,
                    distY = ((myLocation.longitude - currBinLong) * Math.cos(currBinLat)),
                    dist =  DEG_LENGTH * Math.sqrt((distX * distX) + (distY * distY));
            mLocationsArray.get(i).setDistance(dist);
            mClosestLocations[i] = mLocationsArray.get(i);
        }
        Arrays.sort(mClosestLocations);
    }

    private class HttpHandler {

        public HttpHandler() {}

        public String makeServiceCall(String reqUrl) {
            String response = null;
            try {
                URL url = new URL(reqUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                // read the response
                InputStream in = new BufferedInputStream(conn.getInputStream());
                response = convertStreamToString(in);
            } catch (MalformedURLException e) {
                Log.e(TAG, "MalformedURLException: " + e.getMessage());
            } catch (ProtocolException e) {
                Log.e(TAG, "ProtocolException: " + e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "IOException: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Exception: " + e.getMessage());
            }
            return response;
        }

        private String convertStreamToString(InputStream is) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();

            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return sb.toString();
        }
    }

    private class GetBinLocations extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(MapsActivity.this,"Nearest recycle bins are loading!\nJust a sec...",Toast.LENGTH_LONG).show();

        }

        @Override
        protected Void doInBackground(Void... arg0) {
            HttpHandler sh = new HttpHandler();
            // Making a request to url and getting response
            String url = "https://data.cityofnewyork.us/resource/ggvk-gyea.json?$$app_token=TsaFLCw8emTtCfbOt0MsufYue";
            String jsonStr = sh.makeServiceCall(url);

            //     Log.e(TAG, "Response from url: " + jsonStr);
            if (jsonStr != null) {
                try {
                    JSONArray jsonArray = new JSONArray(jsonStr);
                    /*
                     // Description of jsonobj[i]
                       {
                        "address": "E 227 St/Bronx River Pkway",
                        "borough": "Bronx",
                        "latitude": "40.890848989",
                        "longitude": "-73.864223918",
                        "park_site_name": "227th St. Plgd",
                        "site_type": "Subproperty"
                        }
                    */

                    locationsList = new ArrayList<>();
                    mLocationsArray = new ArrayList<>();

                    // looping through all bin locations
                    for (int i = 0; i < jsonArray.length(); i++) {

                        // Get the current recycle bin JSON object
                        JSONObject c = jsonArray.getJSONObject(i);

                        String address = c.isNull("address") ? "None" : c.getString("address");
                        String borough = c.isNull("borough") ? "None" : c.getString("borough");

                        if(c.isNull("latitude") || c.isNull("longitude")){
                            continue;
                        }
                        String latitude = c.getString("latitude");
                        String longitude = c.getString("longitude");
                        String park_site_name = c.isNull("park_site_name") ? "None" : c.getString("park_site_name");
                        String site_type = c.isNull("site_type") ? "None" : c.getString("site_type");


                        RecycleBinLocation newBinLoc = new RecycleBinLocation(address,
                                                                              borough,
                                                                              Double.parseDouble(latitude),
                                                                              Double.parseDouble(longitude),
                                                                              park_site_name,
                                                                              site_type);
                        mLocationsArray.add(newBinLoc);

                    }
                } catch (final JSONException e) {
                    Log.e(TAG, "Json parsing error: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Json parsing error: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }

            } else {
                Log.e(TAG, "Couldn't get json from server.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Couldn't get json from server.",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // Find out current location
            findClosestPlaces(mMyLocation);

            for(int i = 0; i < mClosestLocations.length; i++){
                // Put all the location details into the map
                HashMap<String, String> newBinLocation = new HashMap<>();
                newBinLocation.put("address", mClosestLocations[i].getAddress());
                newBinLocation.put("borough", mClosestLocations[i].getBorough());
                newBinLocation.put("latitude", String.valueOf(mClosestLocations[i].getLatitude()));
                newBinLocation.put("longitude", String.valueOf(mClosestLocations[i].getLongitude()));
                newBinLocation.put("distance", String.valueOf(df.format(mClosestLocations[i].getDistance()) + " miles"));
                newBinLocation.put("park_site_name", mClosestLocations[i].getParkSiteName());
                newBinLocation.put("site_type", mClosestLocations[i].getSiteType());

                // adding the map object to locations list
                locationsList.add(newBinLocation);
            }

            mListOfBins.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                    String lat = ((TextView)view.findViewById(R.id.latitutde)).getText().toString();
                    String lon = ((TextView)view.findViewById(R.id.longitude)).getText().toString();
                    LatLng loc = new LatLng(Double.parseDouble(lat), Double.parseDouble(lon));
                    mMap.addMarker(new MarkerOptions().position(loc).title("Recycle Bin Here!").icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_bin)));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 17f));
                }
            });
            ListAdapter adapter = new SimpleAdapter(MapsActivity.this, locationsList,
                    R.layout.list_item, new String[]{ "address","borough", "latitude", "longitude", "distance", "park_site_name", "site_type"},
                    new int[]{R.id.address, R.id.borough, R.id.latitutde, R.id.longitude, R.id.distance, R.id.park_site_name, R.id.site_type});
            mListOfBins.setAdapter(adapter);

            // Display closest five RecycleBins on GoogleMaps
            for (int i = 0; i < 5; i++) {
                LatLng bin = new LatLng(mClosestLocations[i].getLatitude(), mClosestLocations[i].getLongitude());
                mMap.addMarker(new MarkerOptions().position(bin).title("Recycling Bin").icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_bin)));
            }
            // Center and zoom in on the closest RecycleBin
            final LatLng closest = new LatLng(mClosestLocations[0].getLatitude(), mClosestLocations[0].getLongitude());
            mMap.addMarker(new MarkerOptions().position(mMyLocation).title("Your Current Location!").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
      //      mMap.addPolyline(new PolylineOptions().add(closest, mMyLocation).width(5f).color(Color.BLUE));


            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mMyLocation, 17f));

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(closest, 17f));
                }
            }, 2000);
        }
    } // end ASyncTask

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }
}
