package com.example.myapplication;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TimePicker;


public class CustomDialog2 {

    private Context context;

    public CustomDialog2(Context context) {
        this.context = context;
    }

    public void callFunction() {
        final Dialog dlg = new Dialog(context);
        dlg.setContentView(R.layout.custom_dialog2);
        dlg.show();


        final ImageButton submitinfo = (ImageButton)dlg.findViewById(R.id.btn_submit);

        submitinfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int targetheight;
                int hour, min;
                double speed;

                final EditText edittext = dlg.findViewById(R.id.edittext);
                targetheight = Integer.parseInt(edittext.getText().toString());
                TimePicker time = (TimePicker)dlg.findViewById(R.id.losttime);

                speed = (float)targetheight - 0.16;
                hour = time.getHour();
                min = time.getMinute();

                Intent intent = new Intent(view.getContext(), CustomDialog4.class);
                context.startActivity(intent);
                dlg.dismiss();
                //targetheight, speed, hour, min 서버전송
            }
        });
    }
}