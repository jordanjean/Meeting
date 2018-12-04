package com.example.jordan.meeting.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper  extends SQLiteOpenHelper {
    // Version number to upgrade database version
    private static final int DATABASE_VERSION = 10;

    // Database Name
    private static final String DATABASE_NAME = "meeting.db";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //All necessary tables you like to create will create here

        String CREATE_TABLE_MEETING = "CREATE TABLE " + Meeting.TABLE  + "("
                + Meeting.KEY_ID  + " INTEGER PRIMARY KEY AUTOINCREMENT ,"
                + Meeting.KEY_name + " TEXT, "
                + Meeting.KEY_date + " TEXT, "
                + Meeting.KEY_notes + " TEXT, "
                + Meeting.KEY_location + " TEXT, "
                + Meeting.KEY_time + " TEXT )";

        db.execSQL(CREATE_TABLE_MEETING);

        String CREATE_TABLE_ATTENDEE = "CREATE TABLE " + Attendee.TABLE  + "("
                + Attendee.KEY_ID  + " INTEGER PRIMARY KEY AUTOINCREMENT ,"
                + Attendee.KEY_name + " TEXT )";

        db.execSQL(CREATE_TABLE_ATTENDEE);

        /* Table of association between attendees and meetings (Multi-Multi database relation) */
        String CREATE_TABLE_ATTENDTO = "CREATE TABLE " + AttendTo.TABLE  + "("
                + AttendTo.KEY_attendee  + " INTEGER, "
                + AttendTo.KEY_meeting + " INTEGER )";

        db.execSQL(CREATE_TABLE_ATTENDTO);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed, all data will be gone!!!
        db.execSQL("DROP TABLE IF EXISTS " + Meeting.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + Attendee.TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + AttendTo.TABLE);

        // Create tables again
        onCreate(db);
    }
}
