package lin.widget.dialog.base;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import lin.xposed.R;

/**
 * @author 言子楪世
 *
 * 2023-8-12
 * 傍晚的风总会吹到你的脸上
 */
public class MDialog extends Dialog {
    public boolean isStop;

    public MDialog(Context context) {
        super(context, R.style.dialog);
    }

    public MDialog(Context context, int R) {
        super(context, R);
    }


    @Override
    protected void onStop() {
        isStop = true;
        super.onStop();
    }

    @Override
    public void show() {
        super.show();
    }

    public void setBackground(Drawable drawable) {
        this.getWindow().setBackgroundDrawable(drawable);
    }

    /**
     * 根据屏幕百分百设置弹窗大小
     *
     * @param width 宽度
     * @param height 高度
     */
    public void setDialogWindowAttr(double width, double height) {
        setDialogWindowAttr((int) (getScreenWidth() * width), (int) (getScreenHeight() * height));
    }


    //在dialog.show()或者setView()之后调用 因为设置完dialog的View 宽高会被子布局改变
    public void setDialogWindowAttr(int width, int height) {
        Window window = this.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.gravity = Gravity.CENTER;
        lp.width = width;
        lp.height = height;
        this.getWindow().setAttributes(lp);
    }

    /**
     * 设置宽和高
     *
     * @param width 宽度
     * @param height 高度
     */
    public void setWidthAndWidth(int width, int height) {
        Window window = this.getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        //dialog宽高适应子布局xml
        params.width = width;
        params.height = height;
        window.setAttributes(params);
    }

    public void setMinWidth(int minWidth) {
        getDialogRootView().setMinimumWidth(minWidth);
    }

    public void setMinHeight(int minHeight) {
        getDialogRootView().setMinimumHeight(minHeight);
    }

    public View getDialogRootView() {
        Window window  = getWindow();
        return window.getDecorView();
    }

    /**
     * 获取手机像素宽度
     *
     * @return 宽度px
     */
    protected int getScreenWidth() {
        return getContext().getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * 获取手机像素高度(不包含虚拟导航栏和状态栏)
     *
     * @return 高度px
     */
    protected int getScreenHeight() {
        return getContext().getResources().getDisplayMetrics().heightPixels;
    }
}

