package com.example.wemosd1wifiapp;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.example.wemosd1wifiapp.R;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    public String D1IP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavBar);

        // Load HomeFragment initially
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new HomeFragment())
                    .commit();
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.home2) {
                selectedFragment = new HomeFragment();
            } else if (item.getItemId() == R.id.wifiScan) {
                selectedFragment = new WiFiScanFragment();
            }
            else if(item.getItemId() == R.id.register){
                selectedFragment = new RegisterFragment();
            }
            else if(item.getItemId() == R.id.login){
                selectedFragment = new LoginFragment();
            }

            /*else if(item.getItemId() == R.id.settings){
                selectedFragment = new SettingsFragment();

            }*/ else if (item.getItemId() == R.id.waterPlants) {
                selectedFragment = new WaterPlantsFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, selectedFragment)
                        .commit();
                return true;
            }

            return false;
        });

        TextView connTV = findViewById(R.id.connTV);

// Check if the TextView is empty
        if (connTV != null && connTV.getText().toString().trim().isEmpty()) {

            Log.d("TextViewContent", "ConnTV text: " + connTV.getText().toString().trim());
            Log.d("MainActivity", "Calling ReadFromStorage() because TextView is empty.");

            ReadFromStorage();


        } else {

            String D1IP = connTV.getText().toString().trim();
            if (D1IP.isEmpty()) {

                Toast.makeText(MainActivity.this, "ConnTV is empty, please connect", Toast.LENGTH_LONG).show();
            } else {

                ReadFromStorage();


            }
        }
        Intent intent = getIntent();
        String D1IP = intent.getStringExtra("D1IP");

        if(D1IP != null){
            connTV.setText(D1IP);
        }
    }

    private void ReadFromStorage() {
        try{
            File file = new File(getFilesDir(), "d1ip.txt");
            Log.d("FileCheck", "File exists: " + file.exists());

            if(file.exists()){
                FileInputStream fileInputStream = new FileInputStream(file);
                StringBuilder stringBuilder = new StringBuilder();
                int character;

                while ((character = fileInputStream.read()) != -1) {
                    stringBuilder.append((char) character);
                }
                fileInputStream.close();
                Log.d("FileContent", "Contents of the file: " + stringBuilder.toString());

                String D1IP = stringBuilder.toString().trim();

                if(!D1IP.isEmpty()){
                    Log.d("Ping", "Pinging IP: " + D1IP);
                    ping(D1IP);

                }else{
                    Toast.makeText(this, "No IP address found in file", Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
            }
        }catch (IOException e){
            Log.e("Reading File", "Error reading file", e);
            Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show();
        }
    }

    private void ping(String d1IP) {
        new Thread(() -> {
            try {
                Log.d("INSIDE PING METHOD", "Pinging IP: " + d1IP);
                // Execute the ping command
                Process process = Runtime.getRuntime().exec("ping -c 4 " + d1IP); // For Linux/macOS


                // Read the output from the ping command
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                reader.close();

                // Log the full output of the ping command
                Log.d("PING OUTPUT", "Ping result for IP " + d1IP + ":\n" + output.toString());
                // Parse the packet statistics from the ping output
                String result = output.toString();
                int packetsSent = 0;
                int packetsReceived = 0;
                final int[] packetLoss = {0}; // Make packetLoss final by using an array
                String roundTripTimes = "";

                if (result.contains("ping statistics")) {
                    // Parsing the number of packets sent, received, and packet loss
                    String[] lines = result.split("\n");
                    for (String lineItem : lines) {
                        if (lineItem.contains("packets transmitted")) {
                            String[] stats = lineItem.split(",");
                            packetsSent = Integer.parseInt(stats[0].trim().split(" ")[0]);
                            packetsReceived = Integer.parseInt(stats[1].trim().split(" ")[0]);

                            for (String stat : stats) {
                                if (stat.contains("packet loss")) {
                                    packetLoss[0] = Integer.parseInt(stat.trim().split(" ")[0].replace("%", ""));
                                }
                            }
                        } else if (lineItem.contains("received")) {
                            // Extract number of packets received and packet loss percentage
                            String[] receivedInfo = lineItem.split(",");
                            packetsReceived = Integer.parseInt(receivedInfo[0].split(" ")[0]);
                            packetLoss[0] = Integer.parseInt(receivedInfo[2].split(" ")[1].replace("%", ""));
                        } else if (lineItem.contains("round-trip min/avg/max")) {
                            // Extract round-trip time statistics
                            roundTripTimes = lineItem.trim();
                        }
                    }

                    // Log packet statistics
                    Log.d("PING STATS", "Packets Sent: " + packetsSent + ", Packets Received: " + packetsReceived +
                            ", Packet Loss: " + packetLoss[0] + "%, Round Trip Times: " + roundTripTimes);

                    // Use the data in the UI
                    runOnUiThread(() -> {
                        if (packetLoss[0] == 0) {
                            TextView connTV = findViewById(R.id.connTV);
                            connTV.setText(d1IP);
                            Toast.makeText(this, "ESP is connected with no packet loss at IP: " + d1IP, Toast.LENGTH_LONG).show();

                            HomeFragment homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag("HomeFragment");
                            if(homeFragment!=null){
                                Bundle bundle = new Bundle();
                                bundle.putString("d1IP", d1IP);
                                homeFragment.setArguments(bundle);
                            }

                        } else {
                            Toast.makeText(this, "Packet loss detected. Please ensure the device is connected to your WiFi.", Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    // If ping stats not found
                    Log.e("PING ERROR", "No ping statistics found in the result");
                }
            } catch (IOException e) {
                Log.e("PING ERROR", "Unable to ping ESP at IP: " + d1IP, e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error Pinging device.", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    public String getConnectionIP(){
        TextView connTV = findViewById(R.id.connTV);
        return connTV != null ? connTV.getText().toString() : "";
    }
}

