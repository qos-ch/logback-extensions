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

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * A special bean which may be defined in the Spring {@code ApplicationContext} to make the context available statically
 * to objects which, for whatever reason, cannot be wired up in Spring (for example, logging appenders which must be
 * defined in XML or properties files used to initialize the logging system).
 * <p/>
 * To use this holder, <i>exactly one</i> bean should be declared as follows:
 * <pre>
 *     &lt;bean class="ch.qos.logback.ext.spring.ApplicationContextHolder"/&gt;
 * </pre>
 * Note that no ID is necessary because this holder should always be used via its static accessors, rather than being
 * injected. Any Spring bean which wishes to access the {@code ApplicationContext} should not rely on this holder; it
 * should simply implement {@code ApplicationContextAware}.
 * <p/>
 * <b>WARNING: This object uses static memory to retain the ApplicationContext.</b>  This means this bean (and the
 * related configuration strategy) is only usable when no other Logback-enabled Spring applications exist in the same
 * JVM.
 *
 * @author Bryan Turner
 * @author Les Hazlewood
 * @since 0.1
 */
public class ApplicationContextHolder implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

    private static ApplicationContext applicationContext;
    private static volatile boolean refreshed;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        refreshed = true;
    }

    /**
     * Ensures that the {@code ApplicationContext} has been set <i>and</i> that it has been refreshed. The refresh
     * event is sent when the context has completely finished starting up, meaning all beans have been created and
     * initialized successfully.
     * <p/>
     * This method has a loosely defined relationship with {@link #getApplicationContext()}. When this method returns
     * {@code true}, calling {@link #getApplicationContext()} is guaranteed to return a non-{@code null} context which
     * has been completely initialized. When this method returns {@code false}, {@link #getApplicationContext()} may
     * return {@code null}, or it may return a non-{@code null} context which is not yet completely initialized.
     *
     * @return {@code true} if the context has been set and refreshed; otherwise, {@code false}
     */
    public static boolean hasApplicationContext() {
        return (refreshed && applicationContext != null);
    }

    /**
     * Retrieves the {@code ApplicationContext} set when Spring created and initialized the holder bean. If the
     * holder has not been created (see the class documentation for details on how to wire up the holder), or if
     * the holder has not been initialized, this accessor may return {@code null}.
     * <p/>
     * As a general usage pattern, callers should wrap this method in a check for {@link #hasApplicationContext()}.
     * That ensures both that the context is set and also that it has fully initialized. Using a context which has
     * not been fully initialized can result in unexpected initialization behaviors for some beans. The most common
     * example of this behavior is receiving unproxied references to some beans, such as beans which were supposed
     * to have transactional semantics applied by AOP. By waiting for the context refresh event, the likelihood of
     * encountering such behavior is greatly reduced.
     *
     * @return the set context, or {@code null} if the holder bean has not been initialized
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    /**
     * Returns a flag indicating whether the {@code ApplicationContext} has been refreshed. Theoretically, it is
     * possible for this method to return {@code true} when {@link #hasApplicationContext()} returns {@code false},
     * but in practice that is very unlikely since the bean for the holder should have been created and initialized
     * before the refresh event was raised.
     *
     * @return {@code true} if the context refresh event has been received; otherwise, {@code false}
     */
    public static boolean isRefreshed() {
        return refreshed;
    }
}

