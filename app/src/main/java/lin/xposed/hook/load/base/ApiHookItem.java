package lin.xposed.hook.load.base;

/**
 * API类 不用作功能 只对接功能
 */
public abstract class ApiHookItem extends BaseHookItem {

    @Override
    public boolean isLoadedByDefault() {
        return true;
    }
}
