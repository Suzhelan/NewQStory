package top.linl.dexparser.process;

import top.linl.dexparser.DexParser;
import top.linl.dexparser.bean.ids.DexMethodId;

public class MethodStringFinder extends BaseFinder {
    private final String[] findString;

    public MethodStringFinder(String... str) {
        this.findString = str;
    }

    @Override
    public void startParserMethodId(DexParser dexParser, DexMethodId dexMethodId) {
        if (dexMethodId.getUsedStringList() == null) return;
        for (Integer string_ids : dexMethodId.getUsedStringList()) {
            String method_string = dexParser.dexStringIdsList[string_ids].getString(dexParser);
            if (method_string.contains(findString)) {
                String methodName = dexParser.dexStringIdsList[dexMethodId.name_idx].getString(dexParser);
                if (methodName.equals("<init>") || methodName.equals("<cinit>")) {
                    continue;
                }
                addMethodToResult(dexParser, dexMethodId);
            }
        }
    }

}
