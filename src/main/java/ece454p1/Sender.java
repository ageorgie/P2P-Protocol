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


    public Sender() throws IOException, InterruptedException {
        List<String> peerAddresses = Peer.getPeers().getPeerAddresses();
        sockets = new HashMap<String, Socket>();
        insertPeerFileMapIntoPriorityQueue();
        int numTrials = 0;
        while(!peerAddresses.isEmpty() && numTrials<5) {
            Iterator<String> i = peerAddresses.iterator();
            while(i.hasNext()) {
                String peerAddress = i.next();
                System.out.printf("Sender: Peeraddress: %s\n", peerAddress);
                String[] split = peerAddress.split(" ");
                String host = split[0];
                int port = Integer.parseInt(split[1]);
                if(host.toLowerCase().equals(Peer.host.toLowerCase()) && port==Peer.port) {
                    System.out.println("Sender: Tried to connect to self. Skipping address.");
                    i.remove();
                    continue;
                }
                try {
                    sockets.put(peerAddress, new Socket(host, port));
                    System.out.printf("Sender: Connection accepted for %s: %d - Ready for transfer\n", host, port);
                    i.remove();
                } catch (ConnectException e) {
                    System.out.printf("Sender: Connection refused for %s : %d ... retrying\n", host, port);
                    numTrials++;
                    Thread.sleep(5000);
                }
            }
            numTrials++;
        }
    }
    public static void insertPeerFileMapIntoPriorityQueue() {
        System.out.println("inserting priority map into queue");
        for(PriorityQueue<String> priorityQueue:priorityQueueMap.values()) {
            priorityQueue.offer(String.format("%s_!!PeerFileMap!!", 0));
        }
    }

    public static void insertChunkIntoPriorityQueue(String peerAddress, String fileName, int chunkNum, String destinationAddress, Integer priority) {

        if(!priorityQueueMap.containsKey(peerAddress)) {
            priorityQueueMap.put(peerAddress, new PriorityQueue<String>(100, new StringComparator()));
        }
        String chunkIdentifier = String.format("%s_%s_%s_%s", priority, fileName, chunkNum, destinationAddress);
        System.out.printf("inserting chunk %s\n", chunkIdentifier);
        priorityQueueMap.get(peerAddress).offer(chunkIdentifier);
    }

    public static void emptyPriorityQueues() {
        for(PriorityQueue priorityQueue:priorityQueueMap.values()) {
            priorityQueue.clear();
        }
    }

    public static class StringComparator implements Comparator<String> {
        public int compare(String s1, String s2) {
            return s1.compareTo(s2);
        }
    }

    public static void send(Socket socket, Serializable object) throws IOException {
        OutputStream os = socket.getOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(object);
        oos.close();
        os.close();
    }

    public Integer call() throws Exception {
       try {
           while(true) {
//                Object obj = sendQueue.take();
//                if(obj.getClass().isAssignableFrom(Chunk.class)) {
//                    Chunk chunk = (Chunk) obj;
//                    send(sockets.get(chunk.getDestination()), chunk);
//                } else if (obj.getClass().isAssignableFrom(HashMap.class)) {
//                    Map<String, Map<String, BitSet>> bitSetMap = (Map<String, Map<String, BitSet>>) obj;
//                    for(Socket socket:sockets.values()) {
//                        send(socket, (Serializable) bitSetMap);
//                    }
//                } else {
//                    throw new Exception("Received object type is not recognized");
//                }
               for(Map.Entry<String, PriorityQueue<String>> entry: priorityQueueMap.entrySet()) {
                   String peerAddress = entry.getKey();
                   PriorityQueue<String> priorityQueue = entry.getValue();
                   if(!priorityQueue.isEmpty()) {
                       String[] pollSplit = priorityQueue.poll().split("_");
                       if(pollSplit[1]=="!!PeerFileMap!!") {
                           for(Socket socket:sockets.values()) {
                               System.out.println("sending priority map");
                               send(socket, (Serializable) Peer.getPeers().getPeerFileMap());
                           }
                       } else {
                           String fileName = pollSplit[1];
                           int chunkNum = Integer.parseInt(pollSplit[2]);
                           String destination = pollSplit[3];
                           System.out.printf("sending chunk %s\n", pollSplit);
                           Socket destinationSocket = sockets.get(destination);
                           if(destination == null) {
                               throw new Exception(String.format("Socket for destination address %s does not exist", destination));
                           }
                           Chunk chunk = new Chunk(fileName, chunkNum);
                           send(destinationSocket, chunk);
                       }
                   }
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
