package com.pantherman594.iimessage;

public enum MessageStatus {

    UNSUCCESSFUL(0),
    SUCCESSFUL(1),
    IN_PROGRESS(3);

    private int messageNum;

    MessageStatus(int messageNum) {
        this.messageNum = messageNum;
    }

    public int getVal() {
        return this.messageNum;
    }
}
