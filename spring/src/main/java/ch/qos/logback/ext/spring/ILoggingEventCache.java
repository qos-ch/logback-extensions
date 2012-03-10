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

import java.util.List;

/**
 * Abstraction interface for defining a cache for Logback {@code ILoggingEvent} instances.
 *
 * @author Bryan Turner
 * @since 0.1
 */
public interface ILoggingEventCache {

    /**
     * Retrieves a list containing 0 or more cached {@code ILoggingEvent}s.
     * <p/>
     * Note: Implementations of this method must return a non-{@code null} list, even if the list is empty, and the
     * returned list must not contain any {@code null} elements. If the caching implementation has discarded any of
     * the events that were passed to {@link #put(ILoggingEvent)}, they should be completely omitted from the event
     * list returned.
     *
     * @return a non-{@code null} list containing 0 or more cached events
     */
    List<ILoggingEvent> get();

    /**
     * Stores the provided event in the cache.
     * <p/>
     * Note: Implementations are free to "store" the event in a destructive or potentially-destructive way. This means
     * the "cache" may actually just discard any events it receives, or it may wrap them in a {@code SoftReference} or
     * other {@code java.lang.ref} type which could potentially result in the event being garbage collected before the
     * {@link #get()} method is called.
     *
     * @param event the event to cache
     */
    void put(ILoggingEvent event);
}
