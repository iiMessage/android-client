package com.pantherman594.iimessage;

public enum MessageType {
    RECEIVED(0),
    SENT(1);

    private int messageNum;

    MessageType(int messageNum) {
        this.messageNum = messageNum;
    }

    public int getVal() {
        return this.messageNum;
    }
}

