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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    private BroadcastReceiver wifiScanReceiver;

    private ArrayList<String> discoveredWifi = new ArrayList<>();
    private ArrayAdapter<String> discoveredWifiAdapter;

    public String D1IP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {




        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Reference to the ListView
        ListView networksList = findViewById(R.id.networksList);

        // Initialize the ArrayAdapter with a simple built-in layout
        discoveredWifiAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, discoveredWifi);
        networksList.setAdapter(discoveredWifiAdapter);

        // Initialize WifiManager
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        //Open up 'Water Plants' view
        Button waterBut = (Button) findViewById(R.id.waterView);
        waterBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an intent to start the new activity
                Intent intent = new Intent(MainActivity.this, WaterPlants.class);
                intent.putExtra("D1IP", D1IP);
                startActivity(intent); // Start the new activity
            }
        });

        Button checkBattery = (Button) findViewById(R.id.checkBattery);
        checkBattery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBatteryLevel();
            }
        });



        // Set up the button click listener
        Button wifiBtn = findViewById(R.id.wifiBtn);
        wifiBtn.setOnClickListener(v -> {
            new Thread(() -> {
                try {


                    String url = "http://192.168.4.1/scan"; // IP of the D1 Mini AP
                    URL urlObj = new URL(url);
                    HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
                    connection.setRequestMethod("GET");

                    InputStream is = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line).append("\n");
                    }
                    reader.close();

                    // Handle the response
                    runOnUiThread(() -> Toast.makeText(MainActivity.this,  response.toString().trim(), Toast.LENGTH_LONG).show());
                    //Add to ListView
                    updateListView(response.toString().trim());
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to get scan results", Toast.LENGTH_SHORT).show());
                }
            }).start();
        });

    }

    private void CheckBatteryLevel() {
        Log.d("CheckBattery", "Check Battery Button Pressed!!!");
        Log.d("D1IP", D1IP);


    }


    private void updateListView(String response) {
        runOnUiThread(() -> {
            // Extract network names from the response
            String[] lines = response.split("\n");
            List<String> networks = new ArrayList<>();

            // Skip the first line (number of networks) and start from the second line
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (!line.isEmpty()) {
                    networks.add(line); // Add each network name to the list
                }
            }
            // Update the ArrayAdapter with the new network list
            discoveredWifiAdapter.clear();
            discoveredWifiAdapter.addAll(networks);
            discoveredWifiAdapter.notifyDataSetChanged();
        });
        ListView networksList = (ListView) findViewById(R.id.networksList);
        networksList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String wifiSSID = discoveredWifi.get(position);
                promptForPasswordAndConnect(wifiSSID);



            }
        });
    }

    private void promptForPasswordAndConnect(String ssid) {
        // Create an AlertDialog to prompt the user for the password
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Connect to " + ssid);

        // Set up the input
        final EditText input = new EditText(MainActivity.this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Connect", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String password = input.getText().toString();
                //SEND POST Signal To D1

                new Thread(() -> {
                    HttpURLConnection connection = null;
                    try {
                        Log.d("SEND TO D1", "Creating URL connection");
                        String url = "http://192.168.4.1/connect"; // IP of the D1 Mini AP
                        URL urlObj = new URL(url);
                        connection = (HttpURLConnection) urlObj.openConnection();
                        connection.setRequestMethod("POST");
                        connection.setDoOutput(true);
                        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                        Uri.Builder builder = new Uri.Builder()
                                .appendQueryParameter("ssid", ssid)
                                .appendQueryParameter("password", password);
                        String query = builder.build().getEncodedQuery();

                        Log.d("SEND TO D1", "Sending data: " + query);
                        OutputStream os = connection.getOutputStream();
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                        writer.write(query);
                        writer.flush();
                        writer.close();
                        os.close();


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
                            Toast.makeText(MainActivity.this, "D1 Local IP: " + finalIP, Toast.LENGTH_LONG).show();

                            D1IP = finalIP;
                            TextView connTv = findViewById(R.id.connTV);
                            connTv.setText(D1IP);

                            Button waterButton = findViewById(R.id.waterView);
                            waterButton.setEnabled(true);

                            connectToMainWiFi();




                        });


                        // Handle the response
                        runOnUiThread(() -> {


                            Log.d("SEND TO D1", "Response received: " + response.toString().trim());

                            D1IP = response.toString().trim();
                            Toast.makeText(MainActivity.this, "Response: " + response.toString().trim(), Toast.LENGTH_LONG).show();
                            Log.d("D1IPAddress", "IP ASSIGNED: " + D1IP);


                        });

                    } catch (IOException e) {
                        Log.e("SEND TO D1", "IOException: " + e.getMessage(), e);
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to send data", Toast.LENGTH_SHORT).show());
                    } finally {
                        if (connection != null) {
                            connection.disconnect();
                        }
                    }
                }).start();

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void connectToMainWiFi() {
        // Get the current connected network
        WifiInfo currentNetwork = wifiManager.getConnectionInfo();

        // Ensure that we are currently connected to the D1 Mini's AP
        if (currentNetwork.getSSID() != null && currentNetwork.getSSID().equals("\"Smart_Planter\"")) {
            wifiManager.disconnect();

            // Add a delay to allow for disconnection to complete
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // Wait for 1 second
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Reconnect to the last connected network
                runOnUiThread(() -> {
                    wifiManager.reconnect();
                    Toast.makeText(MainActivity.this, "Attempting to reconnect to the main Wi-Fi...", Toast.LENGTH_SHORT).show();
                });
            }).start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(wifiScanReceiver);
    };

            private void SendWifiDetails(){
                //Send Details to /connect D1 Wemos board

                WifiManager wifiManager1 = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if(wifiManager1 != null){
                    WifiInfo wifiInfo = wifiManager1.getConnectionInfo();
                    String ssid = wifiInfo.getSSID();
                    if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
                        ssid = ssid.substring(1, ssid.length() - 1);
                    }
                    //Create AlertBox to enter password of network
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Password For " + ssid);

                    // Set up the input
                    final EditText input = new EditText(MainActivity.this);
                    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    builder.setView(input);

                    // Set up the buttons
                    builder.setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String password = input.getText().toString();


                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.show();



                    Toast.makeText(MainActivity.this, "Connected to SSID: " + ssid, Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(MainActivity.this, "WiFi Manager not available", Toast.LENGTH_SHORT).show();
                }
            }
        };


