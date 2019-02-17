package experment.zju.statistics.Statistics;

import android.annotation.TargetApi;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Build;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import static experment.zju.statistics.Statistics.AppInformation.bootTime;

public class StatisticsInfo {

    final public static int DAY = 0;
    final public static int WEEK = 1;
    final public static int MONTH = 2;
    final public static int YEAR = 3;

    private ArrayList<AppInformation> ShowList;
    private ArrayList<AppInformation> AppInfoList;
    private List<UsageStats> result;
    private long totalTime;
    private int totalTimes;
    private int style;

    public StatisticsInfo(Context context, int style) {
        try {
            this.style = style;
            setUsageStatsList(context);
            setShowList();

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    //将次数和时间为0的应用信息过滤掉
    private void setShowList() {
        this.ShowList = new ArrayList<>();

        totalTime = 0;

        for (int i = 0; i < AppInfoList.size(); i++) {
            if (AppInfoList.get(i).getUsedTimebyDay() > 0) { //&& AppInfoList.get(i).getTimes() > 0) {

                this.ShowList.add(AppInfoList.get(i));
                totalTime += AppInfoList.get(i).getUsedTimebyDay();
                totalTimes += AppInfoList.get(i).getTimes();
            }
        }

        //将显示列表中的应用按显示顺序排序
        for (int i = 0; i < this.ShowList.size() - 1; i++) {
            for (int j = 0; j < this.ShowList.size() - i - 1; j++) {
                if (this.ShowList.get(j).getUsedTimebyDay() < this.ShowList.get(j + 1).getUsedTimebyDay()) {
                    AppInformation temp = this.ShowList.get(j);
                    this.ShowList.set(j, this.ShowList.get(j + 1));
                    this.ShowList.set(j + 1, temp);
                }
            }
        }
    }


    //统计当天的应用使用时间
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setUsageStatsList(Context context) throws NoSuchFieldException {
        UsageStatsManager m = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        this.AppInfoList = new ArrayList<>();
        if (m != null) {
            Calendar calendar = Calendar.getInstance();
            long now = calendar.getTimeInMillis();
            long begintime = getBeginTime();
            if (style == DAY) {
                this.result = m.queryUsageStats(UsageStatsManager.INTERVAL_BEST, begintime, now);
                AppInfoList = getAccurateDailyStatsList(context, result, m, begintime, now);
            } else {
                if (style == WEEK)
                    this.result = m.queryUsageStats(UsageStatsManager.INTERVAL_WEEKLY, begintime, now);
                else if (style == MONTH)
                    this.result = m.queryUsageStats(UsageStatsManager.INTERVAL_MONTHLY, begintime, now);
                else if (style == YEAR)
                    this.result = m.queryUsageStats(UsageStatsManager.INTERVAL_YEARLY, begintime, now);
                else {
                    this.result = m.queryUsageStats(UsageStatsManager.INTERVAL_BEST, begintime, now);
                }

                List<UsageStats> Mergeresult = MergeList(this.result);
                for (UsageStats usageStats : Mergeresult) {
                    this.AppInfoList.add(new AppInformation(usageStats, context));
                }
                calculateLaunchTimesAfterBootOn(context, AppInfoList);
            }
        }
    }

    /**
     * 根据UsageEvents来对当天的操作次数和开机后运行时间来进行精确计算
     */
    private ArrayList<AppInformation> getAccurateDailyStatsList(Context context, List<UsageStats> result, UsageStatsManager m, long begintime, long now) {
        //针对每个packageName建立一个  使用信息
        HashMap<String, AppInformation> mapData = new HashMap<>();
        //得到包名
        for (UsageStats stats : result) {
            if (stats.getLastTimeUsed() > begintime && stats.getTotalTimeInForeground() > 0) {
                if (mapData.get(stats.getPackageName()) == null) {
                    AppInformation information = new AppInformation(stats, context);
                    //重置总运行时间  开机操作次数
                    information.setTimes(0);
                    information.setUsedTimebyDay(0);
                    mapData.put(stats.getPackageName(), information);
                }
            }
        }

        //这个是相对比较精确的
        long bootTime = AppInformation.bootTime();
        UsageEvents events = m.queryEvents(bootTime, now);

        UsageEvents.Event e = new UsageEvents.Event();
        while (events.hasNextEvent()) {
            events.getNextEvent(e);
            String packageName = e.getPackageName();

            AppInformation information = mapData.get(packageName);
            if (information == null) {
                continue;
            }

            //这里在同时计算开机后的操作次数和运行时间，所以如果获取到的时间戳是昨天的话就得过滤掉 continue

            if (e.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                information.timesPlusPlus();
                if (e.getTimeStamp() < begintime){
                    continue;
                }
                information.setTimeStampMoveToForeground(e.getTimeStamp());
            } else if (e.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                if (e.getTimeStamp() < begintime){
                    continue;
                }
                information.setTimeStampMoveToBackGround(e.getTimeStamp());
                //当前应用是在昨天进入的前台，0点后转入了后台，所以会先得到MOVE_TO_BACKGROUND 的timeStamp
                if (information.getTimeStampMoveToForeground() < 0) {
                    //从今天开始计算即可
                    information.setTimeStampMoveToForeground(begintime);
                }
            }
            information.calculateRunningTime();
        }

        //再计算一次当前应用的运行时间，因为当前应用，最后得不到MOVE_TO_BACKGROUND 的timeStamp
        AppInformation information = mapData.get(context.getPackageName());
        information.setTimeStampMoveToBackGround(now);
        information.calculateRunningTime();

        return new ArrayList<>(mapData.values());
    }

    /**
     * 根据UsageEvents 精确计算APP开机的启动(activity打开的)次数
     */
    private void calculateLaunchTimesAfterBootOn(Context context, List<AppInformation> AppInfoList) {

        UsageStatsManager m = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (m == null || AppInfoList == null || AppInfoList.size() < 1) {
            return;
        }
        //针对每个packageName建立一个  使用信息
        HashMap<String, AppInformation> mapData = new HashMap<>();

        UsageEvents events = m.queryEvents(bootTime(), System.currentTimeMillis());
        for (AppInformation information : AppInfoList) {
            mapData.put(information.getPackageName(), information);
            information.setTimes(0);
        }

        UsageEvents.Event e = new UsageEvents.Event();
        while (events.hasNextEvent()) {
            events.getNextEvent(e);
            String packageName = e.getPackageName();
            AppInformation information = mapData.get(packageName);
            if (information == null) {
                continue;
            }

            if (e.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                information.timesPlusPlus();
            }
        }
    }

    private long getBeginTime() {
        Calendar calendar = Calendar.getInstance();
        long begintime;
        if (style == WEEK) {
            //int weekDay = calendar.get(Calendar.DAY_OF_WEEK);
            calendar.add(Calendar.DATE, -7);
            begintime = calendar.getTimeInMillis();
        } else if (style == MONTH) {
            //int mounthDay = calendar.get(Calendar.DAY_OF_MONTH);
            calendar.add(Calendar.DATE, -30);
            begintime = calendar.getTimeInMillis();
        } else if (style == YEAR) {
            calendar.add(Calendar.YEAR, -1);
            begintime = calendar.getTimeInMillis();
        } else {
            //剩下的输入均显示当天的数据
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            int second = calendar.get(Calendar.SECOND);

            calendar.add(Calendar.SECOND, -1 * second);
            calendar.add(Calendar.MINUTE, -1 * minute);
            calendar.add(Calendar.HOUR, -1 * hour);
            begintime = calendar.getTimeInMillis();

        }
        return begintime;
    }

    private List<UsageStats> MergeList(List<UsageStats> result) {
        List<UsageStats> Mergeresult = new ArrayList<>();
        long begintime;
        begintime = getBeginTime();
        for (int i = 0; i < result.size(); i++) {
            if (result.get(i).getLastTimeUsed() > begintime) {
                int num = FoundUsageStats(Mergeresult, result.get(i));
                if (num >= 0) {
                    UsageStats u = Mergeresult.get(num);
                    u.add(result.get(i));
                    Mergeresult.set(num, u);
                } else Mergeresult.add(result.get(i));
            }
        }
        return Mergeresult;
    }

    private int FoundUsageStats(List<UsageStats> Mergeresult, UsageStats usageStats) {
        for (int i = 0; i < Mergeresult.size(); i++) {
            if (Mergeresult.get(i).getPackageName().equals(usageStats.getPackageName())) {
                return i;
            }
        }
        return -1;
    }


    public long getTotalTime() {
        return totalTime;
    }

    public int getTotalTimes() {
        return totalTimes;
    }

    public ArrayList<AppInformation> getShowList() {
        return ShowList;
    }
}

