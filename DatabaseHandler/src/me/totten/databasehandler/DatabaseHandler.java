package me.totten.databasehandler;

import com.sun.net.httpserver.Authenticator;
import com.sun.org.apache.xpath.internal.operations.Bool;
import me.totten.databasehandler.Handlers.DatabaseManager;
import me.totten.databasehandler.callbacks.BooleanCallback;
import me.totten.databasehandler.callbacks.ListHashCallback;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class DatabaseHandler extends JavaPlugin {
    @Override
    public void onEnable(){
        DatabaseManager.InitHandler(this, "localhost", 3306, "plugin_tests", "plugin", "plugin");

        DatabaseManager.getAllRowsFromTable("players", new ListHashCallback() {
            @Override
            public void onQueryDone(List<HashMap<Object, Object>> hashList) {
                for(HashMap<Object, Object> hash : hashList){
                    System.out.println(hash.values());
                }
            }
        });
    }
    @Override
    public void onDisable(){
        DatabaseManager.closeConnection();
    }
}
