# ContextApiDemo

Sample code that shows how to use the Selerity Context API.

* [Installation](#installation)
* [Examples](#examples)
* [JavaDoc](#javadoc)
* [Questions/Support](#questionssupport)

[![Build Status](https://travis-ci.org/SelerityInc/ContextApiDemo.svg?branch=master)](https://travis-ci.org/SelerityInc/ContextApiDemo)

## Installation

* Request an API key from support@seleritycorp.com for the Selerity Context API
* Install Java 7 (or newer)
* Install Maven
* To query and show items about Google, run the following command (`run-demo.sh` builds the application upon need)
   ```
./run-demo.sh -apikey REPLACE_WITH_YOUR_API_KEY -query Google
```

* To see all options along with some sample queries, run
   ```
./run-demo.sh -help
```

## Examples

In the following examples, replace `REPLACE_WITH_YOUR_API_KEY` with your Selerity Context API key.

```
./run-demo.sh -help
```

The above command will list all available options.


```
./run-demo.sh -apikey REPLACE_WITH_YOUR_API_KEY
```

The above command will query and show the latest breaking news.

```
./run-demo.sh -apikey REPLACE_WITH_YOUR_API_KEY -querytype RECOMMENDATION
```

The above command will query and show the most important recent content.


```
./run-demo.sh -apikey REPLACE_WITH_YOUR_API_KEY -query Google
```

The above command will query and show the latest breaking news for items relating
to Google. Even partial matches like the Twitter username @googleventures are
considered.


```
./run-demo.sh -apikey REPLACE_WITH_YOUR_API_KEY -query Google -exact
```

The above command will query and show the latest breaking news for items relating
to entities that exactly match Google (i.e.: The company itself and the Google
twitter account). It will not consider content that is only relevant to entities
partially matching Google. So for example the Twitter username @googleventures is
not considered.


```
./run-demo.sh -apikey REPLACE_WITH_YOUR_API_KEY -query Google -contributions ALL
```

The above command will query and show the latest breaking news for items relating
to Google (also considering partial matches), and gives details on which aspects
influenced the scoring.


```
./run-demo.sh -apikey REPLACE_WITH_YOUR_API_KEY -query Google -live
```

The above command will query and show the latest breaking news for items relating toGoogle (including partial matches) while skipping pauses between updates. That way,new items are received right away.


```
./run-demo.sh -apikey REPLACE_WITH_YOUR_API_KEY -sources
```

The above command will query and show the sources that your API key is entitled for.


## JavaDoc

JavaDoc for this package is available at https://doc.seleritycorp.com/javadoc/com.seleritycorp.context/ContextApiDemo/master/

JavaDoc for the whole Selerity platform is at https://doc.seleritycorp.com/javadoc/platform/master/

## Questions/Support

If you run into issues or have questions, please let us know at support@seleritycorp.com
