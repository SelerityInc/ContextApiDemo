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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Context API methods.
 */
public class QueryUtils {
  /**
   * Endpoint for source queries.
   */
  private static String PATH_SOURCES = "/v2/sources";

  /**
   * Endpoint for queries for content.
   */
  private static String PATH_QUERY = "/v2/query";

  /**
   * Endpoint for entity data and disambiguation.
   */
  private static String PATH_DDS = "/v2/dds/";
  
  private final String apiKey;
  private final String sessionId;
  private final RequestUtils requestUtils;
  private final SimpleDateFormat timestampFormat;

  /**
   * Constructs a query layer for a given api key and session id on top of the basic request layer.
   * 
   * @param apiKey api key to use for queries.
   * @param sessionId session id to use for queries.
   * @param requestUtils request layer to perform the queries on.
   */
  public QueryUtils(String apiKey, String sessionId, RequestUtils requestUtils) {
    this.apiKey = apiKey;
    this.sessionId = sessionId;
    this.requestUtils = requestUtils;

    // Requests typically require timestamps in ISO format. So we prepare a formatter for those.
    this.timestampFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    this.timestampFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  /**
   * Gets the current timestamp in ISO format at millisecond precision.
   *
   * @return current timestamp in ISO format at millisecond precision.
   */
  private String getTimestamp() {
    return timestampFormat.format(System.currentTimeMillis());
  }

  /**
   * Constructs a base query Json object
   * 
   * @return The constructed base query Json object. 
   */
  private JsonObject buildQueryStub() {
    JsonObject ret = new JsonObject();
    ret.addProperty("apiKey", apiKey);
    ret.addProperty("sessionID", sessionId);
    ret.addProperty("requestSent", getTimestamp());
    return ret;
  }

  /**
   * Queries for sources that are entitled for the api key
   * 
   * @return The entitled sources. Please find the structure of the array elements in the
   *     Selerity Context API documentation.
   * @throws Exception if an error occurs.
   */
  public JsonArray queryEntitledSources() throws Exception {
    JsonObject query = buildQueryStub();
    
    JsonObject response = requestUtils.post(PATH_SOURCES, query);

    return response.get("sources").getAsJsonArray();
  }

  /**
   * Queries for content item recommendations.
   * 
   * @param queryType The type of query to perform FEED, RECOMMENDATION, ...
   * @param isInitial If true, an INITIAL query is made. Otherwise, an UPDATE query is made.
   * @param numItems The maximum number of items to return.
   * @param contributionMode Whether or not to request information about score contributions.
   *     NONE does not request any information. DIRECT request information only about direct
   *     contributors. ALL requests information about all contributors.
   * @param entityIds The entity ids to search for. Can be the empty list to avoid filtering to
   *     entities.
   * @return The recommended content items. Please find the structure of the array elements in the
   *     Selerity Context API documentation.
   * @throws Exception if errors occur
   */
  public JsonArray queryRecommendations(String queryType, boolean isInitial, int numItems,
      String contributionMode, Iterable<String> entityIds) throws Exception {
    JsonObject parameters = new JsonObject();
    parameters.addProperty("queryType", queryType);
    parameters.addProperty("queryMode", isInitial ? "INITIAL" : "UPDATE");
    parameters.addProperty("numItems", numItems);
    parameters.addProperty("contributionMode", contributionMode);

    JsonArray entities = new JsonArray();
    for (String entityId : entityIds) {
      JsonObject weightedEntity = new JsonObject();
      weightedEntity.addProperty("entityID", entityId);
      weightedEntity.addProperty("weight", 1f);
      entities.add(weightedEntity);
    }
    
    JsonObject interests = new JsonObject();
    if (entities.size() > 0) {
      interests.add("entities", entities);
    }

    JsonObject query = buildQueryStub();
    query.add("parameters", parameters);
    query.add("interests", interests);

    JsonObject response = requestUtils.post(PATH_QUERY, query);

    return response.get("recommendations").getAsJsonArray();
  }

  /**
   * Query for entity information.
   * 
   * @param query The string to query DDS for
   * @param queryType One of EXACT_MATCH, PARTIAL_MATCH, and ENTITY_ID.
   * @param maxResults The maximum numbers of results to request
   * @return The found entities. Please find the structure of the array elements in the Selerity
   *     Context API documentation.
   * @throws Exception if errors occur.
   */
  public JsonArray queryEntities(String query, String queryType, int maxResults)
      throws Exception {
    JsonObject queryObj = new JsonObject();
    queryObj.addProperty("query", query);
    queryObj.addProperty("queryType", queryType);
    queryObj.addProperty("maxResults", maxResults);

    JsonObject response = requestUtils.post(PATH_DDS, queryObj);

    return response.get("result").getAsJsonArray();
  }
}
