package lin.xposed.hook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import lin.util.ReflectUtils.ClassUtils;
import lin.util.ReflectUtils.ConstructorUtils;
import lin.util.ReflectUtils.FieldUtils;
import lin.util.ReflectUtils.MethodUtils;
import lin.xposed.R;
import lin.xposed.common.utils.ActivityTools;
import lin.xposed.hook.annotation.HookItem;
import lin.xposed.hook.load.base.BaseHookItem;
import lin.xposed.hook.util.LogUtils;
import lin.xposed.hook.view.main.MainSettingActivity;

@HookItem(value = "注入QQ设置页面", hasPath = false)
public class QQSettingInject extends BaseHookItem {

    private void hook_QQ_8970_Setting() throws Exception {

        //QQNT 8.9.70+ Setting item inject
        Method method = MethodUtils.findMethod("com.tencent.mobileqq.setting.main.MainSettingConfigProvider", null, List.class, new Class[]{Context.class});
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Context context = (Context) param.args[0];
                ActivityTools.injectResourcesToContext(context);

                //获取方法的返回结果 item组包装器List-结构和当前类的DemoItemGroupWraper类似
                Object result = param.getResult();
                List<Object> itemGroupWraperList = (List<Object>) result;
                //获取返回的集合泛类型
                Class<?> itemGroupWraperClass = itemGroupWraperList.get(0).getClass();
                //循环包装器组集合 目的是获取里面的元素
                for (Object wrapper : itemGroupWraperList) {
                    try {
                        //获取包装器里实际存放的Item集合
                        List<Object> itemList = FieldUtils.getFirstField(wrapper, List.class);
                        //筛选
                        if (itemList == null || itemList.isEmpty()) continue;
                        String name = itemList.get(0).getClass().getName();

                        if (!name.startsWith("com.tencent.mobileqq.setting.processor")) continue;
                        //获取itemList的首个元素并取得Class
                        Class<?> itemClass = itemList.get(0).getClass();
                        //新建自己的Item
                        Object mItem = ConstructorUtils.newInstance(itemClass, new Class[]{Context.class, int.class, CharSequence.class, int.class}, context, 0x520a, context.getString(R.string.app_name), R.mipmap.ic_launcher_round);

                        Method[] setOnClickMethods = MethodUtils.fuzzyLookupMethod(itemClass, new MethodUtils.FuzzyLookupConditions() {
                            @Override
                            public boolean isItCorrect(Method currentMethod) {
                                //在这个类查找所有符合 public void ?(Function0 function0)的方法 可以查找到两个 一个是点击事件 一个是item刚被初始化时的事件
                                return currentMethod.getReturnType() == void.class && (currentMethod.getParameterTypes().length == 1 && currentMethod.getParameterTypes()[0].equals(ClassUtils.getClass("kotlin.jvm.functions.Function0")));
                            }
                        });
                        //动态代理设置事件
                        Object onClickListener = Proxy.newProxyInstance(ClassUtils.getHostLoader(), new Class[]{ClassUtils.getClass("kotlin.jvm.functions.Function0")}, new mOnClickListener(context, itemClass));
                        for (Method setOnClickMethod : setOnClickMethods) {
                            setOnClickMethod.invoke(mItem, onClickListener);
                        }

//                    itemList.add(0,mItem);//add在这里就会和qa放一块 但是我觉得( )的人应该放最上面 所以继续走下面的代码

                        //新建类似包装器里的itemList的list用来存放自己的mItem
                        List<Object> mItemGroup = new ArrayList<>();
                        mItemGroup.add(mItem);
                        //按长度获取item包装器的构造器
                        Constructor<?> itemGroupWraperConstructor = ConstructorUtils.findConstructorByParamLength(itemGroupWraperClass, 5);
                        //新建包装器实例并添加到返回结果
                        Object itemGroupWrap = itemGroupWraperConstructor.newInstance(mItemGroup, null, null, 6, null);
                        itemGroupWraperList.add(0, itemGroupWrap);
                        break;
                    } catch (Exception e) {
                        /*
                         * itemClass可能是com.tencent.mobileqq.setting.processor.b 而不是我们想要的 所以需要判断过滤第一次和catch过滤第二次
                         * 通常此catch由ConstructorUtils找不到构造方法抛出异常以实现第二次过滤
                         */
                    }
                }
            }
        });
    }

    @Override
    public void loadHook(ClassLoader loader) throws Exception {
        try {
            //如果是在QQ版本<8.9.70这个类是查找不到的 就会抛出异常去执行hook_QQ_8970_Setting
            Class<?> settingActivityClass = ClassUtils.getClass("com.tencent.mobileqq.activity.QQSettingSettingActivity");
            hookLegacyQQ(settingActivityClass);
        } catch (Exception e) {
            hook_QQ_8970_Setting();
        }

    }

    public void hookLegacyQQ(Class<?> settingActivityClass) {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Activity activity;
                            if (param.thisObject instanceof Activity) {
                                activity = (Activity) param.thisObject;
                            } else {
                                Method getAct = param.thisObject.getClass().getDeclaredMethod("getActivity");
                                getAct.setAccessible(true);
                                activity = (Activity) getAct.invoke(param.thisObject);
                            }
                            ViewGroup viewGroup = null;
                            Class<?> itemClass = ClassUtils.getClass("com.tencent.mobileqq.widget.FormSimpleItem");
                            Field[] fields = param.thisObject.getClass().getDeclaredFields();
                            for (Field field : fields) {
                                if (field.getType() == itemClass) {
                                    try {
                                        field.setAccessible(true);
                                        View itemView = (View) field.get(param.thisObject);//获得其中一个item view
                                        viewGroup = (ViewGroup) itemView.getParent();//通过item view获取到父布局
                                        //过滤掉另一个com.tencent.mobileqq.widget.FormSimpleItem
                                        if (viewGroup instanceof LinearLayout) {
                                            break;
                                        }
                                    } catch (Exception e) {
                                        throw new RuntimeException("反射获取布局异常 \n" + e);
                                    }
                                }
                            }
                            if (viewGroup == null) {
                                throw new RuntimeException("没有获取到父布局");
                            }
                            //新建自己的item
                            View mItem = (View) itemClass.getConstructor(Context.class).newInstance(activity);
                            //左文本
                            itemClass.getDeclaredMethod("setLeftText", CharSequence.class).invoke(mItem, activity.getString(R.string.app_name));
                            //右文本
                            itemClass.getDeclaredMethod("setRightText", CharSequence.class).invoke(mItem, ">");
                            mItem.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    activity.startActivity(new Intent(activity, MainSettingActivity.class));
                                }
                            });
                            viewGroup.addView(mItem, 0);
                        } catch (Exception e) {
                            throw new RuntimeException("设置入口项没有成功注入" + LogUtils.getStackTrace(e));
                        }
                    }
                });
            }
        };
        XposedHelpers.findAndHookMethod(settingActivityClass, "doOnCreate", Bundle.class, hook);
        try {
            //平板
            XposedHelpers.findAndHookMethod(settingActivityClass, "doOnCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class, hook);
        } catch (Exception e) {

        }
    }

    @Override
    public boolean isLoadedByDefault() {
        return true;
    }

    private class mOnClickListener implements InvocationHandler {

        private final Context QQSettingActivity;

        private final Class<?> itemClass;

        private mOnClickListener(Context qqSettingActivity, Class<?> itemClass) {
            QQSettingActivity = qqSettingActivity;
            this.itemClass = itemClass;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            boolean isEnterModuleActivity = false;
            Throwable throwable = new Throwable();
            StackTraceElement[] stackTraceElements = throwable.getStackTrace();

            // 有被自己聪明到)
            //通过调用栈来确定是不是被指定的方法调用的
            for (StackTraceElement stackTraceElement : stackTraceElements) {
                //判断是不是以此类名开头的内部类再处理 (也可以避免栈中出现此类后loadClass找不到类抛错)
                if (!stackTraceElement.getClassName().startsWith(itemClass.getName())) continue;
                //加载此类
                Class<?> stackClass = ClassUtils.getClass(stackTraceElement.getClassName());
                //获取接口列表
                Class<?>[] interfacesList = stackClass.getInterfaces();
                //判断实现接口和方法
                if (interfacesList[0] == View.OnClickListener.class && stackTraceElement.getMethodName().equals("onClick")) {
                    isEnterModuleActivity = true;
                    break;
                }
            }
            if (isEnterModuleActivity)
                QQSettingActivity.startActivity(new Intent(QQSettingActivity, MainSettingActivity.class));
            return null;
        }

    }

    class DemoItemGroupWrapper {

        public List<DemoItem> demoItemList;
    }

    class DemoItem {

        String text;
        ImageView image;
    }
}
