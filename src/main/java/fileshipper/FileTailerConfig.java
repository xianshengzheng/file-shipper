package fileshipper;


import java.util.Map;

public class FileTailerConfig {

    private final String readPath;                         //监听的目录
    private final int threadPollMax;              //线程池的最大值,
    private final Boolean ignoreOld;               //是否忽略老日志
    private final int saveOffsetTime;             //多少秒记录一次偏移量，默认5秒
    private final Long ignoreFileOfTime;          //程序启动的时候，在不忽略老日志的情况下，忽略多少秒没更新的日志 24小时
    private final String fileNameMatch;            //文件通配符
    private Map<String, String> multilineRule ;
    private final String linesPattern;             //多行正则表达式
    private final int MaxLineNum ;                  //多行匹配的最大行数
    private final int MaxLineSize ;                 //一行的最大字节数
    private final String encoding ;              //编码格式
    private final int secondOfRead;                 //读取事件的间隔时间，真实含义是积累的修改时间，默认积累5秒的事件
    private final String offsetPath;               //偏移量文件的存放信息
    public FileTailerConfig(Map configuration) {
        readPath = (String) configuration.get("readPath");
        threadPollMax = configuration.get("threadPollMax") != null ?
                Integer.valueOf((String) configuration.get("threadPollMax")) : 1;
        ignoreOld = configuration.get("readRail") != null ?
                Boolean.valueOf((String) configuration.get("readRail")) : false;
        saveOffsetTime = configuration.get("ignoreOld") != null ?
                Integer.valueOf((String) configuration.get("readOld")) : 5;
        ignoreFileOfTime = configuration.get("ignoreFileOfTime") != null ?
                Long.valueOf((String) configuration.get("ignoreFileOfTime")) : 86400L;
        fileNameMatch = (String) configuration.get("fileNameMatch");
        multilineRule = configuration.get("multilineRule") != null ?
                (Map<String, String>) configuration.get("multilineRule") : null;
        linesPattern = (String) configuration.get("linesPattern");
        MaxLineNum = configuration.get("MaxLineNum") != null ?
                Integer.valueOf((String) configuration.get("MaxLineNum")) : 50;
        MaxLineSize = configuration.get("MaxLineSize") != null ?
                Integer.valueOf((String) configuration.get("MaxLineSize")) : 4096;
        encoding = configuration.get("encoding") != null ?
                (String) configuration.get("encoding") : "utf8";
        secondOfRead = configuration.get("secondOfRead") != null ?
                Integer.valueOf((String) configuration.get("secondOfRead")) : 5;
        offsetPath = configuration.get("offsetPath") !=null ?
                (String) configuration.get("offsetPath") : System.getProperty("user.dir")+"\\offsetMap.txt";
    }

    public String getReadPath() {
        return readPath;
    }

    public int getThreadPollMax() {
        return threadPollMax;
    }

    public Boolean getIgnoreOld() {
        return ignoreOld;
    }

    public int getSaveOffsetTime() {
        return saveOffsetTime;
    }

    public long getIgnoreFileOfTime() {
        return ignoreFileOfTime;
    }

    public String getFileNameMatch() {
        return fileNameMatch;
    }

    public String getLinesPattern() {
        return linesPattern;
    }

    public int getMaxLineNum() {
        return MaxLineNum;
    }

    public int getMaxLineSize() {
        return MaxLineSize;
    }

    public String getEncoding() {
        return encoding;
    }

    public int getSecondOfRead() {
        return secondOfRead;
    }

    public Map<String, String> getMultilineRule() {
        return multilineRule;
    }

    public String getOffsetPath() {
        return offsetPath;
    }
}
