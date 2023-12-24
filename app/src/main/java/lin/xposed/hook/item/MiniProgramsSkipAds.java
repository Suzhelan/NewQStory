package lin.xposed.hook.item;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;
import lin.util.ReflectUtils.ClassUtils;
import lin.util.ReflectUtils.FieldUtils;
import lin.util.ReflectUtils.MethodTool;
import lin.util.ReflectUtils.MethodUtils;
import lin.xposed.hook.HookEnv;
import lin.xposed.hook.QQVersion;
import lin.xposed.hook.annotation.HookItem;
import lin.xposed.hook.load.base.BaseSwitchFunctionHookItem;
import lin.xposed.hook.load.methodfind.IMethodFinder;
import lin.xposed.hook.load.methodfind.MethodFinder;

//TK 刺痛
@HookItem("辅助功能/小程序/小程序跳过广告")
public class MiniProgramsSkipAds extends BaseSwitchFunctionHookItem implements IMethodFinder {

    private Method m1;

    @Override
    public void loadHook(ClassLoader classLoader) throws Exception {
        Class<?> GdtMvViewController = ClassUtils.getClass(HookEnv.getVersionCode() < QQVersion.QQ_8_9_78 ? "com.tencent.gdtad.basics.motivevideo.GdtMvViewController" : "com.tencent.gdtad.basics.motivevideo.GdtMvVideoViewController");

        Method method = MethodTool.find("com.tencent.gdtad.basics.motivevideo.a.b")
                .params(View.class,
                        ClassUtils.getClass("com.tencent.gdtad.basics.motivevideo.a.b$a"),
                        ClassUtils.getClass("com.tencent.gdtad.basics.motivevideo.data.GdtMotiveVideoModel")
                )
                .returnType(void.class)
                .get();
        hookAfter(method,param -> {
            Object Controller = param.args[1];
            XposedHelpers.callMethod(Controller, "a");
            XposedHelpers.callMethod(Controller, "b");
        });

        //代码搜索"GdtMotiveBrowsingDialog", 1, "doOnBackEvent"
        Class<?> classIfExists = ClassUtils.getClass("com.tencent.gdtad.basics.motivebrowsing.GdtMotiveBrowsingDialog");
        if (classIfExists != null) {
            String methodName = "onCreate";
            Method MethodIfExists = XposedHelpers.findMethodExactIfExists(classIfExists, methodName, Bundle.class);
            hookAfter(MethodIfExists, param -> {
                Object thisObject = param.thisObject;
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        FieldUtils.setField(thisObject, "n", boolean.class, true);//代码搜索"GdtMotiveBrowsingDialog", 1, "doOnBackEvent"方法内部判断调用的就是
                        m1.invoke(thisObject);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, 200);
            });

        }
        //代码搜索"GdtMvViewController", 1, "[onBackEvent] mHasWatchAds "
        if (GdtMvViewController != null) {
            Method MethodIfExists;
            MethodIfExists = MethodUtils.findMethod(GdtMvViewController, null, boolean.class, new Class[]{boolean.class});
            if (MethodIfExists != null) {
                hookBefore(MethodIfExists, param -> {
                    FieldUtils.setField(param.thisObject, "k", boolean.class, true);
                });
            }
        }

        //代码搜索"GdtMotiveVideoDialog", 4, "[onClickChangeVideo] innerView is added"

        //代码搜索GdtMotiveVideoDialog", "onWindowFocusChanged() called with: hasFocus = 定位该类
        Class<?> classIfExists3 = ClassUtils.getClass("com.tencent.gdtad.basics.motivevideo.c");
        if (classIfExists3 != null) {
            String methodName = "onCreate";
            Method MethodIfExists = XposedHelpers.findMethodExactIfExists(classIfExists3, methodName, Bundle.class);
            if (MethodIfExists != null) {
                hookAfter(MethodIfExists, param -> {
                    //GdtMvVideoViewController字段
                    Object field = FieldUtils.getFirstField(param.thisObject, GdtMvViewController);//GdtMvViewController字段
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {

                        //GdtMvVideoViewController", 1, "audioSwitch click is called isSilentMode =
                        XposedHelpers.callMethod(field, "a");//代码搜索 GdtMvViewController", 1, "audioSwitch click is called isSilentMode =
                        //"GdtMvVideoViewController", 1, " onClick close_ads"
                        XposedHelpers.callMethod(field, "b");//代码搜索 "GdtMvViewController", 1, " onClick close_ads"
                        //"GdtMvVideoViewController", 1, "[initTitle] NumberFormatException, "
                        XposedHelpers.callMethod(field, HookEnv.getVersionCode() < QQVersion.QQ_8_9_70 ? "V" : "Z");//代码搜索 "GdtMvViewController", 1, "[initTitle] NumberFormatException, "

                    }, 1200);
                });
            }
        }
    }

    @Override
    public void startFind(MethodFinder methodFinder) throws Exception {
        Method[] methods = methodFinder.findMethodString("doOnBackEvent");
        for (Method method : methods) {
            Class<?> dialogClass = ClassUtils.getClass("com.tencent.gdtad.basics.motivebrowsing.GdtMotiveBrowsingDialog");
            if (method.getDeclaringClass() == dialogClass) {
                methodFinder.putMethod("GdtMotiveBrowsingDialog : doOnBackEvent", method);
                break;
            }
        }

    }

    @Override
    public String getTips() {
        return "脆弱的功能 很容易坏";
    }

    @Override
    public void getMethod(MethodFinder finder) {
        m1 = finder.getMethod("GdtMotiveBrowsingDialog : doOnBackEvent");
    }


}
