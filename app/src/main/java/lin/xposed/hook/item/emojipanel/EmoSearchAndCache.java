package lin.xposed.hook.item.emojipanel;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import lin.xposed.hook.util.PathTool;


public class EmoSearchAndCache {
    public static final String PIC_PATH = PathTool.getModuleDataPath() + "/Pic/";

    public static ArrayList<EmoPanel.EmoInfo> searchForEmo(String PathName) {
        String rawPath = PIC_PATH + PathName;
        File[] fs = new File(rawPath).listFiles();

        ArrayList<EmoPanel.EmoInfo> findEmoInfos = new ArrayList<>();
        if (fs != null) {
            TempTimeSortHelper[] temp = new TempTimeSortHelper[fs.length];
            for (int i = 0; i < fs.length; i++) {
                temp[i] = new TempTimeSortHelper();
                temp[i].file = fs[i];
                temp[i].modifiedTime = fs[i].lastModified();
            }

            Arrays.sort(temp, (o1, o2) -> {
                long result = o2.modifiedTime - o1.modifiedTime;
                if (result == 0) return 0;
                return (result < 0) ? -1 : 1;
            });

            for (int i = 0; i < temp.length; i++) {
                fs[i] = temp[i].file;
            }

            for (File f : fs) {
                if (f.isFile() && !f.getName().endsWith(".bak")) {
                    EmoPanel.EmoInfo newInfo = new EmoPanel.EmoInfo();
                    newInfo.type = 1;
                    newInfo.Name = f.getName();
                    newInfo.Path = f.getAbsolutePath();
                    findEmoInfos.add(newInfo);
                }
            }
        }


        return findEmoInfos;
    }

    public static ArrayList<String> searchForPathList() {
        String rawPath = PIC_PATH;
        File[] fs = new File(rawPath).listFiles();
        if (fs == null) return new ArrayList<>();

        ArrayList<String> arrName = new ArrayList<>();
        for (File f : fs) {
            if (f.isDirectory()) {
                arrName.add(f.getName());
            }
        }
        Collections.sort(arrName);
        return arrName;
    }

    public static class TempTimeSortHelper {
        public File file;
        public long modifiedTime;
    }


}
