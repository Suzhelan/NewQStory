package lin.xposed.hook.item.deprecated;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.lang.reflect.Method;

import lin.util.ReflectUtils.MethodTool;
import lin.xposed.hook.load.base.BaseSwitchFunctionHookItem;

@Deprecated(since = "头像圆形和view太难测算 直到找到挂饰的实现方法时再使用")
//@HookItem("辅助功能/季节专属/圣诞帽")
public class SantaHats extends BaseSwitchFunctionHookItem {
    @Override
    protected void loadHook(ClassLoader classLoader) throws Exception {

        Class<?> avatarContainerClass = classLoader.loadClass("com.tencent.mobileqq.aio.widget.AvatarContainer");
        Method getAvatarViewMethod = MethodTool.find("com.tencent.mobileqq.aio.msglist.holder.component.avatar.AIOAvatarContentComponent")
                .returnType(avatarContainerClass)
                .get();
        hookAfter(getAvatarViewMethod, param -> {
            RelativeLayout avatarContainer = (RelativeLayout) param.getResult();
            Context context = avatarContainer.getContext();
            Class<?> commonImageViewClass = classLoader.loadClass("com.tencent.mobileqq.aio.widget.CommonImageView");
            for (int i = 0; i < avatarContainer.getChildCount(); i++) {
                View view = avatarContainer.getChildAt(i);
                if (view.getClass() == commonImageViewClass) {
                    ImageView avatar = (ImageView) view;
                    break;
                }
            }
        });


        /*hookBefore(View.class.getDeclaredMethod("setOnClickListener", View.OnClickListener.class),param -> {
            Throwable throwable = new Throwable();
            View.OnClickListener onClickListener = (View.OnClickListener) param.args[0];
            hookBefore(onClickListener.getClass().getMethod("onClick", View.class),param1 -> {
                LogUtils.addRunLog(LogUtils.getStackTrace(throwable));
            });
        });*/

    }

    @Override
    public boolean isLoadedByDefault() {
        return true;
    }
}
