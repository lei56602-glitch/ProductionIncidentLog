package com.productionincidentlog;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class IncidentDb extends SQLiteOpenHelper {
    public IncidentDb(Context context) { super(context, "production_incidents.db", null, 3); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE equipment(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE,active INTEGER NOT NULL DEFAULT 1)");
        db.execSQL("CREATE TABLE incidents(id INTEGER PRIMARY KEY AUTOINCREMENT,code TEXT NOT NULL UNIQUE,occurred_at TEXT NOT NULL,team TEXT NOT NULL,area TEXT NOT NULL,type TEXT NOT NULL,equipment TEXT NOT NULL,people TEXT,description TEXT NOT NULL,analysis TEXT NOT NULL,cause TEXT NOT NULL,immediate_action TEXT NOT NULL,prevention TEXT NOT NULL,owner TEXT,due_date TEXT,status TEXT NOT NULL DEFAULT '待整改',created_at TEXT NOT NULL)");
        seed(db, "滚筒"); seed(db, "除湿机");
        db.execSQL("CREATE TABLE incident_attachments(id INTEGER PRIMARY KEY AUTOINCREMENT,incident_code TEXT NOT NULL,uri TEXT NOT NULL,mime_type TEXT NOT NULL,added_at TEXT NOT NULL,FOREIGN KEY(incident_code) REFERENCES incidents(code) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE incident_status_history(id INTEGER PRIMARY KEY AUTOINCREMENT,incident_code TEXT NOT NULL,old_status TEXT NOT NULL,new_status TEXT NOT NULL,changed_at TEXT NOT NULL)");
    }

    private void seed(SQLiteDatabase db, String name) {
        ContentValues v = new ContentValues(); v.put("name", name);
        db.insertWithOnConflict("equipment", null, v, SQLiteDatabase.CONFLICT_IGNORE);
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) db.execSQL("CREATE TABLE IF NOT EXISTS incident_attachments(id INTEGER PRIMARY KEY AUTOINCREMENT,incident_code TEXT NOT NULL,uri TEXT NOT NULL,mime_type TEXT NOT NULL,added_at TEXT NOT NULL,FOREIGN KEY(incident_code) REFERENCES incidents(code) ON DELETE CASCADE)");
        if (oldVersion < 3) db.execSQL("CREATE TABLE IF NOT EXISTS incident_status_history(id INTEGER PRIMARY KEY AUTOINCREMENT,incident_code TEXT NOT NULL,old_status TEXT NOT NULL,new_status TEXT NOT NULL,changed_at TEXT NOT NULL)");
    }

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
        SQLiteDatabase database = getWritableDatabase();
        database.insertOrThrow("incidents", null, v);
        ContentValues initialEvent = new ContentValues();
        initialEvent.put("incident_code",code); initialEvent.put("old_status","新建");
        initialEvent.put("new_status","待整改");
        initialEvent.put("changed_at",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.getDefault()).format(new Date()));
        database.insert("incident_status_history",null,initialEvent);
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
        String sql = "SELECT code,occurred_at,team,area,type,equipment,people,description,analysis,cause,immediate_action,prevention,owner,due_date,status FROM incidents " +
            "WHERE (?='' OR substr(occurred_at,1,10)>=?) AND (?='' OR substr(occurred_at,1,10)<=?) " +
            "AND (?='全部班组' OR team=?) AND (?='全部区域' OR area=?) " +
            "AND (?='全部类型' OR type=?) AND (?='全部设备' OR equipment=?) ORDER BY occurred_at DESC,id DESC";
        String[] args = {from,from,to,to,team,team,area,area,type,type,equipment,equipment};
        try (Cursor c = getReadableDatabase().rawQuery(sql,args)) {
            while (c.moveToNext()) {
                String[] row = new String[15];
                for (int i=0;i<15;i++) row[i] = c.isNull(i) ? "" : c.getString(i);
                rows.add(row);
            }
        }
        return rows;
    }


    public void addAttachment(String code, String uri, String mimeType) {
        ContentValues v = new ContentValues();
        v.put("incident_code", code); v.put("uri", uri); v.put("mime_type", mimeType);
        v.put("added_at", String.valueOf(System.currentTimeMillis()));
        getWritableDatabase().insert("incident_attachments", null, v);
    }
    public List<String[]> attachments(String code) {
        List<String[]> rows = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT uri,mime_type,added_at FROM incident_attachments WHERE incident_code=? ORDER BY id", new String[]{code})) {
            while (c.moveToNext()) rows.add(new String[]{c.getString(0),c.getString(1),c.getString(2)});
        }
        return rows;
    }



    public void updateAttachmentUri(String code, String oldUri, String newUri) {
        ContentValues values = new ContentValues(); values.put("uri", newUri);
        getWritableDatabase().update("incident_attachments", values,
            "incident_code=? AND uri=?", new String[]{code, oldUri});
    }

    public List<String[]> statusHistory(String code) {
        List<String[]> rows = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT old_status,new_status,changed_at FROM incident_status_history WHERE incident_code=? ORDER BY id DESC", new String[]{code})) {
            while (c.moveToNext()) rows.add(new String[]{c.getString(0),c.getString(1),c.getString(2)});
        }
        return rows;
    }

    public int updateIncident(String code, String date, String team, String area, String type, String equipment,
            String people, String description, String analysis, String cause, String action, String prevention, String owner, String dueDate) {
        ContentValues v = new ContentValues();
        v.put("occurred_at",date); v.put("team",team); v.put("area",area); v.put("type",type); v.put("equipment",equipment);
        v.put("people",people); v.put("description",description); v.put("analysis",analysis); v.put("cause",cause);
        v.put("immediate_action",action); v.put("prevention",prevention); v.put("owner",owner); v.put("due_date",dueDate);
        return getWritableDatabase().update("incidents",v,"code=?",new String[]{code});
    }
    public void deleteIncident(String code) {
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            database.delete("incident_attachments","incident_code=?",new String[]{code});
            database.delete("incident_status_history","incident_code=?",new String[]{code});
            database.delete("incidents","code=?",new String[]{code});
            database.setTransactionSuccessful();
        } finally { database.endTransaction(); }
    }

    public void updateStatus(String code, String status) {
        SQLiteDatabase database = getWritableDatabase();
        String previous = "";
        try (Cursor c = database.rawQuery("SELECT status FROM incidents WHERE code=?", new String[]{code})) {
            if (c.moveToFirst()) previous = c.getString(0);
        }
        if (status == null || status.equals(previous)) return;
        database.beginTransaction();
        try {
            ContentValues values = new ContentValues(); values.put("status", status);
            database.update("incidents", values, "code=?", new String[]{code});
            ContentValues event = new ContentValues();
            event.put("incident_code",code); event.put("old_status",previous);
            event.put("new_status",status); event.put("changed_at",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.getDefault()).format(new Date()));
            database.insert("incident_status_history",null,event);
            database.setTransactionSuccessful();
        } finally { database.endTransaction(); }
    }
}
