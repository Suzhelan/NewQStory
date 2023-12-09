package lin.xposed.hook.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//声明某个类是hook项目 等待被扫描自动编译添加
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface HookItem {
    //功能所在路径+名称 净化/主页净化/底栏精简
    //或 右上角小字/类别名称/又一个小字/功能名称
    String value() default NoPath;

    boolean hasPath() default true;
    //默认无路径的话则不会添加到 UI 中
    String NoPath = "NO_PATH";
}
