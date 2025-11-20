package com.balloon.net;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

/**
 * 2인용 듀얼 서버 (멀티 게임 버전)
 *
 * - 서버는 한 번만 실행해 두고, 여러 판을 연속으로 진행할 수 있음
 * - 흐름
 *   1) P1, P2 접속 대기
 *   2) JOIN 수신 후 ROLE 전송
 *   3) runGameRounds() 안에서:
 *      - START 전송
 *      - POP / FINISH / BLIND / REVERSE / TOAST 처리
 *      - RESULT 전송
 *      - 두 플레이어가 RETRY → 새 라운드
 *      - 누군가 EXIT → 현재 게임 종료, 소켓 정리 후 다음 P1/P2 대기
 */
public class VersusServer {

    // 플레이어 소켓 래퍼
    private static class Player {
        Socket socket;
        BufferedReader in;
        PrintWriter out;
        String nickname;
        String role; // "P1" 또는 "P2"

        Player(Socket socket, String role) throws IOException {
            this.socket = socket;
            this.role = role;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 5555;
        System.out.println("[SERVER] Versus 서버 시작. 포트: " + port);

        try (ServerSocket server = new ServerSocket(port)) {

            // ★ 새 P1/P2 쌍을 계속 받는 상위 루프
            while (true) {
                System.out.println("[SERVER] === 새로운 게임 대기 중 ===");
                System.out.println("[SERVER] P1 기다리는 중...");
                Player p1 = new Player(server.accept(), "P1");
                System.out.println("[SERVER] P1 접속!");

                System.out.println("[SERVER] P2 기다리는 중...");
                Player p2 = new Player(server.accept(), "P2");
                System.out.println("[SERVER] P2 접속!");

                // JOIN 메시지 수신
                readJoin(p1);
                readJoin(p2);

                // ROLE 한 번만 전송
                p1.out.println("ROLE P1");
                p2.out.println("ROLE P2");

                // 이 쌍(P1, P2)에 대해 여러 라운드 진행
                try {
                    runGameRounds(p1, p2);
                } catch (Exception e) {
                    System.out.println("[SERVER] runGameRounds 중 예외: " + e.getMessage());
                    e.printStackTrace();
                }

                // 소켓 정리
                try { p1.socket.close(); } catch (Exception ignore) {}
                try { p2.socket.close(); } catch (Exception ignore) {}

                System.out.println("[SERVER] 현재 게임 종료. 다음 플레이어를 기다립니다.");
                // while(true) 이므로 다음 P1/P2 접속을 또 기다리게 됨
            }
        }
    }

