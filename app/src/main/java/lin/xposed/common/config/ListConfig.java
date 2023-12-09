package lin.xposed.common.config;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import lin.xposed.common.utils.FileUtils;
import lin.xposed.hook.util.PathTool;
import lin.xposed.hook.util.ToastTool;

/**
 * 采用JSON互转 JSON性能优秀开销小
 */
public class ListConfig<T> {
    private static String getPath() {
        return PathTool.getModuleDataPath() + "/data/";
    }

    public static <T> List<T> getList(String FileName) {
        try {
            JSONArray json = new JSONArray(FileUtils.readFileText(getPath() + FileName));
            ArrayList<T> list = new ArrayList<>(json.length());
            for (int i = 0; i < json.length(); i++) {
                list.add((T) json.get(i));
            }
            return list;
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    public static <T> void setListToFile(String fileName, List<T> list) {
        try {
            JSONArray json = new JSONArray(list.toArray());
            FileUtils.writeTextToFile(getPath() + fileName, json.toString(), false);
        } catch (Exception e) {
            ToastTool.show(e);
        }
    }

    public static <T> Map<String, T> getMap(String fileName) {
        try {
            JSONObject jsonObject = new JSONObject(FileUtils.readFileText(getPath() + fileName));
            HashMap<String, T> hashMap = new HashMap<>(jsonObject.length());
            Iterator<String> iterator = jsonObject.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                Object value = jsonObject.get(key);
                hashMap.put( key, (T) value);
            }
            return hashMap;
        } catch (Exception ex) {
            return new HashMap<>();
        }
    }

    public static <T> void setMapToFile(String fileName, Map<String, T> map) {
        try {
            JSONObject json = new JSONObject();
            for (Map.Entry<String,T> entry : map.entrySet()) {
                json.put(entry.getKey(), entry.getValue());
            }
            FileUtils.writeTextToFile(getPath() + fileName, json.toString(), false);
        } catch (Exception e) {
            ToastTool.show(e);
        }
    }
}
