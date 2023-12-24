package lin.xposed.hook.item;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import lin.util.ReflectUtils.FieldUtils;
import lin.util.ReflectUtils.MethodTool;
import lin.xposed.common.utils.ScreenParamUtils;
import lin.xposed.hook.annotation.HookItem;
import lin.xposed.hook.load.base.BaseSwitchFunctionHookItem;

@HookItem("辅助功能/群聊/群设置页群文件旧版排序")
public class SortTroopSetting extends BaseSwitchFunctionHookItem {

    @Override
    public String getTips() {
        return "将群文件卡片所在位置重新排序到上方";
    }

    @Override
    public void loadHook(ClassLoader classLoader) throws Exception {
        Method doOnCreateMethod = MethodTool.find("com.tencent.mobileqq.troop.troopsetting.activity.TroopSettingActivity")
                .returnType(boolean.class)
                .params(Bundle.class)
                .name("doOnCreate")
                .get();
        hookAfter(doOnCreateMethod, new HookBehavior() {
            @Override
            public void execute(XC_MethodHook.MethodHookParam param) throws Throwable {
                LinearLayout rootView = FieldUtils.getFirstField(param.thisObject, LinearLayout.class);
                int troopInfoTextIndex = 0;
                View troopAppListView = null;
//                View[] views = FieIdUtils.getFirstField(param.thisObject, View[].class);//过于复杂 不如不用

                for (int i = 0; i < rootView.getChildCount(); i++) {
                    View child = rootView.getChildAt(i);
                    if (child instanceof TextView textView) {
                        String text = textView.getText().toString();
                        if (text.equals("群聊信息")) troopInfoTextIndex = i;
                    }
                    if (child instanceof LinearLayout simpleFormItem) {
                        if (simpleFormItem.getChildAt(0) instanceof RelativeLayout itemTitle) {
                            if (itemTitle.getChildAt(0) instanceof TextView titleTextView) {
                                String titleText = titleTextView.getText().toString();
                                if (titleText.equals("群应用")) {
                                    troopAppListView = child;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (troopAppListView != null && troopInfoTextIndex != 0) {
                    rootView.removeView(troopAppListView);
                    //顶部偏移 不然会和群聊成员卡片贴一起 (贴贴
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    layoutParams.topMargin += ScreenParamUtils.dpToPx(rootView.getContext(), 16);
                    rootView.addView(troopAppListView, troopInfoTextIndex, layoutParams);
                }
            }
        });
    }
}
