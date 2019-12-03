package com.example.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AfterLogin extends Activity {
    private Button btn_logout;
    private Button createroom;
    private Button joinroom;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.after_login);

        btn_logout = findViewById(R.id.btn_logout);
        btn_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AfterLogin.this, LoginActivity.class);
                intent.putExtra("logout", true);
                startActivity(intent);
                finish();
            }
        });

        final ImageButton createroom = (ImageButton) findViewById(R.id.createroom); //Map_open 으로 연결되게 해놓음

        final String userID = getIntent().getStringExtra("id");
        createroom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CustomDialog1 customDialog = new CustomDialog1(AfterLogin.this);
                customDialog.callFunction(userID);
            }
        });

        final ImageButton joinroom = (ImageButton) findViewById(R.id.joinroom);
        joinroom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                show();
            }
        });
    }

    void show(){
        final EditText edittext = new EditText(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("방 참가하기");
        builder.setMessage("참가하려는 방의 코드를 입력해주세요.");
        builder.setView(edittext);
        builder.setPositiveButton("입력", new DialogInterface.OnClickListener() {
            String input_key;
            public void onClick(DialogInterface dialog, int which) {
                input_key = edittext.getText().toString();
                room_check(input_key);
                //여기서 키 받고 맞으면 그 뒤 작업 추가
                //틀리면 안내메시지 출력
            }
        });
        builder.setNegativeButton("취소",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        builder.show();
    }

    void room_check(String key)
    {
        DatabaseReference mDatabase;
        mDatabase = FirebaseDatabase.getInstance().getReference();
        Query q = mDatabase.orderByKey().equalTo(key);
        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if(dataSnapshot.exists())
                {
                    Target target = dataSnapshot.child(key).child("Target").getValue(Target.class);
                    Intent intent = new Intent(AfterLogin.this, MapViewActivity.class);
                    intent.putExtra("uid", getIntent().getStringExtra("id"));
                    intent.putExtra("room",key);
                    startActivity(intent);
                    finish();

                }
                else
                {
                    Toast.makeText(getApplicationContext(),"잘못된 Room Code입니다.",Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError){}
        });

    }

}

