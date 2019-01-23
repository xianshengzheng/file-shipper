package fileshipper;


import fileshipper.info.FileOffsetInfo;
import fileshipper.utils.FileUtils;
import fileshipper.utils.MultilineStateMachine;
import fileshipper.utils.OffsetInfoUtils;
import org.apache.log4j.Logger;


import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.apache.commons.io.IOUtils.EOF;

//import com.boyi.logstream.core.worker.shipper.filev1.utils.MultilineStateMachine;

/**
 * @author ZhengHao
 * @Title: Shipper
 * @date 2018/11/22 13:30
 * @Description: TODO
 */
public class FileShipper {

    private static final String RAF_MODE = "rw";
    private FileTailerConfig config;
    private String readPath;               //监听的目录
    private Integer threadPollMax;        //线程池的最大值
    private Boolean ignoreOld;                   //是否从尾部读取文件
    private int saveOffsetTime;          //多少秒记录一次偏移量
    private Long ignoreFileOfTime;       //忽略多少秒没更新的日志
    private String fileNameMatch;         //文件通配符
    private String linesPattern;          //多行正则表达式
    private Integer MaxLineNum;              //多行匹配的最大行数
    private Integer MaxLineSize;              //一行的最大字节数
    private String encoding;              //编码格式
    private int secondOfRead;
    private String offsetPath;          //偏移量文件的存放路径
    LinkedHashMap<File, Object> needReadFiles = new LinkedHashMap<>();  //存放改动文件的集合，之后用线程去消费
    //监听
    private WatchService watcher;
    private Map<WatchKey, Path> keys;
    private Map<Long, FileOffsetInfo> offsetMap = new HashMap<Long, FileOffsetInfo>();    //偏移量
    private ExecutorService threadPool;   //线程池

    //private static Logger LOGGER = LogManager.getLogger(OffsetInfoUtils.class.getName());
    private static Logger LOGGER = Logger.getLogger(FileShipper.class);

    /**
     * @param
     * @return
     * @Author Eric Zheng
     * @Description 读取配置文件，将配置文件信息保存到configuration,并且传到成员变量当中
     * @Date 13:37 2018/11/22
     * @Param []
     */
    public FileShipper(FileTailerConfig config) {
        this.config = config;
    }


    public boolean register() {

        //加载属性文件
        readPath =config.getReadPath();
        threadPollMax = config.getThreadPollMax();
        ignoreOld = config.getIgnoreOld();
        saveOffsetTime = config.getSaveOffsetTime();
        ignoreFileOfTime = config.getIgnoreFileOfTime();
        fileNameMatch = config.getFileNameMatch();
        linesPattern = config.getLinesPattern();
        MaxLineNum = config.getMaxLineNum();
        MaxLineSize = config.getMaxLineSize();
        encoding = config.getEncoding();
        threadPool = Executors.newFixedThreadPool(threadPollMax);
        secondOfRead = config.getSecondOfRead();
        offsetPath = config.getOffsetPath();
        //从文件中读取偏移量
        OffsetInfoUtils offsetInfoManage = new OffsetInfoUtils(offsetPath ,offsetMap, saveOffsetTime);
        Map<Long, FileOffsetInfo> offsetMapFromFile = offsetInfoManage.getOffsetMap();
        System.out.println(offsetMapFromFile);

        //真实目录的文件
        ArrayList<File> allFile = FileUtils.getAllFile(readPath);
        //当偏移量文件存在的时候，判断真实目录与偏移量目录的对应关系
        if (offsetMapFromFile != null && !offsetMapFromFile.isEmpty()) {
            ArrayList<Long> inodeList = new ArrayList<>();
            //如果真实目录的文件偏移量中没有，则添加
            for (File file : allFile) {
                if (!file.isDirectory() && FileUtils.matches(file, fileNameMatch)) {
                    Long inode = OffsetInfoUtils.getInode(file);
                    if (!offsetMapFromFile.containsKey(inode)) {
                        FileOffsetInfo fileOffsetInfo = new FileOffsetInfo(0, new Date(), file.length());
                        offsetMap.put(inode, fileOffsetInfo);
                        needReadFiles.put(file, null);
                    }
                    inodeList.add(OffsetInfoUtils.getInode(file));
                }
            }
            Iterator<Long> iterator = offsetMapFromFile.keySet().iterator();
            //如果真实目录的文件被删除，则将偏移量中的文件也删除
            if (iterator.hasNext()) {
                Long next = iterator.next();
                if (!inodeList.contains(next)) {
                    offsetMapFromFile.remove(next);
                }
            }
            offsetMap.putAll(offsetMapFromFile);
            //将所有文件存放到待读队列中
            for (File f : allFile) {
                if (!f.isDirectory() && FileUtils.matches(f, fileNameMatch)) {
                    needReadFiles.put(f, null);
                }
            }
        } else {
            //如果忽略之前的日志，则在执行器执行之前，将监听目录下的所有文件的偏移量设置为文件大小
            if (ignoreOld == true) {
                for (File f : allFile) {
                    if (!f.isDirectory() && FileUtils.matches(f, fileNameMatch)) {
                        Long inode = OffsetInfoUtils.getInode(f);
                        FileOffsetInfo fileOffsetInfo = new FileOffsetInfo(f.length(), new Date(), f.length());
                        offsetMap.put(inode, fileOffsetInfo);
                    }
                }
            } else {
                for (File f : allFile) {
                    if (!f.isDirectory() && FileUtils.matches(f, fileNameMatch)) {
                        //不忽略日志则判断是否忽略该日志
                        Long inode = OffsetInfoUtils.getInode(f);
                        FileOffsetInfo fileOffsetInfo = null;
                        if (FileUtils.isIgnoreFile(f, ignoreFileOfTime * 1000)) {

                            LOGGER.debug("文件:'" + f.getName() + "'超过" + ignoreFileOfTime + "秒没有更新，忽略读取");
                            fileOffsetInfo = new FileOffsetInfo(f.length(), new Date(), f.length());
                        } else {
                            fileOffsetInfo = new FileOffsetInfo(0, new Date(), f.length());
                        }
                        offsetMap.put(inode, fileOffsetInfo);
                        needReadFiles.put(f, null);
                    }
                }
            }
        }
        //开启存储偏移量线程
        new Thread(offsetInfoManage).start();

        //注册watcher，监听目录
        try {
            this.watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            e.printStackTrace();
        }
        keys = new HashMap<WatchKey, Path>();
        registerWatch();
        processEvents();

        return true;
    }


