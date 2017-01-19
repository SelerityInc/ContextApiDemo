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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

/**
 * Methods for printing query results.
 */
public class PrintUtils {
  /**
   * The PrintStream to print everything to.
   */
  private final PrintStream printer;

  /**
   * Cache of loaded entities.
   * 
   * <p>This cache is used when printing details about score contributions. There, the same
   * entity is typically loaded againt and again. To avoid having to go back to the API again and
   * again, we cache and reuse results for a few minutes.
   */
  private final LoadingCache<String, JsonObject> entitiesDetailCache;

  private final JsonUtils jsonUtils;

  /**
   * Constructs a utility instance for printing.
   * 
   * @param queryUtils query layer used to load entity details for printing. 
   */
  public PrintUtils(QueryUtils queryUtils) {
    this(queryUtils, System.out);
  }

  /**
   * Constructs a utility instance for printing.
   * 
   * @param queryUtils query layer used to load entity details for printing.
   * @param printer The stream to print to
   */
  public PrintUtils(QueryUtils queryUtils, PrintStream printer) {
    this.printer = printer;
    this.jsonUtils = new JsonUtils();
    
    // Setting up cache for entity details that loads automatically
    entitiesDetailCache = CacheBuilder.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .maximumSize(1000)
        .build(new CacheLoader<String, JsonObject>(){
          @Override
          public JsonObject load(String entityId) throws Exception {
            JsonArray entityArray = queryUtils.queryEntities(entityId, "ENTITY_ID", 1);
            if (entityArray.size() != 1) {
              throw new Exception("Querying " + entityId + " did not yield exactly 1 result");
            }

            return entityArray.get(0).getAsJsonObject();
          }
        });
  }

  /**
   * Prints a string to this instance's writer without adding a trailing newline.
   * 
   * @param str The string to print
   */
  public void print(String str) {
    printer.print(str);    
  }

  /**
   * Prints a string to this instance's writer and adds a trailing newline.
   * 
   * @param str The string to print
   */
  public void println(String str) {
    printer.println(str);    
  }

  /**
   * Fetches and prints details for an entity
   * 
   * <p>Fetching uses a cache. So repeated fetching of the same entity will not unnecessarily
   * hammer the API.
   *
   * @param entityId The id of the entity to fetch and print
   */
  public void printEntityDetails(String entityId) {
    try {
      printEntityDetails(entitiesDetailCache.get(entityId));
    } catch (Exception e) {
      // Loading or formatting failed. But since the detailed information is not
      // crucial, we report the failure but otherwise ignore it.
      printer.println(" (failed to load details)");
    }
  }

  /**
   * Prints details for an entity.
   * 
   * @param details The details of the entity to print
   */
  public void printEntityDetails(JsonObject details) {
    printer.println(" (i.e.: " + jsonUtils.getAsString(details, "entityType")
        + ", " + jsonUtils.getAsString(details, "displayName")
        + ", " + jsonUtils.getAsString(details, "description")
        + ")");
  }

  /**
   * Prints a recommended content item to stdout.
   * 
   * @param recommendation The recommended content item
   * @throws Exception if errors occur
   */
  private void printContribution(JsonObject contribution) throws Exception {
    float value = contribution.get("value").getAsFloat();
    String type = jsonUtils.getAsString(contribution, "contributorType");
    String contributor = jsonUtils.getAsString(contribution, "contributor");

    String format = "  score-contribution: %1$.3f %2$-18s %3$s";
    printer.print(String.format(format, value, type, contributor));

    if ("RELEVANCE_ENTITY".equals(type)) {
      printEntityDetails(contributor);
    } else {
      printer.println();
    }
  }

  /**
   * Prints a recommended content item to stdout.
   * 
   * @param recommendation The recommended content item
   * @throws Exception if errors occur
   */
  public void printRecommendation(JsonObject recommendation) throws Exception {
    printer.println("");
    printer.println("* " + jsonUtils.getAsString(recommendation, "headline"));
    printer.println("");
    printer.println("  contentID: " + jsonUtils.getAsString(recommendation, "contentID"));
    printer.println("  contentType: " + jsonUtils.getAsString(recommendation, "contentType"));
    printer.println("  source: " + jsonUtils.getAsString(recommendation, "source"));
    printer.println("  timestamp: " + jsonUtils.getAsString(recommendation, "timestamp"));
    printer.println("  score: " + jsonUtils.getAsString(recommendation, "score"));
    JsonElement contributions = recommendation.get("contributions");
    if (contributions != null) {
      for (JsonElement contribution : contributions.getAsJsonArray()) {
        printContribution(contribution.getAsJsonObject());
      }
    }
    printer.println("  summary: " + jsonUtils.getAsString(recommendation, "summary"));
    printer.println("  socialInfo->author: " + jsonUtils.getAsString(recommendation,
        "socialInfo", "author"));
    printer.println("  linkURL: " + jsonUtils.getAsString(recommendation, "linkURL"));

    JsonArray relatedContentArray = recommendation.get("relatedContent").getAsJsonArray();
    for (JsonElement relatedContentElement: relatedContentArray) {
      JsonObject relatedContentObject = relatedContentElement.getAsJsonObject();
      String line = "  related content:";
      line += " " + jsonUtils.getAsString(relatedContentObject, "relationship");
      line += " " + jsonUtils.getAsString(relatedContentObject, "contentItem", "contentType");
      line += " " + jsonUtils.getAsString(relatedContentObject, "contentItem", "linkURL");
      
      printer.println(line);
    }
  }
}
