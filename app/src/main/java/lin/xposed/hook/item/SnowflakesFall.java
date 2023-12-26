package lin.xposed.hook.item;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import lin.xposed.R;
import lin.xposed.common.utils.ActivityTools;
import lin.xposed.common.utils.ScreenParamUtils;
import lin.xposed.hook.annotation.HookItem;
import lin.xposed.hook.load.base.BaseSwitchFunctionHookItem;

@HookItem("辅助功能/季节专属/雪花飘落")
public class SnowflakesFall extends BaseSwitchFunctionHookItem {
    @Override
    public String getTips() {
        return "想要留住雪花 , 可在掌心里 , 只会化的更快";
    }

    /**
     * 复用池 以便activity即使不在焦点也能保存之前view防止重新下雪
     */
    private static final HashMap<Activity, Show> showList = new HashMap<>();
    /**
     * 键盘高度
     */
    private int keyboardHeight = 0;

    private void updateStates(Activity activity, boolean isShow) {
        Show show = showList.get(activity);
        if (show == null) {
            show = new Show();
            ActivityTools.injectResourcesToContext(activity);
            showList.put(activity, show);
        }
        if (isShow) {
            show.showSnowflakesFall(activity);
        } else {
            show.hideSnowflakesFall();
        }
    }

    @Override
    public boolean isLoadedByDefault() {
        return true;
    }

