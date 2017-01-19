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
import static org.easymock.EasyMock.expect;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class PrintUtilsTest extends EasyMockSupport {
  private ByteArrayOutputStream rawStream;
  private PrintStream printStream;
  private QueryUtils queryUtils;

  @Before
  public void setUp() {
    rawStream = new ByteArrayOutputStream();
    printStream = new PrintStream(rawStream);
    queryUtils = createMock(QueryUtils.class);
  }

  @Test
  public void testPrintSingle() throws UnsupportedEncodingException {
    replayAll();
    
    PrintUtils printUtils = createPrintUtils();
    printUtils.print("foo");

    verifyAll();
    
    assertThat(getPrinted()).isEqualTo("foo");
  }

  @Test
  public void testPrintMultiple() throws UnsupportedEncodingException {
    replayAll();
    
    PrintUtils printUtils = createPrintUtils();
    printUtils.print("foo");
    printUtils.print("bar");

    verifyAll();
    
    assertThat(getPrinted()).isEqualTo("foobar");
  }

  @Test
  public void testPrintlnSingle() throws UnsupportedEncodingException {
    replayAll();
    
    PrintUtils printUtils = createPrintUtils();
    printUtils.println("foo");

    verifyAll();
    
    assertThat(getPrinted()).isEqualTo("foo\n");
  }

  @Test
  public void testPrintlnMultiple() throws UnsupportedEncodingException {
    replayAll();
    
    PrintUtils printUtils = createPrintUtils();
    printUtils.println("foo");
    printUtils.println("bar");

    verifyAll();
    
    assertThat(getPrinted()).isEqualTo("foo\nbar\n");
  }

  @Test
  public void testPrintEntityDetailsIdLoadingOk() throws Exception {
    JsonObject entityDetail = new JsonObject();
    entityDetail.addProperty("entityID", "foo");
    entityDetail.addProperty("entityType", "bar");
    entityDetail.addProperty("displayName", "baz");
    entityDetail.addProperty("description", "quux");

    JsonArray entityDetails = new JsonArray();
    entityDetails.add(entityDetail);
    expect(queryUtils.queryEntities("foo", "ENTITY_ID", 1)).andReturn(entityDetails);

    replayAll();
    
    PrintUtils printUtils = createPrintUtils();
    printUtils.printEntityDetails("foo");

    verifyAll();

    String printed = getPrinted();
    assertThat(printed).contains("bar");
    assertThat(printed).contains("baz");
    assertThat(printed).contains("quux");
  }

  @Test
  public void testPrintEntityDetailsIdCached() throws Exception {
    JsonObject entityDetail = new JsonObject();
    entityDetail.addProperty("entityID", "foo");
    entityDetail.addProperty("entityType", "bar");
    entityDetail.addProperty("displayName", "baz");
    entityDetail.addProperty("description", "quux");

    JsonArray entityDetails = new JsonArray();
    entityDetails.add(entityDetail);
    expect(queryUtils.queryEntities("foo", "ENTITY_ID", 1)).andReturn(entityDetails);

    replayAll();
    
    PrintUtils printUtils = createPrintUtils();
    printUtils.printEntityDetails("foo");
    printUtils.printEntityDetails("foo");

    verifyAll();

    String printed = getPrinted();
    assertThat(printed).matches("(?m)(?s).*bar.*bar.*");
    assertThat(printed).matches("(?m)(?s).*baz.*baz.*");
    assertThat(printed).matches("(?m)(?s).*quux.*quux.*");
  }

  @Test
  public void testPrintEntityDetailsIdLoadingTwoElements() throws Exception {
    JsonObject entityDetail = new JsonObject();
    entityDetail.addProperty("entityID", "foo");
    entityDetail.addProperty("entityType", "bar");
    entityDetail.addProperty("displayName", "baz");
    entityDetail.addProperty("description", "quux");

    JsonArray entityDetails = new JsonArray();
    entityDetails.add(entityDetail);
    entityDetails.add(entityDetail);
    expect(queryUtils.queryEntities("foo", "ENTITY_ID", 1)).andReturn(entityDetails);

    replayAll();
    
    PrintUtils printUtils = createPrintUtils();
    printUtils.printEntityDetails("foo");

    verifyAll();

    String printed = getPrinted();
    assertThat(printed).contains("failed");
  }

  @Test
  public void testPrintEntityDetailsIdLoadingException() throws Exception {
    Exception e = new Exception("catch me");
    expect(queryUtils.queryEntities("foo", "ENTITY_ID", 1)).andThrow(e);

    replayAll();
    
    PrintUtils printUtils = createPrintUtils();
    printUtils.printEntityDetails("foo");

    verifyAll();

    String printed = getPrinted();
    assertThat(printed).contains("failed");
  }

  @Test
  public void testPrintEntityDetailsObject() throws UnsupportedEncodingException {
    JsonObject entityDetail = new JsonObject();
    entityDetail.addProperty("entityID", "foo");
    entityDetail.addProperty("entityType", "bar");
    entityDetail.addProperty("displayName", "baz");
    entityDetail.addProperty("description", "quux");

    replayAll();
    
    PrintUtils printUtils = createPrintUtils();
    printUtils.printEntityDetails(entityDetail);

    verifyAll();

    String printed = getPrinted();
    assertThat(printed).contains("bar");
    assertThat(printed).contains("baz");
    assertThat(printed).contains("quux");
  }

  @Test
  public void testPrintRecommendationOk() throws Exception {
    JsonObject recommendation = createRecommendation();

    replayAll();
    
    PrintUtils printUtils = createPrintUtils();
    printUtils.printRecommendation(recommendation);

    verifyAll();

    verifyBasicRecommendation();
  }

  @Test
  public void testPrintRecommendationRelatedContent() throws Exception {
    JsonArray relatedContent = new JsonArray();

    JsonObject relatedCapsule = new JsonObject();
    relatedCapsule.addProperty("relationship", "relationBar");
    relatedCapsule.add("contentItem", createRecommendation("Bar"));
    relatedContent.add(relatedCapsule);

    relatedCapsule = new JsonObject();
    relatedCapsule.addProperty("relationship", "relationBaz");
    relatedCapsule.add("contentItem", createRecommendation("Baz"));
    relatedContent.add(relatedCapsule);
    
    JsonObject recommendation = createRecommendation();
    recommendation.add("relatedContent", relatedContent);
    
    replayAll();
    
    PrintUtils printUtils = createPrintUtils();
    printUtils.printRecommendation(recommendation);

    verifyAll();

    verifyBasicRecommendation();
    
    String printed = getPrinted();
    assertThat(printed).contains("relationBar");
    assertThat(printed).contains("linkBar");
    assertThat(printed).contains("relationBaz");
    assertThat(printed).contains("linkBaz");
  }

  @Test
  public void testPrintRecommendationSocialInfo() throws Exception {
    JsonObject socialInfo = new JsonObject();
    socialInfo.addProperty("author", "authorFoo");
    JsonObject recommendation = createRecommendation();
    recommendation.add("socialInfo", socialInfo);
    
    replayAll();
    
    PrintUtils printUtils = createPrintUtils();
    printUtils.printRecommendation(recommendation);

    verifyAll();

    verifyBasicRecommendation();
    
    String printed = getPrinted();
    assertThat(printed).contains("authorFoo");
  }

  @Test
  public void testPrintRecommendationContributions() throws Exception {
    JsonArray contributions = new JsonArray();

    JsonObject contribution = new JsonObject();
    contribution.addProperty("contributorType", "typeFoo");
    contribution.addProperty("contributor", "contributorFoo");
    contribution.addProperty("value", 4711f);
    contributions.add(contribution);

    contribution = new JsonObject();
    contribution.addProperty("contributorType", "RELEVANCE_ENTITY");
    contribution.addProperty("contributor", "entityFoo");
    contribution.addProperty("value", 4712f);
    contributions.add(contribution);
    contributions.add(contribution);

    JsonObject recommendation = createRecommendation();
    recommendation.add("contributions", contributions);
    
    JsonObject entityDetail = new JsonObject();
    entityDetail.addProperty("entityID", "entityFoo");
    entityDetail.addProperty("entityType", "detailBar");
    entityDetail.addProperty("displayName", "detailBaz");
    entityDetail.addProperty("description", "detailQuux");

    JsonArray entityDetails = new JsonArray();
    entityDetails.add(entityDetail);
    expect(queryUtils.queryEntities("entityFoo", "ENTITY_ID", 1)).andReturn(entityDetails);

    replayAll();
    
    PrintUtils printUtils = createPrintUtils();
    printUtils.printRecommendation(recommendation);

    verifyAll();

    verifyBasicRecommendation();
    
    String printed = getPrinted();
    assertThat(printed).contains("contributorFoo");
    assertThat(printed).contains("typeFoo");
    assertThat(printed).contains("4711");
    assertThat(printed).contains("contributorFoo");
    assertThat(printed).contains("typeFoo");
    assertThat(printed).contains("4712");
    assertThat(printed).matches("(?m)(?s).*entityFoo.*entityFoo.*");
    assertThat(printed).matches("(?m)(?s).*detailBar.*detailBar.*");
    assertThat(printed).matches("(?m)(?s).*detailBaz.*detailBaz.*");
    assertThat(printed).matches("(?m)(?s).*detailQuux.*detailQuux.*");
  }

  private JsonObject createRecommendation() {
    return createRecommendation("Foo");
  }

  private JsonObject createRecommendation(String postfix) {
    JsonObject recommendation = new JsonObject();
    recommendation.addProperty("contentID", "contentID" + postfix);
    recommendation.addProperty("headline", "headline" + postfix);
    recommendation.addProperty("contentType", "contentType" + postfix);
    recommendation.addProperty("source", "source" + postfix);
    recommendation.addProperty("timestamp", "timestamp" + postfix);
    recommendation.addProperty("score", 42);
    recommendation.add("contributions", new JsonArray());
    recommendation.addProperty("summary", "summary" + postfix);
    recommendation.addProperty("linkURL", "link" + postfix);
    recommendation.add("relatedContent", new JsonArray());
    
    return recommendation;
  }

  private void verifyBasicRecommendation() throws UnsupportedEncodingException {
    String printed = getPrinted();
    assertThat(printed).contains("contentIDFoo");
    assertThat(printed).contains("headlineFoo");
    assertThat(printed).contains("contentTypeFoo");
    assertThat(printed).contains("sourceFoo");
    assertThat(printed).contains("42");
    assertThat(printed).contains("summaryFoo");
    assertThat(printed).contains("linkFoo");    
  }

  private String getPrinted() throws UnsupportedEncodingException {
    return rawStream.toString("UTF-8");
  }
  
  private PrintUtils createPrintUtils() {
    return new PrintUtils(queryUtils, printStream);
  }
}
