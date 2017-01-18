/*
 * Copyright (C) 2017 Selerity, Inc. (support@seleritycorp.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.seleritycorp.context;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * Parses query response to JsonObject, or upon error extracts descriptive error message.
 */
public class RequestResponseHandler implements org.apache.http.client.ResponseHandler<JsonObject> {
  @Override
  public JsonObject handleResponse(HttpResponse response)
      throws ClientProtocolException, IOException {
    StatusLine statusLine = response.getStatusLine();
    int statusCode = statusLine.getStatusCode();
    HttpEntity entity = response.getEntity();
    String responseString = EntityUtils.toString(entity);
    JsonParser parser = new JsonParser();

    // The API does not return partials or some such, so any response that is not a 200,
    // indicates issues.
    if (statusCode != 200) {
      String errorMessage = statusCode + " " + statusLine.getReasonPhrase();
      // Instead of the pure HTTP status information, we try to get a descriptive error
      // message. Context API returns JSON objects that indicate the error. So we
      // opportunistically parse the content and drill down to get the error message. 
      try {
        JsonObject json = parser.parse(responseString).getAsJsonObject();
        String hint = "";
          
        if (json.has("errorCode")) {
          hint = json.toString();
        } else if (json.has("errorMessage")) {
          hint = json.get("errorMessage").getAsString();
        }

        if (hint != null && !hint.isEmpty()) {
          errorMessage += ". (" + hint + ")";
        }
      } catch (Exception e) {
        // Extracting a more detailed error message failed. So we move forward with only the
        // HTTP status line.
      }
      throw new ClientProtocolException("Server responded with " + errorMessage);
    }
    
    // At this point, response has a 200 HTTP status code.

    String contentType = entity.getContentType().getValue();
    String parameterlessContentType = contentType.split("[; ]", 2)[0];
    if (!"application/json".equals(parameterlessContentType)) {
      // Response is not Json
      throw new ClientProtocolException("Received content type '" +  contentType
          + "' instead of 'application/json'");
    }

    // At this point, response got sent as JSON
    
    // Parse the string to JSON
    JsonObject ret = parser.parse(responseString).getAsJsonObject();
    return ret;
  }    
}
