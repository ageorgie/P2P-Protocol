package ece454p1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: omidmortazavi
 * Date: 2013-05-26
 * Time: 6:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class Receiver implements Callable<Integer> {
    ServerSocket serverSocket;

    public Receiver(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    public Integer call() throws Exception {
        try {
            while(true) {
    //            System.out.println(serverSocket.isClosed());
                Socket client = serverSocket.accept();
                ObjectInputStream ois;
                Object obj = new Object();
		
                SocketChannel channel = client.getChannel();
		if(channel==null) {
		    continue;
		}
                try {
                    ois = new ObjectInputStream(Channels.newInputStream(channel));
                    obj = ois.readObject();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                if(obj.getClass().isAssignableFrom(Chunk.class)) {
                    Chunk chunk = (Chunk) obj;
                    Peer.ReceiveChunk(chunk);
                } else if (obj.getClass().isAssignableFrom(Map.class)) {
                    Map<String, Map<String, BitSet>> bitSetMap = (Map<String, Map<String, BitSet>>) obj;
                } else {
                    throw new Exception("Received object type is not recognized");
                }
            }
        } finally {
            serverSocket.close();
        }
    }
}
