package me.totten.databasehandler.Handlers;

import com.sun.org.apache.xpath.internal.operations.Bool;
import me.totten.databasehandler.DatabaseHandler;
import me.totten.databasehandler.callbacks.BooleanCallback;
import me.totten.databasehandler.callbacks.HashCallback;
import me.totten.databasehandler.callbacks.ListHashCallback;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DatabaseManager {
    private static Connection connection;
    private static String host, database, username, password;
    private static int port;

    private static DatabaseHandler plugin;

    public static void InitHandler(DatabaseHandler pluginRef, String hostRef, int portRef, String databaseRef, String usernameRef, String passwordRef) {
        plugin = pluginRef;
        host = hostRef;
        port = portRef;
        database = databaseRef;
        username = usernameRef;
        password = passwordRef;
    }

    public static void openConnection() throws SQLException, ClassNotFoundException {

        if (connection != null && !connection.isClosed()) {
            return;
        }


        synchronized (plugin) {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database, username, password);
        }

    }
    public static Connection getConnection() {
        return connection;
    }
    public static void closeConnection() {
        try {
            connection.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static void runExecutionCommandAsync(final String insert, final BooleanCallback callback){
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try{
                    openConnection();
                    Statement statement = connection.createStatement();

                    int success = statement.executeUpdate(insert);
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            callback.onQueryDone(success > 0 ? true : false);
                        }
                    });
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
    }
    // REFACTOR AT LATER POINT
    public static void insertAsync(final String table, HashMap<String, Object> inserts, final BooleanCallback callback){
        String columns = String.join(", ", inserts.keySet());
        String keySet = "";
        for(Object o : inserts.values()){
            if (o instanceof  String){
                keySet += "'" + o.toString() + "',";
            }else{
                keySet += String.valueOf(o)+ ",";
            }
        }
        keySet = keySet.substring(0, keySet.length() - 1);

        String command = "INSERT INTO " + table + "( " + columns + ") VALUES (" + keySet + ");";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try{
                    openConnection();
                    Statement statement = connection.createStatement();

                    int success = statement.executeUpdate(command);
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            callback.onQueryDone(success > 0 ? true : false);
                        }
                    });
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        });



    }


    public static void updateEntryAsync(final String table, HashMap<String, Object> updates, final String primaryKeyColumn, Object primaryKey, final BooleanCallback callback){
        String changes = "";
        for (String s : updates.keySet()) {
            if (updates.get(s) instanceof String) {
                changes += s + " = '" + updates.get(s) + "',";
                continue;
            }
            changes += s + " = " + updates.get(s) + ",";
        }
        changes = changes.substring(0, changes.length() - 1);
        if(primaryKey instanceof String)
            primaryKey = "'" + primaryKey + "'";
        final String query = String.format("UPDATE %1$s SET %2$s WHERE %3$s = %4$s;", table, changes, primaryKeyColumn, primaryKey);


        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    openConnection();
                    Statement statement = connection.createStatement();
                    statement.executeUpdate(query);
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            callback.onQueryDone(true);
                        }
                    });
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

            }
        });
    }
    public static void updateLinkTableEntryAsync(final String table, HashMap<String, Object> updates, final List<String> primaryKeyColumns, final List<Object> primaryKeys, final BooleanCallback callback ){
        String changes = "";
        for (String s : updates.keySet()) {
            if (updates.get(s) instanceof String) {
                changes += s + " = '" + updates.get(s) + "',";
                continue;
            }
            changes += s + " = " + updates.get(s) + ",";
        }
        changes = changes.substring(0, changes.length() - 1);

        String where  = "WHERE ";
        for(int i = 0; i < primaryKeyColumns.size(); i++){
            String pk = primaryKeyColumns.get(i) instanceof  String ? "'" + primaryKeyColumns.get(i) + "' AND " : primaryKeyColumns.get(i) + " AND ";
            where += primaryKeyColumns.get(i) + " = " + pk;
        }
        changes = changes.substring(0, changes.length() - 4) + ";";

        final String query = String.format("UPDATE %1$S SET %2$S %3$S", table, changes, where);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    openConnection();
                    Statement statement = connection.createStatement();
                    statement.executeUpdate(query);
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            callback.onQueryDone(true);
                        }
                    });
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public static void getRowFromTable(String table, String searchColumn, Object searchValue, HashCallback callback){
        if(searchValue instanceof String)
            searchValue = "'" + searchValue + "'";
        final String query = String.format("SELECT * FROM %1$s WHERE %2$s = %3$s;", table, searchColumn, searchValue);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    openConnection();
                    Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery(query);
                    HashMap<Object, Object> map = new HashMap<Object, Object>();
                    result.next();
                    for (int i = result.getMetaData().getColumnCount(); i > 0; i--) {
                        map.put(result.getMetaData().getColumnName(i), result.getString(i));
                    }
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            callback.onQueryDone(map);
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    public static void getAllRowsFromTable(String table, ListHashCallback callback){
        final String stringQuery = "SELECT * FROM " + table;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try{
                    openConnection();
                    Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery(stringQuery);
                    List<HashMap<Object, Object>> rows = new ArrayList<>();

                    while(result.next()){
                        HashMap<Object, Object> map = new HashMap<Object, Object>();
                        for (int i = result.getMetaData().getColumnCount(); i > 0; i--) {
                            map.put(result.getMetaData().getColumnName(i), result.getObject(i));
                        }
                        rows.add(map);
                    }
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            callback.onQueryDone(rows);
                        }
                    });
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
    }
    public static void getAllRowsWhere(String table, String searchColumn, Object value, ListHashCallback callback){
        if (value instanceof  String)
            value = "'" + value + "'";
        final String query = String.format("SELECT * FROM %1$s WHERE %2$s = %3$s;", table, searchColumn, value);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    openConnection();
                    Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery(query);
                    List<HashMap<Object, Object>> rows = new ArrayList<>() ;

                    while(result.next()) {
                        HashMap<Object, Object> map = new HashMap<Object, Object>();

                        for (int i = result.getMetaData().getColumnCount(); i > 0; i--) {
                            map.put(result.getMetaData().getColumnName(i), result.getObject(i));
                        }

                        rows.add(map);
                    }
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            callback.onQueryDone(rows);
                        }
                    });


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