    /**
     * @return void
     * @Author Eric Zheng
     * @Description 读操作，进来的每一个文件都创建一个线程调度器来进行读取，每隔几秒读一次，并将偏移量和修改时间写入文件当中
     * 当日志多长时间没有修改的时候关闭此文件的调度
     * @Date 14:19 2018/11/23
     * @Param [file]
     **/
    public void execute() {
        while (true) {
            if (!needReadFiles.isEmpty()) {
                Set<File> files = needReadFiles.keySet();
                Iterator<File> iterator = files.iterator();
                final File needReadFile = iterator.next();
                needReadFiles.remove(needReadFile);
                //在线程启动之前就将文件读取出来，保证线程安全
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        //从队列中读取需要读取的文件
                        //如果文件不为空
                        if (needReadFile.exists()) {
                            Long inode = OffsetInfoUtils.getInode(needReadFile);
                            if (!offsetMap.containsKey(inode)) {
                                LOGGER.debug("'" + needReadFile.getName() + "'为新文件，注册偏移量信息");
                                offsetMap.put(inode, new FileOffsetInfo(0, new Date(), 0));
                            } else {
                                if (needReadFile.length() < offsetMap.get(inode).getLastRealSize()) {
                                    LOGGER.debug("'" + needReadFile.getName() + "'为老文件，但是文件大小小于上次记录，重新读取");
                                    offsetMap.put(inode, new FileOffsetInfo(0, new Date(), 0));
                                }
                            }
                            FileOffsetInfo fileOffsetInfo = offsetMap.get(inode);
                            long lastTimeFileSize = fileOffsetInfo.getLastTimeFileSize();
                            try {
                                RandomAccessFile reader = new RandomAccessFile(needReadFile, RAF_MODE);
                                reader.seek(lastTimeFileSize);
                                long l = readLines(reader, needReadFile);
                                fileOffsetInfo.setLastTimeFileSize(l);
                                fileOffsetInfo.setLastRealSize(needReadFile.length());
                                reader.close();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            LOGGER.debug("'" + needReadFile.getName() + "'为老文件，但是文件大小小于上次记录，重新读取");
                        }
                    }
                });
            } else {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * @return void
     * @Author Eric Zheng
     * @Description 监听指定目录
     * @Date 14:12 2018/11/22
     * @Param
     **/
    public void registerWatch() {
        Path dir = Paths.get(readPath);
        WatchKey key = null;
        try {
            key = dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
        } catch (IOException e) {
            e.printStackTrace();
        }
        keys.put(key, dir);
    }


    /**
     * @return void
     * @Author Eric Zheng
     * @Description 监听目录是否有新的文件生成
     * @Date 14:06 2018/11/23
     * @Param []
     **/

    void processEvents() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    WatchKey key;
                    try {
                        key = watcher.take();
                    } catch (InterruptedException x) {
                        return;
                    }
                    Path dir = keys.get(key);
                    if (dir == null) {
                        System.err.println("操作未识别");
                        continue;
                    }
                    try {
                        //积累触发事件
                        Thread.sleep(secondOfRead * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        String fileName = event.context().toString();
                        String newFileName = keys.get(key) + "/" + fileName;
                        File newFile = new File(newFileName);
                        Long inode = OffsetInfoUtils.getInode(newFile);
                        // 事件可能丢失或遗弃
                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            continue;
                        }
                        if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
//                    System.out.println(kind);
                            LOGGER.debug("文件" + fileName + "被删除或者重命名,如果有偏移量将偏移量删除");
                            offsetMap.remove(inode);
                        }
                        if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            if (FileUtils.matches(newFile, fileNameMatch) && !needReadFiles.containsKey(newFile) && offsetMap.get(inode).getLastRealSize() != newFile.length()) {
//                        System.out.println(kind);
                                needReadFiles.put(newFile, null);
                            }
                        }

                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            if (FileUtils.matches(newFile, fileNameMatch) && offsetMap.get(inode) == null) {
                                needReadFiles.put(newFile, null);
                            }
//                    if (FileUtils.matches(newFile, fileNameMatch) && !needReadFiles.containsKey(newFile) && offsetMap.get(inode).getLastRealSize() != newFile.length()) {
////                        System.out.println(kind);
//                        needReadFiles.put(newFile, null);
//                    }
                        }
                    }
                    key.reset();
                }
            }
        }).start();

    }

    /*
     * @Author Eric Zheng
     * @Description 读方法，一行一行的读，或者多行多行的读
     * @Date 14:19 2019/1/3
     * @Param [reader, needReadFile, encoding, linesPattern, maxLine, maxLineSize]
     * @return long
     **/
    public long readLines(final RandomAccessFile reader, File needReadFile) throws IOException {
        final byte[] inbuf = new byte[MaxLineSize];
        //返回指定的字符集CharSet
        final Charset cset = Charset.forName(encoding);
        //配置多行匹配器
        MultilineStateMachine msm = null;
        if (config.getMultilineRule()!=null) {
            msm = new MultilineStateMachine(config.getMultilineRule());
        }
        try (ByteArrayOutputStream lineBuf = new ByteArrayOutputStream(MaxLineSize)) {
            long pos = reader.getFilePointer();
            long rePos = pos; // position to re-read
            int num;
            boolean seenCR = false;
            //   BufferedWriter br = new BufferedWriter(new FileWriter(new File("C:\\Users\\Eric\\Desktop\\aptTest.txt"), true));//todo 测试代码
            while (((num = reader.read(inbuf)) != EOF)) {
                for (int i = 0; i < num; i++) {

                    final byte ch = inbuf[i];
                    switch (ch) {
                        case '\n':
                            seenCR = false; // swallow CR before LF
                            if (config.getMultilineRule()!=null) {
                                //多行匹配

                                String line = new String(lineBuf.toByteArray(), cset);
                                if (msm.in(line)) {
                                    String out = msm.out();
                                   // msm.clearqueue();
                                    sendEvent(needReadFile, out);
                                    int x = i - lineBuf.toByteArray().length - 2;
                                    rePos = pos + x + 1;

                                }
                                lineBuf.reset();
                            } else {
                                //单行匹配
                                sendEvent(needReadFile, new String(lineBuf.toByteArray(), cset));
                                lineBuf.reset();
                                rePos = pos + i + 1;
                            }
                            break;
                        case '\r':
                            if (seenCR) {
                                lineBuf.write('\r');
                            }
                            seenCR = true;
                            break;
                        default:
                            lineBuf.write(ch);
                    }

                }
                pos = reader.getFilePointer();
            }
            reader.seek(rePos); // Ensure we can re-read if necessary

            return rePos;
        }
    }

    /*
     * @Author Eric Zheng
     * @Description 发送Event到lv1cache
     * @Date 15:05 2019/1/3
     * @Param [needReadFile, br, out]
     * @return void
     **/


    private void sendEvent(File needReadFile, String out) throws IOException {
        System.out.println(needReadFile.getName() + "添加了信息:" + out);
    }


    public static void main(String args[]) throws IOException, InterruptedException {

        //配置文件
        Map conf = new HashMap();
        conf.put("readPath", "C:\\testShipper");                                    //监听某个目录
        conf.put("fileNameMatch", "test*.txt");                                     //文件通配符
        conf.put("offsetPath", System.getProperty("user.dir")+"\\offsetMap.txt");   //将偏移量文件存放在项目根目录下
        Map multilineRule = new HashMap();                                          //开启多行匹配
        multilineRule.put("keyRegx", "<129>");                                      //匹配每条数据的第一个行开头的正则
        multilineRule.put("lines", "50");                                           //允许匹配的最大行数
        conf.put("multilineRule", multilineRule);
        FileTailerConfig fileTailerConfig = new FileTailerConfig(conf);
        FileShipper fileShipper = new FileShipper(fileTailerConfig);
        //初始化程序
        fileShipper.register();
        //启动程序
        fileShipper.execute();

    }



}
