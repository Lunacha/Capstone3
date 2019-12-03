package com.example.myapplication;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.ImageButton;

public class CustomDialog1 {

    private Context context;

    public CustomDialog1(Context context) {
        this.context = context;
    }

    public void callFunction() {

        final Dialog dlg = new Dialog(context);

        dlg.setContentView(R.layout.custom_dialog1);

        dlg.show();

        final ImageButton childinfo = (ImageButton)dlg.findViewById(R.id.childinfo);
        final ImageButton elderinfo = (ImageButton)dlg.findViewById(R.id.elderinfo);

        childinfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CustomDialog2 tmp = new CustomDialog2(view.getContext());
                tmp.callFunction();
                dlg.dismiss();
            }
        });

        elderinfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CustomDialog3 tmp = new CustomDialog3(view.getContext());
                tmp.callFunction();
                dlg.dismiss();
            }
        });
    }
}