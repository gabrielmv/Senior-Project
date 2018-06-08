package com.gabriel.tcc;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;

public class SettingsActivity extends AppCompatActivity {

    EditText et_low, et_mid, et_high, et_step_max, et_phoneNumber;
    Button bt_update;
    Preferences preferences = Preferences.getInstance();
    String low = preferences.getLow();
    String mid = preferences.getMid();
    String high = preferences.getHigh();
    String stepMax = preferences.getstepMax();
    String phoneNumber = preferences.getphoneNumber();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        et_low = (EditText)findViewById(R.id.et_low);
        et_mid   = (EditText)findViewById(R.id.et_mid);
        et_high   = (EditText)findViewById(R.id.et_high);
        et_step_max   = (EditText)findViewById(R.id.et_maxSteps);
        et_phoneNumber   = (EditText)findViewById(R.id.et_phone);
        bt_update = (Button)findViewById(R.id.bt_update);

        et_low.setText(low);
        et_mid.setText(mid);
        et_high.setText(high);
        et_step_max.setText(stepMax);
        et_phoneNumber.setText(phoneNumber);

        bt_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                low = et_low.getText().toString();
                mid = et_mid.getText().toString();
                high = et_high.getText().toString();
                stepMax = et_step_max.getText().toString();
                phoneNumber = et_phoneNumber.getText().toString();

                preferences.setLow(low);
                preferences.setMid(mid);
                preferences.setHigh(high);
                preferences.setstepMax(stepMax);
                preferences.setphoneNumber(phoneNumber);
                Toast.makeText(getApplicationContext(),"Updated", Toast.LENGTH_SHORT).show();

                //Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                //intent.putExtra("low", low);
                //intent.putExtra("mid", mid);
                //intent.putExtra("high",high);
                //intent.putExtra("stepMax",stepMax);
                //intent.putExtra("phoneNumber",phoneNumber);
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
