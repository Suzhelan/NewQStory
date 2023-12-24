package lin.xposed.hook.item.emojipanel;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.bumptech.glide.Glide;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.core.BasePopupView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import lin.xposed.R;
import lin.xposed.common.utils.ActivityTools;
import lin.xposed.common.utils.FileUtils;
import lin.xposed.hook.HookEnv;
import lin.xposed.hook.util.ToastTool;

public class EmoPanel {
    static String choiceDirName = "";

    public static void createShow(Context context) {
        ActivityTools.injectResourcesToContext(context);
        Context fixContext = ContextFixUtil.getFixContext(context);
        EmoPanelView NewView = new EmoPanelView(fixContext);

        XPopup.Builder NewPop = new XPopup.Builder(fixContext).isDestroyOnDismiss(true);
        BasePopupView base = NewPop.asCustom(NewView);
        base.show();
    }

    public static void PreSavePicToList(String URL, String MD5, Context context) {
        choiceDirName = "";
        LayoutInflater inflater = LayoutInflater.from(context);
        LinearLayout mRoot = (LinearLayout) inflater.inflate(R.layout.emo_pre_save, null);
        ImageView preView = mRoot.findViewById(R.id.emo_pre_container);
        preView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        EmoInfo NewInfo = new EmoInfo();
        NewInfo.URL = URL;
        NewInfo.type = 2;
        NewInfo.MD5 = MD5.toUpperCase(Locale.ROOT);

        if (!URL.startsWith("http")) {
            NewInfo.Path = URL;
            Glide.with(HookEnv.getHostAppContext())
                    .load(new File(NewInfo.Path))
                    .fitCenter()
                    .into(preView);
        } else {
            EmoOnlineLoader.submit(NewInfo, () -> {
                Glide.with(HookEnv.getHostAppContext())
                        .load(new File(NewInfo.Path))
                        .fitCenter()
                        .into(preView);
            });
        }

        ArrayList<String> NameList = EmoSearchAndCache.searchForPathList();
        RadioGroup group = mRoot.findViewById(R.id.emo_pre_list_choser);
        for (String ItemName : NameList) {
            RadioButton button = new RadioButton(context);
            button.setText(ItemName);
            button.setTextSize(16);
            button.setTextColor(context.getResources().getColor(R.color.font_plugin, null));
            button.setOnCheckedChangeListener((v, ischeck) -> {
                if (v.isPressed() && ischeck) {
                    choiceDirName = ItemName;
                }
            });
            group.addView(button);
        }
        //新建列表按钮
        Button btnCreate = mRoot.findViewById(R.id.createNew);
        btnCreate.setOnClickListener(v -> {
            EditText edNew = new EditText(context);
            new AlertDialog.Builder(context, getDarkModeStatus(context) ? AlertDialog.THEME_HOLO_DARK : AlertDialog.THEME_HOLO_LIGHT)
                    .setTitle("创建新目录")
                    .setView(edNew)
                    .setNeutralButton("确定创建", (dialog, which) -> {
                        String newName = edNew.getText().toString();
                        if (TextUtils.isEmpty(newName)) {
                            ToastTool.show("名字不能为空");
                            return;
                        }
                        String newPath = EmoSearchAndCache.PIC_PATH + newName;
                        if (new File(newPath).exists()) {
                            ToastTool.show("这个名字已经存在");
                            return;
                        }
                        new File(newPath).mkdirs();
                        ArrayList<String> NewList = EmoSearchAndCache.searchForPathList();
                        group.removeAllViews();
                        //确认添加列表后会重新扫描列表并显示
                        for (String ItemName : NewList) {
                            RadioButton button = new RadioButton(context);
                            button.setText(ItemName);
                            button.setTextSize(16);
                            button.setTextColor(context.getResources().getColor(R.color.font_plugin, null));
                            button.setOnCheckedChangeListener((vaa, ischeck) -> {
                                if (vaa.isPressed() && ischeck) {
                                    choiceDirName = ItemName;
                                }
                            });
                            group.addView(button);
                        }
                    })
                    .show();

        });

        new AlertDialog.Builder(context, getDarkModeStatus(context) ? AlertDialog.THEME_HOLO_DARK : AlertDialog.THEME_HOLO_LIGHT)
                .setTitle("是否保存")
                .setView(mRoot)
                .setNeutralButton("保存", (dialog, which) -> {
                    if (TextUtils.isEmpty(choiceDirName)) {
                        ToastTool.show("没有选择任何的保存列表");
                    } else if (TextUtils.isEmpty(NewInfo.Path)) {
                        ToastTool.show("图片尚未加载完毕,保存失败");
                    } else {
                        try {
                            FileUtils.copyFile(NewInfo.Path, EmoSearchAndCache.PIC_PATH + choiceDirName + "/" + MD5);
                            ToastTool.show("已保存到:" + EmoSearchAndCache.PIC_PATH + choiceDirName + "/" + MD5);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }).setOnDismissListener(dialog -> {
                    Glide.with(HookEnv.getHostAppContext()).clear(preView);
                }).show();
    }

    public static boolean getDarkModeStatus(Context context) {
        int mode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    //如果要保存的是多张图片则弹出MD5选择,选择后才弹出确认图片保存框
    public static void PreSaveMultiPicList(ArrayList<String> url, ArrayList<String> MD5, Context context) {
        new AlertDialog.Builder(context, getDarkModeStatus(context) ? AlertDialog.THEME_HOLO_DARK : AlertDialog.THEME_HOLO_LIGHT)
                .setTitle("选择需要保存的图片")
                .setItems(MD5.toArray(new String[0]), (dialog, which) -> {
                    PreSavePicToList(url.get(which), MD5.get(which), context);
                }).setOnDismissListener(dialog -> {

                }).show();
    }

    public static class EmoInfo {
        public String Path;
        public String Name;
        public int type;
        public String MD5;
        public String URL;
        public String thumb;
    }


}
