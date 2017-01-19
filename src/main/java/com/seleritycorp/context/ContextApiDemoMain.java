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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.logging.LogManager;

/**
 * Main entry point for demo of the Selerity Context API.
 */
public class ContextApiDemoMain {
  /**
   * Upper bound for how many entities a DDS query should return.
   */
  private static final int MAX_ENTITIES = 20;
  
  /**
   * Email address to which send questions. 
   */
  private static final String SUPPORT_EMAIL_ADDRESS = "support@selerityinc.com";

  @Option(name = "-help", usage = "Prints this help page", aliases = {"--help","-h","-?"})
  boolean showHelp = false;
  
  @Option(name = "-apiserver", metaVar = "URL", usage = "The Context API server to connect to")
  String apiServerRootUrl = "context-api-test.seleritycorp.com";

  @Option(name = "-apikey", metaVar = "API_KEY", usage = "The key used for the API connections")
  String apiKey = null;

  @Option(name = "-sessionid", metaVar = "SESSION_ID", usage = "The session id to use for requests")
  String sessionId = "<automatic>";

  @Option(name = "-sources", usage = "Output the entitled sources. In this mode, no query for "
      + "content is made.")
  boolean requestedSourcesQuery = false;
  
  @Option(name = "-query", metaVar = "QUERY", usage = "Query only for content items of the given "
      + "entity (E.g.: AAPL, Google)")
  String query = "";
  
  @Option(name = "-exact", usage = "When matching entities, consider only exact matches, instead of"
      + "also partial matches")
  boolean exactMatching = false;

  @Option(name = "-v", usage = "Increases the verbosity. Supply multiple times to increase "
      + "verbosity further and further.")
  boolean[] verboseCollector = {};

  @Option(name = "-querytype", metaVar = "TYPE", usage = "The type of query to make. One of\n"
      + "  - FEED            <- use to keep on top of latest\n"
      + "                       breaking news\n"
      + "  - RECOMMENDATION  <- use to get 'up to speed'\n"
      + "                       quickly\n"
      + "  - SURVEY\n"
      + "  - SEARCH\n"
      + "  - DISCOVERY\n"
      + "(No AUTHOR query, as this demo does not allow to specify authors)")
  String queryType = "FEED";

  @Option(name = "-contributions", metaVar = "MODE", usage = "Allows to expose which factors "
      + "contributed to the score of a found content item. Can be one of:\n"
      + "  - NONE     <- does not show contributions\n"
      + "  - DIRECT   <- shows direct contributions\n"
      + "  - ALL      <- shows all contributions\n")
  String contributions = "NONE";

  @Option(name = "-pause", metaVar = "SECS", hidden = true, usage = "Pause in seconds between "
      + "content update queries")
  long pauseSeconds = 30;

  private QueryUtils queryUtils;
  private PrintUtils printUtils;
  
  /**
   * Handles argument parsing.
   *
   * @param args arguments to use as command line arguments.
   */
  @SuppressFBWarnings(value = "DM_EXIT",
      justification = "Hard exiting on invalid arguments is wanted and desired")
  private void parseArgs(final String[] args) {    
    CmdLineParser parser = new CmdLineParser(this);
    try {
      parser.parseArgument(args);
    } catch (CmdLineException e) {
      // Parsing the command line arguments failed. So we print a help screen
      System.err.println(e.getMessage());
      System.err.println();
      printUsage(parser, System.err);
      System.exit(1);
    }
    
    if (showHelp) {
      printUsage(parser, System.out);
      System.exit(1);
    }
    
    updateLogging();

    // Making sure we to avoid obviously wrong api keys.
    if (apiKey == null || apiKey.isEmpty()) {
      System.err.println("No usable api key given. Please run the demo command with\n"
          + "\n"
          + "  -apikey INSERT-YOUR-API-KEY-HERE\n"
          + "\n"
          + "If you have not yet gotten an API key, get in touch with us at "
          + SUPPORT_EMAIL_ADDRESS);
      System.exit(1);
    }

    // Fixing up apiServerRootUrl to be a proper URL.
    if (!apiServerRootUrl.contains("://")) {
      apiServerRootUrl = "https://" + apiServerRootUrl;
    }
    
    // Making sure, we're on a good session
    if (sessionId == null || sessionId.isEmpty() || "<automatic>".equals(sessionId)) {
      sessionId = UUID.randomUUID().toString();
    }

    // Fixing up queryType
    switch (queryType) {
      case "FEED":
      case "RECOMMENDATION":
      case "SURVEY":
      case "SEARCH":
      case "DISCOVERY":
        break;
      default:
        System.err.println("Unknown query type " + queryType + ". Switching to FEED.");
        queryType = "FEED";
    }
  }

