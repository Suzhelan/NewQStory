package lin.xposed.hook.item.api;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import lin.util.ReflectUtils.ClassUtils;
import lin.util.ReflectUtils.FieIdUtils;
import lin.xposed.hook.QQVersion;
import lin.xposed.hook.annotation.HookItem;
import lin.xposed.hook.load.base.ApiHookItem;
import lin.xposed.hook.load.methodfind.IMethodFinder;
import lin.xposed.hook.load.methodfind.MethodFinder;

@HookItem(value = "Session_info_find_init", hasPath = false)
public class SessionApi extends ApiHookItem implements IMethodFinder {

    /**
     * no qqnt
     */
    private static Object currentSessionInfo, AppInterface;
    /**
     * is qqnt
     */
    private static Object currentAIOContact;

    private final String methodId = "AIO_doOnCreate_initUI";
    private Method doOnAIOCreateMethod;

    public static Object getSession() {
        if (QQVersion.isQQNT()) return currentAIOContact;
        return currentSessionInfo;
    }

    public static Object getAIOContact() {
        return currentAIOContact;
    }
    public static Object getAppInterface() {
        return AppInterface;
    }

    @Override
    public void loadHook(ClassLoader classLoader) throws Exception {
        if (QQVersion.isQQNT()) {

            Class<?> AIOContextImpl = ClassUtils.getClass("com.tencent.aio.runtime.AIOContextImpl");
            XposedHelpers.findAndHookConstructor(AIOContextImpl,
                    ClassUtils.getClass("com.tencent.aio.main.fragment.ChatFragment"),
                    ClassUtils.getClass("com.tencent.aio.data.AIOParam"),
                    ClassUtils.getClass("androidx.lifecycle.LifecycleOwner"),
                    ClassUtils.getClass("kotlin.jvm.functions.Function0"),
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            Object aIOParam = param.args[1];
                            Object aIOSession = FieIdUtils.getFirstField(aIOParam, ClassUtils.getClass("com.tencent.aio.data.AIOSession"));
                            Object aIOContact = FieIdUtils.getFirstField(aIOSession, ClassUtils.getClass("com.tencent.aio.data.AIOContact"));
                            currentAIOContact = aIOContact;
                        }
                    }
            );
        } else {
            XposedBridge.hookMethod(doOnAIOCreateMethod, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object ChatPie = param.thisObject;
                    AppInterface = FieIdUtils.getFirstField(ChatPie, ClassUtils.getClass("com.tencent.mobileqq.app.QQAppInterface"));
                    currentSessionInfo = FieIdUtils.getFirstField(ChatPie, ClassUtils.getClass("com.tencent.mobileqq.activity.aio.SessionInfo"));
                }
            });
        }
    }

    @Override
    public void startFind(MethodFinder finder) throws Exception {
        //qq nt
        if (QQVersion.isQQNT()) {

        }
        //no qq nt
        else {
            Method[] AIO_doOnCreate_initUI = finder.findMethodString("AIO_doOnCreate_initUI");
            for (Method method : AIO_doOnCreate_initUI) {
                if (method.getDeclaringClass().getName().equals("com.tencent.mobileqq.activity.aio.core.BaseChatPie")) {
                    finder.putMethod(methodId, method);
                    break;
                }
            }
        }
    }

    @Override
    public void getMethod(MethodFinder finder) {
        if (QQVersion.isQQNT()) {

        } else {
            doOnAIOCreateMethod = finder.getMethod(methodId);
        }
    }
}
