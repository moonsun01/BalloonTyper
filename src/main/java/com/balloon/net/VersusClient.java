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

    // 풍선 POP 전송
    public void sendPop(String word) {
        out.println("POP " + word);
    }

    // 라운드 종료 전송
    public void sendFinish() {
        out.println("FINISH");
    }

    // 서버에서 한 줄 메시지 받기
    public String readLine() throws IOException {
        return in.readLine();
    }

    // 소켓 닫기
    public void close() throws IOException {
        socket.close();
    }

    // 라운드 재시작 요청
    public void sendRetry() {
        out.println("RETRY");
    }

    // 🔥 BLIND 아이템 사용 전송 (ROLE 포함)
    public void sendBlind() {
        out.println("BLIND");  // 예: "BLIND P1"
    }

}
