package com.balloon.net;

import com.balloon.game.VersusGameRules;

import java.io.*;
import java.net.Socket;

/**
 * VersusGameRoom
 * - P1, P2 두 명이 붙는 듀얼 게임 방
 * - 듀얼 요구사항:
 *   - 타이머/시간으로 게임이 끝나지 않는다.
 *   - 둘 중 먼저 풍선 다 터트리고 FINISH를 보내는 사람이 이김.
 *   - 둘 다 거의 동시에 FINISH면 무승부.
 */
public class VersusGameRoom implements Runnable {

    private static final int INITIAL_TIME_SECONDS = 60;     // 지금은 승패에 영향 X (표시/로그용)
    private static final int SCORE_PER_POP = 10;            // 정답 한 번당 점수

    private final PlayerConn p1;
    private final PlayerConn p2;

    // 서버 쪽에서는 점수/정확도 기록용으로만 사용 (승패 판정에는 사용하지 않음)
    private final VersusGameRules rules =
            new VersusGameRules(INITIAL_TIME_SECONDS);

    private volatile boolean running = true;

    // FINISH 여부
    private volatile boolean p1Finished = false;
    private volatile boolean p2Finished = false;
    private volatile boolean resultSent = false;

    public VersusGameRoom(Socket s1, Socket s2,
                          String nickname1, String nickname2) throws IOException {
        this.p1 = new PlayerConn(s1, "P1", nickname1);
        this.p2 = new PlayerConn(s2, "P2", nickname2);
    }

    @Override
    public void run() {
        try {
            // 1) ROLE 전송
            p1.sendLine("ROLE P1");
            p2.sendLine("ROLE P2");

            // 필요하면 닉네임도 같이 보내고 싶으면
            // p1.sendLine("NAME " + p1.nickname);
            // p2.sendLine("NAME " + p2.nickname);

            // 2) 게임 시작 알림
            broadcast("START");


            // 3) 클라이언트 수신 루프 (각 플레이어 하나씩 스레드)
            Thread t1 = new Thread(() -> listenLoop(p1, 1));
            Thread t2 = new Thread(() -> listenLoop(p2, 2));
            t1.start();
            t2.start();

            // 방이 끝날 때까지 대기
            t1.join();
            t2.join();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    /** 한 플레이어 수신 루프 */
    private void listenLoop(PlayerConn pc, int playerIndex) {
        try {
            String line;
            while (running && (line = pc.readLine()) != null) {
                String msg = line.trim();
                System.out.println("[SERVER][" + pc.role + "] << " + msg);

                if (msg.startsWith("POP ")) {
                    // 클라에서 "POP hello" 형식으로 보낸다고 가정
                    String word = msg.substring(4).trim();
                    handlePop(playerIndex, pc.role, word);

                } else if (msg.equals("FINISH")) {
                    // 클라가 "다 터트렸다"를 알리는 메시지
                    handleFinish(playerIndex);

                } else if (msg.equals("RETRY")) {
                    // 나중에 재시작 로직 넣고 싶으면 여기서 처리
                    // (현재는 무시)
                }
            }
        } catch (IOException e) {
            System.out.println("[SERVER] " + pc.role + " disconnected: " + e.getMessage());
        } finally {
            running = false;
        }
    }

    /** POP 처리: 룰 + 브로드캐스트 */
    private void handlePop(int playerIndex, String role, String word) {
        if (!running) return;

        // allCleared는 서버가 직접 몰라서 false로 둔다.
        // (실제 allCleared는 클라 쪽에서 판단하고 FINISH를 보내게 함)
        boolean allCleared = false;

        // 점수/정확도 기록용
        rules.onPop(playerIndex, SCORE_PER_POP, allCleared);

        // 다른 쪽 클라이언트에게도 POP 알림
        // → VersusGamePanel.networkLoop()에서 "POP P1 hello" 형태로 처리 중
        broadcast("POP " + role + " " + word);

    }

    /** 클라이언트가 FINISH를 보냈을 때 처리 */
    private synchronized void handleFinish(int playerIndex) {
        if (!running || resultSent) return;

        if (playerIndex == 1) {
            p1Finished = true;
        } else if (playerIndex == 2) {
            p2Finished = true;
        }

        VersusGameRules.Winner w = VersusGameRules.Winner.NONE;

        if (p1Finished && p2Finished) {
            // 두 명 모두 FINISH → 무승부
            w = VersusGameRules.Winner.DRAW;
        } else if (p1Finished && !p2Finished) {
            // P1이 먼저 FINISH
            w = VersusGameRules.Winner.P1;
        } else if (!p1Finished && p2Finished) {
            // P2가 먼저 FINISH
            w = VersusGameRules.Winner.P2;
        } else {
            // 아직 한 명만도 FINISH 안 한 상태 → 아무것도 하지 않음
            return;
        }

        resultSent = true;
        sendResultAndStop(w);
    }

    /** 승패 결과를 클라이언트에 보내고 방 종료 */
    private void sendResultAndStop(VersusGameRules.Winner w) {
        if (!running) return;
        running = false;

        String msgP1;
        String msgP2;

        switch (w) {
            case P1 -> {
                msgP1 = "RESULT WIN";
                msgP2 = "RESULT LOSE";
            }
            case P2 -> {
                msgP1 = "RESULT LOSE";
                msgP2 = "RESULT WIN";
            }
            case DRAW -> {
                msgP1 = "RESULT DRAW";
                msgP2 = "RESULT DRAW";
            }
            default -> {
                msgP1 = "RESULT DRAW";
                msgP2 = "RESULT DRAW";
            }
        }

        p1.sendLine(msgP1);
        p2.sendLine(msgP2);

        shutdown();
    }

    private void broadcast(String line) {
        p1.sendLine(line);
        p2.sendLine(line);
    }

    private void shutdown() {
        running = false;
        try { p1.close(); } catch (Exception ignore) {}
        try { p2.close(); } catch (Exception ignore) {}
    }

    // --- 내부용 플레이어 연결 래퍼 ---
    private static class PlayerConn {
        final Socket socket;
        final String role;       // "P1" / "P2"
        final String nickname;   // 그냥 참고용
        final BufferedReader in;
        final BufferedWriter out;

        PlayerConn(Socket socket, String role, String nickname) throws IOException {
            this.socket = socket;
            this.role = role;
            this.nickname = nickname;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
        }

        String readLine() throws IOException {
            return in.readLine();
        }

        synchronized void sendLine(String line) {
            try {
                out.write(line);
                out.write("\n");
                out.flush();
                System.out.println("[SERVER][" + role + "] >> " + line);
            } catch (IOException e) {
                System.out.println("[SERVER][" + role + "] send error: " + e.getMessage());
            }
        }

        void close() {
            try { in.close(); } catch (Exception ignore) {}
            try { out.close(); } catch (Exception ignore) {}
            try { socket.close(); } catch (Exception ignore) {}
        }
    }
}
