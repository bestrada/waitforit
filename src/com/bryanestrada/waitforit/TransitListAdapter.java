package com.bryanestrada.waitforit;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TransitListAdapter extends BaseAdapter
{
	private static final String TAG = "TransitListAdapter";
	
    private final Context _context;
    private final Cursor _cursor;
    
    private final boolean _geoLocateEnabled;
    private final float _latitude;
    private final float _longitude;
    
    public TransitListAdapter(Context context, Cursor cursor)
    {
    	this(context, cursor, 0, 0, false);
    }
    
    public TransitListAdapter(Context context, Cursor cursor, float lat, float lon)
    {
    	this(context, cursor, lat, lon, true);
    }
    
    private TransitListAdapter(Context context, Cursor cursor, float lat, float lon, boolean enableGeoLocate)
    {
    	_context = context;
        _cursor = cursor;
        
        _latitude = lat;
        _longitude = lon;
        _geoLocateEnabled = enableGeoLocate;
    }
    
    public final int getCount() { return _cursor.getCount(); }
    
    public Object getItem(int position)
    {
        String tag = null;
        if (_cursor.getPosition() == position || _cursor.moveToPosition(position))
        {
           tag = _cursor.getString(_cursor.getColumnIndex("tag"));
        }
        return tag;
    }
    
    public long getItemId(int position)
    {
        long result = -1;
        if (_cursor.getPosition() == position || _cursor.moveToPosition(position))
        {
            result = _cursor.getInt(_cursor.getColumnIndex("_id"));
        }
        return result;
    }
    
    public double getLatitude(int position)
    {
    	double result = 0;
    	if (_cursor.getPosition() == position || _cursor.moveToPosition(position))
    	{
    		result = _cursor.getDouble(_cursor.getColumnIndex("lat"));
    	}
    	return result;
    }
    
    public double getLongitude(int position)
    {
    	double result = 0;
    	if (_cursor.getPosition() == position || _cursor.moveToPosition(position))
    	{
    		result = _cursor.getDouble(_cursor.getColumnIndex("lat"));
    	}
    	return result;
    }
    
    public View getView(int position, View convertView, ViewGroup parent)
    {
       LinearLayout result = null;

       if (_cursor.getPosition() == position || _cursor.moveToPosition(position))
       {
          result = null != convertView && convertView instanceof LinearLayout ?  
                (LinearLayout) convertView :
                (LinearLayout) View.inflate(_context, R.layout.list_row, null);

          TextView name = (TextView) result.findViewById(R.id.name);
          String title = _cursor.getString(_cursor.getColumnIndex("title"));
          name.setText(title);
       }
       highlightIfClose(_cursor, result);
       return result;
    }
    
    private void highlightIfClose(Cursor cursor, View view)
    {
    	if (_geoLocateEnabled)
    	{
	    	// first let's see if we can get the lat/lon of this item
	    	int latCol = cursor.getColumnIndex("lat");
	    	int lonCol = cursor.getColumnIndex("lon");
	    	for (int i = 0; i < cursor.getColumnCount(); i++)
	    	{
	    		String name = cursor.getColumnName(i);
	    		Log.i(TAG, name);
	    	}
	    	
	    	float lat = cursor.getFloat(latCol);
	    	float lon = cursor.getFloat(lonCol);
	    	
	    	float delta = (_latitude - lat) + (_longitude - lon);
	    	
	    	// if the delta is within a certain tolerance, then make this view 
	    	// have a green highlight
	    	if (delta < Float.MAX_VALUE)
	    	{
	    		// view.setBackgroundColor(R.color.green_highlight);
	    	}
    	}
    }
}
