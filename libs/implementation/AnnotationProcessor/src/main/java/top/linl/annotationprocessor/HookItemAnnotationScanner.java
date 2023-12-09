package top.linl.annotationprocessor;


import com.google.auto.service.AutoService;

import java.io.IOException;
import java.io.Writer;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

/**
 * 用来自动处理注解
 * 以便不用手动添加需要加载的Hook类
 * 在运行时会自动扫描并加载
 */
@AutoService(Processor.class)
//自动创建\resources\META-INF\services\javax.annotation.processing.Processor写入当前类类名
@SupportedSourceVersion(SourceVersion.RELEASE_17)//版本
@SupportedAnnotationTypes("lin.xposed.hook.annotation.HookItem")//指定只处理哪个注解 如果要处理所有的注解填*
public class HookItemAnnotationScanner extends AbstractProcessor {
    private ArrayList<String> classNameList = new ArrayList<>();

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private void addAsClassArray(StringBuilder builder) {
        //按拼音排序
        classNameList.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return Collator.getInstance(Locale.CHINESE).compare(o1, o2);
            }
        });
        //写文件头
        builder.append("package " + AnnotationClassNameTools.CLASS_PACKAGE + ";\n\n");
        builder.append("import lin.xposed.hook.load.base.BaseHookItem;\n");
        builder.append("public class " + AnnotationClassNameTools.CLASS_NAME + " {\n\n");

        //array
        builder.append("\tprivate static final BaseHookItem[] ALL_HOOK_ITEM_LIST = new BaseHookItem[" + classNameList.size() + "];\n\n");
        //单个方法无法存放太多字符串 因此先拆分初始化方法
        for (int i = 0; i < classNameList.size(); i++) {
            String methodName = "setPathForItem_" + classNameList.get(i).replace('.', '_');
            builder.append("\tprivate void ").append(methodName).append("() {\n");
            String packageName = this.classNameList.get(i);
            builder.append("\t\tALL_HOOK_ITEM_LIST[" + i + "] = new ").append(packageName).append("();\n");
            String itemUiPath = ScanFile.getItemPath(ScanFile.findFile(packageName));
            builder.append("\t\tALL_HOOK_ITEM_LIST[" + i + "]").append(".initPath(\"").append(itemUiPath).append("\");\n");
            builder.append("\t}\n");
        }
        //然后再汇总调用
        builder.append("\tpublic static BaseHookItem[] initAndGetHookItemList() {\n");
        builder.append("\t\t").append(AnnotationClassNameTools.CLASS_NAME).append(" ").append("instance = new ").append(AnnotationClassNameTools.CLASS_NAME).append("();\n");
        for (int i = 0; i < classNameList.size(); i++) {
            //采用实例调用 这样可以不用写这么多静态方法以节省jvm方法区内存
            String methodName = "setPathForItem_" + classNameList.get(i).replace('.', '_');
            builder.append("\t\tinstance.");
            builder.append(methodName).append("();\n");
        }
        builder.append("\t\treturn ALL_HOOK_ITEM_LIST;\n");
        builder.append("\t}");
        //array end

        //build time
        builder.append("\n\tpublic static final String BUILD_TIME =\"").append(ScanFile.getTime()).append("\";");

        builder.append("\n}\n");

    }


    /**
     * @param annotations 所有注解
     * @param roundEnv    environment for information about the current and prior round
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        System.out.println("[start] Start building annotated Hook project class index");
        StringBuilder builder = new StringBuilder();

        for (TypeElement element : annotations) {
            classNameList = initAnnotatedClassList(element, roundEnv);
        }
        addAsClassArray(builder);

        try { // write the file
            JavaFileObject source = processingEnv.getFiler().createSourceFile(AnnotationClassNameTools.CLASS_PACKAGE + "." + AnnotationClassNameTools.CLASS_NAME);
            Writer writer = source.openWriter();
            writer.write(builder.toString());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            // Note: calling e.printStackTrace() will print IO errors
            // that occur from the file already existing after its first run, this is normal
        }
        System.out.println("[End]Index classes have been built for all Hook projects");
        return true;
    }

    private ArrayList<String> initAnnotatedClassList(TypeElement elements, RoundEnvironment roundEnv) {
        ArrayList<String> packageNameList = new ArrayList<>();
        // 获取所有被该注解 标记过的实例
        Set<? extends Element> typeElements = roundEnv.getElementsAnnotatedWith(elements);

        for (Element element : typeElements) {
            //获取被注解的成员变量
            TypeElement typeElement = (TypeElement) element;
            //获取全类名
            String className = typeElement.getQualifiedName().toString();
           /* Annotation itemPathAnnotation = annotatedElement.getAnnotations()[0];
            String itemPath = (String) getAnnotationValue(itemPathAnnotation, "value");
            System.out.println(itemPath);*/
            /*//获取被注解元素的包名
            String classNameList = element.getPackageOf(element).getQualifiedName().toString();
            //取到这个注解元素的包
            String classNameList = element.getEnclosingElement().toString();
            //获取并拼接被注解的类名
            String className = classNameList + "." + element.getSimpleName();*/
            System.out.println("[HookItem]" + className);
            packageNameList.add(className);
        }
        return packageNameList;
    }
}
