package com.memorymate.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


import com.memorymate.models.Person;
import com.memorymate.models.Reminder;
import com.memorymate.models.SafeZone;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "memorymate.db";
    private static final int DATABASE_VERSION = 3; // Increased version for Safe Zone

    // People table
    private static final String TABLE_PEOPLE = "people";
    private static final String COLUMN_PEOPLE_ID = "id";
    private static final String COLUMN_PEOPLE_NAME = "name";
    private static final String COLUMN_PEOPLE_PHOTO = "photo_path";
    private static final String COLUMN_PEOPLE_RELATIONSHIP = "relationship";
    private static final String COLUMN_PEOPLE_PHONE = "phone_number";
    private static final String COLUMN_PEOPLE_VOICE = "voice_note_path";

    // Reminders table
    private static final String TABLE_REMINDERS = "reminders";
    private static final String COLUMN_REMINDER_ID = "id";
    private static final String COLUMN_REMINDER_TYPE = "type";
    private static final String COLUMN_REMINDER_TITLE = "title";
    private static final String COLUMN_REMINDER_DESC = "description";
    private static final String COLUMN_REMINDER_TIMESTAMP = "timestamp";
    private static final String COLUMN_REMINDER_RECURRING = "is_recurring";
    private static final String COLUMN_REMINDER_RECURRENCE_TYPE = "recurrence_type";
    private static final String COLUMN_REMINDER_VIBRATION = "vibration_enabled";
    private static final String COLUMN_REMINDER_VOICE = "voice_enabled";
    private static final String COLUMN_REMINDER_ACTIVE = "is_active";

    // Activity Log table
    private static final String TABLE_ACTIVITY_LOG = "activity_log";
    private static final String COLUMN_LOG_ID = "id";
    private static final String COLUMN_LOG_TYPE = "type";
    private static final String COLUMN_LOG_ACTION = "action";
    private static final String COLUMN_LOG_TITLE = "title";
    private static final String COLUMN_LOG_DETAILS = "details";
    private static final String COLUMN_LOG_TIMESTAMP = "timestamp";
    private static final String COLUMN_LOG_USER_ID = "user_id";

    // Safe Zone table
    private static final String TABLE_SAFE_ZONES = "safe_zones";
    private static final String COLUMN_ZONE_ID = "id";
    private static final String COLUMN_ZONE_NAME = "name";
    private static final String COLUMN_ZONE_LAT = "latitude";
    private static final String COLUMN_ZONE_LNG = "longitude";
    private static final String COLUMN_ZONE_RADIUS = "radius";
    private static final String COLUMN_ZONE_ACTIVE = "is_active";
    private static final String COLUMN_ZONE_USER_ID = "user_id";

    // Create table SQL
    private static final String CREATE_TABLE_PEOPLE =
            "CREATE TABLE " + TABLE_PEOPLE + "(" +
                    COLUMN_PEOPLE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_PEOPLE_NAME + " TEXT," +
                    COLUMN_PEOPLE_PHOTO + " TEXT," +
                    COLUMN_PEOPLE_RELATIONSHIP + " TEXT," +
                    COLUMN_PEOPLE_PHONE + " TEXT," +
                    COLUMN_PEOPLE_VOICE + " TEXT" + ")";

    private static final String CREATE_TABLE_REMINDERS =
            "CREATE TABLE " + TABLE_REMINDERS + "(" +
                    COLUMN_REMINDER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_REMINDER_TYPE + " TEXT," +
                    COLUMN_REMINDER_TITLE + " TEXT," +
                    COLUMN_REMINDER_DESC + " TEXT," +
                    COLUMN_REMINDER_TIMESTAMP + " INTEGER," +
                    COLUMN_REMINDER_RECURRING + " INTEGER," +
                    COLUMN_REMINDER_RECURRENCE_TYPE + " INTEGER," +
                    COLUMN_REMINDER_VIBRATION + " INTEGER," +
                    COLUMN_REMINDER_VOICE + " INTEGER," +
                    COLUMN_REMINDER_ACTIVE + " INTEGER DEFAULT 1" + ")";

    private static final String CREATE_TABLE_ACTIVITY_LOG =
            "CREATE TABLE " + TABLE_ACTIVITY_LOG + "(" +
                    COLUMN_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_LOG_TYPE + " TEXT," +
                    COLUMN_LOG_ACTION + " TEXT," +
                    COLUMN_LOG_TITLE + " TEXT," +
                    COLUMN_LOG_DETAILS + " TEXT," +
                    COLUMN_LOG_TIMESTAMP + " INTEGER," +
                    COLUMN_LOG_USER_ID + " TEXT" + ")";

    private static final String CREATE_TABLE_SAFE_ZONES =
            "CREATE TABLE " + TABLE_SAFE_ZONES + "(" +
                    COLUMN_ZONE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_ZONE_NAME + " TEXT," +
                    COLUMN_ZONE_LAT + " REAL," +
                    COLUMN_ZONE_LNG + " REAL," +
                    COLUMN_ZONE_RADIUS + " REAL," +
                    COLUMN_ZONE_ACTIVE + " INTEGER DEFAULT 1," +
                    COLUMN_ZONE_USER_ID + " TEXT" + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_PEOPLE);
        db.execSQL(CREATE_TABLE_REMINDERS);
        db.execSQL(CREATE_TABLE_ACTIVITY_LOG);
        db.execSQL(CREATE_TABLE_SAFE_ZONES);  // Add Safe Zone table
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PEOPLE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REMINDERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ACTIVITY_LOG);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SAFE_ZONES);
        onCreate(db);
    }

    // ==================== PEOPLE CRUD OPERATIONS ====================

    public long addPerson(Person person) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PEOPLE_NAME, person.getName());
        values.put(COLUMN_PEOPLE_PHOTO, person.getPhotoPath());
        values.put(COLUMN_PEOPLE_RELATIONSHIP, person.getRelationship());
        values.put(COLUMN_PEOPLE_PHONE, person.getPhoneNumber());
        values.put(COLUMN_PEOPLE_VOICE, person.getVoiceNotePath());

        long id = db.insert(TABLE_PEOPLE, null, values);
        db.close();
        return id;
    }

    public List<Person> getAllPeople() {
        List<Person> people = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_PEOPLE + " ORDER BY " + COLUMN_PEOPLE_NAME + " ASC";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Person person = new Person();
                person.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PEOPLE_ID)));
                person.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PEOPLE_NAME)));
                person.setPhotoPath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PEOPLE_PHOTO)));
                person.setRelationship(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PEOPLE_RELATIONSHIP)));
                person.setPhoneNumber(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PEOPLE_PHONE)));
                person.setVoiceNotePath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PEOPLE_VOICE)));
                people.add(person);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return people;
    }

    public void deletePerson(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PEOPLE, COLUMN_PEOPLE_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        Log.d("DatabaseHelper", "Person deleted: " + id);
    }

    public void deleteAllPeople() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PEOPLE, null, null);
        db.close();
        Log.d("DatabaseHelper", "All people deleted");
    }

    // ==================== REMINDERS CRUD OPERATIONS ====================

    public long addReminder(Reminder reminder) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_REMINDER_TYPE, reminder.getType());
        values.put(COLUMN_REMINDER_TITLE, reminder.getTitle());
        values.put(COLUMN_REMINDER_DESC, reminder.getDescription());
        values.put(COLUMN_REMINDER_TIMESTAMP, reminder.getTimestamp());
        values.put(COLUMN_REMINDER_RECURRING, reminder.isRecurring() ? 1 : 0);
        values.put(COLUMN_REMINDER_RECURRENCE_TYPE, reminder.getRecurrenceType());
        values.put(COLUMN_REMINDER_VIBRATION, reminder.isVibrationEnabled() ? 1 : 0);
        values.put(COLUMN_REMINDER_VOICE, reminder.isVoiceEnabled() ? 1 : 0);
        values.put(COLUMN_REMINDER_ACTIVE, reminder.isActive() ? 1 : 0);

        long id = db.insert(TABLE_REMINDERS, null, values);
        db.close();
        return id;
    }

    public List<Reminder> getActiveReminders() {
        List<Reminder> reminders = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_REMINDERS +
                " WHERE " + COLUMN_REMINDER_ACTIVE + " = 1 ORDER BY " + COLUMN_REMINDER_TIMESTAMP + " ASC";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Reminder reminder = new Reminder();
                reminder.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_ID)));
                reminder.setType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_TYPE)));
                reminder.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_TITLE)));
                reminder.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_DESC)));
                reminder.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_TIMESTAMP)));
                reminder.setRecurring(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_RECURRING)) == 1);
                reminder.setRecurrenceType(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_RECURRENCE_TYPE)));
                reminder.setVibrationEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_VIBRATION)) == 1);
                reminder.setVoiceEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_VOICE)) == 1);
                reminder.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_ACTIVE)) == 1);
                reminders.add(reminder);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return reminders;
    }

    public void updateReminderActiveStatus(int id, boolean isActive) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_REMINDER_ACTIVE, isActive ? 1 : 0);
        db.update(TABLE_REMINDERS, values, COLUMN_REMINDER_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteReminder(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_REMINDERS, COLUMN_REMINDER_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        Log.d("DatabaseHelper", "Reminder deleted: " + id);
    }

    public void deleteAllReminders() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_REMINDERS, null, null);
        db.close();
        Log.d("DatabaseHelper", "All reminders deleted");
    }

    // ==================== ACTIVITY LOG CRUD OPERATIONS ====================



    // ==================== SAFE ZONE CRUD OPERATIONS ====================

    public long addSafeZone(SafeZone zone) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ZONE_NAME, zone.getName());
        values.put(COLUMN_ZONE_LAT, zone.getLatitude());
        values.put(COLUMN_ZONE_LNG, zone.getLongitude());
        values.put(COLUMN_ZONE_RADIUS, zone.getRadius());
        values.put(COLUMN_ZONE_ACTIVE, zone.isActive() ? 1 : 0);
        values.put(COLUMN_ZONE_USER_ID, zone.getUserId());

        long id = db.insert(TABLE_SAFE_ZONES, null, values);
        db.close();
        return id;
    }

    public List<SafeZone> getAllSafeZones() {
        List<SafeZone> zones = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_SAFE_ZONES + " WHERE " + COLUMN_ZONE_ACTIVE + " = 1";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                SafeZone zone = new SafeZone();
                zone.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ZONE_ID)));
                zone.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ZONE_NAME)));
                zone.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_ZONE_LAT)));
                zone.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_ZONE_LNG)));
                zone.setRadius(cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_ZONE_RADIUS)));
                zone.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ZONE_ACTIVE)) == 1);
                zone.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ZONE_USER_ID)));
                zones.add(zone);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return zones;
    }

    public void deleteSafeZone(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SAFE_ZONES, COLUMN_ZONE_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        Log.d("DatabaseHelper", "Safe zone deleted: " + id);
    }
}