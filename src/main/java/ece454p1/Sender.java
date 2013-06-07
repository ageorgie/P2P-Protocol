package ece454p1;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created with IntelliJ IDEA.
 * User: omidmortazavi
 * Date: 2013-05-26
 * Time: 5:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class Sender implements Callable<Integer> {

    static ConcurrentHashMap<String, PriorityBlockingQueue<String>> priorityQueueMap;
    static AtomicBoolean broadcast;

    public Sender() throws IOException, InterruptedException {
        priorityQueueMap = new ConcurrentHashMap<String, PriorityBlockingQueue<String>>();
        broadcast = new AtomicBoolean(false);
        for(String address: Peer.getPeers().getOtherPeerAddresses()) {
            priorityQueueMap.putIfAbsent(address, new PriorityBlockingQueue<String>());
        }
        sendPeerFileMap();
    }

    public static synchronized void setBroadcast(Boolean broadcast) {
        Sender.broadcast.getAndSet(broadcast);
    }
//    public static void insertPeerFileMapIntoPriorityQueue() {
//        for(Map.Entry<String, PriorityQueue<String>> entry:priorityQueueMap.entrySet()) {
//            String peerAddress = entry.getKey();
//            if(Peer.getPeers().isConnected(peerAddress)) {
//                PriorityQueue priorityQueue = entry.getValue();
//                System.out.printf("Sender: Inserting peerfileMap into priority queue for %s. Contents:", entry.getKey());
//                priorityQueue.offer(String.format("%s_!!PeerFileMap!!", 0));
//                Iterator i = priorityQueue.iterator();
//                while(i.hasNext()) {
//                    System.out.printf("%s,", i.next());
//                }
//                System.out.printf("\n");
//            }
//        }
//    }

    public static void insertChunkIntoPriorityQueue(String destinationAddress, String fileName, Integer chunkNum, Integer priority, Integer maxChunk) {
//        System.out.printf("insertChunkIntoPriorityQueue\n");
        if(!priorityQueueMap.containsKey(destinationAddress)) {
            priorityQueueMap.put(destinationAddress, new PriorityBlockingQueue<String>(100, new StringComparator()));
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
        for(PriorityBlockingQueue priorityQueue:priorityQueueMap.values()) {
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
        String peerAddress = String.format("%s %s", host, port);
        if(Peer.getPeers().isConnected(peerAddress)) {
//            System.out.printf("Send called for peeraddress: %s\n", peerAddress);
            try {
                System.out.printf("Sender: Send called for host:%s, port %d\n", host, port);
                socket = new Socket(host, port);
//                System.out.printf("Sender: Socket Opened\n");
                OutputStream os = socket.getOutputStream();

                ObjectOutputStream oos = new ObjectOutputStream(os);
                oos.writeObject(object);
//                System.out.printf("Sender: wrote object\n");
                oos.close();
                os.close();
                socket.close();
                System.out.printf("Sender: Object successfully transferred to %s:%d\n", host, port);
            } catch(ConnectException e) {
                System.out.printf("Sender: Connection refused for %s : %d ... retrying\n", host, port);
                Peer.getPeers().setConnectionState(String.format("%s %s", host, port), false);
            }
        }

    }

    public static void sendPeerFileMap() throws IOException {
        System.out.printf("Sending my current peerFileMap: %s\n", Peer.getPeers().getPeerFileMap());
        List<String> addresses = Peer.getPeers().getOtherPeerAddresses();
        for(String address: addresses) {
            String[] split = address.split(" ");
            Serializable peerFileMap = (Serializable) Peer.getPeers().getPeerFileMap();
            send(split[0], Integer.parseInt(split[1]), peerFileMap);
        }
    }

    public Integer call() throws Exception {
       while(true) {
           if(broadcast.get()){
               sendPeerFileMap();
               setBroadcast(false);
           } else {
               for(Map.Entry<String, PriorityBlockingQueue<String>> entry: priorityQueueMap.entrySet()) {
                   String peerAddress = entry.getKey();
                   PriorityBlockingQueue<String> priorityQueue = entry.getValue();
                   boolean isConnected = Peer.getPeers().isConnected(peerAddress);
                   boolean pqEmpty = priorityQueue.isEmpty();
                   if(isConnected && !pqEmpty ) {
                       System.out.printf("Sender: peeraddress: %s, isConnected: %s, priority queue empty: %s \n" ,peerAddress, isConnected, pqEmpty);
                       String poll = priorityQueue.poll();
                       System.out.printf("Sender: Poll for %s: %s\n", peerAddress, poll);
                       if(poll == null){
                           continue;
                       }
                       String[] pollSplit = poll.split("_");
                       String fileName = pollSplit[1];
                       int chunkNum = Integer.parseInt(pollSplit[2]);
                       String destination = pollSplit[3];
                       if(destination == null) {
                           throw new Exception(String.format("Socket for destination address %s does not exist", destination));
                       }
                       String[] split = destination.split(" ");
                       Chunk chunk = new Chunk(fileName, chunkNum);
                       System.out.printf("Sender: Sending file: %s, chunk %d\n", fileName, chunkNum);
                       send(split[0], Integer.parseInt(split[1]), chunk);
                   }
               }
           }
        }
    }
}
