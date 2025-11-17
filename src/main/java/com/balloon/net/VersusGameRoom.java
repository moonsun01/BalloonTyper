package com.balloon.net;

import com.balloon.game.VersusGameRules;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;

/**
 * VersusGameRoom
 * - P1, P2 두 명이 붙는 듀얼 게임 방
 * - VersusGameRules를 사용해 시간/승패/무승부를 판정하고
 *   POP/RESULT 메시지를 클라이언트와 주고받는다.
 */
public class VersusGameRoom implements Runnable {

    private static final int INITIAL_TIME_SECONDS = 60;     // 시작 시간(난이도 조절 포인트)
    private static final int SCORE_PER_POP = 10;            // 정답 한 번당 점수

    private final PlayerConn p1;
    private final PlayerConn p2;

    private final VersusGameRules rules =
            new VersusGameRules(INITIAL_TIME_SECONDS);

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    private volatile boolean running = true;

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

            // 3) 1초마다 시간 감소 + 승패 판정
            scheduler.scheduleAtFixedRate(this::onTick, 1, 1, TimeUnit.SECONDS);

            // 4) 클라이언트 수신 루프 (각 플레이어 하나씩 스레드)
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

    /** 1초마다 호출되는 타이머 콜백 */
    private void onTick() {
        if (!running) return;

        rules.onTick();

        VersusGameRules.Winner w = rules.getWinner();
        if (w != VersusGameRules.Winner.NONE) {
            // 승패 확정 → RESULT 전송 후 방 종료
            sendResultAndStop(w);
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
                    // 클라가 "다 터트렸다"를 보내는 경우 (옵션)
                    // TODO: 여기서 allCleared = true 로 rules.onPop()을 호출하는 식으로 확장 가능

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

        // TODO: allCleared는 나중에 '풍선 다 터진 상태'로 바꿀 것
        boolean allCleared = false;

        // 룰에 반영 (점수/정확도/올클리어 등)
        rules.onPop(playerIndex, SCORE_PER_POP, allCleared);

        // 다른 쪽 클라이언트에게도 POP 알림
        // → VersusGamePanel.networkLoop()에서 "POP P1 hello" 형태로 처리 중
        broadcast("POP " + role + " " + word);

        // POP으로 인해 즉시 승패가 결정됐을 수도 있으므로 한 번 체크
        VersusGameRules.Winner w = rules.getWinner();
        if (w != VersusGameRules.Winner.NONE) {
            sendResultAndStop(w);
        }
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
        scheduler.shutdownNow();
        p1.close();
        p2.close();
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
