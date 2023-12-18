package lin.xposed.hook.item;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import lin.util.ReflectUtils.ClassUtils;
import lin.util.ReflectUtils.FieIdUtils;
import lin.util.ReflectUtils.MethodTool;
import lin.xposed.hook.annotation.HookItem;
import lin.xposed.hook.load.base.BaseSwitchFunctionHookItem;
import lin.xposed.hook.util.ToastTool;

@HookItem("辅助功能/实验功能/删除主页聊天列表")
public class DeleteAllChatItemView extends BaseSwitchFunctionHookItem {
    private final HashMap<Object, Integer> viewHolderList = new LinkedHashMap<>();
    private int deleteTextViewId;

    @Override
    public String getTips() {
        return "NT专用_长按主页右上角加号删除,请勿和QA的清理最近聊天一起开启,否则并不会发生什么";
    }

    public void hookGetDeleteViewId() {
        Class<?> superClass = ClassUtils.getClass("com.tencent.qqnt.chats.biz.guild.GuildDiscoveryItemBuilder").getSuperclass();
        Class<?> findClass = null;
        for (Field field : superClass.getDeclaredFields()) {
            field.setAccessible(true);
            Class<?> type = field.getType();
            if (type.getName().startsWith("com.tencent.qqnt.chats.core.adapter.")) {
                findClass = type;
                break;
            }
        }
        Method method = MethodTool.find(findClass).params(android.view.ViewGroup.class, java.util.List.class).returnType(List.class).get();
        hookAfter(method, param -> {
            if (deleteTextViewId != 0) return;
            List<View> viewList = (List<View>) param.getResult();
            for (View view : viewList) {
                if (view instanceof TextView textView) {
                    if (textView.getText().toString().equals("删除")) {
                        deleteTextViewId = textView.getId();
                        break;
                    }
                }
            }
        });

    }

    @Override
    public void loadHook(ClassLoader classLoader) throws Exception {
        hookGetDeleteViewId();
        //不hook onCreate方法了 那样需要重启才能生效 hook onResume可在界面重新渲染到屏幕时会调用生效
        Method onCreateMethod = MethodTool.find("com.tencent.mobileqq.activity.home.Conversation").name("onResume").params(boolean.class).get();
        hookAfter(onCreateMethod, param -> {
            ImageView imageView = FieIdUtils.getFirstField(param.thisObject, ImageView.class);
            imageView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    new Thread(new DeleteAllItemTask()).start();
                    return true;
                }
            });
        });

    }

    private void hookOnHolder() {
        //find
        Class<?> recentContactItemHolderClass = ClassUtils.getClass("com.tencent.qqnt.chats.core.adapter.holder.RecentContactItemHolder");
        Method onHolderBindTimeingCallSetOnClickMethod = null;
        for (Method method : recentContactItemHolderClass.getDeclaredMethods()) {
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length == 3) {
                if (paramTypes[0].getName().startsWith("com.tencent.qqnt.chats.core.adapter.builder.") && paramTypes[1].getName().startsWith("com.tencent.qqnt.chats.core.adapter.") && paramTypes[2] == int.class) {
                    method.setAccessible(true);
                    onHolderBindTimeingCallSetOnClickMethod = method;
                    break;
                }
            }
        }
        //hook
        hookBefore(onHolderBindTimeingCallSetOnClickMethod, param -> {
            int adapterIndex = (int) param.args[2];
            Object item = param.args[1];
            //Holder在前 索引在后 因为Holder在复用池中所以引用地址不会变 但是索引在Adapter中是随时变化的
            viewHolderList.put(param.thisObject, adapterIndex);
        });

    }

    private class DeleteAllItemTask implements Runnable {
        private static final AtomicReference<Method> deleteMethod = new AtomicReference<>();
        private static Class<?> utilType;
        private static Field itemField;

        private Object findItemField(Object viewHolder) throws IllegalAccessException {
            if (itemField != null) return itemField.get(viewHolder);
            for (Field field : viewHolder.getClass().getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object fieldObj = field.get(viewHolder);
                    if (fieldObj == null) continue;
                    String toStr = fieldObj.toString();
                    if (toStr.contains("RecentContactChatItem")) {
                        field.setAccessible(true);
                        itemField = field;
                        break;
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            return itemField.get(viewHolder);
        }

        private Class<?> findUtilClassType(Object viewHolder) {
            if (utilType != null) return utilType;
            for (Field field : viewHolder.getClass().getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object fieldObj = field.get(viewHolder);
                    if (fieldObj == null) continue;
                    if (fieldObj.getClass().getName().startsWith("com.tencent.qqnt.chats.core.ui.ChatsListVB$")) {
                        utilType = field.getType();
                        break;
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            return utilType;
        }

        private Method getDeleteMethod(Object viewHolder) {
            if (deleteMethod.get() != null) return deleteMethod.get();
            Class<?> findClass = findUtilClassType(viewHolder);
            if (findClass == null) throw new RuntimeException("findClass is null");
            Method finalDeleteMethod = MethodTool.find(findClass).params(int.class,//index ?
                    Object.class,//item
                    ClassUtils.getClass("com.tencent.qqnt.chats.core.adapter.holder.RecentContactItemBinding"),//view binder
                    int.class//click view id
            ).returnType(void.class).get();
            deleteMethod.set(finalDeleteMethod);
            return deleteMethod.get();
        }

        @Override
        public void run() {
            final AtomicBoolean isStop = new AtomicBoolean(false);
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    isStop.set(true);
                }
            };
            //在一秒内尽量删除
            Timer timer = new Timer();
            timer.schedule(task, 1000);

            ToastTool.show("开始清理");
            while (!isStop.get()) {
                int size = viewHolderList.size();
                if (size == 0) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(50);
                        continue;
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                Iterator<Map.Entry<Object, Integer>> iterator = viewHolderList.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Object, Integer> viewHolderEntry = iterator.next();
                    try {
                        Object recentContactItemHolder = viewHolderEntry.getKey();
                        //delete util
                        Object util = FieIdUtils.getFirstField(recentContactItemHolder, findUtilClassType(recentContactItemHolder));//util run time obj
                        int adapterIndex = viewHolderEntry.getValue();//call param 1
                        Object itemInfo = findItemField(recentContactItemHolder);//call param 2
                        Object itemBinder = FieIdUtils.getFirstField(recentContactItemHolder, ClassUtils.getClass("com.tencent.qqnt.chats.core.adapter.holder.RecentContactItemBinding"));//call param 3
                        int viewId = deleteTextViewId;//call param 4
                        getDeleteMethod(recentContactItemHolder).invoke(util, adapterIndex, itemInfo, itemBinder, viewId);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    iterator.remove();
                }
            }
            ToastTool.show("已清理");

        }
    }
}
