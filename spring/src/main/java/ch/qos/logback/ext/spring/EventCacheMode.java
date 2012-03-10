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

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Bryan Turner
 * @since 0.1
 */
public enum EventCacheMode {

    OFF {
        @Override
        public ILoggingEventCache createCache() {
            return new ILoggingEventCache() {

                @Override
                public List<ILoggingEvent> get() {
                    return Collections.emptyList();
                }

                @Override
                public void put(ILoggingEvent event) {
                    //When caching is off, events are discarded as they are received
                }
            };
        }
    },
    ON {
        @Override
        public ILoggingEventCache createCache() {
            return new ILoggingEventCache() {

                private List<ILoggingEvent> events = new ArrayList<ILoggingEvent>();

                @Override
                public List<ILoggingEvent> get() {
                    List<ILoggingEvent> list = Collections.unmodifiableList(events);
                    events = null;
                    return list;
                }

                @Override
                public void put(ILoggingEvent event) {
                    events.add(event);
                }
            };
        }
    },
    SOFT {
        @Override
        public ILoggingEventCache createCache() {
            return new ILoggingEventCache() {

                private List<SoftReference<ILoggingEvent>> references = new ArrayList<SoftReference<ILoggingEvent>>();

                @Override
                public List<ILoggingEvent> get() {
                    List<ILoggingEvent> events = new ArrayList<ILoggingEvent>(references.size());
                    for (SoftReference<ILoggingEvent> reference : references) {
                        ILoggingEvent event = reference.get();
                        if (event != null) {
                            events.add(event);
                        }
                    }
                    references = null;
                    return Collections.unmodifiableList(events);
                }

                @Override
                public void put(ILoggingEvent event) {
                    references.add(new SoftReference<ILoggingEvent>(event));
                }
            };
        }
    };

    public abstract ILoggingEventCache createCache();
}
