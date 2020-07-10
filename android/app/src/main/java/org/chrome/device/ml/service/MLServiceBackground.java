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
package org.chrome.device.ml.service;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import java.util.ArrayList;

import org.chrome.device.ml.experiments.Experiment;
import org.chrome.device.ml.experiments.MobileBertExperiment;

/** Run ML task as a service in background. */
public class MLServiceBackground extends JobIntentService {
  private static final String TAG = "MLServiceBackground";
  private static final int JOB_ID = 1000;
  private static final int MODELS_SIZE = 1;

  private Handler expHandler;
  private ArrayList experiments = null;
  private double expTime;
  private int modelSelection;

  public static void enqueueWork(Context context, Intent work) {
    enqueueWork(context, MLServiceBackground.class, JOB_ID, work);
  }

  @Override
  protected void onHandleWork(@NonNull Intent intent) {
    experimentRun();
  }

  @Override
  public void onCreate() {
    super.onCreate();

    modelSelection = 0;
    expHandler = new Handler(Looper.getMainLooper()) {
      @Override
      public void handleMessage(Message msg) {
        experimentMessageHandler(msg);
      }
    };

    experiments = new ArrayList();
    experiments.add(new MobileBertExperiment(getApplicationContext(), expHandler));
    for (int i=0; i<MODELS_SIZE; i++) {
      ((Experiment)experiments.get(i)).initialize();
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    for (int i=0; i<MODELS_SIZE; i++) {
      ((Experiment)experiments.get(i)).close();
    }
  }

  // Handles messages from experiments
  private void experimentMessageHandler(Message msg) {
    expTime = ((Experiment)experiments.get(modelSelection)).getTime();
    Log.v(TAG, "Time: + " + expTime);
  }

  private void experimentRun() {
    expHandler.post(
      () -> {
        ((Experiment)experiments.get(modelSelection)).evaluate(1);
      }
    );
  }
}