package lin.xposed.hook.item;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import lin.xposed.R;
import lin.xposed.common.utils.ActivityTools;
import lin.xposed.common.utils.MUtils;
import lin.xposed.hook.HookEnv;
import lin.xposed.hook.annotation.HookItem;
import lin.xposed.hook.item.api.ListenChatsShowAndHide;
import lin.xposed.hook.item.voicepanel.FloatingWindowsButton;
import lin.xposed.hook.load.base.BaseSwitchFunctionHookItem;
import lin.xposed.hook.util.PathTool;
import lin.xposed.hook.util.ToastTool;

@HookItem("辅助功能/聊天/语音悬浮窗")
public class AddVoiceFloatingWindow extends BaseSwitchFunctionHookItem {
    public static final String VOICE_PATH = PathTool.getModuleDataPath() + "/Voice/";
    private static Drawable voiceFloatingWindowIcon;
    public ListenChatsShowAndHide.OnChatShowListener onChatShowListener = new ListenChatsShowAndHide.OnChatShowListener() {
        @Override
        public void show() {
            if (isEnabled()) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
//                                ToastTool.show("show");
                        FloatingWindowsButton.Display(true);
                    }
                }, 500);
            }
        }

        @Override
        public void hide() {
            if (isEnabled()) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        FloatingWindowsButton.Display(false);
                    }
                });
            }
        }
    };

    public static Drawable getVoiceFloatingWindowIcon() {
        return voiceFloatingWindowIcon;
    }

    @Override
    public String getTips() {
        return "语音保存在" + VOICE_PATH + "单击可以复制";
    }

    @Override
    public View.OnClickListener getViewOnClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = v.getContext();
                if (MUtils.copyStr(context, VOICE_PATH)) {
                    ToastTool.show("复制成功");
                } else {
                    ToastTool.show("复制失败");
                }
            }
        };
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void loadHook(ClassLoader classLoader) throws Exception {
        //init icon
        new Thread(() -> {
            ActivityTools.injectResourcesToContext(HookEnv.getHostAppContext());
            voiceFloatingWindowIcon = HookEnv.getHostAppContext().getDrawable(R.mipmap.ic_launcher_round);
            //add
            ListenChatsShowAndHide.addOnChatShowListener(onChatShowListener);
        }).start();
    }
}
