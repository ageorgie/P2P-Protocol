package ece454p1;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created with IntelliJ IDEA.
 * User: omidmortazavi
 * Date: 2013-05-26
 * Time: 5:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class Sender implements Callable<Integer> {

    Map<String, Socket> sockets;
    static Map<String, PriorityQueue<String>> priorityQueueMap;

    private static LinkedBlockingQueue<Serializable> sendQueue = new LinkedBlockingQueue<Serializable>();

    public Sender() throws IOException, InterruptedException {
        List<String> peerAddresses = Peer.getPeers().getPeerAddresses();
        sockets = new HashMap<String, Socket>();
        for(String peerAddress: peerAddresses) {
            String[] split = peerAddress.split(" ");
            String host = split[0];
            int port = Integer.parseInt(split[1]);
            Boolean connectionAccepted = false;
            while(!connectionAccepted) {
                try {
                    sockets.put(peerAddress, new Socket(host, port));
                    connectionAccepted = true;
                    System.out.printf("Connection accepted : Ready for transfer");
                } catch (ConnectException e) {
                    System.out.printf("Connection refused for %s : %d ... retrying\n", host, port);
                    Thread.sleep(5000);
                }
            }
        }
    }

    public static void send(Socket socket, Serializable object) throws IOException {
        OutputStream os = socket.getOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(object);
        oos.close();
        os.close();
    }

    public static void enqueue(Serializable object) {
        sendQueue.add(object);
    }

    public Integer call() throws Exception {
       try {
           while(true) {
                Object obj = sendQueue.take();
                if(obj.getClass().isAssignableFrom(Chunk.class)) {
                    Chunk chunk = (Chunk) obj;
                    send(sockets.get(chunk.getDestination()), chunk);
                } else if (obj.getClass().isAssignableFrom(HashMap.class)) {
                    Map<String, Map<String, BitSet>> bitSetMap = (Map<String, Map<String, BitSet>>) obj;
                    for(Socket socket:sockets.values()) {
                        send(socket, (Serializable) bitSetMap);
                    }
                } else {
                    throw new Exception("Received object type is not recognized");
                }
            }
       } catch(Exception e) {
           for(Socket socket:sockets.values()) {
               socket.close();
           }
           throw  e;
       }
    }
}
