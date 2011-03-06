package com.bryanestrada.waitforit.data;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/****
 * Simple database accessor for transit routes. Defines basic read operations 
 * for individual routes and stops as well as the ability to enumerate over 
 * several routes/stops based on the query.
 * 
 * @author <a href="mailto:bryan@bryanestrada.com">Bryan Estrada</a>
 */
public class DataAccessor extends SQLiteOpenHelper
{
    private static final String TAG = "DataAccessor";
    
    private static final String DATABASE_PATH = "/data/data/com.bryanestrada.waitforit/databases/";
    private static final String DATABASE_NAME = "muni.db";
    private static final int DATABASE_VERSION = 1;
    
    private SQLiteDatabase _db;
    private final Context _context;
    
    public DataAccessor(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        _context = context;
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) { /* no-op */ }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }
    
    /**
     * make sure to invoke this the first time you use a data accessor.
     */
    public void createDatabase() throws IOException
    {
        // check to see if the database already exists. if it doesn't, then we 
        // need to make one by copying it from our assets
        if (!checkDatabase())
        {
            // by calling this method and empty database will be created into 
            // the default system path of our application so we are going to be   
            // able to overwrite that database with ours
            getReadableDatabase();
            copyDatabase();
        }
    }
    
    private boolean checkDatabase()
    {
        SQLiteDatabase checkDb = null;
        boolean result = false;
        try
        {
            String path = DATABASE_PATH + DATABASE_NAME;
            checkDb = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READWRITE);
        }
        catch (SQLiteException e)
        {
            Log.i(TAG, "database not found there", e);
        }
        finally
        {
            if (null != checkDb)
            {
                checkDb.close();
                result = true;
            }
        }
        return result;
    }
    
    /**
     * Copy the bytes from the local assets folder to a usable database.
     */
    private void copyDatabase() throws IOException
    {
        InputStream instream = null;
        OutputStream outstream = null;
        try
        {
            instream = _context.getAssets().open(DATABASE_NAME);
            outstream = new FileOutputStream(DATABASE_PATH + DATABASE_NAME);
            
            byte[] buffer = new byte[1024];
            int length = 0;
            while ((length = instream.read(buffer))  > 0)
            {
                outstream.write(buffer, 0, length);
            }
            Log.d(TAG, String.format("finished copying %d bytes of database", length));
        }
        finally
        {
            if (null != instream)
            {
                instream.close();
            }
            if (null != outstream)
            {
                outstream.flush();
                outstream.close();
            }
        }
    }
    
    public void open()
    {
        String path = DATABASE_PATH + DATABASE_NAME;
        _db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY);
    }
    
    @Override
    public synchronized void close()
    {
        if (null != _db)
            _db.close();
    }
    
    /**
     * SQL query to enumerate over the routes.
     */
    public final Cursor getAllRoutes()
    {
        return _db.query("routes", new String[] {"_id","tag","title", "color", "oppositecolor"}, null, null, null, null, null);
    }
    
    public final Cursor getDirections(long routeId)
    {
       return _db.query("directions", new String[] {"_id","tag","title"}, "route_id=?", new String[] {Long.toString(routeId)}, null, null, null);
    }
    
    public final Cursor getStops(long directionId)
    {
       return _db.query("stops INNER JOIN trips", new String[]{"stops._id","stops.tag","stops.title","stops.lat","stops.lon"}, "trips.direction_id=? AND stops._id=trips.stop_id", new String[] {Long.toString(directionId)}, null, null, null);
    }
    /**
     * SQL query to find all directions that are associated with a given route 
     * based on the route's _id.

    private static final String DIRECTIONS_QUERY = 
        "SELECT directions._id,directions.title FROM directions INNER JOIN routes " +
        "WHERE routes._id=%d AND directions.route_id=routes._id";
    private static final String directionsQuery(long routeId)
    {
        return String.format(DIRECTIONS_QUERY, routeId);
    }
    */
    
    /**
     * SQL query to find all stops that are associated with a given direction 
     * based on the direction's _id.
     
    private static final String STOPS_QUERY = 
        "SELECT stops._id,stops.title FROM stops INNER JOIN trips " +
        "WHERE trips.direction_id=%d AND stops._id=trips.stop_id";
    private static final String stopsQuery(long directionId)
    {
        return String.format(STOPS_QUERY, directionId);
    }
    */
}
