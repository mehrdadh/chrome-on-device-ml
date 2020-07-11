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

import android.content.res.AssetManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/** Utils functions */
public class Utils {

  public static ArrayList<String> getURLList(AssetManager assetManager, String path) throws IOException {
    ArrayList<String> urlList = new ArrayList<String>();
    try (InputStream ins = assetManager.open(path);
         BufferedReader reader = new BufferedReader(new InputStreamReader(ins))) {
      while (reader.ready()) {
        urlList.add(reader.readLine());
      }
    }
    return urlList;
  }
}
