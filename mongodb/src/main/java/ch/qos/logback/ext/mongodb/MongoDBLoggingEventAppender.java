/*
 * Copyright (C) 2012, QOS.ch. All rights reserved.
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
package ch.qos.logback.ext.mongodb;

import java.util.Date;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

/**
 * @author Christian Trutz
 * @author Tomasz Nurkiewicz
 * @since 0.1
 */
public class MongoDBLoggingEventAppender extends MongoDBAppenderBase<ILoggingEvent> {

    private boolean includeCallerData;

    @Override
    protected BasicDBObject toMongoDocument(ILoggingEvent event) {
        BasicDBObject logEntry = new BasicDBObject();
        logEntry.append("message", event.getFormattedMessage());
        logEntry.append("logger", event.getLoggerName());
        logEntry.append("thread", event.getThreadName());
        logEntry.append("timestamp", new Date(event.getTimeStamp()));
        logEntry.append("level", event.getLevel().toString());
        if (event.getMDCPropertyMap() != null && !event.getMDCPropertyMap().isEmpty()) {
            logEntry.append("mdc", event.getMDCPropertyMap());
        }
        if (includeCallerData) {
            logEntry.append("callerData", toDocument(event.getCallerData()));
        }
        if (event.getArgumentArray() != null && event.getArgumentArray().length > 0) {
            logEntry.append("arguments", event.getArgumentArray());
        }
        appendThrowableIfAvailable(logEntry, event);
        return logEntry;
    }

    private BasicDBList toDocument(StackTraceElement[] callerData) {
        final BasicDBList dbList = new BasicDBList();
        for (final StackTraceElement ste : callerData) {
            dbList.add(
                    new BasicDBObject()
                            .append("file", ste.getFileName())
                            .append("class", ste.getClassName())
                            .append("method", ste.getMethodName())
                            .append("line", ste.getLineNumber())
                            .append("native", ste.isNativeMethod()));
        }
        return dbList;
    }

    private void appendThrowableIfAvailable(BasicDBObject doc, ILoggingEvent event) {
        if (event.getThrowableProxy() != null) {
            final BasicDBObject val = toMongoDocument(event.getThrowableProxy());
            doc.append("throwable", val);
        }
    }

    private BasicDBObject toMongoDocument(IThrowableProxy throwable) {
        final BasicDBObject throwableDoc = new BasicDBObject();
        throwableDoc.append("class", throwable.getClassName());
        throwableDoc.append("message", throwable.getMessage());
        throwableDoc.append("stackTrace", toSteArray(throwable));
        if (throwable.getCause() != null) {
            throwableDoc.append("cause", toMongoDocument(throwable.getCause()));
        }
        return throwableDoc;
    }

    private String[] toSteArray(IThrowableProxy throwableProxy) {
        final StackTraceElementProxy[] elementProxies = throwableProxy.getStackTraceElementProxyArray();
        final int totalFrames = elementProxies.length - throwableProxy.getCommonFrames();
        final String[] stackTraceElements = new String[totalFrames];
        for (int i = 0; i < totalFrames; ++i)
            stackTraceElements[i] = elementProxies[i].getStackTraceElement().toString();
        return stackTraceElements;
    }

    public void setIncludeCallerData(boolean includeCallerData) {
        this.includeCallerData = includeCallerData;
    }

}
