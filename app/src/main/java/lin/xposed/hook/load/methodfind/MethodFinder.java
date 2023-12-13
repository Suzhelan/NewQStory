package lin.xposed.hook.load.methodfind;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import lin.util.ReflectUtils.ClassUtils;
import lin.util.ReflectUtils.MethodUtils;
import top.linl.dexparser.DexFinder;

public class MethodFinder {
    private final HashMap<String, Method> container = new HashMap<>();
    private final DexFinder dexFinder;

    public MethodFinder(DexFinder dexFinder) {
        this.dexFinder = dexFinder;
    }

    public Method[] findMethodString(String str) throws Exception {
        return dexFinder.findMethodString(str).toArray(new Method[0]);
    }

    public final Method getMethod(String methodId) {
        return container.get(methodId);
    }

    public final void putMethod(String methodId, Method method) {
        container.put(methodId, method);
    }

    private JSONObject getFindResults() {
        JSONObject result = new JSONObject();
        for (Map.Entry<String, Method> entry : container.entrySet()) {
            try {
                result.put(entry.getKey(), getMethodInfo(entry.getValue()));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    private void loadAllMethod(JSONObject methodInfoList) throws Exception {
        Iterator<String> iterator = methodInfoList.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Method method = findMethodByJSON(methodInfoList.getJSONObject(key));
            container.put(key, method);
        }
    }

    private Method findMethodByJSON(JSONObject methodInfo) throws Exception {
        String methodName = methodInfo.getString("MethodName");
        String declareClass = methodInfo.getString("DeclareClass");
        String ReturnType = methodInfo.getString("ReturnType");
        JSONArray methodParams = methodInfo.getJSONArray("Params");
        Class<?>[] params = new Class[methodParams.length()];
        for (int i = 0; i < params.length; i++) {
            params[i] = ClassUtils.getClass(methodParams.getString(i));
        }
        return MethodUtils.findMethod(declareClass, methodName, ClassUtils.getClass(ReturnType), params);
    }

    private JSONObject getMethodInfo(Method method) {
        try {
            method.setAccessible(true);
            JSONObject result = new JSONObject();
            String methodName = method.getName();
            String declareClass = method.getDeclaringClass().getName();
            Class<?>[] methodParams = method.getParameterTypes();
            JSONArray params = new JSONArray();
            for (Class<?> type : methodParams) {
                params.put(type.getName());
            }
            result.put("DeclareClass", declareClass);
            result.put("MethodName", methodName);
            result.put("Params", params);
            result.put("ReturnType", method.getReturnType().getName());
            return result;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

}
