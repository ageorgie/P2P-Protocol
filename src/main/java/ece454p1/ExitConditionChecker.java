package ece454p1;

import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: omidmortazavi
 * Date: 2013-06-07
 * Time: 3:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExitConditionChecker implements Callable<Integer> {

    public Integer call() throws InterruptedException {
        while(true) {
            Thread.sleep(5000);
            if(Peer.getPeers().allowedToLeave()) {
                System.exit(1);
            }
        }

    }
}
