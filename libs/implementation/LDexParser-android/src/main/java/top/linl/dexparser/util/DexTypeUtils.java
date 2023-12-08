package top.linl.dexparser.util;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

public class DexTypeUtils {
    private final static Map<String, Class<?>> CLASS_CACHE = new HashMap<>();

    private static ClassLoader loader;

    public static void setClassLoader(ClassLoader classLoader) {
        loader = classLoader;
    }

    /**
     * find class from smali format className
     *
     * @param fullClassName Ljava/lang/fullClassName ? V
     * @return Class
     */
    public static Class<?> findClass(String fullClassName) {
        if (CLASS_CACHE.containsKey(fullClassName)) {
            return CLASS_CACHE.get(fullClassName);
        }
        if (fullClassName.length() == 1) {
            Class<?> simpleType = findSimpleType(fullClassName.charAt(0));
            CLASS_CACHE.put(fullClassName, simpleType);
            return simpleType;
        } else if (isSimpleArray(fullClassName)) {
            Class<?> simpleArrayType = findSimpleTypeArray(fullClassName);
            CLASS_CACHE.put(fullClassName, simpleArrayType);
            return simpleArrayType;
        } else {
            try {
                String className = conversionTypeName(fullClassName);
                Class<?> result;
                if (className.charAt(0) == '[') {
                    result = loader.loadClass(className.replace("[", ""));
                    StringBuilder sb = new StringBuilder(className);
                    for (int i = 0; i < sb.length(); i++) {
                        char c = sb.charAt(i);
                        if (c == '[') {
                            result = Array.newInstance(result, 0).getClass();
                        } else {
                            break;
                        }
                    }
                } else {
                    result = loader.loadClass(className);
                }
                CLASS_CACHE.put(fullClassName, result);
                return result;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * convert SmaliFormat To JavaFormat
     *
     * @param fullType Full ClassName
     */
    public static String conversionTypeName(String fullType) {
        //is base type array
        if (isSimpleArray(fullType)) return findSimpleTypeArray(fullType).getName();
        //is base type
        if (fullType.length() == 1) return findSimpleType(fullType.charAt(0)).getName();
        StringBuilder sb = new StringBuilder(fullType);
        sb.deleteCharAt(sb.indexOf("L"));//delete ["L"java/lang/name;
        sb.deleteCharAt(sb.length()-1);//delete [java/lang/name";"
        int isFlag ;
        do {
            isFlag = sb.indexOf("/");
            if (isFlag != -1) {
                sb.setCharAt(isFlag, '.');
            }
        } while (isFlag != -1);
        return sb.toString();
    }

    public static Class<?> findSimpleTypeArray(String baseClassName) {
        int baseTypeIndex = baseClassName.lastIndexOf('[') + 1;
        Class<?> result = findSimpleType(baseClassName.charAt(baseTypeIndex));
        for (int i = 0; i < baseClassName.length(); i++) {
            char c = baseClassName.charAt(i);
            if (c == '[') {
                result = Array.newInstance(result, 0).getClass();
            } else {
                break;
            }
        }
        return result;
    }

    private static boolean isSimpleArray(String className) {
        int index = className.lastIndexOf('[');
        return index != -1 && className.charAt(index + 1) != 'L';
    }

    /**
     * conversion base type
     *
     * @param simpleType Smali Base Type V,Z,B,I...
     */
    public static Class<?> findSimpleType(char simpleType) {
        switch (simpleType) {
            case 'V':
                return void.class;
            case 'Z':
                return boolean.class;
            case 'B':
                return byte.class;
            case 'S':
                return short.class;
            case 'C':
                return char.class;
            case 'I':
                return int.class;
            case 'J':
                return long.class;
            case 'F':
                return float.class;
            case 'D':
                return double.class;
        }
        throw new RuntimeException("Not an underlying type");
    }
}
