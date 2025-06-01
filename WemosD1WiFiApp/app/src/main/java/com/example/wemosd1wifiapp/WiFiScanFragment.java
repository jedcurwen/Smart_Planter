package com.example.wemosd1wifiapp;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import androidx.fragment.app.Fragment;

import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

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

public class WiFiScanFragment extends Fragment {

    public String D1IP;
    public ListView networksList;
    private ArrayList<String> discoveredWifi = new ArrayList<>();
    private ArrayAdapter<String> discoveredWifiAdapter;

    private WifiManager wifiManager;
    private BroadcastReceiver wifiScanReceiver;

    public WiFiScanFragment() {
        // Required empty public constructor
    }

    public static WiFiScanFragment newInstance(String param1, String param2) {
        WiFiScanFragment fragment = new WiFiScanFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            // Handle arguments if needed
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_wi_fi_scan, container, false);

        // Initialize the ListView and Adapter
        networksList = root.findViewById(R.id.networksList);
        discoveredWifiAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, discoveredWifi);
        networksList.setAdapter(discoveredWifiAdapter);

        Button wifiBut = root.findViewById(R.id.scanWifiBtn);
        wifiBut.setOnClickListener(v -> {
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

                    // Use getActivity() to access the Activity and run on the UI thread
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getActivity(), responseStr, Toast.LENGTH_LONG).show();
                        updateListView(responseStr); // Update ListView with scan results
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                    getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Failed to get scan results", Toast.LENGTH_SHORT).show());
                }
            }).start();
        });

        networksList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String wifiSSID = discoveredWifi.get(position);
                promptForPasswordAndConnect(wifiSSID);
            }
        });
        return root;  // Return the root view after setting up the button and listeners
    }

    // Method to update the ListView with the scan results
    private void updateListView(String responseStr) {
        // Assuming responseStr contains a list of discovered Wi-Fi networks
        String[] networks = responseStr.split("\n"); // Split response into an array of networks
        discoveredWifi.clear(); // Clear the previous list
        for (String network : networks) {
            discoveredWifi.add(network); // Add each network to the list
        }
        discoveredWifiAdapter.notifyDataSetChanged(); // Notify adapter to update the ListView
    }

    private void promptForPasswordAndConnect(String ssid) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Connect to " + ssid);

        final EditText input = new EditText(getActivity());
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
                        getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Invalid IP received: [" + localIP + "]", Toast.LENGTH_LONG).show());
                        return;
                    }

                    // Update UI with IP
                    getActivity().runOnUiThread(() -> {
                        D1IP = localIP;
                        Toast.makeText(getActivity(), "D1 Local IP: " + D1IP, Toast.LENGTH_LONG).show();

                        Intent intent = new Intent(getActivity(), MainActivity.class);
                        intent.putExtra("D1IP", D1IP);
                        startActivity(intent);

                        Log.d("ESP-32-IP-ADDRESS", "IP ASSIGNED: " + D1IP);


                        saveToStorage(D1IP);

                        // Connect to main WiFi after setting D1IP
                        connectToMainWiFi();
                    });

                } catch (SocketTimeoutException e) {
                    Log.e("Timeout Error", "ESP32 Timeout", e);
                    getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "ESP timeout. Try again.", Toast.LENGTH_LONG).show());
                } catch (IOException e) {
                    Log.e("SEND TO D1", "IOException: " + e.getMessage(), e);
                    getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Failed to communicate with ESP", Toast.LENGTH_SHORT).show());
                    Log.e("Error ESP", "Failed to communicate with ESP32");
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
                getActivity().runOnUiThread(() -> {
                    wifiManager.reconnect();
                    Toast.makeText(getActivity(), "Attempting to reconnect to the main Wi-Fi...", Toast.LENGTH_SHORT).show();
                });
            }).start();
        }
    }

    private void saveToStorage(String d1IP) {
        try {
            // Use getContext() or requireContext() to get the context for file operations
            File file = new File(requireContext().getFilesDir(), "d1ip.txt");
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(d1IP.getBytes());
            fileOutputStream.close();

            Log.d("Saving File", "D1IP stored successfully at filepath: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e("Saving File", "Error saving file", e);
        }
    }
}
