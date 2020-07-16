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
package org.chrome.device.ml.experiments;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.opencsv.CSVWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.chrome.device.ml.ChromeActivity;
import org.chrome.device.ml.ml.LoadDatasetClient;
import org.chrome.device.ml.ml.QaAnswer;
import org.chrome.device.ml.ml.QaClient;

public class MobileBertExperiment implements Experiment {
  private static final String TAG = "MobileBertExperiment";
  private static final String MODEL_PATH = "bert_model.tflite";

  private final Context context;
  private Handler handler;
  private Handler UIHandler;

  private LoadDatasetClient datasetClient;
  private QaClient qaClient;
  private ArrayList<Double>[] timing;
  private int appMode;

  public MobileBertExperiment(Context context, Handler handler) {
    this.context = context;
    this.UIHandler = handler;

    this.datasetClient = new LoadDatasetClient(this.context);

    HandlerThread handlerThread = new HandlerThread("BertExp");
    handlerThread.start();

    this.handler = new Handler(handlerThread.getLooper());
    this.qaClient = new QaClient(this.context, MODEL_PATH);
  }

  public void initialize() {
    handler.post(
      () -> {
        qaClient.loadModel();
        qaClient.loadDictionary();
      });
  }

  public void close() {
    handler.post(
      () -> {
        qaClient.close();
      });
  }

  // Evaluates Bert model with contents and questions
  /**
   * Evaluate the ML model against contents and questions
   * @param numberOfContents  Number of contents that would be evaluated. If zero, all contents
   *                          will be evaluated.
   */
  public void evaluate(int numberOfContents) {
    int contentsRun;
    if (numberOfContents > 0) {
      contentsRun = Math.min(numberOfContents, datasetClient.getNumberOfContents());
    } else {
      contentsRun = datasetClient.getNumberOfContents();
    }
    Log.i(TAG, "Running " + contentsRun + " contents...");

    this.timing = new ArrayList[contentsRun];
    for (int i=0; i<contentsRun; i++) {
      this.timing[i] = new ArrayList<Double>();
    }

    handler.post(
      () -> {
        for (int i = 0; i < contentsRun; i++) {
          // fetch a content
          final String content = datasetClient.getContent(i);
          String[] question_set = datasetClient.getQuestions(i);
          Log.i(TAG, "Content: " + i);
          for (int j = 0; j < question_set.length; j++) {
            // fetch a question
            String question = question_set[j];

            // Add question mark to match with the dataset.
            if (!question.endsWith("?")) {
              question += '?';
            }

            // Run model and store timing
            final String questionToAsk = question;
            long beforeTime = System.currentTimeMillis();
            final List<QaAnswer> answers = qaClient.predict(questionToAsk, content);
            long afterTime = System.currentTimeMillis();
            // store time in milliseconds
            Double contentTime = new Double(afterTime - beforeTime);
            timing[i].add(contentTime);
          }
        }
        // Send message to UI thread
        Message doneMsg = new Message();
        doneMsg.what = 0;
        doneMsg.obj = "Evaluation Finished";
        this.UIHandler.sendMessage(new Message());
      }
    );
  }

  // average time of all contents in milliseconds
  public int getTime() {
    double time = 0;
    int total = 0;
    for (int i=0; i<this.timing.length; i++) {
      ArrayList<Double> tmp = this.timing[i];
      for (Double item: tmp) {
        time += item;
        total++;
      }
    }
    return new Double(time/total).intValue();
  }

  public void contentTimeCSVWrite() {
    this.appMode = 0;
    String appDataDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
    String baseDir;
    String fileName;
    // set directory and file name
    switch (this.appMode) {
      case ChromeActivity.APP_MODE_ML:
        fileName = "content_timing_ml.csv";
        baseDir = appDataDir + File.separator + "ml";
        break;
      case ChromeActivity.APP_MODE_ML_SERVICE:
        fileName = "content_timing_ml_service.csv";
        baseDir = appDataDir + File.separator + "ml_service";
        break;
      case ChromeActivity.APP_MODE_ML_SERVICE_BACKGROUND:
        fileName = "content_timing_ml_background.csv";
        baseDir = appDataDir + File.separator + "ml_service_background";
        break;
      case ChromeActivity.APP_MODE_WEB_STATIC:
        fileName = "content_timing_ml_web_static.csv";
        baseDir = appDataDir + File.separator + "ml_web_static";
        break;
      case ChromeActivity.APP_MODE_WEB_SCROLL:
        fileName = "content_timing_ml_web_scroll.csv";
        baseDir = appDataDir + File.separator + "ml_web_scroll";
        break;
      case ChromeActivity.APP_MODE_WEB_CONTINUES:
        fileName = "content_timing_ml_web_continues.csv";
        baseDir = appDataDir + File.separator + "ml_web_continues";
        break;
      default:
        baseDir = "";
        fileName = "";
        Log.e(TAG, "Error: App mode");
        return;
    }
    // check if directory exitst
    File dirFile = new File(baseDir);
    if (!dirFile.isDirectory()) {
      dirFile.mkdir();
    }
    File file = new File(this.context.getExternalFilesDir(baseDir), fileName);
    if (file.isDirectory()) {
      Log.e(TAG, file.getName() + " is a directory.");
      return;
    }
    // check if file exist
    if(!file.exists()) {
      try {
        file.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    // if experiment did not run the timing would be null
    if (timing == null) {
      Log.e(TAG, "Timing is null");
      return;
    }

    CSVWriter csvWriter;
    try {
      FileWriter fileWrite = new FileWriter(file, false);
      csvWriter = new CSVWriter(fileWrite);
      String[] title = {"Number", "Time", "Questions"};
      csvWriter.writeNext(title);
      Integer contentCounter = 0;
      for (ArrayList<Double> content: timing) {
        Double average = new Double(0);
        for (Double time: content) {
          average += time;
        }
        average = average / content.size() * 1.0;
        // counter, average time, number of questions
        String[] data = {contentCounter.toString(),
                new Integer(average.intValue()).toString(),
                new Integer(content.size()).toString()};
        csvWriter.writeNext(data);
        contentCounter++;
      }
      String[] total = {"Total Average", new Integer(getTime()).toString(), ""};
      csvWriter.writeNext(total);
      csvWriter.close();
      Log.i(TAG, "Data is written to: " + file.getName());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
