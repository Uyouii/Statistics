package experment.zju.statistics.Statistics;

import android.annotation.TargetApi;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Build;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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

        for(int i=0;i<AppInfoList.size();i++) {
            if(AppInfoList.get(i).getUsedTimebyDay() > 0 ) { //&& AppInfoList.get(i).getTimes() > 0) {

                this.ShowList.add(AppInfoList.get(i));
                totalTime += AppInfoList.get(i).getUsedTimebyDay();
                totalTimes += AppInfoList.get(i).getTimes();
            }
        }

        //将显示列表中的应用按显示顺序排序
        for(int i = 0;i<this.ShowList.size() - 1;i++) {
            for(int j = 0; j< this.ShowList.size() - i - 1; j++) {
                if(this.ShowList.get(j).getUsedTimebyDay() < this.ShowList.get(j+1).getUsedTimebyDay()) {
                    AppInformation temp = this.ShowList.get(j);
                    this.ShowList.set(j,this.ShowList.get(j+1));
                    this.ShowList.set(j+1,temp);
                }
            }
        }
    }


    //统计当天的应用使用时间
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setUsageStatsList(Context context) throws NoSuchFieldException {
        Calendar calendar = Calendar.getInstance();
        long now = calendar.getTimeInMillis();
        setResultList(context);
        List<UsageStats> Mergeresult = MergeList(this.result);

        for(UsageStats usageStats:Mergeresult) {
            this.AppInfoList.add(new AppInformation(usageStats , context));
        }
//        for(AppInformation appInformation : this.AppInfoList) {
//            if(appInformation.getUsedTimebyDay() > 0) {
//                System.out.println("packagename " + appInformation.getPackageName());
//                System.out.println("label: " + appInformation.getLabel());
//                System.out.println("time: " + DateUtils.formatElapsedTime(appInformation.getUsedTimebyDay() / 1000 ));
//                System.out.println("Times: " + appInformation.getTimes());
//                System.out.println("firstTimeStmp: " + DateUtils.formatElapsedTime((now - appInformation.getUsageStats().getFirstTimeStamp()) / 1000));
//                System.out.println("lastTimeStmp: " + DateUtils.formatElapsedTime((now- appInformation.getUsageStats().getLastTimeStamp()) / 1000));
//                System.out.println("lastTimeUsed: " + DateUtils.formatElapsedTime((now - appInformation.getUsageStats().getLastTimeUsed() )/ 1000));
//
//                //String info = appInformation.getLabel() +  " " + DateUtils.formatElapsedTime(appInformation.getUsedTimebyDay() / 1000 );
//            }
//        }
    }

    private void setResultList(Context context) {
        UsageStatsManager m = (UsageStatsManager)context.getSystemService(Context.USAGE_STATS_SERVICE);
        this.AppInfoList = new ArrayList<>();
        if(m != null) {
            Calendar calendar = Calendar.getInstance();
            long now = calendar.getTimeInMillis();
            long begintime = getBeginTime();
            if(style == DAY)
                this.result = m.queryUsageStats(UsageStatsManager.INTERVAL_BEST, begintime, now);
            else if(style == WEEK)
                this.result = m.queryUsageStats(UsageStatsManager.INTERVAL_WEEKLY,begintime, now);
            else if(style == MONTH)
                this.result = m.queryUsageStats(UsageStatsManager.INTERVAL_MONTHLY, begintime, now);
            else if(style == YEAR)
                this.result = m.queryUsageStats(UsageStatsManager.INTERVAL_YEARLY, begintime, now);
            else {
                this.result = m.queryUsageStats(UsageStatsManager.INTERVAL_BEST, begintime, now);
            }
        }
    }

    private long getBeginTime() {
        Calendar calendar = Calendar.getInstance();
        long begintime;
        if(style == WEEK) {
            //int weekDay = calendar.get(Calendar.DAY_OF_WEEK);
            calendar.add(Calendar.DATE,-7);
            begintime = calendar.getTimeInMillis();
        }
        else if(style == MONTH) {
            //int mounthDay = calendar.get(Calendar.DAY_OF_MONTH);
            calendar.add(Calendar.DATE,-30);
            begintime = calendar.getTimeInMillis();
        }
        else if(style == YEAR) {
            calendar.add(Calendar.YEAR,-1);
            begintime = calendar.getTimeInMillis();
        }
        else{
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

    private List<UsageStats> MergeList( List<UsageStats> result) {
        List<UsageStats> Mergeresult = new ArrayList<>();

        for(int i=0;i<result.size();i++) {

            long begintime;
            begintime = getBeginTime();

            if(result.get(i).getFirstTimeStamp() > begintime) {
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
        for(int i=0;i<Mergeresult.size();i++) {
            if(Mergeresult.get(i).getPackageName().equals(usageStats.getPackageName())) {
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

