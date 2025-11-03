// 패키지: 기존 화면들이 있는 ui.screens 하위
package com.balloon.ui.screens;
import com.balloon.ui.screens.GamePanel; //마지막 스테이지 읽기용

// 결과 전달 컨텍스트/데이터, 화면 표시 훅 인터페이스
import com.balloon.core.ResultContext;
import com.balloon.core.ResultData;
import com.balloon.core.Showable;

// 라우팅에 필요
import com.balloon.core.ScreenId;
import com.balloon.core.ScreenRouter;

// 스윙/그래픽 유틸
import javax.swing.*;
import java.awt.*;

// 결과 화면: Router가 이 패널을 CardLayout에 붙여 전환
public class ResultScreen extends JPanel implements Showable {

    // ─────────────────────────[ 로컬 UI 헬퍼 ]────────────────────────
    // 파일 추가 없이 진행하기 위해 Theme 의존 제거 → 내부 헬퍼로 대체
    private static Color bg()  { return new Color(0x0F, 0x12, 0x18); }         // 배경색
    private static Color fg()  { return Color.WHITE; }                         // 글자색
    private static Font  h1()  { return new Font("Dialog", Font.BOLD, 32); }   // 타이틀 폰트
    private static Font  h2()  { return new Font("Dialog", Font.BOLD, 24); }   // 본문 폰트
    private static Font  h3()  { return new Font("Dialog", Font.PLAIN, 18); }  // 보조 폰트
    private static Font  btnF(){ return new Font("Dialog", Font.BOLD, 16); }   // 버튼 폰트
    // ────────────────────────────────────────────────────────────────

    // 라우터(버튼 액션에서 사용)
    private final ScreenRouter router;                                         // ★ 라우터 참조

    // 점수/정확도 등을 표시할 라벨들
    private final JLabel titleLabel  = new JLabel("RESULT");
    private final JLabel scoreLabel  = new JLabel("Score: -");
    private final JLabel timeLabel   = new JLabel("Time Left: -s");
    private final JLabel accLabel    = new JLabel("Accuracy: -");
    private final JLabel detailLabel = new JLabel("Hit: -, Miss: -");

    // 버튼: 다시하기, 메인으로
    private final JButton retryButton = new JButton("RETRY");
    private final JButton menuButton  = new JButton("MAIN MENU");

    // 생성자: UI 컴포넌트 배치 + 라우터 주입
    public ResultScreen(ScreenRouter router) {
        this.router = router;                                                  // ★ 인스턴스 보관

        setLayout(new BorderLayout());                                         // 레이아웃: 상/중앙/하 배치
        setBackground(bg());                                                   // 배경색 적용

        // 상단 타이틀
        titleLabel.setFont(h1());                                              // 폰트: 큰 제목
        titleLabel.setForeground(fg());                                        // 글자색: 밝은색
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);              // 가운데 정렬
        add(titleLabel, BorderLayout.NORTH);                                   // 상단 배치

        // 중앙: 결과 수치들 수직 배치
        JPanel center = new JPanel();                                          // 중앙 패널 생성
        center.setOpaque(false);                                               // 배경 투명
        center.setLayout(new GridLayout(4, 1, 0, 8));                          // 4행 1열, 세로 간격 8px
        scoreLabel.setFont(h2());
        timeLabel.setFont(h2());
        accLabel.setFont(h2());
        detailLabel.setFont(h3());
        scoreLabel.setForeground(fg());                                        // 글자색 통일
        timeLabel.setForeground(fg());
        accLabel.setForeground(fg());
        detailLabel.setForeground(fg());
        center.add(scoreLabel);
        center.add(timeLabel);
        center.add(accLabel);
        center.add(detailLabel);
        add(center, BorderLayout.CENTER);                                      // 중앙 배치

        // 하단: 버튼 두 개 가로 정렬
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 16)); // 중앙정렬/여백
        bottom.setOpaque(false);
        retryButton.setFont(btnF());                                           // 버튼 폰트
        menuButton.setFont(btnF());
        bottom.add(retryButton);
        bottom.add(menuButton);
        add(bottom, BorderLayout.SOUTH);                                       // 하단 배치

        // 버튼 리스너: 라우터 호출로 연결
        retryButton.addActionListener(e -> {
            // 마지막으로 완료한 스테이지가 3이면 랭킹 화면으로, 아니면 다음 게임으로
            if (GamePanel.lastCompletedStage >= 3) {
                router.show(ScreenId.RANKING);
            } else {
                router.show(ScreenId.GAME);
            }
        });
        menuButton.addActionListener(e -> {
            // 메인 메뉴로 이동
            router.show(ScreenId.START);
        });
    }

    // 화면이 보일 때: 컨텍스트에서 최신 결과를 읽어 라벨 반영
    // ※ Showable이 마커 인터페이스일 수 있으므로 @Override는 의도적으로 제거
    public void onShown() {
        ResultData rd = ResultContext.get();                                   // GamePanel이 set 해둔 결과 읽기
        if (rd == null) {                                                      // 비정상 진입 방어
            scoreLabel.setText("Score: -");
            timeLabel.setText("Time Left: -s");
            accLabel.setText("Accuracy: -");
            detailLabel.setText("Hit: -, Miss: -");
            return;
        }

        // 정상 결과 반영
        scoreLabel.setText("Score: " + rd.getScore());
        timeLabel.setText("Time Left: " + rd.getTimeLeftSec() + "s");
        int percent = (int) Math.round(rd.getAccuracy() * 100.0);              // 정확도 → %
        accLabel.setText("Accuracy: " + percent + "%");
        detailLabel.setText("Hit: " + rd.getCorrectCount()
                + ", Miss: " + rd.getWrongCount());
    }

    // 화면이 숨겨질 때: 필요시 정리
    public void onHidden() {
        //결과 화면에서 벗어날 때 컨텍스트 초기화
        ResultContext.clear();                                              // 재시작시 초기화가 필요하면 주석 해제
    }
}
