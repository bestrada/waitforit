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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.bryanestrada.waitforit.data.DataAccessor;
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
   private static final int DONE = 3;
   
   private ListView _selectionList;
   private int _listState;
   
   private TextView _prediction;
   private View _throbbler;
   private View _result;
   
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
      findViews();
      setListeners();
      
      _cursor = _db.getAllRoutes();
      startManagingCursor(_cursor);
      setListAdapter(new TransitListAdapter(this, _cursor));
      _listState = ROUTE;
   }
   
   private void findViews()
   {
      _textViews[ROUTE] = (TextView) findViewById(R.id.route_selection);
      _textViews[DIRECTION] = (TextView) findViewById(R.id.direction_selection);
      _textViews[STOP] = (TextView) findViewById(R.id.stop_selection);
      _selectedIds[STOP] = _selectedIds[DIRECTION] = _selectedIds[ROUTE] = -1;
      
      _selectionList = (ListView) findViewById(android.R.id.list);
      _prediction = (TextView) findViewById(R.id.prediction);
      _throbbler = findViewById(R.id.throbbler);
      _result = findViewById(R.id.result);
   }
   
   private void onClickText(View view)
   {
      if (null != _predictionPending)
         _predictionPending.cancel(true);
      
      _throbbler.setVisibility(View.GONE);
      _result.setVisibility(View.GONE);
      Animation pullLeft = AnimationUtils.loadAnimation(this, R.anim.pull_left);
      Animation appear = AnimationUtils.loadAnimation(this, R.anim.appear);
      switch (view.getId())
      {
      case R.id.route_selection:
         _listState = ROUTE;
         _selectedIds[STOP] = _selectedIds[DIRECTION] = _selectedIds[ROUTE] = -1;
         _selectedTags[STOP] = _selectedTags[DIRECTION] = _selectedTags[ROUTE] = null;
         
         _textViews[ROUTE].startAnimation(pullLeft);
         _textViews[DIRECTION].startAnimation(pullLeft);
         _textViews[STOP].startAnimation(pullLeft);
         _textViews[ROUTE].setVisibility(View.GONE);
         _textViews[DIRECTION].setVisibility(View.GONE);
         _textViews[STOP].setVisibility(View.GONE);
         swapListCursor(_db.getAllRoutes());
         break;
      case R.id.direction_selection:
         _listState = DIRECTION;
         _selectedIds[STOP] = _selectedIds[DIRECTION] = -1;
         _selectedTags[STOP] = _selectedTags[DIRECTION] = null;
         
         _textViews[DIRECTION].startAnimation(pullLeft);
         _textViews[STOP].startAnimation(pullLeft);
         _textViews[DIRECTION].setVisibility(View.GONE);
         _textViews[STOP].setVisibility(View.GONE);
         swapListCursor(_db.getDirections(_selectedIds[ROUTE]));
         break;
      case R.id.stop_selection:
         _listState = STOP;
         _selectedIds[STOP] = -1;
         _selectedTags[STOP] = null;
         
         _textViews[STOP].startAnimation(pullLeft);
         _textViews[STOP].setVisibility(View.GONE);
         swapListCursor(_db.getStops(_selectedIds[DIRECTION]));
         break;
      }
      _selectionList.setVisibility(View.VISIBLE);
      _selectionList.startAnimation(appear);
   }
   
   private void setListeners()
   {
      OnClickListener textViewListener = new OnClickListener()
      {
         @Override
         public void onClick(View view)
         {
            Main.this.onClickText(view);
         }
      };
      for (int i = 0; i < _textViews.length; i++)
         _textViews[i].setOnClickListener(textViewListener);
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
            _result.setVisibility(View.GONE);
            
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
               _prediction.setTextAppearance(Main.this, R.style.ResultTextError);
               _prediction.setText(R.string.prediction_error);
            }
         }
         
      };
   }
   
   /**
    * Safely swap out the cursor that is backing this page's list by closing the
    * current one and replacing it with the given one
    * @param c the cursor to swap with
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

      if (v instanceof LinearLayout)
      {
         TextView selected = (TextView) v.findViewById(R.id.name);

         _textViews[_listState].setText(selected.getText());
         _textViews[_listState].setVisibility(View.VISIBLE);
         
         Animation push = AnimationUtils.loadAnimation(this, R.anim.push_right);
         Animation appear = AnimationUtils.loadAnimation(this, R.anim.appear);
         _textViews[_listState].startAnimation(push);
         switch (_listState)
         {
         case ROUTE:
            _selectionList.setVisibility(View.VISIBLE);
            _selectionList.startAnimation(appear);
            //_selectionList.startAnimation(push);
            swapListCursor(_db.getDirections(id));
            _listState = DIRECTION;
            break;
         case DIRECTION:
            _selectionList.setVisibility(View.VISIBLE);
            _selectionList.startAnimation(appear);
            //_selectionList.startAnimation(push);
            swapListCursor(_db.getStops(id));
            _listState = STOP;
            break;
         case STOP:
            _selectionList.setVisibility(View.GONE);
            _listState = DONE;
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
         guiSetText(_prediction, predict, R.style.ResultText);
      }
      else
      {
         guiSetText(_prediction, getString(R.string.prediction_error), R.style.ResultTextError);
      }
      guiSetVisibility(_result, View.VISIBLE);
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
   
   private void guiSetText(final TextView view, final String text, final int style)
   {
      _guiThread.post(new Runnable()
      {
         @Override
         public void run()
         {
            view.setTextAppearance(Main.this, style);
            view.setText(text);
         }
      });
   }

   
   @Override
   public boolean onKeyDown(int keyCode, KeyEvent event)
   {
      boolean result = false;
      if (KeyEvent.KEYCODE_BACK == keyCode)
      {
         if (_listState >= DIRECTION)
         {
            this.onClickText(_textViews[_listState-1]);
            result = true;
         }
      }
      return result ? result : super.onKeyDown(keyCode, event);
   }
}