package game;

import util.TimerUtil;
import util.GameTimer;
import data.*;
import javax.swing.JOptionPane;

public class SingleGameRules {

    private final GameState state;  // 게임 상태(점수/레벨)
    private GameTimer timer;        // 실제 카운트다운 타이머

    public SingleGameRules(GameState gameState, int level) {
        this.state = gameState;
        startLevelTimer(level);     // 레벨 제한시간으로 타이머 시작
    }

    /** 레벨 제한시간으로 타이머 시작 */
    private void startLevelTimer(int level) {

        int limitSec = TimerUtil.Level_Time(level); // 레벨별 제한시간 계산
        timer = new GameTimer(limitSec);            // 타이머 객체 생성

        timer.start(
                // 1초마다 실행되는 코드
                new Runnable() {
                    @Override
                    public void run() {
                        // 1초 지날때마다 남은 시간 UI 업데이트
                        // 예: gamePanel.updateTime(timer.getTime());
                    }
                },

                // 시간이 0초가 되었을 때 실행되는 코드
                new Runnable() {
                    @Override
                    public void run() {
                        timeOver();   // 메서드 참조가 아니라 직접 호출
                    }
                }
        );
    }

    // 레벨 클리어
    public void levelClear() {
        if (timer != null) timer.stop();// 타이머 정지
        state.addRemainingTimeAsScore();                 // 남은 시간 점수로 추가
        state.nextLevel();                               // 다음 레벨로 이동

        startLevelTimer(state.getLevel());               // 다음 레벨 타이머 시작
    }

    // 시간이 다 되었을 때 (0초)
    public void timeOver() {
        if (timer != null) timer.stop();
        end();
    }

    //게임 종료 + 점수 저장
    private void end() {
        int score = state.getTotalScore();

        String name = JOptionPane.showInputDialog("닉네임을 입력하세요:");
        if (name == null || name.isBlank()) name = "Unknown";

        RankingRepository repo = new CsvRankingRepository();
        repo.save(new ScoreEntry(name, score));
    }
}
