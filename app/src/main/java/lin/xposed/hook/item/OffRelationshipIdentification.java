package lin.xposed.hook.item;

import android.view.View;
import android.view.ViewParent;
import android.widget.LinearLayout;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import lin.util.ReflectUtils.ClassUtils;
import lin.util.ReflectUtils.FieldUtils;
import lin.util.ReflectUtils.MethodUtils;
import lin.xposed.hook.annotation.HookItem;
import lin.xposed.hook.load.base.BaseSwitchFunctionHookItem;

@HookItem("净化/资料卡/陌生人资料卡你们的关系标识")
public class OffRelationshipIdentification extends BaseSwitchFunctionHookItem {

    @Override
    public void loadHook(ClassLoader loader) throws Exception {
        Method method = MethodUtils.findMethod("com.tencent.mobileqq.profilecard.component.ProfileInStepComponent", "onDataUpdate",
                boolean.class, new Class[]{ClassUtils.getClass("com.tencent.mobileqq.profilecard.data.ProfileCardInfo")});
        hookAfter(method, new HookBehavior() {
            @Override
            public void execute(XC_MethodHook.MethodHookParam param) throws Throwable {
                Object recyclerView = FieldUtils.getFirstField(param.thisObject, ClassUtils.getClass("com.tencent.biz.richframework.widget.listview.card.RFWCardListView"));
                if (recyclerView == null) return;
                LinearLayout parent = MethodUtils.callNoParamsMethod(recyclerView, "getParent", ViewParent.class);
                if (parent == null) {
                    throw new RuntimeException("ParentLayout == null");
                }
                for (int i = 0; i < parent.getChildCount(); i++) {
                    if (parent.getChildAt(i).getVisibility() != View.GONE) {
                        parent.getChildAt(i).setVisibility(View.GONE);
                    }
                }
                if (parent.getVisibility() != View.GONE) parent.setVisibility(View.GONE);
            }
        });
    }


}
