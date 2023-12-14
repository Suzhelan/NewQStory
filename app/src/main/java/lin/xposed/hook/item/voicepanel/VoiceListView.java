package lin.xposed.hook.item.voicepanel;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import lin.widget.dialog.base.MDialog;
import lin.xposed.R;
import lin.xposed.common.utils.ActivityTools;
import lin.xposed.common.utils.FileUtils;
import lin.xposed.common.utils.ScreenParamUtils;
import lin.xposed.common.utils.ViewUtils;
import lin.xposed.hook.QQVersion;
import lin.xposed.hook.util.PathTool;
import lin.xposed.hook.util.ToastTool;
import lin.xposed.hook.util.qq.LegacyQQSendTool;
import lin.xposed.hook.util.qq.QQNTSendMsgUtils;
import lin.xposed.hook.util.qq.SessionUtils;

public class VoiceListView {
    public static final String VOICE_PATH = PathTool.getModuleDataPath() + "/Voice/";
    private static final AtomicLong updateTime = new AtomicLong();

    private static final AtomicBoolean isRootDirectory = new AtomicBoolean();


    //定义父目录,防止多层目录迷失自我
    private static String Parent;

    @SuppressLint("SetTextI18n")
    public static View buildView(Dialog dialog, Context context, String initPath) {
        isRootDirectory.set(initPath.equals(new File(VOICE_PATH).getAbsolutePath()) || initPath.equals(VOICE_PATH));
        File file = new File(initPath);
        if (!file.exists()) file.mkdirs();
        Parent = file.getParent();
        GradientDrawable dirBackground = new GradientDrawable();
        dirBackground.setCornerRadius(15);
        dirBackground.setAlpha(200);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            dirBackground.setPadding(30, 15, 15, 15);
        }
        dirBackground.setStroke(2, context.getColor(R.color.秘色));

        GradientDrawable fileBackground = new GradientDrawable();
        fileBackground.setCornerRadius(15);
        fileBackground.setAlpha(200);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            fileBackground.setPadding(30, 15, 15, 15);
        }
        fileBackground.setStroke(2, context.getColor(R.color.水色));

        @SuppressLint("InflateParams")
        ScrollView scrollView = (ScrollView) LayoutInflater.from(context).inflate(R.layout.send_voice_layout, null);
        LinearLayout layout = scrollView.findViewById(R.id.send_voice_layout);
        layout.post(() -> {
            TextView tvs = new TextView(context);
            tvs.setText("上一级:" + Parent);
            tvs.setTextColor(Color.parseColor("#000000"));
            tvs.setTextSize(10);
            tvs.setOnClickListener(v -> {
                if (Parent.equals("/storage/emulated")) {
                    ToastTool.show("已是所能探索到的极限");
                    return;
                }
                dialog.setContentView(buildView(dialog, context, Parent));
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(30, 10, 15, 5);
            layout.addView(tvs, params);
            boolean isFirst = true;
            List<File> list = Arrays.asList(file.listFiles());
            // 按文件夹先显示的顺序：
            list.sort((o1, o2) -> {
                if (o1.isDirectory() && o2.isFile())
                    return -1;
                if (o1.isFile() && o2.isDirectory())
                    return 1;
                return o1.getName().compareTo(o2.getName());
            });
            for (File voiceFile : list) {
                if (voiceFile.getAbsolutePath().equals(file.getAbsolutePath())) {
                    continue;
                }
                TextView tv = new TextView(context);
                tv.setText(voiceFile.getName());
                tv.setTextColor(Color.parseColor("#80000000"));
                tv.setPadding(30, 15, 15, 15);
                tv.setTextSize(20);
                tv.setOnLongClickListener(v -> {
                    tryDeleteVoiceFile(voiceFile, dialog, context, initPath);
                    return true;
                });
                if (voiceFile.isDirectory()) tv.setBackground(dirBackground);
                if (voiceFile.isFile()) tv.setBackground(fileBackground);
                tv.setOnClickListener(view -> {
                    //如果是文件
                    if (voiceFile.isFile()) {
                        //尝试发送语音
                        if (QQVersion.isQQNT()) {
                            QQNTSendMsgUtils.sendVoice(SessionUtils.getCurrentAIOContact(), voiceFile.getAbsolutePath());
                        } else {
                            LegacyQQSendTool.sendVoice(SessionUtils.getCurrentAIOContact(), voiceFile.getAbsolutePath());
                        }
                    } else {
                        dialog.setOnCancelListener(dialogs -> {
                            if (isRootDirectory.get()) return;
                            if (Parent.equals("/storage/emulated")) return;
                            dialog.setContentView(buildView(dialog, context, Parent));
                            dialog.show();
                        });
                        dialog.setContentView(buildView(dialog, context, voiceFile.getAbsolutePath()));
                    }
                });
                layout.addView(tv, getParams(isFirst));
                isFirst = false;
            }
            if (isFirst) {
                TextView tv = new TextView(context);
                tv.setText("当前目录无文件");
                tv.setTextColor(Color.parseColor("#80000000"));
                tv.setTextSize(20);
                tv.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params1.setMargins(30, 30, 15, 5);
                layout.addView(tv, params1);
            }
        });
        updateTime.set(file.lastModified());
        return scrollView;
    }

    private static void tryDeleteVoiceFile(File file, Dialog dialogs, Context contexts, String path) {
        Context context = ActivityTools.getActivity();
        MDialog dialog = new MDialog(context);

        RelativeLayout layout = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.save_voice_layout, null, false);
        LinearLayout linearLayout = layout.findViewById(R.id.save_voice_root_layout);
        GradientDrawable background = ViewUtils.BackgroundBuilder.createBaseBackground(context.getColor(R.color.群青色), ScreenParamUtils.dpToPx(context, 15));
        background.setAlpha(200);
        linearLayout.setBackground(background);
        TextView title = layout.findViewById(R.id.save_voice_title);
        title.setText("删除此文件");
        title.setTextColor(context.getColor(R.color.蔷薇色));
        EditText editText = layout.findViewById(R.id.voice_name);
        editText.setText(file.getName());
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().contains("\n") && !s.toString().equals("")) {
                    String names = editText.getText().toString().replace("\n", "");
                    if (new File(file.getParentFile() + "/" + names).exists()) {
                        dialog.dismiss();
                        VoiceTools.repeatFileName(file.getAbsolutePath(), names);
                        return;
                    }
                    if (file.renameTo(new File(file.getParentFile() + "/" + names))) {
                        dialog.dismiss();
                    } else {
                        editText.setText(names);
                        ToastTool.show("重命名失败 可能已经有重名文件或格式错误");
                    }
                }
            }
        });
        Button yesSave = layout.findViewById(R.id.yes_save_voice);
        yesSave.setText("删除");
        yesSave.setOnClickListener(v -> {
            FileUtils.deleteFile(file);
            dialog.dismiss();
        });
        Button noSave = layout.findViewById(R.id.save_voice_close);
        noSave.setText("重命名");
        noSave.setOnClickListener(view -> {
            String names = editText.getText().toString();
            if (file.renameTo(new File(file.getParentFile() + "/" + names))) {
                dialog.dismiss();
            } else {
                ToastTool.show("重命名失败 可能已经有重名文件");
            }
        });
        dialog.setContentView(layout);
        dialog.setOnDismissListener(dialog1 -> dialogs.setContentView(buildView(dialogs, contexts, path)));

        dialog.setDialogWindowAttr(0.6, 0.25);
        dialog.show();
    }

    private static ViewGroup.LayoutParams getParams(boolean isFirst) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(10, isFirst ? 15 : 10, 10, 5);
        return params;
    }
}
