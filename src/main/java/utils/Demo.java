package utils;

import broker.Constants;

/**
 * @author Alberto Delgado on 4/21/22
 * @project dsd-pub-sub
 */
public class Demo {
    public static boolean isDemo = false;
    private static boolean printReplication = false;
    private static boolean printMembershipTable = false;
    private static boolean printHeartbeat = false;
    private static final int WAITING_TIME_MS = 5000;
    private static final String DEMO_FLAG = "--demo";
    private static final String MEMBERSHIP_TABLE_FLAG = "--membership-table";
    private static final String HEARTBEAT_FLAG = "--heartbeat";
    private static final String REPLICATION_FLAG = "--replication";


    /**
     * parse args -> check for demo flags
     *
     * @param args
     */
    public static void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(DEMO_FLAG)) {
                isDemo = true;
                Constants.BROKER_DATASTORE_CACHE_CAPACITY = 5; // make it easier to watch persistence
                System.out.println("[DEMO] Decreasing broker store cache capacity to " + Constants.BROKER_DATASTORE_CACHE_CAPACITY);
            }
            if (args[i].equals(MEMBERSHIP_TABLE_FLAG))
                printMembershipTable = true;
            if (args[i].equals(HEARTBEAT_FLAG))
                printHeartbeat = true;
            if (args[i].equals(REPLICATION_FLAG))
                printReplication = true;
        }
    }

    /**
     * Prints membership table if flag on args
     *
     * @param msg
     */
    public static void printMembershipTable(String msg) {
        if (printMembershipTable) System.out.println(msg);
    }

    /**
     * Prints replication details if flag on args
     *
     * @param msg
     */
    public static void printReplication(String msg) {
        if (printReplication) System.out.println(msg);
    }

    /**
     * Prints heartbeats if flag on args
     *
     * @param msg
     */
    public static void printHeartbeat(String msg) {
        if (printHeartbeat) System.out.println(msg);
    }

    /**
     * Just waiting por WAITING_TIME_MS for action to execute
     * during demo
     */
    public static void printAndDelay() {
        if (!isDemo) return;

        try {
            Thread.sleep(WAITING_TIME_MS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Just waiting for WAITING_TIME_MS for action to execute
     * during demo
     *
     * @param msg
     */
    public static void printAndDelay(String msg) {
        if (isDemo) System.out.println(msg);
        printAndDelay();
    }
}
