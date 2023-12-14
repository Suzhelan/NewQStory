package lin.xposed.hook.item;

import android.content.Context;
import android.view.View;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import lin.util.ReflectUtils.ClassUtils;
import lin.util.ReflectUtils.ConstructorUtils;
import lin.util.ReflectUtils.FieIdUtils;
import lin.util.ReflectUtils.MethodTool;
import lin.util.ReflectUtils.MethodUtils;
import lin.xposed.R;
import lin.xposed.common.utils.ActivityTools;
import lin.xposed.hook.HookEnv;
import lin.xposed.hook.QQVersion;
import lin.xposed.hook.annotation.HookItem;
import lin.xposed.hook.item.api.AIOMessageMenu;
import lin.xposed.hook.item.voicepanel.VoiceTools;
import lin.xposed.hook.load.base.BaseSwitchFunctionHookItem;

@HookItem("辅助功能/聊天/保存语音")
public class HookSaveVoiceFile extends BaseSwitchFunctionHookItem {

    /**
     * 此方法可以注入所有消息类型的长按菜单中 但是暂时不需要
     */
    public void hookAllTypeMsgMenu() {
        try {
            Method setMenu;
            if (HookEnv.getVersionCode() > QQVersion.QQ_8_9_93) {
                setMenu = MethodTool.find("com.tencent.qqnt.aio.menu.ui.QQCustomMenuExpandableLayout").name("setMenu").params(Object.class).get();
            } else {
                setMenu = MethodTool.find("com.tencent.qqnt.aio.menu.ui.QQCustomMenuNoIconLayout").name("setMenu").params(Object.class).get();
            }
            hookBefore(setMenu, param -> {
                Object itemListWrapper = param.args[0];
                //如果想添加Item则add到这个list
                List itemList = FieIdUtils.getFirstField(itemListWrapper, List.class);
                Object item = itemList.get(0);
                Object aioMsgItem = FieIdUtils.getFirstField(item, ClassUtils.getClass("com.tencent.mobileqq.aio.msg.AIOMsgItem"));
                Object msgRecord = XposedHelpers.callMethod(aioMsgItem, "getMsgRecord");

            });
        } catch (Exception e) {
        }
    }

    @Override
    public void loadHook(ClassLoader classLoader) throws Exception {
        //is qqnt
        if (QQVersion.isQQNT()) {
            Class<?> aioMsgItemClass = ClassUtils.getClass("com.tencent.mobileqq.aio.msg.AIOMsgItem");
            Class<?> pttMenuClass = ClassUtils.getClass("com.tencent.mobileqq.aio.msglist.holder.component.ptt.AIOPttContentComponent");
            Class<?> baseMenuClass = ClassUtils.getClass("com.tencent.mobileqq.aio.msglist.holder.component.BaseContentComponent");
            //get aio msg item
            Method getMsgMethod = MethodTool.find(baseMenuClass).returnType(aioMsgItemClass).get();
            //获取父类的get结果方法 父类是抽象类方法有唯一性 下一步再利用父类查找到的方法查找具体实现类中的方法 方法签名 public final T Z0() 可以得知该方法不会被子类重写
            Method superGetResultListMethod = MethodTool.find(baseMenuClass).returnType(List.class).get();
            //通过父类查找具体子实现类的方法
            Method getVoiceMessageMenuMethod = MethodTool.find(pttMenuClass).name(superGetResultListMethod.getName()).returnType(List.class).get();

            hookAfter(getVoiceMessageMenuMethod, new HookBehavior() {
                @Override
                public void execute(XC_MethodHook.MethodHookParam param) throws Throwable {
                    Object pttAIOMsgItem = getMsgMethod.invoke(param.thisObject);
                    Object item = AIOMessageMenu.createAIOMenuItemQQNT(pttAIOMsgItem, "保存到QS", R.mipmap.ic_launcher_round, () -> {
                        Class<?> pttElementClass = classLoader.loadClass("com.tencent.qqnt.kernel.nativeinterface.PttElement");
                        Object pttElement = MethodTool.find(pttAIOMsgItem.getClass()).returnType(pttElementClass).call(pttAIOMsgItem);
                        String pttFilePath = MethodTool.find(pttElement.getClass()).name("getFilePath").returnType(String.class).call(pttElement);
                        VoiceTools.createSaveVoiceDialog(ActivityTools.getActivity(), pttFilePath);
                        return null;
                    });
                    List resultList = (List) param.getResult();
                    resultList.add(item);
                }
            });

        } else {
            //no is qqnt
            Method menuCreate = findMessageMenu(ClassUtils.getClass("com.tencent.mobileqq.activity.aio.item.PttItemBuilder"));
            Method onClickMethod = MethodUtils.findMethod(ClassUtils.getClass("com.tencent.mobileqq.activity.aio.item.PttItemBuilder"), "a", void.class, new Class[]{int.class, Context.class, ClassUtils.getClass("com.tencent.mobileqq.data.ChatMessage")});
            hookAfter(menuCreate, param -> {
                Object arr = param.getResult();
                Object ret = Array.newInstance(arr.getClass().getComponentType(), Array.getLength(arr) + 1);
                //复制数组
                System.arraycopy(arr, 0, ret, 1, Array.getLength(arr));
                //new一个menuitem
                Object MenuItem = ConstructorUtils.newInstance(arr.getClass().getComponentType(), 4192, "保存到QS");
                //设置该菜单的展示优先级
                FieIdUtils.setField(MenuItem, "c", Integer.MAX_VALUE - 2);
                Array.set(ret, 0, MenuItem);
                param.setResult(ret);
            });
            hookAfter(onClickMethod, param -> {
                int InvokeID = (int) param.args[0];
                Context mContext = (Context) param.args[1];
                Object chatMsg = param.args[2];
                if (InvokeID == 4192) {
                    String PTTPath = MethodUtils.callNoParamsMethod(chatMsg, "getLocalFilePath", String.class);
                    //保存语音
                    VoiceTools.createSaveVoiceDialog(mContext, PTTPath);
                }
            });
        }

    }

    public Method findMessageMenu(Class<?> clz) {
        for (Method med : clz.getDeclaredMethods()) {
            if (med.getParameterTypes().length == 1) {
                if (med.getParameterTypes()[0] == View.class) {
                    Class<?> ReturnClz = med.getReturnType();
                    if (ReturnClz.isArray()) {
                        return med;
                    }
                }
            }
        }
        return null;
    }

}
