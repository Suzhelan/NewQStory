package lin.xposed.hook;


import java.util.concurrent.atomic.AtomicBoolean;

import lin.util.ReflectUtils.ClassUtils;

public class QQVersion {
    public static final int QQ_8_9_35 = 3814;
    public static final int QQ_8_9_38 = 3856;
    public static final int QQ_8_9_50 = 3898;
    public static final int QQ_8_9_58 = 4106;
    public static final int QQ_8_9_70 = 4430;
    public static final int QQ_8_9_73 = 4416;
    public static final int QQ_8_9_78 = 4548;
    public static final int QQ_8_9_90 = 4938;
    public static final int QQ_8_9_93 = 5028;
    private static AtomicBoolean isQQNT;

    /* Q_8_9_70(4330),
     Q_8_9_71(4332),
     Q_8_9_73(4416),
     Q_8_9_75(4482),
     Q_8_9_76(4484),
     Q_8_9_78(4548),
     Q_8_9_80(4614),

     Q_8_9_83(4680),

     Q_8_9_85(4766);*/
    public static boolean isQQNT() {
        if (isQQNT != null) return isQQNT.get();
        isQQNT = new AtomicBoolean();
        try {
            ClassUtils.getClass("com.tencent.mobileqq.startup.step.LoadData");
            isQQNT.set(false);
        } catch (Exception e) {
            isQQNT.set(true);
        }
        return isQQNT.get();
    }
}
