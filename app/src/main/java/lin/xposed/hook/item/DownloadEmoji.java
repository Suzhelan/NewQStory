package lin.xposed.hook.item;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import lin.util.ReflectUtils.ClassUtils;
import lin.util.ReflectUtils.ConstructorUtils;
import lin.util.ReflectUtils.FieldUtils;
import lin.util.ReflectUtils.MethodTool;
import lin.util.ReflectUtils.MethodUtils;
import lin.xposed.R;
import lin.xposed.common.utils.ActivityTools;
import lin.xposed.common.utils.BitmapUtils;
import lin.xposed.common.utils.HttpUtils;
import lin.xposed.hook.annotation.HookItem;
import lin.xposed.hook.load.base.BaseSwitchFunctionHookItem;
import lin.xposed.hook.load.methodfind.IMethodFinder;
import lin.xposed.hook.load.methodfind.MethodFinder;
import lin.xposed.hook.util.LogUtils;
import lin.xposed.hook.util.PathTool;
import lin.xposed.hook.util.ToastTool;

@HookItem("辅助功能/表情/在新的QQ中依然可以下载表情")
public class DownloadEmoji extends BaseSwitchFunctionHookItem implements IMethodFinder {

    private final String privateTAG = "保存本地";
    Class<?> emojiInfoClass;
    private Object dialog;
    private String emojiUrl;

    private String emojiMD5;

    @Override
    public String getTips() {
        return "在版本大于等于8.9.80时QQ关闭了表情保存 此功能可以继续下载表情 保存到的路径为 " + PathTool.getStorageDirectory() + "/Pictures/QQ/";
    }

    @Override
    public void loadHook(ClassLoader classLoader) throws Exception {

        Class<?> AIOEmotionFragmentClass = ClassUtils.getClass("com.tencent.mobileqq.emotionintegrate.AIOEmotionFragment");
        Method method = AIOEmotionFragmentClass.getMethod("onCreate", Bundle.class);
        //获取MD5
        hookAfter(method, param -> {
            Object emojiInfo = null;
            for (Field field : param.thisObject.getClass().getDeclaredFields()) {
                if (field.getType() == emojiInfoClass) {
                    emojiInfo = field.get(param.thisObject);
                    break;
                }
            }
            for (Method m : emojiInfo.getClass().getDeclaredMethods()) {
                if (m.getReturnType() == String.class) {
                    m.setAccessible(true);
                    String result = (String) m.invoke(emojiInfo);
                    if (result == null) continue;
                    if (result.length() > 16) {
                        emojiMD5 = result.toUpperCase();
                        emojiUrl = "http://gchat.qpic.cn/gchatpic_new/0/0-0-" + result.toUpperCase() + "/0?term=2";
                        break;
                    }
                }
            }
            Activity activity = MethodUtils.callUnknownReturnTypeNoParamMethod(param.thisObject, "getActivity");
            ActivityTools.injectResourcesToContext(activity);
        });

        //注入点击事件 因为没有找到别的注入点
        Class<?> ActionSheetItemAdapterClass = classLoader.loadClass("com.tencent.mobileqq.widget.share.ShareActionSheetV2$ActionSheetItemAdapter");
        hookAfter(ActionSheetItemAdapterClass.getMethod("getView", int.class, android.view.View.class, android.view.ViewGroup.class), param -> {
            ViewGroup resultView = (ViewGroup) param.getResult();
            for (int i = 0; i < resultView.getChildCount(); i++) {
                View view = resultView.getChildAt(i);
                if (view instanceof TextView textView) {
                    String text = textView.getText().toString();
                    if (text.equals(privateTAG)) {
                        resultView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                new Thread(() -> {
                                    try {
                                        Activity activity = ActivityTools.getActivity();
                                        //关闭弹窗
                                        MethodTool.find(dialog.getClass()).name("dismiss").call(dialog);
                                        //获取后缀
                                        String suffix = BitmapUtils.getImageType(emojiUrl);
                                        //下载
                                        String downloadPath = PathTool.getStorageDirectory() + "/Pictures/QQ/" + emojiMD5 + suffix;
                                        File file = new File(downloadPath);
                                        HttpUtils.fileDownload(emojiUrl, downloadPath);
                                        //通知相册更新
                                        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                        Uri uri = Uri.fromFile(file);
                                        intent.setData(uri);
                                        activity.sendBroadcast(intent);
                                        ToastTool.show("保存成功~");
                                    } catch (Exception e) {
                                        ToastTool.show("下载失败 原因 : " + LogUtils.getStackTrace(e));
                                        LogUtils.addError(e);
                                    }
                                }).start();
                            }
                        });
                    }
                }
            }
        });
        //插入item
        Class<?> ShareActionSheetV2Class = classLoader.loadClass("com.tencent.mobileqq.widget.share.ShareActionSheetV2");
        Method m3 = ShareActionSheetV2Class.getMethod("setActionSheetItems", List[].class);
        hookBefore(m3, param -> {
            List[] params = (List[]) param.args[0];
            for (List list : params) {
                for (Object item : list) {
                    String label = FieldUtils.getField(item, "label", String.class);

                    if (label != null && label.equals("添加到表情")) {
                        Object mItem = ConstructorUtils.newInstance(item.getClass());
                        FieldUtils.setField(mItem, "icon", R.drawable.download_icon);
                        FieldUtils.setField(mItem, "reportID", "QStory_DownLoadEmoji");
                        FieldUtils.setField(mItem, "label", privateTAG);
                        list.add(0, mItem);
                        this.dialog = param.thisObject;
                        break;
                    }
                }
            }
        });

    }

    @Override
    public void startFind(MethodFinder methodFinder) throws Exception {
        Method m = methodFinder.findMethodString("restoreSaveInstanceState execute")[0];
        methodFinder.putMethod("restoreSaveInstanceState execute", m);
    }

    @Override
    public void getMethod(MethodFinder finder) {
        emojiInfoClass = finder.getMethod("restoreSaveInstanceState execute").getDeclaringClass();
    }
}
