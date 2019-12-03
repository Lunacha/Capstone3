package com.example.myapplication;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TimePicker;
import android.util.DisplayMetrics;
import java.util.Date;


public class CustomDialog2 {

    private Context context;

    public CustomDialog2(Context context) {
        this.context = context;
    }

    public void callFunction(String userID) {

        final Dialog dlg = new Dialog(context);
        dlg.setContentView(R.layout.custom_dialog2);

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        ImageView a  = dlg.findViewById(R.id.title);
        a.getLayoutParams().width = width;
        a.requestLayout();

        dlg.show();

        final ImageButton submitinfo = (ImageButton)dlg.findViewById(R.id.btn_submit);

        submitinfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int targetheight;
                int hour, min;
                double speed;
                long pasttime;

                final EditText edittext = dlg.findViewById(R.id.edittext);
                targetheight = Integer.parseInt(edittext.getText().toString());
                TimePicker time = (TimePicker)dlg.findViewById(R.id.losttime);

                speed = (float)targetheight/100.0 - 0.16;
                hour = time.getHour();
                min = time.getMinute();
                pasttime = hour * 3600000 + min * 60000;


                Intent intent = new Intent(view.getContext(), TargetLocationActivity.class);
                intent.putExtra("uid", userID);
                intent.putExtra("height", targetheight);
                intent.putExtra("speed", speed);
                intent.putExtra("time_lost", pasttime);
                context.startActivity(intent);
                dlg.dismiss();
            }
        });
    }
}
