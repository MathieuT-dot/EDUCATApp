package com.educat.android.educatapp.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.educat.android.educatapp.fragments.SettingsFragment;

/**
 * SettingsActivity
 *
 * Activity to display the settings of the app
 */

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}
