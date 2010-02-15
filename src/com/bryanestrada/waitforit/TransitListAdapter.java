package com.bryanestrada.waitforit;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TransitListAdapter extends BaseAdapter
{
    private final Context _context;
    private final Cursor _cursor;
    
    public TransitListAdapter(Context context, Cursor cursor)
    {
        _context = context;
        _cursor = cursor;
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
       return result;
    }
}
