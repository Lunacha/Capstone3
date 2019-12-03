package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class TargetLocationActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final String[] REQUIRED_PERMISSIONS  = { Manifest.permission.ACCESS_FINE_LOCATION };
    private static final String LOG_TAG = "TargetLocationActivity";

    private GoogleMap map;

    private FirebaseDatabase mdatabase = FirebaseDatabase.getInstance();
    private DatabaseReference myRef = mdatabase.getReference();

    class TargetInfo
    {
        public long time;
        public double height;
        public double speed;
        public LatLng location;
    }

    TargetInfo targetInfo = null;

    @Override
    protected void onCreate(
            Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onCreate called.");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.target_confirm);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        final ImageButton confirmButton = findViewById(R.id.confirm);

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.confirm:
                        if (null == targetInfo)
                        {
                            return;
                        }
                        synchronized (targetInfo) {
                            myRef.child("RoomNumber").child("Target").child("time").setValue(targetInfo.time);
                            myRef.child("RoomNumber").child("Target").child("height").setValue(targetInfo.height);
                            myRef.child("RoomNumber").child("Target").child("speed").setValue(targetInfo.speed);
                            myRef.child("RoomNumber").child("Target").child("latitude").setValue(targetInfo.location.latitude);
                            myRef.child("RoomNumber").child("Target").child("longitude").setValue(targetInfo.location.longitude);
                        }
                        Intent intent = new Intent(TargetLocationActivity.this, MapViewActivity.class);
                        intent.putExtra("uid", getIntent().getStringExtra("uid"));
                        startActivity(intent);
                        finish();
                }
            }
        });
    }

    @Override
    public void onMapReady(
            final GoogleMap googleMap) {
        map = googleMap;
        map.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(37.3d, 127d)));
        checkRunTimePermission();

        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                TargetInfo t = new TargetInfo();

                long time_lost_LONG = getIntent().getLongExtra("time_lost", -1);

                Calendar c = Calendar.getInstance();
                c.setTime(new Date(System.currentTimeMillis()));
                long epoch_Today
                        = c.getTimeInMillis()
                        - ((((c.get(Calendar.HOUR_OF_DAY) * 60) + c.get(Calendar.MINUTE)) * 60) + c.get(Calendar.SECOND) * 1000 + c.get(Calendar.MILLISECOND));

                t.time = epoch_Today + time_lost_LONG;
                if (epoch_Today + time_lost_LONG >= c.getTimeInMillis())
                {
                    t.time -= 24 * 3600 * 1000;
                }
                t.height = getIntent().getIntExtra("height", -1) / 100d;
                t.speed = getIntent().getDoubleExtra("speed", -1);
                t.location = point;
                targetInfo = t;
            }
        });
    }

    @Override
    protected void onDestroy() {
        Log.i(LOG_TAG, "onDestroy called.");
        super.onDestroy();

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
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
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
}
