package util;
//시간을 흘러가게 하는 타이머
public class GameTimer {
    private int sec;//남은시간
    private boolean running; //

    public GameTimer(int sec) {
        this.sec = sec;
        this.running = false;//시작 전
    }

    public void start(Runnable onTick, Runnable onFinish) {
        running = true;

        //타이머 전용 스레드
        new Thread(() -> {
            while (running && sec > 0) {
                try { Thread.sleep(1000); } catch(Exception e){}
                sec--;
                onTick.run();//남은 시간 업데이트
            }
            if (running&&sec==0) onFinish.run();
        }).start();
    }

    public void stop() { running = false; }
    public int getTime() { return sec; }
}
