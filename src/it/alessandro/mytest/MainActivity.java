package it.alessandro.mytest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends FragmentActivity {

	private static final String TAG_RESULTS = "results";
	private static final String TAG_ID = "id";
	private static final String TAG_NAME = "name";
	private static final String TAG_LAT = "lat";
	private static final String TAG_LON = "lng";
	private static final String TAG_GEOMETRY = "geometry";
	private static final String TAG_LOCATION = "location";

	private static final String P_LOCATION = "location";
	private static final String P_KEY = "key";
	private static final String P_RADIUS = "radius";
	private static final String P_TYPES = "types";
	private static final String P_SENSOR = "sensor";
	
	private static final double TURIN_LAT = 45.0709;
	private static final double TURIN_LON = 7.6858;

	private static final String PREFS_NAME = "littleapp";
	private static final String SAVED_LIST = "saved_list";

	private static final String LIST_FRAGMENT = "list";

	private static final String DOMAIN = "https://maps.googleapis.com/";
	private static final String ACTION_GETPLACES = DOMAIN + "maps/api/place/search/json";

	private boolean isMapVisible = false;
	private PlacesListFragment listFragment;
	private SupportMapFragment mapFragment;

	double longitude = TURIN_LON;
	double latitude = TURIN_LAT;

	private GoogleMap mMap = null;

	// Hashmap for ListView
	ArrayList<HashMap<String, String>> placesList = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Acquire a reference to the system Location Manager
		LocationManager locationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);

		// Define a listener that responds to location updates
		LocationListener locationListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				// Called when a new location is found by the network location
				// provider.
				longitude = location.getLongitude();
				latitude = location.getLatitude();
			}

			public void onStatusChanged(String provider, int status,
					Bundle extras) {
			}

			public void onProviderEnabled(String provider) {
			}

			public void onProviderDisabled(String provider) {
			}
		};

		// Register the listener with the Location Manager to receive location
		// updates
		locationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
				0, locationListener);
		// Restore preferences
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		String places = settings.getString(SAVED_LIST, null);

		if (places != null) {
			System.out.println("stored places found");
			try {
				JSONObject jobj = new JSONObject(places);
				placesList = parseJSON(jobj);
			} catch (JSONException e) {
				// TODO: handle exception
			}
		}
		if (findViewById(R.id.fragment_container) != null) {

			if (savedInstanceState != null) {
				return;
			}

			listFragment = new PlacesListFragment();

			GoogleMapOptions options = new GoogleMapOptions();
			options.mapType(GoogleMap.MAP_TYPE_NORMAL).camera(
					new CameraPosition(new LatLng(latitude, longitude),
							(float) 13.0, (float) 0, (float) 112.5));
			mapFragment = SupportMapFragment.newInstance(options);

			// Add the fragment to the 'fragment_container' FrameLayout
			getSupportFragmentManager().beginTransaction()
					.add(R.id.fragment_container, listFragment, LIST_FRAGMENT)
					.add(R.id.fragment_container, mapFragment)
					.hide(mapFragment).commit();

			if (placesList != null) {
				ListAdapter adapter = new SimpleAdapter(MainActivity.this,
						placesList, R.layout.list_item,
						new String[] { TAG_NAME }, new int[] { R.id.name });
				listFragment.setListAdapter(adapter);
				drawMarkers();
			} else {
				new GetPlacesTask().execute(latitude, longitude);
			}

			View map = findViewById(R.id.map);
			map.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					if (!isMapVisible) {
						isMapVisible = true;
						FragmentTransaction transaction = getSupportFragmentManager()
								.beginTransaction();
						transaction.show(mapFragment);
						transaction.commit();
						drawMarkers();
					} else {
						isMapVisible = false;
						if (listFragment != null) {
							FragmentTransaction transaction = getSupportFragmentManager()
									.beginTransaction();
							transaction.hide(mapFragment);
							transaction.show(listFragment);
							transaction.commit();
						}
					}
				}
			});
			View refresh = findViewById(R.id.refresh);
			refresh.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					new GetPlacesTask().execute(latitude, longitude);
				}

			});
		}
	}

	public ArrayList<HashMap<String, String>> parseJSON(JSONObject obj) {

		ArrayList<HashMap<String, String>> tmpList = null;
		tmpList = new ArrayList<HashMap<String, String>>();
		JSONArray results = null;

		try {
			results = obj.getJSONArray(TAG_RESULTS);
			for (int i = 0; i < results.length(); i++) {
				JSONObject c = results.getJSONObject(i);
				String id = c.getString(TAG_ID);
				String name = c.getString(TAG_NAME);
				JSONObject geometry = c.getJSONObject(TAG_GEOMETRY);
				JSONObject location = geometry.getJSONObject(TAG_LOCATION);
				String lat = String.valueOf(location.getDouble(TAG_LAT));
				String lon = String.valueOf(location.getDouble(TAG_LON));
				HashMap<String, String> map = new HashMap<String, String>();

				map.put(TAG_ID, id);
				map.put(TAG_NAME, name);
				map.put(TAG_LAT, lat);
				map.put(TAG_LON, lon);
				tmpList.add(map);
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return tmpList;
	}

	private void drawMarkers(){
		if (mMap == null) {
			// Try to obtain the map from the
			// SupportMapFragment.
			mMap = mapFragment.getMap();
		}
		// Check if we were successful in obtaining the map.
		if (mMap != null) {
			mMap.clear();
			for (int i = 0; i < placesList.size(); i++) {
				HashMap<String, String> tmp = placesList.get(i);
				double lat = Double.parseDouble(tmp
						.get(TAG_LAT));
				double lon = Double.parseDouble(tmp
						.get(TAG_LON));
				String name = tmp.get(TAG_NAME);
				mMap.addMarker(new MarkerOptions().position(
						new LatLng(lat, lon)).title(name));

			}

		}
	}
	
	private class GetPlacesTask extends
			AsyncTask<Double, Void, ArrayList<HashMap<String, String>>> {
		@Override
		protected ArrayList<HashMap<String, String>> doInBackground(
				Double... coords) {
			double longitude = coords[1];
			double latitude = coords[0];
			HashMap<String, String> params = new HashMap<String, String>();
			
			try {
				params.put(P_LOCATION, latitude + "," + longitude);
				params.put(P_RADIUS, "1000");
				params.put(P_TYPES, "restaurant");
				params.put(P_KEY, "AIzaSyDClGU4fSgxzhFcpZVdib68LsM5QTvewLs");
				params.put(P_SENSOR, "true");
				JSONObject response = ServerUtilities
//						.getJSON(
//								ACTION_GETPLACES
//										+ "location="
//										+ latitude
//										+ ","
//										+ longitude
//										+ "&radius=1000&types=restaurant&sensor=true&key=AIzaSyDClGU4fSgxzhFcpZVdib68LsM5QTvewLs",
//								"");
				.getJSON(ACTION_GETPLACES, params);
				SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
				SharedPreferences.Editor editor = settings.edit();
				editor.putString(SAVED_LIST, response.toString());
				editor.commit();

				return parseJSON(response);

			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(ArrayList<HashMap<String, String>> result) {
			super.onPostExecute(result);
			if (result != null) {
				ListAdapter adapter = new SimpleAdapter(MainActivity.this,
						result, R.layout.list_item, new String[] { TAG_NAME },
						new int[] { R.id.name });
				listFragment.setListAdapter(adapter);
				placesList = result;
				drawMarkers();			}

		}
	}

}