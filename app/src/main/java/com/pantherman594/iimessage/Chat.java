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

import java.util.TreeMap;
import java.util.TreeSet;

class Chat implements Comparable<Chat> {
    private String id;
    private String name;
    private TreeMap<Long, Message> messages;

    Chat(String id, String name) {
        this.id = id;
        this.name = name;
        this.messages = new TreeMap<>();
    }

    String getId() {
        return id;
    }

    String getName() {
        if (name.equals("")) {
            if (messages.isEmpty()) return id.substring(11);
            else return getLastMessage().getSender();
        }
        return name;
    }

    TreeSet<Message> getMessages() {
        return new TreeSet<>(messages.values());
    }

    Message getLastMessage() {
        return messages.lastEntry().getValue();
    }

    void addMessage(long messageId, Message message) {
        this.messages.put(messageId, message);
    }

    void removeMessage(long messageId) {
        this.messages.remove(messageId);
    }

    public void setMessages(TreeMap<Long, Message> messages) {
        this.messages = messages;
    }

    TreeSet<Message> getUnreadMessages() {
        TreeSet<Message> messages = new TreeSet<>();
        for (Message message : this.messages.values()) {
            if (!message.isRead()) {
                messages.add(message);
            }
        }
        return messages;
    }

    int getNumUnread() {
        return getUnreadMessages().size();
    }

    boolean shouldUpdate(Chat newChat) {
        return id.equals(newChat.getId()) && name.equals(newChat.getName());
    }

    @Override
    public int compareTo(Chat chat) {
        return getLastMessage().compareTo(chat.getLastMessage());
    }
}
