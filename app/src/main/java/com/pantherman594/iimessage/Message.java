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

public class Message implements Comparable<Message> {
    private long id;
    private String msg;
    private long date;
    private String sender;
    private MessageStatus messageStatus;
    private MessageType messageType;
    private boolean read;
    private String chatId;
    private String chatName; // will only be used if chat doesn't yet exist

    Message(String msg, String chatId, String chatName) {
        this.id = 0;
        this.msg = msg;
        this.date = Constants.getNowEpochSeconds();
        this.sender = Constants.ME;
        this.messageStatus = MessageStatus.IN_PROGRESS;
        this.messageType = MessageType.SENT;
        this.read = true;
        this.chatId = chatId;
        this.chatName = chatName;
    }

    Message(long id, String msg, long date, String sender, int isSent, int isFromMe, int isRead, String chatId, String chatName) {
        this(id, msg, date, sender, MessageStatus.SUCCESSFUL, MessageType.SENT, isRead == 1, chatId, chatName);
        if (isSent == 0) {
            messageStatus = MessageStatus.UNSUCCESSFUL;
        }

        if (isFromMe == 0) {
            messageType = MessageType.RECEIVED;
        }
    }

    private Message(long id, String msg, long date, String sender, MessageStatus messageStatus, MessageType messageType, boolean read, String chatId, String chatName) {
        this.id = id;
        this.msg = msg;
        this.date = date;
        this.sender = sender;
        this.messageStatus = messageStatus;
        this.messageType = messageType;
        this.read = read;
        this.chatId = chatId;
        this.chatName = chatName;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getDate() {
        return date;
    }

    void setDate(long date) {
        this.date = date;
    }

    String getMsg() {
        return msg;
    }

    String getSender() {
        if (messageType == MessageType.SENT) return Constants.ME;
        return sender;
    }

    MessageType getMessageType() {
        return messageType;
    }

    MessageStatus getMessageStatus() {
        return messageStatus;
    }

    void setMessageStatus(MessageStatus messageStatus) {
        this.messageStatus = messageStatus;
    }

    boolean isRead() {
        return read || messageType == MessageType.SENT;
    }

    void setRead(boolean read) {
        setReadVar(read);
        if (read) {
            iiMessageApplication.getInstance().getServerBridge().markAsRead(this);
        }
    }

    void setReadVar(boolean read) {
        this.read = read;
    }

    String getChatId() {
        return chatId;
    }

    String getChatName() {
        if (chatName.equals("")) return sender;
        return chatName;
    }

    Chat getChat() {
        return iiMessageApplication.getInstance().getChats().get(chatId);
    }

    @Override
    public int compareTo(Message m) {
        return (int) (id - m.getId());
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", date=" + date +
                ", msg='" + msg + '\'' +
                ", sender='" + sender + '\'' +
                ", messageType=" + messageType +
                ", messageStatus=" + messageStatus +
                ", chatId='" + chatId + '\'' +
                '}';
    }
}
