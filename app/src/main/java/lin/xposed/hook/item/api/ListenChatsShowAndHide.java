package lin.xposed.hook.item.api;

import java.lang.reflect.Method;
import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import lin.xposed.hook.QQVersion;
import lin.xposed.hook.annotation.HookItem;
import lin.xposed.hook.load.base.ApiHookItem;
import lin.xposed.hook.load.methodfind.IMethodFinder;
import lin.xposed.hook.load.methodfind.MethodFinder;

@HookItem("ListenToChatWindowsAsTheyAppearAndClose")
public class ListenChatsShowAndHide extends ApiHookItem implements IMethodFinder {
    private static final ArrayList<OnChatShowListener> onChatShowListenerList = new ArrayList<>();
    private final String showMethodID = "chat_show_method";
    private final String hideMethodID = "chat_hide_method";

    private Method showMethod, hideMethod;

    public static void addOnChatShowListener(OnChatShowListener chatShowListener) {
        if (chatShowListener != null) onChatShowListenerList.add(chatShowListener);
    }

    public static boolean removeChatShowListener(OnChatShowListener onChatShowListener) {
        return onChatShowListenerList.remove(onChatShowListener);
    }

    @Override
    public void loadHook(ClassLoader classLoader) throws Exception {

        if (QQVersion.isQQNT()) {
            // mian chat
            XposedHelpers.findAndHookMethod("com.tencent.qqnt.aio.SplashAIOFragment", classLoader, "onHiddenChanged", boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    boolean isHide = (boolean) param.args[0];
                    if (isHide) {
                        for (OnChatShowListener onChatShowListener : onChatShowListenerList) {
                            onChatShowListener.hide();
                        }
                    } else {
                        for (OnChatShowListener onChatShowListener : onChatShowListenerList) {
                            onChatShowListener.show();
                        }
                    }
                }
            });
        } else {
            //when the chat is show
            XposedBridge.hookMethod(showMethod, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    for (OnChatShowListener onChatShowListener : onChatShowListenerList) {
                        onChatShowListener.show();
                    }
                }
            });
            //when the chat is hidden
            XposedBridge.hookMethod(hideMethod, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    for (OnChatShowListener onChatShowListener : onChatShowListenerList) {
                        onChatShowListener.hide();
                    }
                }
            });
        }
    }

    @Override
    public void startFind(MethodFinder finder) throws Exception {
        String findShowString, findHideString;
        if (QQVersion.isQQNT()) {
            return;
            /*findShowString = "[show]: isAIOShow ";
            findHideString = "[hide]: nick is ";*/
        } else {
            findShowString = "loadBackgroundAsync: skip for mosaic is on";
            findHideString = "doOnStop";
        }
        Method show = finder.findMethodString(findShowString)[0];
        finder.putMethod(showMethodID, show);
        Method[] onStopMethodList = finder.findMethodString(findHideString);
        if (QQVersion.isQQNT()) {
            finder.putMethod(hideMethodID, onStopMethodList[0]);
        } else {
            for (Method m : onStopMethodList) {
                if (m.getDeclaringClass().getName().equals("com.tencent.mobileqq.activity.aio.core.BaseChatPie")) {
                    finder.putMethod(hideMethodID, m);
                    break;
                }
            }
        }
    }

    @Override
    public void getMethod(MethodFinder finder) {
        if (!QQVersion.isQQNT()) {
            showMethod = finder.getMethod(showMethodID);
            hideMethod = finder.getMethod(hideMethodID);
        }
    }


    public interface OnChatShowListener {
        void show();

        void hide();
    }
}
