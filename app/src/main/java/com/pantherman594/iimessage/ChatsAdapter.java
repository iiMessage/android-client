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

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ChatsAdapter extends ArrayAdapter<Chat> {
    private static final String TAG = ChatsAdapter.class.getSimpleName();
    private Context context;
    private TextView messagesTitle;
    private int numUnread = 0;

    ChatsAdapter(Context context, ArrayList<Chat> chats, TextView messagesTitle) {
        super(context, 0, chats);
        this.context = context;
        this.messagesTitle = messagesTitle;
        messagesTitle.setText(context.getString(R.string.messages));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.i(TAG, "Hitting ChatsAdapter to insert chat");

        Chat chat = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_chat, parent, false);
        }

        TextView tvChatName = (TextView) convertView.findViewById(R.id.tvChatName);
        TextView tvLastMessage = (TextView) convertView.findViewById(R.id.tvLastMessage);
        TextView tvLastDate = (TextView) convertView.findViewById(R.id.tvLastDate);
        View vUnreadCircle = convertView.findViewById(R.id.vUnreadCircle);

        String name = chat.getName();
        String lastMessageText = chat.getLastMessage().getMsg();
        if (chat.getLastMessage().getMessageType() == MessageType.RECEIVED) {
            lastMessageText = chat.getLastMessage().getSender() + ": " + lastMessageText;
        }
        tvLastDate.setText(getDateStr(chat.getLastMessage().getDate()));
        tvChatName.setText(name);
        tvLastMessage.setText(lastMessageText);

        if (chat.getNumUnread() > 0) {
            vUnreadCircle.setVisibility(View.VISIBLE);
            messagesTitle.setText(context.getString(R.string.messages) + " (" + ++numUnread + ")");
        }


        return convertView;
    }

    int getNumUnread() {
        return numUnread;
    }

    private String getDateStr(long dateSeconds) {
        long dateDiff = Constants.getNowEpochSeconds() - dateSeconds;
        dateSeconds = dateSeconds * 1000 + Constants.get2001Milliseconds(); // Change to milliseconds since epoch
        Date date = new Date(dateSeconds);

        SimpleDateFormat dateStr = new SimpleDateFormat("M/d/yy", Locale.US);
        SimpleDateFormat dayOfWeek = new SimpleDateFormat("E", Locale.US);
        SimpleDateFormat time = new SimpleDateFormat("h:mm a", Locale.US);
        if (dateDiff > 7*24*60*60) { // > 1 week
            return dateStr.format(date);
        } else if (dateDiff > 2*24*60*60) { // > 2 days
            return dayOfWeek.format(date);
        } else if (dateDiff > 24*60*60) {// 1 day
            return "Yesterday";
        } else {
            return time.format(date);
        }
    }
}
