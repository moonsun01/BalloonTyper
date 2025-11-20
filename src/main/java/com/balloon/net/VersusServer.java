package com.balloon.net;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

/**
 * 2인용 듀얼 서버 (멀티 라운드 버전)
 * - P1, P2 접속
 * - JOIN 처리 후 ROLE, START 전송
 * - 한 라운드 진행: POP / FINISH
 * - RESULT 전송
 * - 양쪽이 RETRY 보내면 새 라운드 다시 START
 * - 누군가 끊기거나 EXIT 보내면 서버 종료
 */
public class VersusServer {

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

            // 1) P1 접속
            System.out.println("[SERVER] P1 기다리는 중...");
            Player p1 = new Player(server.accept(), "P1");
            System.out.println("[SERVER] P1 접속!");

            // 2) P2 접속
            System.out.println("[SERVER] P2 기다리는 중...");
            Player p2 = new Player(server.accept(), "P2");
            System.out.println("[SERVER] P2 접속!");

            // 3) JOIN 메시지 받기
            readJoin(p1);
            readJoin(p2);

            // ROLE 알려주기 (한 번만)
            p1.out.println("ROLE P1");
            p2.out.println("ROLE P2");

            boolean keepPlaying = true;

            // ====== 여러 라운드를 돌리는 메인 루프 ======
            while (keepPlaying) {

                // 4) 라운드 시작 알림
                broadcast(p1, p2, "START");
                System.out.println("[SERVER] === NEW ROUND START ===");

                // 라운드 상태
                Set<String> poppedWords = new HashSet<>();
                boolean finished = false;
                String winnerRole = null;

                // 5) 라운드 메인 루프: POP / FINISH 처리
                while (!finished) {

                    // P1 처리
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
                            } else if ("P2".equals(winnerRole)) {
                                // 거의 동시에 FINISH → 무승부
                                winnerRole = "DRAW";
                            }
                            finished = true;
                        } else if (line.equals("EXIT")) {
                            keepPlaying = false;
                            finished = true;
                            winnerRole = null;
                            break;
                        } else if (line.startsWith("TOAST")) {
                            broadcast(p1, p2, line);
                        }
                    }

                    // P2 처리
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
                            } else if ("P1".equals(winnerRole)) {
                                winnerRole = "DRAW";
                            }
                            finished = true;
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

                // 라운드가 정상 종료 안 된 경우(끊김 등)
                if (!keepPlaying || winnerRole == null) {
                    System.out.println("[SERVER] 클라이언트 종료/에러로 게임 종료");
                    break;
                }

                // 6) RESULT 전송
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

                // 7) 양쪽이 RETRY 할지, 그만둘지 확인
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
                    System.out.println("[SERVER] RETRY 안 함 / 종료 요청. 서버 종료.");
                    break;
                }

                // 여기까지 왔으면 p1Retry && p2Retry == true
                // → while(keepPlaying) 돌면서 새 라운드 시작
            }

            // 소켓 정리
            try { p1.socket.close(); } catch (Exception ignore) {}
            try { p2.socket.close(); } catch (Exception ignore) {}
        }
    }

    private static void readJoin(Player p) throws IOException {
        String line = p.in.readLine();
        if (line != null && line.startsWith("JOIN ")) {
            p.nickname = line.substring(5).trim();
            System.out.println("[SERVER] " + p.role + " = " + p.nickname);
        } else {
            System.out.println("[SERVER] " + p.role + " JOIN 메시지 이상함: " + line);
        }
    }

    private static void broadcast(Player p1, Player p2, String msg) {
        p1.out.println(msg);
        p2.out.println(msg);
        System.out.println("[SERVER] >> " + msg);
    }
}
