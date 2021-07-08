package com.educat.android.educatapp.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.educat.android.educatapp.R;
import com.educat.android.educatapp.helperClasses.MyLog;

/**
 * AlarmDistanceSimulationActivity
 *
 * Activity to simulate the alarm distance of the OAS.
 */
public class AlarmDistanceSimulationActivity extends AppCompatActivity {

    private static final String TAG = "AlarmDistanceSimulation";

    private TextView alarmDistanceTextView;
    private View colorView;

    private TextView distanceTextView;
    private SeekBar distanceSeekBar;
    private float distanceValue = 0;
    private float distanceMin = -15;
    private float distanceMax = 255;
    private float distanceUnit = 51f;

    private TextView speedTextView;
    private SeekBar speedSeekBar;
    private float speedValue = 0;
    private float speedMin = 0;
    private float speedMax = 12;

    private TextView slopeStartTextView;
    private SeekBar slopeStartSeekBar;
    private float slopeStartValue = 5;
    private float slopeStartMin = 0;
    private float slopeStartMax = 30;

    private TextView slopePercentageTextView;
    private SeekBar slopePercentageSeekBar;
    private float slopePercentageValue = 10;
    private float slopePercentageMin = 0;
    private float slopePercentageMax = 50;

    private TextView slopeEndTextView;
    private SeekBar slopeEndSeekBar;
    private float slopeEndValue = 80;
    private float slopeEndMin = 40;
    private float slopeEndMax = 250;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boundary_simulation);

        alarmDistanceTextView = findViewById(R.id.alarmDistanceTextView);

        colorView = findViewById(R.id.colorView);

        distanceTextView = findViewById(R.id.distanceTextView);
        distanceTextView.setText("Distance: " + distanceValue + " cm");

        distanceSeekBar = findViewById(R.id.distanceSeekBar);
        distanceSeekBar.setMax((int) (distanceMax - distanceMin));
        distanceSeekBar.setProgress((int) (distanceValue - distanceMin));
        distanceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColor();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                updateColor();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                updateColor();
            }
        });

        speedTextView = findViewById(R.id.speedTextView);
        speedTextView.setText("Speed: " + speedValue + " km/h");

        speedSeekBar = findViewById(R.id.speedSeekBar);
        speedSeekBar.setMax((int) (speedMax - speedMin));
        speedSeekBar.setProgress((int) (speedValue - speedMin));
        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColor();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                updateColor();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                updateColor();
            }
        });

        slopeStartTextView = findViewById(R.id.slopeStartTextView);
        slopeStartTextView.setText("Slope start: " + slopeStartValue + " cm");

        slopeStartSeekBar = findViewById(R.id.slopeStartSeekBar);
        slopeStartSeekBar.setMax((int) (slopeStartMax - slopeStartMin));
        slopeStartSeekBar.setProgress((int) (slopeStartValue - slopeStartMin));
        slopeStartSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColor();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                updateColor();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                updateColor();
            }
        });

        slopePercentageTextView = findViewById(R.id.slopePercentageTextView);
        slopePercentageTextView.setText("Slope percentage: " + slopePercentageValue + " %");

        slopePercentageSeekBar = findViewById(R.id.slopePercentageSeekBar);
        slopePercentageSeekBar.setMax((int) (slopePercentageMax - slopePercentageMin));
        slopePercentageSeekBar.setProgress((int) (slopePercentageValue - slopePercentageMin));
        slopePercentageSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColor();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                updateColor();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                updateColor();
            }
        });

        slopeEndTextView = findViewById(R.id.slopeEndTextView);
        slopeEndTextView.setText("Slope end: " + slopeEndValue + " cm");

        slopeEndSeekBar = findViewById(R.id.slopeEndSeekBar);
        slopeEndSeekBar.setMax((int) (slopeEndMax - slopeEndMin));
        slopeEndSeekBar.setProgress((int) (slopeEndValue - slopeEndMin));
        slopeEndSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColor();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                updateColor();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                updateColor();
            }
        });

        updateColor();
    }

    private void updateColor() {

        distanceValue = distanceSeekBar.getProgress() + distanceMin;
        distanceTextView.setText(getString(R.string.distance_colon, (int) distanceValue));

        speedValue = speedSeekBar.getProgress() + speedMin;
        speedTextView.setText(getString(R.string.speed_colon, (int) speedValue));

        slopeStartValue = slopeStartSeekBar.getProgress() + slopeStartMin;
        slopeStartTextView.setText(getString(R.string.slope_start_colon,  (int) slopeStartValue));

        slopePercentageValue = slopePercentageSeekBar.getProgress() + slopePercentageMin;
        slopePercentageTextView.setText(getString(R.string.slope_percentage_colon, (int) slopePercentageValue));

        slopeEndValue = slopeEndSeekBar.getProgress() + slopeEndMin;
        slopeEndTextView.setText(getString(R.string.slope_end_colon, (int) slopeEndValue));

        float alarmDistance;

        if (speedValue / speedMax * 100f < slopePercentageValue) {
            alarmDistance = slopeStartValue;
        }
        else {
            alarmDistance = (slopeEndValue - slopeStartValue) / (100f - slopePercentageValue) * (speedValue / speedMax * 100f - slopePercentageValue) + slopeStartValue;
        }

        if (distanceValue < alarmDistance) {
            alarmDistanceTextView.setTextColor(Color.RED);
        }
        else {
            alarmDistanceTextView.setTextColor(getResources().getColor(R.color.default_text));
        }

        alarmDistanceTextView.setText(getString(R.string.alarm_distance_colon, (int) alarmDistance));

        if (distanceValue - alarmDistance < distanceUnit){
            colorView.setBackgroundColor((int) evaluateArgb((distanceValue - alarmDistance - distanceUnit * 0f) / distanceUnit, getResources().getColor(R.color.radar_red), getResources().getColor(R.color.radar_orange)));
        }
        else if (distanceValue - alarmDistance < distanceUnit * 2f){
            colorView.setBackgroundColor((int) evaluateArgb((distanceValue - alarmDistance - distanceUnit * 1f) / distanceUnit, getResources().getColor(R.color.radar_orange), getResources().getColor(R.color.radar_yellow)));
        }
        else if (distanceValue - alarmDistance < distanceUnit * 3f){
            colorView.setBackgroundColor((int) evaluateArgb((distanceValue - alarmDistance - distanceUnit * 2f) / distanceUnit, getResources().getColor(R.color.radar_yellow), getResources().getColor(R.color.radar_light_green)));
        }
        else if (distanceValue - alarmDistance < distanceUnit * 4f){
            colorView.setBackgroundColor((int) evaluateArgb((distanceValue - alarmDistance - distanceUnit * 3f) / distanceUnit, getResources().getColor(R.color.radar_light_green), getResources().getColor(R.color.radar_dark_green)));
        }
        else if (distanceValue - alarmDistance < distanceUnit * 5f){
            colorView.setBackgroundColor((int) evaluateArgb((distanceValue - alarmDistance - distanceUnit * 4f) / distanceUnit, getResources().getColor(R.color.radar_dark_green), getResources().getColor(R.color.radar_gray)));
        }
        else {
            colorView.setBackgroundColor(getResources().getColor(R.color.transparant_gray));
        }

    }

    private Object evaluateArgb(float fraction, Object startValue, Object endValue){
        int startInt = (Integer) startValue;
        float startA = ((startInt >> 24) & 0xff) / 255.0f;
        float startR = ((startInt >> 16) & 0xff) / 255.0f;
        float startG = ((startInt >> 8) & 0xff) / 255.0f;
        float startB = ((startInt) & 0xff) / 255.0f;

        int endInt = (Integer) endValue;
        float endA = ((endInt >> 24) & 0xff) / 255.0f;
        float endR = ((endInt >> 16) & 0xff) / 255.0f;
        float endG = ((endInt >> 8) & 0xff) / 255.0f;
        float endB = ((endInt) & 0xff) / 255.0f;

        // convert from sRGB to linear
        startR = (float) Math.pow(startR, 2.2);
        startG = (float) Math.pow(startG, 2.2);
        startB = (float) Math.pow(startB, 2.2);

        endR = (float) Math.pow(endR, 2.2);
        endG = (float) Math.pow(endG, 2.2);
        endB = (float) Math.pow(endB, 2.2);

        // compute the interpolated color in linear space
        float a = startA + fraction * (endA - startA);
        float r = startR + fraction * (endR - startR);
        float g = startG + fraction * (endG - startG);
        float b = startB + fraction * (endB - startB);

        MyLog.d("evaluateArgb", "compute the interpolated color in linear space: a = " + a + ", r = " + r + ", g = " + g + ", b = " + b);

        // convert back to sRGB in the [0..255] range
        a = a * 255.0f;
        r = (float) Math.pow(r, 1.0 / 2.2) * 255.0f;
        g = (float) Math.pow(g, 1.0 / 2.2) * 255.0f;
        b = (float) Math.pow(b, 1.0 / 2.2) * 255.0f;

        MyLog.d("evaluateArgb", "convert back to sRGB in the [0..255] range: a = " + a + ", r = " + r + ", g = " + g + ", b = " + b);

        Object o = Math.round(a) << 24 | Math.round(r) << 16 | Math.round(g) << 8 | Math.round(b);

        MyLog.d("evaluateArgb", "integer representation of the color: " + (int) o);

        return o;
    }
}