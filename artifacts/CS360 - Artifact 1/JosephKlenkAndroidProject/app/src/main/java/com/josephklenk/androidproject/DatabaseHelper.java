package com.josephklenk.androidproject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "WeightTracker.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_USERS = "users";
    private static final String TABLE_WEIGHTS = "weights";
    private static final String TABLE_GOALS = "goals";

    private static final String KEY_ID = "id";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_WEIGHT = "weight";
    private static final String KEY_DATE = "date";
    private static final String KEY_GOAL_WEIGHT = "goal_weight";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_USERNAME + " TEXT UNIQUE,"
                + KEY_PASSWORD + " TEXT" + ")";

        String CREATE_WEIGHTS_TABLE = "CREATE TABLE " + TABLE_WEIGHTS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_USER_ID + " INTEGER,"
                + KEY_WEIGHT + " REAL,"
                + KEY_DATE + " TEXT,"
                + "FOREIGN KEY(" + KEY_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + KEY_ID + "))";

        String CREATE_GOALS_TABLE = "CREATE TABLE " + TABLE_GOALS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_USER_ID + " INTEGER UNIQUE,"
                + KEY_GOAL_WEIGHT + " REAL,"
                + "FOREIGN KEY(" + KEY_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + KEY_ID + "))";

        db.execSQL(CREATE_USERS_TABLE);
        db.execSQL(CREATE_WEIGHTS_TABLE);
        db.execSQL(CREATE_GOALS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GOALS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEIGHTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    public long addUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_USERNAME, username);
        values.put(KEY_PASSWORD, password);
        return db.insert(TABLE_USERS, null, values);
    }

    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{KEY_ID},
                KEY_USERNAME + "=? AND " + KEY_PASSWORD + "=?",
                new String[]{username, password}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public long getUserId(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{KEY_ID},
                KEY_USERNAME + "=?", new String[]{username}, null, null, null);
        long id = -1;
        if (cursor.moveToFirst()) {
            id = cursor.getLong(0);
        }
        cursor.close();
        return id;
    }

    public long addWeight(long userId, double weight, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_USER_ID, userId);
        values.put(KEY_WEIGHT, weight);
        values.put(KEY_DATE, date);
        return db.insert(TABLE_WEIGHTS, null, values);
    }

    public Cursor getWeights(long userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {KEY_ID, KEY_WEIGHT, KEY_DATE};
        String selection = KEY_USER_ID + "=?";
        String[] selectionArgs = {String.valueOf(userId)};
        String orderBy = KEY_ID + " DESC"; // Order by ID for most recent first
        return db.query(TABLE_WEIGHTS, columns, selection, selectionArgs, null, null, orderBy);
    }

    public double getLatestWeight(long userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + KEY_WEIGHT + " FROM " + TABLE_WEIGHTS +
                " WHERE " + KEY_USER_ID + "=?" +
                " ORDER BY " + KEY_ID + " DESC LIMIT 1"; // Ensure most recent
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        double weight = -1;
        if (cursor != null && cursor.moveToFirst()) {
            weight = cursor.getDouble(0);
            cursor.close();
        }
        return weight;
    }

    public long setGoalWeight(long userId, double weight) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_USER_ID, userId);
        values.put(KEY_GOAL_WEIGHT, weight);
        return db.insertWithOnConflict(TABLE_GOALS, null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public double getGoalWeight(long userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_GOALS, new String[]{KEY_GOAL_WEIGHT},
                KEY_USER_ID + "=?", new String[]{String.valueOf(userId)},
                null, null, null);
        double weight = -1;
        if (cursor.moveToFirst()) {
            weight = cursor.getDouble(0);
        }
        cursor.close();
        return weight;
    }
    public void deleteWeight(long weightId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_WEIGHTS, KEY_ID + "=?", new String[]{String.valueOf(weightId)});
    }
}