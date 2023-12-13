package lin.xposed.common.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.ColorInt;

/**
 * @author 言子楪世suzhelan asmclk@163.com
 */
public class ViewUtils {

    /**
     * 标题适配状态栏
     * 请勿设置 android:fitsSystemWindows="true" 或 setFitsSystemWindows(true)
     *
     * @param titleBar 标题布局
     */
    public static void titleBarAdaptsToStatusBar(ViewGroup titleBar) {
        Context context = titleBar.getContext();
        requestTranslucentStatusBar((Activity) context);
        //获取状态栏高度
        int statusBarHeight = 0;
        @SuppressLint({"DiscouragedApi", "InternalInsetResource"})
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
        }
        //适配高度
        ViewGroup.LayoutParams params = titleBar.getLayoutParams();
        params.height += statusBarHeight;
        //模拟setFitsSystemWindows(ture)填充
        titleBar.setPadding(titleBar.getPaddingLeft(), titleBar.getPaddingTop() + statusBarHeight, titleBar.getPaddingRight(), titleBar.getPaddingBottom());
    }


    public static void requestTranslucentStatusBar(Activity activity) {
        Window window = activity.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
    }

    public static class BackgroundBuilder {

        /**
         * 创建背景
         *
         * @param color  填充色
         * @param radius 圆角角度 默认px
         */
        public static GradientDrawable createBaseBackground(@ColorInt int color, int radius) {
            return createRoundedBackground(color, 0, 0, radius);
        }

        /**
         * 创建背景
         *
         * @param color       填充色
         * @param strokeColor 线条颜色
         * @param strokeWidth 线条宽度  单位px
         * @param radius      圆角角度
         */
        public static GradientDrawable createRoundedBackground(@ColorInt int color, @ColorInt int strokeColor, int strokeWidth, int radius) {
            GradientDrawable radiusBg = new GradientDrawable();
            //设置Shape类型
            radiusBg.setShape(GradientDrawable.RECTANGLE);
            //设置填充颜色
            radiusBg.setColor(color);
            //设置线条粗心和颜色,px
            if (strokeColor != 0 && strokeWidth != 0) radiusBg.setStroke(strokeWidth, strokeColor);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                radiusBg.setCornerRadius(radius);
            }
            return radiusBg;
        }

        /**
         * 创建背景颜色
         *
         * @param color       填充色
         * @param strokeColor 线条颜色
         * @param strokeWidth 线条宽度  单位px
         * @param radius      角度  px,长度为4,分别表示左上,右上,右下,左下的角度
         */
        public static GradientDrawable createRectangleDrawable(@ColorInt int color, @ColorInt int strokeColor, int strokeWidth, float[] radius) {
            try {
                GradientDrawable radiusBg = new GradientDrawable();
                //设置Shape类型
                radiusBg.setShape(GradientDrawable.RECTANGLE);
                //设置填充颜色
                radiusBg.setColor(color);
                //设置线条粗心和颜色,px
                if (strokeColor != 0 && strokeWidth != 0)
                    radiusBg.setStroke(strokeWidth, strokeColor);
                //每连续的两个数值表示是一个角度,四组:左上,右上,右下,左下
                if (radius != null && radius.length == 4) {
                    radiusBg.setCornerRadii(new float[]{radius[0], radius[0], radius[1], radius[1], radius[2], radius[2], radius[3], radius[3]});
                }
                return radiusBg;
            } catch (Exception e) {
                return new GradientDrawable();
            }
        }
    }


}
