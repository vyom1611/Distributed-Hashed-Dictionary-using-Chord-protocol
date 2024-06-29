import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NodeImpl extends UnicastRemoteObject implements Node, Serializable {
    private static final long serialVersionUID = 1L;
    private boolean joinLock;
    private static final int NUM_FINGERS = 31;
    private Node successor;
    private Node predecessor;
    private final ConcurrentHashMap<Integer, Node> fingerTable;
    private final ConcurrentHashMap<String, String> dictionary;
    private final int id;
    private final String url;
    private final NodeLogger nodeLogger;


    public NodeImpl(int id, String url) throws RemoteException {
        super();
        this.id = id;
        this.url = url;
        this.dictionary = new ConcurrentHashMap<>();
        this.fingerTable = new ConcurrentHashMap<>(NUM_FINGERS);
        this.predecessor = this;
        this.successor = this;
        this.nodeLogger = new NodeLogger(id);
        nodeLogger.logInfo("Node " + id + " initialized at URL " + url);


    }

    private void initFingerTable() throws RemoteException {
        for (int i = 0; i < NUM_FINGERS; i++) {
            int fingerStart = (id + (1 << i)) % NUM_FINGERS;
            Node fingerNode = findSuccessor(fingerStart);
            fingerTable.put(i, fingerNode);
        }
    }

    @Override
    public Node findPredecessor(int key) throws RemoteException {
        Node n = this;
        while (key < n.getID() || key > n.successor().getID()) {
            n = n.closestPrecedingFinger(key);
        }
        return n;
    }


    @Override
    public Node closestPrecedingFinger(int keyHash) throws RemoteException {
        for (int i = NUM_FINGERS - 1; i >= 0; i--) {
            Node f = fingerTable.get(i);
            if (f != null && f.getID() > this.getID() && f.getID() < keyHash) {
                nodeLogger.logInfo("Closest preceding finger found: Node " + f.getID() + " for key " + keyHash);
                return f;
            }
        }
        return this; // Fallback to this node if no closer node is found
    }

    @Override
    public Node successor() throws RemoteException {
        return this.successor != null ? this.successor : this; // return this if successor is null
    }

    @Override
    public Node predecessor() throws RemoteException {
        return this.predecessor;
    }

    @Override
    public boolean acquireJoinLock(String nodeURL) throws RemoteException {
        if (!joinLock) {
            joinLock = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean releaseJoinLock(String nodeURL) throws RemoteException {
        if (joinLock) {
            joinLock = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(String word) throws RemoteException {
        return this.dictionary.remove(word, this.dictionary.get(word));
    }

    @Override
    public void setDictionary(String word, String definition) throws RemoteException {
        dictionary.put(word, definition);
        nodeLogger.logInfo("Word '" + word + "' with definition '" + definition + "' added to the dictionary on Node " + id);
    }

    @Override
    public ConcurrentHashMap<String, String> getDictionary() throws RemoteException {
        return dictionary;
    }

    @Override
    public Node insert(String word, String definition) throws RemoteException {
        int key = hash32(word); // Compute hash of the word
        Node successorNode = findSuccessor(key); // Find the successor node for this key
        successorNode.setDictionary(word, definition); // Add word with the given definition
        nodeLogger.logInfo("Word '" + word + "' with definition '" + definition + "' added to the dictionary on Node " + successorNode.getID());
        return successorNode;
    }



    @Override
    public Node insert(String word) throws RemoteException {
        int key = hash32(word); // Compute hash of the word
        Node node = findSuccessor(key); // Find the successor node for this key
        node.setDictionary(word, null); // Add word with a null definition
        nodeLogger.logInfo("Word '" + word + "' added to the dictionary on Node " + node.getID());
        return node;
    }


    @Override
    public String lookup(String word) throws RemoteException {
        int key = hash32(word);
        Node responsibleNode = findSuccessor(key);

        if (responsibleNode.getID() == this.id) {
            if (dictionary.containsKey(word)) {
                nodeLogger.logInfo("Lookup for word '" + word + "' found on Node " + id);
                return dictionary.get(word);  // Return the definition found
            }
            return "Definition not found.";  // Or handle the case where the word isn't in the dictionary
        } else {
            return responsibleNode.lookup(word);
        }
    }

    @Override
    public String printFingerTable() throws RemoteException {
        StringBuilder sb = new StringBuilder();
        sb.append("Finger Table for ").append(url).append(":\n");
        for (int i = 0; i < fingerTable.size(); i++) {
            sb.append("Finger ").append(i).append(": ").append(fingerTable.get(modulo31Add(id, (int) Math.pow(2, i)))).append("\n");
        }
        return sb.toString();
    }

    @Override
    public int getDictionarySize() throws RemoteException {
        return dictionary.size();  // Return the count of entries in the dictionary
    }

    @Override
    public String printDictionary() throws RemoteException {
        StringBuilder sb = new StringBuilder();
        sb.append("Dictionary for ").append(url).append(":\n");
        for (Map.Entry<String, String> entry : dictionary.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    @Override
    public int getID() throws RemoteException {
        return id;
    }

    @Override
    public String getURL() throws RemoteException {
        return url;
    }

    @Override
    public void join(Node bootstrapNode) throws RemoteException, InterruptedException {
        if (bootstrapNode != null) {
            nodeLogger.logInfo("Attempting to join the network via Node " + bootstrapNode.getURL());
            boolean lockAcquired = bootstrapNode.acquireJoinLock(this.url);
            if (!lockAcquired) {
                nodeLogger.logInfo("Failed to acquire join lock, join aborted.");
                return;
            }
            try {
                initFingerTable(bootstrapNode);
                updateOthers();
                nodeLogger.logInfo("Joined the network, finger table and others updated.");
            } finally {
                bootstrapNode.releaseJoinLock(this.url);
            }

            move_keys_predecessor_to_new_node();
        } else {
            this.predecessor = this;
            this.successor = this;
            for (int i = 0; i < NUM_FINGERS; i++) {
                fingerTable.put(i, this);
            }
            nodeLogger.logInfo("Node0 has started and initialized the Chord ring.");
        }
    }

    private void move_keys_predecessor_to_new_node() throws RemoteException {
        Map<String, String> allPredecessorKeys = predecessor.getDictionary();
        List<String> keysToTransfer = new ArrayList<>();

        for (Map.Entry<String, String> entry : allPredecessorKeys.entrySet()) {
            String key = entry.getKey();
            int keyHash = hash32(key);

            if (isInRange(keyHash, predecessor.getID(), id)) {
                keysToTransfer.add(key);
            }
        }

        for (String key : keysToTransfer) {
            predecessor.remove(key);
            this.insert(key, allPredecessorKeys.get(key));
        }
    }

    public void updateOthers() throws RemoteException {
        if (this.predecessor != this) {
            this.predecessor.setSuccessor(this);
        }
        for (int i = 0; i < fingerTable.size(); i++) {
            Node successorNode = fingerTable.get(i);
            if (successorNode != null && !successorNode.equals(this) && !successorNode.equals(this.predecessor)) {
                successorNode.updateFingerTable(i);
            }
        }
    }

    public void updateFingerTable(int i) throws RemoteException {
        if (this.fingerTable.get(i) == null || isInRange(modulo31Add(this.id, (int) Math.pow(2, i)), this.fingerTable.get(i).getID(), this.id)) {
            this.fingerTable.put(i, this);
        }
    }

    private void initFingerTable(Node n_prime) throws RemoteException {
        System.out.println("Initializing finger table...");
        this.fingerTable.put(0, n_prime.findSuccessor(modulo31Add(this.id, 1)));
        //this.successor = fingerTable[0];
        this.predecessor = this.fingerTable.get(0).predecessor();

        //if (this.getURL().equals("node-3")) {
        //    System.out.println("!!!CHECKPOINT CHECK: " + this.fingerTable[0].getURL());
        //}
        this.fingerTable.get(0).setPredecessor(this);

        for (int i = 0; i < 30; i++) {
            int finger_i_start = modulo31Add(this.id, (1 << (i + 1)));

            if (isInRange(finger_i_start, this.id, fingerTable.get(i).getID())) {
                fingerTable.put(i+1,fingerTable.get(i));
            }
            else {
                fingerTable.put(i+1, n_prime.findSuccessor(finger_i_start));
            }
        }
        System.out.println("Finished initializing finger table...");
    }

    private boolean isInRange(int keyHash, int start, int end) {
        if (start <= end) {
            return start <= keyHash && keyHash < end;
        } else {
            return start <= keyHash || keyHash < end;
        }
    }

    private int modulo31Add(int id, int i) {
        int mod = (int) Math.pow(2, NUM_FINGERS - 1);
        int mod31 = (int) Math.pow(2, 31);
        return ((id + i) % mod + mod) % mod31;
    }

    @Override
    public Node findSuccessor(int key) throws RemoteException {
        nodeLogger.logInfo("Finding successor for key: " + key);
        Node current = this;
        while (key > current.getID() && key <= current.successor().getID()) {
            if (current.equals(this)) {
                break;
            }
            current = current.closestPrecedingFinger(key);
        }
        if (current == null || current.successor() == null) {
            nodeLogger.logWarning("No valid successor found for key: " + key + ", returning self.");
            return this;
        }
        return current.successor();
    }


    private Node closestPrecedingNode(int keyHash) throws RemoteException {
        for (int i = NUM_FINGERS - 1; i >= 0; i--) {
            if (this.fingerTable.get(i) != null && this.fingerTable.get(i).getID() != this.id) {
                if (isInRange(keyHash, this.fingerTable.get(i).getID(), this.id)) {
                    return this.fingerTable.get(i);
                }
            }
        }
        return this.predecessor;
    }

    @Override
    public void setSuccessor(Node newSuccessor) throws RemoteException {
        this.successor = newSuccessor;
    }

    @Override
    public void setPredecessor(Node newPredecessor) throws RemoteException {
        this.predecessor = newPredecessor;
    }

    private static final int JOIN_LOCK_TIMEOUT = 10;

    private int hash32(String key) {
        return FNV1aHash.hash32(key);
    }

}