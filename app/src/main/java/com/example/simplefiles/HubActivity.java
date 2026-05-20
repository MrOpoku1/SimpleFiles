package com.example.simplefiles;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class HubActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_hub);

        View cardBrowse    = findViewById(R.id.cardBrowse);
        View cardCustomize = findViewById(R.id.thumbnail);
        FloatingActionButton fab = findViewById(R.id.floatingActionButton2);

        cardBrowse.setOnClickListener(v ->
                startActivity(new Intent(this, FileBrowserActivity.class)));

        cardCustomize.setOnClickListener(v ->
                startActivity(new Intent(this, CustomizeNavActivity.class)));

        fab.setOnClickListener(v ->
                startActivity(new Intent(this, FileBrowserActivity.class)));
    }
}
