package lin.xposed.hook.item;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import lin.util.ReflectUtils.ClassUtils;
import lin.util.ReflectUtils.FieldUtils;
import lin.util.ReflectUtils.MethodTool;
import lin.util.ReflectUtils.MethodUtils;
import lin.xposed.R;
import lin.xposed.common.utils.ActivityTools;
import lin.xposed.common.utils.DrawableUtil;
import lin.xposed.common.utils.MUtils;
import lin.xposed.hook.HookEnv;
import lin.xposed.hook.QQVersion;
import lin.xposed.hook.annotation.HookItem;
import lin.xposed.hook.item.api.AIOMessageMenu;
import lin.xposed.hook.item.emojipanel.EmoPanel;
import lin.xposed.hook.item.emojipanel.EmoSearchAndCache;
import lin.xposed.hook.load.base.BaseSwitchFunctionHookItem;
import lin.xposed.hook.util.PathTool;
import lin.xposed.hook.util.ToastTool;

@HookItem("辅助功能/聊天/表情面板#1")
public class EmoPanelInject extends BaseSwitchFunctionHookItem {

    private final String emoIconFilePath = PathTool.getModuleDataPath() + "/滑稽.png";
    private final String night_emoIconPath = PathTool.getModuleDataPath() + "/暗黑滑稽.png";
    private Drawable emojiPanelIconImage;

    private boolean isNightMode = false;

    @Override
    public String getTips() {
        return " ̶因̶为̶F̶u̶n̶P̶a̶n̶e̶l̶的̶Q̶T̶o̶o̶l̶表̶情̶面̶板̶#̶2̶难̶用̶ , 所以有了这个从QTool移植适配来的表情面板#1 , 表情以文件夹组保存在" + PathTool.getModuleDataPath() + "/Pic/" + "单击可以复制";
    }

    @Override
    public View.OnClickListener getViewOnClickListener() {
        return v -> {
            Context context = v.getContext();
            if (MUtils.copyStr(context, PathTool.getModuleDataPath() + "/Pic/")) {
                ToastTool.show("复制成功");
            } else {
                ToastTool.show("复制失败");
            }
        };
    }

    public void initIcon() {
        boolean darkModeStatus = EmoPanel.getDarkModeStatus(HookEnv.getHostAppContext());
        if (emojiPanelIconImage != null && isNightMode == darkModeStatus) return;
        File emoIconFile = new File(emoIconFilePath);
        if (!emoIconFile.exists()) {
            Drawable icon = HookEnv.getHostAppContext().getDrawable(R.drawable.emo_icon);
            DrawableUtil.drawableToFile(icon, emoIconFilePath, Bitmap.CompressFormat.PNG);
            ToastTool.show("表情面板图标初始化完毕");
        }
        File nightEmoIconFile = new File(night_emoIconPath);
        if (!nightEmoIconFile.exists()) {
            Drawable icon = HookEnv.getHostAppContext().getDrawable(R.drawable.night_emo_icon);
            DrawableUtil.drawableToFile(icon, night_emoIconPath, Bitmap.CompressFormat.WEBP);
            ToastTool.show("深色模式表情面板图标初始化完毕");
        }
        isNightMode = darkModeStatus;
        if (isNightMode)
            emojiPanelIconImage = DrawableUtil.readDrawableFromFile(HookEnv.getHostAppContext(), night_emoIconPath);
        else
            emojiPanelIconImage = DrawableUtil.readDrawableFromFile(HookEnv.getHostAppContext(), emoIconFilePath);

    }

