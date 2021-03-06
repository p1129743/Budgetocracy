package projet.ift2905.budgetocracy;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper_Expenses extends SQLiteOpenHelper {

    public static final String DATABASE_NAME_EXPENSES = "expenses.db";
    public static final String TABLE_NAME = "expenses_table";
    public static final String COL_EXPENSES_0 = "_id";
    public static final String COL_EXPENSES_1 = "NAME";
    public static final String COL_EXPENSES_2 = "CATEGORY_ID";
    public static final String COL_EXPENSES_3 = "AMOUNT";
    public static final String COL_EXPENSES_4 = "DATE";


    public DBHelper_Expenses(Context context) {
        super(context, DATABASE_NAME_EXPENSES, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_NAME + " ("
                + COL_EXPENSES_0 + " INTEGER PRIMARY KEY AUTOINCREMENT  , "
                + COL_EXPENSES_1 + " TEXT , "
                + COL_EXPENSES_2 + " INTEGER , "
                + COL_EXPENSES_3 + " FLOAT , "
                + COL_EXPENSES_4 + " TEXT ) ";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
        db.execSQL(sql);
        onCreate(db);
    }

    /**** EXEMPLE INSERT  *****/
    public boolean insertDataName(String name, Integer category, float amount, String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_EXPENSES_1, name);
        contentValues.put(COL_EXPENSES_2, category);
        contentValues.put(COL_EXPENSES_3, amount);
        contentValues.put(COL_EXPENSES_4, date);
        long result = db.insert(TABLE_NAME, null, contentValues);
        if (result == -1) return false;
        return true;
    }

    public Cursor getAllData() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("select * from " + TABLE_NAME, null);
    }

    public Boolean updateData(String id, String name, Integer categoryID, float amount, String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_EXPENSES_0, id);
        contentValues.put(COL_EXPENSES_1, name);
        contentValues.put(COL_EXPENSES_2, categoryID);
        contentValues.put(COL_EXPENSES_3, amount);
        contentValues.put(COL_EXPENSES_4, date);
        db.update(TABLE_NAME, contentValues, "_id = ?", new String[]{id});
        return true;
    }

    public Integer deleteData(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME, "_id = ?", new String[]{id});
    }

    public void deleteDataBase() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME); //delete all rows in a table
        db.execSQL("UPDATE SQLITE_SEQUENCE SET SEQ=0 WHERE NAME='expenses_table'"); //reset the primary keys
    }

    public Cursor getExpenseListByKeyword(String search, EnumSort sort) {
        //Open connection to read only
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "";

        switch (sort.getSort()) {
            case sortByDate:
                selectQuery = "SELECT  rowid as " +
                        DBHelper_Expenses.COL_EXPENSES_0 + "," +
                        DBHelper_Expenses.COL_EXPENSES_1 + "," +
                        DBHelper_Expenses.COL_EXPENSES_2 + "," +
                        DBHelper_Expenses.COL_EXPENSES_3 + "," +
                        DBHelper_Expenses.COL_EXPENSES_4 +
                        " FROM " + DBHelper_Expenses.TABLE_NAME +
                        " WHERE " + DBHelper_Expenses.COL_EXPENSES_1 + "  LIKE  '%" + search + "%' " +
                        " ORDER BY DATE(" + DBHelper_Expenses.COL_EXPENSES_4 + ")"
                ;

                break;
            case sortByAmount:
                selectQuery = "SELECT  rowid as " +
                        DBHelper_Expenses.COL_EXPENSES_0 + "," +
                        DBHelper_Expenses.COL_EXPENSES_1 + "," +
                        DBHelper_Expenses.COL_EXPENSES_2 + "," +
                        DBHelper_Expenses.COL_EXPENSES_3 + "," +
                        DBHelper_Expenses.COL_EXPENSES_4 +
                        " FROM " + DBHelper_Expenses.TABLE_NAME +
                        " WHERE " + DBHelper_Expenses.COL_EXPENSES_1 + "  LIKE  '%" + search + "%' " +
                        " ORDER BY " + DBHelper_Expenses.COL_EXPENSES_3
                ;

                break;

            case sortByName:
                selectQuery = "SELECT  rowid as " +
                        DBHelper_Expenses.COL_EXPENSES_0 + "," +
                        DBHelper_Expenses.COL_EXPENSES_1 + "," +
                        DBHelper_Expenses.COL_EXPENSES_2 + "," +
                        DBHelper_Expenses.COL_EXPENSES_3 + "," +
                        DBHelper_Expenses.COL_EXPENSES_4 +
                        " FROM " + DBHelper_Expenses.TABLE_NAME +
                        " WHERE " + DBHelper_Expenses.COL_EXPENSES_1 + "  LIKE  '%" + search + "%' " +
                        " ORDER BY " + DBHelper_Expenses.COL_EXPENSES_1
                ;
                break;

            default:
                break;
        }

        Cursor cursor = db.rawQuery(selectQuery, null);
        // looping through all rows and adding to list
        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;
    }


    public String getNameExpenseWithID(String id) {
        //Open connection to read only
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT " +
                DBHelper_Expenses.COL_EXPENSES_1 +
                " FROM " + DBHelper_Expenses.TABLE_NAME +
                " WHERE _id = " + id;
        //return db.execSQL(selectQuery);
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }

        return cursor.getString(0);

    }

    public Cursor getExpensesAssociateToBudget(String idBudget) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + DBHelper_Expenses.TABLE_NAME +
                " WHERE " + DBHelper_Expenses.COL_EXPENSES_2 + "=" + idBudget;

        Cursor cursor = db.rawQuery(selectQuery, null);
        // looping through all rows and adding to list

        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;
    }


    public Cursor getExpense(String id) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + DBHelper_Expenses.TABLE_NAME +
                " WHERE " + DBHelper_Expenses.COL_EXPENSES_0 + "=" + id;

        Cursor cursor = db.rawQuery(selectQuery, null);
        // looping through all rows and adding to list

        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;
    }

    public void deleteExpensesOfBudget(String budgetId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME + " WHERE " + COL_EXPENSES_2 + "=" + budgetId);
    }
}