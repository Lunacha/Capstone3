package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.LogoutResponseCallback;
import com.kakao.usermgmt.callback.MeV2ResponseCallback;
import com.kakao.usermgmt.response.MeV2Response;

import java.util.ArrayList;
import java.util.List;

public class AfterLogin extends Activity {
    private Button btn_logout;
    private Button btn_openmap;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.after_login);
        List<String> keys = new ArrayList<>();
        keys.add("properties.nickname");
        keys.add("properties.profile_image");
        //keys.add("kakao_account.email");


        UserManagement.getInstance().me(keys,new MeV2ResponseCallback(){
            TextView id_info = (TextView)findViewById(R.id.id_info);
           @Override
            public void onFailure(ErrorResult errorResult){
               id_info.setText("errorResult");
           }

           @Override
           public void onSessionClosed(ErrorResult errorResult){
               id_info.setText("errorResult");
           }

           public void onSuccess(MeV2Response response){
               id_info.setText("id: " + response.getId());
           }
        });


        btn_logout = (Button) findViewById(R.id.btn_logout);
        btn_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UserManagement.getInstance().requestLogout(new LogoutResponseCallback() {
                    @Override
                    public void onCompleteLogout() {
                        Intent intent = new Intent(AfterLogin.this, LoginActivity.class);
                        startActivity(intent);
                    }
                });
            }
        });

        btn_openmap = (Button) findViewById(R.id.btn_open_map);
        btn_openmap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UserManagement.getInstance().requestLogout(new LogoutResponseCallback() {
                    @Override
                    public void onCompleteLogout() {
                        Intent intent = new Intent(AfterLogin.this, MapViewActivity.class);
                        startActivity(intent);
                    }
                });
            }
        });
    }
}

