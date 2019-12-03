package com.example.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;



import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

public class AfterLogin extends Activity {
    private Button createroom;
    private Button joinroom;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.after_login);
        Intent intentgetter = getIntent();
        TextView info = (TextView) findViewById(R.id.id_info);
        String userID = intentgetter.getStringExtra("id");
        info.setText(userID);

        final ImageButton createroom = (ImageButton) findViewById(R.id.createroom); //Map_open 으로 연결되게만 우선 해둠
        createroom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AfterLogin.this, MapViewActivity.class);
                intent.putExtra("uid", userID);
                startActivity(intent);
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

}
