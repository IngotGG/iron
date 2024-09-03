import gg.ingot.iron.Iron;
import gg.ingot.iron.IronSettings;
import gg.ingot.iron.strategies.NamingStrategy;
import models.UserClass;
import models.UserRecord;
import org.junit.jupiter.api.Test;

class JavaDatabaseTest {
    
    @Test
    void testCompletableConnection() throws Exception {
        final var iron = Iron.create(
            "jdbc:sqlite::memory:",
            new IronSettings.Builder()
                .namingStrategy(NamingStrategy.SNAKE_CASE)
                .build()
        ).connect().completable();
        
        iron.transaction((transaction) -> {
            transaction.afterCommit(() -> {
                System.out.println("Transaction committed");
            });
            
            transaction.prepare("CREATE TABLE IF NOT EXISTS users (name TEXT PRIMARY KEY, age INTEGER, active BOOLEAN)");
            transaction.prepare("INSERT INTO users (name, age, active) VALUES (?, ?, ?)", "John Doe", 30, true);
        }).thenCompose((ignored) -> {
            
            return iron.prepare("SELECT * FROM users").thenAccept((result) -> {
                final var user = result.single(UserClass.class);
                System.out.println(user.name());
            });
            
        }).get();
    }
    
    @Test
    void testCompletableRecordConnection() throws Exception {
        final var iron = Iron.create(
            "jdbc:sqlite::memory:",
            new IronSettings.Builder()
                .namingStrategy(NamingStrategy.SNAKE_CASE)
                .build()
        ).connect().completable();
        
        iron.transaction((transaction) -> {
            transaction.afterCommit(() -> {
                System.out.println("Transaction committed");
            });
            
            transaction.prepare("CREATE TABLE IF NOT EXISTS users (name TEXT PRIMARY KEY, age INTEGER, active BOOLEAN)");
            transaction.prepare("INSERT INTO users (name, age, active) VALUES (?, ?, ?)", "John Doe", 30, true);
        }).thenCompose((ignored) -> {
            
            return iron.prepare("SELECT * FROM users").thenAccept((result) -> {
                final var user = result.single(UserRecord.class);
                System.out.println(user.name());
            });
            
        }).get();
    }
    
    @Test
    void testBlockingConnection() throws Exception {
        final var iron = Iron.create(
            "jdbc:sqlite::memory:",
            new IronSettings.Builder()
                .namingStrategy(NamingStrategy.SNAKE_CASE)
                .build()
        ).connect().blocking();
        
        iron.transaction((transaction) -> {
            transaction.afterCommit(() -> {
                System.out.println("Transaction committed");
            });
            
            transaction.prepare("CREATE TABLE IF NOT EXISTS users (name TEXT PRIMARY KEY, age INTEGER, active BOOLEAN)");
            transaction.prepare("INSERT INTO users (name, age, active) VALUES (?, ?, ?)", "John Doe", 30, true);
        });
        
        final var user = iron.prepare("SELECT * FROM users").single(UserClass.class);
        System.out.println(user.name());
    }
    
    @Test
    void testBlockingRecordConnection() throws Exception {
        final var iron = Iron.create(
            "jdbc:sqlite::memory:",
            new IronSettings.Builder()
                .namingStrategy(NamingStrategy.SNAKE_CASE)
                .build()
        ).connect().blocking();
        
        iron.transaction((transaction) -> {
            transaction.afterCommit(() -> {
                System.out.println("Transaction committed");
            });
            
            transaction.prepare("CREATE TABLE IF NOT EXISTS users (name TEXT PRIMARY KEY, age INTEGER, active BOOLEAN)");
            transaction.prepare("INSERT INTO users (name, age, active) VALUES (?, ?, ?)", "John Doe", 30, true);
        });
        
        final var user = iron.prepare("SELECT * FROM users").single(UserRecord.class);
        System.out.println(user.name());
    }
    
}
