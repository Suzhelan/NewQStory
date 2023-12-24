package lin.xposed.hook.item;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import java.io.File;

import lin.xposed.R;
import lin.xposed.common.utils.DrawableUtil;
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
    private static final String voiceIconFilePath = PathTool.getModuleDataPath() + "/voice_icon.png";
    private static Drawable voiceFloatingWindowIcon;
    public ListenChatsShowAndHide.OnChatShowListener onChatShowListener = new ListenChatsShowAndHide.OnChatShowListener() {
        @Override
        public void show() {
            if (isEnabled()) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> FloatingWindowsButton.Display(true), 300);
            }
        }

        @Override
        public void hide() {
            if (isEnabled()) {
                new Handler(Looper.getMainLooper()).post(() -> FloatingWindowsButton.Display(false));
            }
        }
    };

    public static Drawable getVoiceFloatingWindowIcon() {
        if (voiceFloatingWindowIcon != null) return voiceFloatingWindowIcon;
        File voiceIconFile = new File(voiceIconFilePath);
        if (voiceIconFile.exists()) {
            voiceFloatingWindowIcon = DrawableUtil.readDrawableFromFile(HookEnv.getHostAppContext(), voiceIconFilePath);
        } else {
            @SuppressLint("UseCompatLoadingForDrawables")
            Drawable icon = HookEnv.getHostAppContext().getDrawable(R.drawable.voice_icon);
            DrawableUtil.drawableToFile(icon, voiceIconFilePath, Bitmap.CompressFormat.PNG);
            voiceFloatingWindowIcon = DrawableUtil.readDrawableFromFile(HookEnv.getHostAppContext(), voiceIconFilePath);
            ToastTool.show("语音图标初始化完成");
        }
        return voiceFloatingWindowIcon;
    }

    @Override
    public String getTips() {
        return "语音保存在" + VOICE_PATH + "单击可以复制";
    }

    @Override
    public View.OnClickListener getViewOnClickListener() {
        return v -> {
            Context context = v.getContext();
            if (MUtils.copyStr(context, VOICE_PATH)) {
                ToastTool.show("复制成功");
            } else {
                ToastTool.show("复制失败");
            }
        };
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void loadHook(ClassLoader classLoader) throws Exception {
        //init icon
        new Thread(AddVoiceFloatingWindow::getVoiceFloatingWindowIcon).start();
        ListenChatsShowAndHide.addOnChatShowListener(onChatShowListener);
    }
}
