package com.nextbus.webservices;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

public class NextBus
{
   private static final String TAG = "NextBus";
   
   private DocumentBuilderFactory _dbf;
   private HttpClient _httpClient;
   
   public NextBus()
   {
      _dbf = DocumentBuilderFactory.newInstance();
      _httpClient = new DefaultHttpClient();
   }
   
   
}
