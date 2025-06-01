package com.example.wemosd1wifiapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WaterPlantsFragment extends Fragment {
    private View rootView;
//connectedDevice
    public WaterPlantsFragment() {

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_water_plants, container, false);

        Button waterPlants = rootView.findViewById(R.id.waterPlantsButton);
        waterPlants.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity activity = (MainActivity)getActivity();
                if(activity != null){
                    String IP = activity.getConnectionIP();
                    WaterPlants(IP);
                }


            }
        });
        // Inflate the layout for this fragment
        return rootView;
    }
    private void WaterPlants(String IP){
        //Send message to ESP-32 to water the plants
        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                Log.d("ESP-32-WaterPlants", "Attempting to water plants");
                Log.d("ESP-32-WaterPlants-Check-IP", IP);

                String url = "http://" + IP + "/waterPlants";
                URL urlObj = new URL(url);
                connection = (HttpURLConnection) urlObj.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);  // 5 seconds timeout
                connection.setReadTimeout(10000);


                int responseCode = connection.getResponseCode();
                Log.d("ESP-32-WaterPlants", "Response Code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    Log.d("ESP-32-WaterPlants", "Response: " + response.toString());
                } else {
                    Log.e("ESP-32-WaterPlants", "Failed with code: " + responseCode);
                }
            }catch (IOException e){
                Log.e("Exception_Water_Plants", e.getMessage());
            }
        }).start();
    }
}