import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;

public class DictionaryLoader {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java DictionaryLoader <nodeURL> <dictionaryFile>");
            return;
        }

        try {
            String nodeURL = args[0];
            String dictionaryFile = args[1];
            Node initialNode = (Node) Naming.lookup(nodeURL);

            loadDictionary(initialNode, dictionaryFile);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void loadDictionary(Node initialNode, String dictionaryFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(dictionaryFile));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(":");
            if (parts.length < 2) continue;  // Skip malformed lines
            String word = parts[0].trim();
            String definition = parts[1].trim();

            try {
                int keyHash = FNV1aHash.hash32(word);  // Assuming FNV1aHash is accessible
                Node responsibleNode = initialNode.findSuccessor(keyHash);
                responsibleNode.insert(word, definition);
                System.out.println("Inserted '" + word + "' at node " + responsibleNode.getURL());
            } catch (RemoteException e) {
                System.err.println("Failed to insert '" + word + "': " + e.getMessage());
            }
        }
        reader.close();
    }
}
