package com.example.wemosd1wifiapp;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
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

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class WiFiScan extends AppCompatActivity {
    public String D1IP;
    public ListView networksList;
    private ArrayList<String> discoveredWifi = new ArrayList<>();
    private ArrayAdapter<String> discoveredWifiAdapter;

    private WifiManager wifiManager;
    private BroadcastReceiver wifiScanReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavBar);
        bottomNavigationView.setSelectedItemId(R.id.wifiScan); // Ensures "WiFi Scan" is selected



        // Initialize the class member (not a local variable)
        networksList = findViewById(R.id.networksList);
        if (networksList == null) {
            Log.e("WiFiScan", "ListView not found! Check your XML layout.");
        }

        // Initialize the ArrayAdapter with a simple built-in layout
        discoveredWifiAdapter = new ArrayAdapter<>(WiFiScan.this, android.R.layout.simple_list_item_1, discoveredWifi);
        networksList.setAdapter(discoveredWifiAdapter);

        // Initialize WifiManager
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        networksList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String wifiSSID = discoveredWifi.get(position);
                promptForPasswordAndConnect(wifiSSID);
            }
        });

        Button wifiScan = findViewById(R.id.scanWifiBtn);
        wifiScan.setOnClickListener(v -> {
            new Thread(() -> {
                try {
                    Log.d("SEND TO D1", "Scanning for networks..");

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
                    String responseStr = response.toString().trim();
                    runOnUiThread(() -> Toast.makeText(WiFiScan.this, responseStr, Toast.LENGTH_LONG).show());

                    // Update the ListView with the scan results
                    runOnUiThread(() -> updateListView(responseStr));
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(WiFiScan.this, "Failed to get scan results", Toast.LENGTH_SHORT).show());
                }
            }).start();
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
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
        networksList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String wifiSSID = discoveredWifi.get(position);
                promptForPasswordAndConnect(wifiSSID);
            }
        });
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
                    Toast.makeText(WiFiScan.this, "Attempting to reconnect to the main Wi-Fi...", Toast.LENGTH_SHORT).show();
                });
            }).start();
        }
    }

    private void promptForPasswordAndConnect(String ssid) {
        AlertDialog.Builder builder = new AlertDialog.Builder(WiFiScan.this);
        builder.setTitle("Connect to " + ssid);

        final EditText input = new EditText(WiFiScan.this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Connect", (dialog, which) -> {
            String password = input.getText().toString();

            new Thread(() -> {
                HttpURLConnection connection = null;
                try {
                    Log.d("SEND TO D1", "Creating URL connection");
                    String url = "http://192.168.4.1/connect";
                    URL urlObj = new URL(url);
                    connection = (HttpURLConnection) urlObj.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setConnectTimeout(5000);  // 5 seconds timeout for connection
                    connection.setReadTimeout(10000);    // 10 seconds timeout for reading response

                    Uri.Builder uriBuilder = new Uri.Builder()
                            .appendQueryParameter("ssid", ssid)
                            .appendQueryParameter("password", password);
                    String query = uriBuilder.build().getEncodedQuery();

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

                    // Process response
                    String responseString = response.toString().trim();
                    Log.d("SEND TO D1", "Raw Response: [" + responseString + "]");

                    if (responseString.isEmpty()) {
                        throw new IOException("Empty response from ESP");
                    }

                    String localIP = responseString.trim();

                    // Validate IP format
                    if (!localIP.matches("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b")) {
                        runOnUiThread(() -> Toast.makeText(WiFiScan.this, "Invalid IP received: [" + localIP + "]", Toast.LENGTH_LONG).show());
                        return;
                    }

                    // Update UI with IP
                    runOnUiThread(() -> {
                        D1IP = localIP;
                        Toast.makeText(WiFiScan.this, "D1 Local IP: " + D1IP, Toast.LENGTH_LONG).show();

                        Intent intent = new Intent(WiFiScan.this, MainActivity.class);
                        intent.putExtra("D1IP", D1IP);
                        startActivity(intent);

                        Log.d("D1IPAddress", "IP ASSIGNED: " + D1IP);


                        saveToStorage(D1IP);

                        // Connect to main WiFi after setting D1IP
                        connectToMainWiFi();
                    });

                } catch (SocketTimeoutException e) {
                    Log.e("SEND TO D1", "Timeout waiting for response", e);
                    runOnUiThread(() -> Toast.makeText(WiFiScan.this, "ESP timeout. Try again.", Toast.LENGTH_LONG).show());
                } catch (IOException e) {
                    Log.e("SEND TO D1", "IOException: " + e.getMessage(), e);
                    runOnUiThread(() -> Toast.makeText(WiFiScan.this, "Failed to communicate with ESP", Toast.LENGTH_SHORT).show());
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }).start();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void saveToStorage(String d1IP) {
        try{

            File file = new File(getFilesDir(), "d1ip.txt");
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(d1IP.getBytes());
            fileOutputStream.close();

            Log.d("Saving File", "D1IP stored successfully at filepath: "+ file.getAbsolutePath());


        }catch (IOException e){
            Log.e("Saving File", "Error saving file", e);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(wifiScanReceiver);
    };
};

