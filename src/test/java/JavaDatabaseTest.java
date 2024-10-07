import gg.ingot.iron.Iron;
import gg.ingot.iron.IronSettings;
import gg.ingot.iron.stratergies.NamingStrategy;
import models.UserClass;
import models.UserOptional;
import models.UserRecord;
import org.junit.jupiter.api.Test;

import java.util.Optional;

class JavaDatabaseTest {
    
    @Test
    void testJavaCompletable() throws Exception {
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
                assert user.name().equals("John Doe");
                assert user.age() == 30;
                assert user.active();
            });
            
        }).get();
    }
    
    @Test
    void testCompletableRecord() throws Exception {
        final var iron = Iron.create(
            "jdbc:sqlite::memory:",
            new IronSettings.Builder()
                .namingStrategy(NamingStrategy.SNAKE_CASE)
                .build()
        ).connect().completable();
        
        iron.transaction((transaction) -> {
            transaction.prepare("CREATE TABLE IF NOT EXISTS users (name TEXT PRIMARY KEY, age INTEGER, active BOOLEAN)");
            transaction.prepare("INSERT INTO users (name, age, active) VALUES (?, ?, ?)", "John Doe", 30, true);
        }).thenCompose((ignored) -> {
            
            return iron.prepare("SELECT * FROM users").thenAccept((result) -> {
                final var user = result.single(UserRecord.class);
                assert user.name().equals("John Doe");
                assert user.age() == 30;
                assert user.active();
            });
            
        }).get();
    }
    
    @Test
    void testJavaBlocking() {
        final var iron = Iron.create(
            "jdbc:sqlite::memory:",
            new IronSettings.Builder()
                .namingStrategy(NamingStrategy.SNAKE_CASE)
                .build()
        ).connect().blocking();
        
        iron.transaction((transaction) -> {
            transaction.prepare("CREATE TABLE IF NOT EXISTS users (name TEXT PRIMARY KEY, age INTEGER, active BOOLEAN)");
            transaction.prepare("INSERT INTO users (name, age, active) VALUES (?, ?, ?)", "John Doe", 30, true);
        });
        
        final var user = iron.prepare("SELECT * FROM users").single(UserClass.class);
        assert user.name().equals("John Doe");
        assert user.age() == 30;
        assert user.active();
    }
    
    @Test
    void testBlockingRecord() {
        final var iron = Iron.create(
            "jdbc:sqlite::memory:",
            new IronSettings.Builder()
                .namingStrategy(NamingStrategy.SNAKE_CASE)
                .build()
        ).connect().blocking();
        
        iron.transaction((transaction) -> {
            transaction.prepare("CREATE TABLE IF NOT EXISTS users (name TEXT PRIMARY KEY, age INTEGER, active BOOLEAN)");
            transaction.prepare("INSERT INTO users (name, age, active) VALUES (?, ?, ?)", "John Doe", 30, true);
        });
        
        final var user = iron.prepare("SELECT * FROM users").single(UserRecord.class);
        assert user.name().equals("John Doe");
        assert user.age() == 30;
        assert user.active();
    }
    
    @Test
    void testOptionalRecord() {
        final var iron = Iron.create(
            "jdbc:sqlite::memory:",
            new IronSettings.Builder()
                .namingStrategy(NamingStrategy.SNAKE_CASE)
                .build()
        ).connect().blocking();
        
        iron.transaction((transaction) -> {
            transaction.prepare("CREATE TABLE IF NOT EXISTS users (name TEXT PRIMARY KEY, age INTEGER, active BOOLEAN)");
            transaction.prepare("INSERT INTO users (name, age, active) VALUES (?, ?, ?)", "John Doe", Optional.empty(), true);
        });
        
        var user = iron.prepare("SELECT * FROM users").single(UserOptional.class);
        assert user.age().isEmpty();
        
        iron.prepare("INSERT INTO users(name, age, active) VALUES(?, ?, ?)", "Jane Doe", Optional.of(30), false);
        user = iron.prepare("SELECT * FROM users WHERE name = ?", "Jane Doe").single(UserOptional.class);
        
        assert user.age().isPresent();
        assert user.age().get() == 30;
    }
    
}