/*                //networksList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    //public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        //String selectedNetwork = discoveredWifi.get(position);
                        //promptForPasswordAndConnect(selectedNetwork);

                //});
                /*
            private void promptForPasswordAndConnect(String ssid) {
                // Create an AlertDialog to prompt the user for the password
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Connect to " + ssid);

                // Set up the input
                final EditText input = new EditText(MainActivity.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String password = input.getText().toString();
                        connectToWifi(ssid, password);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        */
                /*
            private void connectToWifi(String ssid, String password) {
                WifiConfiguration wifiConfig = new WifiConfiguration();
                wifiConfig.SSID = String.format("\"%s\"", ssid);
                wifiConfig.preSharedKey = String.format("\"%s\"", password);

                // Add the configuration to the WifiManager
                int netId = wifiManager.addNetwork(wifiConfig);
                wifiManager.disconnect();
                wifiManager.enableNetwork(netId, true);
                wifiManager.reconnect();

                // Optional: You can check the connection status or show a Toast message
                Toast.makeText(MainActivity.this, "Connecting to " + ssid, Toast.LENGTH_SHORT).show();
            }

            private void scanFailure() {
                Toast.makeText(MainActivity.this, "Scan failed! Please try again.", Toast.LENGTH_SHORT).show();
            }
        };

        // Register the BroadcastReceiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiScanReceiver, intentFilter);
*/

/*
public void sendWifiD1(String ssid, String password){
        Log.d("SEND TO D1", "Sending Data to D1 - Start");
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                Log.d("SEND TO D1", "Creating URL connection");
                String url = "http://192.168.4.1/connect"; // IP of the D1 Mini AP
                URL urlObj = new URL(url);
                connection = (HttpURLConnection) urlObj.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("ssid", "123456789")
                        .appendQueryParameter("password", "123456789");
                String query = builder.build().getEncodedQuery();

                Log.d("SEND TO D1", "Sending data: " + query);
                OutputStream os = connection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(query);
                writer.flush();
                writer.close();
                os.close();

                Log.d("SEND TO D1", "Reading response");
                InputStream is = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
                reader.close();

                // Handle the response
                runOnUiThread(() -> {
                    Log.d("SEND TO D1", "Response received: " + response.toString().trim());
                    Toast.makeText(MainActivity.this, "Response: " + response.toString().trim(), Toast.LENGTH_LONG).show();
                });

            } catch (IOException e) {
                Log.e("SEND TO D1", "IOException: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to send data", Toast.LENGTH_SHORT).show());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }
 */