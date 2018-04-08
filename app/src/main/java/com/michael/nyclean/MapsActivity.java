package com.michael.nyclean;

import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    public static final String TAG = "MapsActivity";

    private GoogleMap mMap;
    private ListView mListOfBins;
    private ArrayList<HashMap<String, String>> locationsList;

    private ArrayList<RecycleBinLocation> mLocationsArray;
    private RecycleBinLocation[] mClosestLocations;

    private LatLng mMyLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
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
        final double DEG_LENGTH = 110.25;

        for (int i = 0;i<mLocationsArray.size();i++){
            double currBinLat = mLocationsArray.get(i).getLatitude(),
                    currBinLong = mLocationsArray.get(i).getLongitude();

            double distX = myLocation.latitude - currBinLat,
                    distY = ((myLocation.longitude - currBinLong) * Math.cos(currBinLat)),
                    dist =  DEG_LENGTH * Math.sqrt((distX * distX) + (distY * distY));
            mLocationsArray.get(i).setDistance(dist);
            mClosestLocations[i] = mLocationsArray.get(i);
        }
        log("Before sorting...");
        for (int i = 0; i < mClosestLocations.length; i++) {
            log(mClosestLocations[i].toString());
        }
        Arrays.sort(mClosestLocations);
        log("After sorting...");
        for (int i = 0; i < mClosestLocations.length; i++) {
            log(mClosestLocations[i].toString());
        }
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

                        // Put all the location details into the map
                        HashMap<String, String> newBinLocation = new HashMap<>();
                        newBinLocation.put("address", address);
                        newBinLocation.put("borough", borough);
                        newBinLocation.put("latitude", latitude);
                        newBinLocation.put("longitude", longitude);
                        newBinLocation.put("park_site_name", park_site_name);
                        newBinLocation.put("site_type", site_type);

                        // adding map object to locations list
                        locationsList.add(newBinLocation);

                        mListOfBins.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                                String lat = ((TextView)view.findViewById(R.id.latitutde)).getText().toString();
                                String lon = ((TextView)view.findViewById(R.id.longitude)).getText().toString();
                                LatLng loc = new LatLng(Double.parseDouble(lat), Double.parseDouble(lon));
                                mMap.addMarker(new MarkerOptions().position(loc).title("Recycle Bin Here!"));
                                mMap.moveCamera(CameraUpdateFactory.zoomTo(17f));
                                mMap.moveCamera(CameraUpdateFactory.newLatLng(loc));
                            }
                        });
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
            mMyLocation = new LatLng(40.7284738, -73.9950050);
            findClosestPlaces(mMyLocation);
            log("Size of mClosestLocation: " + mClosestLocations.length);




            ListAdapter adapter = new SimpleAdapter(MapsActivity.this, locationsList,
                    R.layout.list_item, new String[]{ "address","borough", "latitude", "longitude", "park_site_name", "site_type"},
                    new int[]{R.id.address, R.id.borough, R.id.latitutde, R.id.longitude, R.id.park_site_name, R.id.site_type});
            mListOfBins.setAdapter(adapter);

            log("5 closest...");
            // Display closest five RecycleBins on GoogleMaps
            for (int i = 0; i < 5; i++) {
                LatLng bin = new LatLng(mClosestLocations[i].getLatitude(), mClosestLocations[i].getLongitude());
                mMap.addMarker(new MarkerOptions().position(bin).title("Recycling Bin"));
                log(mClosestLocations[i].toString());
            }
            // Center and zoom in on the closest RecycleBin
            LatLng closest = new LatLng(mClosestLocations[0].getLatitude(), mClosestLocations[0].getLongitude());
            mMap.moveCamera(CameraUpdateFactory.zoomTo(17f));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(closest));
          //  onMapReady(mMap);
        }
    } // end ASyncTask

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        String markerTitle = "Recycle bin here!";

        // TODO: fix to be current location
        mMyLocation = new LatLng(40.7284738, -73.9950050);
        mMap.addMarker(new MarkerOptions().position(mMyLocation).title(markerTitle));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(17f));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(mMyLocation));

    }
}
