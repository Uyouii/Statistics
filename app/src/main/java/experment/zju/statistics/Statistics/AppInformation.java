package experment.zju.statistics.Statistics;

import android.app.usage.UsageStats;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;


public class AppInformation {
    private UsageStats usageStats;
    private String packageName;
    private String label;
    private Drawable Icon;
    private long UsedTimebyDay;  //milliseconds
    private Context context;
    private int times;


    public AppInformation(UsageStats usageStats, Context context) {
        this.usageStats = usageStats;
        this.context = context;

        try {
            GenerateInfo();
        } catch (PackageManager.NameNotFoundException | IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private void GenerateInfo() throws PackageManager.NameNotFoundException, NoSuchFieldException, IllegalAccessException {
        PackageManager packageManager = context.getPackageManager();
        this.packageName = usageStats.getPackageName();
        if (this.packageName != null && !this.packageName.equals("")) {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(this.packageName, 0);
            this.label = (String) packageManager.getApplicationLabel(applicationInfo);
            this.UsedTimebyDay = usageStats.getTotalTimeInForeground();
            this.times = (Integer) usageStats.getClass().getDeclaredField("mLaunchCount").get(usageStats);

            if (this.UsedTimebyDay > 0) {
                this.Icon = applicationInfo.loadIcon(packageManager);
            }
        }
    }

    public UsageStats getUsageStats() {
        return usageStats;
    }

    public int getTimes() {
        return times;
    }

    public void setTimes(int times) {
        this.times = times;
    }

    public void setUsedTimebyDay(long usedTimebyDay) {
        this.UsedTimebyDay = usedTimebyDay;
    }

    public Drawable getIcon() {
        return Icon;
    }

    public long getUsedTimebyDay() {
        return UsedTimebyDay;
    }

    public String getLabel() {
        return label;
    }

    public String getPackageName() {
        return packageName;
    }

    private long timeStampMoveToForeground = -1;

    private long timeStampMoveToBackGround = -1;


    public void setTimeStampMoveToForeground(long timeStampMoveToForeground) {
//        if (timeStampMoveToForeground > bootTime()){
//            timesPlusPlus();
//        }
        this.timeStampMoveToForeground = timeStampMoveToForeground;
    }

    public void timesPlusPlus(){
        times++;
    }

    public void setTimeStampMoveToBackGround(long timeStampMoveToBackGround) {
        this.timeStampMoveToBackGround = timeStampMoveToBackGround;
    }

    public long getTimeStampMoveToBackGround() {
        return timeStampMoveToBackGround;
    }

    public long getTimeStampMoveToForeground() {
        return timeStampMoveToForeground;
    }

    public void calculateRunningTime() {

        if (timeStampMoveToForeground < 0 || timeStampMoveToBackGround < 0) {
            return;
        }

        if (timeStampMoveToBackGround > timeStampMoveToForeground) {
            UsedTimebyDay += (timeStampMoveToBackGround - timeStampMoveToForeground);
            timeStampMoveToForeground = -1;
            timeStampMoveToBackGround = -1;
        }

    }


    // 返回开机时间，单位微妙
    public static long bootTime() {
        return System.currentTimeMillis() - SystemClock.elapsedRealtime();
    }

}
