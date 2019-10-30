package com.chi.justdownloader.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.chi.justdownloader.download.DownloadInfo;

/**
 * Created by jxsong on 2019/8/22.
 *
 * @author jxsong
 */

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static final String DB_NAME = "download.db";
    private static final int VERSION = 1;
    private static final String TABLE_NAME = "downloadinfo";
    private static final String ID = "_id";
    private static final String URL = "url";
    private static final String PATH = "path";
    private static final String INDEX = "startIndex";
    private static final String SIZE = "size";
    private static final String RECEIVE = "receive";

    public DatabaseHelper(Context context, String name, CursorFactory factory,
                          int version) {
        super(context, name, factory, version);
    }

    public DatabaseHelper(Context context) {
        this(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate");
        db.execSQL("create table " + TABLE_NAME + "(" + ID
                + " integer primary key autoincrement," + URL + " char(200),"
                + PATH + " char(100)," + INDEX + " integer," + SIZE + " integer," + RECEIVE
                + " integer)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
        Log.i(TAG, "onUpgrade");
    }

    public void insert(String url, String path, int size, int startIndex) {
        synchronized (DatabaseHelper.class) {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(URL, url);
            values.put(PATH, path);
            values.put(SIZE, size);
            values.put(INDEX, startIndex);
            values.put(RECEIVE, 0);
            db.insert(TABLE_NAME, null, values);
            db.close();
        }
    }

    public DownloadInfo getDownloadInfo(String url, String path, int startIndex, int tid) {
        DownloadInfo info = null;
        Log.d(TAG, tid+"---getDownloadInfo: --------------------");
        synchronized (DatabaseHelper.class) {
            Log.d(TAG, tid+"---getDownloadInfo: ++++++++++++++++++++");
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.query(TABLE_NAME, null, URL + "=? and " + PATH
                    + "=? and " + INDEX + "=" + startIndex, new String[]{url, path}, null, null, null);
            Log.d(TAG, tid+"---getDownloadInfo: " + cursor);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    info = new DownloadInfo();
                    info.setUrl(cursor.getString(cursor.getColumnIndex(URL)));
                    info.setPath(cursor.getString(cursor.getColumnIndex(PATH)));
                    info.setStartIndex(cursor.getInt(cursor.getColumnIndex(INDEX)));
                    info.setSize(cursor.getInt(cursor.getColumnIndex(SIZE)));
                    info.setReceive(cursor.getInt(cursor.getColumnIndex(RECEIVE)));
                    break;
                }
                cursor.close();
            }
            db.close();
        }
        return info;
    }

    public void delete(String url, String path, int startIndex) {
        synchronized (DatabaseHelper.class) {
            SQLiteDatabase db = getWritableDatabase();
            db.delete(TABLE_NAME, URL + "=? and " + PATH + "=? and " + INDEX + "=" + startIndex, new String[]{

                    url, path});
            db.close();
        }
    }

    public void updateFileLength(String url, String path, int fileLenght, int startIndex) {
        synchronized (DatabaseHelper.class) {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(SIZE, fileLenght);
            db.update(TABLE_NAME, cv, URL + "=? and " + PATH + "=? and " + INDEX + "=" + startIndex, new String[]{
                    url, path});
            db.close();
        }
    }

    public void updateReceive(String url, String path, int receive, int startIndex) {
        synchronized (DatabaseHelper.class) {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(RECEIVE, receive);
            db.update(TABLE_NAME, cv, URL + "=? and " + PATH + "=? and " + INDEX + "=" + startIndex, new String[]{
                    url, path});
            db.close();
        }
    }

    /*public void update(String url, String path, int size, int receive) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(SIZE, size);
        cv.put(RECEIVE, receive);
        db.update(TABLE_NAME, cv, URL + "=? and " + PATH + "=?", new String[]{
                url, path});
        db.close();
    }*/
}
