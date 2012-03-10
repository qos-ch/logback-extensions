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

import ch.qos.logback.core.LayoutBase;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

/**
 * @author Les Hazlewood
 * @author Pierre Queinnec
 * @since 0.1
 */
public abstract class JsonLayoutBase<E> extends LayoutBase<E> {

    public final static String CONTENT_TYPE = "application/json";

    protected boolean includeTimestamp;
    protected String timestampFormat;
    protected String timestampFormatTimezoneId;

    protected JsonFormatter jsonFormatter;

    public JsonLayoutBase() {
        this.includeTimestamp = true;
    }

    @Override
    public String doLayout(E event) {
        Map map = toJsonMap(event);
        if (map == null || map.isEmpty()) {
            return null;
        }
        JsonFormatter formatter = getJsonFormatter();
        if (formatter == null) {
            addError("JsonFormatter has not been configured on JsonLayout instance " + getClass().getName() + ".  Defaulting to map.toString().");
            return map.toString();
        }
        try {
            return formatter.toJsonString(map);
        } catch (Exception e) {
            addError("JsonFormatter failed.  Defaulting to map.toString().  Message: " + e.getMessage(), e);
            return map.toString();
        }
    }

    protected String formatTimestamp(long timestamp) {
        if (this.timestampFormat == null || timestamp < 0) {
            return String.valueOf(timestamp);
        }
        Date date = new Date(timestamp);
        DateFormat format = createDateFormat(this.timestampFormat);

        if (this.timestampFormatTimezoneId != null) {
            TimeZone tz = TimeZone.getTimeZone(this.timestampFormatTimezoneId);
            format.setTimeZone(tz);
        }

        return format(date, format);
    }

    protected DateFormat createDateFormat(String timestampFormat) {
        return new SimpleDateFormat(timestampFormat);
    }

    protected String format(Date date, DateFormat format) {
        return format.format(date);
    }

    protected abstract Map toJsonMap(E e);

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

    public boolean isIncludeTimestamp() {
        return includeTimestamp;
    }

    public void setIncludeTimestamp(boolean includeTimestamp) {
        this.includeTimestamp = includeTimestamp;
    }

    public JsonFormatter getJsonFormatter() {
        return jsonFormatter;
    }

    public void setJsonFormatter(JsonFormatter jsonFormatter) {
        this.jsonFormatter = jsonFormatter;
    }

    public String getTimestampFormat() {
        return timestampFormat;
    }

    public void setTimestampFormat(String timestampFormat) {
        this.timestampFormat = timestampFormat;
    }

    public String getTimestampFormatTimezoneId() {
        return timestampFormatTimezoneId;
    }

    public void setTimestampFormatTimezoneId(String timestampFormatTimezoneId) {
        this.timestampFormatTimezoneId = timestampFormatTimezoneId;
    }
}
