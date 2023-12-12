package lin.xposed.hook.util.qq;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lin.util.ReflectUtils.ClassUtils;
import lin.util.ReflectUtils.MethodTool;
import lin.xposed.hook.util.ToastTool;

public class SendMsgUtils {

    private static final ExecutorService service = Executors.newSingleThreadExecutor();

    /**
     * 发送一条消息
     *
     * @param contact     发送联系人 通过 getContact方法创建
     * @param elementList 元素列表 通过 {@link CreateElement}创建元素
     */
    public static void sendMsg(Object contact, ArrayList elementList) {
        if (contact == null) {
            ToastTool.show("contact==null");
            return;
        }
        if (elementList == null) {
            ToastTool.show("elementList==null");
            return;
        }
        Class<?> IMsgServiceClass = ClassUtils.getClass("com.tencent.qqnt.msg.api.IMsgService");
        Object msgServer = QQEnvTool.getQRouteApi(IMsgServiceClass);
        MethodTool.find(msgServer.getClass()).params(ClassUtils.getClass("com.tencent.qqnt.kernel.nativeinterface.Contact"), ArrayList.class, ClassUtils.getClass("com.tencent.qqnt.kernel.nativeinterface.IOperateCallback")).returnType(void.class).name("sendMsg").call(msgServer, contact, elementList, Proxy.newProxyInstance(ClassUtils.getHostLoader(), new Class[]{ClassUtils.getClass("com.tencent.qqnt.kernel.nativeinterface.IOperateCallback")}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                // void onResult(int i2, String str);
                return null;
            }
        }));



        /*int random = (int) Math.random();
        Object c = XposedHelpers.callStaticMethod(ClassUtils.getClass("com.tencent.qqnt.msg.g"), "c");
        XposedHelpers.callMethod(c, "sendMsg", new Class[]{
                        long.class,
                        ClassUtils.getClass("com.tencent.qqnt.kernel.nativeinterface.Contact"),
                        ArrayList.class,
                        HashMap.class,
                        ClassUtils.getClass("com.tencent.qqnt.kernel.nativeinterface.IOperateCallback")
                },
                random, contact, elementList, new HashMap<>(),
                //代理此接口
                Proxy.newProxyInstance(ClassUtils.getHostLoader(),
                        new Class[]{ClassUtils.getClass("com.tencent.qqnt.kernel.nativeinterface.IOperateCallback")}, new InvocationHandler() {
                            @Override
                            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                return null;
                            }
                        }));*/
    }

}
