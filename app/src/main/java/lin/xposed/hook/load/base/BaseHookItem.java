package lin.xposed.hook.load.base;


import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedBridge;
import lin.xposed.BuildConfig;
import lin.xposed.hook.util.LogUtils;

/*
 * 不要在此类写构造方法 子类不重写的话反射实例化会报异常
 * 如果要求子类重写这在以后会增加一定的工作量
 */
public abstract class BaseHookItem {

    private String itemPath;

    private boolean hasUiPath = true;
    private ExceptionCollectionTool exceptionCollectionTool;


    /**
     * 获取异常收集工具实例
     */
    public final ExceptionCollectionTool getExceptionCollectionToolInstance() {
        if (exceptionCollectionTool != null) return exceptionCollectionTool;
        return exceptionCollectionTool = new ExceptionCollectionTool();
    }

    public boolean hasPath() {
        return hasUiPath;
    }

    public void setHasUiPath(boolean isHasPath) {
        this.hasUiPath = isHasPath;
    }
    /**
     * 是默认加载
     */
    public boolean isLoadedByDefault() {
        return false;
    }

    /**
     * 加载代码
     */
    public abstract void loadHook(ClassLoader classLoader) throws Exception;

    /*
     * 初始化项目路径
     */
    public final void initPath(String path) {
        this.itemPath = path;
    }

    /*
     * 获取项目完整路径 不允许子类重写
     */

    public final String getItemPath() {
        return this.itemPath;
    }

    /**
     * 异常收集工具
     * 统计的异常包含 查找不到方法 , 版本不匹配 , 方法执行异常等...
     */
    public static final class ExceptionCollectionTool {
        /**
         * 异常列表
         */
        private List<Throwable> ErrorList;

        /**
         * 获取异常列表
         */
        public List<Throwable> getErrorList() {
            return this.ErrorList;
        }

        /**
         * 添加异常到异常列表
         */
        public void addException(Throwable exception) {
            if (this.ErrorList == null) this.ErrorList = new ArrayList<>();
            this.ErrorList.add(exception);
            //如果是debug则直接写到文件
            if (!BuildConfig.DEBUG) {
                XposedBridge.log(exception);
            }
            if (BuildConfig.DEBUG) LogUtils.addError(exception);
        }

        /**
         * 是否有有异常
         */
        public boolean hasException() {
            if (ErrorList == null) return false;
            return !ErrorList.isEmpty();
        }
    }


}
