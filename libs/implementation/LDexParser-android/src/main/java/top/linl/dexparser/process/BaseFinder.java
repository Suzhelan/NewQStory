package top.linl.dexparser.process;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;
import java.util.List;

import top.linl.dexparser.DexParser;
import top.linl.dexparser.bean.ids.DexMethodId;
import top.linl.dexparser.bean.ids.DexTypeId;
import top.linl.dexparser.util.DexTypeUtils;

public abstract class BaseFinder {
    private final ArrayList<String> result = new ArrayList<>();

    /**
     * 每解析到一个dex会调用此方法
     */
    public void startFind(DexParser dexParser) {
        for (DexMethodId dexMethodId : dexParser.dexMethodIdsList) {
            String methodName = dexParser.dexStringIdsList[dexMethodId.name_idx].getString(dexParser);
            if (methodName.equals("<init>") || methodName.equals("<cinit>")) {
                continue;
            }
            startParserMethodId(dexParser, dexMethodId);
        }
    }


    public abstract void startParserMethodId(DexParser dexParser, DexMethodId dexMethodId);

    protected final void addMethodToResult(DexParser dexParser, DexMethodId dexMethodId) {
        result.add(getMethodJSON(dexParser, dexMethodId));
    }

    protected final ArrayList<String> getResult() {
        return this.result;
    }

    protected <T> boolean checkListContain(List<T> list, T[] array) {
        int targetSize = 0;
        for (T value : list) {
            if (targetSize >= array.length) break;
            for (T arrayValue : array) {
                if (value.equals(arrayValue)) targetSize++;
            }
        }
        return targetSize >= array.length;
    }

    /**
     * 从一个method id解析出对应信息的JSON String
     */
    protected final String getMethodJSON(DexParser dexParser, DexMethodId dexMethodId) {
        String methodName = dexParser.dexStringIdsList[dexMethodId.name_idx].getString(dexParser);
        String declareClass = dexParser.dexStringIdsList[dexParser.dexTypeIdsList[dexMethodId.class_ids].descriptor_idx].getString(dexParser);
        declareClass = DexTypeUtils.conversionTypeName(declareClass);
        DexTypeId[] methodParams = dexMethodId.getMethodParams(dexParser);
        JSONObject json = new JSONObject();
        json.put("DeclareClass", declareClass);
        json.put("MethodName", methodName);
        JSONArray params = new JSONArray();
        for (DexTypeId dexTypeId : methodParams) {
            params.add(DexTypeUtils.conversionTypeName(dexTypeId.getString(dexParser)));
        }
        json.put("Params", params);
        json.put("ReturnType", DexTypeUtils.conversionTypeName(dexMethodId.getReturnType(dexParser).getString(dexParser)));
        return json.toString();
    }
}
