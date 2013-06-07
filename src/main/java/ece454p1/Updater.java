package ece454p1;

import java.io.*;
import java.net.Socket;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: omidmortazavi
 * Date: 2013-06-06
 * Time: 6:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class Updater implements Callable<Integer> {

    Socket socket;

    public Updater(Socket socket) {
        this.socket = socket;
    }


    public static void receiveChunk(Chunk chunk) throws IOException {
        System.out.printf("Updater: called receivechunk file: %s, chunk: %s\n", chunk.getFileName(), chunk.getChunkNum());
        String fileName = chunk.getFileName();
        File file;
        Map<String, File> fileMap = Peer.getFileMap();
        System.out.println("1");
        if(!fileMap.containsKey(fileName)) {
            String[] split = Peer.getHostAndPort().split(" ");
            file = new File(String.format("%s/ECE454_Downloads/%s-%s/%s", System.getProperty("user.home"), split[0], split[1], fileName));
            System.out.println("2");
            if(file.exists()){
                file.delete();
                file.createNewFile();
            }
            file.setReadable(true);
            file.setWritable(true);
            System.out.println("3");
            fileMap.put(fileName, file);
        } else {
            file = fileMap.get(fileName);
        }
        System.out.println("4");
        int byteOffset = chunk.getChunkNum()*Config.CHUNK_SIZE;
        System.out.println("5");

        System.out.println(file.toString());
//        try {
        RandomAccessFile raf = new RandomAccessFile(file, "w");
        System.out.println("6");
        try {
            raf.seek(byteOffset);
            System.out.println("7");
            raf.write(chunk.getByteArray());
            System.out.println("Before updatePeerFileMap");
            Peer.getPeers().updatePeerFileMap(chunk);
        } catch (Exception e){
            System.out.println("Error while writing to file");
        } finally {
            raf.close();
        }
//
//        } catch (IOException ex) {
//            System.out.println("Caught ioexception");
//            throw ex;
//        }
    }


    public Integer call() throws Exception {
        Object obj = new Object();
        InputStream is = socket.getInputStream();
        System.out.printf("Updater: Accepted incoming connection from %s\n", socket.getInetAddress().getHostName());
        String senderHostName = socket.getInetAddress().getHostName().toLowerCase();
//        System.out.printf("senderhostname: %s\n", senderHostName);
//        System.out.printf("host to port map: %s\n", Peer.getPeers().hostToPortMap);
        int senderPort = Peer.getPeers().getPort(senderHostName);
//        System.out.printf("senderport: %d\n", senderPort);
        Peer.getPeers().setConnectionState(String.format("%s %d", senderHostName, senderPort), true);
        ObjectInputStream ois = new ObjectInputStream(is);

        try {
            obj = ois.readObject();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if(obj.getClass().isAssignableFrom(Chunk.class)) {
            Chunk chunk = (Chunk) obj;
            System.out.printf("Updater: Received file: %s, chunk: %s\n", chunk.getFileName(), chunk.getChunkNum());
            receiveChunk(chunk);
        } else if (obj.getClass().isAssignableFrom(HashMap.class)) {
            Map<String, Map<String, BitSet>> bitSetMap = (Map<String, Map<String, BitSet>>) obj;
//            System.out.printf("Updater: Received bitsetmap: %s\n", bitSetMap);
            Peer.getPeers().updatePeerFileMap(bitSetMap);
            System.out.printf("Updater: My own peerfilemap after update: %s\n", Peer.getPeers().getPeerFileMap());
        } else {
//            throw new Exception("Updater: Received object type is not recognized");
        }
        System.out.println("way down here");

        return 1;
    }
}
