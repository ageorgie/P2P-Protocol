package ece454p1;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Peer and Status are the classes we really care about Peers is a container;
 * feel free to do a different container
 */
public class Peer {
    static Map<String, File> fileMap;
    static Peers peers;
    static String host;
    static int port;
    static ExecutorService executorService = Executors.newFixedThreadPool(15);
    static Future senderFuture;
    static Future receiverFuture;


    public static String getHost() {
        return host;
    }

    private Peer() throws IOException, InterruptedException {
        InputStream in = getClass().getClassLoader().getResourceAsStream("addresses.txt");
        host = InetAddress.getLocalHost().getHostName().toLowerCase();
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line;
        Map<String, Map<String, BitSet>> peerFileMap = new HashMap<String, Map<String, BitSet>>();
        peers = new Peers();
        while ((line = br.readLine()) != null) {
            line = line.toLowerCase();
            String[] split = line.split(" ");
            if(!split[0].toLowerCase().equals(host)) {
                peers.hostToPortMap.put(split[0].toLowerCase(), Integer.parseInt(split[1]));
                peers.setConnectionState(line.toLowerCase(), true);
            }
            if(host.toLowerCase().equals(split[0].toLowerCase())) {
                port = Integer.parseInt(split[1]);
            }
            peerFileMap.put(line.toLowerCase(), new HashMap<String, BitSet>());
        }
        br.close();
        peers.setPeerFileMap(peerFileMap);
        System.err.printf("Peer created. Host: %s Port: %d", host, port);
        fileMap = new HashMap<String, File>();
        String basePath =String.format("%s/ECE454_Downloads/", System.getProperty("user.home"));
        String path = String.format("%s/%s-%d/", basePath, host, port);
        System.err.println(path);

        File baseDir = new File(basePath);
        if (!baseDir.exists()) {
            boolean mkdirOut = baseDir.mkdir();
            System.err.printf("mkdir returned %s\n", mkdirOut);
        }
        baseDir.setReadable(true,true);
        baseDir.setWritable(true, true);
        File theDir = new File(path);
        if (!theDir.exists()) {
            boolean mkdirOut = theDir.mkdir();
            System.err.printf("mkdir returned %s\n", mkdirOut);
        }
        theDir.setReadable(true, true);
        theDir.setWritable(true,true);
    }


    public static String getHostAndPort() {
        return host.toLowerCase() + " " + port;
    }

    public static Peers getPeers() {
        return peers;
    }



	public static int insert(String filePath) {

        File file = new File(filePath);
        if(!file.isFile() || !file.canRead()) {
            return -1;
        }
        int numChunks = (int) Math.ceil((file.length() * 1.0000)/(Config.CHUNK_SIZE * 1.0000));
        String[] splitPath = filePath.split("/");
        String fileName = splitPath[splitPath.length - 1];
        peers.insertNewFile(fileName, numChunks);
        fileMap.put(fileName, file);
        return 0;
    };

	public static int query(Status status){
        status = new Status();
        System.out.printf("status: %s", status.toString());
        System.out.printf("Weighted Least Replication: %s", status.weightedLeastReplication);
        System.out.printf("Least Replication: %s", status.leastReplication);
        System.out.printf("Fraction of local files: %s", status.local);
        System.out.printf("Fraction of system files: %s", status.system);
        System.out.printf("Number of Files: %d", status.numberOfFiles());
        return -1;
    };


	/*
	 * Note that we should have the peer list, so it is not needed as a
	 * parameter
	 */
	public static int join() throws IOException, InterruptedException {

        receiverFuture = executorService.submit(new Receiver(port));
        senderFuture = executorService.submit(new Sender());
        executorService.submit(new StateBroadcaster());

        return 1;
    };

	public static int leave(){
        executorService.submit(new ExitConditionChecker());
        return -1;
    };

//
//	private enum State {
//		connected, disconnected, unknown
//	};
//
//	private State currentState;

    public static void main(String[] args) throws IOException, InterruptedException {
        new Peer();
        while(true) {
            System.out.print(">>");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            try {
                executeCommand(br.readLine());
            } catch (IOException ioe) {
                System.out.println("Error while buffering input");
            }
        }
    }

    public static Map<String, File> getFileMap() {
        return fileMap;
    }

    public static void executeCommand(String command) throws IOException, InterruptedException {
        if(command.isEmpty()) {
            return;
        }
        String[] split = command.split(" ");
        Command commandEnum = Command.valueOf(split[0]);
        switch (commandEnum) {
            case join:
                Peer.join();
                break;
            case leave:
                Peer.leave();
                break;
            case insert:
                Peer.insert(split[1]);
                break;
            case query:
                Peer.query(null);
                break;
            case refresh_sender:
                senderFuture.cancel(true);
                senderFuture = executorService.submit(new Sender());
            case refresh_receiver:
                receiverFuture.cancel(true);
                receiverFuture = executorService.submit(new Receiver(port));
            default:
                break;
        }

    }


     public enum Command {
         join,
         leave,
         insert,
         query,
         refresh_sender,
         refresh_receiver
     }
}
