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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.TreeMap;
import java.util.TreeSet;

class ServerBridge {
    private Socket socket;
    private PrintWriter output;
    private BufferedReader input;

    private boolean isConnected = false;
    private boolean shouldPing = true;

    private SecretKeySpec secretKey;
    private Cipher cipher;
    private byte[] iv;

    private TreeMap<String, Chat> chats = iiMessageApplication.getInstance().getChats();

    private final File cacheFile = new File(iiMessageApplication.getInstance().getApplicationContext().getFilesDir(), "messagesCache");

    ServerBridge() throws IOException {
        final iiMessageApplication.SocketInfo sInfo = iiMessageApplication.getInstance().getSocket();
        try {
            cipher = Cipher.getInstance(Constants.AES_PADDING);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }

        if (!cacheFile.createNewFile()) {
            try (FileReader fr = new FileReader(cacheFile);
                 BufferedReader br = new BufferedReader(fr)) {
                String data;
                while((data = br.readLine()) != null && !data.isEmpty()) {
                    JSONObject dataJson = new JSONObject(data);
                    if (dataJson.has(Constants.NUM_MESSAGES)) {
                        parseAddNotify(dataJson);
                    } else if (dataJson.getString(Constants.ACTION).equals(Constants.Action.READ)){
                        for (Message message : iiMessageApplication.getInstance().getChats().get(dataJson.getString(Constants.Col.CHAT_ID)).getMessages()) {
                            if (message.getId() == dataJson.getLong(Constants.Col.ID)) {
                                message.setReadVar(true);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(sInfo.getSocketAddress(), sInfo.getPort());
                    output = new PrintWriter(socket.getOutputStream(), true);
                    input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                boolean start = true;
                while (shouldPing) {
                    try {
                        if (start) start = false;
                        else {
                            if (LifecycleHandler.isApplicationInForeground()) { // If application in foreground, sync evey 3 seconds. If not, every 30.
                                Thread.sleep(3 * 1000);
                            } else {
                                Thread.sleep(30 * 1000);
                            }
                        }

                        if (!socket.isConnected()) {
                            isConnected = false;
                            shouldPing = false;
                            iiMessageApplication.getInstance().startServerBridge();
                            continue;
                        }
                        if (!isConnected) establishConnection();
                        else requestNew(false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private synchronized void establishConnection() {
        try {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(iiMessageApplication.getInstance());
            String password = sharedPreferences.getString(iiMessageApplication.getInstance().getString(R.string.pref_password), Constants.TEST_DATA);
            secretKey = genSecretKey(password);

            iv = new byte[Constants.SECRET_PAD_LEN];
            SecureRandom prng = new SecureRandom();
            prng.nextBytes(iv);

            JSONObject connectJson = new JSONObject();
            connectJson.put(Constants.ACTION, Constants.Action.EST);
            connectJson.put(Constants.Col.MSG, Constants.TEST_DATA);
            String connectStr = connectJson.toString();
            connectJson = new JSONObject();
            try {
                connectJson.put(Constants.ENCRYPTED, encrypt(connectStr));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            connectJson.put(Constants.IV, Base64.encodeToString(iv, Base64.NO_WRAP));
            Log.i(ServerBridge.class.getSimpleName(), "Waiting for verification...");
            sendNoEncrypt(connectJson, new ResponseCallback() { // Step 1 of connecting: send test data to server (encrypted)
                @Override
                public void run(JSONObject responseJson) {
                    isConnected = false;
                    try {
                        if (responseJson.getBoolean(Constants.SUCCESS)) { // Step 2 of connecting: receive server's response, whether connection successful (encrypted)
                            Log.i(ServerBridge.class.getSimpleName(), "Success!");
                            isConnected = true;
                        } else {
                            Log.i(ServerBridge.class.getSimpleName(), "Failure!");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static SecretKeySpec genSecretKey(String password) throws UnsupportedEncodingException {
        if (password.length() < Constants.SECRET_PAD_LEN) {
            int missingLength = Constants.SECRET_PAD_LEN - password.length();
            StringBuilder passwordBuilder = new StringBuilder(password);
            for (int i = 0; i < missingLength; i++) {
                passwordBuilder.append(" ");
            }
            password = passwordBuilder.toString();
        }
        byte[] key = password.substring(0, Constants.SECRET_PAD_LEN).getBytes(Constants.CHARSET);
        return new SecretKeySpec(key, Constants.AES);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    void requestNew(boolean getAll) {
        long date = 0;
        if (!getAll && !chats.isEmpty()) date = new TreeSet<>(this.chats.values()).last().getLastMessage().getDate();
        if (getAll && isConnected) {
            cacheFile.delete();
            try {
                cacheFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        JSONObject message = new JSONObject();
        try {
            message.put(Constants.ACTION, Constants.Action.REQ);
            message.put(Constants.Col.DATE, date);
        } catch (JSONException ignored) {
        }
        sendRaw(message, new ResponseCallback() {
            @Override
            public void run(JSONObject responseJson) {
                try {
                    if (responseJson.getInt(Constants.NUM_MESSAGES) > 0) {
                        try (FileWriter fw = new FileWriter(cacheFile, true);
                             BufferedWriter bw = new BufferedWriter(fw);
                             PrintWriter out = new PrintWriter(bw)) {
                            out.println(responseJson.toString());
                            out.close();
                            bw.close();
                            fw.close();
                            parseAddNotify(responseJson);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    void markAsRead(Message msg) {
        JSONObject message = new JSONObject();
        try {
            message.put(Constants.ACTION, Constants.Action.READ);
            message.put(Constants.Col.ID, msg.getId());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendRaw(message, null);
        try (FileWriter fw = new FileWriter(cacheFile, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            message.put(Constants.Col.CHAT_ID, msg.getChatId());
            out.println(message.toString());
            out.close();
            bw.close();
            fw.close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    void sendMessage(final Message tempMessage, final MessageActivity messageActivity, final boolean isReply) {
        final JSONObject message = new JSONObject();
        try {
            message.put(Constants.ACTION, Constants.Action.SEND);
            message.put(Constants.Col.CHAT_ID, tempMessage.getChatId());
            message.put(Constants.Col.MSG, tempMessage.getMsg());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendRaw(message, new ResponseCallback() {
            @Override
            public void run(JSONObject responseJson) {
                try {
                    Message newMessage = parseMessage(responseJson);
                    Chat chat = newMessage.getChat();
                    chat.removeMessage(tempMessage.getId());
                    chat.addMessage(newMessage.getId(), newMessage);
                    if (!isReply) {
                        messageActivity.initMessagesListAdapter();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void sendRaw(JSONObject message, final ResponseCallback callback) {
        while (!isConnected) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        String messageString = message.toString();
        message = new JSONObject();
        try {
            message.put(Constants.ENCRYPTED, encrypt(messageString));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendNoEncrypt(message, new ResponseCallback() {
            @Override
            public void run(JSONObject response) {
                if (response.has(Constants.ENCRYPTED)) {
                    try {
                        response = new JSONObject(decrypt(response.getString(Constants.ENCRYPTED)));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                if (callback != null) callback.run(response);
            }
        });
    }

    private synchronized void sendNoEncrypt(final JSONObject message, final ResponseCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                output.println(message.toString());
                try {
                    String response = input.readLine();
                    if (response == null) {
                        isConnected = false;
                        return;
                    }
                    JSONObject responseJson = new JSONObject(response);
                    if (callback != null) callback.run(responseJson);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static TreeSet<Message> parseMessages(JSONObject messagesJson) throws JSONException {
        TreeSet<Message> messages = new TreeSet<>();
        JSONArray messagesArray = messagesJson.getJSONArray(Constants.INCOMING);

        for (int i = 0; i < messagesArray.length(); i++) {
            messages.add(parseMessage((JSONObject) messagesArray.get(i)));
        }
        return messages;
    }

    private static Message parseMessage(JSONObject messageJson) throws JSONException {
        long messageId = messageJson.getLong(Constants.Col.ID);
        String msg = messageJson.getString(Constants.Col.MSG);
        long date = messageJson.getLong(Constants.Col.DATE);
        int isFromMe = messageJson.getInt(Constants.Col.IS_FROM_ME);
        int isSent = messageJson.getInt(Constants.Col.IS_SENT);
        String sender = messageJson.getString(Constants.Col.SENDER);
        int isRead = messageJson.getInt(Constants.Col.IS_READ);
        String chatId = messageJson.getString(Constants.Col.CHAT_ID);
        String chatName = messageJson.getString(Constants.Col.CHAT_NAME);

        return new Message(messageId, msg, date, sender, isSent, isFromMe, isRead, chatId, chatName);
    }

    @SuppressWarnings("ConstantConditions")
    private void parseAddNotify(JSONObject messages) throws JSONException {
        if (messages.has(Constants.NUM_MESSAGES) && messages.getInt(Constants.NUM_MESSAGES) > 0) {
            String openChatId = "";
            if (iiMessageApplication.getActivity() != null && iiMessageApplication.getActivity() instanceof MessageActivity) {
                openChatId = ((MessageActivity) iiMessageApplication.getActivity()).getChatId();
            }

            for (Message message : parseMessages(messages)) {
                iiMessageApplication.getInstance().addMessage(message);
                if (!message.isRead() && !openChatId.equals(message.getChatId())) {
                    Notification.updateNotification(message.getChatId());
                }
            }
            final Activity activity = iiMessageApplication.getActivity();
            if (activity != null) {
                if (activity instanceof ChatsActivity) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((ChatsActivity) activity).reloadChatListAndAdapter();
                            ((ChatsActivity) activity).stopRefreshing();
                        }
                    });
                } else if (activity instanceof MessageActivity) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((MessageActivity) activity).initMessagesListAdapter();
                        }
                    });
                }
            }
        }
    }

    private String encrypt(String data) {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
            return Base64.encodeToString(cipher.doFinal(data.getBytes(Constants.CHARSET)), Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String decrypt(String data) {
        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            return new String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)), Constants.CHARSET);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    void stop() {
        try {
            socket.close();
            output.close();
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        cipher = null;
        isConnected = false;
        shouldPing = false;
    }

    interface ResponseCallback {
        void run(JSONObject response);
    }
}
