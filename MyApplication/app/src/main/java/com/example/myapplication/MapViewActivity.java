package com.example.myapplication;


import android.Manifest;
import android.app.AlertDialog;
import android.os.SystemClock;
import android.view.WindowManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import net.daum.android.map.coord.MapCoord;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPolyline;
import net.daum.mf.map.api.MapReverseGeoCoder;
import net.daum.mf.map.api.MapView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.Vector;


public class MapViewActivity
        extends AppCompatActivity
        implements MapView.CurrentLocationEventListener, MapReverseGeoCoder.ReverseGeoCodingResultListener {

    private static final String LOG_TAG = "MapViewActivity";

    private MapView mMapView;

    final long millis = 1;
    final long second = 1000 * millis;
    final long minute = 60 * second;
    final long hour = 60 * minute;
    final long traceClassifier[] =
            {
                    24 * hour, // 48 * hour
                    2 * minute, // 30 * minute,
                    1 * minute, // 8 * minute,
                    30 * second, // 3 * minute,
                    15 * second, // minute,
                    1 * millis
            };


    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS  = {Manifest.permission.ACCESS_FINE_LOCATION};


    private TreeMap<String, TreeMap<Date, MapPoint>> traces = new TreeMap<>();
    private final long epoch_LocalTime = System.currentTimeMillis();
    private final long epoch_Device = SystemClock.elapsedRealtime();

    private FirebaseDatabase mdatabase = FirebaseDatabase.getInstance();
    private DatabaseReference myRef = mdatabase.getReference();

    private String myUID = null;

    public void addOtherLocationUpdateListener(){
        myRef.child("RoomNumber").child("Location").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                if(dataSnapshot.getKey() != myUID) {
                    String uid = dataSnapshot.getKey();
                    for (DataSnapshot nodes : dataSnapshot.getChildren()) {
                        long node_time = Long.valueOf(nodes.getKey());
                        Location l = nodes.getValue(Location.class);
                        if(traces.get(uid).size() != 0) {
                            if (node_time > traces.get(uid).lastKey().getTime()) {
                                MapPoint loc = MapPoint.mapPointWithGeoCoord(l.latitude,l.longitude);
                                traces.get(uid).put(new Date(node_time),loc);
                            }
                        }
                        else{
                            MapPoint loc = MapPoint.mapPointWithGeoCoord(l.latitude,l.longitude);
                            traces.get(uid).put(new Date(node_time),loc);
                        }
                    }

                }
                //traces.get()
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        myUID = getIntent().getStringExtra("uid");
        setContentView(R.layout.map_view);

        mMapView = findViewById(R.id.map_view);
        //mMapView.setDaumMapApiKey(MapApiConst.DAUM_MAPS_ANDROID_APP_API_KEY);
        mMapView.setCurrentLocationEventListener(this);

        if (!checkLocationServicesStatus()) {

            showDialogForLocationServiceSetting();
        }
        else {

            checkRunTimePermission();
        }

        traces.put(myUID, new TreeMap<Date, MapPoint>());

        addOtherLocationUpdateListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mMapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
        mMapView.setShowCurrentLocationMarker(false);
    }

    @Override
    public void onCurrentLocationUpdate(
            MapView mapView,
            MapPoint currentLocation,
            float accuracyInMeters) {
        long now_Device = SystemClock.elapsedRealtime();
        long now_LocalTime = now_Device - epoch_Device + epoch_LocalTime;
        /*                 = System.currentTimeMillis();
         *
         * Currently using elapsed real time.
         * To use currentTimeLillis, be aware that...
         *
         * 1) the timer can be shifted from some time correction & sync mechanisms of the system, or time zone changes.
         *    Note that the TreeMap 'traces' will be corrupted compared to what really happened, in case of a timer shift occurs.
         *    Using epoch_LocalTime with epoch_Device can solve this problem.
         *
         * 2) the timer of each device will differ from each other.
         *
         */

        MapPoint.GeoCoordinate mapPointGeo = currentLocation.getMapPointGeoCoord();
        Log.w(
                LOG_TAG,
                String.format(
                        "MapView onCurrentLocationUpdate (%f,%f) at %s, accuracy (%f)",
                        mapPointGeo.latitude,
                        mapPointGeo.longitude,
                        new Date(now_LocalTime).toString(),
                        accuracyInMeters));

        Location locData = new Location(currentLocation.getMapPointGeoCoord().latitude
                ,currentLocation.getMapPointGeoCoord().longitude);

        //drawPath(myUID,now_LocalTime,mapView,currentLocation);

        sendGPS(now_LocalTime,locData);
    }

    private void drawPath(String myUID, long now_LocalTime, MapView mapView, MapPoint currentLocation)
    {
        Date currentTime = new Date(now_LocalTime);
        traces.get(myUID).put(currentTime, currentLocation);

        Vector<NavigableMap<Date, MapPoint>> tracesClassified = new Vector<>();

        for (int i = 0; i < 5; i++) {
            try {
                tracesClassified.add(
                        traces.get(myUID).subMap(
                                traces.get(myUID).higherKey(new Date(now_LocalTime - traceClassifier[i])),
                                true,
                                traces.get(myUID).higherKey(new Date(now_LocalTime - traceClassifier[i + 1])),
                                true));
            }
            catch (NullPointerException e)
            {
                tracesClassified.add(null);
            }
        }

        for(MapPolyline target = mapView.findPolylineByTag(0); null != target; target = mapView.findPolylineByTag(0))
        {
            mapView.removePolyline(target);
        }

        int lineAlpha = 0x20;

        for (NavigableMap<Date, MapPoint> map : tracesClassified) {
            MapPolyline line = new MapPolyline();
            line.setTag(0);
            line.setLineColor(android.graphics.Color.argb(lineAlpha, 0x00, 0xFF, 0x00));
            lineAlpha += 0x30;

            if(null != map)
            {
                for (MapPoint p : map.values()) {
                    line.addPoint(p);
                }
                mapView.addPolyline(line);
            }
        }
    }

    public void sendGPS(long now_LocalTime, Location locData){

        Log.w(LOG_TAG, String.format("%s",myUID));

        myRef.child("RoomNumber").child("Location").child(myUID).child(Long.toString(now_LocalTime)).setValue(locData);

        /*MapPoint.GeoCoordinate mapPointGeo = currentLocation.getMapPointGeoCoord();
        String url = "http://IP:Port";

        //JSON형식으로 데이터 통신을 진행
        JSONObject position = new JSONObject();
        try {
            position.put("latitude", Double.toString(mapPointGeo.latitude));
            position.put("longitude", Double.toString(mapPointGeo.longitude));
            position.put("userid", "SomeUserID");
            String jsonString = position.toString(); //완성된 json 포맷

            final RequestQueue requestQueue = Volley.newRequestQueue(MapViewActivity.this);
            final JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, position, new Response.Listener<JSONObject>() {

                //Get Response
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.toString());

                        String latitude = jsonObject.getString("latitude");
                        String longitude = jsonObject.getString("longitude");
                        String userid = jsonObject.getString("userid");

                        Toast.makeText(getApplicationContext(), "latitude : " + latitude + " longitude : " + longitude + " UserID : " + userid, Toast.LENGTH_LONG).show();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {   //when error
                @Override
                public void onErrorResponse(VolleyError error) {
                    error.printStackTrace();
                    Toast.makeText(MapViewActivity.this, error.toString(), Toast.LENGTH_SHORT).show();
                }
            });
            jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            requestQueue.add(jsonObjectRequest);

        } catch (JSONException e) {
            e.printStackTrace();
        }*/
    }

    @Override
    public void onCurrentLocationDeviceHeadingUpdate(MapView mapView, float v) {

    }

    @Override
    public void onCurrentLocationUpdateFailed(MapView mapView) {

    }

    @Override
    public void onCurrentLocationUpdateCancelled(MapView mapView) {

    }

    @Override
    public void onReverseGeoCoderFoundAddress(MapReverseGeoCoder mapReverseGeoCoder, String s) {
        mapReverseGeoCoder.toString();
        onFinishReverseGeoCoding(s);
    }

    @Override
    public void onReverseGeoCoderFailedToFindAddress(MapReverseGeoCoder mapReverseGeoCoder) {
        onFinishReverseGeoCoding("Fail");
    }

    private void onFinishReverseGeoCoding(String result) {
//        Toast.makeText(LocationDemoActivity.this, "Reverse Geo-coding : " + result, Toast.LENGTH_SHORT).show();
    }




    /*
     * ActivityCompat.requestPermissions를 사용한 퍼미션 요청의 결과를 리턴받는 메소드입니다.
     */
    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {

        if ( permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {

            // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면

            boolean check_result = true;


            // 모든 퍼미션을 허용했는지 체크합니다.

            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }


            if ( check_result ) {
                Log.d("@@@", "start");
                //위치 값을 가져올 수 있음
                mMapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeading);
            }
            else {
                // 거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합니다.2 가지 경우가 있습니다.

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])) {

                    Toast.makeText(MapViewActivity.this, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.", Toast.LENGTH_LONG).show();
                    finish();


                }else {

                    Toast.makeText(MapViewActivity.this, "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ", Toast.LENGTH_LONG).show();

                }
            }

        }
    }

    void checkRunTimePermission(){

        //런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크합니다.
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(MapViewActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);


        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED ) {

            // 2. 이미 퍼미션을 가지고 있다면
            // ( 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된 걸로 인식합니다.)


            // 3.  위치 값을 가져올 수 있음
            mMapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeading);


        } else {  //2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.

            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            if (ActivityCompat.shouldShowRequestPermissionRationale(MapViewActivity.this, REQUIRED_PERMISSIONS[0])) {

                // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                Toast.makeText(MapViewActivity.this, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(MapViewActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);


            } else {
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(MapViewActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }

        }

    }



    //여기부터는 GPS 활성화를 위한 메소드들
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MapViewActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하실래요?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case GPS_ENABLE_REQUEST_CODE:

                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {

                        Log.d("@@@", "onActivityResult : GPS 활성화 되있음");
                        checkRunTimePermission();
                        return;
                    }
                }

                break;
        }
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
}