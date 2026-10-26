package com.qwerty.homenet.intercom;

public enum CallState {
    IDLE,
    RINGING,
    CONNECTED,
    DIALING;

    public static CallState byId(int id) {
        CallState[] v = values();
        return id >= 0 && id < v.length ? v[id] : IDLE;
    }
}
