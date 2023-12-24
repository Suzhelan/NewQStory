package lin.xposed.hook.item;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import lin.util.ReflectUtils.FieldUtils;
import lin.util.ReflectUtils.MethodTool;
import lin.widget.dialog.SimpleRadioDialog;
import lin.xposed.R;
import lin.xposed.common.config.SimpleConfig;
import lin.xposed.common.utils.ActivityTools;
import lin.xposed.common.utils.ScreenParamUtils;
import lin.xposed.hook.annotation.HookItem;
import lin.xposed.hook.load.base.BaseSwitchFunctionHookItem;
import lin.xposed.hook.util.ToastTool;

@HookItem("辅助功能/消息通知/快捷免打扰")
public class QuicklyTurnOnDNDMessages extends BaseSwitchFunctionHookItem {

    private static SimpleConfig timeConfig;

    /**
     * 日期格式化为字符串
     */
    public static String formatDate(Date date, String format) {
        try {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat(format);
            return sdf.format(date);
        } catch (Exception e) {
            return "日期格式化失败 " + format;
        }
    }

    public void showDialog(Activity activity) {
        timeConfig = new SimpleConfig(QuicklyTurnOnDNDMessages.class.getName());
        SimpleRadioDialog simpleRadioDialog = new SimpleRadioDialog(activity, new String[]{"8小时", "2小时", "关闭"});
        Long targetTime = timeConfig.get("targetTime");

        Date currentTime = new Date();
        //不在范围时间内 清理任务信息
        if (targetTime == null || currentTime.getTime() > targetTime) {
            timeConfig.removeAll();
            timeConfig.put("TaskName", "关闭");
            simpleRadioDialog.setTitle("需要屏蔽通知的时长");
        } else {
            long timeRemaining = targetTime - currentTime.getTime();//结束时间 减去当前时间是剩余时间
            long hour = timeRemaining / (60 * 60 * 1000);
            long minute = (timeRemaining - hour * 60 * 60 * 1000) / (60 * 1000);
            simpleRadioDialog.setTitle("剩余 " + hour + "时" + minute + "分");
        }

        simpleRadioDialog.setSelected(timeConfig.get("TaskName"));
        simpleRadioDialog.setOnClick(text -> {
            //add task
            if (text.equals("关闭")) {
                timeConfig.removeAll();
                simpleRadioDialog.setTitle("需要屏蔽通知的时长");
                ToastTool.show("关闭了免打扰");
            } else {
                Calendar calendar = Calendar.getInstance();//默认是当前日期
                calendar.add(Calendar.HOUR, Integer.parseInt(text.substring(0, text.indexOf("小时"))));//添加目标日期
                timeConfig.put("targetTime", calendar.getTimeInMillis());

                long timeRemaining = calendar.getTimeInMillis() - System.currentTimeMillis();//结束时间 减去当前时间是剩余时间
                long hour = timeRemaining / (60 * 60 * 1000);
                simpleRadioDialog.setTitle("剩余 " + hour + "时");

                ToastTool.show("将在 " + formatDate(new Date((Long) timeConfig.get("targetTime")), "MM-dd hh:mm") + " 结束");
            }
            timeConfig.put("TaskName", text);
            timeConfig.submit();//提交配置
        });
        simpleRadioDialog.show();
    }

    @Override
    public String getTips() {
        return "无需重启 开启后生成图标在QQ主页右上角 (我想睡个好觉)";
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void loadHook(ClassLoader classLoader) throws Exception {
        //不hook onCreate方法了 那样需要重启才能生效 hook onResume可在界面重新渲染到屏幕时会调用生效
        Method onCreateMethod = MethodTool.find("com.tencent.mobileqq.activity.home.Conversation").name("onResume").params(boolean.class).get();
        hookAfter(onCreateMethod, param -> {
            //这个是QQ原本的主页右上角 + 图标 尽量不要动这个的长按事件 因为qa已经用了
            ImageView imageView = FieldUtils.getFirstField(param.thisObject, ImageView.class);
            RelativeLayout titleView = (RelativeLayout) imageView.getParent().getParent();
            Activity activity = (Activity) imageView.getContext();
            ActivityTools.injectResourcesToContext(activity);
            //new my icon view param
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ScreenParamUtils.dpToPx(activity, 38), ScreenParamUtils.dpToPx(activity, 38));
            layoutParams.addRule(RelativeLayout.START_OF, ((View) imageView.getParent()).getId());
            layoutParams.rightMargin = ScreenParamUtils.dpToPx(activity, 10);
            layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
            //my icon
            ImageView myIcon = new ImageView(activity);
            int padding = ScreenParamUtils.dpToPx(activity, 8);
            myIcon.setPadding(padding, padding, padding, padding);
            myIcon.setImageDrawable(activity.getDrawable(R.drawable.do_not_disturb_icon));
            myIcon.setOnClickListener(view -> showDialog((Activity) imageView.getContext()));
            //add view
            titleView.addView(myIcon, layoutParams);
        });
        hookBefore(NotificationManager.class.getMethod("notify", int.class, Notification.class), param -> {
            if (timeConfig == null)
                timeConfig = new SimpleConfig(QuicklyTurnOnDNDMessages.class.getName());
            //发送通知先先查询是否在勿扰时间内
            Long targetTime = timeConfig.get("targetTime");
            if (targetTime != null) {
                Date currentTime = new Date();
                if (currentTime.getTime() < targetTime) param.setResult(null);
            }
        });


    }

}
