import java.io.*;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author qiyulin  @date 2019.06.17
 * client 测试工具可以使用 socketTool
 * **/
public class IOTServer {

    public static  void main( String args[]){
        new IOTServer().start();//启动
    }
    private static int count = 0;
    private static String port = "9092";
    private static String path = "";
    private static boolean cmdLock = true;
    public static Map<String,ConnectionThread> cacheThread =new HashMap<String,ConnectionThread>();

    static{
        Properties pro = new Properties();
        try {
            pro.load(IOTServer.class.getClassLoader().getResourceAsStream("config.properties"));
            port = pro.getProperty("PORT");
            path = pro.getProperty("LOG");
        } catch (IOException e) {}
    }


    //start
    public void start(){
        final SocketServer server = new SocketServer(Integer.parseInt(port), new MessageHandler() {
            @Override
            public void onReceive(ConnectionThread connectionThread, Connection connection, byte[] message) {
                log("Got a message from "+connection.getSocket().getLocalAddress()+" client:");
                String did = report(message);
                cacheThread.put(did,connectionThread);
                if(!did.equals("")){
                    log("Send '01' back to the client.");
                    connection.println(toBytes("01"));
                }else{
                    log("Send '00' back to the client.");
                    connection.println(toBytes("00"));
                }
            }
        });
        log("Waiting client connect ..."+port);
        //show log
        while(true){
            if(count%60==0){//60秒后执行
                log(server.clearThread());
            }
            if(count%3==0&&cmdLock){//3秒后获取执行命令
                cmdLock = false;
                sendCmd();
                cmdLock= true;
            }
            try { Thread.sleep(1000); }catch (Exception e){  }
            count++;
        }
        //server.close();
    }

    //cmd
    private void sendCmd(){
        List<Map> cmdLines = getCmd();
        for(Map line:cmdLines){ //循环执行可操作命令
            String cmd = (String)line.get("cmd");
            String did = (String) line.get("did");
            System.out.println("GetExecute did->"+did+" cmd->"+cmd);
            if(cmd!=null&&did!=null){
                ConnectionThread ct= cacheThread.get(did);
                if(ct!=null){
                    ct.getConnection().println(toBytes(cmd));
                    log("Sendok client -> did:"+did+",cmd:"+cmd+" ");
                    removeCmd(line.get("id").toString());
                }
            }
        }
    }

    //cmd
    public List<Map> getCmd(){
        return JDBC.getCmd();
    }
    //remove cmd
    public void removeCmd(String id){
        JDBC.removeCmd(id);
    }


