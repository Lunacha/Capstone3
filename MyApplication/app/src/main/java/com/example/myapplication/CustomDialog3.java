package com.example.myapplication;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.TimePicker;

import org.w3c.dom.Text;


public class CustomDialog3{

    private Context context;

    public CustomDialog3(Context context) {
        this.context = context;
    }

    SeekBar seekbar;
    TextView outcome;
    public double number = 0;

    public void callFunction() {

        final Dialog dlg = new Dialog(context);
        dlg.setContentView(R.layout.custom_dialog3);

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        ImageView a  = dlg.findViewById(R.id.title);
        a.getLayoutParams().width = width;
        a.requestLayout();

        dlg.show();

        final ImageButton submitinfo = (ImageButton) dlg.findViewById(R.id.cd3button);

        seekbar = (SeekBar) dlg.findViewById(R.id.seekbar);
        outcome = (TextView) dlg.findViewById(R.id.seekbartext);

        seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekbar) {
                number = (float)(seekbar.getProgress() + 60) / (float)100;
                number = Math.round(number*100)/100.0;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekbar) {
                number = (float)(seekbar.getProgress() + 60) / (float)100;
                number = Math.round(number*100)/100.0;
            }

            @Override
            public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {
                number = (float)(seekbar.getProgress() + 60) / (float)100;
                number = Math.round(number*100)/100.0;
                update();
            }
        });


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

                number = Math.round(number*100)/100.0;
                hour = time.getHour();
                min = time.getMinute();
                pasttime = hour * 3600000 + min * 60000;

                Intent intent = new Intent(view.getContext(), TargetLocationActivity.class);
                intent.putExtra("height", targetheight);
                intent.putExtra("speed", number);
                intent.putExtra("time_lost", pasttime);
                context.startActivity(intent);
                dlg.dismiss();
            }
        });
    }

    public void update(){
        String tmp = new StringBuilder().append(number) + " m/s";
        outcome.setText(tmp);
    }
}