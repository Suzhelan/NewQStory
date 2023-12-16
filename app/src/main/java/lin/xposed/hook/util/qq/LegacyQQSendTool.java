package lin.xposed.hook.util.qq;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

import lin.util.ReflectUtils.ClassUtils;
import lin.util.ReflectUtils.FieIdUtils;
import lin.util.ReflectUtils.MethodUtils;
import lin.xposed.common.utils.DataUtils;
import lin.xposed.common.utils.FileUtils;
import lin.xposed.hook.HookEnv;
import lin.xposed.hook.util.LogUtils;
import lin.xposed.hook.util.PathTool;

public class LegacyQQSendTool {

    static String TAG = "QQSendUtils";

    public static void sendTextMsg(Object session, String text, ArrayList atList) {
        Method send = MethodUtils.findMethod("com.tencent.mobileqq.activity.ChatActivityFacade", null, void.class, new Class[]{ClassUtils.getClass("com.tencent.mobileqq.app.QQAppInterface"), Context.class, ClassUtils.getClass("com.tencent.mobileqq.activity.aio.SessionInfo"), String.class, ArrayList.class});
        try {
            send.invoke(null, SessionUtils.LegacyQQ.getAppInterface(), HookEnv.getHostAppContext(), session, text, atList);
        } catch (Exception e) {
            LogUtils.addRunLog(TAG, e);
        }
    }

    //通过路径发送图片
    public static void sendByPicPath(Object session, String path) {
        sendPic(session, MsgBuilder.builderPic(session, MsgBuilder.checkAndGetCastPic(path)));
    }

    //发送对象,图片对象
    public static void sendPic(Object session, Object pic) {
        Method m = MethodUtils.findMethod(ClassUtils.getClass("com.tencent.mobileqq.activity.ChatActivityFacade"), null, void.class, new Class[]{ClassUtils.getClass("com.tencent.mobileqq.app.QQAppInterface"), ClassUtils.getClass("com.tencent.mobileqq.activity.aio.SessionInfo"), ClassUtils.getClass("com.tencent.mobileqq.data.MessageForPic"), int.class});
        if (m == null) {
            LogUtils.addError(new NullPointerException("find sendPic method == null"));
        }

        try {
            m.invoke(null, SessionUtils.LegacyQQ.getAppInterface(), session, pic, 0);
        } catch (Exception e) {
            LogUtils.addRunLog(TAG, e);
        }
    }

    public static void sendReplyMsg(Object session, Object msg) {

//        FinalApiBuilder.builder(MsgApi_sendReply.class, session, msg);
    }

    public static void sendVoice(Object _Session, String path) {
        try {
            if (!path.contains("com.tencent.mobileqq/Tencent/MobileQQ/" + QQEnvTool.getCurrentUin())) {
                String newPath = Environment.getExternalStorageDirectory() + "/Android/data/com.tencent.mobileqq/Tencent/MobileQQ/" + QQEnvTool.getCurrentUin() + "/ptt/" + new File(path).getName();
                FileUtils.copyFile(path, newPath);
                path = newPath;
            }
            Method CallMethod = MethodUtils.findMethod(ClassUtils.getClass("com.tencent.mobileqq.activity.ChatActivityFacade"), null, long.class, new Class[]{ClassUtils.getClass("com.tencent.mobileqq.app.QQAppInterface"), ClassUtils.getClass("com.tencent.mobileqq.activity.aio.SessionInfo"), String.class});
            CallMethod.invoke(null, SessionUtils.LegacyQQ.getAppInterface(), _Session, path);
        } catch (Exception e) {
            LogUtils.addRunLog(TAG, e);
        }
    }

    public static class MsgBuilder {


        //构建要发送的图片消息
        public static Object builderPic(Object session, String path) {
            try {
                Method CallMethod = MethodUtils.findMethod(ClassUtils.getClass("com.tencent.mobileqq.activity.ChatActivityFacade"), null, ClassUtils.getClass("com.tencent.mobileqq.data.ChatMessage"), new Class[]{ClassUtils.getClass("com.tencent.mobileqq.app.QQAppInterface"), ClassUtils.getClass("com.tencent.mobileqq.activity.aio.SessionInfo"), String.class});
                Object PICMsg = CallMethod.invoke(null, SessionUtils.LegacyQQ.getAppInterface(), session, path);
                FieIdUtils.setField(PICMsg, "md5", DataUtils.getFileMD5(new File(path)));
                FieIdUtils.setField(PICMsg, "uuid", DataUtils.getFileMD5(new File(path)) + ".jpg");
                FieIdUtils.setField(PICMsg, "localUUID", UUID.randomUUID().toString());
                MethodUtils.callNoParamsMethod(PICMsg, "prewrite", void.class);
                return PICMsg;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        //如果图片太大压缩后再返回压缩后的图片路径
        private static String checkAndGetCastPic(String Path) {
            File f = new File(Path);
            if (f.exists() && f.length() > 128) {
                try {
                    byte[] buffer = new byte[4];
                    FileInputStream ins = new FileInputStream(f);
                    ins.read(buffer);
                    ins.close();
                    if (buffer[0] == 'R' && buffer[1] == 'I' && buffer[2] == 'F' && buffer[3] == 'F') {
                        Bitmap bitmap = BitmapFactory.decodeFile(Path);
                        String CachePath = PathTool.getModuleCachePath("img") + "/" + DataUtils.getFileMD5(f);
                        BufferedOutputStream bOut = new BufferedOutputStream(new FileOutputStream(CachePath));
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bOut);
                        if (new File(CachePath).length() > 128) {
                            return CachePath;
                        }
                    }
                } catch (Exception e) {

                }

            }
            return Path;
        }
    }
}
