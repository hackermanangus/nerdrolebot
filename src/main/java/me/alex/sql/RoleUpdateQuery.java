package me.alex.sql;


import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class RoleUpdateQuery implements Runnable, DatabaseAccessListener {
    private boolean inQueue = false;
    private boolean safeToAccess = true;
    private final ArrayList<ScoreMapReadyListener> scoreMapReadyListeners = new ArrayList<>();
    private final DatabaseConnectionManager databaseConnectionManager;
    public void addListener(ScoreMapReadyListener scoreMapReadyListener) {
        scoreMapReadyListeners.add(scoreMapReadyListener);
    }
    public RoleUpdateQuery(DatabaseConnectionManager databaseConnectionManager) {
        databaseConnectionManager.addListener(this);
        this.databaseConnectionManager = databaseConnectionManager;
    }
    @Override
    public void run() {
        if (safeToAccess) {
            inQueue = false;
            databaseConnectionManager.notifyAccess();
            setScoreMap();
            databaseConnectionManager.notifyStopAccess();
            try {
                Thread.sleep(120000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            inQueue = true;
        }
    }

    @Override
    public void onDatabaseAccessEvent() {
        safeToAccess = false;
    }

    @Override
    public void onDatabaseStopAccessEvent()  {
        safeToAccess = true;
        if (inQueue) {
            databaseConnectionManager.notifyAccess();
            setScoreMap();
            databaseConnectionManager.notifyStopAccess();
            try {
                Thread.sleep(120000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    public void setScoreMap() {
        HashMap<Long, Long> scoreMap = new HashMap<>();
        String workingDir = Paths.get("").toAbsolutePath().toString();
        String url = "jdbc:sqlite:" + workingDir + "\\nerds.db";
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
            if (conn != null) {
                long twoWeeksInMillis = (long) 1.21e+9;
                String sql = "SELECT DISTINCT id FROM messages WHERE time >= " + System.currentTimeMillis() + " - " + twoWeeksInMillis;
                Statement statement = conn.createStatement();
                statement.execute(sql);
                ResultSet resultSet = statement.getResultSet();
                while (resultSet.next()) {
                    scoreMap.put(resultSet.getLong("id"), null);
                }
                for (long i : scoreMap.keySet()) {
                    sql = String.format("SELECT count(id) FROM messages WHERE time >= %s - %s and id = %s", System.currentTimeMillis(), twoWeeksInMillis, i);
                    statement = conn.createStatement();
                    statement.execute(sql);
                    resultSet = statement.getResultSet();
                    while (resultSet.next()) {
                        scoreMap.put(i, resultSet.getLong("count(id)"));
                    }
                }
                for (ScoreMapReadyListener scoreMapReadyListener : scoreMapReadyListeners) {
                    scoreMapReadyListener.onScoreMapReadyEvent(scoreMap);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

}