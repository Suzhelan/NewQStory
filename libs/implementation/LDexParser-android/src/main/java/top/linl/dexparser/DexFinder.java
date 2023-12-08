package top.linl.dexparser;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import top.linl.dexparser.bean.ids.DexMethodId;
import top.linl.dexparser.bean.ids.DexTypeId;
import top.linl.dexparser.util.DexTypeUtils;
import top.linl.dexparser.util.FileUtils;

/**
 * 由于要读取dex的item 需要创建大量的对象造成大量内存开销
 * 这在安卓上是不可容许的 内存开销大会造成界面无响应
 */
public class DexFinder {

    private Builder builder;


    private DexFinder() throws Exception {

    }

    public static ArrayList<DexParser> getDexParsersList() {
        return Builder.dexParsersList;
    }

    private void init() {

    }

    public ArrayList<Method> findMethodString(String str) throws Exception {
        if (builder.cachedLocally()) {
            return useLocalLookupMethodString(str);
        } else {
            return findMethodAppearedString(str);
        }
    }

    public ArrayList<String> testFindMethodString(String str) throws Exception {
        ArrayList<String> result = new ArrayList<>();
        File[] cacheList = builder.getCacheList();
        ExecutorService findTask = Executors.newFixedThreadPool(3);
        for (File cacheFile : cacheList) {
            findTask.execute(() -> {
                try {
                    DexParser dexParser = (DexParser) FileUtils.readFileObject(cacheFile);
                    result.addAll(testFindStringInWhichMethodAppears(dexParser, str));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        findTask.shutdown();
        findTask.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        return result;
    }

    /**
     * 默认模式查找方法出现的字符串
     */
    private ArrayList<Method> findMethodAppearedString(String string) throws Exception {
        ArrayList<Method> result = new ArrayList<>();
        ExecutorService findTaskList = Executors.newFixedThreadPool(Builder.mThreadSize);
        for (DexParser dexParser : getDexParsersList()) {
            findTaskList.execute(() -> {
                try {
                    result.addAll(findStringInWhichMethodAppears(dexParser, string));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        findTaskList.shutdown();
        findTaskList.awaitTermination(15, TimeUnit.SECONDS);
        return result;
    }

    /**
     * Use local cache mode to find the string in which the method appears
     * <p>
     * {@link Builder#setCachePath(String)}
     *
     * @param str A string constant that appears inside a method
     */
    private ArrayList<Method> useLocalLookupMethodString(String str) throws Exception {
        ArrayList<Method> result = new ArrayList<>();
        File[] cacheList = builder.getCacheList();
        ExecutorService findTask = Executors.newFixedThreadPool(Builder.mThreadSize);
        for (File cacheFile : cacheList) {
            findTask.execute(() -> {
                try {
                    DexParser dexParser = (DexParser) FileUtils.readFileObject(cacheFile);
                    result.addAll(findStringInWhichMethodAppears(dexParser, str));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        findTask.shutdown();
        findTask.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        return result;
    }


    private ArrayList<Method> findStringInWhichMethodAppears(DexParser dexParser, String string) throws NoSuchMethodException {
        ArrayList<Method> result = new ArrayList<>();
        MethodFor:
        for (DexMethodId dexMethodId : dexParser.dexMethodIdsList) {
            if (dexMethodId.getUsedStringList() == null) continue;
            for (Integer integer : dexMethodId.getUsedStringList()) {
                String method_string = dexParser.dexStringIdsList[integer].getString(dexParser);
                if (method_string.contains(string)) {
                    String methodName = dexParser.dexStringIdsList[dexMethodId.name_idx].getString(dexParser);
                    if (methodName.equals("<init>") || methodName.equals("<cinit>")) {
                        continue MethodFor;
                    }
                    String declareClass = dexParser.dexStringIdsList[dexParser.dexTypeIdsList[dexMethodId.class_ids].descriptor_idx].getString(dexParser);
                    DexTypeId[] methodParams = dexMethodId.getMethodParams(dexParser);
                    Class<?> clz = DexTypeUtils.findClass(declareClass);
                    Class<?>[] params = new Class[methodParams.length];
                    for (int i = 0; i < params.length; i++) {
                        String className = dexParser.dexStringIdsList[methodParams[i].descriptor_idx].getString(dexParser);
                        params[i] = DexTypeUtils.findClass(className);
                    }
                    Method method = clz.getDeclaredMethod(methodName, params);
                    result.add(method);
                    continue MethodFor;
                }
            }
        }
        System.gc();//Remind the JVM to free up memory This function is very effective
        return result;
    }

    private ArrayList<String> testFindStringInWhichMethodAppears(DexParser dexParser, String string) {
        ArrayList<String> result = new ArrayList<>();
        MethodFor:
        for (DexMethodId dexMethodId : dexParser.dexMethodIdsList) {
            if (dexMethodId.getUsedStringList() == null) continue;
            for (Integer integer : dexMethodId.getUsedStringList()) {
                String method_string = dexParser.dexStringIdsList[integer].getString(dexParser);
                if (method_string.contains(string)) {
                    String methodName = dexParser.dexStringIdsList[dexMethodId.name_idx].getString(dexParser);
                    String declareClass = dexParser.dexStringIdsList[dexParser.dexTypeIdsList[dexMethodId.class_ids].descriptor_idx].getString(dexParser);
                    DexTypeId[] methodParams = dexMethodId.getMethodParams(dexParser);
                    declareClass = DexTypeUtils.conversionTypeName(declareClass);
                    JSONObject json = new JSONObject();
                    json.put("DeclareClass", declareClass);
                    json.put("MethodName", methodName);
                    JSONArray params = new JSONArray();
                    for (DexTypeId dexTypeId : methodParams) {
                        params.add(DexTypeUtils.conversionTypeName(dexTypeId.getString(dexParser)));
                    }
                    json.put("Params", params);
                    json.put("ReturnType", DexTypeUtils.conversionTypeName(dexMethodId.getReturnType(dexParser).getString(dexParser)));
                    result.add(json.toString());
                    continue MethodFor;
                }
            }
        }
        System.gc();//Remind the JVM to free up memory This function is very effective
        return result;
    }

    public static Builder builder(ClassLoader classLoader,String apkPath) throws Exception {
        return new Builder(classLoader, apkPath);
    }

    public static Builder builder(String apkPath) throws Exception {
        return new Builder(apkPath);
    }
    /**
     *
     */
    public void close() {
        builder.close();
    }

    /**
     * The constructor is responsible for local data interaction
     */
    public static class Builder {
        private static final ArrayList<DexParser> dexParsersList = new ArrayList<>();
        public static int mThreadSize = 3;

        private final DexFinder dexFinder;
        private final String apkPath;
        private final ZipFile apkZipFile;
        private String cachePath;

        private OnProgress mOnProgress;
        public Builder(ClassLoader classLoader, String apkPath) throws Exception {
            DexTypeUtils.setClassLoader(classLoader);
            dexFinder = new DexFinder();
            dexFinder.builder = this;
            this.apkPath = apkPath;
            apkZipFile = new ZipFile(apkPath);
        }

        public Builder(String apkPath) throws Exception {
            dexFinder = new DexFinder();
            dexFinder.builder = this;
            this.apkPath = apkPath;
            apkZipFile = new ZipFile(apkPath);
        }
        public Builder setOnProgress(OnProgress onProgress) {
            this.mOnProgress = onProgress;
            return this;
        }
        /**
         * Release the cache file and trigger the GC
         */
        private void close() {
            if (cachedLocally()) {

                FileUtils.deleteFile(new File(this.cachePath));
            }

            System.gc();
        }
        private boolean cachedLocally() {
            return this.cachePath != null;
        }

        /**
         * Set cache directory ,
         * If it is never set,
         * the heap memory (mobile phone running memory) will be used as a buffer area,
         * which can bring performance improvement,
         * When the number of DEXs is large or the DEX memory consumption is large,
         * it is best to use this method to set the cache path to prevent insufficient heap memory
         */
        public Builder setCachePath(String path) {
            this.cachePath = path;
            return this;
        }

        private boolean cacheToPath(DexParser dexParser) throws IOException {
            String fileName = dexParser.getDexName() + ".parser";
            FileUtils.writeObjectToFile(cachePath + "/" + fileName, dexParser);
            return true;
        }

        private File[] getCacheList() {
            return new File(cachePath).listFiles(pathname -> pathname.isFile() && pathname.getName().endsWith(".parser"));
        }

        public Builder setThreadNumber(int size) {
            mThreadSize = size;
            return this;
        }

        public DexFinder build() throws Exception {
            //start parser all dex
            initializeDexParserList();
            this.dexFinder.init();
            return dexFinder;
        }

        private void initializeDexParserList() throws Exception {

            InputStream inputStream = new FileInputStream(new File(this.apkPath));
            //zip read
            ZipInputStream zipInput = new ZipInputStream(inputStream);
            //task list
            ExecutorService dexInitTask = Executors.newFixedThreadPool(mThreadSize);
            Enumeration<? extends ZipEntry> entries = apkZipFile.entries();
            List<ZipEntry> dexFileList = new ArrayList<>();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".dex")) continue;
                dexFileList.add(entry);
            }
            if (this.mOnProgress != null) mOnProgress.init(dexFileList.size());
            AtomicInteger progress = new AtomicInteger();
            for (ZipEntry entry : dexFileList) {
                dexInitTask.submit(() -> {
                    try {
                        //read dex file stream
                        InputStream stream = this.apkZipFile.getInputStream(entry);
                        byte[] dexData = FileUtils.readAllByte(stream, (int) entry.getSize());
                        stream.close();

                        //start parse the dex File
                        DexParser dexParser = new DexParser(dexData, entry.getName());
                        dexParser.startParse();

                        //LocallyMode , cacheToLocally
                        if (cachedLocally()) {
                            cacheToPath(dexParser);
                        } else {
                            getDexParsersList().add(dexParser);
                        }

                        System.gc();
                        if (this.mOnProgress != null) mOnProgress.parse(progress.getAndIncrement(),entry.getName());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            dexInitTask.shutdown();//stop add task
            while (true) {
                if (dexInitTask.isTerminated()) {
                    System.out.println("init end");
                    break;
                }
                Thread.sleep(1);
            }
            zipInput.close();
            inputStream.close();
        }
    }

    public static interface OnProgress {
        public void init(int dexSize);

        public void parse(int progress,String dexName);
    }
}
