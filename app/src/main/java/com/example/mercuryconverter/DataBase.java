package com.example.mercuryconverter;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.view.SurfaceControl;

public class DataBase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "mercury_history.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_NAME = "history";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "youtube_title";
    private static final String COLUMN_URL = "youtube_url";
    private static final String COLUMN_PATH = "local_path";
    private static final String COLUMN_DATE = "download_date";

    public DataBase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable= "CREATE TABLE " + TABLE_NAME + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TITLE + " TEXT,"
                + COLUMN_URL + " TEXT,"
                + COLUMN_PATH + " TEXT,"
                + COLUMN_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP)";
        db.execSQL(createTable);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
    public boolean addDownload(String title, String url, String path) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_TITLE,title);
        values.put(COLUMN_URL,url);
        values.put(COLUMN_PATH,path);

        long result = db.insert(TABLE_NAME, null, values);
        db.close();
        return result != -1;
    }

    public android.database.Cursor getAllHistory() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_DATE + " DESC";
        android.database.Cursor cursor = null;
        if(db != null){
            cursor = db.rawQuery(query,null);
        }
        return cursor;
    }

}
