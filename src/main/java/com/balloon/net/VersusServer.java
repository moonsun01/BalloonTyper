package com.balloon.net;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

/**
 * 아주 단순한 2인용 서버 뼈대
 * - P1, P2 접속 대기
 * - 두 명 모두 JOIN 하면 START
 * - POP / FINISH 처리해서 양쪽에 브로드캐스트
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

            // 3) 플레이어들의 JOIN 메시지 받기
            readJoin(p1);
            readJoin(p2);

            // 역할 알려주기
            p1.out.println("ROLE P1");
            p2.out.println("ROLE P2");

            // 4) START 브로드캐스트
            broadcast(p1, p2, "START");

            // 5) 게임 상태
            Set<String> poppedWords = new HashSet<>();
            boolean finished = false;
            String winnerRole = null;

            // 6) 메인 루프: 두 플레이어의 메시지 처리
            while (!finished) {
                // P1 처리
                if (p1.in.ready()) {
                    String line = p1.in.readLine();
                    if (line == null) break;
                    if (line.startsWith("POP ")) {
                        String word = line.substring(4).trim();
                        if (poppedWords.add(word)) {
                            broadcast(p1, p2, "POP P1 " + word);
                        }
                    } else if (line.equals("FINISH")) {
                        winnerRole = "P1";
                        finished = true;
                    }
                }

                // P2 처리
                if (p2.in.ready()) {
                    String line = p2.in.readLine();
                    if (line == null) break;
                    if (line.startsWith("POP ")) {
                        String word = line.substring(4).trim();
                        if (poppedWords.add(word)) {
                            broadcast(p1, p2, "POP P2 " + word);
                        }
                    } else if (line.equals("FINISH")) {
                        if (!finished) {
                            winnerRole = "P2";
                        } else if ("P1".equals(winnerRole)) {
                            // 이미 P1이 FINISH 했다면 동시 FINISH → 무승부 처리 가능
                            winnerRole = "DRAW";
                        }
                        finished = true;
                    }
                }

                // 너무 CPU 안 쓰게 잠깐 쉼
                Thread.sleep(10);
            }

            // 7) 결과 전송
            if ("P1".equals(winnerRole)) {
                p1.out.println("RESULT WIN");
                p2.out.println("RESULT LOSE");
            } else if ("P2".equals(winnerRole)) {
                p1.out.println("RESULT LOSE");
                p2.out.println("RESULT WIN");
            } else {
                // 동시 Finish 등
                broadcast(p1, p2, "RESULT DRAW");
            }

            System.out.println("[SERVER] 게임 종료. winner = " + winnerRole);

            p1.socket.close();
            p2.socket.close();
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
