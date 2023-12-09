package lin.xposed.hook.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import lin.util.ReflectUtils.MethodTool;
import lin.xposed.common.utils.ActivityTools;
import lin.xposed.common.utils.ScreenParamUtils;
import lin.xposed.hook.HookEnv;

public class ToastTool {
    public static void show(Object content) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Context activity = ActivityTools.getActivity();
                try {
                    Object qqToast = MethodTool.find("com.tencent.mobileqq.widget.QQToast")
                            .name("makeText")
                            .params(Context.class, CharSequence.class, int.class)
                            .callStatic(activity, String.valueOf(content), 0);
//                int height = MethodTool.find(ActivityTools.getActivity().getClass()).returnType(int.class).name("getTitleBarHeight").call(activity);//QQ原本的写法
                    int height = ScreenParamUtils.getScreenHeight(HookEnv.getHostAppContext()) / 2;
                    MethodTool.find(qqToast.getClass()).params(int.class).name("show").call(qqToast, height);
                } catch (Exception e) {
                    Toast.makeText(activity, String.valueOf(content), Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
