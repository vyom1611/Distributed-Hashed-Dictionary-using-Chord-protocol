import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws RemoteException {
        if (args.length < 1) {
            System.out.println("Usage: java Client <nodeURL>");
            return;
        }

        Node node = null;
        try {
            String nodeURL = "rmi://localhost/" + args[0];
            node = (Node) Naming.lookup(nodeURL);
            System.out.println("Connected to node at " + nodeURL);
        } catch (Exception e) {
            System.err.println("Client error: Unable to connect to the node at " + args[0]);
            e.printStackTrace();
            return; // Exit if cannot connect to the RMI server
        }

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println("Enter 1 to lookup, 2 to insert, or 3 to exit:");
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume the newline

                switch (choice) {
                    case 1:
                        System.out.println("Enter a word:");
                        String word = scanner.nextLine();
                        String definition = node.lookup(word);
                        System.out.println("Definition: " + definition);
                        break;
                    case 2:
                        System.out.println("Enter a word:");
                        word = scanner.nextLine();
                        System.out.println("Enter the definition:");
                        definition = scanner.nextLine();
                        node.insert(word, definition);
                        System.out.println("Inserted.");
                        break;
                    case 3:
                        System.out.println("Exiting...");
                        return;
                    default:
                        System.out.println("Invalid choice.");
                        break;
                }
            }
        }
    }
}
