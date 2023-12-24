package lin.xposed.hook;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Bundle;

import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import lin.util.ReflectUtils.ClassUtils;
import lin.xposed.R;
import lin.xposed.common.utils.ActivityTools;
import top.linl.activity.ActivityProxyManager;
import top.linl.dexparser.util.Utils;

public class InitInject implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    private static final AtomicBoolean Initialized = new AtomicBoolean();

    private static void initAppContextAndHookItem(Context applicationContext) {
        //往context注入res
        ActivityTools.injectResourcesToContext(applicationContext);
        //获取和设置全局上下文和类加载器
        HookEnv.setHostAppContext(applicationContext);
        ClassUtils.setHostClassLoader(applicationContext.getClassLoader());
        ClassUtils.setModuleLoader(InitInject.class.getClassLoader());
        //初始化注入活动代理
        ActivityProxyManager.initActivityProxyManager(applicationContext, HookEnv.getModuleApkPath(), R.string.app_name);
        try {
            //加载hook
            HookInit.initMainHook();
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam)  {
        String packageName = loadPackageParam.packageName;
        if (!loadPackageParam.isFirstApplication) return;
        if (!packageName.matches(HookEnv.getTargetPackageName())) return;

        //设置当前应用包名
        HookEnv.setCurrentHostAppPackageName(packageName);
        //设置宿主apk路径
        HookEnv.setHostApkPath(loadPackageParam.appInfo.sourceDir);
        //设置过渡类加载器
        ClassUtils.setHostClassLoader(loadPackageParam.classLoader);

        Class<?> qqAppContextClass = ClassUtils.getClass("com.tencent.mobileqq.qfix.QFixApplication");
        //hook多个初始化方法以适配不同框架
        XC_MethodHook initAppContextHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param)  {
                //防止App创建serve的时候Application对象重复创建调用导致的重复注入
                if (Initialized.getAndSet(true)) return;
                XposedBridge.log("[QStory] " + param.method.getName() + " startLoad");
                Utils.MTimer mTimer = new Utils.MTimer();
                try {
                    ContextWrapper applicationContext = (ContextWrapper) param.thisObject;
                    initAppContextAndHookItem(applicationContext.getBaseContext());
                } catch (Exception e) {
                    XposedBridge.log(e);
                } finally {
                    XposedBridge.log("[QStory] " + param.method.getName() + " Loading Time " + mTimer.get());
                }
            }
        };
        XposedHelpers.findAndHookMethod(qqAppContextClass, "onCreate", initAppContextHook);
        XposedHelpers.findAndHookMethod(qqAppContextClass, "attachBaseContext", Context.class, initAppContextHook);
        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (Initialized.getAndSet(true)) return;
                Activity activity = (Activity) param.thisObject;
                Context appContext = activity.getApplicationContext();
                initAppContextAndHookItem(appContext);
            }
        });
//        XposedHelpers.findAndHookMethod(ContextWrapper.class, "onCreate", initHook);
    }

    @Override
    public void initZygote(StartupParam startupParam) {
        HookEnv.setModuleApkPath(startupParam.modulePath);
    }
}
