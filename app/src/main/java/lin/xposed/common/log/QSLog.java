package lin.xposed.common.log;

import lin.xposed.hook.load.HookItemLoader;
import lin.xposed.hook.load.base.BaseHookItem;

public class QSLog {
    public static void e(Class<? extends BaseHookItem> clz, Throwable throwable) {
        BaseHookItem baseHookItem = HookItemLoader.HookInstance.get(clz);
        baseHookItem.getExceptionCollectionToolInstance().addException(throwable);
    }
}
