package lin.xposed.hook;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

@SuppressLint("StaticFieldLeak")
public class HookEnv {
    //目标包名 如果通用填.+
    private static final String TargetPackageName = "com.tencent.mobileqq|com.tencent.tim";
    private static String currentHostAppPackageName;

    private static String ModuleApkPath;
    private static String hostApkPath;
    private static String versionName;
    private static int versionCode;
    private static Context hostAppContext;

    public static String getTargetPackageName() {
        return TargetPackageName;
    }

    public static String getCurrentHostPackageName() {
        return getCurrentHostAppPackageName();
    }

    public static void setCurrentHostAppPackageName(String currentHostAppPackageName) {
        HookEnv.currentHostAppPackageName = currentHostAppPackageName;
    }

    public static Context getHostAppContext() {
        return hostAppContext;
    }

    public static void setHostAppContext(Context hostAppContext) {
        HookEnv.hostAppContext = hostAppContext;
        PackageManager packageManager = hostAppContext.getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(hostAppContext.getPackageName(), 0);
            setVersionCode((int) packageInfo.getLongVersionCode());
            setVersionName(packageInfo.versionName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static String getCurrentHostAppPackageName() {
        return currentHostAppPackageName;
    }

    public static String getModuleApkPath() {
        return ModuleApkPath;
    }

    public static void setModuleApkPath(String moduleApkPath) {
        ModuleApkPath = moduleApkPath;
    }

    public static String getHostApkPath() {
        return hostApkPath;
    }

    public static void setHostApkPath(String hostApkPath) {
        HookEnv.hostApkPath = hostApkPath;
    }

    public static String getVersionName() {
        return versionName;
    }

    public static void setVersionName(String versionName) {
        HookEnv.versionName = versionName;
    }

    public static int getVersionCode() {
        return versionCode;
    }

    public static void setVersionCode(int versionCode) {
        HookEnv.versionCode = versionCode;
    }
}
