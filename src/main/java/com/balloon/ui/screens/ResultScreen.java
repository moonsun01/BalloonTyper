package com.balloon.ui.screens;                                      // 결과 화면 패키지

import com.balloon.core.ScreenId;                                    // 화면 식별자(enum)
import com.balloon.core.ScreenRouter;                                // 라우터
import com.balloon.ranking.RankingCSV;                               // CSV 유틸
import com.balloon.ranking.RankingEntry;                             // DTO
import com.balloon.ui.theme.Theme;                                   // 테마(색/폰트)
import javax.swing.*;                                                // 스윙 컴포넌트
import java.awt.*;                                                   // 레이아웃/색
// 결과 -> 랭킹 저장/이동 화면

public class ResultScreen extends JPanel {                            // 패널 상속
    private final ScreenRouter router;                                // 라우터 참조
    private int score = 0;                                            // 최종 점수
    private double accuracy = 0.0;                                    // 최종 정확도
    private int timeLeft = 0;                                         // 남은 시간(초)

    private final JLabel lScore = new JLabel("Score: 0");             // 점수 라벨
    private final JLabel lAcc   = new JLabel("Accuracy: 0.00%");      // 정확도 라벨
    private final JLabel lTime  = new JLabel("Time Left: 0s");        // 시간 라벨

    public ResultScreen(ScreenRouter router) {                        // 생성자
        this.router = router;                                         // 라우터 저장
        setLayout(new BorderLayout(Theme.gapMD(), Theme.gapMD()));    // 여백 포함 보더 레이아웃
        setBackground(Theme.bgColor());                               // 배경색 적용

        JLabel title = new JLabel("RESULT");                          // 상단 제목
        title.setFont(Theme.h1());                                    // 큰 제목 폰트
        title.setHorizontalAlignment(SwingConstants.CENTER);           // 가운데 정렬
        add(title, BorderLayout.NORTH);                               // 상단 배치

        JPanel center = new JPanel(new GridLayout(3,1,8,8));          // 중간 정보 영역
        center.setOpaque(false);                                      // 부모 배경 사용
        lScore.setFont(Theme.h2());                                   // 점수 폰트
        lAcc.setFont(Theme.h2());                                     // 정확도 폰트
        lTime.setFont(Theme.h2());                                    // 시간 폰트
        center.add(lScore);                                           // 점수 라벨 추가
        center.add(lAcc);                                             // 정확도 라벨 추가
        center.add(lTime);                                            // 시간 라벨 추가
        add(center, BorderLayout.CENTER);                             // 중앙 배치

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER,  // 하단 버튼 영역
                16, 16));                                             // 버튼 간격
        bottom.setOpaque(false);                                      // 투명 처리

        JButton btnSave = new JButton("이름 입력 후 랭킹 저장");          // 저장 버튼
        JButton btnRanking = new JButton("랭킹 보러가기");              // 랭킹 이동 버튼
        btnSave.setFont(Theme.body());                                // 버튼 폰트
        btnRanking.setFont(Theme.body());                             // 버튼 폰트
        btnRanking.setEnabled(false);                                 // 저장 전 비활성화

        bottom.add(btnSave);                                          // 하단에 버튼 추가
        bottom.add(btnRanking);                                       // 하단에 버튼 추가
        add(bottom, BorderLayout.SOUTH);                              // 하단 배치

        btnSave.addActionListener(e -> {                              // 저장 버튼 클릭 시
            String name = JOptionPane.showInputDialog(                //  이름 입력 다이얼로그
                    this,                                             //  부모
                    "이름을 입력하세요(최대 16자):",                       //  메시지
                    "랭킹 저장",                                       //  타이틀
                    JOptionPane.PLAIN_MESSAGE);                       //  입력형
            if (name == null) return;                                 //  취소 시 종료
            name = name.trim();                                       //  공백 제거
            if (name.isEmpty()) {                                     //  빈 문자열 방지
                JOptionPane.showMessageDialog(this,                   //   경고
                        "이름이 비어 있습니다.", "오류",
                        JOptionPane.ERROR_MESSAGE);
                return;                                               //   종료
            }
            if (name.length() > 16) name = name.substring(0,16);      //  길이 제한

            RankingEntry entry = new RankingEntry(                     //  DTO 생성
                    name,                                              //   이름
                    score,                                             //   점수
                    accuracy,                                          //   정확도
                    timeLeft,                                          //   남은 시간
                    System.currentTimeMillis()                         //   현재 시각
            );                                                         //  DTO 끝
            RankingCSV.append(entry);                                  //  CSV에 추가
            System.out.println("[RankingCSV] saved: " + entry.name           //   (로그)
                    + ", score=" + entry.score + ", acc=" + entry.accuracy);

            JOptionPane.showMessageDialog(this,                               // 4) 저장 완료 안내
                    "랭킹에 저장되었습니다!",
                    "완료",
                    JOptionPane.INFORMATION_MESSAGE);

            btnRanking.setEnabled(true);                                      // 5) 버튼도 활성화(보조)
            router.show(ScreenId.RANKING);                                //  이동 버튼 활성화
        });                                                            // 저장 리스너 끝

        btnRanking.addActionListener(e ->                              // 랭킹 이동 버튼
                router.show(ScreenId.RANKING));                        //  랭킹 화면으로 전환
    }                                                                  // 생성자 끝

    public void setResult(int score, double accuracy, int timeLeft) {  // 외부에서 결과 주입
        this.score = score;                                            // 점수 저장
        this.accuracy = accuracy;                                      // 정확도 저장
        this.timeLeft = timeLeft;                                      // 남은 시간 저장
        lScore.setText("Score: " + score);                             // 라벨 반영
        lAcc.setText(String.format("Accuracy: %.2f%%", accuracy));     // 라벨 반영
        lTime.setText("Time Left: " + timeLeft + "s");                 // 라벨 반영
    }                                                                  // setResult 끝
}                                                                      // 클래스 끝
