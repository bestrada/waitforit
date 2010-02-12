package com.bryanestrada.waitforit;

import java.io.IOException;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.bryanestrada.waitforit.data.DataAccessor;
import com.nextbus.webservices.NextBus;
import com.nextbus.webservices.Prediction;

/**
 * @author <a href="mailto:bryan@bryanestrada.com">Bryan Estrada</a>
 */
public class Main extends ListActivity
{
   private static final String TAG = "Main";

   private final TextView[] _textViews = new TextView[3];
   private final long[] _selectedIds = new long[3];
   private final String[] _selectedTags = new String[3];
   
   private static final int ROUTE = 0;
   private static final int DIRECTION = 1;
   private static final int STOP = 2;
   
   private ListView _selectionList;
   private int _listState;
   
   private TextView _prediction;
   
   private DataAccessor _db;
   private Cursor _cursor;
   private NextBus _nb;
   
   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      _db = new DataAccessor(this);
      _nb = new NextBus();
      try
      {
         _db.createDatabase();
      } catch (IOException e)
      {
         Log.e(TAG, "couldn't initialize database", e);
         if (null != _db)
            _db.close();

         _db = null;
         return;
      }
      _db.open();

      super.onCreate(savedInstanceState);
      this.setContentView(R.layout.main);

      _textViews[ROUTE] = (TextView) findViewById(R.id.route_selection);
      _textViews[DIRECTION] = (TextView) findViewById(R.id.direction_selection);
      _textViews[STOP] = (TextView) findViewById(R.id.stop_selection);
      _selectedIds[STOP] = _selectedIds[DIRECTION] = _selectedIds[ROUTE] = -1;
      
      OnClickListener textViewListener = new OnClickListener()
      {
         @Override
         public void onClick(View view)
         {
            _prediction.setVisibility(View.GONE);
            switch (view.getId())
            {
            case R.id.route_selection:
               _listState = ROUTE;
               _selectedIds[STOP] = _selectedIds[DIRECTION] = _selectedIds[ROUTE] = -1;
               _selectedTags[STOP] = _selectedTags[DIRECTION] = _selectedTags[ROUTE] = null;
               _textViews[ROUTE].setVisibility(View.GONE);
               _textViews[DIRECTION].setVisibility(View.GONE);
               _textViews[STOP].setVisibility(View.GONE);
               swapListCursor(_db.getAllRoutes());
               break;
            case R.id.direction_selection:
               _listState = DIRECTION;
               _selectedIds[STOP] = _selectedIds[DIRECTION] = -1;
               _selectedTags[STOP] = _selectedTags[DIRECTION] = null;
               _textViews[DIRECTION].setVisibility(View.GONE);
               _textViews[STOP].setVisibility(View.GONE);
               swapListCursor(_db.getDirections(_selectedIds[ROUTE]));
               break;
            case R.id.stop_selection:
               _listState = STOP;
               _selectedIds[STOP] = -1;
               _selectedTags[STOP] = null;
               _textViews[STOP].setVisibility(View.GONE);
               swapListCursor(_db.getStops(_selectedIds[DIRECTION]));
               break;
            }
            _selectionList.setVisibility(View.VISIBLE);
         }
      };
      for (int i = 0; i < _textViews.length; i++)
         _textViews[i].setOnClickListener(textViewListener);

      _selectionList = (ListView) findViewById(android.R.id.list);
      _prediction = (TextView) findViewById(R.id.prediction);
      _cursor = _db.getAllRoutes();
      this.startManagingCursor(_cursor);
      this.setListAdapter(new TransitListAdapter(this, _cursor));
      _listState = ROUTE;
   }

   /**
    * Safely swap out the cursor that is backing this page's list by closing the
    * current one and replacing it with the given one
    * 
    * @param c
    *           the cursor to swap with
    */
   private void swapListCursor(Cursor c)
   {
      this.stopManagingCursor(_cursor);
      setListAdapter(new TransitListAdapter(this, c));
      _cursor.close();
      _cursor = c;
      this.startManagingCursor(_cursor);
   }

   @Override
   protected void onListItemClick(ListView l, View v, int position, long id)
   {
      _selectedIds[_listState] = id;
      _selectedTags[_listState] = (String) this.getListAdapter().getItem(position);

      if (v instanceof TextView)
      {
         TextView selected = (TextView) v;
         _textViews[_listState].setText(selected.getText());
         _textViews[_listState].setVisibility(View.VISIBLE);
         
         switch (_listState)
         {
         case ROUTE:
            _selectionList.setVisibility(View.VISIBLE);
            swapListCursor(_db.getDirections(id));
            _listState = DIRECTION;
            break;
         case DIRECTION:
            _selectionList.setVisibility(View.VISIBLE);
            swapListCursor(_db.getStops(id));
            _listState = STOP;
            break;
         case STOP:
            _selectionList.setVisibility(View.GONE);
            _listState = -1;
            String routeTag =  _selectedTags[ROUTE];
            String stopTag  = _selectedTags[STOP];
            showPrediction(routeTag, stopTag);
            _prediction.setVisibility(View.VISIBLE);
            break;
         }
      }
   }
   
   @Override
   public boolean onCreateOptionsMenu(Menu menu)
   {
       MenuInflater inflater = getMenuInflater();
       inflater.inflate(R.menu.menu, menu);
       return true;
   }
   
   private void showPrediction(String routeTag, String stopTag)
   {
      StringBuilder b = new StringBuilder();
      for (Prediction p : _nb.getPrediction(routeTag, stopTag))
      {
         b.append(p.toString());
         b.append('\n');
      }
      _prediction.setText(b.toString());
   }
}