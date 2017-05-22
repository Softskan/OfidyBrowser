package com.ofidy.ofidybrowser.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.gson.Gson;
import com.ofidy.ofidybrowser.model.Address;
import com.ofidy.ofidybrowser.model.Region;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OfidyDB {

	private static final String PRODUCTS = "products";
    private static final String TRANSACTIONS = "transactions";
    private static final String REGIONS = "regions";
    private static final String CURRENCIES = "currencies";
	private static final String ADDRESSES = "addresses";
    private static final String CATEGORIES = "categories";
    private static final String SUB_CATEGORIES = "sub_categories";
    private static final String IMAGES = "images";

	private static NobsDBOpenHelper tdHelper;
    private static OfidyDB instance;
    private static SQLiteDatabase db=null;
    static final String C_ROWID = "_id";

    static final String DB_NAME = "Oversabi";
	static final int DB_VERSION = 2;
	private Gson gson = new Gson();

    OfidyDB(Context context) {
		tdHelper = new NobsDBOpenHelper(context, DB_NAME, null, DB_VERSION);
	}
    
    public static OfidyDB getInstance(Context con) {
    	if (instance == null) {
            instance = new OfidyDB(con);
        }
        if (db==null) {
            db= tdHelper.getWritableDatabase();
        }
        return instance;
    }
    
    private static class NobsDBOpenHelper extends SQLiteOpenHelper {
        static final String CREATE_TABLE = "CREATE TABLE ";

		public NobsDBOpenHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_TABLE + PRODUCTS + " (" +
                    C_ROWID + " integer PRIMARY KEY autoincrement," +
                    "id integer ," +
                    "title text ," +
                    "date long ," +
                    "body text ," +
                    "category string ,"+
                    "UNIQUE (id)" +
                    ");"
            );
            db.execSQL(CREATE_TABLE + TRANSACTIONS + " (" +
                    C_ROWID + " integer PRIMARY KEY autoincrement," +
                    "id integer ," +
                    "body text ," +
                    "date long ," +
                    "UNIQUE (id)" +
                    ");"
            );
			db.execSQL(CREATE_TABLE + ADDRESSES + " (" +
            		C_ROWID + " integer PRIMARY KEY autoincrement," +
                    "id integer ," +
                    "body text ," +
                    "date long ," +
                    "UNIQUE (id)" +
                    ");"
            );
            db.execSQL(CREATE_TABLE + REGIONS + " (" +
                    C_ROWID + " integer PRIMARY KEY autoincrement," +
                    "id integer ," +
                    "name text ," +
                    "UNIQUE (id)" +
                    ");"
            );
            db.execSQL(CREATE_TABLE + CURRENCIES + " (" +
                    C_ROWID + " integer PRIMARY KEY autoincrement," +
                    "id integer ," +
                    "name text ," +
                    "code text ," +
                    "UNIQUE (id)" +
                    ");"
            );
            db.execSQL(CREATE_TABLE + IMAGES + " (" +
                    C_ROWID + " integer PRIMARY KEY autoincrement," +
                    "id integer ," +
                    "url text ," +
                    "UNIQUE (id)" +
                    ");"
            );
            db.execSQL(CREATE_TABLE + CATEGORIES + " (" +
                    C_ROWID + " integer PRIMARY KEY autoincrement," +
                    "id integer ," +
                    "name text ," +
                    "desc text ," +
                    "logo text ," +
                    "time long ," +
                    "UNIQUE (id)" +
                    ");"
            );
            db.execSQL(CREATE_TABLE + SUB_CATEGORIES + " (" +
                    C_ROWID + " integer PRIMARY KEY autoincrement," +
                    "id integer ," +
                    "groupId integer ," +
                    "name text ," +
                    "desc text ," +
                    "logo text ," +
                    "time long ," +
                    "UNIQUE (id)" +
                    ");"
            );
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int arg1, int arg2) {
            db.execSQL(CREATE_TABLE + IMAGES + " (" +
                    C_ROWID + " integer PRIMARY KEY autoincrement," +
                    "id integer ," +
                    "url text ," +
                    "UNIQUE (id)" +
                    ");"
            );
		}
    }

    public boolean emptyTransactions() {
        return db.delete(TRANSACTIONS, null, null) == 1;
    }

    public void insertAddress(Address address) {
        ContentValues values = new ContentValues();
        values.put("id", address.getId());
        values.put("date", System.currentTimeMillis());
        values.put("body", gson.toJson(address));
        try {
            db.insertOrThrow(ADDRESSES, null, values);
        } catch (SQLException e) {

        }
    }

    public boolean emptyAddress() {
        return db.delete(ADDRESSES, null, null) == 1;
    }

    public ArrayList<Address> getAddresses() {
        ArrayList<Address> all = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + ADDRESSES +" ORDER BY date";
        Cursor mCursor = db.rawQuery ( selectQuery, null );
        if (mCursor != null) {
            if(mCursor.moveToFirst()) {
                do {
                    String date = mCursor.getString(mCursor.getColumnIndex("body"));
                    all.add(gson.fromJson(date, Address.class));
                }
                while (mCursor.moveToNext());
            }
        }
        mCursor.close();
        Collections.reverse(all);
        return all;
    }

    public String getSubCategory(long id) {
        Cursor c;
        c = db.query(SUB_CATEGORIES, new String[]{"name"}, "id = ?",
                new String[] { String.valueOf(id)}, null, null, null);
        if (c.getCount()>0) {
            c.moveToFirst();
            return c.getString(0);
        }
        c.close();
        return null;
    }

    public void emptyCategories() {
        db.delete(CATEGORIES, null, null);
        db.delete(SUB_CATEGORIES, null, null);
    }

    public void emptyCurrencies() {
        db.delete(CURRENCIES, null, null);
    }

    public void emptyImages() {
        db.delete(IMAGES, null, null);
    }

    public void emptyRegions() {
        db.delete(REGIONS, null, null);
    }

    public void insertRegion(Region region) {
        ContentValues values = new ContentValues();
        values.put("id", region.getId());
        values.put("name", region.getName());
        try {
            db.insertOrThrow(REGIONS, null, values);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Region> getRegions() {
        ArrayList<Region> all = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + REGIONS ;
        Cursor mCursor = db.rawQuery ( selectQuery, null );
        if (mCursor != null) {
            if(mCursor.moveToFirst()) {
                do {
                    String id = mCursor.getString(mCursor.getColumnIndex("id"));
                    String name = mCursor.getString(mCursor.getColumnIndex("name"));
                    Region region = new Region(id, name);
                    all.add(region);
                }
                while (mCursor.moveToNext());
            }
        }
        mCursor.close();
        Collections.reverse(all);
        return all;
    }

}
