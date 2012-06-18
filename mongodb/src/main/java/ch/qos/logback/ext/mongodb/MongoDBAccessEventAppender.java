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

import ch.qos.logback.access.spi.IAccessEvent;

import com.mongodb.BasicDBObject;

/**
 * @author Tomasz Nurkiewicz
 * @author Christian Trutz
 * @since 0.1
 */
public class MongoDBAccessEventAppender extends MongoDBAppenderBase<IAccessEvent> {

    private boolean serverName = true;
    private boolean requestUri = true;
    private boolean requestProtocol = true;
    private boolean requestMethod = true;
    private boolean requestPostContent = true;
    private boolean requestSessionId = true;
    private boolean requestUserAgent = true;
    private boolean requestReferer = true;
    private boolean remoteHost = true;
    private boolean remoteUser = true;
    private boolean remoteAddr = true;
    private boolean responseContentLength = true;
    private boolean responseStatusCode = true;

    @Override
    protected BasicDBObject toMongoDocument(IAccessEvent event) {
        final BasicDBObject doc = new BasicDBObject();
        doc.append("timeStamp", new Date(event.getTimeStamp()));
        if (serverName)
            doc.append("serverName", event.getServerName());
        addRemote(doc, event);
        addRequest(doc, event);
        addResponse(doc, event);
        return doc;
    }

    private void addRemote(BasicDBObject parent, IAccessEvent event) {
        final BasicDBObject remote = new BasicDBObject();
        if (remoteHost)
            remote.append("host", event.getRemoteHost());
        final String remoteUserName = event.getRemoteUser();
        if (remoteUser && remoteUserName != null && !remoteUserName.equals("-")) {
            remote.append("user", remoteUserName);
        }
        if (remoteAddr)
            remote.append("addr", event.getRemoteAddr());
        if (!remote.isEmpty())
            parent.put("remote", remote);
    }

    private void addRequest(BasicDBObject parent, IAccessEvent event) {
        final BasicDBObject request = new BasicDBObject();
        final String uri = event.getRequestURI();
        if (requestUri && uri != null && !uri.equals("-")) {
            request.append("uri", uri);
        }
        if (requestProtocol)
            request.append("protocol", event.getProtocol());
        if (requestMethod)
            request.append("method", event.getMethod());
        final String requestContent = event.getRequestContent();
        if (requestPostContent && requestContent != null && !requestContent.equals("")) {
            request.append("postContent", requestContent);
        }
        final String jSessionId = event.getCookie("JSESSIONID");
        if (requestSessionId && !jSessionId.equals("-"))
            request.append("sessionId", jSessionId);
        final String userAgent = event.getRequestHeader("User-Agent");
        if (requestUserAgent && !userAgent.equals("-"))
            request.append("userAgent", userAgent);
        final String referer = event.getRequestHeader("Referer");
        if (requestReferer && !referer.equals("-"))
            request.append("referer", referer);
        if (!request.isEmpty())
            parent.put("request", request);
    }

    private void addResponse(BasicDBObject doc, IAccessEvent event) {
        final BasicDBObject response = new BasicDBObject();
        if (responseContentLength)
            response.append("contentLength", event.getContentLength());
        if (responseStatusCode)
            response.append("statusCode", event.getStatusCode());
        if (!response.isEmpty())
            doc.append("response", response);
    }

    public void setServerName(boolean serverName) {
        this.serverName = serverName;
    }

    public void setRequestUri(boolean requestUri) {
        this.requestUri = requestUri;
    }

    public void setRequestProtocol(boolean requestProtocol) {
        this.requestProtocol = requestProtocol;
    }

    public void setRequestMethod(boolean requestMethod) {
        this.requestMethod = requestMethod;
    }

    public void setRequestPostContent(boolean requestPostContent) {
        this.requestPostContent = requestPostContent;
    }

    public void setRequestSessionId(boolean requestSessionId) {
        this.requestSessionId = requestSessionId;
    }

    public void setRequestUserAgent(boolean requestUserAgent) {
        this.requestUserAgent = requestUserAgent;
    }

    public void setRequestReferer(boolean requestReferer) {
        this.requestReferer = requestReferer;
    }

    public void setRemoteHost(boolean remoteHost) {
        this.remoteHost = remoteHost;
    }

    public void setRemoteUser(boolean remoteUser) {
        this.remoteUser = remoteUser;
    }

    public void setRemoteAddr(boolean remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    public void setResponseContentLength(boolean responseContentLength) {
        this.responseContentLength = responseContentLength;
    }

    public void setResponseStatusCode(boolean responseStatusCode) {
        this.responseStatusCode = responseStatusCode;
    }

}
