import java.io.OutputStream;
import java.net.Socket;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class Connection {
    private Socket socket;

    public Connection(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket(){
        return socket;
    }


    public void println(byte[] message) {
        try {
            OutputStream os = socket.getOutputStream();
            os.write(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}