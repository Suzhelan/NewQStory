package top.linl.annotationprocessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ScanFile {
    public static String getProjectRootDirectory() {
        return System.getProperty("user.dir");
    }

    public static File findFile(String packageName) {
        String projectAppCodeDir = getProjectRootDirectory() + "/app/src/main/java";
        File targetFile = new File(projectAppCodeDir, packageName.replace('.', '/'));
        File dir = targetFile.getParentFile();
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) continue;
            String fileName = file.getName().substring(0, file.getName().lastIndexOf("."));
            if (targetFile.getName().equals(fileName)) {
                targetFile = file;
                break;
            }
        }
        return targetFile;
    }

    public static String getItemPath(File file) {
        File path = new File(file.getAbsolutePath());
        String hookItemNoteLine;
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            while ((hookItemNoteLine = reader.readLine()) != null) {
                if (hookItemNoteLine.startsWith("@HookItem")) break;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int startIndex = hookItemNoteLine.indexOf("\"") + 1;
        int endIndex = hookItemNoteLine.lastIndexOf("\"");
        return hookItemNoteLine.substring(startIndex, endIndex);
    }

    public static String getTime() {
        SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd"), df3 = new SimpleDateFormat("HH:mm:ss");
        Calendar calendar = Calendar.getInstance();
        String TimeMsg1 = df1.format(calendar.getTime()), TimeMsg3 = df3.format(calendar.getTime());
        if (TimeMsg1.contains("-0")) {
            TimeMsg1 = TimeMsg1.replace("-0", "-");
        }
        return TimeMsg1 + " " + TimeMsg3;
    }
}
