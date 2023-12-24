package lin.xposed.hook.util;

import android.content.Context;
import android.os.Environment;

import java.io.File;

import lin.xposed.common.config.GlobalConfig;
import lin.xposed.hook.HookEnv;

public class PathTool {

    public static String getDataSavePath(Context context, String dirName) {
        //getExternalFilesDir()：SDCard/Android/data/你的应用的包名/files/dirName
        return context.getExternalFilesDir(dirName).getAbsolutePath();
    }

    public static String getStorageDirectory() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public static String getModuleDataPath() {
        String path = getStorageDirectory() + "/Android/media/" + HookEnv.getCurrentHostAppPackageName() + "/QStory";
        new File(path).mkdirs();
        return path;
    }

    public static String getModuleCachePath(String dirName) {
        File cache = new File(getModuleDataPath() + "/cache/" + dirName);
        if (!cache.exists()) cache.mkdirs();
        return cache.getAbsolutePath();
    }

    public static void updateDataPath(String path) {
        GlobalConfig.putString("DataPath", path);
    }
}
