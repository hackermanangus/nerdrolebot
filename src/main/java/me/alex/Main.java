package me.alex;

import me.alex.discord.RoleUpdater;
import me.alex.sql.DatabaseConnectionManager;
import me.alex.sql.RoleUpdateQuery;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.*;

/**
 * Hello world!
 *
 */
public class Main {
    public static void main(String[] args) {
        try {
            firstTimeDatabaseSetup();
            JDA jda;
            JDABuilder jdaBuilder = JDABuilder.createDefault("token");
            jda = jdaBuilder.build();
            jda.awaitReady();
            DatabaseConnectionManager databaseConnectionManager = new DatabaseConnectionManager();
            RoleUpdateQuery roleUpdateQuery = new RoleUpdateQuery(databaseConnectionManager);
            roleUpdateQuery.addListener(new RoleUpdater(jda));
            roleUpdateQuery.run();
        } catch (LoginException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void firstTimeDatabaseSetup() throws IOException {
        String workingDir = Paths.get("").toAbsolutePath().toString();
        if (new File(workingDir + "\\nerds.db").exists()) return;
        System.err.println("Could not find existing nerds database, creating...");
        String url = "jdbc:sqlite:" + workingDir + "\\nerds.db";
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                System.out.println("The driver name is " + meta.getDriverName());
                System.out.println("Nerds database file has been created!");
                String sql = "CREATE TABLE messages (id integer, time sqlite3_int64);";
                Statement statement = conn.createStatement();
                statement.execute(sql);
                System.out.println("Created table messages!");
                sql = "CREATE TABLE levels (id integer, score integer);";
                statement = conn.createStatement();
                statement.execute(sql);
                System.out.println("Created table levels!");
            }
        } catch (SQLException e) {
            throw new IOException("Couldn't create new database with tables!", e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        System.out.println("Successfully connected to the database!");
    }
}