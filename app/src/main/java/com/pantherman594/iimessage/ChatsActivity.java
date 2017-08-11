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
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ChatsActivity extends Activity {
    private static final String TAG = ChatsActivity.class.getSimpleName();
    private static final int SETTINGS_RESULT = 1;
    private SwipeRefreshLayout swipeContainer;
    private ListView lvChats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chats);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarChat);
//        setSupportActionBar(toolbar);

        Log.i(TAG, "creating ChatsActivity");

        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                iiMessageApplication.getInstance().getServerBridge().requestNew(true);
            }
        });

        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_dark);

        TextView tvEdit = (TextView) findViewById(R.id.tvEdit);
        tvEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent iiMPreferenceIntent = new Intent(ChatsActivity.this, iiMPreferenceActivity.class);
                startActivityForResult(iiMPreferenceIntent, SETTINGS_RESULT);
            }
        });

        reloadChatListAndAdapter();

        // Navigate to MessageActivity when selecting chat item
        lvChats.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Chat chat = (Chat) parent.getItemAtPosition(position);

                Intent messageIntent = new Intent(ChatsActivity.this, MessageActivity.class);
                messageIntent.putExtra(Constants.Col.CHAT_ID, chat.getId());
                messageIntent.putExtra(Constants.Col.CHAT_NAME, chat.getName());
                startActivity(messageIntent);
            }
        });

        ImageButton composeButton = (ImageButton) findViewById(R.id.composeButton);
        composeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent messageIntent = new Intent(ChatsActivity.this, MessageActivity.class);
                messageIntent.putExtra(Constants.Col.CHAT_ID, "");
                messageIntent.putExtra(Constants.Col.CHAT_NAME, "");
                startActivity(messageIntent);
            }
        });
    }

    void reloadChatListAndAdapter() {

        ArrayList<Chat> chatsList = new ArrayList<>(iiMessageApplication.getInstance().getChats().values());
        Collections.sort(chatsList, new Comparator<Chat>() {
            @Override
            public int compare(Chat o1, Chat o2) {
                return o2.compareTo(o1);
            }
        });

        // Set adapter for chats list view
        lvChats = (ListView) findViewById(R.id.lvChats);
        TextView messagesTitle = (TextView) findViewById(R.id.tvMessageTitle);
        ChatsAdapter chatsAdapter = new ChatsAdapter(this, chatsList, messagesTitle);
        lvChats.setAdapter(chatsAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_conversation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent iiMPreferenceIntent = new Intent(this, iiMPreferenceActivity.class);
            startActivityForResult(iiMPreferenceIntent, SETTINGS_RESULT);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void stopRefreshing() {
        if (swipeContainer.isRefreshing()) swipeContainer.setRefreshing(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "Resuming ChatsActivity");
        reloadChatListAndAdapter();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "Destroying activity");
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "Pausing activity");
        super.onPause();
    }
}
