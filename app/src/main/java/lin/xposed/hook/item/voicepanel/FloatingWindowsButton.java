package lin.xposed.hook.item.voicepanel;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import java.util.concurrent.atomic.AtomicBoolean;

import lin.widget.dialog.base.MDialog;
import lin.xposed.R;
import lin.xposed.common.config.SimpleConfig;
import lin.xposed.common.utils.ActivityTools;
import lin.xposed.common.utils.ScreenParamUtils;
import lin.xposed.common.utils.ViewUtils;


//添加悬浮窗
public class FloatingWindowsButton {
    public final static String TAG = "FloatingWindows";
    private static final AtomicBoolean isShowing = new AtomicBoolean();
    @SuppressLint("StaticFieldLeak")
    private static Activity LastActivity;
    private static WindowManager.LayoutParams layoutParams;
    private static WindowManager windowManager;
    @SuppressLint("StaticFieldLeak")
    private static ImageView floatingButton;

    @SuppressLint("ClickableViewAccessibility")
    public synchronized static void Display(boolean isShow) {
        Activity activity = ActivityTools.getActivity();
        ActivityTools.injectResourcesToContext(activity);
        try {
            if (isShow) {
                if (activity == LastActivity) {
                    if (!isShowing.getAndSet(true)) {
                        windowManager.addView(floatingButton, getWindowManagerParams(activity));
                    }
                } else {
                    if (isShowing.getAndSet(false)) {
                        windowManager.removeViewImmediate(floatingButton);
                    }
                    windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
                    initFloatingButton(activity);
                    windowManager.addView(floatingButton, getWindowManagerParams(activity));
                    isShowing.set(true);
                    LastActivity = activity;
                }
            } else {
                if (windowManager != null && isShowing.getAndSet(false)) {
                    windowManager.removeViewImmediate(floatingButton);
                    //关闭时保存展示位置
                    SimpleConfig config = new SimpleConfig(TAG);
                    config.put("show x", layoutParams.x);
                    config.put("show y", layoutParams.y);
                    config.submit();
                }
            }
        } catch (Exception e) {

        }
    }


    //初始化显示的悬浮窗控件
    @SuppressLint("ClickableViewAccessibility")
    private static void initFloatingButton(Context context) {
        floatingButton = new ImageView(context);
        floatingButton.setImageDrawable(AddVoiceFloatingWindow.getVoiceFloatingWindowIcon());
        floatingButton.setAdjustViewBounds(true);
        floatingButton.setOnTouchListener(new OnTouchListener());
        floatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        MDialog dialog = new MDialog(context);
                        GradientDrawable dialogBackground = ViewUtils.BackgroundBuilder.createBaseBackground(context.getColor(R.color.露草色), 0);
                        dialogBackground.setAlpha(200);
                        dialogBackground.setCornerRadius(40);
                        dialog.setBackground(dialogBackground);
                        dialog.setOnDismissListener(dialog1 -> floatingButton.setVisibility(View.VISIBLE));
                        dialog.setContentView(VoiceListView.buildView(dialog, context, VoiceListView.VOICE_PATH));
                        dialog.show();
                        floatingButton.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    private static WindowManager.LayoutParams getWindowManagerParams(Context context) {
        if (layoutParams == null) {
            layoutParams = new WindowManager.LayoutParams();
            //只在应用内展示
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            layoutParams.format = PixelFormat.RGBA_8888;
            //布局大小
            layoutParams.width = ScreenParamUtils.dpToPx(context, 30);
            layoutParams.height = ScreenParamUtils.dpToPx(context, 30);
            //获取展示位置配置
            SimpleConfig config = new SimpleConfig(TAG);
            int x = config.get("show x"), y = config.get("show y");
            //初始的展示位置
            layoutParams.x = x != 0 ? x : 50;
            layoutParams.y = y != 0 ? y : 50;
        }
        return layoutParams;
    }

    //监听控件触摸
    private static class OnTouchListener implements View.OnTouchListener {
        int x, y;

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                //手在第一次触摸的时候触发
                case MotionEvent.ACTION_DOWN:
                    x = (int) event.getRawX();
                    y = (int) event.getRawY();
                    break;
                //控件在移动时会多次触发
                case MotionEvent.ACTION_MOVE:
                    int nowX = (int) event.getRawX();
                    int nowY = (int) event.getRawY();
                    int movedX = nowX - x;
                    int movedY = nowY - y;
                    x = nowX;
                    y = nowY;
                    //更新控件布局参数的展示位置
                    layoutParams.x = layoutParams.x + movedX;
                    layoutParams.y = layoutParams.y + movedY;
                    // 更新悬浮窗控件布局
                    windowManager.updateViewLayout(v, layoutParams);
                    break;
                //手离开屏幕时触发
                case MotionEvent.ACTION_UP:

                    break;
                default:
                    break;
            }
            return false;
        }
    }
}
