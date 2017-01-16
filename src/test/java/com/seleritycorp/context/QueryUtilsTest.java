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
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.eq;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import org.easymock.Capture;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class QueryUtilsTest extends EasyMockSupport {
  private RequestUtils requestUtils;

  @Before
  public void setUp() {
    requestUtils = createMock(RequestUtils.class);
  }

  @Test
  public void testQueryEntitledSourcesOk() throws Exception {
    Capture<JsonObject> payloadCapture = newCapture();
    
    JsonObject response = new JsonObject();
    JsonArray sources = new JsonArray();
    sources.add("foo");
    sources.add("bar");
    response.add("sources", sources);
    
    expect(requestUtils.post(eq("/v2/sources"), capture(payloadCapture))).andReturn(response);
    
    replayAll();
    
    QueryUtils queryUtils = createQueryUtils();

    long start = System.currentTimeMillis();
    JsonArray actual = queryUtils.queryEntitledSources();
    long end = System.currentTimeMillis();
    
    verifyAll();
    
    JsonObject payload = payloadCapture.getValue();
    verifyPayloadCommonFields(payload, start, end);
    
    assertThat(actual).containsExactly(new JsonPrimitive("foo"), new JsonPrimitive("bar"));
  }

  @Test
  public void testQueryEntitledSourcesInvalidResponse() throws Exception {
    Capture<JsonObject> payloadCapture = newCapture();
    
    Exception e = new Exception("catch me");
    JsonObject response = new JsonObject();
    JsonArray sources = new JsonArray();
    sources.add("foo");
    sources.add("bar");
    response.add("sources", sources);
    
    expect(requestUtils.post(eq("/v2/sources"), capture(payloadCapture))).andThrow(e);
    
    replayAll();
    
    QueryUtils queryUtils = createQueryUtils();
    long start = System.currentTimeMillis();
    try {
      queryUtils.queryEntitledSources();
      failBecauseExceptionWasNotThrown(Exception.class);
    } catch (Exception actual) {
      assertThat(actual.getMessage()).contains("catch me");
    }
    long end = System.currentTimeMillis();
    
    verifyAll();
    
    JsonObject payload = payloadCapture.getValue();
    verifyPayloadCommonFields(payload, start, end);
  }

  @Test
  public void testQueryRecommendationsInitial() throws Exception {
    Capture<JsonObject> payloadCapture = newCapture();
    
    JsonObject response = new JsonObject();
    JsonArray recommendations = new JsonArray();
    recommendations.add("foo");
    recommendations.add("bar");
    response.add("recommendations", recommendations);
    
    expect(requestUtils.post(eq("/v2/query"), capture(payloadCapture))).andReturn(response);
    
    replayAll();
    
    QueryUtils queryUtils = createQueryUtils();

    long start = System.currentTimeMillis();
    JsonArray actual = queryUtils.queryRecommendations("typeFoo", true, 42, new LinkedList<>());
    long end = System.currentTimeMillis();
    
    verifyAll();
    
    JsonObject payload = payloadCapture.getValue();
    verifyPayloadCommonFields(payload, start, end);
    JsonObject parameters = payload.getAsJsonObject("parameters"); 
    assertThat(parameters.get("queryType").getAsString()).isEqualTo("typeFoo");
    assertThat(parameters.get("queryMode").getAsString()).isEqualTo("INITIAL");
    assertThat(parameters.get("numItems").getAsInt()).isEqualTo(42);
    
    JsonObject interests = payload.getAsJsonObject("interests");
    assertThat(interests.entrySet()).isEmpty();

    assertThat(actual).containsExactly(new JsonPrimitive("foo"), new JsonPrimitive("bar"));
  }

  @Test
  public void testQueryRecommendationsUpdate() throws Exception {
    Capture<JsonObject> payloadCapture = newCapture();
    
    JsonObject response = new JsonObject();
    JsonArray recommendations = new JsonArray();
    recommendations.add("foo");
    recommendations.add("bar");
    response.add("recommendations", recommendations);
    
    expect(requestUtils.post(eq("/v2/query"), capture(payloadCapture))).andReturn(response);
    
    replayAll();
    
    QueryUtils queryUtils = createQueryUtils();

    long start = System.currentTimeMillis();
    JsonArray actual = queryUtils.queryRecommendations("typeFoo", false, 42, new LinkedList<>());
    long end = System.currentTimeMillis();
    
    verifyAll();
    
    JsonObject payload = payloadCapture.getValue();
    verifyPayloadCommonFields(payload, start, end);
    JsonObject parameters = payload.getAsJsonObject("parameters"); 
    assertThat(parameters.get("queryType").getAsString()).isEqualTo("typeFoo");
    assertThat(parameters.get("queryMode").getAsString()).isEqualTo("UPDATE");
    assertThat(parameters.get("numItems").getAsInt()).isEqualTo(42);
    
    JsonObject interests = payload.getAsJsonObject("interests");
    assertThat(interests.entrySet()).isEmpty();

    assertThat(actual).containsExactly(new JsonPrimitive("foo"), new JsonPrimitive("bar"));
  }

  @Test
  public void testQueryRecommendationsEntities() throws Exception {
    Capture<JsonObject> payloadCapture = newCapture();
    
    JsonObject response = new JsonObject();
    JsonArray recommendations = new JsonArray();
    recommendations.add("foo");
    recommendations.add("bar");
    response.add("recommendations", recommendations);
    
    expect(requestUtils.post(eq("/v2/query"), capture(payloadCapture))).andReturn(response);
    
    replayAll();
    
    QueryUtils queryUtils = createQueryUtils();

    List<String> requestedEntities = new ArrayList<>(2);
    requestedEntities.add("quux");
    requestedEntities.add("quuux");
    
    long start = System.currentTimeMillis();
    JsonArray actual = queryUtils.queryRecommendations("typeFoo", true, 42, requestedEntities);
    long end = System.currentTimeMillis();
    
    verifyAll();
    
    JsonObject payload = payloadCapture.getValue();
    verifyPayloadCommonFields(payload, start, end);
    JsonObject parameters = payload.getAsJsonObject("parameters"); 
    assertThat(parameters.get("queryType").getAsString()).isEqualTo("typeFoo");
    assertThat(parameters.get("queryMode").getAsString()).isEqualTo("INITIAL");
    assertThat(parameters.get("numItems").getAsInt()).isEqualTo(42);
    
    JsonObject interests = payload.getAsJsonObject("interests");
    assertThat(interests.entrySet()).hasSize(1);
    JsonArray entities = interests.getAsJsonArray("entities");
    JsonObject entity = entities.get(0).getAsJsonObject();
    assertThat(entity.get("entityID").getAsString()).isEqualTo("quux");
    assertThat(entity.get("weight").getAsFloat()).isEqualTo(1f);
    entity = entities.get(1).getAsJsonObject();
    assertThat(entity.get("entityID").getAsString()).isEqualTo("quuux");
    assertThat(entity.get("weight").getAsFloat()).isEqualTo(1f);

    assertThat(actual).containsExactly(new JsonPrimitive("foo"), new JsonPrimitive("bar"));
  }

  @Test
  public void testQueryEntitiesExact() throws Exception {
    Capture<JsonObject> payloadCapture = newCapture();
    
    JsonObject response = new JsonObject();
    JsonArray entities = new JsonArray();
    entities.add("foo");
    entities.add("bar");
    response.add("result", entities);
    
    expect(requestUtils.post(eq("/v2/dds/"), capture(payloadCapture))).andReturn(response);
    
    replayAll();
    
    QueryUtils queryUtils = createQueryUtils();    
    JsonArray actual = queryUtils.queryEntities("queryFoo", true, 42);
    
    verifyAll();
    
    JsonObject payload = payloadCapture.getValue();
    assertThat(payload.get("query").getAsString()).isEqualTo("queryFoo");
    assertThat(payload.get("queryType").getAsString()).isEqualTo("EXACT_MATCH");
    assertThat(payload.get("maxResults").getAsInt()).isEqualTo(42);
    assertThat(payload.get("queryMode")).isNull();

    assertThat(actual).containsExactly(new JsonPrimitive("foo"), new JsonPrimitive("bar"));
  }

  @Test
  public void testQueryEntitiesPartial() throws Exception {
    Capture<JsonObject> payloadCapture = newCapture();
    
    JsonObject response = new JsonObject();
    JsonArray entities = new JsonArray();
    entities.add("foo");
    entities.add("bar");
    response.add("result", entities);
    
    expect(requestUtils.post(eq("/v2/dds/"), capture(payloadCapture))).andReturn(response);
    
    replayAll();
    
    QueryUtils queryUtils = createQueryUtils();    
    JsonArray actual = queryUtils.queryEntities("queryFoo", false, 42);
    
    verifyAll();
    
    JsonObject payload = payloadCapture.getValue();
    assertThat(payload.get("query").getAsString()).isEqualTo("queryFoo");
    assertThat(payload.get("queryType").getAsString()).isEqualTo("PARTIAL_MATCH");
    assertThat(payload.get("maxResults").getAsInt()).isEqualTo(42);
    assertThat(payload.get("queryMode")).isNull();

    assertThat(actual).containsExactly(new JsonPrimitive("foo"), new JsonPrimitive("bar"));
  }

  private void verifyPayloadCommonFields(JsonObject payload, long start, long end) throws ParseException {
    assertThat(payload.get("apiKey").getAsString()).isEqualTo("apiKeyFoo");
    assertThat(payload.get("sessionID").getAsString()).isEqualTo("sessionIdFoo");

    String requestSentString = payload.get("requestSent").getAsString();
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    long requestSent = formatter.parse(requestSentString).getTime();
    
    long delta = 10;
    assertThat(requestSent).isBetween(start - delta, end + delta);
  }

  private QueryUtils createQueryUtils() {
    return new QueryUtils("apiKeyFoo", "sessionIdFoo", requestUtils);
  }
}
