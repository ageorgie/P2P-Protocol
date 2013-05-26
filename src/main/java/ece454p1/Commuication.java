package ece454p1;

import java.io.*;
import java.net.Socket;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: omidmortazavi
 * Date: 2013-05-26
 * Time: 5:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class Commuication {

    Map<String, Socket> socketMap;


    public void send(String host, int port, Serializable object) throws IOException {
        Socket socket = new Socket(host, port);
        OutputStream os = socket.getOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(object);
        oos.close();
        os.close();
        socket.close();
    }
}
