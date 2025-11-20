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

    /** 단어 POP 전송 */
    public void sendPop(String word) {
        out.println("POP " + word);
    }

    /** 라운드 종료 통보 */
    public void sendFinish() {
        out.println("FINISH");
    }

    /** 서버에서 한 줄 읽기 */
    public String readLine() throws IOException {
        return in.readLine();
    }

    /** 소켓 닫기 */
    public void close() throws IOException {
        socket.close();
    }

    /** 클라이언트에서 RETRY 요청 보낼 때 사용 */
    public void sendRetry() {
        out.println("RETRY");
    }

    /**
     * 아이템 토스트를 서버로 전송
     * flag: "1" = 좋은 효과, "0" = 나쁜 효과
     * msg:  토스트에 표시할 문자열
     *
     * 예) TOAST 1 내 풍선 +2!
     */
    public void sendToast(String flag, String msg) {
        out.println("TOAST " + flag + " " + msg);
    }

    /**
     * REVERSE 아이템 발동을 서버로 전송
     * targetRole: "P1" 또는 "P2"
     * durationMillis: reverse 지속 시간(ms)
     *
     * 예) REVERSE P2 5000
     */
    public void sendReverse(String targetRole, long durationMillis) {
        out.println("REVERSE " + targetRole + " " + durationMillis);
    }

    //  BLIND 아이템 사용 전송 (ROLE 포함)
    public void sendBlind() {
        out.println("BLIND");  // 예: "BLIND P1"
    }
}
