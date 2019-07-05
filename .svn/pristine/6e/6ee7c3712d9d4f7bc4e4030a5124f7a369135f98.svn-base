import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

class ConnectionThread extends Thread {
    private Socket socket;
    private SocketServer socketServer;
    private Connection connection;
    private boolean isRunning;

    public ConnectionThread(Socket socket, SocketServer socketServer) {
        this.socket = socket;
        this.socketServer = socketServer;
        connection = new Connection(socket);
        isRunning = true;
    }

    @Override
    public void run() {
        while(isRunning) {
            // Check whether the socket is closed.
            if (socket.isClosed()) {
                isRunning = false;
                break;
            }

            try {
                InputStream is = socket.getInputStream();
                if(is.available()>0){
                    byte[] b = new byte[is.available()];
                    is.read(b);
                    socketServer.getMessageHandler().onReceive(this,connection, b);
                }
            }catch (IOException e){}
        }
    }

    public boolean isRunning() {
        return isRunning;
    }
    
    public void stopRunning() {
        isRunning = false;
        try {
            socket.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public Connection getConnection(){
        return connection;
    }
}