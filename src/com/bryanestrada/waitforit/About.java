package com.bryanestrada.waitforit;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
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
      //AssetFileDescriptor fd = getAssets().openFd("about.html");
      webview.loadUrl("file:///android_asset/about.html");
   }
}
