package lin.xposed.hook;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import lin.util.ReflectUtils.MethodUtils;
import lin.widget.dialog.base.MDialog;
import lin.xposed.R;
import lin.xposed.common.utils.ActivityTools;
import lin.xposed.common.utils.ViewUtils;
import lin.xposed.hook.load.HookItemLoader;
import lin.xposed.hook.load.MethodFindProcessor;
import lin.xposed.hook.util.PathTool;
import lin.xposed.hook.util.ToastTool;


public class HookInit {

    private static final int REQUEST_EXTERNAL_STORAGE = 2376;//请求权限标识码
    private static final String[] PERMISSIONS_STORAGE = {"android.permission.READ_EXTERNAL_STORAGE",//外部读
            "android.permission.WRITE_EXTERNAL_STORAGE"//外部写
    };

    public static void initMainHook() throws Exception {
        //方法数据已过期 开始查找方法
       if (!HookItemLoader.methodDataIsOutOfDate()) {
           //需要有activity才能展示查找方法的动画 所以等activity创建再开始查找
           XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
               @Override
               protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                   Activity activity = (Activity) param.thisObject;
                   ActivityTools.injectResourcesToContext(activity);
                   MethodFindProcessor.startFindAllMethod(activity);
               }
           });
       } else {
            //正常没有过期 扫描本地方法和加载Hook
            MethodFindProcessor.scanMethod();
            HookItemLoader.initHookItem();
        }
    }

    /**
     * 通过创建文件验证读写权限
     *
     * @return 是否有读写权限 有会返回false
     */
    private static boolean verifyStoragePermissions() {
        File nomedia = new File(PathTool.getModuleDataPath() + "/.nomedia");
        if (nomedia.exists()) nomedia.delete();
        try {
            if (nomedia.getParentFile().exists()) nomedia.getParentFile().mkdirs();
            return nomedia.createNewFile();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * @param context 上下文
     * @return false表示没有权限
     * @deprecated 该方法在android 11已弃用
     */
    @Deprecated
    public static boolean verifyStoragePermissions(Context context) {
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(context, "android.permission.WRITE_EXTERNAL_STORAGE");
            return permission == PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void dialog() {
        Activity act = ActivityTools.getActivity();
        if (act == null) return;
        hookPermissionResults(act.getClass());
        MDialog dialog = new MDialog(act);

        LinearLayout layout = new LinearLayout(act);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        Drawable background = ViewUtils.BackgroundBuilder.createBaseBackground(act.getColor(R.color.蔷薇色), 20);
        background.setAlpha(255);
        layout.setBackground(background);
        layout.setPadding(20, 20, 20, 10);

        TextView textView = new TextView(act);
        textView.setTextSize(20);
        textView.setText("QQ没有读写权限导致无法初始化\n接下来将会进行申请读写权限\n授权成功后请强行停止(重启)QQ\n如没有弹出授权弹窗请手动到权限管理授权");
        textView.setTextColor(Color.parseColor("#000000"));
        layout.addView(textView);

        Button button = new Button(act);
        button.setText("确定");
        button.setTextSize(20);
        button.setBackgroundResource(R.drawable.button_shape);
        button.setOnClickListener(view -> {
            ActivityCompat.requestPermissions(act, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            dialog.dismiss();
        });
        layout.addView(button);

        dialog.setContentView(layout);
        dialog.setDialogWindowAttr(0.7f, 0.5f);
        dialog.show();
    }

    private static void hookPermissionResults(Class<?> clz) {
        Method m = MethodUtils.findUnknownReturnTypeMethod(clz.getName(), "onRequestPermissionsResult", new Class[]{int.class, String[].class, int[].class});
        XposedBridge.hookMethod(m, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                int requestCode = (int) param.args[0];
                String[] permissions = (String[]) param.args[1];
                int[] grantResults = (int[]) param.args[2];

                if (requestCode == REQUEST_EXTERNAL_STORAGE) {
                    for (int i = 0; i < permissions.length; i++) {
                        if (grantResults[i] != -1) {
                            //权限设置成功
                            ToastTool.show("授权成功 请重启");
                            new Handler(Looper.getMainLooper()).postDelayed(() -> ActivityTools.killAppProcess((Activity) param.thisObject), 500);
                        } else {
                            //没成功
                            ToastTool.show("授权可能没有成功");
                        }
                    }
                }
            }
        });
    }
}
