package com.example.simplefiles;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class CustomizationMenu extends AppCompatActivity {
   // ImageView thumbnail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.customization_menu);
       // thumbnail = (ImageView)findViewById(R.id.thumbnail);


     /*   thumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(this, customization_menu.class);
                startActivity(intent);
            }
        });*/

    }


}