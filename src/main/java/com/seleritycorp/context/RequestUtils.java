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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


/**
 * Low-level Utilities to make actual requests to the Context API endpoint.
 */
public class RequestUtils {
  private static final Log log = LogFactory.getLog(RequestUtils.class);

  private static final Gson GSON = new GsonBuilder().serializeNulls().create();

  protected CloseableHttpClient getHttpClient() {
    return HttpClients.createSystem();
  }

  /**
   * Context API endpoint to connect to.
   * 
   * <p/>Has to contain both protocol and path. To connect to the test endpoint, use
   * {@code https://context-api-test.seleritycorp.com/}
   */
  private final String apiServerRootUrl;

  /**
   * User agent to use for requests.
   */
  private final String userAgent;

  /**
   * Constructs RequestUtils for a given Context API endpoint.
   * 
   * @param apiServerRootUrl The Context API endpoint to use for requests. Has to contain both
   *     protocol and path. To connect to the test endpoint, use
   *     {@code https://context-api-test.seleritycorp.com/}.
   */
  public RequestUtils(String apiServerRootUrl) {
    this.apiServerRootUrl = apiServerRootUrl;
    this.userAgent = getUserAgent();
  }

  /**
   * Builds a User-Agent header value that identifies this build.
   *
   * @return the User-Agent header value that identifies this build
   */
  private String getUserAgent() {
    String buildPropertiesPath = "/META-INF/main-application/build.properties";
    Properties properties = new Properties();
    try (InputStream stream = RequestUtils.class.getResourceAsStream(buildPropertiesPath)) {
      properties.load(stream);
    } catch (IOException e) {
      System.err.println("Could not load build properties at '" + buildPropertiesPath + "'."
          + e.toString());
    }
    
    return properties.getProperty("artifactId") + "/" + properties.getProperty("version")
      + " (build " + properties.getProperty("git.description") + "/"
      + properties.getProperty("build.time") + ")";
  }

  /**
   * Performs a POST request
   * 
   * @param path The path for the post request (relative to the Context API endpoint)
   * @param payload Request's payload to send with the request
   * @return Context API's response json 
   * @throws Exception if server did not indicate success, or the response did not parse as JSON.
   */
  public JsonObject post(String path, JsonObject payload) throws Exception {
    String requestUrl = apiServerRootUrl + path;
    String payloadString = GSON.toJson(payload);

    List<NameValuePair> urlParameters = new ArrayList<NameValuePair>(1);
    urlParameters.add(new BasicNameValuePair("json", payloadString));

    HttpPost httpPost = new HttpPost(requestUrl);
    httpPost.setHeader("Accept", "application/json");
    httpPost.setHeader("User-Agent", userAgent);
    httpPost.setEntity(new UrlEncodedFormEntity(urlParameters));

    ResponseHandler<JsonObject> handler = new RequestResponseHandler();

    JsonObject ret;
    try (CloseableHttpClient httpclient = getHttpClient()) {
      log.debug("POSTing request to " + requestUrl + " with payload json=" + payloadString);
      ret = httpclient.execute(httpPost, handler);
    }
    return ret;
  }
}
