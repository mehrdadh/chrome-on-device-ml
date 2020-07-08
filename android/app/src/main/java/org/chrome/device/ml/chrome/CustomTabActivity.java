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
package org.chrome.device.ml.chrome;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsSession;


import org.chrome.device.ml.ChromeActivity;

public class CustomTabActivity extends Activity {
  public static final int MSG_CLIENT_OPEN  = 0;
  public static final int MSG_CLIENT_CLOSED = 1;

  private final String TAG = "CutomeTabHanlder";
  private CustomTabsIntent.Builder intentBuilder;
  private CustomTabActivityHelper mCustomTabActivityHelper;
  private CustomTabsCallback mCustomTabsCallback;
  private Handler mCCTHandler;
  private String url;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent intent = getIntent();
    this.url = intent.getStringExtra("url");
    Log.i(TAG, this.url);

    mCustomTabsCallback = new CustomTabsCallback() {
      @Override
      public void onNavigationEvent(int navigationEvent, @Nullable Bundle extras) {
        if (navigationEvent == CustomTabsCallback.NAVIGATION_FINISHED) {
          finish();
          Intent myIntent = new Intent(CustomTabActivity.this, ChromeActivity.class);
          myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
          myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          startActivity(myIntent);
        }
      }
    };

    mCCTHandler = new Handler(Looper.getMainLooper()) {
      @Override
      public void handleMessage(Message msg) {
        switch (msg.what) {
          case MSG_CLIENT_OPEN:
            openTab(url);
            break;
          default:
            break;
        }
      }
    };

    mCustomTabActivityHelper = new CustomTabActivityHelper(mCCTHandler);
    mCustomTabActivityHelper.bindCustomTabsService(this);
  }

  @Override
  protected void onStart() {
    super.onStart();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mCustomTabActivityHelper.unbindCustomTabsService(this);
  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  private void openTab(String url) {
    this.intentBuilder = new CustomTabsIntent.Builder();
    CustomTabsSession session = mCustomTabActivityHelper.getSession(mCustomTabsCallback);
    if (session != null) {
      this.intentBuilder.setSession(session);
    } else {
      Log.e(TAG, "CCT session is null");
    }

    CustomTabsIntent customTabsIntent = intentBuilder.build();
    customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    customTabsIntent.launchUrl(this, Uri.parse(url));
  }
}
