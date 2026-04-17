package com.example.simplefiles;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class HubActivity extends AppCompatActivity {
    ImageView thumbnail;
    FloatingActionButton load;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_hub);

        thumbnail = (ImageView)findViewById(R.id.thumbnail);
        load = (FloatingActionButton) findViewById(R.id.floatingActionButton2);

        thumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(HubActivity.this, CustomizeNavActivity.class);
                startActivity(intent);
            }
        });


        load.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(HubActivity.this, ViewTextFile.class);
                intent.putExtra("FILE_PATH", "app/sampledata/textdata.txt");
                startActivity(intent);
            }
        });
    }


}