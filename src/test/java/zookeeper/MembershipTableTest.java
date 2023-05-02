package zookeeper;

import com.google.gson.Gson;
import models.Node;

/**
 * @author Alberto Delgado on 4/12/22
 * @project dsd-pub-sub
 */
public class MembershipTableTest {
    private static final Gson gson = new Gson();
    private static final int ID = 0;
    private static final int ZK_PORT = 5001;
    private static final int BROKER_PORT = 5000;
    private static final String HOSTNAME = "localhost";
    private static final Node node = new Node(ID, HOSTNAME, ZK_PORT, BROKER_PORT);
}