    /**
     * 한 P1/P2 쌍에 대해 여러 라운드를 진행하는 메서드.
     * EXIT가 들어오면 keepPlaying=false 되어 함수가 리턴된다.
     */
    private static void runGameRounds(Player p1, Player p2) throws Exception {

        boolean keepPlaying = true;

        // ====== 여러 라운드를 돌리는 메인 루프 ======
        while (keepPlaying) {

            // 1) 라운드 시작 알림
            broadcast(p1, p2, "START");
            System.out.println("[SERVER] === NEW ROUND START ===");

            // 2) 라운드 메인 루프: POP / FINISH / BLIND / REVERSE 처리
            Set<String> poppedWords = new HashSet<>();
            boolean finished = false;
            String winnerRole = null;

            while (!finished) {

                // ----- P1 처리 -----
                if (p1.in.ready()) {
                    String line = p1.in.readLine();
                    if (line == null) {   // 클라 끊김
                        keepPlaying = false;
                        finished = true;
                        winnerRole = null;
                        break;
                    }
                    line = line.trim();
                    System.out.println("[SERVER][P1] << " + line);

                    if (line.startsWith("POP ")) {
                        String word = line.substring(4).trim();
                        if (poppedWords.add(word)) {
                            broadcast(p1, p2, "POP P1 " + word);
                        }

                    } else if (line.equals("FINISH")) {
                        if (!finished) {
                            winnerRole = "P1";
                            finished = true;
                        } else if ("P2".equals(winnerRole)) {
                            // 거의 동시에 FINISH → 무승부
                            winnerRole = "DRAW";
                        }

                    } else if (line.startsWith("REVERSE ")) {
                        // REVERSE P2 5000 같은 메시지를 그대로 양쪽에 전달
                        broadcast(p1, p2, line);

                    } else if (line.equals("BLIND")) {
                        // P1이 BLIND 사용 → 두 클라에 "BLIND P2" 전송
                        broadcast(p1, p2, "BLIND P2");

                    } else if (line.equals("EXIT")) {
                        keepPlaying = false;
                        finished = true;
                        winnerRole = null;
                        break;

                    } else if (line.startsWith("TOAST")) {
                        broadcast(p1, p2, line);
                    }
                }

                // ----- P2 처리 -----
                if (p2.in.ready()) {
                    String line = p2.in.readLine();
                    if (line == null) {
                        keepPlaying = false;
                        finished = true;
                        winnerRole = null;
                        break;
                    }
                    line = line.trim();
                    System.out.println("[SERVER][P2] << " + line);

                    if (line.startsWith("POP ")) {
                        String word = line.substring(4).trim();
                        if (poppedWords.add(word)) {
                            broadcast(p1, p2, "POP P2 " + word);
                        }

                    } else if (line.equals("FINISH")) {
                        if (!finished) {
                            winnerRole = "P2";
                            finished = true;
                        } else if ("P1".equals(winnerRole)) {
                            // 거의 동시에 FINISH → 무승부
                            winnerRole = "DRAW";
                        }

                    } else if (line.startsWith("REVERSE ")) {
                        broadcast(p1, p2, line);

                    } else if (line.equals("BLIND")) {
                        // P2가 BLIND 사용 → 두 클라에 "BLIND P1" 전송
                        broadcast(p1, p2, "BLIND P1");

                    } else if (line.equals("EXIT")) {
                        keepPlaying = false;
                        finished = true;
                        winnerRole = null;
                        break;

                    } else if (line.startsWith("TOAST")) {
                        broadcast(p1, p2, line);
                    }
                }

                Thread.sleep(10);
            }

            // 클라이언트 종료/에러로 게임 종료된 경우
            if (!keepPlaying || winnerRole == null) {
                System.out.println("[SERVER] 클라이언트 종료/에러로 라운드 종료");
                break;
            }

            // 3) RESULT 전송
            if ("P1".equals(winnerRole)) {
                p1.out.println("RESULT WIN");
                p2.out.println("RESULT LOSE");
            } else if ("P2".equals(winnerRole)) {
                p1.out.println("RESULT LOSE");
                p2.out.println("RESULT WIN");
            } else { // DRAW
                broadcast(p1, p2, "RESULT DRAW");
            }

            System.out.println("[SERVER] ROUND END. winner = " + winnerRole);

            // 4) 양쪽이 RETRY 할지, 그만둘지 확인
            boolean p1Retry = false;
            boolean p2Retry = false;

            System.out.println("[SERVER] WAITING FOR RETRY FROM BOTH...");

            while (true) {
                // 둘 다 RETRY면 새 라운드
                if (p1Retry && p2Retry) {
                    System.out.println("[SERVER] BOTH RETRY. NEXT ROUND.");
                    break;
                }

                // P1 쪽 메시지
                if (p1.in.ready()) {
                    String line = p1.in.readLine();
                    if (line == null) {
                        keepPlaying = false;
                        break;
                    }
                    line = line.trim();
                    System.out.println("[SERVER][P1] (post-result) << " + line);

                    if (line.equals("RETRY")) {
                        p1Retry = true;
                    } else if (line.equals("EXIT")) {
                        keepPlaying = false;
                        break;
                    }
                }

                // P2 쪽 메시지
                if (p2.in.ready()) {
                    String line = p2.in.readLine();
                    if (line == null) {
                        keepPlaying = false;
                        break;
                    }
                    line = line.trim();
                    System.out.println("[SERVER][P2] (post-result) << " + line);

                    if (line.equals("RETRY")) {
                        p2Retry = true;
                    } else if (line.equals("EXIT")) {
                        keepPlaying = false;
                        break;
                    }
                }

                Thread.sleep(10);
            }

            if (!keepPlaying) {
                System.out.println("[SERVER] RETRY 안 함 / 종료 요청. 게임 루프 종료.");
                break;
            }

            // 여기까지 오면 p1Retry && p2Retry == true
            // → while(keepPlaying) 루프의 다음 반복에서 새 라운드 시작
        }
    }

    // JOIN 메시지 읽기
    private static void readJoin(Player p) throws IOException {
        String line = p.in.readLine();
        if (line != null && line.startsWith("JOIN ")) {
            p.nickname = line.substring(5).trim();
            System.out.println("[SERVER] " + p.role + " = " + p.nickname);
        } else {
            System.out.println("[SERVER] " + p.role + " JOIN 메시지 이상함: " + line);
        }
    }

    // 양쪽에 브로드캐스트
    private static void broadcast(Player p1, Player p2, String msg) {
        p1.out.println(msg);
        p2.out.println(msg);
        System.out.println("[SERVER] >> " + msg);
    }
}
