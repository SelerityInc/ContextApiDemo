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
import static org.easymock.EasyMock.expect;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicStatusLine;
import org.easymock.EasyMockSupport;
import org.junit.Test;

import com.google.gson.JsonObject;

public class RequestResponseHandlerTest extends EasyMockSupport {
  @Test
  public void testHandleResponseOk() throws Exception {
    HttpResponse response = mockResponse(200, "{\"foo\":42}");
    RequestResponseHandler handler = createRequestResponseHandler();

    replayAll();
    
    JsonObject actual = handler.handleResponse(response);
    
    verifyAll();
    
    assertThat(actual.getAsJsonPrimitive("foo").getAsInt()).isEqualTo(42);
    assertThat(actual.entrySet()).hasSize(1);
  }

  @Test
  public void testHandleResponseClientErrorNonJson() throws Exception {
    HttpResponse response = mockResponse(400, "nonJsonContentFoo");
    RequestResponseHandler handler = createRequestResponseHandler();

    replayAll();
    
    try {
      handler.handleResponse(response);
      failBecauseExceptionWasNotThrown(ClientProtocolException.class);
    } catch (ClientProtocolException e) {
      assertThat(e.getMessage()).contains("400");
      assertThat(e.getMessage()).contains("reasonFoo");
      assertThat(e.getMessage()).doesNotContain("nonJsonContentFoo");
    }
    
    verifyAll();
  }

  @Test
  public void testHandleResponsePartialNonJson() throws Exception {
    HttpResponse response = mockResponse(206, "nonJsonContentFoo");
    RequestResponseHandler handler = createRequestResponseHandler();

    replayAll();
    
    try {
      handler.handleResponse(response);
      failBecauseExceptionWasNotThrown(ClientProtocolException.class);
    } catch (ClientProtocolException e) {
      assertThat(e.getMessage()).contains("206");
      assertThat(e.getMessage()).contains("reasonFoo");
      assertThat(e.getMessage()).doesNotContain("nonJsonContentFoo");
    }
    
    verifyAll();
  }

  @Test
  public void testHandleResponseRedirectNonJson() throws Exception {
    HttpResponse response = mockResponse(302, "nonJsonContentFoo");
    RequestResponseHandler handler = createRequestResponseHandler();

    replayAll();
    
    try {
      handler.handleResponse(response);
      failBecauseExceptionWasNotThrown(ClientProtocolException.class);
    } catch (ClientProtocolException e) {
      assertThat(e.getMessage()).contains("302");
      assertThat(e.getMessage()).contains("reasonFoo");
      assertThat(e.getMessage()).doesNotContain("nonJsonContentFoo");
    }
    
    verifyAll();
  }

  @Test
  public void testHandleResponseServerErrorNonJson() throws Exception {
    HttpResponse response = mockResponse(500, "nonJsonContentFoo");
    RequestResponseHandler handler = createRequestResponseHandler();

    replayAll();
    
    try {
      handler.handleResponse(response);
      failBecauseExceptionWasNotThrown(ClientProtocolException.class);
    } catch (ClientProtocolException e) {
      assertThat(e.getMessage()).contains("500");
      assertThat(e.getMessage()).contains("reasonFoo");
      assertThat(e.getMessage()).doesNotContain("nonJsonContentFoo");
    }
    
    verifyAll();
  }

  @Test
  public void testHandleResponseClientErrorJsonMarkerless() throws Exception {
    HttpResponse response = mockResponse(400, "{\"bar\":42}");
    RequestResponseHandler handler = createRequestResponseHandler();

    replayAll();
    
    try {
      handler.handleResponse(response);
      failBecauseExceptionWasNotThrown(ClientProtocolException.class);
    } catch (ClientProtocolException e) {
      assertThat(e.getMessage()).contains("400");
      assertThat(e.getMessage()).contains("reasonFoo");
      assertThat(e.getMessage()).doesNotContain("bar");
      assertThat(e.getMessage()).doesNotContain("42");
    }
    
    verifyAll();
  }

  @Test
  public void testHandleResponseClientErrorJsonErrorMessage() throws Exception {
    HttpResponse response = mockResponse(400, "{\"errorMessage\":\"bar\"}");
    RequestResponseHandler handler = createRequestResponseHandler();

    replayAll();
    
    try {
      handler.handleResponse(response);
      failBecauseExceptionWasNotThrown(ClientProtocolException.class);
    } catch (ClientProtocolException e) {
      assertThat(e.getMessage()).contains("400");
      assertThat(e.getMessage()).contains("reasonFoo");
      assertThat(e.getMessage()).contains("bar");
    }
    
    verifyAll();
  }

  @Test
  public void testHandleResponseClientErrorJsonErrorObject() throws Exception {
    HttpResponse response = mockResponse(400, "{\"errorCode\":\"bar\", \"foo\":42}");
    RequestResponseHandler handler = createRequestResponseHandler();

    replayAll();
    
    try {
      handler.handleResponse(response);
      failBecauseExceptionWasNotThrown(ClientProtocolException.class);
    } catch (ClientProtocolException e) {
      assertThat(e.getMessage()).contains("400");
      assertThat(e.getMessage()).contains("reasonFoo");
      assertThat(e.getMessage()).contains("errorCode");
      assertThat(e.getMessage()).contains("bar");
      assertThat(e.getMessage()).contains("foo");
      assertThat(e.getMessage()).contains("42");
    }
    
    verifyAll();
  }

  private HttpResponse mockResponse(int statusCode, String content) throws Exception {
    ProtocolVersion proto = new ProtocolVersion("protoFoo", 1, 2);
    StatusLine statusLine = new BasicStatusLine(proto, statusCode, "reasonFoo");

    byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
    InputStream contentStream = new ByteArrayInputStream(contentBytes);

    Header contentType = new BasicHeader("Content-Type", "application/json");

    HttpEntity httpEntity = createMock(HttpEntity.class);
    expect(httpEntity.getContent()).andReturn(contentStream);
    expect(httpEntity.getContentLength()).andReturn((long)content.length()).anyTimes();
    expect(httpEntity.getContentType()).andReturn(contentType);

    HttpResponse response = createMock(HttpResponse.class);
    expect(response.getStatusLine()).andReturn(statusLine);
    expect(response.getEntity()).andReturn(httpEntity);
    
    return response;
  }

  private RequestResponseHandler createRequestResponseHandler() {
    return new RequestResponseHandler();
  }
}
