package com.example.simplefiles;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.FileInputStream;

public class ViewTextFile extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.file_view_text);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());


        //Find Id's
        TextView fileContent = findViewById(R.id.fileContentText);

        //File Variables
        //String filepath = getIntent().getStringExtra("FILE_PATH");
        String filepath = "app/sampledata/textdata.txt";
        assert filepath != null;
        File file = new File(filepath);

        //Load file
        executor.execute(() -> {
            try (FileInputStream fis = new FileInputStream(file)) {
                // Local Variables
                int content;
                String finalResult;

                // Parse data Byte by Byte into a string and append a new character everytime byte is parsed.
                StringBuilder sb = new StringBuilder();
                while ((content = fis.read()) != -1) {
                    sb.append((char) content);
                }

                //Build sb result into string and set to the TextView: fileContent
                finalResult = sb.toString();
                handler.post(() -> {
                    fileContent.setText(finalResult);
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
