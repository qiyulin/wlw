public interface MessageHandler {
    public void onReceive(ConnectionThread connectionThread,Connection connection, byte[] message);
}