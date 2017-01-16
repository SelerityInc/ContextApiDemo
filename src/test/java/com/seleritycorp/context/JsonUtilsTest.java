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

import org.junit.Test;

import com.google.gson.JsonObject;

public class JsonUtilsTest {
  @Test
  public void testGetAsStringNull() {
    JsonObject json = null;
    
    JsonUtils jsonUtils = new JsonUtils(); 
    String actual = jsonUtils.getAsString(json, "foo");
    
    assertThat(actual).contains("no proper");
    assertThat(actual).contains("foo");
  }

  @Test
  public void testGetAsStringMissingField() {
    JsonObject json = new JsonObject();
    json.addProperty("bar", "baz");
    
    JsonUtils jsonUtils = new JsonUtils(); 
    String actual = jsonUtils.getAsString(json, "foo");
    
    assertThat(actual).contains("no proper");
    assertThat(actual).contains("foo");
  }

  @Test
  public void testGetAsStringMatchString() {
    JsonObject json = new JsonObject();
    json.addProperty("foo", "bar");
    
    JsonUtils jsonUtils = new JsonUtils(); 
    String actual = jsonUtils.getAsString(json, "foo");
    
    assertThat(actual).isEqualTo("bar");
  }

  @Test
  public void testGetAsStringMatchNumber() {
    JsonObject json = new JsonObject();
    json.addProperty("foo", 3);
    
    JsonUtils jsonUtils = new JsonUtils(); 
    String actual = jsonUtils.getAsString(json, "foo");
    
    assertThat(actual).isEqualTo("3");
  }

  @Test
  public void testGetAsStringMatchObject() {
    JsonObject foo = new JsonObject();
    foo.addProperty("bar", "baz");
    JsonObject json = new JsonObject();
    json.add("foo", foo);
    
    JsonUtils jsonUtils = new JsonUtils(); 
    String actual = jsonUtils.getAsString(json, "foo");
  
    // An object is not a proper primitive value to extract.
    assertThat(actual).contains("no proper");
    assertThat(actual).contains("foo");
  }

  @Test
  public void testGetAsStringSubFieldNull() {
    JsonObject json = null;
    
    JsonUtils jsonUtils = new JsonUtils(); 
    String actual = jsonUtils.getAsString(json, "foo", "bar");
    
    assertThat(actual).contains("no proper");
    assertThat(actual).contains("foo->bar");
  }

  @Test
  public void testGetAsStringSubFieldMissingField() {
    JsonObject json = new JsonObject();
    
    JsonUtils jsonUtils = new JsonUtils(); 
    String actual = jsonUtils.getAsString(json, "foo", "bar");
    
    assertThat(actual).contains("no proper");
    assertThat(actual).contains("foo->bar");
  }

  @Test
  public void testGetAsStringSubFieldPrimitiveField() {
    JsonObject json = new JsonObject();
    json.addProperty("foo", "quux");

    JsonUtils jsonUtils = new JsonUtils(); 
    String actual = jsonUtils.getAsString(json, "foo", "bar");
    
    assertThat(actual).contains("no proper");
    assertThat(actual).contains("foo->bar");
  }

  @Test
  public void testGetAsStringSubFieldMissingSubField() {
    JsonObject json = new JsonObject();
    json.add("foo", new JsonObject());

    JsonUtils jsonUtils = new JsonUtils(); 
    String actual = jsonUtils.getAsString(json, "foo", "bar");
    
    assertThat(actual).contains("no proper");
    assertThat(actual).contains("foo->bar");
  }

  @Test
  public void testGetAsStringSubFieldMatchString() {
    JsonObject sub = new JsonObject();
    sub.addProperty("bar", "baz");
    JsonObject json = new JsonObject();
    json.add("foo", sub);

    JsonUtils jsonUtils = new JsonUtils(); 
    String actual = jsonUtils.getAsString(json, "foo", "bar");
    
    assertThat(actual).isEqualTo("baz");
  }

  @Test
  public void testGetAsStringSubFieldMatchNumber() {
    JsonObject sub = new JsonObject();
    sub.addProperty("bar", 3);
    JsonObject json = new JsonObject();
    json.add("foo", sub);

    JsonUtils jsonUtils = new JsonUtils(); 
    String actual = jsonUtils.getAsString(json, "foo", "bar");
    
    // 3 as string.
    assertThat(actual).isEqualTo("3");
  }

  @Test
  public void testGetAsStringSubFieldMatchObject() {
    JsonObject sub = new JsonObject();
    sub.add("bar", new JsonObject());
    JsonObject json = new JsonObject();
    json.add("foo", sub);

    JsonUtils jsonUtils = new JsonUtils(); 
    String actual = jsonUtils.getAsString(json, "foo", "bar");
    
    assertThat(actual).contains("no proper");
    assertThat(actual).contains("foo->bar");
  }
}
