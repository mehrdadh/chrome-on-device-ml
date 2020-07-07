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
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.browser.customtabs.CustomTabsIntent;

public class CustomTabHandler {
  private final String TAG = "CutomeTabHanlder";
  private CustomTabsIntent.Builder intentBuilder;
  private Activity activity;
  private Handler mHandler;
  private String url;

  public CustomTabHandler(Activity activity, String url) {
    this.url = url;
    this.activity = activity;
    this.intentBuilder = new CustomTabsIntent.Builder();
    this.intentBuilder.setShowTitle(true);

    mHandler = new Handler(Looper.getMainLooper()) {
      @Override
      public void handleMessage(Message msg) {
        close();
      }
    };
  }

  public void open() {
    String packageName = CustomTabsHelper.getPackageNameToUse(activity);
    CustomTabsIntent customTabsIntent = intentBuilder.build();
//    customTabsIntent.intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
//    customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

    this.mHandler.sendEmptyMessageDelayed(0, 5000);

    if (packageName == null) {
      Log.e(TAG, "packagename null");
    } else {
      customTabsIntent.intent.setPackage(packageName);
      customTabsIntent.launchUrl(activity, Uri.parse(this.url));
    }
  }

  public void close() {
    Log.v(TAG, "close");
  }
}
