package lin.xposed.hook.load;


import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import lin.app.main.ModuleBuildInfo;
import lin.util.ReflectUtils.ReflectException;
import lin.xposed.common.config.SimpleConfig;
import lin.xposed.common.utils.FileUtils;
import lin.xposed.hook.HookEnv;
import lin.xposed.hook.load.base.BaseHookItem;
import lin.xposed.hook.load.base.BaseSwitchFunctionHookItem;
import lin.xposed.hook.util.LogUtils;
import lin.xposed.hook.util.PathTool;
import lin.xposed.hook.util.ToastTool;
import top.linl.annotationprocessor.AnnotationClassNameTools;

//扫描hook项目并加载
public class HookItemLoader {

    public static final HashMap<Class<?>, BaseHookItem> HookInstance = new LinkedHashMap<>();

    /**
     * 方法数据已更新
     */
    private static final AtomicBoolean methodDataUpdate = new AtomicBoolean();

    static {
        //扫描hook类
        try {
            ClassLoader classLoader = HookItemLoader.class.getClassLoader();
            if (classLoader == null)
                throw new ReflectException("HookItemLoader.class.getClassLoader() == null");
            Class<?> hookItemNameClass = classLoader.loadClass(AnnotationClassNameTools.getClassName());
            BaseHookItem[] baseHookItemList = (BaseHookItem[]) hookItemNameClass.getMethod("initAndGetHookItemList").invoke(null);
            for (BaseHookItem baseHookItem : baseHookItemList) {
                HookInstance.put(baseHookItem.getClass(), baseHookItem);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 初始化hook类
     */
    public static void initHookItem() {
        //对所有的项目做想做的事
        for (Map.Entry<Class<?>, BaseHookItem> hookItemEntry : HookInstance.entrySet()) {
            Class<?> hookItemClass = hookItemEntry.getKey();
            BaseHookItem hookItemInstance = hookItemEntry.getValue();
            try {
                /*
                 * 是默认加载的话那就直接加载(比如设置页入口注入)
                 * (前提不是可开关的功能类 不然关闭了还加载就...)
                 * Class.isAssignableFrom判断左边是不是右边的父类
                 */
                if (hookItemInstance.isLoadedByDefault() && !BaseSwitchFunctionHookItem.class.isAssignableFrom(hookItemClass)) {
                    hookItemInstance.startLoadHook();
                }
            } catch (Exception e) {
                hookItemInstance.getExceptionCollectionToolInstance().addException(e);
            }
        }
        SettingLoader.loadSetting();//加载本地数据
    }

    public static boolean methodDataIsOutOfDate() {
        if (methodDataUpdate.get()) {
            return methodDataUpdate.get();
        }
        SimpleConfig config = new SimpleConfig("BaseConfig");
        String moduleVersionAndHostAppVersion = ModuleBuildInfo.moduleVersionName + ":" + HookEnv.getVersionName();
        String oldData = config.get("moduleVersionAndHostAppVersion");
        boolean dataIsOutOfDate = moduleVersionAndHostAppVersion.equals(oldData);
        methodDataUpdate.set(dataIsOutOfDate);
        return dataIsOutOfDate;
    }


    public static class SettingLoader {
        public static final String IS_ENABLED = "是否开启";
        public static final String BYPASS_DEFAULT_LOAD = "绕过默认加载";
        //字符流可以节省一定的性能 并且文件肉眼可读性也比序列化高
        private static JSONObject dataList;

        private static String getDataPath() {
            return PathTool.getModuleDataPath() + "/data/item";
        }

        /**
         * 移出默认加载
         * 通知默认加载文件 该hook item已经不用管理默认加载
         */
        public static void moveOutDefaultLoad(BaseSwitchFunctionHookItem hookItem) {
            if (hookItem == null) return;
            SimpleConfig defaultLoadConfig = new SimpleConfig("DefaultLoad");
            defaultLoadConfig.put(hookItem.getItemPath() + "_" + BYPASS_DEFAULT_LOAD, true);
            defaultLoadConfig.submit();
        }

        /**
         * 获取该item是否被跳过了默认加载
         */
        public static boolean getDefaultLoadState(BaseSwitchFunctionHookItem hookItem) {
            SimpleConfig defaultLoadConfig = new SimpleConfig("DefaultLoad");
            Boolean skipDefaultLoad = defaultLoadConfig.get(hookItem.getItemPath() + "_" + BYPASS_DEFAULT_LOAD);
            if (skipDefaultLoad == null) return false;
            return skipDefaultLoad;
        }

        public static void saveData(BaseSwitchFunctionHookItem theHookItem) {
            moveOutDefaultLoad(theHookItem);
            if (dataList == null) dataList = new JSONObject();
            for (Map.Entry<Class<?>, BaseHookItem> hookItemEntry : HookInstance.entrySet()) {
                BaseHookItem itemInstance = hookItemEntry.getValue();
                //只保存能开关的数据
                if (BaseSwitchFunctionHookItem.class.isAssignableFrom(itemInstance.getClass())) {
                    try {
                        BaseSwitchFunctionHookItem hookItem = (BaseSwitchFunctionHookItem) itemInstance;
                        JSONObject data = new JSONObject();
                        data.put(IS_ENABLED, hookItem.isEnabled());
                        dataList.put(hookItem.getItemPath(), data);
                    } catch (Exception e) {
                        itemInstance.getExceptionCollectionToolInstance().addException(e);
                    }
                }
            }
            FileUtils.writeTextToFile(getDataPath(), dataList.toString(), false);
        }


        public static void loadSetting() {
            //文件不存在那么保存一次
            if (!new File(getDataPath()).exists()) {
                saveData(null);
            }
            //读取设置
            try {
                dataList = new JSONObject(FileUtils.readFileText(getDataPath()));
            } catch (Exception e) {
                //如果配置出现修改导致出错则保存一次
                saveData(null);
                LogUtils.addError(e);
                ToastTool.show("[QStory]加载设置失败qwq " + e);
                return;
            }
            //加载设置
            for (Map.Entry<Class<?>, BaseHookItem> hookItemEntry : HookInstance.entrySet()) {
                BaseHookItem itemInstance = hookItemEntry.getValue();
                //只加载能开关的数据
                if (BaseSwitchFunctionHookItem.class.isAssignableFrom(itemInstance.getClass())) {
                    BaseSwitchFunctionHookItem hookItem = (BaseSwitchFunctionHookItem) itemInstance;
                    if (hookItem.isLoadedByDefault() && !getDefaultLoadState(hookItem)) {
                        try {
                            hookItem.setEnabled(true);
                            hookItem.startLoadHook();
                            continue;
                        } catch (Exception e) {
                            hookItem.getExceptionCollectionToolInstance().addException(e);
                        }
                    }
                    //没有可能是新加的功能 略过
                    if (dataList.isNull(hookItem.getItemPath())) continue;
                    try {
                        JSONObject data = dataList.getJSONObject(hookItem.getItemPath());
                        if (data.getBoolean(IS_ENABLED)) {
                            boolean enabled = data.getBoolean(IS_ENABLED);
                            hookItem.setEnabled(enabled);
                            hookItem.startLoadHook();
                        }
                    } catch (Exception e) {
                        hookItem.getExceptionCollectionToolInstance().addException(e);
                    }

                }
            }
        }

    }


}
