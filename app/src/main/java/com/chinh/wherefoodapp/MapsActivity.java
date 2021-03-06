package com.chinh.wherefoodapp;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.fragment.app.FragmentActivity;
import com.chinh.wherefoodapp.Webservices.RetrofitAPI;
import com.chinh.wherefoodapp.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    SupportMapFragment mapFragment;
    private List<LatLng> polylineList;
    private Marker marker;
    private float v;
    private double lat,lng;
    private Handler handler;
    private LatLng startPosition, endPosition;
    private int index, next;
    private Button btnGo;
    private EditText edtPlace;
    private String destination;
    private PolylineOptions polylineOptions, BlackPolylineOptions;
    private Polyline blackPolyline, greyPolyline;
    private LatLng myLocation;


    RetrofitAPI mService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        polylineList = new ArrayList<>();
        mapFragment.getMapAsync(MapsActivity.this);
//        btnGo = (Button)findViewById(R.id.btnSearch);
//        edtPlace = (EditText)findViewById(R.id.edtplace);
//        btnGo.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                destination = edtPlace.getText().toString();
//                destination = destination.replace(" ","+");
//                Log.d("Test_log", "Here" + destination);
//                mapFragment.getMapAsync(MapsActivity.this);
//
//            }
//        });
        mService = Common.getGoogleApi();

    }

    public Bitmap resizeMapIcons(String iconName,int width, int height){
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(),getResources().getIdentifier(iconName, "drawable", getPackageName()));
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);
        return resizedBitmap;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setTrafficEnabled(false);
        mMap.setIndoorEnabled(false);
        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        Intent intent = getIntent();

        double start_lat = intent.getDoubleExtra("lat", 0);
        double start_lng = intent.getDoubleExtra("lng", 0);

        double end_lat = intent.getDoubleExtra("current_lat", 0);
        double end_lng = intent.getDoubleExtra("current_lng", 0);

        // Add a marker in Sydney and move the camera
        LatLng start_point = new LatLng(start_lat, start_lng);
        LatLng destination = new LatLng(end_lat, end_lng);

        mMap.addMarker(new MarkerOptions().position(start_point).title("Location Place"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(start_point));
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                .target(googleMap.getCameraPosition().target)
                .zoom(17)
                .bearing(30)
                .tilt(45)
                .build()));
        String requestUrl = null;
        try{
            requestUrl = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "mode=driving&"+
                    "transit_routing_preference=less_driving&"+
                    "origin=" + start_point.latitude + "," + start_point.longitude + "&" +
                    "&destination=" + destination.latitude + "," + destination.longitude + "&" +
                    "&key=" + getResources().getString(R.string.API_KEY);
            Log.d("URL",requestUrl);
            mService.getDataFromGoogleApi(requestUrl)
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            try{
                                JSONObject jsonObject = new JSONObject(response.body().toString());
                                JSONArray jsonArray = jsonObject.getJSONArray("routes");
                                for(int i = 0;i<jsonArray.length();i++){
                                    JSONObject route = jsonArray.getJSONObject(i);
                                    JSONObject poly = route.getJSONObject("overview_polyline");
                                    String polyline = poly.getString("points");
                                    polylineList = decodePoly(polyline);
                                }
                                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                for(LatLng latlng:polylineList)
                                    builder.include(latlng);
                                LatLngBounds bounds = builder.build();
                                CameraUpdate mCameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds,2);
                                mMap.animateCamera(mCameraUpdate);

                                polylineOptions = new PolylineOptions();
                                polylineOptions.color(Color.GRAY);
                                polylineOptions.width(5);
                                polylineOptions.startCap(new SquareCap());
                                polylineOptions.endCap(new SquareCap());
                                polylineOptions.jointType(JointType.ROUND);
                                polylineOptions.addAll(polylineList);
                                greyPolyline = mMap.addPolyline(polylineOptions);

                                BlackPolylineOptions = new PolylineOptions();
                                BlackPolylineOptions.color(Color.BLACK);
                                BlackPolylineOptions.width(5);
                                BlackPolylineOptions.startCap(new SquareCap());
                                BlackPolylineOptions.endCap(new SquareCap());
                                BlackPolylineOptions.jointType(JointType.ROUND);
                                BlackPolylineOptions.addAll(polylineList);
                                blackPolyline = mMap.addPolyline(BlackPolylineOptions);

                                mMap.addMarker(new MarkerOptions().position(polylineList.get(polylineList.size()-1)));
                                ValueAnimator polylineAnimator = ValueAnimator.ofInt(0,100);
                                polylineAnimator.setDuration(2000);
                                polylineAnimator.setInterpolator(new LinearInterpolator());
                                polylineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                    @Override
                                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                        List<LatLng> points = greyPolyline.getPoints();
                                        int percentValue = (int) valueAnimator.getAnimatedValue();
                                        int size = points.size();
                                        int newPoints = (int) (size * (percentValue / 100.0f));
                                        List<LatLng> p = points.subList(0, newPoints);
                                        blackPolyline.setPoints(p);
                                    }
                                });
                                polylineAnimator.start();

                                marker = mMap.addMarker(new MarkerOptions().position(start_point)
                                        .flat(true)
                                        .icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons("oto",40,80))));

                                handler = new Handler();
                                index = -1;
                                next = 1;
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (index < polylineList.size()-1){
                                            index++;
                                            next = index + 1;
                                        }
                                        if (index < polylineList.size()-1){
                                            startPosition = polylineList.get(index);
                                            endPosition = polylineList.get(next);
                                        }
                                        else {
                                            Toast.makeText(getApplicationContext(), "Successful.", Toast.LENGTH_LONG).show();
                                            // Giao hang thanh cong - Co the viet them tai day
                                            return;
                                        }
                                        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0,1);
                                        valueAnimator.setDuration(3000);
                                        valueAnimator.setInterpolator(new LinearInterpolator());
                                        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                            @Override
                                            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                                v = valueAnimator.getAnimatedFraction();
                                                lng = v * endPosition.longitude + (1 - v)
                                                        * startPosition.longitude;
                                                lat = v * endPosition.latitude + (1 - v)
                                                        * startPosition.latitude;
                                                LatLng newPos = new LatLng(lat,lng);
                                                marker.setPosition(newPos);
                                                marker.setAnchor(0.5f, 0.5f);
                                                marker.setRotation(getBearing(startPosition, newPos));
                                                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                                                        .target(newPos)
                                                        .zoom(15.5f)
                                                        .build()));
                                            }

                                        });
                                        valueAnimator.start();
                                        handler.postDelayed(this,3000);
                                    }
                                },3000);




                            }
                            catch(Exception e){
                                e.printStackTrace();
                            }

                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(MapsActivity.this, ""+t.getMessage(), Toast.LENGTH_SHORT).show();

                        }
                    });
        }
        catch (Exception e){
            e.printStackTrace();
        }


    }

    private float getBearing(LatLng startPosition, LatLng newPos) {
        double lat = Math.abs(startPosition.latitude - newPos.latitude);
        double lng = Math.abs(startPosition.longitude - newPos.longitude);

        if (startPosition.latitude < newPos.latitude && startPosition.longitude < newPos.longitude)
            return (float) (Math.toDegrees(Math.atan(lng/lat)));
        else if (startPosition.latitude >= newPos.latitude && startPosition.longitude < newPos.longitude)
            return (float) ((90-Math.toDegrees(Math.atan(lng/lat)))+90);
        else if (startPosition.latitude >= newPos.latitude && startPosition.longitude >= newPos.longitude)
            return (float) (Math.toDegrees(Math.atan(lng/lat))+180);
        else if (startPosition.latitude < newPos.latitude && startPosition.longitude >= newPos.longitude)
            return (float) ((90-Math.toDegrees(Math.atan(lng/lat)))+270);
        return -1;

    }

    private List<LatLng> decodePoly(String encoded) {
        List poly = new ArrayList();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }
}