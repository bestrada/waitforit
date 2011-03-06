package com.bryanestrada.waitforit;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class About extends Activity
{
   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.about);
      
      WebView webview = (WebView) findViewById(R.id.webview);
      webview.loadUrl("file:///android_asset/about.html");
   }
}
