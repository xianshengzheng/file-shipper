package fileshipper.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Eric
 * @Title: FileUtils
 * @date 2018/12/3 16:33
 * @Description: TODO
 */
public class FileUtils{
    //缓存
//    private static final byte[] inbuf = new byte[4096];
    //返回指定的字符集CharSet
//    private static final Charset cset = Charset.forName("utf8");
    //返回虚拟机默认的字符集CharSet
    // private static final Charset cset = Charset.defaultCharset();


    /**
     * @return java.io.File[]
     * @Author Eric Zheng
     * @Description 通配符为空则获取所有文件，通配符不为空则使用通配符匹配文件
     * @Date 17:36 2018/11/22
     * @Param [logPath]
     **/
    public static ArrayList<File> getAllFile(String logPath) {
        File file = new File(logPath);
        if (!file.exists())
            return null;
        File[] files = file.listFiles();    //遍历path下的文件和目录，放在File数组中
        ArrayList<File> allFile = new ArrayList<File>(Arrays.asList(files));
        return allFile;
    }


    /**
     * @return long
     * @Author Eric Zheng
     * @Description 判断该文件是否大于某个时间没有更新
     * @Date 13:48 2018/12/6
     * @Param [file]
     **/
    public static Boolean isIgnoreFile(File file, Long time) {
        long l = file.lastModified();
        if (l + time > new Date().getTime())
            return false;
        return true;
    }


    /**
     * @return boolean
     * @Author Eric Zheng
     * @Description 文件名的正则匹配，匹配成功返回true
     * @Date 16:05 2018/12/6
     * @Param [file, pattern]
     **/
    public static boolean matches(File file, String pattern) {
        if (pattern == null || pattern.trim().length() == 0)
            return true;
        String[] split = pattern.split(",");
        for (String s : split) {
            s = s.replace('.', '#');
            s = s.replaceAll("#", "\\\\.");
            s = s.replace('*', '#');
            s = s.replaceAll("#", ".*");
            s = s.replace('?', '#');
            s = s.replaceAll("#", ".?");
            s = "^" + s + "$";
            Pattern p = Pattern.compile(s);
            Matcher fMatcher = p.matcher(file.getName());
            if (fMatcher.matches()) {
                return true;
            }
        }
        return false;
    }

}
