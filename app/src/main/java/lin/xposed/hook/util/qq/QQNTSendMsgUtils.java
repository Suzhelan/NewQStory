package lin.xposed.hook.util.qq;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;

import lin.util.ReflectUtils.ClassUtils;
import lin.util.ReflectUtils.MethodTool;
import lin.xposed.hook.util.ToastTool;

public class QQNTSendMsgUtils {
    public static void sendPic(Object contact, String picUrl) {
        ArrayList element = new ArrayList();
        element.add(CreateElement.createPicElement(picUrl));
        sendMsg(contact, element);
    }

    public static void sendVoice(Object contact, String voiceFile) {
        ArrayList element = new ArrayList();
        element.add(CreateElement.createPttElement(voiceFile));
        sendMsg(contact, element);
    }

    /**
     * 发送一条消息
     *
     * @param contact     发送联系人 通过 {@link SessionUtils} 类创建
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
    }

}
