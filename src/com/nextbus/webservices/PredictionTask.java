package com.nextbus.webservices;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
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
         URI uri = URIUtils.createURI("http", "webservices.nextbus.com", -1, "/service/publicXMLFeed", URLEncodedUtils.format(query, "UTF-8"), null);
         InputStream in = httpClient.execute(new HttpGet(uri)).getEntity().getContent();
         doc = dbf.newDocumentBuilder().parse(in);
      }
      catch (ParserConfigurationException e) { Log.e(TAG, e.getMessage(), e); doc = null; }
      catch (SAXException e) { Log.e(TAG, e.getMessage(), e); doc = null; }
      catch (IOException e) { Log.e(TAG, e.getMessage(), e); doc = null; } 
      catch (URISyntaxException e) { Log.e(TAG, e.getMessage(), e); doc = null; }
      
      if (null != doc)
      {
         result = new ArrayList<Prediction>();
         NodeList directions = doc.getElementsByTagName("direction");
         for (int i = 0; i < directions.getLength(); i++)
         {
            Node dnode = directions.item(i);
            if (Node.ELEMENT_NODE == dnode.getNodeType())
            {
               String direction = ((Element) dnode).getAttribute("title");
               NodeList predictions = dnode.getChildNodes();
               for (int j = 0; j < predictions.getLength(); j++)
               {
                  Node pnode = predictions.item(j);
                  if (Node.ELEMENT_NODE == pnode.getNodeType())
                  {
                     Element prediction = (Element) pnode;
                     String seconds = prediction.getAttribute("seconds");
                     result.add(new Prediction(Integer.parseInt(seconds), direction));
                  }
               }
            }
         }
         Collections.sort(result);
      }
      return result;
   }
}