    @Override
    public void loadHook(ClassLoader classLoader) throws Exception {
        //窗口焦点可见时
        hookAfter(Activity.class.getDeclaredMethod("onWindowFocusChanged", boolean.class), param -> {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    boolean hasFocus = (boolean) param.args[0];
                    if (!hasFocus) return;
                    Activity activity = (Activity) param.thisObject;
                    updateStates(activity, true);
                }
            }, 50);
        });
        //通知关闭
        hookBefore(Activity.class.getDeclaredMethod("onPause"), param -> {
            Activity activity = (Activity) param.thisObject;
            updateStates(activity, false);
        });
        //act 销毁 从复用池移出
        hookBefore(Activity.class.getDeclaredMethod("onDestroy"), param -> {
            Activity activity = (Activity) param.thisObject;
            showList.remove(activity);
            //触发gc防止资源不回收
            System.gc();
        });
    }

    public static class FallObject {
        private static final int defaultWindLevel = 0;//默认风力等级
        private static final int defaultWindSpeed = 10;//默认单位风速
        private static final float HALF_PI = (float) Math.PI / 2;//π/2
        private static final int defaultSpeed = 10;//默认下降速度
        private final boolean isAngleChange;//下落物体角度是否改变
        private final boolean isSpeedRandom;//物体初始下降速度比例是否随机
        private final boolean isSizeRandom;//物体初始大小比例是否随机
        public int initWindLevel;//初始风力等级
        public float initSpeed;//初始下降速度
        public float presentSpeed;//当前下降速度
        public float presentX;//当前位置X坐标
        public float presentY;//当前位置Y坐标
        public Builder builder;
        private int initX;
        private int initY;
        private Random random;
        private int parentWidth;//父容器宽度
        private int parentHeight;//父容器高度
        private float objectWidth;//下落物体宽度
        private float objectHeight;//下落物体高度
        private float angle;//下落物体角度

        private Bitmap bitmap;

        public FallObject(Builder builder, int parentWidth, int parentHeight) {
            random = new Random();
            this.parentWidth = parentWidth;
            this.parentHeight = parentHeight;
            initX = random.nextInt(parentWidth);//随机物体的X坐标
            initY = random.nextInt(parentHeight) - parentHeight;//随机物体的Y坐标，并让物体一开始从屏幕顶部下落
            presentX = initX;
            presentY = initY;
            this.builder = builder;
            isSpeedRandom = builder.isSpeedRandom;
            isSizeRandom = builder.isSizeRandom;
            isAngleChange = builder.isAngleChange;
            initWindLevel = builder.initWindLevel;
            randomSpeed();
            randomSize();
        }

        private FallObject(Builder builder) {
            this.builder = builder;
            initSpeed = builder.initSpeed;
            bitmap = builder.bitmap;
            isSpeedRandom = builder.isSpeedRandom;
            isSizeRandom = builder.isSizeRandom;
            isAngleChange = builder.isAngleChange;
            initWindLevel = builder.initWindLevel;
        }

        /**
         * 改变bitmap的大小
         *
         * @param bitmap 目标bitmap
         * @param newW   目标宽度-
         * @param newH   目标高度
         * @return
         */
        public static Bitmap changeBitmapSize(Bitmap bitmap, int newW, int newH) {
            int oldW = bitmap.getWidth();
            int oldH = bitmap.getHeight();
            // 计算缩放比例
            float scaleWidth = ((float) newW) / oldW;
            float scaleHeight = ((float) newH) / oldH;
            // 取得想要缩放的matrix参数
            Matrix matrix = new Matrix();
            matrix.postScale(scaleWidth, scaleHeight);
            // 得到新的图片
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, oldW, oldH, matrix, true);
            return bitmap;
        }

        /**
         * drawable图片资源转bitmap
         *
         * @param drawable
         * @return
         */
        public static Bitmap drawableToBitmap(Drawable drawable) {
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            drawable.draw(canvas);
            return bitmap;
        }

        /**
         * 绘制物体对象
         *
         * @param canvas
         */
        public void drawObject(Canvas canvas) {
            moveObject();
            canvas.drawBitmap(bitmap, presentX, presentY, null);
        }

        /**
         * 移动物体对象
         */
        private void moveObject() {
            moveX();
            moveY();
            if (presentY > parentHeight || presentX < -bitmap.getWidth() || presentX > parentWidth + bitmap.getWidth()) {
                reset();
            }
        }

        /**
         * Y轴上的移动逻辑
         */
        private void moveY() {
            presentY += presentSpeed;
        }

        private void moveX() {
            presentX += defaultWindSpeed * Math.sin(angle);
            if (isAngleChange) {
                angle += (float) (random.nextBoolean() ? -1 : 1) * Math.random() * 0.0025;
            }
        }

        /**
         * 重置object位置
         */
        private void reset() {
            presentY = -objectHeight;
            randomSpeed();
            randomWind();//记得重置一下初始角度，不然雪花会越下越少（因为角度累加会让雪花越下越偏）
        }

        /**
         * 随机风的风向和风力大小比例，即随机物体初始下落角度
         */
        private void randomWind() {
            if (isAngleChange) {
                angle = (float) ((random.nextBoolean() ? -1 : 1) * Math.random() * initWindLevel / 50);
            } else {
                angle = (float) initWindLevel / 50;
            }
            //限制angle的最大最小值
            if (angle > HALF_PI) {
                angle = HALF_PI;
            } else if (angle < -HALF_PI) {
                angle = -HALF_PI;
            }
        }

        /**
         * 随机物体初始下落速度
         */
        private void randomSpeed() {
            if (isSpeedRandom) {
                initSpeed = (float) ((random.nextInt(3) + 1) * 0.1 + 1) * builder.initSpeed;
            } else {
                initSpeed = builder.initSpeed;
            }
            presentSpeed = initSpeed;
        }

        /**
         * 随机物体初始大小比例
         */
        private void randomSize() {
            if (isSizeRandom) {
                float r = (random.nextInt(10) + 1) * 0.1f;
                float rW = r * builder.bitmap.getWidth();
                float rH = r * builder.bitmap.getHeight();
                bitmap = changeBitmapSize(builder.bitmap, (int) rW, (int) rH);
            } else {
                bitmap = builder.bitmap;
            }
            objectWidth = bitmap.getWidth();
            objectHeight = bitmap.getHeight();
        }

        public static final class Builder {
            private float initSpeed;
            private Bitmap bitmap;
            private boolean isSpeedRandom;
            private boolean isSizeRandom;
            private int initWindLevel;//下落物体角度

            private boolean isAngleChange;//下落物体角度是否改变

            public Builder(Bitmap bitmap) {
                this.initSpeed = defaultSpeed;
                this.bitmap = bitmap;
            }

            public Builder(Drawable drawable) {
                this.initSpeed = defaultSpeed;
                this.bitmap = drawableToBitmap(drawable);
            }

            /**
             * 设置物体的初始下落速度
             *
             * @param level
             * @param isAngleChange 物体初始下降速度比例是否随机
             * @return
             */
            public Builder setWind(int level, boolean isAngleChange) {
                this.initWindLevel = level;
                this.isAngleChange = isAngleChange;
                return this;
            }

            /**
             * 设置物体的初始下落速度
             *
             * @param speed
             * @return
             */
            public Builder setSpeed(float speed) {
                this.initSpeed = speed;
                return this;
            }

            /**
             * 设置物体的初始下落速度
             *
             * @param speed
             * @param isRandomSpeed 物体初始下降速度比例是否随机
             * @return
             */
            public Builder setSpeed(float speed, boolean isRandomSpeed) {
                this.initSpeed = speed;
                this.isSpeedRandom = isRandomSpeed;
                return this;
            }

            /**
             * 设置下落物体的大小
             *
             * @param w
             * @param h
             * @return
             */
            public Builder setSize(int w, int h) {
                this.bitmap = changeBitmapSize(this.bitmap, w, h);
                return this;
            }

            /**
             * 设置物体大小
             *
             * @param w
             * @param h
             * @param isRandomSize 物体初始大小比例是否随机
             * @return
             */
            public Builder setSize(int w, int h, boolean isRandomSize) {
                this.bitmap = changeBitmapSize(this.bitmap, w, h);
                this.isSizeRandom = isRandomSize;
                return this;
            }

            /**
             * 构建FallObject
             *
             * @return
             */
            public FallObject build() {
                return new FallObject(this);
            }
        }

    }

    private class Show {

        private final AtomicBoolean isShowing = new AtomicBoolean(false);
        /**
         * 布局监听器
         */
        private ViewTreeObserver.OnGlobalLayoutListener globalLayoutListener;
        /**
         * 窗口管理器
         */
        private WindowManager windowManager;
        /**
         * 雪花View
         */
        private FallingView fallingView;
        /**
         * 悬浮窗布局
         */
        private WindowManager.LayoutParams layoutParams;

        private Activity activity;

        private void hideSnowflakesFall() {
            if (isShowing.getAndSet(false)) {
                //移除监听器 防止性能消耗
                View rootView = activity.getWindow().getDecorView();
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(globalLayoutListener);
                //移除悬浮窗
                windowManager.removeViewImmediate(fallingView);
            }
        }

        private void showSnowflakesFall(Activity activity) {
            if (this.activity == null) this.activity = activity;
            if (windowManager == null) windowManager = activity.getWindowManager();
            if (layoutParams == null) layoutParams = getWindowManagerParams();
            if (fallingView == null) {
                fallingView = new FallingView(activity);
                fallingView.setClickable(false);
                FallObject.Builder builder = new FallObject.Builder(activity.getDrawable(R.drawable.snowflakes));
                int size = ScreenParamUtils.dpToPx(activity, 40);
                FallObject fallObject = builder.setSpeed(8, true).setSize(size, size, true).setWind(5, true).build();
                fallingView.addFallObject(fallObject, 50);
            }
            if (!isShowing.get()) {
                windowManager.addView(fallingView, layoutParams);
                isShowing.set(true);
                View rootView = activity.getWindow().getDecorView();
                //监听键盘
                globalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        boolean mKeyboardUp = isKeyboardShown(rootView);
                        //通知
                        notifiesKeyboardHeightChange(rootView.getContext(), keyboardHeight);
                    }
                };
                rootView.getViewTreeObserver().addOnGlobalLayoutListener(globalLayoutListener);
            }
        }

        /**
         * 监听同时通知键盘高度
         */
        private boolean isKeyboardShown(View rootView) {
            final int softKeyboardHeight = 100;
            Rect rect = new Rect();
            rootView.getWindowVisibleDisplayFrame(rect);
            DisplayMetrics dm = rootView.getResources().getDisplayMetrics();
            keyboardHeight = rootView.getHeight() - rect.bottom;
            int heightDiff = rootView.getBottom() - rect.bottom;
            return heightDiff > softKeyboardHeight * dm.density;
        }

        private WindowManager.LayoutParams getWindowManagerParams() {
            layoutParams = new WindowManager.LayoutParams();
            //只在应用内展示
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    //不覆盖点击事件
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            layoutParams.format = PixelFormat.RGBA_8888;
            //布局大小
            layoutParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
            return layoutParams;
        }

        /**
         * 通知键盘高度已更新 防止雪花悬浮窗盖住键盘导致键盘没反应
         */
        private void notifiesKeyboardHeightChange(Context context, int keyboardHeight) {
            if (keyboardHeight != 0 && layoutParams != null) {
                layoutParams.height = ScreenParamUtils.getScreenHeight(context) - keyboardHeight;
                windowManager.updateViewLayout(fallingView, layoutParams);
            } else if (layoutParams != null) {
                layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
                windowManager.updateViewLayout(fallingView, layoutParams);
            }
        }
    }

    public class FallingView extends View {


        private static final int defaultWidth = 600;//默认宽度
        private static final int defaultHeight = 1000;//默认高度
        private static final int intervalTime = 10;//重绘间隔时间
        private final Context mContext;
        // 重绘线程
        private final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        };
        private AttributeSet mAttrs;
        private int viewWidth;
        private int viewHeight;
        private List<FallObject> fallObjects;

        public FallingView(Context context) {
            super(context);
            mContext = context;
            init();
        }

        public FallingView(Context context, AttributeSet attrs) {
            super(context, attrs);
            mContext = context;
            mAttrs = attrs;
            init();
        }

        private void init() {
            fallObjects = new ArrayList<>();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            int height = measureSize(defaultHeight, heightMeasureSpec);
            int width = measureSize(defaultWidth, widthMeasureSpec);
            setMeasuredDimension(width, height);

            viewWidth = width;
            viewHeight = height;
        }

        private int measureSize(int defaultSize, int measureSpec) {
            int result = defaultSize;
            int specMode = MeasureSpec.getMode(measureSpec);
            int specSize = MeasureSpec.getSize(measureSpec);

            if (specMode == MeasureSpec.EXACTLY) {
                result = specSize;
            } else if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
            return result;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (fallObjects.size() > 0) {
                for (int i = 0; i < fallObjects.size(); i++) {
                    //然后进行绘制
                    fallObjects.get(i).drawObject(canvas);
                }
                // 隔一段时间重绘一次, 动画效果
                getHandler().postDelayed(runnable, intervalTime);
            }
        }

        /**
         * 向View添加下落物体对象
         *
         * @param fallObject 下落物体对象
         * @param num        view.getViewTreeObserver().addOnPreDrawListener(opdl)
         *                   此方法在视图绘制前会被调用，测量结束，客户获取到一些数据。再计算一些动态宽高时可以使用。
         *                   调用一次后需要注销这个监听，否则会阻塞ui线程。
         */
        public void addFallObject(final FallObject fallObject, final int num) {
            getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    getViewTreeObserver().removeOnPreDrawListener(this);
                    for (int i = 0; i < num; i++) {
                        FallObject newFallObject = new FallObject(fallObject.builder, viewWidth, viewHeight);
                        fallObjects.add(newFallObject);
                    }
                    invalidate();
                    return true;
                }
            });
        }
    }


}
