package com.example.myapplication;


import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;

import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.Vector;


public class MapViewActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final String[] REQUIRED_PERMISSIONS  = { Manifest.permission.ACCESS_FINE_LOCATION };

    private static final String LOG_TAG = "MapViewActivity";

    static final long millis = 1;
    static final long second = 1000 * millis;
    static final long minute = 60 * second;
    static final long hour = 60 * minute;
    static final long traceClassifier[] =
            {
                    24 * hour, // 48 * hour
                    2 * minute, // 30 * minute,
                    1 * minute, // 8 * minute,
                    30 * second, // 3 * minute,
                    15 * second, // minute
            };

    static final int lineColors[] = {
            android.graphics.Color.rgb(0xFF, 0x00, 0x00),
            android.graphics.Color.rgb(0xFF, 0x8C, 0x00),
            android.graphics.Color.rgb(0xFF, 0xFF, 0x00),
            android.graphics.Color.rgb(0x00, 0x00, 0xFF),
            android.graphics.Color.rgb(0xFF, 0x00, 0x8C)
    };

    private class Room {
        private String rID;
        private int counterForLineTag = 0;
        private ArrayList<Member> members = new ArrayList<>();
        final private ChildEventListener roomListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                String newUser = dataSnapshot.getKey();
                addMember(newUser);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };

        public Room(String roomID) {
            rID = roomID;
            myRef.child(rID).child("MyLocation").addChildEventListener(roomListener);
            Log.i(LOG_TAG, String.format("you joined to room: %s", rID));
        }

        public void fin() {
            myRef.child(rID).child("MyLocation").removeEventListener(roomListener);
            for (Member m : members) {
                m.fin();
            }
            members.clear();
            Log.i(LOG_TAG, String.format("you exited from room: %s", rID));
        }

        void addMember(String uID) {
            members.add(new Member(uID, ++counterForLineTag));
        }

        @UiThread
        void drawTraces(long now) {
            try {
                for (Member m : members) {
                    m.drawTrace(now);
                }
            }
            catch (ConcurrentModificationException e)
            {
                //
            }
        }
    }

    public class Member {
        private String uID;
        private Vector<Polyline> polylines;
        final private TreeMap<Date, LatLng> trace = new TreeMap<>();

        final private ChildEventListener userListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                long time_latest = Long.valueOf(dataSnapshot.getKey());
                double lat_latest = dataSnapshot.child("latitude").getValue(double.class);
                double lon_latest = dataSnapshot.child("longitude").getValue(double.class);

                Log.i(
                        LOG_TAG,
                        String.format("new data updated - uID: %s, time:%d, lat:%f, lon:%f",
                                uID,
                                time_latest,
                                lat_latest,
                                lon_latest));

                synchronized (trace) {
                    trace.put(new Date(time_latest), new LatLng(lat_latest, lon_latest));
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };

        @UiThread
        public Member(String userID, int Tag) {
            uID = userID;
            int lineColor =
                    (0 == myUID.compareTo(uID))
                            ? Color.rgb(0x00, 0xFF, 0x00)
                            : lineColors[Tag % lineColors.length];
            int lineAlpha = 0x20;
            float zIndex = (0 == myUID.compareTo(uID)) ?(1000f) :(-Float.valueOf(uID));

            myRef.child("RoomNumber").child("MyLocation").child(uID).addChildEventListener(userListener);
            Log.i(LOG_TAG, String.format("user joined to your room: %s", uID));

            polylines = new Vector<>();
            for (long x : traceClassifier) {
                polylines.add(map.addPolyline(new PolylineOptions()
                        .color(Color.argb(
                                lineAlpha,
                                Color.red(lineColor),
                                Color.green(lineColor),
                                Color.blue(lineColor)))
                        .zIndex(zIndex)
                        .startCap(new RoundCap())
                        .endCap(new RoundCap())
                        .jointType(JointType.ROUND)));
                lineAlpha += 0x30;
            }
        }

        @UiThread
        public void fin() {
            myRef.child("RoomNumber").child("MyLocation").child(uID).removeEventListener(userListener);

            for(Polyline l : polylines)
            {
                l.remove();
            }

            Log.i(LOG_TAG, String.format("user removed from your room: %s", uID));
        }

        @UiThread
        public void drawTrace(long now) {
            int traceClassifierPicker = traceClassifier.length - 1;

            Vector<LatLng> points = new Vector<>();
            synchronized (trace) {
                for (Map.Entry<Date, LatLng> entry : trace.descendingMap().entrySet()) {
                    if (now - traceClassifier[traceClassifierPicker] > entry.getKey().getTime()) {
                        points.add(entry.getValue());
                        polylines.get(traceClassifierPicker--).setPoints(points);
                        points = new Vector<>();
                    }

                    if (0 > traceClassifierPicker) {
                        break;
                    }

                    points.add(entry.getValue());
                }
                polylines.get(traceClassifierPicker).setPoints(points);
            }
        }
    }

    private final long epoch_LocalTime = System.currentTimeMillis();
    private final long epoch_Device = SystemClock.elapsedRealtime();

    final long pathRenewingPeriod = 500;

    private Timer t;
    private TimerTask task_drawingPaths = new TimerTask() {
        @Override
        public void run() {
            long now_Device = SystemClock.elapsedRealtime();
            long now_LocalTime = now_Device - epoch_Device + epoch_LocalTime;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    myRoom.drawTraces(now_LocalTime);
                }
            });
        }
    };

    private String myUID = null;

    private GoogleMap map;

    private Room myRoom;

    private FirebaseDatabase mdatabase = FirebaseDatabase.getInstance();
    private DatabaseReference myRef = mdatabase.getReference();


    @Override
    protected void onCreate(
            Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onCreate called.");
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.map_view);

        myUID = getIntent().getStringExtra("uid");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        myRoom = new Room("RoomNumber");
    }

    @Override
    public void onMapReady(
            final GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setMyLocationButtonEnabled(false);
        checkRunTimePermission();
        map.setOnMyLocationChangeListener(this::onLocationChanged); // WARNING: DEPRECATED

        t = new Timer();
        t.schedule(task_drawingPaths, 500, pathRenewingPeriod);
    }

    @Override
    protected void onDestroy() {
        Log.i(LOG_TAG, "onDestroy called.");
        super.onDestroy();

        t.cancel();
        myRoom.fin();
        myRoom = null;


        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        map = null;
    }

    @Override
    public void onRequestPermissionsResult(
            int permsRequestCode,
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
                checkRunTimePermission();
            }
            else {
                // 거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합니다. 2 가지 경우가 있습니다.
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])) {
                    Toast.makeText(
                            this,
                            "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.",
                            Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(
                            this,
                            "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다.",
                            Toast.LENGTH_LONG).show();
                }
                finish();
            }

        }
    }

    void checkRunTimePermission() {
        // 런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크합니다.
        int hasFineLocationPermission =
                ContextCompat.checkSelfPermission(MapViewActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED ) {
            // 2. 이미 퍼미션을 가지고 있다면
            // ( 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된 걸로 인식합니다.)
            // 3.  위치 값을 가져올 수 있음
            map.setMyLocationEnabled(true);
        } else {
            // 2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.
            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])) {
                // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                Toast.makeText(
                        this,
                        "이 앱을 실행하려면 위치 접근 권한이 필요합니다.",
                        Toast.LENGTH_LONG).show();
                // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(
                        this,
                        REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }
            else {
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(
                        this,
                        REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }

        }

    }

    public void onLocationChanged (
            Location location) {
        long now_Device = SystemClock.elapsedRealtime();
        long now_LocalTime = now_Device - epoch_Device + epoch_LocalTime;

        Log.i(
                LOG_TAG,
                String.format(
                        "MapView onCurrentLocationUpdate (%f,%f) at %s, accuracy (%f)",
                        location.getLatitude(),
                        location.getLongitude(),
                        new Date(now_LocalTime).toString(),
                        location.getAccuracy()));

        map.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
        sendGPS(now_LocalTime,new MyLocation(location.getLatitude(), location.getLongitude()));
    }

    public void sendGPS(long now, MyLocation locData) {
        myRef.child("RoomNumber").child("MyLocation").child(myUID).child(Long.toString(now)).setValue(locData);
    }
}

