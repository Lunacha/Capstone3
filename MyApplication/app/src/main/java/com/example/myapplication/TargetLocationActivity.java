package com.example.myapplication;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class TargetLocationActivity extends AppCompatActivity implements
        OnMapReadyCallback {
    private static final String LOG_TAG = "TargetLocationActivity";

    private GoogleMap map;

    private FirebaseDatabase mdatabase = FirebaseDatabase.getInstance();
    private DatabaseReference myRef = mdatabase.getReference();

    TargetInfo targetInfo = null;

    Marker m = null;

    long targetTime;

    @Override
    protected void onCreate(
            Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onCreate called.");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.target_confirm);

        Calendar c = Calendar.getInstance();
        c.setTime(new Date(System.currentTimeMillis()));
        long time_lost_LONG = getIntent().getLongExtra("time_lost", -1);
        long epoch_Today
                = c.getTimeInMillis()
                - (c.get(Calendar.HOUR_OF_DAY) * 3600000)
                - (c.get(Calendar.MINUTE) * 60000)
                - (c.get(Calendar.SECOND) * 1000)
                - c.get(Calendar.MILLISECOND);

        targetTime = epoch_Today + time_lost_LONG;
        if (targetTime >= c.getTimeInMillis())
        {
            targetTime -= 24 * 3600 * 1000;
            Log.w(LOG_TAG, String.format("Your choice means yesterday, doesn't it? %d %d %d %d, %d:%d:%d.%4d", targetTime, time_lost_LONG, epoch_Today, c.getTimeInMillis(), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND), c.get(Calendar.MILLISECOND)));
        }

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
                        String RoomNumber = UUID.randomUUID().toString().replaceAll("-","");
                        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        ClipData clipData = ClipData.newPlainText("RoomNumber",RoomNumber);
                        clipboardManager.setPrimaryClip(clipData);
                        Toast.makeText(getApplicationContext(),"Room Code가 복사되었습니다.",Toast.LENGTH_SHORT).show();
                        synchronized (targetInfo) {
                            myRef.child(RoomNumber).child("Target").child("time").setValue(targetInfo.time);
                            myRef.child(RoomNumber).child("Target").child("height").setValue(targetInfo.height);
                            myRef.child(RoomNumber).child("Target").child("speed").setValue(targetInfo.speed);
                            myRef.child(RoomNumber).child("Target").child("latitude").setValue(targetInfo.location.latitude);
                            myRef.child(RoomNumber).child("Target").child("longitude").setValue(targetInfo.location.longitude);
                        }
                        Intent intent = new Intent(TargetLocationActivity.this, MapViewActivity.class);
                        intent.putExtra("uid", getIntent().getStringExtra("uid"));
                        intent.putExtra("room",RoomNumber);
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
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(37.2934204446d, 126.97467286d), 14));

        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                TargetInfo t = new TargetInfo();


                t.time = targetTime;
                t.height = getIntent().getIntExtra("height", -1) / 100d;
                t.speed = getIntent().getDoubleExtra("speed", -1);
                t.location = point;
                targetInfo = t;
                if (null == m)
                {
                    m = map.addMarker(new MarkerOptions().position(point));
                }
                m.setPosition(point);
            }
        });
    }

    @Override
    protected void onDestroy() {
        Log.i(LOG_TAG, "onDestroy called.");
        super.onDestroy();

        map = null;
    }
}
