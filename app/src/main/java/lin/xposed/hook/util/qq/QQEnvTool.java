package lin.xposed.hook.util.qq;

import android.annotation.SuppressLint;
import android.content.res.Resources;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;
import lin.util.ReflectUtils.ClassUtils;
import lin.util.ReflectUtils.MethodUtils;
import lin.xposed.hook.HookEnv;

public class QQEnvTool {

    @SuppressLint("DiscouragedApi")
    public static int findResId(String idName) {
        Resources resources = HookEnv.getHostAppContext().getResources();
        return resources.getIdentifier(idName, "id", HookEnv.getCurrentHostAppPackageName());
    }

    /**
     * uin转peerUid
     */
    public static String getUidFromUin(String uin) {
        Object o = getQRouteApi(ClassUtils.getClass("com.tencent.relation.common.api.IRelationNTUinAndUidApi"));
        return (String) XposedHelpers.callMethod(o, "getUidFromUin", uin);
    }

    /**
     * peerUid转uin
     */
    public static String getUinFromUid(String uid) {
        Object o = getQRouteApi(ClassUtils.getClass("com.tencent.relation.common.api.IRelationNTUinAndUidApi"));
        return (String) XposedHelpers.callMethod(o, "getUinFromUid", uid);
    }

    public static Object getQRouteApi(Class<?> clz) {
        Method api = MethodUtils.findUnknownReturnTypeMethod("com.tencent.mobileqq.qroute.QRoute", "api", new Class[]{Class.class});
        try {
            return api.invoke(null, clz);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static long getLongAccountUin() {
        try {
            Object runTime = getAppRuntime();
            if (runTime == null) return 0;
            return MethodUtils.callMethod(runTime, "getLongAccountUin", long.class, new Class[]{});
        } catch (Exception e) {
            return 0;
        }
    }

    public static String getCurrentUin() {
        try {
            Object runTime = getAppRuntime();
            if (runTime == null) return null;
            return MethodUtils.callMethod(runTime, "getCurrentAccountUin", String.class, new Class[]{});
        } catch (Exception e) {
            return null;
        }
    }

    public static String getPSkey(String url) {
        try {
            Object manager = getManager(2);
            return MethodUtils.callMethod(manager, "getPskey", String.class, new Class[]{String.class, String.class}, getCurrentUin(), url);
        } catch (Exception e) {
            return null;
        }
    }

    public static Object getManager(int i) {
        try {
            Object runTime = getAppRuntime();
            if (runTime == null) return null;
            return MethodUtils.callMethod(runTime, "getManager", ClassUtils.getClass("mqq.manager.Manager"), new Class[]{int.class}, i);
        } catch (Exception e) {
            return null;
        }
    }

    public static Object getAppRuntime() throws Exception {
        Object sApplication = MethodUtils.callStaticMethod(ClassUtils.getClass("com.tencent.common.app.BaseApplicationImpl"),
                "getApplication", ClassUtils.getClass("com.tencent.common.app.BaseApplicationImpl"), new Class[]{});
        return MethodUtils.callMethod(sApplication, "getRuntime", ClassUtils.getClass("mqq.app.AppRuntime"), new Class[]{});
    }

    public static boolean checkQQ(String qq) {
        if (qq == null) return false;
        //先验证是否为5—12位数字
        if (qq.length() < 5 || qq.length() > 12) {
            return false;
        }
        //首位不能是0
        if (qq.charAt(0) == '0') {
            return false;
        }
        //验证每一位数字都在1-9内
        for (int x = 0; x < qq.length(); x++) {
            char ch = qq.charAt(x);
            if (ch < '0' || ch > '9') {
                return false;
            }
        }
        return true;
    }
}
