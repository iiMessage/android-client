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
import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.TreeMap;

public class iiMessageApplication extends Application {
    private final static String TAG = iiMessageApplication.class.getSimpleName();
    private static iiMessageApplication instance;
    private ServerBridge serverBridge = null;
    private TreeMap<String, Chat> chats;  //  chatId, chat

    public iiMessageApplication() {
        if (instance == null) {
            instance = this;
        } else {
            Log.e(TAG, "There is an error. You tried to create more than 1 iiMessageApp");
        }
    }

    static iiMessageApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "iMessageApp is loaded and running");

        chats = new TreeMap<>();

        registerActivityLifecycleCallbacks(new LifecycleHandler());
        startServerBridge();
    }

    void startServerBridge() {
        if (serverBridge != null) {
            Log.i(TAG, "Stopping server bridge");
            serverBridge.stop();
        }
        Log.i(TAG, "Starting server bridge");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.contains(getString(R.string.pref_socket_address_key))) {
            try {
                serverBridge = new ServerBridge();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    ServerBridge getServerBridge() {
        return serverBridge;
    }

    synchronized TreeMap<String, Chat> getChats() {
        return chats;
    }

    void addMessage(Message message) {
        if (!getChats().containsKey(message.getChatId())) {
            getChats().put(message.getChatId(), new Chat(message.getChatId(), message.getChatName()));
        }
        message.getChat().addMessage(message.getId(), message);
    }

    void removeMessage(Message oldMessage) {
        if (getChats().containsKey(oldMessage.getChatId())) {
            oldMessage.getChat().removeMessage(oldMessage.getId());
            if (oldMessage.getChat().getMessages().isEmpty()) {
                getChats().remove(oldMessage.getChatId());
            }
        }
    }

    static Activity getActivity() {
        try {
            Class activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);

            Map<Object, Object> activities = (Map<Object, Object>) activitiesField.get(activityThread);
            if (activities == null)
                return null;

            for (Object activityRecord : activities.values()) {
                Class activityRecordClass = activityRecord.getClass();
                Field pausedField = activityRecordClass.getDeclaredField("paused");
                pausedField.setAccessible(true);
                if (!pausedField.getBoolean(activityRecord)) {
                    Field activityField = activityRecordClass.getDeclaredField("activity");
                    activityField.setAccessible(true);
                    return (Activity) activityField.get(activityRecord);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    SocketInfo getSocket() throws IOException {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String socketAddress = sharedPreferences.getString(this.getString(R.string.pref_socket_address_key), "127.0.0.1");
        int port = 5000;
        if (socketAddress.contains(":")) {
            port = Integer.parseInt(socketAddress.split(":")[1]);
            socketAddress = socketAddress.split(":")[0];
        }
        Log.i(TAG, socketAddress + ":" + port);
        return new SocketInfo(socketAddress, port);
    }

    class SocketInfo {
        private String socketAddress;
        private int port;

        SocketInfo(String socketAddress, int port) {
            this.socketAddress = socketAddress;
            this.port = port;
        }

        String getSocketAddress() {
            return socketAddress;
        }

        int getPort() {
            return port;
        }
    }
}
