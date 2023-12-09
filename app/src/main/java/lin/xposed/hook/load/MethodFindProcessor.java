package lin.xposed.hook.load;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.robv.android.xposed.XposedBridge;
import lin.app.main.ModuleBuildInfo;
import lin.util.ReflectUtils.ClassUtils;
import lin.widget.dialog.SimpleLoadingDialog;
import lin.xposed.common.config.SimpleConfig;
import lin.xposed.common.utils.ActivityTools;
import lin.xposed.common.utils.FileUtils;
import lin.xposed.hook.HookEnv;
import lin.xposed.hook.load.base.BaseHookItem;
import lin.xposed.hook.load.methodfind.IMethodFinder;
import lin.xposed.hook.load.methodfind.MethodFinder;
import lin.xposed.hook.util.LogUtils;
import lin.xposed.hook.util.PathTool;
import top.linl.dexparser.DexFinder;

public class MethodFindProcessor {
    /**
     * 是否方法查找期
     */
    public static final AtomicBoolean isMethodFindPeriod = new AtomicBoolean();
    private static Handler mHandler;

    public static void startFindAllMethod(Activity activity) {
        if (isMethodFindPeriod.getAndSet(true)) return;//防止意外的多次进入方法查找期
        SimpleLoadingDialog loadingDialog = new SimpleLoadingDialog(activity);
        loadingDialog.setCanceledOnTouchOutside(false);
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {  //处理线程中handler发送的消息
                super.handleMessage(msg);
                String op = (String) msg.obj;
                switch (op) {
                    case "END" -> loadingDialog.dismiss();
                    case "START" -> loadingDialog.show();
                    default -> loadingDialog.setTitle(op);
                }
            }
        };
        loadingDialog.setTitle("开始初始化");
        new Thread(() -> {
            sendMsgToDialog("START");
            sendMsgToDialog("读取旧数据");
            SimpleConfig config = new SimpleConfig("BaseConfig");
            try {
                config.put("startTime", LogUtils.getTime());
                JSONObject json = new JSONObject();
                sendMsgToDialog("初始化中(LDexParser)...");

                //先初始化
                DexFinder dexFinder = new DexFinder.Builder(ClassUtils.getHostLoader(), HookEnv.getHostApkPath())
                        .setCachePath(PathTool.getModuleDataPath() + "/MethodFinderCache")//设置运行缓存路径 将使用本地内存代表堆内存 这样可以在解析大且多的dex时造成堆溢出
                        .setOnProgress(new MyOnProgress(loadingDialog)).build();//调用build方法后会开始解析 目测300mp的qq解析时间在20s以内
                sendMsgToDialog("初始化完成 开始查找方法...");
                AtomicInteger progress = new AtomicInteger();
                new Handler(Looper.getMainLooper()).post(() -> loadingDialog.progressBar.setMax(HookItemLoader.HookInstance.size() - 1 ));

                Method getResult = MethodFinder.class.getDeclaredMethod("getFindResults");
                getResult.setAccessible(true);
                for (BaseHookItem hookItem : HookItemLoader.HookInstance.values()) {

                    new Handler(Looper.getMainLooper()).post(() -> loadingDialog.progressBar.setProgress(progress.getAndIncrement()));
                    sendMsgToDialog("当前处理的类 : " + hookItem.getClass().getName());

                    if (hookItem instanceof IMethodFinder iMethodFinder) {
                        try {
                            //start find method
                            MethodFinder finder = new MethodFinder(hookItem.getClass(), dexFinder);
                            iMethodFinder.startFind(finder);//收集想要查找的方法信息
                            json.put(hookItem.getClass().getName(), getResult.invoke(finder));
                        } catch (Exception e) {
                            hookItem.getExceptionCollectionToolInstance().addException(e);
                        }
                    }
                }
                sendMsgToDialog("所有方法查找完成 准备保存与重启");

                FileUtils.writeTextToFile(PathTool.getModuleDataPath() + "/data/MethodCache", json.toString(), false);
                //find end
                dexFinder.close();

                isMethodFindPeriod.set(false);
                config.put("moduleVersionAndHostAppVersion", ModuleBuildInfo.moduleVersionName + ":" + HookEnv.getVersionName());
                config.put("time", LogUtils.getTime());
                sendMsgToDialog("END");
            } catch (Exception e) {
                XposedBridge.log(e);
                LogUtils.addError(e);
            } finally {
                config.submit();
            }
            ActivityTools.killAppProcess(HookEnv.getHostAppContext());
        }).start();

    }

    private static void sendMsgToDialog(String s) {
        Message message = new Message();
        message.obj = s;
        mHandler.sendMessage(message);
    }

    public static void scanMethod() throws Exception {
        //从本地扫描方法
        JSONObject methodData = new JSONObject(FileUtils.readFileText(PathTool.getModuleDataPath() + "/data/MethodCache"));
        Method loadMethod = MethodFinder.class.getDeclaredMethod("loadAllMethod", JSONObject.class);
        loadMethod.setAccessible(true);
        for (BaseHookItem hookItem : HookItemLoader.HookInstance.values()) {
            if (hookItem instanceof IMethodFinder iMethodFinder) {
                try {
                    //再运行一次方法查找器并进入方法得到期来让项可以得到方法
                    if (methodData.isNull(hookItem.getClass().getName())) continue;//如果没有 可能是没找到方法
                    JSONObject classMethodData = methodData.getJSONObject(hookItem.getClass().getName());
                    MethodFinder finder = new MethodFinder(hookItem.getClass(), null);
                    loadMethod.invoke(finder, classMethodData);
                    iMethodFinder.getMethod(finder);
                } catch (Exception e) {
                    hookItem.getExceptionCollectionToolInstance().addException(e);
                }
            }
        }
    }

    private static class MyOnProgress implements DexFinder.OnProgress {
        private SimpleLoadingDialog loadingDialog;

        public MyOnProgress(SimpleLoadingDialog loadingDialog) {
            this.loadingDialog = loadingDialog;
        }

        //重写此方法以监听dex解析数量情况 可以用作通知View解析进度
        @Override
        public void init(int dexSize) {
            new Handler(Looper.getMainLooper()).post(() -> loadingDialog.progressBar.setMax(dexSize));
        }

        @Override
        public void parse(int progress, String dexName) {
            new Handler(Looper.getMainLooper()).post(() -> loadingDialog.progressBar.setProgress(progress));
        }
    }
}

