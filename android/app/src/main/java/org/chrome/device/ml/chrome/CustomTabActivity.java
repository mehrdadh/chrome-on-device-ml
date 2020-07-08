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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.browser.customtabs.CustomTabsIntent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class CustomTabHandler extends Activity{
  private final String TAG = "CutomeTabHanlder";
  private CustomTabsIntent.Builder intentBuilder;
  private Activity activity;
  private Handler mHandler;
  private String url;

  public static final String REFRESH_ACTION = CustomTabHandler.class.getSimpleName() + ".action_refresh";
  private boolean shouldCloseCustomTab = true;
  private BroadcastReceiver redirectReceiver;

//  public CustomTabHandler(Activity activity, String url) {
//    this.url = url;
//    this.activity = activity;
//    this.intentBuilder = new CustomTabsIntent.Builder();
//    this.intentBuilder.setShowTitle(true);
//
//    mHandler = new Handler(Looper.getMainLooper()) {
//      @Override
//      public void handleMessage(Message msg) {
//        close();
//      }
//    };
//  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.v(TAG, "Flag: " + shouldCloseCustomTab);

    mHandler = new Handler(Looper.getMainLooper()) {
      @Override
      public void handleMessage(Message msg) {
        Log.v(TAG, "msg received");
//        shouldCloseCustomTab = true;
//        Intent tmp = new Intent(REFRESH_ACTION);
//        tmp.putExtra("PushType", "test");
//        LocalBroadcastManager.getInstance(CustomTabHandler.this).sendBroadcast(tmp);
      }
    };

    // Custom Tab Redirects should not be creating a new instance of this activity
    if (RedirectActivity.CUSTOM_TAB_REDIRECT_ACTION.equals(getIntent().getAction())) {
      finish();
      return;
    }

    if (savedInstanceState == null) {
      String url = "https://example.com/";
      CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
      CustomTabsIntent customTabsIntent = builder.build();
      customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
      this.mHandler.sendEmptyMessageDelayed(0, 5000);
      Log.v(TAG, "shouldCloseCustomTab: " + shouldCloseCustomTab);
      customTabsIntent.launchUrl(this, Uri.parse(url));

      shouldCloseCustomTab = false;

      // This activity will receive a broadcast if it can't be opened from the back stack
      redirectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          // Remove the custom tab on top of this activity.
          Intent newIntent = new Intent(CustomTabHandler.this, CustomTabHandler.class);
          newIntent.setAction(REFRESH_ACTION);
          newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
          startActivity(newIntent);
        }
      };
      LocalBroadcastManager.getInstance(this).registerReceiver(redirectReceiver,
              new IntentFilter(RedirectActivity.CUSTOM_TAB_REDIRECT_ACTION)
      );
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    Log.v(TAG, "onNewIntent");

    if (REFRESH_ACTION.equals(intent.getAction())) {
      Log.v(TAG, "Referesh action");

      // The custom tab is now destroyed so we can finish the redirect activity
      Intent broadcast = new Intent(RedirectActivity.DESTROY_ACTION);
      LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
      unregisterAndFinish();
    } else if (RedirectActivity.CUSTOM_TAB_REDIRECT_ACTION.equals(intent.getAction())) {
      // We have successfully redirected back to this activity. Return the result and close.
      unregisterAndFinish();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.v(TAG, "onResume");
    if (shouldCloseCustomTab) {
      // The custom tab was closed without getting a result.
      unregisterAndFinish();
    }
    shouldCloseCustomTab = true;
  }

  private void unregisterAndFinish() {
    LocalBroadcastManager.getInstance(this).unregisterReceiver(redirectReceiver);
    finish();
  }

//  public void open() {
//    String packageName = CustomTabsHelper.getPackageNameToUse(activity);
//    CustomTabsIntent customTabsIntent = intentBuilder.build();
////    customTabsIntent.intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
////    customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//
//    this.mHandler.sendEmptyMessageDelayed(0, 5000);
//
//    if (packageName == null) {
//      Log.e(TAG, "Packagename is Null.");
//    } else {
//      customTabsIntent.intent.setPackage(packageName);
//      customTabsIntent.launchUrl(activity, Uri.parse(this.url));
//    }
//  }

//  public void close() {
//    Log.v(TAG, "close");
//  }
}
