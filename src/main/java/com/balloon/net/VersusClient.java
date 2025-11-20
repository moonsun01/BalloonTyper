package com.balloon.net;

import java.io.*;
import java.net.Socket;

public class VersusClient {

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;

    public VersusClient(String hostIp, int port, String nickname) throws IOException {
        this.socket = new Socket(hostIp, port);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);

        // 접속 후 바로 내 닉네임 전송
        out.println("JOIN " + nickname);
    }

    public void sendPop(String word) {
        out.println("POP " + word);
    }

    public void sendFinish() {
        out.println("FINISH");
    }

    public String readLine() throws IOException {
        return in.readLine();
    }

    public void close() throws IOException {
        socket.close();
    }

    // 🔥 여기만 수정
    public void sendRetry() {
        out.println("RETRY");   // 그냥 한 줄 보내면 됨
    }

    // VersusClient.java 안에 추가
    public void sendReverse(String targetRole, long durationMillis) {
        out.println("REVERSE " + targetRole + " " + durationMillis);
    }

}
