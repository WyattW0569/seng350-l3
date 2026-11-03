import java.io.FilenameFilter;
import java.net.Inet4Address;
import java.nio.file.Files;
import java.util.Arrays;
import java.io.File;

import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;

import Demo.CallbackPrx;
import Demo.ChatManagerPrx;

public class Client {
    public static void main(String[] args) {
        try (com.zeroc.Ice.Communicator communicator = com.zeroc.Ice.Util.initialize(args, "client.cfg")) {

            Demo.ChatManagerPrx chatManagerPrx = Demo.ChatManagerPrx
                    .checkedCast(communicator.propertyToProxy("ChatManager.Proxy"));
            if (args.length < 4) {
                System.err.println("Usage: java Client <busID> <maxImages> <frequencyMs> <generalInterestPercentage>");
                System.exit(1);
            }
            try {
                //Directory name / Bus ID
                int busID = Integer.parseInt(args[0]);
                // Maximum number of images to send
                int maxImages = Integer.parseInt(args[1]);
                // Frequency for sending images in milliseconds
                int frequencyMs = Integer.parseInt(args[2]);
                // Percentage of images tagged with general interest
                int generalInterestPercentage = Integer.parseInt(args[3]);


                CallbackImp callbackImp = new CallbackImp(chatManagerPrx);
                ObjectAdapter adapter = communicator.createObjectAdapter("Callback");
                ObjectPrx objectPrx = adapter.add(callbackImp, Util.stringToIdentity("Callback"));
                adapter.activate();

                CallbackPrx prx = CallbackPrx.uncheckedCast(objectPrx);
                chatManagerPrx.subscribe(prx);

                String hostname = Inet4Address.getLocalHost().getHostName();
                System.out.println("hostname: " + hostname);

                String directoryPath = "./" + Integer.toString(busID) + "/";
                File directory = new File(directoryPath);
                if (!directory.exists() || !directory.isDirectory()) {
                    System.err.println("Directory " + directoryPath + " does not exist or is not a directory");
                    return;
                }
                FilenameFilter jpgFilter = (directory1, name) -> name.toLowerCase().endsWith(".jpg");
                File[] imageFiles = directory.listFiles(jpgFilter);
                if (imageFiles == null || imageFiles.length == 0) {
                    System.err.println("No JPG images found in directory " + directoryPath);
                    return;
                }

                int limit = (maxImages == 0) ? imageFiles.length : Math.min(maxImages, imageFiles.length);

                for (int i = 0; i < limit; i++) {
                    File imageFile = imageFiles[i];
                    try {
                        byte[] imagePayload = Files.readAllBytes(imageFile.toPath());
                        // Message format to bus goes here

                        //now sleep before send next message
                        //avoid sleep after very last image
                        if (i < limit - 1) {
                            Thread.sleep(frequencyMs);
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to read or send image: " + imageFile.getName());
                        e.printStackTrace();
                    }
                }

                chatManagerPrx.sendMessage(hostname + ": Hello World");

                communicator.waitForShutdown();

            } catch (Exception e) {
                // saveMessage(dest,)
                e.printStackTrace();
            }
        }

    }
}
