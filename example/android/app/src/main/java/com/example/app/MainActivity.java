package com.example.app;

import android.os.Bundle;
import com.alitajs.http.http.Http;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    registerPlugin(Http.class);
  }
}
