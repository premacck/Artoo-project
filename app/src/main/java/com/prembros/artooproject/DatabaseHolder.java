package com.prembros.artooproject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import java.util.ArrayList;

/**
 *
 * Created by Prem $ on 9/12/2017.
 */

public class DatabaseHolder {
    private static final String database_name = "ArtooProject";
    private static final int database_version = 1;

    private static final String routesTable = "Routes";
    private static final String routeColumn = "route";

    private static final String create_table_routes = "create table if not exists " + routesTable + " (" + routeColumn +" text not null);";

    private static DatabaseHelper dbHelper;
    private final Context context;
    private SQLiteDatabase db;

    public DatabaseHolder(Context context) {
        this.context = context;
        if (dbHelper == null) {
            dbHelper = new DatabaseHelper(context);
        }
    }

    public void open() {
        db  = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    /**
    *INSERTION / REMOVAL METHODS
    */
    public void insertInRoutesTable(final String url){
        new Thread(new Runnable() {
            @Override
            public void run() {
                ContentValues content = new ContentValues();
                content.put(routeColumn, url);
                db.insertOrThrow(routesTable, null, content);
            }
        });
    }

    /**
    * RETRIEVAL METHODS
    */
    public ArrayList<String> returnRoutes(){
        Cursor cursor = null;

        try{
            cursor = db.query(true, routesTable, new String[]{routeColumn}, null, null, null, null, null, null);
        }
        catch (SQLiteException e){
            if (e.getMessage().contains("no such table")){
                Toast.makeText(context, "ERROR: Routes table doesn't exist!", Toast.LENGTH_SHORT).show();
            } else e.printStackTrace();
        }

        ArrayList<String> list = new ArrayList<>();

        if (cursor != null) {
            cursor.moveToFirst();
            if (cursor.getColumnCount() > 0) {
                while (!cursor.isAfterLast()) {
                    list.add(cursor.getString(cursor.getColumnIndex(routeColumn)));
                    cursor.moveToNext();
                }
            }
        cursor.close();
        }

        return list;
    }

    /**
     * RESET TABLES METHOD
     */
    void resetTables(){
        open();
        db.execSQL("DROP TABLE IF EXISTS " + routesTable);
        try{
            db.execSQL(create_table_routes);
        } catch(SQLException e) {
            e.printStackTrace();
        }
        close();
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, database_name, null, database_version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            try{
                db.execSQL(create_table_routes);
            } catch(SQLException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (newVersion > oldVersion) {
                db.execSQL("DROP TABLE IF EXISTS " + routesTable);
                onCreate(db);
            }
        }
    }
}