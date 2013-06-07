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

    static Map<String, PriorityQueue<String>> priorityQueueMap;


    public Sender() throws IOException, InterruptedException {
        priorityQueueMap = new HashMap<String, PriorityQueue<String>>();
        for(String address: Peer.getPeers().getOtherPeerAddresses()) {
            priorityQueueMap.put(address, new PriorityQueue<String>());
        }
        insertPeerFileMapIntoPriorityQueue();
    }


    public static void insertPeerFileMapIntoPriorityQueue() {
        for(PriorityQueue<String> priorityQueue:priorityQueueMap.values()) {
            priorityQueue.offer(String.format("%s_!!PeerFileMap!!", 0));
        }
    }

    public static void insertChunkIntoPriorityQueue(String destinationAddress, String fileName, Integer chunkNum, Integer priority, Integer maxChunk) {
//        System.out.printf("insertChunkIntoPriorityQueue\n");
        if(!priorityQueueMap.containsKey(destinationAddress)) {
            priorityQueueMap.put(destinationAddress, new PriorityQueue<String>(100, new StringComparator()));
        }
        StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0; i < maxChunk.toString().length() - chunkNum.toString().length(); i++) {
            stringBuilder.append("0");
        }
        stringBuilder.append(chunkNum);
        String chunkIdentifier = String.format("%s_%s_%s_%s", priority, fileName, stringBuilder.toString(), destinationAddress);
//        System.out.printf("inserting chunk %s\n", chunkIdentifier);
        priorityQueueMap.get(destinationAddress).offer(chunkIdentifier);
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

    public static void send(String host, int port, Serializable object) throws IOException {
        Socket socket;
        try {
//            System.out.printf("Send called for host:%s, port %d\n", host, port);
            socket = new Socket(host, port);
            OutputStream os = socket.getOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(os);
            oos.writeObject(object);
            oos.close();
            os.close();
            socket.close();
//            System.out.printf("Sender: Object successfully transferred\n", host, port);
        } catch(ConnectException e) {
            System.out.printf("Sender: Connection refused for %s : %d ... retrying\n", host, port);
        }

    }

    public static void sendPeerFileMap() throws IOException {
        System.out.printf("Sending my current peerFileMap: %s\n", Peer.getPeers().getPeerFileMap());
        List<String> addresses = Peer.getPeers().getOtherPeerAddresses();
        for(String address: addresses) {
            String[] split = address.split(" ");
            Serializable peerFileMap = (Serializable) Peer.getPeers().getPeerFileMap();
            Sender.send(split[0], Integer.parseInt(split[1]), peerFileMap);
        }
    }

    public Integer call() throws Exception {
       while(true) {
           for(Map.Entry<String, PriorityQueue<String>> entry: priorityQueueMap.entrySet()) {
               String peerAddress = entry.getKey();
               PriorityQueue<String> priorityQueue = entry.getValue();
               if(!priorityQueue.isEmpty()) {
                   String poll = priorityQueue.poll();
//                   System.out.printf("Poll: %s\n", poll);
                   String[] pollSplit = poll.split("_");
                   if(pollSplit[1].equals("!!PeerFileMap!!")) {
                       sendPeerFileMap();
                   } else {
                       String fileName = pollSplit[1];
                       int chunkNum = Integer.parseInt(pollSplit[2]);
                       String destination = pollSplit[3];
                       if(destination == null) {
                           throw new Exception(String.format("Socket for destination address %s does not exist", destination));
                       }
                       String[] split = destination.split(" ");
                       Chunk chunk = new Chunk(fileName, chunkNum);
                       System.out.printf("Sending file: %s, chunk %d", fileName, chunkNum);
                       send(split[0], Integer.parseInt(split[1]), chunk);
                   }
               }
           }
        }
    }
}
