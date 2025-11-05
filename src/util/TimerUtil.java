package util;

public class TimerUtil {
    public static final int start = 90;
    public static final int decrease = 10;
    public static final int min = 0;

    public static int Level_Time(int level){
        int seconds = start - (decrease*(level-1));
        return Math.max(min, seconds); //음수 방지
    }

    public static String format(int sec){
        return String.format("%02d:%02d", sec/60, sec%60);
    }


}


