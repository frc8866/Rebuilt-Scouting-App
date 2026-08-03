package com.example.scoutingapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.scoutingapp.ui.home.HomeActivity;

/**
 * Java equivalent of the original single-Activity setup (MainActivity hosting a
 * Compose NavHost that started at AppRoutes.HOME). Since navigation is now handled
 * via separate Activities, MainActivity's only job is to immediately launch HomeActivity.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}