  /**
   * Updates logging configuration to reflect verbosity settings.  
   */
  private void updateLogging() {
    int verbosity = (verboseCollector == null) ? 0 : verboseCollector.length;

    if (verbosity > 0) {
      LogManager logManager = LogManager.getLogManager();
      String globalLevel = (verbosity <= 2) ? "INFO" : "FINEST";
      String comSeleritycorpLevel = (verbosity <= 1) ? "FINE" : "FINEST";
      String configString = "\n"
          + ".level=" + globalLevel + "\n"
          + "com.seleritycorp.level=" + comSeleritycorpLevel + "\n"
          + "handlers=java.util.logging.ConsoleHandler\n"
          + "java.util.logging.ConsoleHandler.level = FINEST\n"
          + "java.util.logging.SimpleFormatter.format=%1$tFT%1$tT.%1$tL %4$s %2$s - %5$s%6$s%n";
      InputStream configStream = new ByteArrayInputStream(configString.getBytes(
          StandardCharsets.UTF_8));

      try {
        logManager.readConfiguration(configStream);
      } catch (SecurityException | IOException e) {
        System.err.print("Failed to update logging config. " + e.toString());
      }
    }
  }

  /**
   * Prints usage information to a given stream.
   * 
   * @param parser The parser for command line options
   * @param printer The stream to print usage information to
   */
  private void printUsage(CmdLineParser parser, PrintStream printer) {
    printer.println("\nUsage:\n");
    printer.print("  run-demo.sh ");
    parser.printSingleLineUsage(printer);
    printer.println();

    printer.println();
    
    printer.println("The following options are available:");
    printer.println();

    parser.printUsage(printer);

    printer.println("\n"
        + "\n"
        + "Examples:\n"
        + "\n"
        + "  ./run-demo.sh -apikey REPLACE_WITH_YOUR_API_KEY\n"
        + "\n"
        + "The above command will query and show the latest breaking news.\n"
        + "\n"
        + "\n"
        + "  ./run-demo.sh -apikey REPLACE_WITH_YOUR_API_KEY -querytype RECOMMENDATION\n"
        + "\n"
        + "The above command will query and show the most important recent content.\n"
        + "\n"
        + "\n"
        + "  ./run-demo.sh -apikey REPLACE_WITH_YOUR_API_KEY -query Google\n"
        + "\n"
        + "The above command will query and show the latest breaking news for items relating\n"
        + "to Google. Even partial matches like the Twitter username @googleventures are\n"
        + "considered.\n"
        + "\n"
        + "\n"
        + "  ./run-demo.sh -apikey REPLACE_WITH_YOUR_API_KEY -query Google -exact\n"
        + "\n"
        + "The above command will query and show the latest breaking news for items relating\n"
        + "to entities that exactly match Google (i.e.: The company itself and the Google\n"
        + "twitter account). It will not consider content that is only relevant to entities\n"
        + "partially matching Google. So for example the Twitter username @googleventures is\n"
        + "not considered.\n"
        + "\n"
        + "\n"
        + "  ./run-demo.sh -apikey REPLACE_WITH_YOUR_API_KEY -query Google -contributions ALL\n"
        + "\n"
        + "The above command will query and show the latest breaking news for items relating\n"
        + "to Google (also considering partial matches), and gives details on which aspects\n"
        + "influenced the scoring.\n"
        + "\n"
        + "\n"
        + "  ./run-demo.sh -apikey REPLACE_WITH_YOUR_API_KEY -sources\n"
        + "\n"
        + "The above command will query and show the sources that your API key is entitled for.\n"
        + "\n"
        + "\n"
        + "\n"
        + "Questions/Support:\n"
        + "\n"
        + "If you run into issues or have questions, please let us know at\n"
        + SUPPORT_EMAIL_ADDRESS);
  }

  /**
   * Queries for and prints entitled sources.
   *
   * @throws Exception, if any error occurs.
   */
  private void printEntitledSources() throws Exception {
    JsonArray sources = queryUtils.queryEntitledSources();

    if (sources.size() == 0) {
      printUtils.println("API key is not entitled for any source.");
    } else {    
      printUtils.println("API key is entitled for the following sources:");
      for (JsonElement source : sources) {
        printUtils.println("* " + source.getAsString());
      }
    }
  }

