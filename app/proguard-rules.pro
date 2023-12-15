# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-optimizationpasses 5
# 保留行数
-keepattributes SourceFile,LineNumberTable
# 混合时不使用大小写混合，混合后的类名为小写
-dontusemixedcaseclassnames

# 指定不去忽略非公共库的类
-dontskipnonpubliclibraryclasses

# 这句话能够使我们的项目混淆后产生映射文件
# 包含有类名->混淆后类名的映射关系
-verbose

# 指定不去忽略非公共库的类成员
-dontskipnonpubliclibraryclassmembers

# 不做预校验，preverify是proguard的四个步骤之一，Android不需要preverify，去掉这一步能够加快混淆速度。
-dontpreverify


-keep class lin.xposed.hook.InitInject {*;}
-keep class lin.xposed.hook.load.** {*;}
-keep class lin.xposed.hook.util.qq.** {*;}
-keep class * extends lin.xposed.hook.load.base.BaseHookItem {*;}

#-keep @lin.xposed.hook.annotation.HookItem class * {*;}

-keep class lin.xposed.hook.view.main.itemview.** {*;}

-keep class * extends android.app.Activity {*;}

-keep class lin.app.main.LinStringForImpl {*;}

#动态字节库 不排除可能会 java.lang.ExceptionInInitializerError
-keep class net.bytebuddy.** {*;}

#java.lang.IllegalStateException: Could not resolve dispatcher: j1.b.translate [class h1.a, class [B, class j1.a, class i1.a, class com.android.dx.dex.file.c]
-keep class com.android.dx.** {*;}


-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
-keep class * implements java.io.Serializable { *; }

#base
-dontwarn javax.**
-dontwarn java.**

#bytebuddy
-dontwarn com.sun.**
-dontwarn edu.umd.**