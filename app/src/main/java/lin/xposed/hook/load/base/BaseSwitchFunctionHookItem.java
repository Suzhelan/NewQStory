package lin.xposed.hook.load.base;


import android.view.View;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * 可以开关的Hook项目
 */
public abstract class BaseSwitchFunctionHookItem extends BaseHookItem {
    private Boolean enabled = false;

    /**
     * @return 提示文字信息
     */
    public String getTips() {
        return null;
    }

    /**
     * @return 点击item view时被调用
     */
    public View.OnClickListener getViewOnClickListener() {
        return null;
    }

    /**
     * 得到功能开关状态
     *
     * @return 开关状态
     */
    public final boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置功能开关状态
     *
     * @param enabled 开关状态
     */
    public final void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }



    /**
     * 对方法进行 Hook 调用后
     *
     * @param method       targetMethod
     * @param hookBehavior hook
     */
    protected final void hookAfter(Method method, HookBehavior hookBehavior) {
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (isEnabled()) {
                    try {
                        hookBehavior.execute(param);
                    } catch (Throwable e) {
                        getExceptionCollectionToolInstance().addException(e);
                    }
                }
            }
        });
    }

    /**
     * 对方法 Hook 调用前
     *
     * @param method       targetMethod
     * @param hookBehavior hook
     */
    protected final void hookBefore(Method method, HookBehavior hookBehavior) {

        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (isEnabled()) {
                    try {
                        hookBehavior.execute(param);
                    } catch (Throwable e) {
                        getExceptionCollectionToolInstance().addException(e);
                    }
                }
            }
        });
    }

    /**
     * @param priority 当有多个hook作用于同一个方法 这个参数决定 哪个hook先执行 越高执行的优先级越高
     */
    protected final void hookBefore(Method method, HookBehavior hookBehavior, int priority) {
        XposedBridge.hookMethod(method, new XC_MethodHook(priority) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (isEnabled()) {
                    try {
                        hookBehavior.execute(param);
                    } catch (Throwable e) {
                        getExceptionCollectionToolInstance().addException(e);
                    }
                }
            }
        });
    }

    protected final void hookAfter(Method method, HookBehavior hookBehavior, int priority) {
        XposedBridge.hookMethod(method, new XC_MethodHook(priority) {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (isEnabled()) {
                    try {
                        hookBehavior.execute(param);
                    } catch (Throwable e) {
                        getExceptionCollectionToolInstance().addException(e);
                    }
                }
            }
        });
    }

    /**
     * 等同于 new XC_MethodHook() {
     * protected void beforeHookedMethod(MethodHookParam param) {}
     * protected void afterHookedMethod(MethodHookParam param) {}
     * }
     */
    protected interface HookBehavior {
        void execute(XC_MethodHook.MethodHookParam param) throws Throwable;
    }


}
