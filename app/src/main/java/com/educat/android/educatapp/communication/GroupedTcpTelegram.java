package com.educat.android.educatapp.communication;

import com.xuhao.didi.core.iocore.interfaces.ISendable;

import java.util.ArrayList;

public class GroupedTcpTelegram implements ISendable {

    private ArrayList<byte[]> bytesArrayList;
    private int size;

    public GroupedTcpTelegram() {
        this.bytesArrayList = new ArrayList<>();
        this.size = 0;
    }

    public ArrayList<byte[]> getBytesArrayList() {
        return bytesArrayList;
    }

    public void setBytesArrayList(ArrayList<byte[]> bytesArrayList) {
        this.bytesArrayList = bytesArrayList;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int addTcpTelegram(TcpTelegram tcpTelegram) {
        this.bytesArrayList.add(tcpTelegram.parse());
        this.size++;
        return size;
    }

    @Override
    public byte[] parse() {
        int totalLength = 0;
        for (byte[] bytes : bytesArrayList) {
            totalLength += bytes.length;
        }
        byte[] mergedBytes = new byte[totalLength];
        int destPos = 0;
        for (byte[] bytes : bytesArrayList) {
            System.arraycopy(bytes, 0, mergedBytes, destPos, bytes.length);
            destPos += bytes.length;
        }
        return mergedBytes;
    }
}
