package lin.xposed.common.config;

import org.json.JSONException;
import org.json.JSONObject;

import lin.xposed.common.utils.FileUtils;
import lin.xposed.hook.util.PathTool;

public class SimpleConfig {
    private JSONObject dataReader;
    private final String fileName;
    private String getPath() {
        return PathTool.getModuleDataPath() + "/data/simple/" + fileName;
    }

    public SimpleConfig(String fileName) {
        this.fileName = fileName;
        try {
            this.dataReader = new JSONObject(FileUtils.readFileText(getPath()));
        } catch (Exception e) {
            this.dataReader = new JSONObject();
        }
    }

    public <T> T get(String key) {
        try {
            return (T) dataReader.get(key);
        } catch (JSONException e) {
            return null;
        }
    }

    public <T> T get(String key, T defaultValue) {
        try {
            return (T) dataReader.get(key);
        } catch (JSONException e) {
            return defaultValue;
        }
    }

    public void put(String key, Object value) {
        try {
            dataReader.put(key, value);
        } catch (JSONException e) {

        }
    }

    public void remove(String key) {
        dataReader.remove(key);
    }

    public void removeAll() {
        this.dataReader = new JSONObject();
    }

    public boolean submit() {
        try {
            FileUtils.writeTextToFile(getPath(), dataReader.toString(), false);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
