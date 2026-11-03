import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import com.zeroc.Ice.Current;
import Demo.CallbackPrx;
import Demo.BusMsg;
import java.util.Random;

public class ChatManagerImp implements Demo.ChatManager {

    private List<String> messages;
    private final List<CallbackPrx> subscribers = new CopyOnWriteArrayList<>();
    private final String teamFolder = "./team4";

    // for part 2
    private final AtomicInteger processedMessages = new AtomicInteger(0);
    private final AtomicInteger dispatchedNotifications = new AtomicInteger(0);
    private long startTime = 0;

    ChatManagerImp() {
        messages = new ArrayList<>();
    }

    @Override
    public void subscribe(CallbackPrx callback, Current current) {
        if (callback != null && !subscribers.contains(callback)) {
            subscribers.add(callback);
            System.out.println("New bus subscribed. Total subscribers: " + subscribers.size());
        }
    }

    @Override
    public String[] getState(Current current) {
        System.out.println("GetState");
        String[] state = new String[messages.size()];

        for (int i = 0; i < state.length; i++) {
            state[i] = messages.get(i);
        }
        return state;
    }

    @Override
    public void sendMessage(BusMsg msg, Current current) {
        // check dir exists
        File busDir = new File(teamFolder + "/" + msg.busId);
        if (!busDir.exists()) {
            busDir.mkdirs();
        }

        int count = processedMessages.incrementAndGet();

        // persist image payload
        String safeTimestamp = msg.timestamp.replaceAll("[: ]", "_");
        String filename = busDir.getPath() + "/" + safeTimestamp + "_" + count + ".jpg";

        try (FileOutputStream fos = new FileOutputStream(filename)) {
            fos.write(msg.image);
        } catch (IOException e) {
            System.err.println("Failed to write image for bus " + msg.busId + ": " + e.getMessage());
        }
        if (count == 1) {
            startTime = System.currentTimeMillis();
        }

        // routing based on textTag
        if ("GENERALINTEREST".equalsIgnoreCase(msg.textTag)) {
            String notification = "GENERALINTEREST from " + msg.busId;
            broadcastNotification(notification);
        }

        // print throughput every 50 msg
        if (count % 50 == 0) {
            printThroughput();
        }

        // Auto-print final summary when completing 300/client (900) or 600/client (1800)
        if (count == 900 || count == 1800) {
            printFinalSummary();
        }
    }

    private void broadcastNotification(String notification) {
        for (CallbackPrx subscriber : subscribers) {
            try {
                subscriber.notifyCallback(notification);
                dispatchedNotifications.incrementAndGet();
            } catch (Exception e) {
                // If a client disconnected, remove them to keep list healthy
                subscribers.remove(subscriber);
                System.out.println("Removed unreachable subscriber.");
            }
        }
    }

    public void printThroughput() {
        double elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0;
        if (elapsedSec > 0) {
            int totalMsgs = processedMessages.get();
            int totalNotifs = dispatchedNotifications.get();
            double msgThroughput = totalMsgs / elapsedSec;
            double notifThroughput = totalNotifs / elapsedSec;

            System.out.printf("Throughput [%.1fs] -> Ingestion: %.2f msg/sec | Notifications: %.2f msg/sec%n",
                    elapsedSec, msgThroughput, notifThroughput);
        }
    }

    public void printFinalSummary() {
        double elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0;
        int totalMsgs = processedMessages.get();
        int totalNotifs = dispatchedNotifications.get();
        double msgThroughput = totalMsgs / elapsedSec;
        double notifThroughput = totalNotifs / elapsedSec;

        System.out.println("\n=================== BENCHMARK COMPLETE ===================");
        System.out.printf("Total Elapsed Time:         %.2f s%n", elapsedSec);
        System.out.printf("Total Messages Processed:   %d%n", totalMsgs);
        System.out.printf("Message Throughput:         %.2f msg/sec%n", msgRate(msgThroughput));
        System.out.printf("Total Notifications Sent:   %d%n", totalNotifs);
        System.out.printf("Notification Throughput:    %.2f notif/sec%n", notifThroughput);
        System.out.println("==========================================================\n");
    }

    private double msgRate(double rate) {
        return rate;
    }

}
