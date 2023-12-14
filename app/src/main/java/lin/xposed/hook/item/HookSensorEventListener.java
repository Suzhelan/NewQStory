package lin.xposed.hook.item;

import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.CountDownTimer;

import java.lang.reflect.Method;
import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import lin.util.ReflectUtils.ClassUtils;
import lin.util.ReflectUtils.MethodUtils;
import lin.xposed.hook.load.base.BaseHookItem;

/**
 * 该项已废弃 因为hook ClassLoader太过消耗性能 直到找到合适的实现方法时会开放
 */
@Deprecated(since = "弃用")
//@HookItem(value = "禁止启动时触发摇一摇跳转到其他软件", hasPath = false)
public class HookSensorEventListener extends BaseHookItem {
    private final long TOTAL_TIME = 1000 * 10;
    private final long ON_TICK_TIME = 1000;
    private final ArrayList<XC_MethodHook.Unhook> unhooks = new ArrayList<>();
    /**
     * CountDownTimer 实现倒计时
     */
    private final CountDownTimer countDownTimer = new CountDownTimer(TOTAL_TIME/*总时长*/,
            ON_TICK_TIME/*触发onTick的间隔*/) {

        @Override
        public void onTick(long l) {

        }

        @Override
        public void onFinish() {
            //10秒后启动页结束不再继续拦截 解开所有方法钩
            for (XC_MethodHook.Unhook unhook : unhooks) {
                unhook.unhook();
            }
        }
    };

    @Override
    public void loadHook(ClassLoader loader) {
        //开始倒计时
        countDownTimer.start();
        Method allClassMethod = MethodUtils.findMethod(ClassLoader.class, "loadClass", Class.class, new Class[]{String.class});
        unhooks.add(XposedBridge.hookMethod(allClassMethod, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Class<?> clz = (Class<?>) param.getResult();
                if (clz == null) return;
                //排除常用类提高性能
                if (ClassUtils.isCommonlyUsedClass(clz.getName())) return;
                //获取类实现的接口列表 因为没法直接hook接口的抽象方法
                Class<?>[] interfacesList = clz.getInterfaces();
                if (interfacesList != null) {
                    for (Class<?> interfaces : interfacesList) {
                        //判断该类是否实现了这个接口
                        if (interfaces == SensorEventListener.class) {
                            Method onSensorChanged = MethodUtils.findUnknownReturnTypeMethod(clz, "onSensorChanged", new Class[]{SensorEvent.class});
                            unhooks.add(XposedBridge.hookMethod(onSensorChanged, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    param.setResult(null);//beforeHookedMethod时调用会终止方法
                                }
                            }));
                        }
                    }
                }
            }
        }));
    }

    @Override
    public boolean isLoadedByDefault() {
        return true;
    }
}
