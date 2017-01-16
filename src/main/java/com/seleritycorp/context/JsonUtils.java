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

import com.google.gson.JsonObject;

/**
 * Utility Methods for working with Json objects.  
 */
public class JsonUtils {
  /**
   * Gets the value that marks that the Json object does not have a given field.
   * 
   * @param field The field to get the empty marker for.
   * @return The empty marker.
   */
  private String getEmptyMarker(String field) {
    return "<no proper " + field + ">";    
  }

  /**
   * Gets a Json object's field as string.
   * 
   * @param object The object to get the field on.
   * @param field The field to get on the object
   * @return The fields value as string or the empty marker, if the object does not have the given
   *     field.
   */
  public String getAsString(JsonObject object, String field) {
    String ret;
    try {
      ret = object.get(field).getAsString();
    } catch (Exception e) {
      // Failed to get the field.
      ret = getEmptyMarker(field);
    }
    return ret;
  }

  /**
   * Gets a Json object's field of a field as string.
   * 
   * @param object The object to get the field on.
   * @param field The field to get on the object.
   * @param subfield The field to get on the top-level field object.
   * @return The subfield's value as string or the empty marker, if the object does not have the
   *     given field.
   */
  public String getAsString(JsonObject object, String field, String subfield) {
    String ret;
    try {
      ret = object.get(field).getAsJsonObject().get(subfield).getAsString();
    } catch (Exception e) {
      // Failed to get the field.
      ret = getEmptyMarker(field + "->" + subfield);
    }
    return ret;
  }
}
