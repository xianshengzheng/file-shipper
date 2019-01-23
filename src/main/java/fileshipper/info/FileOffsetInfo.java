package fileshipper.info;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Eric
 * @Title: FilesetInfo
 * @date 2018/11/22 17:25
 * @Description:
 */
public class FileOffsetInfo implements Serializable {
    private long lastTimeFileSize;
    private Date lastUpdateTime;
    private long lastRealSize;

    public FileOffsetInfo(long lastTimeFileSize, Date lastUpdateTime) {
        this.lastTimeFileSize = lastTimeFileSize;
        this.lastUpdateTime = lastUpdateTime;
    }

    public FileOffsetInfo(long lastTimeFileSize, Date lastUpdateTime, long lastRealSize) {
        this.lastTimeFileSize = lastTimeFileSize;
        this.lastUpdateTime = lastUpdateTime;
        this.lastRealSize = lastRealSize;
    }

    public long getLastRealSize() {
        return lastRealSize;
    }

    public void setLastRealSize(long lastRealSize) {
        this.lastRealSize = lastRealSize;
    }

    public long getLastTimeFileSize() {
        return lastTimeFileSize;
    }

    public void setLastTimeFileSize(long lastTimeFileSize) {
        this.lastTimeFileSize = lastTimeFileSize;
    }


    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Date lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    @Override
    public String toString() {
        return "FileOffsetInfo{" +
                "lastTimeFileSize=" + lastTimeFileSize +
                ", lastUpdateTime=" + lastUpdateTime +
                ", lastRealSize=" + lastRealSize +
                '}';
    }
}