// TODO: use FusedLocationProviderClient
// TODO: add button to switch between compass view <-> static follow view
// TODO: use hitmap library

//
//
//import android.Manifest;
//import android.app.AlertDialog;
//import android.os.SystemClock;
//import android.view.WindowManager;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.location.LocationManager;
//import android.os.Bundle;
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//import androidx.appcompat.app.AppCompatActivity;
//import android.util.Log;
//import android.widget.Toast;
//
//import com.google.firebase.database.ChildEventListener;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//
//import net.daum.mf.map.api.MapPoint;
//import net.daum.mf.map.api.MapPolyline;
//import net.daum.mf.map.api.MapReverseGeoCoder;
//import net.daum.mf.map.api.MapView;
//
//import java.util.ArrayList;
//import java.util.ConcurrentModificationException;
//import java.util.Date;
//import java.util.Map;
//import java.util.Timer;
//import java.util.TimerTask;
//import java.util.TreeMap;
//import java.util.Vector;
//
//public class MapViewActivity
//        extends AppCompatActivity
//        implements MapView.CurrentLocationEventListener, MapReverseGeoCoder.ReverseGeoCodingResultListener {
//
//    private static final String LOG_TAG = "MapViewActivity";
//
//    private MapView mMapView;
//
//    final static long millis = 1;
//    final static long second = 1000 * millis;
//    final static long minute = 60 * second;
//    final static long hour = 60 * minute;
//    final static long traceClassifier[] =
//            {
//                    24 * hour, // 48 * hour
//                    2 * minute, // 30 * minute,
//                    1 * minute, // 8 * minute,
//                    30 * second, // 3 * minute,
//                    15 * second, // minute
//            };
//
//    final static int lineColors[] = {
//            android.graphics.Color.rgb(0xFF, 0x00, 0x00),
//            android.graphics.Color.rgb(0xFF, 0x8C, 0x00),
//            android.graphics.Color.rgb(0xFF, 0xFF, 0x00),
//            android.graphics.Color.rgb(0x00, 0x00, 0xFF),
//            android.graphics.Color.rgb(0xFF, 0x00, 0x8C)
//    };
//
//    final long pathRenewingPeriod = 500;
//
//    private Timer t;
//    private TimerTask task_drawingPaths = new TimerTask() {
//        @Override
//        public void run() {
//            long now_Device = SystemClock.elapsedRealtime();
//            long now_LocalTime = now_Device - epoch_Device + epoch_LocalTime;
//            myRoom.drawTraces(now_LocalTime, mMapView);
//        }
//    };
//
//    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
//    private static final int PERMISSIONS_REQUEST_CODE = 100;
//    String[] REQUIRED_PERMISSIONS  = {Manifest.permission.ACCESS_FINE_LOCATION};
//
//    private final long epoch_LocalTime = System.currentTimeMillis();
//    private final long epoch_Device = SystemClock.elapsedRealtime();
//
//    private FirebaseDatabase mdatabase = FirebaseDatabase.getInstance();
//    private DatabaseReference myRef = mdatabase.getReference();
//
//    private String myUID = null;
//
////    public void addOtherLocationUpdateListener(){
////        myRef.child("RoomNumber").child("MyLocation").addChildEventListener(new ChildEventListener() {
////            @Override
////            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
////
////            }
////
////            @Override
////            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
////                Log.i(LOG_TAG, "db loc updated!");
////                if(dataSnapshot.getKey() != myUID) {
////                    String uid = dataSnapshot.getKey();
////                    for (DataSnapshot nodes : dataSnapshot.getChildren()) {
////                        long node_time = Long.valueOf(nodes.getKey());
////                        MyLocation l = nodes.getValue(MyLocation.class);
////                        if(traces.get(uid).size() != 0) {
////                            if (node_time > traces.get(uid).lastKey().getTime()) {
////                                MapPoint loc = MapPoint.mapPointWithGeoCoord(l.latitude,l.longitude);
////                                traces.get(uid).put(new Date(node_time),loc);
////                            }
////                        }
////                        else{
////                            MapPoint loc = MapPoint.mapPointWithGeoCoord(l.latitude,l.longitude);
////                            traces.get(uid).put(new Date(node_time),loc);
////                        }
////                    }
////
////                }
////                //traces.get()
////            }
////
////            @Override
////            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}
////
////            @Override
////            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {}
////
////            @Override
////            public void onCancelled(@NonNull DatabaseError databaseError) {}
////        });
////    }
//
//    private class Room {
//        private String rID;
//        private int counterForLineTag = 0;
//        private ArrayList<Member> members = new ArrayList<>();
//        final private ChildEventListener roomListener = new ChildEventListener() {
//            @Override
//            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
//                String newUser = dataSnapshot.getKey();
//                addMember(newUser);
//            }
//
//            @Override
//            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
//
//            @Override
//            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}
//
//            @Override
//            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {}
//        };
//
//        public Room(String roomID) {
//            rID = roomID;
//            myRef.child(rID).child("MyLocation").addChildEventListener(roomListener);
//            Log.i(LOG_TAG, String.format("you joined to room: %s", rID));
//        }
//
//        public void fin() {
//            myRef.child(rID).child("MyLocation").removeEventListener(roomListener);
//            for (Member m : members) {
//                m.fin();
//            }
//            members.clear();
//            Log.i(LOG_TAG, String.format("you exited from room: %s", rID));
//        }
//
//        void addMember(String uID) {
//            members.add(new Member(uID, ++counterForLineTag));
//        }
//
//        void drawTraces(long now, MapView mapView) {
//            try {
//                for (Member m : members) {
//                    m.drawTrace(now, mapView);
//                }
//            }
//            catch (ConcurrentModificationException e)
//            {
//
//            }
//        }
//    }
//
//    public class Member {
//        private String uID;
//        private int lineTag;    // for line tag feature, which does not support long type
//        private int lineColor;
//        private TreeMap<Date, MapPoint> trace = new TreeMap<>();
//
//        final private ChildEventListener userListener = new ChildEventListener() {
//            @Override
//            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
//                long time_latest = Long.valueOf(dataSnapshot.getKey());
//                double lat_latest = dataSnapshot.child("latitude").getValue(double.class);
//                double lon_latest = dataSnapshot.child("longitude").getValue(double.class);
//                Log.i(LOG_TAG, String.format("new data updated - uID: %s, time:%d, lat:%f, lon:%f", uID, time_latest, lat_latest, lon_latest));
//                synchronized (trace) {
//                    trace.put(new Date(time_latest), MapPoint.mapPointWithGeoCoord(lat_latest, lon_latest));
//                }
//            }
//
//            @Override
//            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {}
//
//            @Override
//            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}
//
//            @Override
//            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {}
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {}
//        };
//
//        public Member(String userID, int Tag) {
//            uID = userID;
//            lineTag = Tag;
//            lineColor =
//                    (0 == myUID.compareTo(uID))
//                            ? android.graphics.Color.rgb(0x00, 0xFF, 0x00)
//                            : lineColors[Tag % lineColors.length];
//            myRef.child("RoomNumber").child("MyLocation").child(uID).addChildEventListener(userListener);
//            Log.i(LOG_TAG, String.format("user joined to your room: %s", uID));
//        }
//
//        public void fin() {
//            myRef.child("RoomNumber").child("MyLocation").child(uID).removeEventListener(userListener);
//            Log.i(LOG_TAG, String.format("user removed from your room: %s", uID));
//        }
//
//        public void drawTrace(long now, MapView mapView) {
//            Vector<MapPolyline> lines = new Vector<>();
//            for (long x : traceClassifier) {
//                lines.add(new MapPolyline());
//            }
//            int traceClassifierPicker = traceClassifier.length - 1;
//
//
//            synchronized (trace) {
//                for (Map.Entry<Date, MapPoint> entry : trace.descendingMap().entrySet()) {
//                    if (now - traceClassifier[traceClassifierPicker] > entry.getKey().getTime()) {
//                        lines.get(traceClassifierPicker--).addPoint(entry.getValue());
//                    }
//
//                    if (0 > traceClassifierPicker) {
//                        break;
//                    }
//
//                    lines.get(traceClassifierPicker).addPoint(entry.getValue());
//                }
//            }
//
//            MapPolyline prevline;
//            for (prevline = mapView.findPolylineByTag(lineTag); null != prevline; prevline = mapView.findPolylineByTag(lineTag)) {
//                mapView.removePolyline(prevline);
//            }
//
//            int lineAlpha = 0x20;
//
//            for (MapPolyline l : lines) {
//                l.setTag(lineTag);
//                l.setLineColor(android.graphics.Color.argb(
//                        lineAlpha,
//                        android.graphics.Color.red(lineColor),
//                        android.graphics.Color.green(lineColor),
//                        android.graphics.Color.blue(lineColor)));
//                lineAlpha += 0x30;
//                mapView.addPolyline(l);
//            }
//        }
//
//    }
//
//    Room myRoom;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        Log.i(LOG_TAG, "onCreate called.");
//        super.onCreate(savedInstanceState);
//
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//
//        myUID = getIntent().getStringExtra("uid");
//        setContentView(R.layout.map_view);
//
//        mMapView = findViewById(R.id.map_view);
//        //mMapView.setDaumMapApiKey(MapApiConst.DAUM_MAPS_ANDROID_APP_API_KEY);
//        mMapView.setCurrentLocationEventListener(this);
//
//        if (!checkLocationServicesStatus()) {
//
//            showDialogForLocationServiceSetting();
//        }
//        else {
//
//            checkRunTimePermission();
//        }
//        myRoom = new Room("RoomNumber");
//
//        t = new Timer();
//        t.schedule(task_drawingPaths, 500, pathRenewingPeriod);
//    }
//
//    @Override
//    protected void onDestroy() {
//        Log.i(LOG_TAG, "onDestroy called.");
//        super.onDestroy();
//
//        t.cancel();
//        myRoom.fin();
//        myRoom = null;
//
//
//        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        mMapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
//        mMapView.setShowCurrentLocationMarker(false);
//    }
//
//    @Override
//    public void onCurrentLocationUpdate(
//            MapView mapView,
//            MapPoint currentLocation,
//            float accuracyInMeters) {
//        long now_Device = SystemClock.elapsedRealtime();
//        long now_LocalTime = now_Device - epoch_Device + epoch_LocalTime;
//
//        MapPoint.GeoCoordinate mapPointGeo = currentLocation.getMapPointGeoCoord();
//        Log.i(
//                LOG_TAG,
//                String.format(
//                        "MapView onCurrentLocationUpdate (%f,%f) at %s, accuracy (%f)",
//                        mapPointGeo.latitude,
//                        mapPointGeo.longitude,
//                        new Date(now_LocalTime).toString(),
//                        accuracyInMeters));
//
//        MyLocation locData = new MyLocation(currentLocation.getMapPointGeoCoord().latitude
//                ,currentLocation.getMapPointGeoCoord().longitude);
//
//        //drawPath(myUID,now_LocalTime,mapView,currentLocation);
//
//        sendGPS(now_LocalTime,locData);
//    }
//
//    public void sendGPS(long now, MyLocation locData){
//        myRef.child("RoomNumber").child("MyLocation").child(myUID).child(Long.toString(now)).setValue(locData);
//
//        /*MapPoint.GeoCoordinate mapPointGeo = currentLocation.getMapPointGeoCoord();
//        String url = "http://IP:Port";
//
//        //JSON형식으로 데이터 통신을 진행
//        JSONObject position = new JSONObject();
//        try {
//            position.put("latitude", Double.toString(mapPointGeo.latitude));
//            position.put("longitude", Double.toString(mapPointGeo.longitude));
//            position.put("userid", "SomeUserID");
//            String jsonString = position.toString(); //완성된 json 포맷
//
//            final RequestQueue requestQueue = Volley.newRequestQueue(MapViewActivity.this);
//            final JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, position, new Response.Listener<JSONObject>() {
//
//                //Get Response
//                @Override
//                public void onResponse(JSONObject response) {
//                    try {
//                        JSONObject jsonObject = new JSONObject(response.toString());
//
//                        String latitude = jsonObject.getString("latitude");
//                        String longitude = jsonObject.getString("longitude");
//                        String userid = jsonObject.getString("userid");
//
//                        Toast.makeText(getApplicationContext(), "latitude : " + latitude + " longitude : " + longitude + " UserID : " + userid, Toast.LENGTH_LONG).show();
//
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }, new Response.ErrorListener() {   //when error
//                @Override
//                public void onErrorResponse(VolleyError error) {
//                    error.printStackTrace();
//                    Toast.makeText(MapViewActivity.this, error.toString(), Toast.LENGTH_SHORT).show();
//                }
//            });
//            jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
//            requestQueue.add(jsonObjectRequest);
//
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }*/
//    }
//
//    @Override
//    public void onCurrentLocationDeviceHeadingUpdate(MapView mapView, float v) {
//
//    }
//
//    @Override
//    public void onCurrentLocationUpdateFailed(MapView mapView) {
//
//    }
//
//    @Override
//    public void onCurrentLocationUpdateCancelled(MapView mapView) {
//
//    }
//
//    @Override
//    public void onReverseGeoCoderFoundAddress(MapReverseGeoCoder mapReverseGeoCoder, String s) {
//        mapReverseGeoCoder.toString();
//        onFinishReverseGeoCoding(s);
//    }
//
//    @Override
//    public void onReverseGeoCoderFailedToFindAddress(MapReverseGeoCoder mapReverseGeoCoder) {
//        onFinishReverseGeoCoding("Fail");
//    }
//
//    private void onFinishReverseGeoCoding(String result) {
////        Toast.makeText(LocationDemoActivity.this, "Reverse Geo-coding : " + result, Toast.LENGTH_SHORT).show();
//    }
//
//
//
//
//    /*
//     * ActivityCompat.requestPermissions를 사용한 퍼미션 요청의 결과를 리턴받는 메소드입니다.
//     */
//    @Override
//    public void onRequestPermissionsResult(int permsRequestCode,
//                                           @NonNull String[] permissions,
//                                           @NonNull int[] grandResults) {
//
//        if ( permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {
//
//            // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면
//
//            boolean check_result = true;
//
//
//            // 모든 퍼미션을 허용했는지 체크합니다.
//
//            for (int result : grandResults) {
//                if (result != PackageManager.PERMISSION_GRANTED) {
//                    check_result = false;
//                    break;
//                }
//            }
//
//
//            if ( check_result ) {
//                Log.d("@@@", "start");
//                //위치 값을 가져올 수 있음
//                mMapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeading);
//            }
//            else {
//                // 거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합니다.2 가지 경우가 있습니다.
//
//                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])) {
//
//                    Toast.makeText(MapViewActivity.this, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.", Toast.LENGTH_LONG).show();
//                    finish();
//
//
//                }else {
//
//                    Toast.makeText(MapViewActivity.this, "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ", Toast.LENGTH_LONG).show();
//
//                }
//            }
//
//        }
//    }
//
//
//
//
//    //여기부터는 GPS 활성화를 위한 메소드들
//    private void showDialogForLocationServiceSetting() {
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(MapViewActivity.this);
//        builder.setTitle("위치 서비스 비활성화");
//        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
//                + "위치 설정을 수정하실래요?");
//        builder.setCancelable(true);
//        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int id) {
//                Intent callGPSSettingIntent
//                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
//            }
//        });
//        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int id) {
//                dialog.cancel();
//            }
//        });
//        builder.create().show();
//    }
//
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        switch (requestCode) {
//
//            case GPS_ENABLE_REQUEST_CODE:
//
//                //사용자가 GPS 활성 시켰는지 검사
//                if (checkLocationServicesStatus()) {
//                    if (checkLocationServicesStatus()) {
//
//                        Log.d("@@@", "onActivityResult : GPS 활성화 되있음");
//                        checkRunTimePermission();
//                        return;
//                    }
//                }
//
//                break;
//        }
//    }
//
//    public boolean checkLocationServicesStatus() {
//        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
//
//        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
//                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
//    }
//}