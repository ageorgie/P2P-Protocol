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
//        int numTrials = 0;
//        while(!peerAddresses.isEmpty() && numTrials<5) {
//            Iterator<String> i = peerAddresses.iterator();
//            while(i.hasNext()) {
//                String peerAddress = i.next();
//                System.out.printf("Sender: Peeraddress: %s\n", peerAddress);
//                String[] split = peerAddress.split(" ");
//                String host = split[0];
//                int port = Integer.parseInt(split[1]);
//                if(host.toLowerCase().equals(Peer.host.toLowerCase()) && port==Peer.port) {
//                    System.out.println("Sender: Tried to connect to self. Skipping address.");
//                    i.remove();
//                    continue;
//                }
//                try {
//                    sockets.put(peerAddress, new Socket(host, port));
//                    System.out.printf("Sender: Connection accepted for %s: %d - Ready for transfer\n", host, port);
//                    i.remove();
//                } catch (ConnectException e) {
//                    System.out.printf("Sender: Connection refused for %s : %d ... retrying\n", host, port);
//                    numTrials++;
//                    Thread.sleep(5000);
//                }
//            }
//            numTrials++;
//        }
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

    public static void send(String host, int port, Serializable object) throws IOException {
        Socket socket;
        try {
            System.out.printf("Send called for host:%s, port %d\n", host, port);
            socket = new Socket(host, port);
            System.out.printf("Sender: Connection accepted for %s: %d - Ready for transfer\n", host, port);
        } catch(ConnectException e) {
            System.out.printf("Sender: Connection refused for %s : %d ... retrying\n", host, port);
            throw e;
        }
        OutputStream os = socket.getOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(object);
        oos.close();
        os.close();
        socket.close();
    }

    public static void sendPeerFileMap() throws IOException {
        System.out.println("SendPeerFileMap called");
        List<String> addresses = Peer.getPeers().getOtherPeerAddresses();
        System.out.printf("other addresses: %s\n", addresses);
        for(String address: addresses) {
            String[] split = address.split(" ");
            System.out.printf("address: %s, %s\n", split[0], split[1]);
            Serializable peerFileMap = (Serializable) Peer.getPeers().getPeerFileMap();
            System.out.printf("peerfilemap: %s\n", peerFileMap);
            Sender.send(split[0], Integer.parseInt(split[1]), peerFileMap);
        }
        System.out.println("Exiting sendPeerFileMap");
    }

    public Integer call() throws Exception {

       while(true) {
           for(Map.Entry<String, PriorityQueue<String>> entry: priorityQueueMap.entrySet()) {
               String peerAddress = entry.getKey();
               System.out.printf("In sender for loop: peeraddress: %s\n", peerAddress);
               PriorityQueue<String> priorityQueue = entry.getValue();
               if(!priorityQueue.isEmpty()) {
                   String poll = priorityQueue.poll();
                   System.out.printf("Poll: %s\n", poll);
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
                       System.out.printf("sending chunk %s\n", pollSplit);
                       send(split[0], Integer.parseInt(split[1]), chunk);
                   }
               }
           }
        }
    }
}
