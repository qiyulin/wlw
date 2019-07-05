<%@ page import="java.io.*"%><%@ page import="java.util.Map"%><%@ page import="java.util.HashMap"%><%@ page import="java.text.SimpleDateFormat"%><%@ page import="com.zt.JDBC"%><%@ page import="java.util.List"%><%@ page contentType="application/json;charset=UTF-8" language="java" %>
<%!

//out
private void outJson(int status,String datetime,HttpServletResponse response){
    try{
        PrintWriter pw = response.getWriter();
        if(datetime==null){
            pw.print("{\"status\":"+status+"}");
        }else{
            pw.print("{\"status\":"+status+",\"execute_time\":'"+datetime+"'}");
        }
        pw.close();
    }catch (Exception e){}
}

private Map getCmd(String id){
    String sql = "select * from iot_cmds where id=" + id;
    List<Map> list = JDBC.query(sql);
    return list != null && list.size() > 0 ? (Map)list.get(0) : null;
}
%>
<%
  String id = request.getParameter("cmdid");
  if(id!=null&&!id.equals("")){
      Map info = getCmd(id);
      if(info!=null){
          String datetime = (String)info.get("receive_time");
          int status = (Integer) info.get("status");
          if(status==1){//执行成功
              outJson(200,(String)info.get("execute_time"),response);
          }else{
               long dt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(datetime).getTime()+11*1000;
               long dq = System.currentTimeMillis();
               if(dt>=dq){//在执行时间内
                    outJson(201,null,response);
               }else{
                  outJson(502,null,response);
               }
          }
      }else{
          outJson(503,null,response);
      }
  }else{
      outJson(501,null,response);
  }
%>