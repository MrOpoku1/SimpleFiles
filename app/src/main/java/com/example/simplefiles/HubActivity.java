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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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

        //load test file

        File file = new File(getFilesDir(), "testdata.txt");
        try (FileOutputStream fos = new FileOutputStream(file, false)) { // false = overwrite
            fos.write("Hi aseda you suck".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

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
                File file = new File(getFilesDir(), "testdata.txt");
                intent.putExtra("FILE_PATH", file.getAbsolutePath());
                startActivity(intent);
            }
        });
    }


}