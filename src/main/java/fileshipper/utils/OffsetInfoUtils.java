package fileshipper.utils;


import fileshipper.FileShipper;
import fileshipper.info.FileOffsetInfo;
import org.apache.log4j.Logger;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Eric
 * @Title: OffsetInfoManage
 * @date 2018/11/28 16:20
 * @Description: 管理offset的写操作和file对应key的读取
 */
public class OffsetInfoUtils implements Runnable {
    private static Logger LOGGER = Logger.getLogger(OffsetInfoUtils.class);
    public static String ROOTPATH = Paths.get(System.getProperty("user.dir")).toString();
    private static Map offsetMap;
    private static int time;
    private static String offsetPath;

    public OffsetInfoUtils(String offsetPath,Map offsetMap, int time) {
        this.offsetMap = offsetMap;
        this.time = time;
        this.offsetPath = offsetPath;
    }


    public Map  getOffsetMap() {
        FileInputStream freader;
        HashMap<String, FileOffsetInfo> map = null;
        try {
            //测试环境
//            File file = Paths.get(System.getProperty("user.dir")+"\\offsetMap.txt").toFile();
            File file = Paths.get(offsetPath).toFile();
            freader = new FileInputStream(file);

            ObjectInputStream objectInputStream = new ObjectInputStream(freader);

            new HashMap<String, FileOffsetInfo>();

            map = (HashMap<String, FileOffsetInfo>) objectInputStream.readObject();

            freader.close();
        } catch (FileNotFoundException e) {
            //  e.printStackTrace();
            LOGGER.debug("创建偏移量信息文件");
        } catch (Exception e) {
            LOGGER.error("获取偏移量信息出错");
        }

        return map;

    }

    @Override
    public void run() {
        while (true) {
            FileOutputStream outStream = null;
            ObjectOutputStream objectOutputStream = null;
            try {
                //测试环境
                File file = Paths.get(offsetPath).toFile();
                outStream = new FileOutputStream(file);

                objectOutputStream = new ObjectOutputStream(outStream);

                objectOutputStream.writeObject(offsetMap);

                objectOutputStream.close();
                outStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            } finally {

            }
            try {
                Thread.sleep(time * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }


    /**
     * @return java.lang.Long
     * @Author Eric Zheng
     * @Description 如果系统为linux则用日志inod来确定文件标识符
     * @Date 14:45 2018/11/29
     * @Param [file]
     **/
    public static Long getInode(File file) {


        String property = System.getProperty("os.name");

        if (property.startsWith("Linux")) {
            Integer inodeNo = null;
            try {
                Path path = Paths.get(file.getPath());
                BasicFileAttributes inode = Files.readAttributes(path, BasicFileAttributes.class);
                Object StringMap = inode.fileKey();     //(dev=fd00,ino=203392031)
                inodeNo = Integer.valueOf(StringMap.toString().split("=")[2].replace(")", ""));
            } catch (IOException e) {
                // e.printStackTrace();
                LOGGER.error("inodeManager:file文件没有找到");
            }
            return Long.valueOf(inodeNo);
        } else if (property.startsWith("Windows")) {
            return Long.valueOf(file.hashCode());
        }else
            return null;

    }
}
