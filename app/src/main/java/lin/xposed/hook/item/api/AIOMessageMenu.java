package lin.xposed.hook.item.api;

import android.content.Context;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.android.AndroidClassLoadingStrategy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import lin.util.ReflectUtils.ClassUtils;
import lin.util.ReflectUtils.MethodTool;
import lin.xposed.hook.HookEnv;
import lin.xposed.hook.QQVersion;
import lin.xposed.hook.annotation.HookItem;
import lin.xposed.hook.load.base.ApiHookItem;
import lin.xposed.hook.load.methodfind.IMethodFinder;
import lin.xposed.hook.load.methodfind.MethodFinder;

@HookItem("API_BuildMenuElement")
public class AIOMessageMenu extends ApiHookItem implements IMethodFinder {
    private static Method method;
    private final String methodId = "Menu_Item_Class";
    /**
     * 创建消息长按菜单元素
     *
     * @param aioMsg   AIOMsgItem
     * @param textName text
     * @param id       id
     * @param callable 调用回调
     * @return item
     */
    public static Object createAIOMenuItemQQNT(Object aioMsg, String textName, int id, Callable<Object> callable) {
        Class<?> msgClass = ClassUtils.getClass("com.tencent.mobileqq.aio.msg.AIOMsgItem");
        Class<?> declaringClass = method.getDeclaringClass();
        //callable method
        Method method = MethodTool.find(declaringClass).returnType(void.class).get();
        Class<?> menuItemClass;
        if (HookEnv.getVersionCode() > QQVersion.QQ_8_9_93) {
            //采用动态字节库ByteBuddy来继承qq的抽象类
            menuItemClass = new ByteBuddy().subclass(declaringClass)
                    //text
                    .method(ElementMatchers.returns(String.class)).intercept(FixedValue.value(textName))
                    //id
                    .method(ElementMatchers.named("b")).intercept(FixedValue.value(id))
                    //method
                    .method(ElementMatchers.is(method)).intercept(MethodCall.call(callable))
                    //res id
                    .method(ElementMatchers.named("a")).intercept(FixedValue.value(id))

                    .make()
                    .load(declaringClass.getClassLoader(),
                            new AndroidClassLoadingStrategy.Wrapping(HookEnv.getHostAppContext().getDir("generated", Context.MODE_PRIVATE)))
                    .getLoaded();
        } else {
            menuItemClass = new ByteBuddy().subclass(declaringClass)
                    //text
                    .method(ElementMatchers.returns(String.class)).intercept(FixedValue.value(textName))
                    //id
                    .method(ElementMatchers.returns(int.class)).intercept(FixedValue.value(id))
                    //click
                    .method(ElementMatchers.is(method)).intercept(MethodCall.call(callable))
                    //build
                    .make()
                    .load(declaringClass.getClassLoader(),
                            new AndroidClassLoadingStrategy.Wrapping(HookEnv.getHostAppContext().getDir("generated", Context.MODE_PRIVATE))
                    ).getLoaded();
        }
        try {
            return menuItemClass.getDeclaredConstructor(msgClass).newInstance(aioMsg);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void loadHook(ClassLoader classLoader) throws Exception {

    }

    @Override
    public void startFind(MethodFinder finder) throws Exception {
        Method[] methods = finder.findMethodString("QQCustomMenuItem{title='");
        for (Method method : methods) {
            if (method.getDeclaringClass().getName().startsWith("com.tencent.qqnt.aio.menu.ui")) {
                finder.putMethod(methodId, method);
                break;
            }
        }
    }

    @Override
    public void getMethod(MethodFinder finder) {
        method = finder.getMethod(methodId);
    }
}
