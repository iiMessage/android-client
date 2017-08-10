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
