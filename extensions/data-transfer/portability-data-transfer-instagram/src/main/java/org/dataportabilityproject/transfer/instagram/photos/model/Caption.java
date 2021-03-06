/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dataportabilityproject.transfer.instagram.photos.model;

/** DataModel for a Caption object in the Instagram API. Instantiated by JSON mapping. */
public final class Caption {

  private String created_time;

  private String id;

  private String text;

  public String getCreated_time() {
    return created_time;
  }

  public String getId() {
    return id;
  }

  public String getText() {
    return text;
  }
}