  /**
   * Resolves the query to entities, dumps information about them, and yields the entity ids. 
   * 
   * <p/>Queries DDS for entities that match the query command line argument. If the command line
   * arguments requested exact matching, exact matching is requested from DDS, otherwise partial
   * matching is performed.
   * 
   * </p>Only up to {@link #MAX_ENTITIES} are fetched from DDS. 
   *
   * @return the entity ids of the entities for the query
   * @throws Exception, if any error occurs
   */
  private Iterable<String> resolveQueryEntityIds() throws Exception {
    List<String> entityIds = new LinkedList<>();

    if (query != null && !query.isEmpty()) {
      String entityQueryMode = exactMatching ? "EXACT_MATCH" : "PARTIAL_MATCH";
      JsonArray results = queryUtils.queryEntities(query, entityQueryMode, MAX_ENTITIES);
    
      printUtils.println("Query for '" + query + "' will look for those entities:");
      for (JsonElement resultElement : results) {
        JsonObject result = resultElement.getAsJsonObject();
        String id = result.getAsJsonPrimitive("entityID").getAsString();
      
        printUtils.print("* " + id);
        printUtils.printEntityDetails(result);
      
        entityIds.add(id);
      }
    }
    
    return entityIds;
  }

  /**
   * Waits a bit before asking API for updates.
   * 
   * @throws InterrutedException upon Thread interruption.
   */
  private void pauseBeforeUpdate() throws InterruptedException {
    try {
      printUtils.println("Sleeping for " + pauseSeconds + " seconds before asking for updated "
          + "content items");
      Thread.sleep(pauseSeconds * 1000);
    } catch (InterruptedException e) {
      throw e;
    }    
  }

  /**
   * Performs intitial query, performs update endlessly, and prints new content items.
   * 
   * @throws Exception, if any errors occur
   */
  private void queryWithUpdates() throws Exception { 
    // First, we resolve the query to entity ids.
    Iterable<String> entityIds = resolveQueryEntityIds();
    
    int batchSize = 10; // requesting only up to 10 items per query
    Deque<String> seenContentIds = new LinkedList<>(); // Used to filter seen items from updates
    JsonArray recommendations; // Will hold the recommended content items of the last query
    boolean isInitial = true; // Whether or not to perform an INITIAL or UPDATE query.
    while (true) {
      // Perform the query
      recommendations = queryUtils.queryRecommendations(queryType, isInitial, batchSize,
          contributions, entityIds);
      isInitial = false; // From now on, all queries are UPDATES

      // Dumping the response
      printUtils.println("Received " + recommendations.size() + " recommendations. "
          + "(Printing only new ones.)");
      for (JsonElement recommendationElement : recommendations) {
        JsonObject recommendation = recommendationElement.getAsJsonObject();
        String contentId = recommendation.getAsJsonPrimitive("contentID").getAsString();
        if (contentId != null) {
          if (!seenContentIds.contains(contentId)) {
            // Content item has not been seen, so we print it.
            printUtils.printRecommendation(recommendation);
            
            // And we remember that we saw that content item.
            seenContentIds.addFirst(contentId);
            
            // To avoid keeping too many items in memory, we prune old items. 
            if (seenContentIds.size() > 2 * batchSize) {
              seenContentIds.removeLast();                
            }
          }
        }
      }

      // Backing-off a bit before the next query to avoid hammering servers.
      pauseBeforeUpdate();
    }   
  }
 
  /**
   * Runs Context API demo.
   *
   * @param args arguments to use as command line arguments.
   */
  public void doMain(final String[] args) {
    parseArgs(args);

    // Setting up the endpoint config
    RequestUtils requestUtils = new RequestUtils(apiServerRootUrl);

    // Setting up query helpers for the endpoint
    queryUtils = new QueryUtils(apiKey, sessionId, requestUtils);

    // Finally, setting the print helpers
    printUtils = new PrintUtils(queryUtils);
    
    printUtils.println("Using Selerity Context API server at " + apiServerRootUrl);

    // Now that setup is complete, start the queries. 
    try {
      if (requestedSourcesQuery) {
        printEntitledSources();
      } else {
        queryWithUpdates();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Spawns a demo object and runs it.
   * 
   * @param args arguments to use as command line arguments.
   */
  public static void main(final String[] args) {
    (new ContextApiDemoMain()).doMain(args);
  }
}
