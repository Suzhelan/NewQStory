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
        int theNumberOfStringOccurrences = 0;
        for (Integer string_ids : dexMethodId.getUsedStringList()) {
            String method_string = dexParser.dexStringIdsList[string_ids].getString(dexParser);
            for (String find : findString) {
                if (method_string.contains(find)) {
                    theNumberOfStringOccurrences++;
                }
            }
            if (theNumberOfStringOccurrences >= findString.length) {
                addMethodToResult(dexParser, dexMethodId);
                break;
            }
        }
    }

}
