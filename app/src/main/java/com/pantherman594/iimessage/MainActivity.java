/*
 * Copyright (c) 2017 David Shen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pantherman594.iimessage;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private String prefSocketAddressKey;
    private String prefPassword;
    private EditText etPassword;

    static boolean validIP(String ip) {
        try {
            if (ip == null || ip.isEmpty()) {
                return false;
            }

            if (ip.contains(":")) {
                String[] parts = ip.split(":");
                if (parts.length != 2) {
                    return false;
                }

                int port = Integer.parseInt(parts[1]);
                if ((port < 0) || (port > 65535)) {
                    return false;
                }
                ip = parts[0];
            }

            return !ip.endsWith(".");
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.prefSocketAddressKey = getString(R.string.pref_socket_address_key);
        this.prefPassword = getString(R.string.pref_password);

        // Check if Set IP Address
        if (!sharedPreferences.contains(prefSocketAddressKey)) {
            // Show IP Set up
            setContentView(R.layout.activity_main);

            etPassword = (EditText) findViewById(R.id.etPassword);
            sharedPreferences.getString(prefSocketAddressKey, "127.0.0.1");

            final Button btnStartIiM = (Button) findViewById(R.id.btnStartIiM);
            btnStartIiM.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Get value and
                    EditText etSocketAddress = (EditText) findViewById(R.id.etSocketAddress);
                    boolean error = false;

                    // Save preference
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    // Check if IP is valid string
                    if (validIP(etSocketAddress.getText().toString().trim())) {
                        editor.putString(prefSocketAddressKey, etSocketAddress.getText().toString().trim());
                    } else {
                        CharSequence text = "Invalid IP address";
                        Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
                        error = true;
                    }

                    if (etPassword.getText().toString() != null && !etPassword.getText().toString().isEmpty()) {
                        editor.putString(prefPassword, etPassword.getText().toString());
                    } else {
                        CharSequence text = "Invalid password";
                        Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
                        error = true;
                    }

                    if (!error) {
                        editor.apply();

                        // Start ReceiveMessagesService and load ChatActivity
                        iiMessageApplication.getInstance().startServerBridge();
                        startChatActivity();
                    }
                }
            });

            etPassword.setOnEditorActionListener(new EditText.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        btnStartIiM.performClick();
                    }
                    return false;
                }
            });
        } else {
            // Load to ChatActivity
            startChatActivity();
        }

    }

    private void startChatActivity() {
        Intent chatActivityIntent = new Intent(this, ChatsActivity.class);
        startActivity(chatActivityIntent);
        finish();
    }
}
