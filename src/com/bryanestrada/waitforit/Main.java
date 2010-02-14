package com.bryanestrada.waitforit;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bryanestrada.waitforit.data.DataAccessor;
import com.nextbus.webservices.NextBus;
import com.nextbus.webservices.Prediction;
import com.nextbus.webservices.PredictionResultHandler;
import com.nextbus.webservices.PredictionTask;

/**
 * @author <a href="mailto:bryan@bryanestrada.com">Bryan Estrada</a>
 */
public class Main extends ListActivity implements PredictionResultHandler
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
   private View _throbbler;
   
   private DataAccessor _db;
   private Cursor _cursor;
   
   private Handler _guiThread;
   private ExecutorService _predictionThread;
   private Runnable _updateTask;
   private Future _predictionPending;
   
   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      _db = new DataAccessor(this);
      try
      {
         _db.createDatabase();
      }
      catch (IOException e)
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
      initializeThreading();

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
            _throbbler.setVisibility(View.GONE);
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
      _throbbler = findViewById(R.id.throbbler);
      
      _cursor = _db.getAllRoutes();
      this.startManagingCursor(_cursor);
      this.setListAdapter(new TransitListAdapter(this, _cursor));
      _listState = ROUTE;
   }
   
   /**
    * initialize the second thread. therea are two: 1) for the main thread that 
    * is running this program and 2) for the web request thing that is used to 
    * invoke the nextbus webservice in the background.
    */
   private void initializeThreading()
   {
      _guiThread = new Handler();
      _predictionThread = Executors.newSingleThreadExecutor();
      
      _updateTask = new Runnable()
      {
         @Override
         public void run()
         {
            String routeTag = _selectedTags[ROUTE];
            String stopTag = _selectedTags[STOP];
            
            // cancel the previous prediction if there was one
            if (null != _predictionPending)
               _predictionPending.cancel(true);
            
            // let the user know we're doing something
            _throbbler.setVisibility(View.VISIBLE);
            _prediction.setVisibility(View.GONE);
            
            // begin prediction, but don't wait for it
            try
            {
               PredictionTask predict = new PredictionTask(Main.this, routeTag, stopTag);
               _predictionPending = _predictionThread.submit(predict);
            }
            catch (RejectedExecutionException e)
            {
               Log.e(TAG, e.getMessage(), e);
               _throbbler.setVisibility(View.GONE);
               _prediction.setText(R.string.prediction_error);
            }
         }
         
      };
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
            showPrediction();
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
   
   private void showPrediction()
   {
      // first replace the text to indicate wait for it, then spin off a thread 
      // that will change it when it's finally done.
      _throbbler.setVisibility(View.VISIBLE);
      
      // now queue an update for the prediction text
      _guiThread.removeCallbacks(_updateTask);
      _guiThread.postDelayed(_updateTask, 200);
   }

   @Override
   public void setPredictionResult(Iterable<Prediction> result)
   {
      StringBuilder b = new StringBuilder();
      for (Prediction p : result)
      {
         b.append(p.toString());
         b.append('\n');
      }
      String predict = b.toString();
      guiSetVisibility(_throbbler, View.GONE);
      if (predict.length() > 0)
      {
         guiSetText(_prediction, predict);
      }
      else
      {
         guiSetText(_prediction, getString(R.string.prediction_error));
      }
      guiSetVisibility(_prediction, View.VISIBLE);
   }
   
   private void guiSetVisibility(final View view, final int visibility)
   {
      _guiThread.post(new Runnable()
      {
         @Override
         public void run()
         {
            view.setVisibility(visibility);
         }
      });
   }
   
   private void guiSetText(final TextView view, final String text)
   {
      _guiThread.post(new Runnable()
      {
         @Override
         public void run()
         {
            view.setText(text);
         }
      });
   }
}