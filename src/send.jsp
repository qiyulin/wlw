<%@ page import="java.io.*"%><%@ page import="java.util.Map"%><%@ page import="java.util.HashMap"%><%@ page import="java.text.SimpleDateFormat"%><%@ page import="java.util.Date"%><%@ page import="com.zt.JDBC"%><%@ page contentType="application/json;charset=UTF-8" language="java" %>
<%!

//out
private void outJson(int status,int cmdid,HttpServletResponse response){
    try{
        PrintWriter pw = response.getWriter();
        if(cmdid!=0){
            pw.print("{\"status\":"+status+",\"cmdid\":"+cmdid+"}");
        }else{
            pw.print("{\"status\":"+status+"}");
        }
        pw.close();
    }catch (Exception e){}
}

private String buling(int count,String str){
    if(count>=str.length()){
        String res = "";
        for(int i=0;i<(count-str.length());i++){
            res+="0";
        }
        res+=str;
        return res;
    }else{
        return str.substring(0,count);
    }
}
private int addCmd(String did,String cmd){
    String datetime = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date());
    String sql = "insert into iot_cmds(did,cmd,status,receive_time) values ('" + did + "','" + cmd + "',0,'" + datetime + "')";
    return JDBC.execute_id(sql);
}
//cache
private static Map<String,Long> cacheMap = new HashMap<String,Long>();
%>
<%
    if(request.getMethod().toUpperCase().equals("POST")){
        String did = request.getParameter("did");
        String relayno = request.getParameter("relayno"); //继电器编号
        String relayonof = request.getParameter("relayonof");//继电器开关
        String relaytime = request.getParameter("relaytime");//控制时间
        String bg = request.getParameter("bg");//背景图
        String uptime = request.getParameter("uptime");//上传间隔时间
        if(did!=null&&did.length()==12&&
        relayno!=null&&(relayno.equals("01")||relayno.equals("02")||relayno.equals("03")||relayno.equals("04")||relayno.equals("05")||relayno.equals("06")||relayno.equals("07")||
        relayno.equals("08")||relayno.equals("09"))&&
        relayonof!=null&&( relayonof.equals("00") || relayonof.equals("01") )&&
        bg!=null&&( bg.equals("0000") || bg.equals("0001") )&&
        relaytime!=null&&Integer.parseInt(relaytime)<65535&&
        uptime!=null&&Integer.parseInt(uptime)<65535
        ){
            int rt = Integer.parseInt(relaytime);
            int ut = Integer.parseInt(uptime);
            if(rt>=0&&rt<=10000&&ut>=30&&ut<=3600){
                String relaytimeHex = Integer.toHexString(Integer.parseInt(relaytime));
                String uptimeHex = Integer.toHexString(Integer.parseInt(uptime));
                relaytimeHex = buling(4,relaytimeHex);
                uptimeHex =  buling(4,uptimeHex);
                String cmd = "FEDC01"+did+"00000001850002"+relayno+""+relayonof+""+relaytimeHex+""+bg+""+uptimeHex+"00";
                cmd = cmd.toUpperCase();
                System.out.println(cmd);
                if(cmd.length()==50){
                    if(relayonof.equals("01")&&(relayno.equals("01")||relayno.equals("02")||relayno.equals("03")||relayno.equals("04")||relayno.equals("05")
                        ||relayno.equals("06")||relayno.equals("07")||relayno.equals("08"))){
                        String str = "";
                        if(relayno.equals("01")||relayno.equals("02")) str="0102";
                        if(relayno.equals("03")||relayno.equals("04")) str="0304";
                        if(relayno.equals("05")||relayno.equals("06")) str="0506";
                        if(relayno.equals("07")||relayno.equals("08")) str="0708";
                        if(!str.equals("")){
                            Long end_ms2 = cacheMap.get(did+"_"+str);
                            System.out.println(end_ms2+" = "+System.currentTimeMillis());
                            if(end_ms2!=null&&System.currentTimeMillis()<end_ms2){//还在执行中
                                outJson(505,0,response);
                            }else{
                                cacheMap.put(did+"_"+str,(System.currentTimeMillis()+(Integer.parseInt(relaytime)*1000)));
                                int id =addCmd(did,cmd);
                                outJson(200,id,response);
                            }
                        }else{
                            int id = addCmd(did,cmd);
                            outJson(200,id,response);
                        }
                    }else{
                        int id = addCmd(did,cmd);
                        outJson(200,id,response);
                    }

                }else{
                     outJson(503,0,response);
                }
            }else{
                outJson(504,0,response);
            }
        }else{
            outJson(502,0,response);
        }
    }else{
        cacheMap.clear();//1天1清理
        outJson(501,0,response);
    }
%>