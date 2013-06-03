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
    //            System.out.println(serverSocket.isClosed());
                Socket client = serverSocket.accept();
								System.out.println("Am I here?");
                Object obj = new Object();
								
								InputStream is = client.getInputStream();
								ObjectInputStream ois = new ObjectInputStream(is);
								
								System.out.println("I am still working at this point");
                try {
                		System.out.println("Can I read the object?");
                    obj = ois.readObject();
                		System.out.println("I might have read the object");
                		System.out.println(obj.getClass());
                } catch (Exception ex) {
                		System.out.println("exception was caught");
                    ex.printStackTrace();
                }

                if(obj.getClass().isAssignableFrom(Chunk.class)) {
                    System.out.println("Received Chunk!");
                    Chunk chunk = (Chunk) obj;
                    Peer.ReceiveChunk(chunk);
                } else if (obj.getClass().isAssignableFrom(HashMap.class)) {
                		System.out.println("Received FileMap");
                    Map<String, Map<String, BitSet>> bitSetMap = (Map<String, Map<String, BitSet>>) obj;
                    System.out.println(bitSetMap);
                } else {
                		System.out.println("That's a cold ass honky");
                    throw new Exception("Received object type is not recognized");
                }
            }
        } finally {
        		System.out.println("Did the socket close?");
            serverSocket.close();
        }
    }
}
