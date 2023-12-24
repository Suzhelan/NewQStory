package lin.xposed.hook.item.emojipanel;

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lin.xposed.common.utils.DataUtils;
import lin.xposed.common.utils.HttpUtils;
import lin.xposed.hook.util.PathTool;

public class EmoOnlineLoader {
    public static ExecutorService syncThread = Executors.newFixedThreadPool(16);
    static ExecutorService savePool = Executors.newFixedThreadPool(16);
    static ExecutorService savePoolSingle = Executors.newSingleThreadExecutor();

    public static void submit(EmoPanel.EmoInfo info, Runnable run) {
        syncThread.submit(() -> {
            try {
                String CacheDir = PathTool.getModuleCachePath("img") + "/" + info.MD5;
                if (info.MD5.equals(DataUtils.getFileMD5(new File(CacheDir)))) {
                    info.Path = CacheDir;
                    new Handler(Looper.getMainLooper()).post(run);
                    return;
                }
                new File(CacheDir).delete();

                HttpUtils.fileDownload(info.URL, CacheDir);
                info.Path = CacheDir;
                new Handler(Looper.getMainLooper()).post(run);
            } catch (Throwable th) {
                new Handler(Looper.getMainLooper()).post(run);
            }

        });
    }

    public static void submit2(EmoPanel.EmoInfo info, Runnable run) {
        savePool.submit(() -> {
            try {
                String CacheDir = PathTool.getModuleCachePath("img") + "/" + info.MD5;
                if (info.MD5.equals(DataUtils.getFileMD5(new File(CacheDir)))) {
                    info.Path = CacheDir;
                    new Handler(Looper.getMainLooper()).post(run);
                    return;
                }
                new File(CacheDir).delete();

                HttpUtils.fileDownload(info.URL, CacheDir);
                info.Path = CacheDir;
                new Handler(Looper.getMainLooper()).post(run);
            } catch (Throwable th) {
                new Handler(Looper.getMainLooper()).post(run);
            }

        });
    }
}