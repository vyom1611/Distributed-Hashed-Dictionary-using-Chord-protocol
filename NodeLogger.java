import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class NodeLogger {
    private final Logger logger;
    private static final String LOG_FILE_PATTERN = "node-%d-log.log";
    private static final String LOG_FOLDER = "./logs/";  // Directory where logs are stored

    public NodeLogger(int nodeId) {
        this.logger = Logger.getLogger("Node" + nodeId);
        setupLogger(nodeId);
    }

    private void setupLogger(int nodeId) {
        try {
            // Ensure the logs directory exists or create it
            new java.io.File(LOG_FOLDER).mkdirs();

            // Create a new log file
            FileHandler fileHandler = new FileHandler(LOG_FOLDER + String.format(LOG_FILE_PATTERN, nodeId), true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setUseParentHandlers(false); // Do not use the console handler
        } catch (IOException e) {
            logger.severe("Failed to initialize logger: " + e.getMessage());
        }
    }

    public void logInfo(String message) {
        logger.info(String.format("%s [%s] %s: %s%n", getTimestamp(), Thread.currentThread().getName(), "INFO", message));
    }

    public void logWarning(String message) {
        logger.warning(String.format("%s [%s] %s: %s%n", getTimestamp(), Thread.currentThread().getName(), "WARNING", message));
    }

    public void logSevere(String message) {
        logger.severe(String.format("%s [%s] %s: %s%n", getTimestamp(), Thread.currentThread().getName(), "SEVERE", message));
    }

    private String getTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
    }
}