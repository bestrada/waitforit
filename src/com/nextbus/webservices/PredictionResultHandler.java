package com.nextbus.webservices;

/****
 * Callback class for handling results from PredictionTask objects.
 */
public interface PredictionResultHandler
{
   /**
    * Invoked when a PredictionTask completes.
    * @param result the result of the PredictionTask
    */
   public void setPredictionResult(Iterable<Prediction> result);
}
