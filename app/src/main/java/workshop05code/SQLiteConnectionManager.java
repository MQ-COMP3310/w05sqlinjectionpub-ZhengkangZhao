package workshop05code;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

//Import for logging exercise
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class SQLiteConnectionManager {
    //Start code logging exercise
    static {
        // must set before the Logger
        // loads logging.properties from the classpath
        try {// resources\logging.properties
            LogManager.getLogManager().readConfiguration(new FileInputStream("resources/logging.properties"));
        } catch (SecurityException | IOException e1) {
            e1.printStackTrace();
        }
    }

    private static final Logger logger = Logger.getLogger(SQLiteConnectionManager.class.getName());

    static {
        setupLogger();
    }

    private static void setupLogger() {
        try {
            // Console handler that only logs game-related info
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.INFO);
            consoleHandler.setFormatter(new SimpleFormatter());

            // File handler logs all messages
            Handler fileHandler = new FileHandler("wordle_game_logs.log", true);
            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(new SimpleFormatter());

            logger.addHandler(fileHandler);
            logger.addHandler(consoleHandler);
            logger.setUseParentHandlers(false);
            logger.setLevel(Level.ALL); 

            LogManager.getLogManager().readConfiguration(); 
        } catch (IOException e) {
            System.err.println("Failed to initialize logger handlers: " + e.getMessage());
        }
    }
    //End code logging exercise
    
    private String databaseURL = "";

    private static final String WORDLE_DROP_TABLE_STRING = "DROP TABLE IF EXISTS wordlist;";
    private static final String WORDLE_CREATE_STRING = "CREATE TABLE wordlist (\n"
            + " id integer PRIMARY KEY,\n"
            + " word text NOT NULL\n"
            + ");";

    private static final String VALID_WORDS_DROP_TABLE_STRING = "DROP TABLE IF EXISTS validWords;";
    private static final String VALID_WORDS_CREATE_STRING = "CREATE TABLE validWords (\n"
            + " id integer PRIMARY KEY,\n"
            + " word text NOT NULL\n"
            + ");";
    /**
     * Set the database file name in the sqlite project to use
     *
     * @param fileName the database file name
     */
    public SQLiteConnectionManager(String filename) {
        databaseURL = "jdbc:sqlite:sqlite/" + filename;

    }

    /**
     * Connect to a sample database
     *
     * @param fileName the database file name
     */
    public void createNewDatabase(String fileName) {

        try (Connection conn = DriverManager.getConnection(databaseURL)) {
            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                logger.info("The driver name is " + meta.getDriverName());
                logger.info("A new database has been created.");

            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to create new database.", e);
        }
    }

    /**
     * Check that the file has been cr3eated
     *
     * @return true if the file exists in the correct location, false otherwise. If
     *         no url defined, also false.
     */
    public boolean checkIfConnectionDefined() {
        if (databaseURL.equals("")) {
            return false;
        } else {
            try (Connection conn = DriverManager.getConnection(databaseURL)) {
                if (conn != null) {
                    return true;
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to establish a database connection.", e);
                return false;
            }
        }
        return false;
    }

    /**
     * Create the table structures (2 tables, wordle words and valid words)
     *
     * @return true if the table structures have been created.
     */
    public boolean createWordleTables() {
        if (databaseURL.equals("")) {
            return false;
        } else {
            try (Connection conn = DriverManager.getConnection(databaseURL);
                    Statement stmt = conn.createStatement()) {
                stmt.execute(WORDLE_DROP_TABLE_STRING);
                stmt.execute(WORDLE_CREATE_STRING);
                stmt.execute(VALID_WORDS_DROP_TABLE_STRING);
                stmt.execute(VALID_WORDS_CREATE_STRING);
                logger.info("Tables created successfully.");
                return true;

            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to create tables.", e);
                return false;
            }
        }
    }

    /**
     * Take an id and a word and store the pair in the valid words
     * 
     * @param id   the unique id for the word
     * @param word the word to store
     */
    public void addValidWord(int id, String word) {
        // Validate that the word is exactly 4 lowercase letters.
        if (!word.matches("^[a-z]{4}$")) {
            logger.warning("Invalid input attempt: " + word); // Log invalid guess at WARNING level
            System.out.println("Ignoring unacceptable input: " + word);
            return;
    }

        String sql = "INSERT INTO validWords(id,word) VALUES(?, ?)";

        try (Connection conn = DriverManager.getConnection(databaseURL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            pstmt.setString(2, word);
            pstmt.executeUpdate();
            logger.info("Added valid word to database: " + word);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "SQL error when trying to add a valid word", e);
        }

    }
    

    /**
     * Possible weakness here?
     * 
     * @param guess the string to check if it is a valid word.
     * @return true if guess exists in the database, false otherwise
     */
    public boolean isValidWord(String guess) {
        String sql = "SELECT count(id) as total FROM validWords WHERE word like ?";

        try (Connection conn = DriverManager.getConnection(databaseURL);
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, guess);
            ResultSet resultRows = stmt.executeQuery();
            if (resultRows.next()) {
                int result = resultRows.getInt("total");
                return (result >= 1);
            }

            return false;

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }

    }
}
