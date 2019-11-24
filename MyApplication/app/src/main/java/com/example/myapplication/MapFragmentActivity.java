package com.example.myapplication;


import android.Manifest;
import android.graphics.Color;
import android.location.Location;
import android.os.SystemClock;
import android.view.WindowManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.MultipartPathOverlay;
import com.naver.maps.map.util.FusedLocationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

public class MapFragmentActivity
        extends AppCompatActivity
        implements OnMapReadyCallback {

    private static final String LOG_TAG = "MapFragmentActivity";

    private final static long millis = 1;
    private final static long second = 1000 * millis;
    private final static long minute = 60 * second;
    private final static long hour = 60 * minute;
    private final static long traceClassifier[] =
            {
                    24 * hour,      // 48 * hour
                    2 * minute,     // 30 * minute,
                    1 * minute,     // 8 * minute,
                    30 * second,    // 3 * minute,
                    15 * second,    // minute
            };

    private final static int lineColors[] = {
            android.graphics.Color.rgb(0xFF, 0x00, 0x00),
            android.graphics.Color.rgb(0xFF, 0x8C, 0x00),
            android.graphics.Color.rgb(0xFF, 0xFF, 0x00),
            android.graphics.Color.rgb(0x00, 0x00, 0xFF),
            android.graphics.Color.rgb(0xFF, 0x00, 0x8C)
    };

    final long pathRenewingPeriod = 500;

    private static final int PERMISSIONS_REQUEST_CODE = 100;

    private final long epoch_LocalTime = System.currentTimeMillis();
    private final long epoch_Device = SystemClock.elapsedRealtime();

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

        final public NaverMap.OnLocationChangeListener onLocationChangeListener = new NaverMap.OnLocationChangeListener() {
            @Override
            public void onLocationChange(Location location) {
                long now_Device = SystemClock.elapsedRealtime();
                long now_LocalTime = now_Device - epoch_Device + epoch_LocalTime;

                Log.i(
                        LOG_TAG,
                        String.format(
                                "onLocationChange (%f,%f) at %s, accuracy (%f)",
                                location.getLatitude(),
                                location.getLongitude(),
                                new Date(now_LocalTime).toString(),
                                location.getAccuracy()));

                MyLocation locData = new MyLocation(location.getLatitude(), location.getLongitude());

                sendGPS(now_LocalTime,locData);
            }

            private void sendGPS(long now, MyLocation locData) {
                myRef.child(rID).child("Location").child(myUID).child(Long.toString(now)).setValue(locData);
            }
        };

        public Room(String roomID) {
            rID = roomID;
            myRef.child(rID).child("Location").addChildEventListener(roomListener);
            Log.i(LOG_TAG, String.format("you joined to room: %s", rID));
        }

        public void fin() {
            myRef.child(rID).child("Location").removeEventListener(roomListener);
            for (Member m : members) {
                m.fin();
            }
            members.clear();
            Log.i(LOG_TAG, String.format("you exited from room: %s", rID));
        }

        void addMember(String uID) {
            members.add(new Member(uID, ++counterForLineTag));
        }

        void drawTraces(long now) {
            try {
                for (Member m : members) {
                    m.drawTrace(now);
                }
            }
            catch (ConcurrentModificationException e)
            {

            }
        }
    }

    public class Member {
        final int traceClassifierL = traceClassifier.length - 1;

        private String uID;
        private MultipartPathOverlay pathOverlay;
        private int lineTag;    // for line tag feature, which does not support long type
        private int lineColor;
        private TreeMap<Date, LatLng> trace = new TreeMap<>();

        final private ChildEventListener userListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                long time_latest = Long.valueOf(dataSnapshot.getKey());
                double lat_latest = dataSnapshot.child("latitude").getValue(double.class);
                double lng_latest = dataSnapshot.child("longitude").getValue(double.class);
                Log.d(LOG_TAG, String.format("new data updated - uID: %s, time:%d, lat:%f, lon:%f", uID, time_latest, lat_latest, lng_latest));
                synchronized (trace) {
                    trace.put(new Date(time_latest), new LatLng(lat_latest, lng_latest));
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

        public Member(String userID, int Tag) {
            uID = userID;
            lineTag = Tag;
            lineColor =
                    (0 == myUID.compareTo(uID))
                            ? android.graphics.Color.rgb(0x00, 0xFF, 0x00)
                            : lineColors[Tag % lineColors.length];
            myRef.child("RoomNumber").child("Location").child(uID).addChildEventListener(userListener);
            Log.i(LOG_TAG, String.format("user joined to your room: %s", uID));

            pathOverlay = new MultipartPathOverlay();
            int lineAlpha = 0x20;
            List<MultipartPathOverlay.ColorPart> colorParts = new ArrayList<>();
            for(int i = 0; i < traceClassifierL; i++) {
                colorParts.add(
                        new MultipartPathOverlay.ColorPart(
                                Color.argb(lineAlpha, Color.red(lineColor), Color.red(lineColor), Color.red(lineColor)),
                                Color.TRANSPARENT,
                                Color.TRANSPARENT,
                                Color.TRANSPARENT));
                lineAlpha += 0x30;
            }
            pathOverlay.setColorParts(colorParts);
        }

        public void fin() {
            myRef.child("RoomNumber").child("Location").child(uID).removeEventListener(userListener);
            Log.i(LOG_TAG, String.format("user removed from your room: %s", uID));
        }

        public void drawTrace(long now) {
            List<List<LatLng>> coordParts = new ArrayList<>();


            for (long x : traceClassifier) {
                coordParts.add(new ArrayList<>());
            }

            int traceClassifierPicker = traceClassifier.length - 1;
            synchronized (trace) {
                for (Map.Entry<Date, LatLng> entry : trace.descendingMap().entrySet()) {
                    if (now - traceClassifier[traceClassifierPicker] > entry.getKey().getTime()) {
                        coordParts.get(traceClassifierPicker--).add(entry.getValue());
                    }
                    if (0 > traceClassifierPicker) {
                        break;
                    }
                    coordParts.get(traceClassifierPicker).add(entry.getValue());
                }

            }
            try {
                pathOverlay.setCoordParts(coordParts);
                pathOverlay.setMap(map);
            }
            catch (IllegalArgumentException e) {

            }
        }
    }

    NaverMap map;
    MapFragment mapFragment;

    private String myUID = null;

    private Room myRoom;

    private FusedLocationSource locationSource;

    private FirebaseDatabase mdatabase = FirebaseDatabase.getInstance();
    private DatabaseReference myRef = mdatabase.getReference();

    private Timer t;
    private TimerTask task_drawingPaths = new TimerTask() {
        @Override
        public void run() {
            long now_Device = SystemClock.elapsedRealtime();
            long now_LocalTime = now_Device - epoch_Device + epoch_LocalTime;
            myRoom.drawTraces(now_LocalTime);
        }
    };


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

        FragmentManager fm = getSupportFragmentManager();
        mapFragment = (MapFragment)fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }

        mapFragment.getMapAsync(this);

        locationSource = new FusedLocationSource(this, PERMISSIONS_REQUEST_CODE);
    }

    @Override
    protected void onDestroy() {
        Log.i(LOG_TAG, "onDestroy called.");
        super.onDestroy();

        t.cancel();
        myRoom.fin();
        myRoom = null;

        locationSource = null;

        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onRequestPermissionsResult(
            int permsRequestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(permsRequestCode, permissions, grantResults)) {
            return;
        }

        super.onRequestPermissionsResult(permsRequestCode, permissions, grantResults);
    }

    @Override
    public void onMapReady(
            @NonNull NaverMap naverMap) {
        map = naverMap;

        map.getUiSettings().setScrollGesturesEnabled(false);
        map.getUiSettings().setIndoorLevelPickerEnabled(true);

        map.setLocationSource(locationSource);
        map.setLocationTrackingMode(LocationTrackingMode.Face);

        joinRoom("RoomNumber");
        // TODO: 방을 생성하는 경우 실종 위치 지정을 위해 Map이 필요한데, 이 때는 joinRoom이 호출되어서는 안됨
    }

    private void joinRoom(
            String roomID) {
        if(null != myRoom) {
            myRoom.fin();
        }
        myRoom = new Room(roomID);

        map.addOnLocationChangeListener(myRoom.onLocationChangeListener);

        t = new Timer();
        t.schedule(task_drawingPaths, 500, pathRenewingPeriod);
    }
}