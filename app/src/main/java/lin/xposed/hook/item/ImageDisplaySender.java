package lin.xposed.hook.item;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lin.util.ReflectUtils.ClassUtils;
import lin.util.ReflectUtils.FieldUtils;
import lin.util.ReflectUtils.MethodUtils;
import lin.xposed.R;
import lin.xposed.common.utils.ActivityTools;
import lin.xposed.common.utils.ScreenParamUtils;
import lin.xposed.common.utils.ViewUtils;
import lin.xposed.hook.QQVersion;
import lin.xposed.hook.annotation.HookItem;
import lin.xposed.hook.load.base.BaseSwitchFunctionHookItem;
import lin.xposed.hook.util.LogUtils;
import lin.xposed.hook.util.ToastTool;

@HookItem("辅助功能/聊天/聊天界面图片显示发送者")
public class ImageDisplaySender extends BaseSwitchFunctionHookItem {

    private Bundle bundle;

    private String fieldName = "";

    @Override
    public void loadHook(ClassLoader loader) throws Exception {
        if (QQVersion.isQQNT()) {
            hookNTQQ();
        } else {
            hookNotNTQQ();
        }
    }

    private Map<String, String> outBundle(Bundle bundle) {
        HashMap<String, String> map = new HashMap<>();
        for (String key : bundle.keySet()) {
            map.put(key,String.valueOf( bundle.get(key)));
        }
        return map;
     }

