package com.example.wemosd1wifiapp;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class WaterPlants extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_water_plants);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);



            String D1IP = getIntent().getStringExtra("D1IP");
            TextView connIP = findViewById(R.id.connTV2);
            connIP.setText(D1IP);


            Button water = (Button) findViewById(R.id.waterBtn);
            water.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    waterPlants(D1IP);
                }
            });
            return insets;
        });
    }

    private void waterPlants(final String D1IP) {


        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                final String ip = D1IP;
                Log.d("SEND TO D1", "Creating URL connection");
                String url = "http://" + ip + "/water"; // IP of the D1 Mini AP
                URL urlObj = new URL(url);
                connection = (HttpURLConnection) urlObj.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");



                Log.d("SEND TO D1", "Reading response");
                InputStream is = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
                reader.close();

                //get D1 localIP
                String responseString = response.toString().trim();
                String localIP = "";
                if(responseString.contains("Local IP")){
                    localIP = responseString.split("Local IP: ")[1].trim();
                }
                String finalIP = localIP;
                runOnUiThread(()->{
                    Toast.makeText(WaterPlants.this, "D1 Local IP: " + finalIP, Toast.LENGTH_LONG).show();

                });


                // Handle the response
                runOnUiThread(() -> {
                    Log.d("SEND TO D1", "Response received: " + response.toString().trim());
                    Toast.makeText(WaterPlants.this, "Response: " + response.toString().trim(), Toast.LENGTH_LONG).show();
                });

            } catch (IOException e) {
                Log.e("SEND TO D1", "IOException: " + e.getMessage(), e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();

    }
}


