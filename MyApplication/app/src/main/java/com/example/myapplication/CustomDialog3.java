package com.example.myapplication;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
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
    public int number = 0;

    public void callFunction() {
        final Dialog dlg = new Dialog(context);
        dlg.setContentView(R.layout.custom_dialog3);
        dlg.show();

        final EditText edittext = dlg.findViewById(R.id.edittext);
        final ImageButton submitinfo = (ImageButton) dlg.findViewById(R.id.cd3button);
        String targetheight;
        int hour, min;
        TimePicker time = (TimePicker) dlg.findViewById(R.id.losttime);
        targetheight = edittext.getText().toString();
        hour = time.getHour();
        min = time.getMinute();

        seekbar = (SeekBar) dlg.findViewById(R.id.seekbar);
        outcome = (TextView) dlg.findViewById(R.id.seekbartext);

        seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekbar) {
                number = (seekbar.getProgress() + 60) / 100;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekbar) {
                number = (seekbar.getProgress() + 60) / 100;
            }

            @Override
            public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {
                number = (seekbar.getProgress() + 60) / 100;
                update();
            }
        });


        submitinfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), CustomDialog4.class);
                context.startActivity(intent);
                dlg.dismiss();
                //targetheight, speed, hour, min 서버전송
            }
        });
    }

    public void update(){
        String tmp = new StringBuilder().append(number) + " m/s";
        outcome.setText(tmp);
    }
}