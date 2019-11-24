package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

public class AfterLogin extends Activity {
    private Button btn_logout;
    private Button btn_openmap;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.after_login);
        Intent intentgetter = getIntent();
        TextView info = (TextView)findViewById(R.id.id_info);
        String userID = intentgetter.getStringExtra("id");
        String userIDToken = intentgetter.getStringExtra("id_token");
        info.setText(userID + ", token: " + userIDToken);

        btn_logout = findViewById(R.id.btn_logout);
        btn_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AfterLogin.this, LoginActivity.class);
                intent.putExtra("logout",true);
                startActivity(intent);
                finish();
            }
        });

        btn_openmap = findViewById(R.id.btn_open_map);
        btn_openmap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AfterLogin.this, MapFragmentActivity.class);
                intent.putExtra("uid",userID);
                startActivity(intent);
            }
        });
    }


}

