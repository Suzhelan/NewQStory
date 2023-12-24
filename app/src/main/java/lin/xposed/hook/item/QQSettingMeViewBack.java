package lin.xposed.hook.item;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import lin.xposed.hook.HookEnv;
import lin.xposed.hook.QQVersion;
import lin.xposed.hook.annotation.HookItem;
import lin.xposed.hook.load.base.BaseSwitchFunctionHookItem;

@HookItem("净化/侧滑栏/侧滑栏右上角返回")
public class QQSettingMeViewBack extends BaseSwitchFunctionHookItem {
    @Override
    public void loadHook(ClassLoader loader) throws Exception {
        Class<?> settingView;
        Class<?> resultClass;
        if (HookEnv.getVersionCode() >= QQVersion.QQ_9_0_0) {
            settingView = loader.loadClass("com.tencent.mobileqq.QQSettingMe");
            resultClass = loader.loadClass("com.tencent.mobileqq.QQSettingMeView").getSuperclass();
        } else if (HookEnv.getVersionCode() >= QQVersion.QQ_8_9_90) {
            settingView = loader.loadClass("com.tencent.mobileqq.QQSettingMe");
            resultClass = loader.loadClass("com.tencent.mobileqq.BaseQQSettingMeView");
        } else {
            settingView = loader.loadClass("com.tencent.mobileqq.activity.QQSettingMe");
            resultClass = loader.loadClass("com.tencent.mobileqq.activity.BaseQQSettingMeView");
        }
        Loop:
        for (Class<?> currentClass = settingView; currentClass != null; currentClass = currentClass.getSuperclass()) {
            for (Method method : currentClass.getDeclaredMethods()) {
                method.setAccessible(true);
                if (method.getReturnType() == resultClass) {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            //获取返回对象
                            Object BaseSettingMeView = param.getResult();
                            //获取返回对象的父类
                            Class<?> au = BaseSettingMeView.getClass().getSuperclass().getSuperclass();

                            Field viewGroupField = null;
                            for (Field field : au.getDeclaredFields()) {
                                field.setAccessible(true);
                                //判断类型符合ViewGroup
                                if (field.getType() == ViewGroup.class) {
                                    viewGroupField = field;
                                    break;
                                }
                            }
                            ViewGroup viewGroup = (ViewGroup) viewGroupField.get(BaseSettingMeView);

                            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                                View view = viewGroup.getChildAt(i);
                                if (view instanceof FrameLayout backLayout) {
                                    if (backLayout.getChildCount() == 1) {
                                        if (backLayout.getChildAt(0) instanceof ImageView) {
                                            viewGroup.removeView(backLayout);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    });
                    break Loop;
                }
            }
        }
    }

    @Override
    public String getTips() {
        return "重启生效,该项仅在旧版侧滑栏生效";
    }
}
