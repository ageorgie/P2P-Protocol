package ece454p1;

import java.io.Serializable;
import java.net.Socket;
import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: omidmortazavi
 * Date: 2013-06-04
 * Time: 7:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class StateBroadcaster implements Callable<Integer> {

    public Integer call() throws InterruptedException {
        while(true) {
            Thread.sleep(1000);
            Sender.enqueue((Serializable) Peer.getPeers().getPeerFileMap());
        }
    }
}
