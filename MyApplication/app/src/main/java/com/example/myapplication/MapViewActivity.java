package com.example.myapplication;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
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
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.SphericalUtil;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.util.GeometricShapeFactory;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;
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

    private Button btn_keycopy;
    static final long millis = 1;
    static final long second = 1000 * millis;
    static final long minute = 60 * second;
    static final long hour = 60 * minute;
    static final long[] traceClassifier =
            {
                    24 * hour, // 48 * hour
                    2 * minute, // 30 * minute,
                    minute, // 8 * minute,
                    30 * second, // 3 * minute,
                    15 * second, // minute
            };

    static final int[] lineColors = {
            android.graphics.Color.rgb(232, 11, 24),
            android.graphics.Color.rgb(29, 182, 190),
            android.graphics.Color.rgb(253, 181, 45),
            android.graphics.Color.rgb(14, 93, 223),
            android.graphics.Color.rgb(253, 127, 35),
            android.graphics.Color.rgb(102, 39, 218),
            android.graphics.Color.rgb(106, 182, 35),
            android.graphics.Color.rgb(226, 21, 141)
    };

    private class Room {
        private String rID;
        private int counterForLineTag = 0;
        private Target target;
        final private Vector<Member> members = new Vector<>();

        private Circle searchingArea = null;
        private Circle expectedArea = null;

        final private ChildEventListener roomListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                String newUser = dataSnapshot.getKey();
                if (null == newUser)
                {
                    return;
                }
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
        final private ValueEventListener targetListner = new ValueEventListener () {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long time_lost;
                double height;
                double speed;
                double lat_lost;
                double lon_lost;
                try {
                    time_lost = dataSnapshot.child("time").getValue(long.class);
                    height = dataSnapshot.child("height").getValue(double.class);
                    speed = dataSnapshot.child("speed").getValue(double.class);
                    lat_lost = dataSnapshot.child("latitude").getValue(double.class);
                    lon_lost = dataSnapshot.child("longitude").getValue(double.class);
                }
                catch (NullPointerException e) {
                    Log.i(LOG_TAG, "Target info NULL.");
                    return;
                }

                myRef.child(rID).child("Target").removeEventListener(targetListner);

                GeometryFactory fact = new GeometryFactory();

                List<LatLng> list = createCircleLatLngList(
                        new LatLng(lat_lost, lon_lost),
                        0.001);

                List<Coordinate> coords = convertLatLngList2CoordinateList(list);

                target = new Target(time_lost, height, speed, new LatLng(lat_lost, lon_lost));

                map.setOnMyLocationChangeListener(MapViewActivity.this::onLocationChanged); // WARNING: DEPRECATED

                myRef.child(rID).child("MyLocation").addChildEventListener(roomListener);

                t1.schedule(task_drawingPaths, 500, pathRenewingPeriod);
                t2.schedule(task_drawingArea, 1500, AreaRenewingPeriod);
                t3.schedule(task_drawingExpectedArea, 10000, expectedAreaRenewingPeriod);
                Log.i(LOG_TAG, "Target confirmed.");
            }

            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getApplicationContext(), "Database error occured!", Toast.LENGTH_LONG);
                Log.e(LOG_TAG, "Database error occured!");
                System.exit(0);
            }
        };

        private Room(String roomID) {
            rID = roomID;
            Log.i(LOG_TAG, String.format("you joined to room: %s", rID));
            myRef.child(rID).child("Target").addValueEventListener(targetListner);
        }

        private void fin() {
            myRef.child(rID).child("MyLocation").removeEventListener(roomListener);
            for (Member m : members) {
                m.fin();
            }
            members.clear();
            Log.i(LOG_TAG, String.format("you exited from room: %s", rID));
        }

        void addMember(String uID) {
            members.add(new Member(rID, uID, ++counterForLineTag));
        }

        @UiThread
        void drawSearchingArea(long now) {
            if (null == searchingArea)
            {
                searchingArea = map.addCircle(new CircleOptions()
                        .zIndex(-100000000000000000000000000000f)
                        .fillColor(Color.argb(120, 50, 50, 50))
                        .center(target.location_lost)
                        .radius(target.getSpeed() * (now - target.getLostTime()) / 1000d));
            }
            else
            {
                synchronized (searchingArea)
                {
                    searchingArea.setRadius(target.getSpeed() * (now - target.getLostTime()) / 1000d);
                }
            }
        }

        @UiThread
        void drawTraces(long now) {
            Log.i(LOG_TAG, "Drawing Traces.");
            try {
                for (Member m : members) {
                    m.drawTrace(now);
                    m.drawSearchedArea();
                }
            }
            catch (ConcurrentModificationException e)
            {
                //
            }
        }

        void updateSearchedAreas(long now) {
            Log.i(LOG_TAG, "Drawing Traces.");
            try {
                for (Member m : members) {
                    m.updateSearchedArea(now, target);
                }
            }
            catch (ConcurrentModificationException e)
            {
                //
            }
        }

        @UiThread
        void drawExpectedposition(long now){
            Log.i(LOG_TAG, "Drawing Expected positions.");
            double initialtime = (now - target.getLostTime()) / 1000d;

            String url = "https://golden-finder.firebaseapp.com/api";
            //String url = "http://172.30.1.44:5000";

            //JSON형식으로 데이터 통신을 진행
            JSONObject position = new JSONObject();
            try {
                position.put("losttime", Double.toString(initialtime));
                position.put("targetspeed", Double.toString(target.getSpeed()));
                position.put("latitude", Double.toString(target.location_lost.latitude));
                position.put("longitude", Double.toString(target.location_lost.longitude));
                String jsonString = position.toString(); //완성된 json 포맷

                final RequestQueue requestQueue = Volley.newRequestQueue(MapViewActivity.this);
                final JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, position, new Response.Listener<JSONObject>() {

                    //Get Response
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response.toString());
                            JSONArray jsonArray = new JSONObject(jsonObject.toString()).getJSONArray("node");

                            for(int i = 0; i < 3; i ++){
                                JSONObject jObject = jsonArray.getJSONObject(i);
                                LatLng expectedPosition = new LatLng(Double.parseDouble(jObject.optString("latitude")),
                                        Double.parseDouble(jObject.optString("longitude")));

                                expectedArea = map.addCircle(new CircleOptions()
                                        .zIndex(-99999999999999999999999999990f)
                                        .fillColor(Color.argb(60, 20, 100, 20))
                                        .center(expectedPosition)
                                        .radius(50));
                            }
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
                //
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class Member {
        private String uID;

        private int lineColor;
        private float zIndex;
        private Vector<Polyline> polylines;
        private Circle headMarker = null;
        private com.google.android.gms.maps.model.Polygon polygon = null;

        Geometry searchArea = null;

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

                if (!(uID.equals(myUID)))
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (null == headMarker) {
                                headMarker = map.addCircle(new CircleOptions()
                                        .center(new LatLng(0, 0))
                                        .radius(15)
                                        .fillColor(lineColor)
                                        .zIndex(zIndex)
                                        .visible(false));
                            }
                            headMarker.setCenter(new LatLng(lat_latest, lon_latest));
                        }
                    });
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
        private Member(String rID, String userID, int Tag) {
            uID = userID;
            lineColor =
                    (-1 == Tag)
                            ? Color.rgb(0x00, 0xFF, 0x00)
                            : lineColors[Tag % lineColors.length];
            int lineAlpha = 0x30;
            zIndex = (0 == myUID.compareTo(uID)) ?(1f) :(-Float.valueOf(uID));

            myRef.child(rID).child("MyLocation").child(uID).addChildEventListener(userListener);
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
                        .width(30)
                        .startCap(new RoundCap())
                        .endCap(new RoundCap())
                        .jointType(JointType.ROUND)));
                lineAlpha += 0x15;
            }
        }

        @UiThread
        private void fin() {
            myRef.child(myRoom.rID).child("MyLocation").child(uID).removeEventListener(userListener);

            for(Polyline l : polylines)
            {
                l.remove();
            }

            if (null != headMarker)
            {
                synchronized (headMarker) {
                    headMarker.remove();
                }
            }

            Log.i(LOG_TAG, String.format("user was removed from your room: %s", uID));
        }

        @UiThread
        private void drawTrace(long now) {
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
                if (0 <= traceClassifierPicker) {
                    polylines.get(traceClassifierPicker).setPoints(points);
                }
            }
        }

        public final String getUID() {
            return uID;
        }

        public final int getLineColor() {
            return lineColor;
        }

        private void updateSearchedArea(long now, Target target) {
            Log.w(LOG_TAG, "updateSearchedArea called.");

            GeometryFactory geometryFactory = new GeometryFactory();

            Geometry searchedArea = null;

            List<Coordinate> prevSearchAreaVertices = null;
            synchronized (trace) {
                for (Map.Entry<Date, LatLng> entry : trace.descendingMap().entrySet()) {
                    double radius
                            = target.getSearchRadius(now)
                            - ((now - entry.getKey().getTime()) / 1000d * target.getSpeed());

                    radius = (0 > radius) ? 0 : radius;

                    if (null == searchedArea)
                    {
                        if (0 >= radius)
                        {
                            return;
                        }

                        prevSearchAreaVertices
                                = convertLatLngList2CoordinateList(
                                createCircleLatLngList(entry.getValue(), radius));

                        searchedArea = geometryFactory.createPolygon(prevSearchAreaVertices.toArray(new Coordinate[0])).convexHull();
                    }
                    else
                    {
                        final List<Coordinate> currentSearchAreaVertices
                                = convertLatLngList2CoordinateList(
                                createCircleLatLngList(entry.getValue(), radius));

                        prevSearchAreaVertices.addAll(currentSearchAreaVertices);

                        searchedArea = searchedArea.union(geometryFactory.createLineString(
                                prevSearchAreaVertices.toArray(new Coordinate[0])).convexHull());

                        prevSearchAreaVertices = currentSearchAreaVertices;
                    }

                    if (0 == radius)
                    {
                        break;
                    }
                }
            }

            if (null == searchArea) {
                searchArea = searchedArea;
                return;
            }
            synchronized (searchArea)
            {
                searchArea = searchedArea;
            }
        }

        @UiThread
        private void drawSearchedArea() {
            Log.w(LOG_TAG, "drawSearchedArea called.");

            if (null == searchArea)
            {
                return;
            }
            synchronized (searchArea)
            {

                if(searchArea.isEmpty())
                {
                    return;
                }

                if (null != polygon)
                {
                    polygon.remove();
                }

                PolygonOptions options = new PolygonOptions()
                        .zIndex(zIndex)
                        .strokeColor(Color.TRANSPARENT)
                        .fillColor(Color.argb(80, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor)));

                ArrayList<Coordinate> arr;
                try {
                    org.locationtech.jts.geom.Polygon ppp = (org.locationtech.jts.geom.Polygon)searchArea;

                    arr = new ArrayList<>(
                            Arrays.asList(ppp.getExteriorRing().getCoordinates()));
                    for (Coordinate coordinate : arr) {
                        options.add(new LatLng(coordinate.getX(), coordinate.getY()));
                    }

                    int numHoles = ppp.getNumInteriorRing();
                    for (int h = 0; h < numHoles; h++)
                    {
                        arr = new ArrayList<>(
                                Arrays.asList(ppp.getInteriorRingN(h).getCoordinates()));
                        ArrayList<LatLng> latLngs = new ArrayList<>();
                        for (Coordinate coordinate : arr) {
                            latLngs.add(new LatLng(coordinate.getX(), coordinate.getY()));
                        }
                        options.addHole(latLngs);
                        Log.w(LOG_TAG, "draw a hole");
                    }
                }
                catch (NullPointerException e) {
                    return;
                }
                polygon = map.addPolygon(options);
                Log.w(LOG_TAG, "draw a polygon");
            }
        }
    }


    private class Target
    {
        private long time_lost;
        private double height;
        private double speed;
        private LatLng location_lost;

        Target(long time_lost_in, double height_in, double speed_in, LatLng location_lost_in) {
            time_lost = time_lost_in;
            height = height_in;
            speed = speed_in;
            location_lost = location_lost_in;


            map.moveCamera(CameraUpdateFactory.newLatLngZoom(location_lost, 15));
        }

        private double getSearchRadius(long now) {
            Calendar c = Calendar.getInstance();
            c.setTime(new Date(now));
            return (height * 0.22d + 11.0d) * (((c.get(Calendar.HOUR_OF_DAY) + 5) % 24 < 12 ) ? (3d / 2d) : 1);
        }

        public final double getLostTime() {
            return time_lost;
        }

        private double getSpeed() {
            return speed;
        }
    }



    private final long epoch_LocalTime = System.currentTimeMillis();
    private final long epoch_Device = SystemClock.elapsedRealtime();

    final long pathRenewingPeriod = 500;
    final long AreaRenewingPeriod = 1000;
    final long expectedAreaRenewingPeriod = 10000;

    private Timer t1;
    private Timer t2;
    private Timer t3;
    private TimerTask task_drawingPaths = new TimerTask() {
        @Override
        public void run() {
            long now_Device = SystemClock.elapsedRealtime();
            long now_LocalTime = now_Device - epoch_Device + epoch_LocalTime;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    myRoom.drawTraces(now_LocalTime);
                    myRoom.drawSearchingArea(now_LocalTime);
                }
            });
        }
    };
    private TimerTask task_drawingArea = new TimerTask() {
        @Override
        public void run() {
            long now_Device = SystemClock.elapsedRealtime();
            long now_LocalTime = now_Device - epoch_Device + epoch_LocalTime;
            myRoom.updateSearchedAreas(now_LocalTime);
        }
    };
    private TimerTask task_drawingExpectedArea = new TimerTask() {
        @Override
        public void run() {
            long now_Device = SystemClock.elapsedRealtime();
            long now_LocalTime = now_Device - epoch_Device + epoch_LocalTime;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    myRoom.drawExpectedposition(now_LocalTime);
                }
            });
        }
    };

    private String myUID = null;
    private String myRoomNum = null;

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
        myRoomNum = getIntent().getStringExtra("room");

        btn_keycopy = findViewById(R.id.btn_keycopy);
        btn_keycopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("RoomNumber",myRoomNum);
                clipboardManager.setPrimaryClip(clipData);
                Toast.makeText(getApplicationContext(),"Room Code가 복사되었습니다.",Toast.LENGTH_SHORT).show();
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(
            final GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setMyLocationButtonEnabled(false);
        checkRunTimePermission();

        myRoom = new Room(myRoomNum);

        t1 = new Timer();
        t2 = new Timer();
        t3 = new Timer();
    }

    @Override
    protected void onDestroy() {
        Log.i(LOG_TAG, "onDestroy called.");
        super.onDestroy();

        t1.cancel();
        t2.cancel();
        t3.cancel();

        myRoom.fin();


        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
        myRef.child(myRoom.rID).child("MyLocation").child(myUID).child(Long.toString(now)).setValue(locData);
    }

    public List<LatLng> createCircleLatLngList(LatLng center, double radius) {

        PolygonOptions opt = new PolygonOptions();

        if(0 == radius)
        {
            opt.add(center);
            return opt.getPoints();
        }

        for(double dir = 0; dir < 360d; dir += 360d / 32d)
        {
            opt.add(SphericalUtil.computeOffset(center, radius, dir));
        }

        opt.add(opt.getPoints().get(0));

        return opt.getPoints();
    }

    public List<Coordinate> convertLatLngList2CoordinateList(List<LatLng> latLngList) {
        List<Coordinate> list = new ArrayList<>();
        for (LatLng latLng : latLngList) {
            list.add(new Coordinate(latLng.latitude, latLng.longitude));
        }
        return list;
    }
}

// TODO: use FusedLocationProviderClient
// TODO: add button to switch between compass view <-> static follow view
// TODO: use hitmap library
