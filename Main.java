import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Main {
    public static void main(String[] args) {
            if (args.length != 2) {
                System.out.println("Usage: java Main <nodeID> <port>");
                System.exit(1);
            }
            int id = Integer.parseInt(args[0]);
            int port = Integer.parseInt(args[1]);
            String url = "Node" + id;
            Registry registry = null;

            try {
                NodeImpl node = new NodeImpl(id, url);
                registry = LocateRegistry.getRegistry("localhost", 1099);
                registry.rebind(url, node);
                System.out.println("Node " + url + " is running at port " + port + "...");

                if (id != 0) {
                    Node node0 = (Node) registry.lookup("Node0");
                    node.join(node0);
                    System.out.println("Node " + url + " has joined the ring via Node0.");
                } else {
                    System.out.println("Node0 has started, initializing the Chord ring...");
                    node.join(null); // Node0 initializes the ring
                }

                // Start the DictionaryLoader to load the dictionary file
                String nodeURL = "rmi://localhost/" + url;
                String dictionaryFile = "sample-dictionary-file.txt";
                DictionaryLoader.main(new String[]{nodeURL, dictionaryFile});
                generateReport();

            } catch (Exception e) {
                System.err.println("Exception in Node " + id + ": " + e);
                e.printStackTrace();
            }
        }

    private static void generateReport() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            String[] boundNames = registry.list();  // Get all registered names in the registry
            String reportFileName = "ChordSystemReport.txt";
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(reportFileName))) {
                writer.write("Network Report:\n");
                for (String name : boundNames) {
                    try {
                        Node node = (Node) registry.lookup(name);
                        int dictionarySize = node.getDictionarySize();
                        String fingerTable = node.printFingerTable();
                        writer.write("Node " + name + " has " + dictionarySize + " words.\n");
                        writer.write("Finger Table for " + name + ":\n" + fingerTable + "\n\n");
                    } catch (RemoteException e) {
                        writer.write("Failed to connect to node " + name + ": " + e.getMessage() + "\n");
                    }
                }
                // Add the Chord ring's diagram
                writer.write("Chord Ring Diagram:\n");
                // Assuming a simple diagram. You may want to develop a method to create a more complex diagram.
                for (String name : boundNames) {
                    writer.write(name + " -> ");
                }
                writer.write(boundNames[0] + "\n"); // Completing the circle
            } catch (IOException e) {
                System.err.println("Error writing to the report file: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("Error generating report: " + e.getMessage());
            e.printStackTrace();
        }
    }
}