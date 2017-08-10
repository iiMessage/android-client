package com.pantherman594.iimessage;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class iiMPreferenceActivity extends android.preference.PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new iiMPreferenceFragment()).commit();
    }

    public static class iiMPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        private static final String TAG = iiMPreferenceFragment.class.getSimpleName();
        private String oldAddr;
        private String oldPass;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Load user settings xml resource
            addPreferencesFromResource(R.xml.user_settings);
            EditTextPreference passwordPref = (EditTextPreference) findPreference(getString(R.string.pref_password));
            passwordPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    preference.setKey("");
                    return true;
                }
            });
            updateSocketAddressSummary(findPreference(getString(R.string.pref_socket_address_key)));

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(iiMessageApplication.getInstance());
            oldAddr = sharedPreferences.getString(this.getString(R.string.pref_socket_address_key), "127.0.0.1");
            oldPass = sharedPreferences.getString(this.getString(R.string.pref_password), "password");
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference pref = findPreference(key);

            boolean restartBridge = false;

            Log.i(TAG, "Preference change detected");
            if (pref instanceof EditTextPreference && key.equals(getString(R.string.pref_socket_address_key))) {
                Log.i(TAG, "Setting IP address");

                String socketAddress = sharedPreferences.getString(this.getString(R.string.pref_socket_address_key), "127.0.0.1");

                if (MainActivity.validIP(socketAddress)) {
                    restartBridge = true;
                } else {
                    CharSequence text = "Invalid IP address";
                    Toast.makeText(iiMessageApplication.getInstance(), text, Toast.LENGTH_SHORT).show();
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(this.getString(R.string.pref_socket_address_key), oldAddr);
                    editor.apply();
                }

                updateSocketAddressSummary(pref);
            } else if (pref instanceof EditTextPreference && key.equals(getString(R.string.pref_password))) {
                Log.i(TAG, "Setting password");
                String password = sharedPreferences.getString(this.getString(R.string.pref_password), "password");

                if (!password.isEmpty()) {
                    restartBridge = true;
                } else {
                    CharSequence text = "Invalid password";
                    Toast.makeText(iiMessageApplication.getInstance(), text, Toast.LENGTH_SHORT).show();
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(this.getString(R.string.pref_password), oldPass);
                    editor.apply();
                }
            }

            if (restartBridge) {
                iiMessageApplication.getInstance().startServerBridge();
            }
        }

        private void updateSocketAddressSummary(Preference pref) {
            EditTextPreference editTextPreference = (EditTextPreference) pref;
            pref.setSummary(editTextPreference.getText());
        }

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
            PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this);
        }
    }


}
