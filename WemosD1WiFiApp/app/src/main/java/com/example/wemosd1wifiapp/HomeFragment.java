package com.example.wemosd1wifiapp;

import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;


import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.ekn.gruzer.gaugelibrary.ArcGauge;
import com.ekn.gruzer.gaugelibrary.Range;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HomeFragment extends Fragment {

    public ArcGauge arcGauge;
    public ArcGauge arcGauge2;
    public Spinner potSelector;
    public String d1IP;

    public HomeFragment() {

    }
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        arcGauge = root.findViewById(R.id.arcGauge);
        arcGauge2 = root.findViewById(R.id.arcGauge2);

        potSelector = root.findViewById(R.id.potSelector);


        if(getArguments() != null){
            d1IP = getArguments().getString("d1IP");
        }

        if(d1IP != null && !d1IP.isEmpty()){
            updatePotSelectorDropDown(d1IP);
        }


        arcGauge2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity activity = (MainActivity) getActivity();
                String IP = activity.getConnectionIP();
                GetWaterLevel(IP);
            }
        });

// Set min and max values (0 to 100 for percentage-based representation)
        arcGauge.setMinValue(0.0);
        arcGauge.setMaxValue(100.0);

// Define color ranges
        Range low = new Range();
        low.setFrom(0.0);
        low.setTo(25.0);
        low.setColor(Color.RED);

        Range mid = new Range();
        mid.setFrom(25.0);
        mid.setTo(50.0);
        mid.setColor(Color.YELLOW);

        Range midHigh = new Range();
        midHigh.setFrom(50.0);
        midHigh.setTo(75.0);
        midHigh.setColor(Color.parseColor("#8da832"));

        Range high = new Range();
        high.setFrom(75.0);
        high.setTo(100.0);
        high.setColor(Color.GREEN);


        //Adding 'ARC's' for the battery level indicator
        arcGauge.addRange(low);
        arcGauge.addRange(mid);
        arcGauge.addRange(midHigh);
        arcGauge.addRange(high);

// Set initial value
        arcGauge.setValue(50.0);

        //Check Battery Button
        Button checkBattery = root.findViewById(R.id.checkBattery);
        checkBattery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null) {
                    String IP = activity.getConnectionIP();
                    CheckBatteryLevels(IP);

                }else {
                    Log.e("HomeFragment", "MainActivity is null");
                }
            }
        });
        //BrokerBtn
        Button sendToBroker = root.findViewById(R.id.brokerBtn);
        sendToBroker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendToBroker();
            }
        });


        Button addPot = root.findViewById(R.id.addPotBtn);
        addPot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(requireContext(), "Button Pressed", Toast.LENGTH_LONG).show();

                Fragment selectedFragment = new AddPot();
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragmentContainer, selectedFragment);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });

        return root; //  Return the correctly inflated view
    }

    private void updatePotSelectorDropDown(String d1IP) {

        ArrayAdapter<String> adapter = (ArrayAdapter<String>) potSelector.getAdapter();
        adapter.add(d1IP);
    }

    private void GetWaterLevel(String ip) {
        //Send request to the Arduino
        new Thread(() -> {
            HttpURLConnection connection = null;
            try{
                Log.d("ESP-32-Battery", "Sending Water Level Request");
                String url = "http://" + ip + "/checkPlants";
                URL urlObj = new URL(url);
                connection = (HttpURLConnection) urlObj.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setConnectTimeout(5000);  // 5 seconds timeout for connection
                connection.setReadTimeout(10000);

                InputStream is = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
                reader.close();
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getActivity(), "Water Level Response: " + response, Toast.LENGTH_LONG).show();
                    double waterLevel = Double.parseDouble(response.toString().trim());
                    arcGauge2.setValue(waterLevel);
                });
            }catch (IOException e){
                Log.e("Error-Battery", "Error sending Request. Error Code: " + e.getMessage());
            }
        }).start(); //STart the thread.
    }

    private void CheckBatteryLevels(String IP) {
        Log.d("BatteryCheck", "IP from MainActivity: " + IP);

        //Send Request to ESP

        new Thread(() -> {
            HttpURLConnection connection = null;
            try{
                Log.d("ESP-32-Battery", "Sending Battery Level Request");
                String url = "http://" + IP + "/checkBattery";
                URL urlObj = new URL(url);
                connection = (HttpURLConnection) urlObj.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setConnectTimeout(5000);  // 5 seconds timeout for connection
                connection.setReadTimeout(10000);

                InputStream is = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
                reader.close();
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getActivity(), "Battery Level Response: " + response, Toast.LENGTH_LONG).show();
                    double battLevel = Double.parseDouble(response.toString().trim());
                    arcGauge.setValue(battLevel);
                });

            }catch (IOException e){
                Log.e("Error-Battery", "Error sending Request. Error Code: " + e.getMessage());
            }
        }).start(); //STart the thread.
    }
    private void SendToBroker() {
        try {
            // Create an MQTT client
            MqttClient mqttClient = new MqttClient("tcp://46.64.12.240:1883", "ClientID", new MemoryPersistence());

            // Set connection options
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            // Connect to the MQTT broker
            mqttClient.connect(options);

            // Publish a message to a topic
            MqttMessage message = new MqttMessage();
            message.setPayload("Hello, World!".getBytes());
            mqttClient.publish("test/topic", message);

            // Close the connection
            mqttClient.disconnect();
        } catch (MqttException e) {
            System.err.println("Error connecting to MQTT Broker: " + e.getMessage());
            e.printStackTrace(); // Log the error
        }
    }
}