    private void hookNTQQ() {
        Class<?> QQGalleryActivityClass = ClassUtils.getClass("com.tencent.richframework.gallery.QQGalleryActivity");
        Method m = MethodUtils.findMethod(QQGalleryActivityClass, "onCreate", void.class, new Class[]{Bundle.class});
        hookAfter(m,param -> {
            Activity activity = (Activity) param.thisObject;
            bundle = activity.getIntent().getExtras();
        });
        Method m2 = MethodUtils.findNoParamsMethod(QQGalleryActivityClass, "onResume", void.class);
        hookAfter(m2,param -> {
            Context context = (Context) param.thisObject;
            ActivityTools.injectResourcesToContext(context);

            if (bundle == null) return;

            int chat_type = bundle.getInt("forward_source_uin_type");
            if (chat_type == 0) return;//私聊不需要显示发送者
            RelativeLayout root = null;
            List<View> allViewsOfTheCurrentActivity = ActivityTools.getAllChildViews((Activity) param.thisObject);
            for (View view : allViewsOfTheCurrentActivity) {
                if (view instanceof RelativeLayout) {
                    root = (RelativeLayout) view;
                    break;
                }
            }

            ViewGroup.LayoutParams params2 = root.getLayoutParams();
            params2.height +=  ScreenParamUtils.dpToPx(context, 80);
            ActivityTools.injectResourcesToContext(context);

            @SuppressLint("InflateParams")
            View top = LayoutInflater.from(context).inflate(R.layout.pic_top_layout, null, false);
            TextView sendUinView = top.findViewById(R.id.pic_send_uin);
            TextView isGroup = top.findViewById(R.id.type);
            isGroup.setOnClickListener(v -> ToastTool.show("可能点旁边的字会有什么反应呢"));
            TextView source = top.findViewById(R.id.pic_group_uin);
            if (chat_type == 1) {
                String sendUin = bundle.getString("extra.GROUP_CODE");
                String groupName = bundle.getString("key_troop_group_name");
                String groupUin = bundle.getString("extra.GROUP_UIN");
                isGroup.setText("群聊消息 ");
                source.setText("来自群组 : " + groupName + "(" +
                        groupUin + ")");
                source.setOnClickListener(v -> context.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("mqqapi://card/show_pslcard?src_type=internal&version=1&uin="
                                + groupUin + "&card_type=group&source=qrcode"))));
            } else if (chat_type == 0) {
                String sendUin = bundle.getString("uin");
                source.setText("来自私聊与 " + sendUin + " 的对话");
                source.setOnClickListener(v -> {
                    String urlQQ = "mqq://card/show_pslcard?src_type=internal&source=sharecard&version=1&uin="
                            + sendUin;
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(urlQQ)));
                });
            }

            String uin = chat_type == 1 ? bundle.getString("extra.GROUP_CODE") : bundle.getString("uin");
            sendUinView.setText("发送者 : " + uin);

            sendUinView.setOnClickListener(v -> {
                String urlQQ = "mqq://card/show_pslcard?src_type=internal&source=sharecard&version=1&uin="
                        + uin;
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(urlQQ)));
            });

            top.setOnClickListener(v -> {
                if (top.getAlpha() >= 50) {
                    top.setAlpha(0);
                } else {
                    top.setAlpha(1);
                }
            });
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            root.addView(top.getRootView(), params);
        });

    }
    private void hookNotNTQQ() {
        Class<?> AIOGalleryActivityClass = ClassUtils.getClass("com.tencent.mobileqq.richmediabrowser.AIOGalleryActivity");
        Method m1 = MethodUtils.findMethod(AIOGalleryActivityClass, "onCreate", void.class, new Class[]{Bundle.class});
        hookAfter(m1,param -> {
            Intent intent = MethodUtils.callNoParamsMethod(param.thisObject, "getIntent", Intent.class);
            bundle = intent.getExtras();
        });
        Method m2 = MethodUtils.findNoParamsMethod(AIOGalleryActivityClass, "onResume", void.class);
        hookAfter(m2,param -> {

            if (bundle == null) {
                LogUtils.addRunLog("PicUI bundle==null");
                return;
            }
            List<View> views = ActivityTools.getAllChildViews((Activity) param.thisObject);
            Context context = (Context) param.thisObject;
            ActivityTools.injectResourcesToContext(context);
            RelativeLayout root = null;
            for (int i = 0; i < views.size(); i++) {
                View view = views.get(i);
                if (root == null) {
                    if (view instanceof RelativeLayout && i >= 3) {
                        root = (RelativeLayout) view;
                    }
                }
                if (view.getClass().equals(View.class)) {
                    view.setBackground(ViewUtils.BackgroundBuilder.createBaseBackground(context.getColor(R.color.群青色),0));
                    break;
                }
            }
            @SuppressLint("InflateParams")
            View top = LayoutInflater.from(context).inflate(R.layout.pic_top_layout, null, false);

            String groupUin;
            String sendName = null;
            String sendUin;
            TextView sendUinView = top.findViewById(R.id.pic_send_uin);
            TextView isGroup = top.findViewById(R.id.type);
            isGroup.setOnClickListener(v -> ToastTool.show("可能点旁边的字会有什么反应呢"));
            TextView source = top.findViewById(R.id.pic_group_uin);
            //私聊时此值为聊天对象Uin
            String keyTroopGroupName = bundle.getString("key_troop_group_name");
            int type = bundle.getInt("uintype");
            if (type == 1) {
                groupUin = bundle.getString("extra.GROUP_UIN");
                isGroup.setText("群聊消息 ");
                source.setText("来自群组 : " + keyTroopGroupName + "(" +
                        groupUin + ")");
                source.setOnClickListener(v -> context.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("mqqapi://card/show_pslcard?src_type=internal&version=1&uin="
                                + groupUin + "&card_type=group&source=qrcode"))));
            } else {
                groupUin = null;
                if (type == 0) {
                    source.setText("来自私聊与 " + keyTroopGroupName + " 的对话");
                    source.setOnClickListener(v -> {
                        String urlQQ = "mqq://card/show_pslcard?src_type=internal&source=sharecard&version=1&uin="
                                + keyTroopGroupName;
                        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(urlQQ)));
                    });
                }
            }

            Object picData = bundle.getParcelable("extra.EXTRA_CURRENT_IMAGE");
            //查找被混淆的字段 发送者UIN
            Class<?> picDataClz = picData.getClass();
            for (Field field : picDataClz.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.getType().equals(String.class)) {
                    String isSendUin = (String) field.get(picData);
                    //不同场景查找方式不同
                    if (groupUin != null) {
                        if (checkQQ(isSendUin) && !isSendUin.equals(groupUin)) {
                            fieldName = field.getName();
                        }
                    } else if (checkQQ(isSendUin) && !isSendUin.equals(keyTroopGroupName)) {
                        fieldName = field.getName();
                    }
                }
            }
            try {
                sendUin = FieldUtils.getField(picData, fieldName, String.class);
            } catch (Exception e) {
                return;
            }

            sendUinView.setText("发送者 : " + sendUin);

            String finalSendUin = sendUin;
            sendUinView.setOnClickListener(v -> {
                String urlQQ = "mqq://card/show_pslcard?src_type=internal&source=sharecard&version=1&uin="
                        + finalSendUin;
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(urlQQ)));
            });

            int statusBarHeight1 = -1;
            //获取status_bar_height资源的ID
            @SuppressLint({"InternalInsetResource", "DiscouragedApi"})
            int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                //根据资源ID获取响应的尺寸值
                statusBarHeight1 = context.getResources().getDimensionPixelSize(resourceId);
            }

            top.setOnClickListener(v -> {
                if (top.getAlpha() >= 50) {
                    top.setAlpha(0);
                } else {
                    top.setAlpha(1);
                }
            });
            top.setPadding(0, statusBarHeight1, 0, ScreenParamUtils.dpToPx(context, 10));
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            root.addView(top.getRootView(), params);
        });
    }

    private boolean checkQQ(String qq) {
        if (TextUtils.isEmpty(qq)) {
            return false;
        }
        int length = qq.length();
        if (length < 5 || length > 10) {
            return false;
        }
        if (qq.startsWith("0")) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            char a = qq.charAt(i);
            if (a < '0' || a > '9') {
                return false;
            }
        }
        return true;
    }
}