    //log record
    public void log(String msg){
        if(msg!=null&&!msg.equals("")){
            String datetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            msg = datetime+" : "+msg;
            if(!"".equals(path)){
                writeFile(path+File.separator+"log.html",msg+"\r\n",true);
            }
            System.out.println(msg);
        }
    }
    public void clear(String msg){
        if(msg!=null&&!msg.equals("")){
            String datetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            msg = datetime+" : "+msg;
            if(!"".equals(path)){
                writeFile(path+File.separator+"log.html",msg+"\r\n",false);
            }
            System.out.println(msg);
        }
    }
    //append file
    private void writeFile(String fileFullPath,String content,boolean b) {
        FileOutputStream fos = null;
        try {
            File f = new File(fileFullPath);
            if(!f.exists()) f.createNewFile();//创建新文件
            //wt
            fos = new FileOutputStream(fileFullPath, b);
            fos.write(content.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            if(fos != null){
                try {
                    fos.flush();
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * @param bytes 收到msg报文
     *
    FEDC01			协议头
    3E0BF339BDF6	设备编号
    00000004		sessionid
    030040			64字节数据

    00000106		空气温度，转10进制 26.2 ℃
    0000013D		空气湿度，以上类推
    0000010F		土壤温度，以上类推
    00000000		土壤湿度，以上此类推
    0000042A		光照强度，以上类推
    000000F5		二氧化碳，以上类推
    00000000		土壤PH  ，以上类推
    00000000		电导率   ，以上类推

    0100			1号继电器 ， 01 开，00 关
    0200			2号继电器 ， 01 开，00 关
    0300			3号继电器 ， 01 开，00 关
    0400			4号继电器 ， 01 开，00 关
    0500			5号继电器 ， 01 开，00 关
    0600			6号继电器 ， 01 开，00 关
    0700			7号继电器 ， 01 开，00 关
    0800			8号继电器 ， 01 开，00 关
    0900			9号继电器 ， 01 开，00 关
    0A00		    10号继电器 ， 01 开，00 关

    010A  			卷被的正转和反转时间，单位：秒
    020B			通风1时间，单位：秒
    030C			通风2时间，单位：秒
    040D			通风3时间，单位：秒
    0000			当前背景图片，0000 （中天） 和 0001 （农嗨）
    003C			上报时间间隔，单位：秒， 默认1分钟



    00
     *
     * 多余丢弃，无用！！！
     * @return 返回封装map
     * */
    public String report(byte[] bytes){
        String hex = byteToHex(bytes);
        log("source->"+hex);
        //FEDC01 为 威海厂家
        if(hex!=null&&(hex.startsWith("FEDC01"))){
            String datetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            try {
                String did = hex.substring(6,18); //did
                //上报数值
                String awd = hex.substring(32,40);//空气温度
                String asd = hex.substring(40,48);//空气湿度
                String lwd = hex.substring(48,56);//土壤温度
                String lsd = hex.substring(56,64);//土壤湿度
                String sun = hex.substring(64,72);//光照强度
                String co2 = hex.substring(72,80);//二氧化碳
                String ph = hex.substring(80,88);//土壤ph
                String ddv = hex.substring(88,96);//电导率
                //上报继电器
                String j01 = hex.substring(98,100);  //继电器01的状态
                String j02 = hex.substring(102,104); //继电器02的状态
                String j03 = hex.substring(106,108); //继电器03的状态
                String j04 = hex.substring(110,112); //继电器04的状态
                String j05 = hex.substring(114,116); //继电器05的状态
                String j06 = hex.substring(118,120); //继电器06的状态
                String j07 = hex.substring(122,124); //继电器07的状态
                String j08 = hex.substring(126,128); //继电器08的状态
                String j09 = hex.substring(130,132); //继电器09的状态
                String j0a = hex.substring(134,136); //继电器10的状态
                //卷被，通风1，通风2，通风3，背景，上报间隔
                String jb_time = hex.substring(136,140); //卷被时间间隔
                String tf1_time = hex.substring(140,144); //通风1时间间隔
                String tf2_time = hex.substring(144,148); //通风2时间间隔
                String tf3_time = hex.substring(148,152); //通风3的时间间隔
                String bg = hex.substring(152,156);//背景图
                String up_time = hex.substring(156,160);//上报间隔
                //新增8，8，8，8

                String phws = "";//风速
                if(hex.length()>168) phws = hex.substring(160,168);
                String phwd = "";//风向
                if(hex.length()>176) phwd = hex.substring(168,176);
                String phqy = "";//气压
                if(hex.length()>184) phqy=hex.substring(176,184);
                String phyl = "";//雨量
                if(hex.length()>192) phyl=hex.substring(184,192);

                if(did!=null&&!did.equals("")){
                    awd = trans(awd,true);
                    asd = trans(asd,true);
                    sun = trans(sun,false);
                    co2 = trans(co2,false);
                    ph = trans(ph,true);
                    lwd = trans(lwd,true);
                    lsd = trans(lsd,true);
                    ddv = trans(ddv,false);
                    jb_time = trans10(jb_time);
                    tf1_time = trans10(tf1_time);
                    tf2_time = trans10(tf2_time);
                    tf3_time = trans10(tf3_time);
                    up_time = trans10(up_time);

                    phws = trans(phws,true);
                    phwd = trans(phwd,true);
                    phqy = trans2(phqy);
                    phyl = trans(phyl,false);

                    //json
                    String json = "{\"datetime\":\""+datetime+"\"," +
                            "\"did\":\""+did+"\"," +
                            "\"awd\":\""+awd+"\"," +
                            "\"asd\":\""+asd+"\"," +
                            "\"sun\":\""+sun+"\"," +
                            "\"co2\":\""+co2+"\"," +
                            "\"ph\":\""+ph+"\"," +
                            "\"lwd\":\""+lwd+"\"," +
                            "\"lsd\":\""+lsd+"\"," +
                            "\"ddv\":\""+ddv+"\"," +
                            "\"phws\":\""+phws+"\"," +
                            "\"phwd\":\""+phwd+"\"," +
                            "\"phqy\":\""+phqy+"\"," +
                            "\"phyl\":\""+phyl+"\"," +
                            "\"j01\":\""+j01+"\"," +
                            "\"j02\":\""+j02+"\"," +
                            "\"j03\":\""+j03+"\"," +
                            "\"j04\":\""+j04+"\"," +
                            "\"j05\":\""+j05+"\"," +
                            "\"j06\":\""+j06+"\"," +
                            "\"j07\":\""+j07+"\"," +
                            "\"j08\":\""+j08+"\"," +
                            "\"j09\":\""+j09+"\"," +
                            "\"j0a\":\""+j0a+"\"," +
                            "\"bg\":\""+bg+"\"," +
                            "\"jb_time\":\""+jb_time+"\"," +
                            "\"tf1_time\":\""+tf1_time+"\"," +
                            "\"tf2_time\":\""+tf2_time+"\"," +
                            "\"tf3_time\":\""+tf3_time+"\"," +
                            "\"up_time\":\""+up_time+"\"}";
                    //输入到日志
                    log(json);
                    //写到json目录/did.json文件中
                    writeJson(json,did);
                }
                return did;
            }catch (Exception e){}
        }else{
            log("Error head -> "+hex);
        }
        return "";
    }

    //16进制转具体值
    private String trans(String info,boolean xs){
        if(info==null||info.length()!=8) return "";
        String result =  Long.parseLong(info,16)+"";
        if(xs) result= Long.parseLong(info,16)*0.1+""; //空气温度
        DecimalFormat format = new DecimalFormat("0.00");
        return format.format(new BigDecimal(result));
    }

    private String trans2(String info){
        if(info==null||info.length()!=8) return "";
        String result = Long.parseLong(info,16)*0.01+"";
        DecimalFormat format = new DecimalFormat("0.00");
        return format.format(new BigDecimal(result));
    }

    //转10进制
    private String trans10(String info){
        if(info==null||info.length()!=4) return "";
        String result =  Long.parseLong(info,16)+"";
        return result;
    }

    /**
     * byte数组转hex
     * @param bytes
     * @return
     */
    private static final char[] HEX_CHAR = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    public String byteToHex(byte[] bytes){
        char[] buf = new char[bytes.length * 2];
        int index = 0;
        for(byte b : bytes) { // 利用位运算进行转换，可以看作方法一的变种
            buf[index++] = HEX_CHAR[b >>> 4 & 0xf];
            buf[index++] = HEX_CHAR[b & 0xf];
        }

        return new String(buf);
    }
    /**
     * string转byte
     * @param str
     * */
    public static byte[] toBytes(String str) {
        if(str == null || str.trim().equals("")) {
            return new byte[0];
        }
        byte[] bytes = new byte[str.length() / 2];
        for(int i = 0; i < str.length() / 2; i++) {
            String subStr = str.substring(i * 2, i * 2 + 2);
            bytes[i] = (byte) Integer.parseInt(subStr, 16);
        }
        return bytes;
    }

    //json make
    public void writeJson(String json,String id){
        writeFile(path+File.separator+"json"+File.separator+id+".json",json,false);
    }
}