    @Override
    public void loadHook(ClassLoader classLoader) throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                File dir = new File(EmoSearchAndCache.PIC_PATH);
                if (!dir.exists()) dir.mkdirs();
                initIcon();
            }
        }).start();
        if (QQVersion.isQQNT()) {
            QQNT qqnt = new QQNT();
            qqnt.createEmoPanelIcon();
            qqnt.hookLongClick();
        } else {
            LegacyQQ legacyQQ = new LegacyQQ();
            legacyQQ.createEmoPanelIcon();
        }

    }

    private ImageView createImageIcon(Context context) {
        initIcon();
        ImageView emoIcon = new ImageView(context);
        emoIcon.setContentDescription("QStory表情面板");
        emoIcon.setOnClickListener(v -> EmoPanel.createShow(context));
        emoIcon.setImageDrawable(emojiPanelIconImage);
        return emoIcon;
    }

    public class QQNT {
        public void hookLongClick() {
            Class<?> aioMsgItemClass = ClassUtils.getClass("com.tencent.mobileqq.aio.msg.AIOMsgItem");
            String[] component = new String[]{"com.tencent.mobileqq.aio.msglist.holder.component.pic.AIOPicContentComponent", "com.tencent.mobileqq.aio.msglist.holder.component.mix.AIOMixContentComponent",};
            //get aio msg item
            Class<?> baseMenuClass = ClassUtils.getClass("com.tencent.mobileqq.aio.msglist.holder.component.BaseContentComponent");
            Method getMsgMethod = MethodTool.find(baseMenuClass).returnType(aioMsgItemClass).get();//获取父类的get结果方法 父类是抽象类方法有唯一性 下一步再利用父类查找到的方法查找具体实现类中的方法 方法签名 public final T Z0() 可以得知该方法不会被子类重写
            Method superGetResultListMethod = MethodTool.find(baseMenuClass).returnType(List.class).get();//返回结果方法
            for (String findClassName : component) {
                //通过父类查找具体子实现类的方法
                Method getPicMessageMenuMethod = MethodTool.find(findClassName).name(superGetResultListMethod.getName()).returnType(List.class).get();
                //开始注入
                hookAfter(getPicMessageMenuMethod, param -> {
                    Object picAIOMsgItem = getMsgMethod.invoke(param.thisObject);
                    Object msgRecord = MethodTool.find(aioMsgItemClass).name("getMsgRecord").call(picAIOMsgItem);
                    ArrayList<Object> elements = FieldUtils.getField(msgRecord, "elements", ArrayList.class);

                    ArrayList<String> picUrlList = new ArrayList<>();
                    ArrayList<String> picMD5List = new ArrayList<>();
                    for (Object MsgElement : elements) {
                        Object picElement = MethodTool.find(MsgElement.getClass()).name("getPicElement").returnType(ClassUtils.getClass("com.tencent.qqnt.kernel.nativeinterface.PicElement")).call(MsgElement);
                        if (picElement != null) {
                            String md5 = FieldUtils.getField(picElement, "md5HexStr", String.class);
                            md5 = md5.toUpperCase();
                            String url = "http://gchat.qpic.cn/gchatpic_new/0/0-0-" + md5 + "/0";
                            picMD5List.add(md5);
                            picUrlList.add(url);
                        }
                    }
                    Object item = AIOMessageMenu.createAIOMenuItemQQNT(picAIOMsgItem, "保存到QS", R.mipmap.ic_launcher_round, () -> {
                        //集合中只有一张图片不打开多选保存框
                        if (picUrlList.size() == 1 && picMD5List.size() == 1) {
                            EmoPanel.PreSavePicToList(picUrlList.get(0), picMD5List.get(0), ActivityTools.getActivity());
                        } else {
                            EmoPanel.PreSaveMultiPicList(picUrlList, picMD5List, ActivityTools.getActivity());
                        }
                        return null;
                    });
                    List resultList = (List) param.getResult();
                    resultList.add(item);
                });
            }

        }

        public void createEmoPanelIcon() {
            Class<?> clz = ClassUtils.getClass("com.tencent.qqnt.aio.shortcutbar.PanelIconLinearLayout");
            Method hookMethod = null;
            for (Method method : clz.getDeclaredMethods()) {
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 3 && params[0] == int.class && params[1] == String.class) {
                    hookMethod = method;
                    break;
                }
            }
            if (hookMethod == null) {
                throw new RuntimeException("No method found");
            }
            hookAfter(hookMethod, param -> {
                LinearLayout layout = (LinearLayout) param.thisObject;
                Context context = layout.getContext();
                for (int i = 0; i < layout.getChildCount(); i++) {
                    View PaneIconImage = layout.getChildAt(i);

                    if (PaneIconImage.getContentDescription().equals("红包")) {
                        LinearLayout.LayoutParams oldLayoutParam = (LinearLayout.LayoutParams) PaneIconImage.getLayoutParams();
                        layout.addView(createImageIcon(context), i + 1, oldLayoutParam);
                        break;
                    }
                }
            });
        }
    }

    private class LegacyQQ {
        public void createEmoPanelIcon() {
            Method method = MethodUtils.findMethod("com.tencent.mobileqq.activity.aio.panel.PanelIconLinearLayout", null, void.class, new Class[]{ClassUtils.getClass("com.tencent.mobileqq.activity.aio.core.BaseChatPie")});
            method.setAccessible(true);
            hookAfter(method, param -> {
                LinearLayout layout = (LinearLayout) param.thisObject;
                Context context = layout.getContext();
                //好友和群聊情况
                if (layout.getChildCount() >= 4) {
                    @SuppressLint("ResourceType") View v = layout.getChildAt(2);
                    if (v == null) return;
                    layout.post(new Runnable() {
                        @Override
                        public void run() {
                            layout.addView(createImageIcon(context), 4, v.getLayoutParams());
                        }
                    });
                    //qq公众号情况
                } else if (layout.getChildCount() >= 2) {
                    layout.post(() -> layout.removeViewAt(1));
                }
            });
        }

    }

}
