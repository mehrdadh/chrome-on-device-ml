/* Copyright 2020. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/
package org.chrome.device.ml;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsIntent;

import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import java.io.IOException;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.chrome.device.ml.chrome.CustomTabActivity;
import org.chrome.device.ml.chrome.CustomTabActivityHelper;
import org.chrome.device.ml.experiments.MobileBertExperiment;
import org.chrome.device.ml.service.MLService;
import org.chrome.device.ml.service.MLServiceBackground;
import org.chrome.device.ml.service.RemoteService;
import org.chrome.device.ml.service.RemoteServiceCallback;

public class ChromeActivity extends AppCompatActivity {
  private static final String TAG = "ChromeOnDeviceML";
  private static final String URL_PATH = "url_list.txt";
  private static final int MSG_TIME_UPDATE = 1;

  public static final int APP_MODE_ML = 0;
  public static final int APP_MODE_ML_SERVICE = 1;
  public static final int APP_MODE_ML_SERVICE_BACKGROUND = 2;
  public static final int APP_MODE_WEB_STATIC = 3;
  public static final int APP_MODE_WEB_SCROLL = 4;
  public static final int APP_MODE_WEB_CONTINUES = 5;
  public static int APP_MODE;

  private Button classifyButton;
  private Handler mhandler;
  private Handler tabHandler;
  public TextView resultTextView;
  private ScrollView scrollView;
  private Spinner modelSpinner;
  private int modelSelection;
  private Spinner appModeSpinner;

  // ML Service
  private RemoteService mService;
  private Handler serviceHandler;
  private RemoteServiceCallback serviceCallback;
  private ServiceConnection mServiceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
      Log.v(TAG, "Service connected.");
      mService = RemoteService.Stub.asInterface(service);
      // Monitor service
      try{
        mService.registerCallback(serviceCallback);
      } catch (RemoteException e) {
        Log.e(TAG, e.getStackTrace().toString());
      }
    }
    @Override
    public void onServiceDisconnected(ComponentName componentName) {
      Log.e(TAG, "Service has unexpectedly disconnected");
      mService = null;
    }
  };

  // Custom Tab
  private ArrayList<String> urlList;
  private int urlNumber;
  private boolean customTabsStarted;

  // Experiment
  private MobileBertExperiment mobileBert;
  private Handler expHandler;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.v(TAG, "onCreate");
    setContentView(R.layout.activity_main);
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    mhandler = new Handler(Looper.getMainLooper());
    tabHandler = new Handler();

    modelSpinner = findViewById(R.id.modelSpinner);
    appModeSpinner = findViewById(R.id.appModeSpinner);
    classifyButton = findViewById(R.id.button);
    classifyButton.setOnClickListener(
            (View v) -> {
              buttonHandler();
            });
    scrollView = findViewById(R.id.scroll_view);
    resultTextView = findViewById(R.id.result_text_view);
    addItemsOnSpinner();
    addListenerOnSpinnerItemSelection();

    urlNumber = 0;
    customTabsStarted = false;
    urlList = new ArrayList<String>();
    try {
      urlList = Utils.getURLList(getApplicationContext().getAssets(), URL_PATH);
    } catch (IOException e) {
      Log.e(TAG, "Error in reading URL list.");
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    Log.v(TAG, "onStart");
    serviceHandler = new Handler() {
      @Override public void handleMessage(Message msg) {
        switch (msg.what) {
          case MSG_TIME_UPDATE:
            double time = (double) msg.obj;
            textboxAppend("Time: " + time + "\n");
            break;
          default:
            super.handleMessage(msg);
        }
      }
    };

    serviceCallback = new RemoteServiceCallback.Stub() {
      // This is called by the remote service regularly to tell us about new values.
      public void timeChanged(double time) {
        Message msg = new Message();
        msg.what = MSG_TIME_UPDATE;
        msg.obj = time;
        serviceHandler.sendMessage(msg);
      }
    };

    // request file storage permission
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
              Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.v(TAG, "onResume");
    if (urlNumber < urlList.size() & customTabsStarted) {
      openCustomTabActivity();
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    Log.v(TAG, "onStop");

//    if (mService != null) {
//      try {
//        mService.unregisterCallback(serviceCallback);
//      } catch (RemoteException e) {
//        Log.e(TAG, "Error unregister callback");
//      }
//    }
//    if (isMyServiceRunning(MLService.class)) {
//      Log.i(TAG, "Closing ML service.");
//      unbindService(mServiceConnection);
//      stopService(new Intent(ChromeActivity.this, MLService.class));
//    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // flate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
     // Handle action bar item clicks here. The action bar will
     // automatically handle clicks on the Home/Up button, so long
     // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    // noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  public void addListenerOnSpinnerItemSelection() {
    modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        modelSelection = i;
      }
      
      @Override
      public void onNothingSelected(AdapterView<?> adapterView) {
        modelSelection = 0;
      }
    });

    appModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        APP_MODE = i;
      }

      @Override
      public void onNothingSelected(AdapterView<?> adapterView) {
        APP_MODE = 0;
      }
    });
  }

  public static int getAppMode() {
    return APP_MODE;
  }

  private void addItemsOnSpinner() {
    // model spinner
    String [] MODELS = {"MobileBert"};
    List<String> list = new ArrayList<String>();
    for (int i=0; i<MODELS.length; i++) {
      list.add(MODELS[i]);
    }
    ArrayAdapter<String> dataAdapter1 = new ArrayAdapter<String>(getApplicationContext(),
      android.R.layout.simple_spinner_item, list);
    dataAdapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    modelSpinner.setAdapter(dataAdapter1);

    // app mode spinner
    String [] appModes = {"ML Task w/o Service", "ML Task w/ Service",
            "ML Task w/ Background Service", "Background Service, Static Web",
            "Background Service, Scrolling Web", "Background Service, Continues Web"};
    List<String> modeList = new ArrayList<String>();
    for (String item: appModes) {
      modeList.add(item);
    }
    ArrayAdapter<String> dataAdapter2 = new ArrayAdapter<String>(getApplicationContext(),
            android.R.layout.simple_spinner_item, modeList);
    dataAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    appModeSpinner.setAdapter(dataAdapter2);
    appModeSpinner.setSelection(APP_MODE_ML);
  }

  // Handles button actions
  private void buttonHandler() {
    switch (APP_MODE) {
      case APP_MODE_ML:
        textboxAppend("Running ML...\n");
        expHandler = new Handler(Looper.getMainLooper()) {
          @Override
          public void handleMessage(Message msg) {
            mobileBert.contentTimeCSVWrite();
            textboxAppend("Time: " + mobileBert.getTime() + "\n");
          }
        };
        mobileBert = new MobileBertExperiment(this, expHandler);
        expHandler.post(
          () -> {
            mobileBert.initialize();
            mobileBert.evaluate(0);
          }
        );
        break;
      case APP_MODE_ML_SERVICE:
        textboxAppend("Running ML on service...\n");
        Intent service = new Intent(ChromeActivity.this, MLService.class);
        service.setAction(RemoteService.class.getName());
        bindService(service, mServiceConnection, Context.BIND_AUTO_CREATE);
        this.startService(service);
        break;
      case APP_MODE_ML_SERVICE_BACKGROUND:
        textboxAppend("Running ML on background service...\n");
        MLServiceBackground.enqueueWork(this, new Intent());
        break;
      case APP_MODE_WEB_STATIC:
        textboxAppend("Running ML on service with static web page...");
        Intent mlService = new Intent(ChromeActivity.this, MLService.class);
        mlService.setAction(RemoteService.class.getName());
//        bindService(mlService, mServiceConnection, Context.BIND_AUTO_CREATE);
        this.startService(mlService);

        String url = "https://www.pinterest.com";
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent cctIntent = builder.build();
        CustomTabActivityHelper.openCustomTab(this, cctIntent, Uri.parse(url), null);
        break;
      case APP_MODE_WEB_SCROLL:
        textboxAppend("mode: " + APP_MODE + "\n");
        //start background service
        //open a tab, scroll on the page
        break;
      case APP_MODE_WEB_CONTINUES:
        textboxAppend("mode: " + APP_MODE + "\n");
        break;
      default:
        textboxAppend("Wrong mode selected.\n");
        Log.e(TAG, "Wrong mode selected.");
    }

//    urlNumber = 35;
//    customTabsStarted = true;
//    Collections.shuffle(urlList);
  }

  // Show experiment result in textbox
  private void showExperimentResult(double time, int numberOfContents) {
    runOnUiThread(
      () -> {
        DecimalFormat df2 = new DecimalFormat("##.##");
        String textToShow = "Time: " + df2.format(time) + "\n";
        textboxAppend(textToShow);
      }
    );
  }

  private void openCustomTabActivity() {
    if (urlNumber >= urlList.size()) {
      Log.e(TAG, "URL index out of size");
      return;
    }
    Intent tmp = new Intent(ChromeActivity.this, CustomTabActivity.class);
    tmp.putExtra("url", urlList.get(urlNumber));
    tmp.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    urlNumber++;
    startActivity(tmp);
  }

  // Append text to the textbox and scroll down the view
  private void textboxAppend(String text) {
    resultTextView.append(text);
    scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
  }

  private boolean isMyServiceRunning(Class<?> serviceClass) {
    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
    for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
      if (serviceClass.getName().equals(service.service.getClassName())) {
        return true;
      }
    }
    return false;
  }
}