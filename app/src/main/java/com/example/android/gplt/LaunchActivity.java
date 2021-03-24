package com.example.android.gplt;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class LaunchActivity extends AppCompatActivity {

    Button frontLaunchButton, backLaunchButton, externalCamLaunchButton;
    public static String intentKey = "com.example.android.gplt";
    int frontCameraFlag = 1, backCameraFlag = 0, externalCameraFlag = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        frontLaunchButton = findViewById(R.id.front_launch_button);
        backLaunchButton = findViewById(R.id.back_launch_button);
        externalCamLaunchButton = findViewById(R.id.external_camera_launch_button);

        frontLaunchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openActivity(frontCameraFlag);
            }
        });

        backLaunchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openActivity(backCameraFlag);
            }
        });

        externalCamLaunchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LaunchActivity.this, "External Camera not detected, Please connect one and try again", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void openActivity(int cameraFlag) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(intentKey, cameraFlag );
        startActivity(intent);
    }
}
