package lin.xposed.hook.item.api;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import lin.util.ReflectUtils.ClassUtils;
import lin.util.ReflectUtils.FieIdUtils;
import lin.util.ReflectUtils.MethodTool;
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

    public static Object getAppInterface() {
        return AppInterface;
    }

    @Override
    public void loadHook(ClassLoader classLoader) throws Exception {
        if (QQVersion.isQQNT()) {

            Class<?> AIODelegateClass = classLoader.loadClass("com.tencent.qqnt.aio.activity.AIODelegate");
            Method method = MethodTool.find("com.tencent.qqnt.aio.SplashAIOFragment").returnType(AIODelegateClass).get();
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object result = param.getResult();
                    Class<?> findClass = result.getClass();
                    currentAIOContact = MethodTool.find(findClass).returnType(classLoader.loadClass("com.tencent.aio.data.AIOContact")).call(result);
                }
            });
            /*Method getAIOContact = classLoader.loadClass("com.tencent.qqnt.aio.SplashAIOFragment").getMethod("getAIOContact");
            XposedBridge.hookMethod(getAIOContact, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    //进入聊天时会调用三次 退出时会调用两次
                    //退出时AIOContact内的参数都会为空
                    //进出频道不会调用此方法
                    currentAIOContact = param.getResult();
                }
            });*/
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
