## FileShipper是什么
> FileShipper为您提供一种轻量型方法，用于监听目录和抓取数据。
## FileShipper的特点
>它是一个监听目录下所有新增信息的一个轻量型方法，但他有以下特点。
>* 容错性，当程序宕掉的时候，可以通过偏移量文件恢复对日志的监听，不会重复读取。
>* 文件筛选，选择监听的目录后可配置文件通配符，进行针对性的监听指定文件。
>* 多行匹配，某些日志不是一行一条，而是多行一条，可通过简单配置实现多行日志的抓取。
>* 有较为详细的任务执行日志，保存在logs文件夹当中。
>
>* 准实时性，当监听的目录下有新的日志生成，能迅速获取并且通知程序。

## 如何贡献代码

 > #### 1、请先fork本项目，在你自己的仓库里创建一个副本。
 > 
 > #### 2、进入到你的workspace，执行以下命令下载项目，并且你需要给你的开发工具安装lombok插件，强烈建议使用idea作为你的开发工具。
 > 
 > ```
 > git clone https://github.com/.../FileShipper.git
 > ```
 > 
 > #### 3、进入到clone好的FileShipper目录里，执行以下命令，将你的副本地址加入到你的远端仓库中。
 > 
 > ```
 > git remote add remote [你fork的地址，比如：git@github.com:your_username/FileShipper.git]
 > ```
 > 
 > #### 4、进行你想要进行的任何代码修改。
 > 
 > #### 5、提交之前，请先执行以下命令与主库的代码保持同步。在这个过程中，你可能需要处理冲突。(PS：该过程必须要经常做，时刻让本地的代码与主库代码保持一致，这样做有助于减少冲突。)
 > 
 > ```
 > git pull origin master
 > ```
 > 
 > #### 6、当你处理完冲突，并在本地测试完毕以后，请执行以下命令先提交到自己fork的副本仓库。
 > 
 > ```
 > git push remote master
 > ```
 > 
 > #### 7、在github上面创建一个Pull Request，将你的代码提交到主库。
 >
 > #### 8、重复4-7步即可持续贡献你的代码。

## 项目结构
 > #### 整体结构：
 > ![大叔眼睛泽](https://github.com/xianshengzheng/FileShipper/blob/xianshengzheng-image/images/%E9%A1%B9%E7%9B%AE%E7%BB%93%E6%9E%84.png) 
 > #### FileShipper核心分为五大模块：
  > ###### 1、	目录监听器  
 >* 启动目录监听器前需要注册监听的目录，当成功注册后，该目录下文件的增删改操作都会触发一个事件，触发条件和事件如下：  
 >~~~~
 > (1)、Windows：  
 > 创建文件：创建，修改  
 > 删除文件：删除  
 > 修改文件：修改，修改  
 > 重命名文件：删除，创建，修改  
 > (1)、Linux：  
 > 创建文件：创建，修改   
 > 删除文件：删除  
 > 修改文件：修改  
 > 重命名文件：删除，创建  
 >~~~~
 >*	当触发事件的时候会将修改的文件存储到待读队列中排队。如果队列中有该文件，那么此次事件将被忽略。
 >*	当触发删除事件的时候，会将文件的偏移量从内存信息中删除，待到下一次偏移量监控器将偏移量存储磁盘的时候，会删除该文件在本地偏移量文件中的信息。
 >
 > ###### 2、	执行器
 >* 执行器本质是一个线程池，通过默认的配置可创建一个大小为1的线程池，不断监控并消费待读队列。
 >* 当执行器`第一次启动`的时候首先判断偏移量文件是否存在，当偏移量文件不存在的时候根据配置信息是否忽略老日志。当偏移量文件存在，则可以判断程序宕机，此时根据偏移量文件恢复对日志的监听。
 >
 > ###### 3、	偏移量监控器
 >* 读取本地偏移量文件
 >* 周期性将偏移量文件存储到磁盘
 >
 > ###### 4、	待读队列
 >*	本质为LinkHashMap的的队列，每次目录监听器触发事件后，会将触发事件的文件放到待读队列中进行排队
 >*	执行器每次消费的是队列的第一个文件。并在消费前将该文件从队列中移除
 >
 > ###### 5、	本地偏移量文件
 >* 存储的是序列化后的map对象。文件启动后会先读取偏移量文件。并且在启动的时候会保证监听目录中的文件与本地偏移量文件所记录的信息是到一致的。

 ## 配置信息说明
>           配置参数
>       {
> 			"readPath" : "/root/log",               #必填项。监听的目录
> 			"fileNameMatch": "1*.txt",              #可选项。文件通配符，不填默认监听所有文件
> 			"threadPollMax": "1",                   #可选项。读取文件的线程池的最大值，默认1
> 			"ignoreOld": "false",                   #可选项。程序第一次启动是否忽略老日志，默认false（读取所有日志）
> 			"ignoreFileOfTime": "86400",            #可选项。程序第一次启动的时候，在不忽略老日志的情况下，忽略多少秒没更新的日志,默认24小时
> 			"saveOffsetTime": "5",                  #可选项。多少秒记录一次偏移量，默认5秒
> 			"encoding": "utf8",                     #可选项。编码格式，默认utf8
> 			"secondOfRead": "5",                    #可选项。积累操作时间，默认5秒
> 			"multilineRule": {                      #填写这个配置的时候为多行匹配，不填则为单行
> 			"keyRegx": "<137>"                      #必填项（当multiline为true时）。匹配关键行的正则表达式
> 			"maxAge": 3600000,                      #可选项。最大等待时间，不写默认为60000
> 			"lines": 50,                            #可选项。匹配的最大行数，不写默认为50
> 			"what": "next"                          #可选项。向前或向后合并，有next和previous两种值，默认为next
> 			}
> 		}

## 如何使用
 > 在FileShipper程序中有一个main方法,执行后会对C:\\testShipper目录下所有以text开头.txt结尾的文件进行监控。并且将偏移量文件信息存在在当前项目下。开启多行匹配，说明该日志下记录的每条数据是多行，并且以<129>开头。每次抓取的都是以<129>开头的多行数据。具体如下：
 > 
```
    public static void main(String args[]){
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
```
 ## 未解决问题
  > 1、在linux下可以根据inode来追踪到老日志，防止重命名后的日志被重复读取。但是windows下无法判断更名后的日志是否是已经读过的，所以此时会将更名后的文件当成一个新的文件重新读取。
 

