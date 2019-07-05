import java.sql.*;

import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class JDBC {
    /**
     * 新增表数据库命令队列
     * CREATE TABLE `iot_cmds` (
     *   `id` int(11) NOT NULL AUTO_INCREMENT,
     *   `did` varchar(12) DEFAULT NULL COMMENT '设备id',
     *   `cmd` varchar(255) DEFAULT NULL COMMENT '命令详情',
     *   `status` int(11) DEFAULT NULL COMMENT '执行状态 0 未执行 1已经执行',
     *   `receive_time` varchar(20) DEFAULT NULL COMMENT '收到时间',
     *   `execute_time` varchar(20) DEFAULT NULL COMMENT '执行时间',
     *   PRIMARY KEY (`id`)
     * ) ENGINE=MyISAM AUTO_INCREMENT=5 DEFAULT CHARSET=utf8
     * */
    private static void log(String str){
        System.out.println(str);
    }
    //jdbc
    private static String driver = "com.mysql.jdbc.Driver";
    private static String url="jdbc:mysql://localhost:3306/zt_ncc?useUnicode=true&characterEncoding=utf-8";
    private static String user = "xx";
    private static String pwd = "xx";
    private static int cha = 10*1000; //10s
    //open
    public static Connection open(){
        try {
            Class.forName(driver).newInstance();
            Connection con= DriverManager.getConnection(url, user, pwd);
            return con;
        } catch (Exception e) {
            e.printStackTrace();
            log("open error");
        }
        return null;
    }
    public static void close(Connection con){
        if(con!=null)
            try {
                con.close();
            } catch (SQLException e) {
                log("close error");
            }
    }
    public static List query(String sql){
        log(sql);
        Connection con = open();
        try{
            List list = new ArrayList();
            Statement stat = con.createStatement();
            ResultSet rs = stat.executeQuery(sql);
            ResultSetMetaData m=rs.getMetaData();
            int columns=m.getColumnCount();
            while(rs.next()){
                Map<String,Object> map = new HashMap<String,Object>();
                for(int i=1;i<=columns;i++)
                {
                    String name = m.getColumnName(i);
                    Object obj =rs.getObject(name);
                    map.put(name,obj);
                }
                list.add(map);
            }
            return list;
        }catch(Exception e){
            log("query error");
        }finally{
            close(con);
        }
        return null;
    }
    public static void execute(String sql){
        log(sql);
        Connection con = open();
        try{
            Statement stat = con.createStatement();
            stat.executeUpdate(sql);
        }catch(Exception e){
            log("insert error");
        }finally{
            close(con);
        }
    }
    public static int execute_id(String sql){
        log(sql);
        Connection con = open();
        try{
            Statement stat = con.createStatement();
            stat.executeUpdate(sql);
            ResultSet results = stat.getGeneratedKeys();
            if(results.next()){
                return results.getInt(1);
            }
        }catch (Exception e) {
            log("execute_id error");
        }finally{
            close(con);
        }
        return 0;
    }


    //getCmd
    public static List<Map> getCmd(){
        String datetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        long ms = System.currentTimeMillis()-cha;
        String sdatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(ms));
        String sql = "select * from iot_cmds where status=0 and receive_time between '"+sdatetime+"' and '"+datetime+"'";
        List<Map> list = query(sql);
        return list;
    }

    //removeCmd
    public static void removeCmd(String id){
        String datetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String sql ="update iot_cmds set status=1,execute_time='"+datetime+"' where id="+id;
        execute(sql);
    }
    //addCmd
    public static int addCmd(String did,String cmd){
        String datetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String sql = "insert into iot_cmds(did,cmd,status,receive_time) values ('"+did+"','"+cmd+"',0,'"+datetime+"')";
        return execute_id(sql);
    }
    public static Map getCmd(String id){
        String sql = "select * from iot_cmds where id="+id;
        List<Map> list = query(sql);
        if(list!=null&&list.size()>0) return list.get(0);
        return null;
    }
}
