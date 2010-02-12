package com.nextbus.webservices;

public class Prediction
{
   private final int _secondsFromNow;
   private final String _direction;
   Prediction(int seconds, String direction)
   {
      _secondsFromNow = seconds;
      _direction = direction;
   }
   
   public int getSecondsFromNow()
   {
      return _secondsFromNow;
   }
   public String getDirection()
   {
      return _direction;
   }
   
   @Override
   public String toString()
   {
      String result = null;
      if (_secondsFromNow < 60)
      {
         result = String.format(_secondsFromNow != 1 ? "%d seconds" : "%d second", _secondsFromNow);
      }
      else
      {
         int minutes = Math.round(_secondsFromNow / 60.0f);
         result = String.format(minutes != 1 ? "%d minutes" : "%d minute", minutes);
      }
      return result;
   }
}
