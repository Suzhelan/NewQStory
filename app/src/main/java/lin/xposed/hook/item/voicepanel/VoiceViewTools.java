package lin.xposed.hook.item.voicepanel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import lin.widget.dialog.base.BaseSimpleDialog;
import lin.widget.dialog.base.MDialog;
import lin.xposed.R;
import lin.xposed.common.utils.ActivityTools;
import lin.xposed.common.utils.FileUtils;
import lin.xposed.hook.item.AddVoiceFloatingWindow;
import lin.xposed.hook.util.LogUtils;
import lin.xposed.hook.util.PathTool;
import lin.xposed.hook.util.ToastTool;

public class VoiceViewTools {
    private static MDialog dialog;

    public static void createSaveVoiceDialog(Context context, String path) {
        ActivityTools.injectResourcesToContext(context);
        try {
            dialog = new BaseSimpleDialog(context);
            View v = getSaveVoiceView(context, path);
            dialog.setContentView(v);
            dialog.show();
        } catch (Exception e) {
            LogUtils.addError("创建保存语音面板 已抛出异常", e);
        }
    }

    private static View getSaveVoiceView(Context context, String path) {
        @SuppressLint("InflateParams")
        RelativeLayout layout = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.save_voice_layout, null, false);
        LinearLayout linearLayout = layout.findViewById(R.id.save_voice_root_layout);
        EditText editText = layout.findViewById(R.id.voice_name);
        editText.setText("");
        //监听实时更改dialog大小 防止出现对话框内容多占满整块布局
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
                    saveVoice(path, s.toString().replace("\n", ""));
                }
            }
        });
        Button yesSave = layout.findViewById(R.id.yes_save_voice);
        yesSave.setOnClickListener(v -> {
            String name = editText.getText().toString();
            saveVoice(path, name);
        });
        Button noSave = layout.findViewById(R.id.save_voice_close);
        noSave.setOnClickListener(view -> {
            //关闭dialog
            dialog.dismiss();
        });
        return layout;
    }

    private static void saveVoice(String path, String name) {
        if (!new File(path).exists()) {
            ToastTool.show("语音可能尚未加载完毕");
            return;
        } else if (name.trim().equals("")) {
            name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
        }
        //防止重复的文件名
        else if (new File(PathTool.getModuleDataPath() + "/Voice", name).exists()) {
            repeatFileName(path, name);
            return;
        }
        final String newName = name;
        new Thread(() -> {
            try {
                FileUtils.copyFile(path, AddVoiceFloatingWindow.VOICE_PATH + newName);
                ToastTool.show("语音已保存" + newName);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            //关闭dialog 复制文件在线程里做 避免阻塞UI
        }).start();
        dialog.dismiss();
    }

    @SuppressLint("SetTextI18n")
    public static void repeatFileName(String path, String name) {
        Context context = ActivityTools.getActivity();
        MDialog dialog1 = new BaseSimpleDialog(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams")
        RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.save_voice_layout, null, false);

        TextView title = layout.findViewById(R.id.save_voice_title);
        title.setText("文件名重复 目录已存在 " + name);
        title.setTextColor(context.getColor(R.color.蔷薇色));
        EditText editText = layout.findViewById(R.id.voice_name);
        editText.setText(name);
        Button yesSave = layout.findViewById(R.id.yes_save_voice);
        yesSave.setText("覆盖此目标文件");
        yesSave.setOnClickListener(v -> {
            try {
                FileUtils.copyFile(path, AddVoiceFloatingWindow.VOICE_PATH + name);
                dialog1.dismiss();
                ToastTool.show("已覆盖该文件");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        Button noSave = layout.findViewById(R.id.save_voice_close);
        noSave.setText("保留两个文件");
        noSave.setOnClickListener(view -> {
            extracted(path, name);
            dialog1.dismiss();
        });
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
                    extracted(path, names);
                    dialog1.dismiss();
                    return;
                }
                yesSave.setText("保存");
                noSave.setText("思考");
                noSave.setOnClickListener(v -> dialog1.dismiss());
            }
        });
        dialog1.setContentView(layout);
        dialog1.show();
        dialog.dismiss();
    }

    private static void extracted(String path, String name) {
        String numbering = AddVoiceFloatingWindow.VOICE_PATH + name + "(1)";
        while (new File((numbering)).exists()) {
            //获取编号值
            String num = numbering.substring(numbering.lastIndexOf("(") + 1, numbering.lastIndexOf(")"));
            //提升编号索引
            int i = Integer.parseInt(num);
            i++;
            numbering = AddVoiceFloatingWindow.VOICE_PATH + name + "(" + i + ")";
        }
        try {
            FileUtils.copyFile(path, numbering);
            ToastTool.show("已生成新的语音文件" + numbering);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
