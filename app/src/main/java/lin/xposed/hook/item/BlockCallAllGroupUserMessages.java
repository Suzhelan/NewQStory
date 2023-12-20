package lin.xposed.hook.item;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import lin.util.ReflectUtils.ClassUtils;
import lin.util.ReflectUtils.FieIdUtils;
import lin.util.ReflectUtils.MethodTool;
import lin.util.ReflectUtils.MethodUtils;
import lin.xposed.hook.QQVersion;
import lin.xposed.hook.annotation.HookItem;
import lin.xposed.hook.load.base.BaseSwitchFunctionHookItem;
import lin.xposed.hook.util.LogUtils;

@HookItem("净化/通知/屏蔽AT全体消息的系统通知")
public class BlockCallAllGroupUserMessages extends BaseSwitchFunctionHookItem {
    @Override
    public void loadHook(ClassLoader classLoader) throws Exception {

        if (QQVersion.isQQNT()) {
            //com.tencent.qqnt.notification.NotificationFacade.E(mqq.app.AppRuntime,com.tencent.qqnt.notification.struct.d,com.tencent.qqnt.kernel.nativeinterface.NotificationCommonInfo)
            Class<?> findClass = classLoader.loadClass("com.tencent.qqnt.notification.NotificationFacade");
            Method method = MethodTool.find(findClass)
                    .returnType(void.class)
                    .params(
                            ClassUtils.getClass("mqq.app.AppRuntime"),
                            Object.class,
                            ClassUtils.getClass("com.tencent.qqnt.kernel.nativeinterface.NotificationCommonInfo")
                    ).get();
            hookBefore(method, new HookBehavior() {
                @Override
                public void execute(XC_MethodHook.MethodHookParam param) throws Throwable {
                    String contentObjToString = String.valueOf(param.args[1]);
                    if (contentObjToString.contains("content=[@全体成员]")) {
                        LogUtils.addRunLog("AtAllUserNotification", contentObjToString);
                        param.setResult(null);
                    }
                }
            });
        } else {
            Method method = MethodUtils.findMethod("com.tencent.mobileqq.app.QQAppInterface",
                    "notifyMessageReceived", void.class,
                    new Class[]{ClassUtils.getClass("com.tencent.imcore.message.Message"), boolean.class, boolean.class});
            hookBefore(method, new HookBehavior() {
                @Override
                public void execute(XC_MethodHook.MethodHookParam param) throws Throwable {
                    Object message = param.args[0];
                    String messageText = FieIdUtils.getField(message, "msg", String.class);
                    int bizType = FieIdUtils.getField(message, "bizType", int.class);
                    if (bizType == 14 && messageText.contains("@全体成员"))
                        param.setResult(null);
                }
            });
        }


    }
}
