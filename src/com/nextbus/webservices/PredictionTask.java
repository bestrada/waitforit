package com.nextbus.webservices;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.util.Log;

public class PredictionTask implements Runnable
{
   private static final String TAG = "PredictionTask";
   
   private final String _routeTag;
   private final String _stopTag;
   private PredictionResultHandler _handler;
   
   private static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
   private static HttpClient httpClient = new DefaultHttpClient();
   
   public PredictionTask(PredictionResultHandler handler, String routeTag, String stopTag)
   {
      _routeTag = routeTag;
      _stopTag = stopTag;
      _handler = handler;
   }

   @Override
   public void run()
   {
      Iterable<Prediction> predictions = getPrediction(_routeTag, _stopTag);
      _handler.setPredictionResult(predictions);
   }
   
   private static Iterable<Prediction> getPrediction(String routeTag, String stopTag)
   {
      List<Prediction> result = null;
      Document doc = null;
      
      List<NameValuePair> query = new ArrayList<NameValuePair>(3);
      query.add(new BasicNameValuePair("command", "predictions"));
      query.add(new BasicNameValuePair("a", "sf-muni"));
      query.add(new BasicNameValuePair("r", routeTag));
      query.add(new BasicNameValuePair("s", stopTag));
      try
      {
         DocumentBuilder db = dbf.newDocumentBuilder();
         URI uri = URIUtils.createURI("http", "webservices.nextbus.com", -1, "/service/publicXMLFeed", URLEncodedUtils.format(query, "UTF-8"), null);
         HttpGet get = new HttpGet(uri);
         HttpResponse response = httpClient.execute(get);
         HttpEntity entity =  response.getEntity();
         InputStream in = entity.getContent();
         doc = db.parse(in);
      }
      catch (ParserConfigurationException e)
      {
         Log.e(TAG, e.getMessage(), e);
         doc = null;
      }
      catch (SAXException e)
      {
         Log.e(TAG, e.getMessage(), e);
         doc = null;
      }
      catch (IOException e)
      {
         Log.e(TAG, e.getMessage(), e);
         doc = null;
      } 
      catch (URISyntaxException e)
      {
         Log.e(TAG, e.getMessage(), e);
         doc = null;
      }
      
      if (null != doc)
      {
         NodeList predictions = doc.getElementsByTagName("prediction");
         result = new ArrayList<Prediction>(predictions.getLength());
         for (int i = 0; i < predictions.getLength(); i++)
         {
            Node node = predictions.item(i);
            if (Node.ELEMENT_NODE == node.getNodeType())
            {
               Element prediction = (Element) node;
               String seconds = prediction.getAttribute("seconds");
               result.add(new Prediction(Integer.parseInt(seconds), null));
            }
         }
      }
      return result;
   }
}
