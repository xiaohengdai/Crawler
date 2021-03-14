package util;


import java.util.*;


public class TimeTest {

    public static void showDayTime() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        System.out.println("执行ShowDayTime");
        calendar.set(year, month, day, 21, 10, 00);//设置要执行的日期时间

        Date defaultdate = calendar.getTime();

        Timer dTimer = new Timer();
        dTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("每日任务已经执行");
                String cmd = "java -jar target/UICrawler-2.0.jar -f config.yml -t 4723  -u 127.0.0.1:62001";
                ArrayList<String> cmdList = new ArrayList<>();
                cmdList.add(cmd);
//[ro.build.version.sdk]: [25]
                String res = Util.exeCmd(cmdList);
            }
        }, defaultdate , 24* 60* 60 * 1000);//24* 60* 60 * 1000
    }

    public static void main(String[] args) {
        showDayTime();
    }

}


