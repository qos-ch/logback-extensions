package ch.qos.logback.ext.graalvm.appenders;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Lazy async appender with worker initialization on start phase.
 */
public class LazyAsyncAppender extends LazyAsyncAppenderBase<ILoggingEvent> {

    boolean includeCallerData = false;

    /**
     * Events of level TRACE, DEBUG and INFO are deemed to be discardable.
     *
     * @param event
     * @return true if the event is of level TRACE, DEBUG or INFO false otherwise.
     */
    protected boolean isDiscardable(ILoggingEvent event) {
        Level level = event.getLevel();
        return level.toInt() <= Level.INFO_INT;
    }

    protected void preprocess(ILoggingEvent eventObject) {
        eventObject.prepareForDeferredProcessing();
        if (includeCallerData)
            eventObject.getCallerData();
    }

    public boolean isIncludeCallerData() {
        return includeCallerData;
    }

    public void setIncludeCallerData(boolean includeCallerData) {
        this.includeCallerData = includeCallerData;
    }

}
