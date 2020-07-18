package me.totten.databasehandler.callbacks;

import java.util.HashMap;
import java.util.List;

public interface ListHashCallback {
    public void onQueryDone(List<HashMap<Object, Object>> hashList);
}
