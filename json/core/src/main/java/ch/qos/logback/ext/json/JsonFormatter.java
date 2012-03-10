/*
 * Copyright 2012 Ceki Gulcu, Les Hazlewood, et. al.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.qos.logback.ext.json;

import java.util.Map;

/**
 * A {@code JsonFormatter} formats a data {@link Map Map} into a JSON string.
 *
 * @author Les Hazlewood
 * @since 0.1
 */
public interface JsonFormatter {

    /**
     * Converts the specified map into a JSON string.
     *
     * @param m the map to be converted.
     * @return a JSON String representation of the specified Map instance.
     * @throws Exception if there is a problem converting the map to a String.
     */
    String toJsonString(Map m) throws Exception;
}
