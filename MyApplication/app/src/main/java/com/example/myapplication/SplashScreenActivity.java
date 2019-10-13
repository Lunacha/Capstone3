package com.example.myapplication;

import android.Manifest;

import android.app.AlertDialog;

import android.content.DialogInterface;

import android.content.Intent;

import android.content.pm.PackageManager;

import android.location.LocationManager;

import android.os.Bundle;

import androidx.annotation.NonNull;

import androidx.core.app.ActivityCompat;

import androidx.core.content.ContextCompat;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;

import android.widget.Toast;



import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapReverseGeoCoder;
import net.daum.mf.map.api.MapView;

public class SplashScreenActivity extends AppCompatActivity  {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);

            Thread.sleep(3000); // ONLY FOR TEST PURPOSE! REMOVE BEFORE RELEASE
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
