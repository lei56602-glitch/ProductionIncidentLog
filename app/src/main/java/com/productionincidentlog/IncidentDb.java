package com.productionincidentlog;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class IncidentDb extends SQLiteOpenHelper {
    public IncidentDb(Context context) { super(context, "production_incidents.db", null, 1); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE equipment(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE,active INTEGER NOT NULL DEFAULT 1)");
        db.execSQL("CREATE TABLE incidents(id INTEGER PRIMARY KEY AUTOINCREMENT,code TEXT NOT NULL UNIQUE,occurred_at TEXT NOT NULL,team TEXT NOT NULL,area TEXT NOT NULL,type TEXT NOT NULL,equipment TEXT NOT NULL,people TEXT,description TEXT NOT NULL,analysis TEXT NOT NULL,cause TEXT NOT NULL,immediate_action TEXT NOT NULL,prevention TEXT NOT NULL,owner TEXT,due_date TEXT,status TEXT NOT NULL DEFAULT '待整改',created_at TEXT NOT NULL)");
        seed(db, "滚筒"); seed(db, "除湿机");
    }

    private void seed(SQLiteDatabase db, String name) {
        ContentValues v = new ContentValues(); v.put("name", name);
        db.insertWithOnConflict("equipment", null, v, SQLiteDatabase.CONFLICT_IGNORE);
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }

    public List<String> equipmentNames() {
        List<String> names = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT name FROM equipment WHERE active=1 ORDER BY name", null)) {
            while (c.moveToNext()) names.add(c.getString(0));
        }
        return names;
    }
    public boolean addEquipment(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        ContentValues v = new ContentValues(); v.put("name", name.trim());
        return getWritableDatabase().insertWithOnConflict("equipment", null, v, SQLiteDatabase.CONFLICT_IGNORE) != -1;
    }
    public String addIncident(String date, String team, String area, String type, String equipment,
            String people, String description, String analysis, String cause, String action, String prevention, String owner, String dueDate) {
        String code = "INC-" + System.currentTimeMillis();
        ContentValues v = new ContentValues();
        v.put("code", code); v.put("occurred_at", date); v.put("team", team); v.put("area", area);
        v.put("type", type); v.put("equipment", equipment); v.put("people", people);
        v.put("description", description); v.put("analysis", analysis); v.put("cause", cause);
        v.put("immediate_action", action); v.put("prevention", prevention); v.put("owner", owner); v.put("due_date", dueDate);
        v.put("created_at", String.valueOf(System.currentTimeMillis()));
        getWritableDatabase().insertOrThrow("incidents", null, v);
        return code;
    }
    public int countAll() { return scalar("SELECT COUNT(*) FROM incidents", null); }
    public int countType(String type) { return scalar("SELECT COUNT(*) FROM incidents WHERE type=?", new String[]{type}); }
    public int countStatus(String status) { return scalar("SELECT COUNT(*) FROM incidents WHERE status=?", new String[]{status}); }
    private int scalar(String sql, String[] args) {
        try (Cursor c = getReadableDatabase().rawQuery(sql, args)) { return c.moveToFirst() ? c.getInt(0) : 0; }
    }
    public List<String[]> incidents(String keyword) {
        List<String[]> rows = new ArrayList<>();
        String q = "%" + keyword + "%";
        String sql = "SELECT code,occurred_at,team,area,type,equipment,people,description,analysis,cause,immediate_action,prevention,owner,due_date,status FROM incidents WHERE (?='' OR code LIKE ? OR equipment LIKE ? OR type LIKE ? OR description LIKE ?) ORDER BY occurred_at DESC,id DESC";
        try (Cursor c = getReadableDatabase().rawQuery(sql, new String[]{keyword,q,q,q,q})) {
            while (c.moveToNext()) {
                String[] row = new String[15];
                for (int i=0;i<15;i++) row[i] = c.isNull(i) ? "" : c.getString(i);
                rows.add(row);
            }
        }
        return rows;
    }

    public List<String[]> incidentsFiltered(String from, String to, String team, String area, String type, String equipment) {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT code,occurred_at,team,area,type,equipment,people,description,analysis,cause,immediate_action,prevention,status FROM incidents " +
            "WHERE (?='' OR substr(occurred_at,1,10)>=?) AND (?='' OR substr(occurred_at,1,10)<=?) " +
            "AND (?='全部班组' OR team=?) AND (?='全部区域' OR area=?) " +
            "AND (?='全部类型' OR type=?) AND (?='全部设备' OR equipment=?) ORDER BY occurred_at DESC,id DESC";
        String[] args = {from,from,to,to,team,team,area,area,type,type,equipment,equipment};
        try (Cursor c = getReadableDatabase().rawQuery(sql,args)) {
            while (c.moveToNext()) {
                String[] row = new String[13];
                for (int i=0;i<13;i++) row[i] = c.isNull(i) ? "" : c.getString(i);
                rows.add(row);
            }
        }
        return rows;
    }

    public void updateStatus(String code, String status) {
        ContentValues values = new ContentValues();
        values.put("status", status);
        getWritableDatabase().update("incidents", values, "code=?", new String[]{code});
    }
}
