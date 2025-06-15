package com.josephklenk.androidproject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "WeightTracker.db";
    private static final int DATABASE_VERSION = 2; // Incremented for schema changes

    // Table names
    private static final String TABLE_USERS = "users";
    private static final String TABLE_WEIGHTS = "weights";
    private static final String TABLE_GOALS = "goals";

    // Column names
    private static final String KEY_ID = "id";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_SALT = "salt";
    private static final String KEY_WEIGHT = "weight";
    private static final String KEY_DATE = "date";
    private static final String KEY_GOAL_WEIGHT = "goal_weight";

    // Security constants
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            // Enhanced users table with salt column for security
            String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                    + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + KEY_USERNAME + " TEXT UNIQUE NOT NULL,"
                    + KEY_PASSWORD + " TEXT NOT NULL,"
                    + KEY_SALT + " TEXT NOT NULL" + ")";

            String CREATE_WEIGHTS_TABLE = "CREATE TABLE " + TABLE_WEIGHTS + "("
                    + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + KEY_USER_ID + " INTEGER NOT NULL,"
                    + KEY_WEIGHT + " REAL NOT NULL,"
                    + KEY_DATE + " TEXT NOT NULL,"
                    + "FOREIGN KEY(" + KEY_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + KEY_ID + "))";

            String CREATE_GOALS_TABLE = "CREATE TABLE " + TABLE_GOALS + "("
                    + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + KEY_USER_ID + " INTEGER UNIQUE NOT NULL,"
                    + KEY_GOAL_WEIGHT + " REAL NOT NULL,"
                    + "FOREIGN KEY(" + KEY_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + KEY_ID + "))";

            db.execSQL(CREATE_USERS_TABLE);
            db.execSQL(CREATE_WEIGHTS_TABLE);
            db.execSQL(CREATE_GOALS_TABLE);

            // Create indexes for better performance
            db.execSQL("CREATE INDEX idx_user_weights ON " + TABLE_WEIGHTS + "(" + KEY_USER_ID + ")");
            db.execSQL("CREATE INDEX idx_weight_date ON " + TABLE_WEIGHTS + "(" + KEY_DATE + ")");

            Log.d(TAG, "Database tables created successfully");
        } catch (SQLiteException e) {
            Log.e(TAG, "Error creating database tables", e);
            throw new RuntimeException("Database creation failed", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        
        try {
            if (oldVersion < 2) {
                // Add salt column to existing users table
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + KEY_SALT + " TEXT DEFAULT ''");
                
                // Generate salts for existing users (if any)
                Cursor cursor = db.query(TABLE_USERS, new String[]{KEY_ID}, null, null, null, null, null);
                while (cursor.moveToNext()) {
                    long userId = cursor.getLong(0);
                    String salt = generateSalt();
                    ContentValues values = new ContentValues();
                    values.put(KEY_SALT, salt);
                    db.update(TABLE_USERS, values, KEY_ID + "=?", new String[]{String.valueOf(userId)});
                }
                cursor.close();
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Error upgrading database", e);
            // If upgrade fails, recreate tables
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_GOALS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEIGHTS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
            onCreate(db);
        }
    }

    /**
     * Adds a new user with secure password hashing
     * @param username User's username
     * @param password Plain text password
     * @return User ID if successful, -1 if failed
     */
    public long addUser(String username, String password) {
        if (!isValidInput(username) || !isValidInput(password)) {
            Log.w(TAG, "Invalid username or password provided");
            return -1;
        }

        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            
            String salt = generateSalt();
            String hashedPassword = hashPassword(password, salt);
            
            ContentValues values = new ContentValues();
            values.put(KEY_USERNAME, username.trim());
            values.put(KEY_PASSWORD, hashedPassword);
            values.put(KEY_SALT, salt);
            
            long result = db.insert(TABLE_USERS, null, values);
            Log.d(TAG, "User added with ID: " + result);
            return result;
            
        } catch (SQLiteException | SecurityException e) {
            Log.e(TAG, "Error adding user: " + username, e);
            return -1;
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    /**
     * Authenticates user with secure password verification
     * @param username User's username
     * @param password Plain text password
     * @return true if authentication successful, false otherwise
     */
    public boolean checkUser(String username, String password) {
        if (!isValidInput(username) || !isValidInput(password)) {
            return false;
        }

        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = this.getReadableDatabase();
            cursor = db.query(TABLE_USERS, 
                new String[]{KEY_ID, KEY_PASSWORD, KEY_SALT},
                KEY_USERNAME + "=?", new String[]{username.trim()}, 
                null, null, null);
                
            if (cursor.moveToFirst()) {
                String storedHash = cursor.getString(1);
                String salt = cursor.getString(2);
                
                // Handle legacy users without salt
                if (salt == null || salt.isEmpty()) {
                    return password.equals(storedHash); // Fallback for old passwords
                }
                
                return verifyPassword(password, storedHash, salt);
            }
            return false;
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error authenticating user: " + username, e);
            return false;
        } finally {
            if (cursor != null) cursor.close();
            if (db != null && db.isOpen()) db.close();
        }
    }

    /**
     * Gets user ID by username
     * @param username Username to look up
     * @return User ID or -1 if not found
     */
    public long getUserId(String username) {
        if (!isValidInput(username)) {
            return -1;
        }

        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = this.getReadableDatabase();
            cursor = db.query(TABLE_USERS, new String[]{KEY_ID},
                    KEY_USERNAME + "=?", new String[]{username.trim()}, 
                    null, null, null);
            
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
            return -1;
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting user ID for: " + username, e);
            return -1;
        } finally {
            if (cursor != null) cursor.close();
            if (db != null && db.isOpen()) db.close();
        }
    }

    /**
     * Adds a weight entry with validation
     * @param userId User ID
     * @param weight Weight value
     * @param date Date string
     * @return Weight entry ID if successful, -1 if failed
     */
    public long addWeight(long userId, double weight, String date) {
        if (!isValidWeight(weight) || !isValidInput(date)) {
            Log.w(TAG, "Invalid weight or date provided");
            return -1;
        }

        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(KEY_USER_ID, userId);
            values.put(KEY_WEIGHT, weight);
            values.put(KEY_DATE, date.trim());
            
            long result = db.insert(TABLE_WEIGHTS, null, values);
            Log.d(TAG, "Weight entry added with ID: " + result);
            return result;
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error adding weight entry for user: " + userId, e);
            return -1;
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    /**
     * Gets weight entries for a user with optional pagination
     * @param userId User ID
     * @return Cursor with weight entries
     */
    public Cursor getWeights(long userId) {
        SQLiteDatabase db = null;
        try {
            db = this.getReadableDatabase();
            String[] columns = {KEY_ID, KEY_WEIGHT, KEY_DATE};
            String selection = KEY_USER_ID + "=?";
            String[] selectionArgs = {String.valueOf(userId)};
            String orderBy = KEY_ID + " DESC LIMIT 50"; // Limit to recent 50 entries for performance
            
            return db.query(TABLE_WEIGHTS, columns, selection, selectionArgs, null, null, orderBy);
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting weights for user: " + userId, e);
            return null;
        }
    }

    /**
     * Gets the latest weight for a user
     * @param userId User ID
     * @return Latest weight or -1 if not found
     */
    public double getLatestWeight(long userId) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = this.getReadableDatabase();
            String query = "SELECT " + KEY_WEIGHT + " FROM " + TABLE_WEIGHTS +
                    " WHERE " + KEY_USER_ID + "=?" +
                    " ORDER BY " + KEY_ID + " DESC LIMIT 1";
            cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getDouble(0);
            }
            return -1;
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting latest weight for user: " + userId, e);
            return -1;
        } finally {
            if (cursor != null) cursor.close();
            if (db != null && db.isOpen()) db.close();
        }
    }

    /**
     * Sets goal weight for a user
     * @param userId User ID
     * @param weight Goal weight
     * @return Entry ID if successful, -1 if failed
     */
    public long setGoalWeight(long userId, double weight) {
        if (!isValidWeight(weight)) {
            Log.w(TAG, "Invalid goal weight provided");
            return -1;
        }

        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(KEY_USER_ID, userId);
            values.put(KEY_GOAL_WEIGHT, weight);
            
            long result = db.insertWithOnConflict(TABLE_GOALS, null, values,
                    SQLiteDatabase.CONFLICT_REPLACE);
            Log.d(TAG, "Goal weight set for user: " + userId);
            return result;
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error setting goal weight for user: " + userId, e);
            return -1;
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    /**
     * Gets goal weight for a user
     * @param userId User ID
     * @return Goal weight or -1 if not found
     */
    public double getGoalWeight(long userId) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = this.getReadableDatabase();
            cursor = db.query(TABLE_GOALS, new String[]{KEY_GOAL_WEIGHT},
                    KEY_USER_ID + "=?", new String[]{String.valueOf(userId)},
                    null, null, null);
            
            if (cursor.moveToFirst()) {
                return cursor.getDouble(0);
            }
            return -1;
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting goal weight for user: " + userId, e);
            return -1;
        } finally {
            if (cursor != null) cursor.close();
            if (db != null && db.isOpen()) db.close();
        }
    }

    /**
     * Safely deletes a weight entry
     * @param weightId Weight entry ID to delete
     */
    public void deleteWeight(long weightId) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            int rowsAffected = db.delete(TABLE_WEIGHTS, KEY_ID + "=?", 
                new String[]{String.valueOf(weightId)});
            
            Log.d(TAG, "Deleted weight entry ID: " + weightId + ", rows affected: " + rowsAffected);
            
        } catch (SQLiteException e) {
            Log.e(TAG, "Error deleting weight entry: " + weightId, e);
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    // SECURITY UTILITY METHODS (Embedded to avoid new files)

    /**
     * Generates a random salt for password hashing
     * @return Base64 encoded salt string
     */
    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hashes a password with the provided salt
     * @param password Plain text password
     * @param salt Salt value for hashing
     * @return Hashed password as Base64 string
     * @throws SecurityException if hashing fails
     */
    private String hashPassword(String password, String salt) throws SecurityException {
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(Base64.getDecoder().decode(salt));
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new SecurityException("Password hashing failed", e);
        }
    }

    /**
     * Verifies a password against a stored hash
     * @param password Plain text password to verify
     * @param storedHash Stored password hash
     * @param salt Salt used for original hash
     * @return true if password matches, false otherwise
     */
    private boolean verifyPassword(String password, String storedHash, String salt) {
        try {
            String hashedInput = hashPassword(password, salt);
            return hashedInput.equals(storedHash);
        } catch (SecurityException e) {
            Log.e(TAG, "Password verification failed", e);
            return false;
        }
    }

    // VALIDATION UTILITY METHODS

    /**
     * Validates input strings
     * @param input Input to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidInput(String input) {
        return input != null && !input.trim().isEmpty();
    }

    /**
     * Validates weight values
     * @param weight Weight to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidWeight(double weight) {
        return weight > 0 && weight <= 1000; // Reasonable weight range
    }
}