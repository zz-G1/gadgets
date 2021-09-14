import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 图片下载类
 */
public class SougouImgPipeline {
    private String extension = ".ipg";
    private String path;
    private volatile AtomicInteger suc;
    private volatile AtomicInteger failed;

    public SougouImgPipeline(){
        setPath("E:/pipeline");
        suc=new AtomicInteger();
        failed=new AtomicInteger();
    }
    public SougouImgPipeline(String path){
        setPath(path);
        suc=new AtomicInteger();
        failed=new AtomicInteger();
    }
    public SougouImgPipeline(String path,String extension){
        setPath(path);
        this.extension=extension;
        suc=new AtomicInteger();
        failed=new AtomicInteger();
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * 下载
     * @param url 下载链接
     * @param cate 存放目录
     * @param name
     * @throws Exception
     */
    private void downloadImg(String url,String cate,String name)throws Exception{
        String path =this.path+"/"+cate+"/";
        File dir = new File(path);
        if (!dir.exists()){
            //不存在则创建
            dir.mkdirs();
        }

        //获取扩展名,判断和extension的相符性还有命名文件名
        String realExt=url.substring(url.lastIndexOf("."));
        //命名文件名
        String fileName=name+realExt;
        //去除文件名中的“-”
        fileName=fileName.replace("-","");
        String filePath=path+fileName;
        //本地下载目录
        File img=new File(filePath);
        if(img.exists()){
            // 若文件之前已经下载过，则跳过
            System.out.println(String.format("文件%s已被下载过",fileName));
            return;
        }

        //打开链接
        URLConnection con=new URL(url).openConnection();

        //connect timeout 是建立连接的超时时间；
        //read timeout， 是传递数据的超时时间
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);
        InputStream inputStream=con.getInputStream();
        byte[] bs=new byte[1024];
        File file =new File(filePath);
        FileOutputStream os=new FileOutputStream(file,true);
        // 开始读取 写入
        int len;
        while ((len=inputStream.read(bs))!=-1){
            os.write(bs,0,len);
        }
        System.out.println("picurl:"+url);
        //原子操作
        System.out.println("正在下载第"+suc.incrementAndGet()+"张图片");
    }

    /**
     * 单线程处理
     * @param data 下载链接集合
     * @param word 关键词
     */
    public void process(List<String> data,String word){
        //获取时间戳
        long start=System.currentTimeMillis();
        for(String picUrl:data){
            if (picUrl==null){
                continue;
            }
            try {
                downloadImg(picUrl,word,picUrl);
            }catch (Exception e){
                failed.incrementAndGet();
            }
        }
        System.out.println("下载成功："+suc.get());
        System.out.println("下载失败："+failed.get());
        long endTime=System.currentTimeMillis();
        System.out.println("耗时"+(endTime-start)/1000+"秒");

    }

    /**
     * 多线程处理
     * @param data 与单线程同
     * @param word 与单线程同
     */
    public void processSync(List<String> data,String word){
        long startTime=System.currentTimeMillis();
        int count=0;
        //创建缓存线程池
        ExecutorService executorService=Executors.newCachedThreadPool();
        for(int i=0;i<data.size();i++){
            String picUrl=data.get(i);
            if (picUrl==null){
                continue;
            }
            String name="";
            if(i<10){
                name="000"+i;
            }else if (i<100){
                name="00"+i;
            }else if(i<1000){
                name="0"+i;
            }
            String fileName=name;
            executorService.execute(()->{
                try{
                    downloadImg(picUrl,word,fileName);
                }catch (Exception e){
                    failed.incrementAndGet();
                }
            });
            count++;
        }
        executorService.shutdown();
        try {
            if(!executorService.awaitTermination(60,TimeUnit.SECONDS)){
                //超时的时候向线程池发出中断（INTERRUPTED）
                executorService.shutdownNow();
            }
            System.out.println("AwaitTermination Finished");
            System.out.println("共有URL："+data.size());
            System.out.println("下载成功："+suc);
            System.out.println("下载失败："+failed);

            File dir=new File(this.path+"/"+word+"/");
            int len=Objects.requireNonNull(dir.list()).length;
            System.out.println("当前共有文件："+len);

            long end=System.currentTimeMillis();
            System.out.println("耗时"+(end-startTime)/1000+"秒");
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }
    public void processSysnc2(List<String> data,final String word,int threadNum){
        if (data.size()<threadNum){
            //如果需处理数据在能力范围内，则执行单线程下载
            process(data,word);
        }else {
            ExecutorService executorService=Executors.newCachedThreadPool();
            //每段要处理的数量
            int num=data.size()/threadNum;
            for (int i=0;i<threadNum;i++){
                int start=i*num;
                int end=(i+1)*num;
                if (i==threadNum-1){
                    end=data.size();
                }
                final List<String> cutList=data.subList(start,end);
                //执行下载
                executorService.execute(()->process(cutList,word));
            }
            //关闭
            executorService.shutdown();
        }
    }

}
