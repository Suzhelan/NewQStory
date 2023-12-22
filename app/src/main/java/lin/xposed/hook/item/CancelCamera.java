package lin.xposed.hook.item;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.LinearLayout;

import java.lang.reflect.Method;

import lin.util.ReflectUtils.MethodUtils;
import lin.xposed.hook.QQVersion;
import lin.xposed.hook.annotation.HookItem;
import lin.xposed.hook.load.base.BaseSwitchFunctionHookItem;

@HookItem("净化/聊天/精简聊天界面相机按钮")
public class CancelCamera extends BaseSwitchFunctionHookItem {

    @Override
    public void loadHook(ClassLoader loader) throws Exception {

        if (QQVersion.isQQNT()) {
            Class<?> clz = loader.loadClass("com.tencent.qqnt.aio.shortcutbar.PanelIconLinearLayout");
            Method hookMethod = null;
            for (Method method : clz.getDeclaredMethods()) {
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 3 && params[0] == int.class && params[1] == String.class) {
                    hookMethod = method;
                    break;
                }
            }
            if (hookMethod == null) {
                throw new RuntimeException("No method found");
            }
            hookAfter(hookMethod, param -> {
                LinearLayout layout = (LinearLayout) param.thisObject;
                for (int i = 0; i < layout.getChildCount(); i++) {
                    View PaneIconImage = layout.getChildAt(i);
                    CharSequence contentDescription = PaneIconImage.getContentDescription();
                    if (contentDescription != null && contentDescription.equals("拍照")) {
                        layout.removeViewAt(i);
                        break;
                    }
                }
            });
        } else {
            Method method = MethodUtils.findMethod("com.tencent.mobileqq.activity.aio.panel.PanelIconLinearLayout",
                    null, void.class, new Class[]{loader.loadClass("com.tencent.mobileqq.activity.aio.core.BaseChatPie")});
            method.setAccessible(true);
            hookAfter(method, param -> {
                LinearLayout layout = (LinearLayout) param.thisObject;
                //好友和群聊情况
                if (layout.getChildCount() >= 4) {
                    @SuppressLint("ResourceType")
                    View v = layout.getChildAt(2);
                    if (v == null) return;
                    layout.post(() -> layout.removeView(v));
                } else if (layout.getChildCount() >= 2) {
                    layout.post(() -> layout.removeViewAt(1));
                }
            });
        }

    }
}
