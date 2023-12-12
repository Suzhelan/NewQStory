package top.linl.dexparser;

public class NewDexFinder {/*
    private DexFinder.Builder builder;
    private static BlockingQueue<DexParser> dexParserSynchronousQueue = new ArrayBlockingQueue<>(2);
    public ArrayList<Method> findMethodByMatcher(BaseMatcher... baseMatcherList) {
        ArrayList<String> resultJson = new ArrayList<>();
        //开始向队列添加文件
        new Thread(()->{
            dexParserSynchronousQueue.put(null);//假设add了一个
        }).start();

        for (BaseMatcher matcher : baseMatcherList) {
            if (matcher instanceof ClassMatcher) {

            }
        }
    }

    *//**
 * The constructor is responsible for local data interaction
 *//*
    public static class Builder {
        private static final ArrayList<DexParser> dexParsersList = new ArrayList<>();
        public static int mThreadSize = 3;

        private final DexFinder dexFinder;
        private final String apkPath;
        private final ZipFile apkZipFile;
        private String cachePath;

        private DexFinder.OnProgress mOnProgress;
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
        public DexFinder.Builder setOnProgress(DexFinder.OnProgress onProgress) {
            this.mOnProgress = onProgress;
            return this;
        }
        *//**
 * Release the cache file and trigger the GC
 *//*
        private void close() {
            if (cachedLocally()) {

                FileUtils.deleteFile(new File(this.cachePath));
            }

            System.gc();
        }
        private boolean cachedLocally() {
            return this.cachePath != null;
        }

        *//**
 * Set cache directory ,
 * If it is never set,
 * the heap memory (mobile phone running memory) will be used as a buffer area,
 * which can bring performance improvement,
 * When the number of DEXs is large or the DEX memory consumption is large,
 * it is best to use this method to set the cache path to prevent insufficient heap memory
 *//*
        public DexFinder.Builder setCachePath(String path) {
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

        public DexFinder.Builder setThreadNumber(int size) {
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
    }*/
}
