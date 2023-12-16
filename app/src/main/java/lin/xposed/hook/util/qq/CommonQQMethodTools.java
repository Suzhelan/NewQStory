package lin.xposed.hook.util.qq;

import android.os.Bundle;

import java.lang.reflect.Method;
import java.util.List;

import lin.util.ReflectUtils.ClassUtils;
import lin.util.ReflectUtils.MethodTool;

public class CommonQQMethodTools {

    public static Method getQQNTAIOGetMsgItemViewMethod() {
        /*Method onUIUpdate = MethodTool.find("com.tencent.mobileqq.aio.msglist.holder.AIOBubbleMsgItemVB").params(int.class, Object.class, List.class, Bundle.class).returnType(void.class).get();
        hookAfter(onUIUpdate, param -> {
            Object thisObject = param.thisObject;
            View itemView = FieIdUtils.getFirstField(thisObject, View.class);

            //get aio msg item
            Object aioMsgItem = FieIdUtils.getFirstField(thisObject, ClassUtils.getClass("com.tencent.mobileqq.aio.msg.AIOMsgItem"));

            Object msgRecord = MethodTool.find(aioMsgItem.getClass()).name("getMsgRecord").call(aioMsgItem);
        });*/
        Method onUIUpdate = MethodTool.find("com.tencent.mobileqq.aio.msglist.holder.AIOBubbleMsgItemVB")
                .params(int.class, Object.class, List.class, Bundle.class)
                .returnType(void.class)
                .get();
        return onUIUpdate;
    }

    public static Class<?> getMsgRecordClass() {
        return ClassUtils.getClass("com.tencent.qqnt.kernel.nativeinterface.MsgRecord");
    }

    public static Class<?> getAIOContactClass() {
        return ClassUtils.getClass("com.tencent.aio.data.AIOContact");
    }

    public static Class<?> getAIOMsgItemClass() {
        return ClassUtils.getClass("com.tencent.mobileqq.aio.msg.AIOMsgItem");
    }
}
