package fileshipper.utils;

import org.joda.time.DateTime;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Eric
 * @Title: FileUtils
 * @date 2018/12/3 16:33
 * @Description: 多行匹配器
 */
public class MultilineStateMachine {

    private int maxAge = 60000;             //一条数据如果是多行，当始终无法匹配行首时，超过多少毫秒则忽略该日志
    private int lines = 50;
    private String what = "next";
    private Pattern keyRegx;
    private boolean hasKeyLine = false;
    private Queue<String> queue;
    private boolean canOut = false;
    private String outString;
    private long beginTime;

    public MultilineStateMachine(Map arg) {
        if (arg.get("maxAge") != null && !arg.get("maxAge").toString().equals("")) {
            Double tmp = Double.parseDouble(arg.get("maxAge").toString());
            this.maxAge = tmp.intValue();
        }
        if (arg.get("lines") != null && !arg.get("lines").toString().equals("")) {
            Double tmp = Double.parseDouble(arg.get("lines").toString());
            this.lines = tmp.intValue();
        }
        if (arg.get("what") != null) {
            this.what = arg.get("what").toString();
        }
        this.keyRegx = Pattern.compile(arg.get("keyRegx").toString());
        queue = new ArrayBlockingQueue(lines);
    }

    public boolean in(String str) {
        Matcher matcher = keyRegx.matcher(str);
        if (matcher.lookingAt()) { // 找到新的关键行
            beginTime = DateTime.now().getMillis();
            addKeyLine(str);
        } else { // 不是关键行
            if (DateTime.now().getMillis() >= beginTime + maxAge) { // 达到最大等待时间
                this.merge();
            } else {
                addLine(str);
            }
        }
        return canOut;
    }

    /**
     * 添加普通行
     *
     * @param line
     */
    private void addLine(String line) {
        switch (what) {
            case "previous": // 向前合并时，如果队列满了则依次抛弃旧数据
                if (queue.isEmpty()) { // 如果队列为空则设置起始时间
                    beginTime = DateTime.now().getMillis();
                }
                if (!queue.offer(line)) { // 入列失败则删除第一条然后重新入列
                    queue.poll();
                    queue.offer(line);
                }
                break;
            case "next": // 向后合并，如果队列满了则不入列新数据
            default:
                queue.offer(line);
                break;
        }
    }

    /**
     * 添加关键行
     *
     * @param str
     */
    private void addKeyLine(String str) {
        switch (what) {
            case "previous": // 向前合并，将最新入列的关键行和队列前的所有数据合并
                queue.add(str); // 关键行入列
                this.hasKeyLine = true;
                // 合并并输出
                outString = this.merge();
                canOut = true;
                break;
            case "next": // 向后合并，将关键行和之后的非关键行合并到一起
            default:
                if (!queue.isEmpty()) {
                    // 将之前的数据合并输出
                    outString = this.merge();
                    canOut = true;
                }
                // 队列写入新的关键行并记录时间
                queue.add(str);
                this.hasKeyLine = true;
                beginTime = DateTime.now().getMillis(); //向后合并时，以关键行为起始行
                break;
        }
    }

    /**
     * 将当前队列中的数据合并成一条
     *
     * @return
     */
    public String merge() {
        if (!hasKeyLine) { //如果没有关键行则无法合并
            queue.clear(); //清空
            this.hasKeyLine = false;
            return null;
        }
        StringBuffer sb = new StringBuffer();
        while (!queue.isEmpty()) {
            sb.append(queue.poll() + "\n");
        }
        this.hasKeyLine = false;
        return sb.toString().trim();
    }

    public String out() {
        canOut = false;
        return outString;
    }

    /**
     * @return void
     * @Author Eric Zheng
     * @Description 将多行的第一行假设为@。第一条信息的获取需要匹配第二条信息的第一行@。
     * 当第一条信息获取后，queue会设置为空，并将第二条信息的第一行@放进队列中（正常逻辑是没有问题的）
     * 但是，因为要记录偏移量，所以此时偏移量还是放在第二条的第一行@的首部，下一次读取还是读取的第二条信息的第一条
     * 就会不断的读取第二条信息的第一行，造成死循环。所以每一条信息获取后都必须再次手动将队列清空
     * @Date 14:29 2018/12/7
     * @Param []
     **/
//    public void clearqueue() {
//        queue.remove();
//    }

}
