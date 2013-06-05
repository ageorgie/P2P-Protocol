package ece454p1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.util.BitSet;
import java.util.HashMap;
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
                System.out.println("top of while loop");
                Socket client = serverSocket.accept();
                System.out.println("Receiver: New connection accepted.");
//                if()
//                Map<String, Map<String, BitSet>> emptyMap = new HashMap<String, Map<String, BitSet>>();
//                emptyMap.put(client.)
//                Peer.getPeers().updatePeerFileMap();
                Object obj = new Object();
                InputStream is = client.getInputStream();
                System.out.printf("inputstream.available: %d\n", is.available());
                ObjectInputStream ois = new ObjectInputStream(is);
                System.out.println("after ois");

                try {
                    obj = ois.readObject();
                		System.out.println(obj.getClass());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                if(obj.getClass().isAssignableFrom(Chunk.class)) {
                    Chunk chunk = (Chunk) obj;
                    Peer.ReceiveChunk(chunk);
                } else if (obj.getClass().isAssignableFrom(HashMap.class)) {
                    Map<String, Map<String, BitSet>> bitSetMap = (Map<String, Map<String, BitSet>>) obj;
                    Peer.getPeers().updatePeerFileMap(bitSetMap);
                    System.out.println(bitSetMap);
                } else {
                    throw new Exception("Received object type is not recognized");
                }
            }
        } finally {
            serverSocket.close();
        }
    }
}
