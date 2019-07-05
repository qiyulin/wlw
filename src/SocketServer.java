import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

public class SocketServer {
    private ServerSocket serverSocket;
    private ListeningThread listeningThread;
    private MessageHandler messageHandler;

    public SocketServer(int port, MessageHandler handler) {
        messageHandler = handler;
        try {
            serverSocket = new ServerSocket(port);
            listeningThread = new ListeningThread(this, serverSocket);
            listeningThread.start();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public void setMessageHandler(MessageHandler handler) {
        messageHandler = handler;
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }
    
    /*
     * Ready for use.
     */
    public void close() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                listeningThread.stopRunning();
                listeningThread.suspend();
                listeningThread.stop();

                serverSocket.close();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    //线程池清理
    public String clearThread(){
        if(IOTServer.cacheThread!=null){ //根据唯一标准，1个设备对应1个IP和socket通道，如果同一个设备ID对应的connectionThread为多个则关闭旧的线程，统计更新connectionThreads
            Vector<ConnectionThread> cons= listeningThread.getConnectionThreads();
            StringBuilder sb = new StringBuilder();
            sb.append("thread size ->"+cons.size()+", device num ->"+IOTServer.cacheThread.size());
            for (int i = 0; i < cons.size(); i++){
                Set<String> set = IOTServer.cacheThread.keySet();
                Iterator<String> it = set.iterator();
                while(it.hasNext()){
                    String did = it.next();
                    ConnectionThread ct = IOTServer.cacheThread.get(did);
                    try {
                        if (ct.hashCode() != cons.elementAt(i).hashCode()) {
                            sb.append("\t" + cons.elementAt(i).getName() + " close!");
                            cons.elementAt(i).stopRunning(); //关闭某一个
                            cons.removeElementAt(i); //remove 一个
                        } else {
                            sb.append("\tdid->" + did);
                            sb.append(",thread ->" + ct.getName());
                        }
                    }catch (Exception e){}
                }
                return sb.toString();
            }

        }
        return "";
    }

}