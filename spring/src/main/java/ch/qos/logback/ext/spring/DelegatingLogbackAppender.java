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
package ch.qos.logback.ext.spring;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import java.util.List;

/**
 * A Logback {@code Appender} implementation which delegates the actual appending to a named bean contained in a Spring
 * {@code ApplicationContext}.
 * <p/>
 * This appender is similar in spirit to Spring's {@code DelegatingFilterProxy}, which allows servlet filters to be
 * created and wired in the ApplicationContext and then accessed in the filter chain. As with the filter proxy, the
 * delegating appender uses its own name to find the target appender in the context.
 * <p/>
 * Because the logging framework is usually started before the Spring context, this appender supports caching for
 * {@code ILoggingEvent}s which are received before the {@code ApplicationContext} is available. This caching has
 * 3 possible modes:
 * <ul>
 * <li><b>off</b> - Events are discarded until the {@code ApplicationContext} is available.</li>
 * <li><b>on</b> - Events are cached with strong references until the {@code ApplicationContext} is available, at
 * which time they will be forwarded to the delegate appender. In systems which produce substantial amounts of log
 * events while starting up the {@code ApplicationContext} this mode may result in heavy memory usage.</li>
 * <li><b>soft</b> - Events are wrapped in {@code SoftReference}s and cached until the {@code ApplicationContext}
 * is available. Memory pressure may cause the garbage collector to collect some or all of the cached events before
 * the {@code ApplicationContext} is available, so some or all events may be lost. However, in systems with heavy
 * logging, this mode may result in more efficient memory usage.</li>
 * </ul>
 * Caching is <b>{@code on}</b> by default, so strong references will be used for all events.
 * <p/>
 * An example of how to use this appender in {@code logback.xml}:
 * <pre>
 * &lt;appender name="<em>appenderBeanName</em>" class="ch.qos.logback.ext.spring.DelegatingLogbackAppender"/&gt;
 * </pre>
 * <p/>
 * Or, if specifying a different cache mode, e.g.:
 * <pre>
 * &lt;appender name="<em>appenderBeanName</em>" class="ch.qos.logback.ext.spring.DelegatingLogbackAppender"&gt;
 *     &lt;cacheMode&gt;soft&lt;/cacheMode&gt;
 * &lt;/appender&gt;
 * </pre>
 * Using this appender requires that the {@link ApplicationContextHolder} be included in the {@code ApplicationContext}.
 *
 * @author Bryan Turner
 * @since 0.1
 */
public class DelegatingLogbackAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private final Object lock;

    private String beanName;
    private ILoggingEventCache cache;
    private EventCacheMode cacheMode;
    private volatile Appender<ILoggingEvent> delegate;

    public DelegatingLogbackAppender() {
        cacheMode = EventCacheMode.ON;
        lock = new Object();
    }

    public void setCacheMode(String mode) {
        cacheMode = Enum.valueOf(EventCacheMode.class, mode.toUpperCase());
    }

    @Override
    public void start() {
        if (isStarted()) {
            return;
        }

        if (beanName == null || beanName.trim().isEmpty()) {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalStateException("A 'name' or 'beanName' is required for DelegatingLogbackAppender");
            }
            beanName = name;
        }
        cache = cacheMode.createCache();

        super.start();
    }

    @Override
    public void stop() {
        super.stop();

        if (cache != null) {
            cache = null;
        }
        if (delegate != null) {
            delegate.stop();
            delegate = null;
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        //Double-check locking here to optimize out the synchronization after the delegate is in place. This also has
        //the benefit of dealing with the race condition where 2 threads are trying to log and one gets the lock with
        //the other waiting and the lead thread sets the delegate, logs all cached events and then returns, allowing
        //the blocked thread to acquire the lock. At that time, the delegate is no longer null and the event is logged
        //directly to it, rather than being cached.
        if (delegate == null) {
            synchronized (lock) {
                //Note the isStarted() check here. If multiple threads are logging at the time the ApplicationContext
                //becomes available, the first thread to acquire the lock _may_ stop this appender if the context does
                //not contain an Appender with the expected name. If that happens, when the lock is released and other
                //threads acquire it, isStarted() will return false and those threads should return without trying to
                //use either the delegate or the cache--both of which will be null.
                if (!isStarted()) {
                    return;
                }
                //If we're still started either no thread has attempted to load the delegate yet, or the delegate has
                //been loaded successfully. If the latter, the delegate will no longer be null
                if (delegate == null) {
                    if (ApplicationContextHolder.hasApplicationContext()) {
                        //First, load the delegate Appender from the ApplicationContext. If it cannot be loaded, this
                        //appender will be stopped and null will be returned.
                        Appender<ILoggingEvent> appender = getDelegate();
                        if (appender == null) {
                            return;
                        }

                        //Once we have the appender, unload the cache to it.
                        List<ILoggingEvent> cachedEvents = cache.get();
                        for (ILoggingEvent cachedEvent : cachedEvents) {
                            appender.doAppend(cachedEvent);
                        }

                        //If we've found our delegate appender, we no longer need the cache.
                        cache = null;
                        delegate = appender;
                    } else {
                        //Otherwise, if the ApplicationContext is not ready yet, cache this event and wait
                        cache.put(event);

                        return;
                    }
                }
            }
        }

        //If we make it here, the delegate should always be non-null and safe to append to.
        delegate.doAppend(event);
    }

    private Appender<ILoggingEvent> getDelegate() {
        ApplicationContext context = ApplicationContextHolder.getApplicationContext();

        try {
            @SuppressWarnings("unchecked")
            Appender<ILoggingEvent> appender = context.getBean(beanName, Appender.class);
            appender.setContext(getContext());
            if (!appender.isStarted()) {
                appender.start();
            }
            return appender;
        } catch (NoSuchBeanDefinitionException e) {
            stop();
            addError("The ApplicationContext does not contain an Appender named [" + beanName +
                    "]. This delegating appender will now stop processing events.", e);
        }
        return null;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }
}

