package com.example.wemosd1wifiapp;

import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RegisterFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RegisterFragment extends Fragment {


    public RegisterFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment RegisterFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RegisterFragment newInstance(String param1, String param2) {
        RegisterFragment fragment = new RegisterFragment();
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
        // Inflate the layout for this fragment
        View root =  inflater.inflate(R.layout.fragment_register, container, false);

        //Get values from fields
        EditText firstNameInput = root.findViewById(R.id.firstName);
        EditText surnameInput = root.findViewById(R.id.surname);
        EditText userNameInput = root.findViewById(R.id.username);
        EditText passwordInput = root.findViewById(R.id.password);
        EditText emailInput = root.findViewById(R.id.email);





        Button registerButton = root.findViewById(R.id.registerBtn);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String firstname = firstNameInput.getText().toString().trim();
                String surname = surnameInput.getText().toString().trim();
                String username = userNameInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();
                String email = emailInput.getText().toString().trim();

                Log.d("Register Input", "FirstName: " + firstname);
                Log.d("Register Input", "LastName: " + surname);
                Log.d("Register Input", "UserName: " + username);
                Log.d("Register Input", "Password: " + password);
                Log.d("Register Input", "Email: " + email);

                CompleteRegistration(firstname, surname, username, password, email);
            }
        });




        return root;
    }

    private void CompleteRegistration(String firstname, String surname, String username, String password, String email) {

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                Log.d("Registering New User", "Register New User");

                // Prepare the POST data
                String postData = "UserName=" + URLEncoder.encode(username, "UTF-8") +
                        "&Password=" + URLEncoder.encode(password, "UTF-8") +
                        "&Email=" + URLEncoder.encode(email, "UTF-8") +
                        "&FirstName=" + URLEncoder.encode(firstname, "UTF-8") +
                        "&LastName=" + URLEncoder.encode(surname, "UTF-8");


                // First connection
                URL url = new URL("https://iotsplant.duckdns.org/api/UserLoginRegister/register");
                connection = (HttpURLConnection) url.openConnection();
                connection.setInstanceFollowRedirects(false); // Disable automatic redirect
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(10000);

                // Write data to request body
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = postData.getBytes("UTF-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                Log.d("Register Response", "Response Code: " + responseCode);

                // Handle redirect manually if needed
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == 307) {
                    String redirectUrl = connection.getHeaderField("Location");
                    Log.d("Redirect", "Redirect to: " + redirectUrl);

                    // Open new connection to redirected URL
                    connection.disconnect();
                    URL newUrl = new URL(redirectUrl);
                    connection = (HttpURLConnection) newUrl.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                    try (OutputStream os = connection.getOutputStream()) {
                        byte[] input = postData.getBytes("UTF-8");
                        os.write(input, 0, input.length);
                    }

                    responseCode = connection.getResponseCode();
                }

                // Read the response body
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    Log.d("Register Response", "Response Body: " + response.toString());
                }

            } catch (Exception e) {
                Log.e("Register Error", "Exception: ", e);

                // read the server's error response
                if (connection != null) {
                    try (InputStream errorStream = connection.getErrorStream()) {
                        if (errorStream != null) {
                            BufferedReader br = new BufferedReader(new InputStreamReader(errorStream, "UTF-8"));
                            StringBuilder errorResponse = new StringBuilder();
                            String line;
                            while ((line = br.readLine()) != null) {
                                errorResponse.append(line.trim());
                            }
                            Log.e("Register Error Body", "Error response from server: " + errorResponse.toString());
                        } else {
                            Log.e("Register Error Body", "Error stream is null");
                        }
                    } catch (IOException ioException) {
                        Log.e("Register Error", "Failed to read error stream", ioException);
                    }
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }


}