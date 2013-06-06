package ece454p1;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
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

    public Integer call() throws Exception {
        System.out.println("New updater");
        Object obj = new Object();
        InputStream is = socket.getInputStream();
        ObjectInputStream ois = new ObjectInputStream(is);

        try {
            obj = ois.readObject();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if(obj.getClass().isAssignableFrom(Chunk.class)) {
            Chunk chunk = (Chunk) obj;
            Peer.ReceiveChunk(chunk);
        } else if (obj.getClass().isAssignableFrom(HashMap.class)) {
            Map<String, Map<String, BitSet>> bitSetMap = (Map<String, Map<String, BitSet>>) obj;
            System.out.printf("Received %s\n", bitSetMap);
            Peer.getPeers().updatePeerFileMap(bitSetMap);
            System.out.printf("My own peerfilemap after update: %s\n", Peer.getPeers().getPeerFileMap());
        } else {
            throw new Exception("Received object type is not recognized");
        }
        return 1;
    }
}
