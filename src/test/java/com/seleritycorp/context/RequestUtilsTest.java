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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.newCapture;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.easymock.Capture;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;

public class RequestUtilsTest extends EasyMockSupport {
  private CloseableHttpClient httpClient;

  @Before
  public void setUp() {
    httpClient = createMock(CloseableHttpClient.class);
  }
  
  @Test  
  public void testGetHttpClientNonNull() throws IOException {
    replayAll();

    RequestUtils requestUtils = new RequestUtils("https://foo.example.com/");
    CloseableHttpClient httpClient = requestUtils.getHttpClient();

    httpClient.close();

    verifyAll();
    
    assertThat(httpClient).isNotNull();
  }

  @Test  
  public void testPostRequestOk() throws Exception {
    JsonObject payload = new JsonObject();
    payload.addProperty("foo", "bar/baz");

    JsonObject response = new JsonObject();
    response.addProperty("quux", "quuux");

    Capture<HttpUriRequest> requestCapture = newCapture();
    Capture<ResponseHandler<JsonObject>> handlerCapture = newCapture();
    expect(httpClient.execute(capture(requestCapture), capture(handlerCapture))).andReturn(response);
    httpClient.close();

    replayAll();

    RequestUtils requestUtils = createRequestUtilsPartialMock();
    JsonObject actual = requestUtils.post("pathFoo", payload);

    verifyAll();

    verifyPostRequest(requestCapture);
    verifyHandler(handlerCapture);
    assertThat(actual.getAsJsonPrimitive("quux").getAsString()).isEqualTo("quuux");
    assertThat(actual.entrySet()).hasSize(1);
  }

  @Test  
  public void testPostRequestClientProtocolException() throws Exception {
    JsonObject payload = new JsonObject();
    payload.addProperty("foo", "bar/baz");

    Exception e = new ClientProtocolException("catch me");

    Capture<HttpUriRequest> requestCapture = newCapture();
    Capture<ResponseHandler<JsonObject>> handlerCapture = newCapture();
    expect(httpClient.execute(capture(requestCapture), capture(handlerCapture))).andThrow(e);
    httpClient.close();

    replayAll();

    RequestUtils requestUtils = createRequestUtilsPartialMock();
    try {
      requestUtils.post("pathFoo", payload);
      failBecauseExceptionWasNotThrown(ClientProtocolException.class);
    } catch (ClientProtocolException actual) {
      assertThat(actual.getMessage()).contains("catch me");
    }

    verifyAll();

    verifyPostRequest(requestCapture);
    verifyHandler(handlerCapture);
  }

  @Test  
  public void testPostRequestIOException() throws Exception {
    JsonObject payload = new JsonObject();
    payload.addProperty("foo", "bar/baz");

    Exception e = new IOException("catch me");

    Capture<HttpUriRequest> requestCapture = newCapture();
    Capture<ResponseHandler<JsonObject>> handlerCapture = newCapture();
    expect(httpClient.execute(capture(requestCapture), capture(handlerCapture))).andThrow(e);
    httpClient.close();

    replayAll();

    RequestUtils requestUtils = createRequestUtilsPartialMock();
    try {
      requestUtils.post("pathFoo", payload);
      failBecauseExceptionWasNotThrown(IOException.class);
    } catch (IOException actual) {
      assertThat(actual.getMessage()).contains("catch me");
    }

    verifyAll();

    verifyPostRequest(requestCapture);
    verifyHandler(handlerCapture);
  }

  private void verifyPostRequest(Capture<HttpUriRequest> requestCapture) throws IOException {
    HttpUriRequest request = requestCapture.getValue();
    assertThat(request.getMethod()).isEqualTo("POST");

    assertThat(request).isInstanceOf(HttpPost.class);
    HttpPost post = (HttpPost) request;

    assertThat(post.getURI().toString()).isEqualTo("https://foo.example.com/pathFoo");
    verifyHeader(post, "Accept", "application/json");
    assertThat(requestEntityToString(post)).isEqualTo("json=%7B%22foo%22%3A%22bar%2Fbaz%22%7D");    
  }
  
  private void verifyHeader(HttpPost post, String name, String value) {
    assertThat(post.getHeaders(name)).hasSize(1);
    assertThat(post.getFirstHeader(name).getValue()).isEqualTo(value);
  }

  private void verifyHandler(Capture<ResponseHandler<JsonObject>> handlerCapture) throws IOException {
    ResponseHandler<JsonObject> handler = handlerCapture.getValue();
    
    assertThat(handler).isInstanceOf(RequestResponseHandler.class);
  }
  
  private String requestEntityToString(HttpPost post) throws IOException {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    post.getEntity().writeTo(stream);
    return stream.toString();
  }
  private RequestUtils createRequestUtilsPartialMock() {
    return new RequestUtilsMock();
  }

  private class RequestUtilsMock extends RequestUtils {
    public RequestUtilsMock() {
      super("https://foo.example.com/");
    }

    @Override
    protected CloseableHttpClient getHttpClient() {
      return httpClient;
    }
  }
}
