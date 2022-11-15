package ch.qos.logback.ext.graalvm.appenders;

import org.graalvm.nativeimage.ImageInfo;

import ch.qos.logback.core.rolling.RollingFileAppender;

/**
 * GraalVM implementation of the {@link RollingFileAppender} with initialization of all files descriptors not in the build image phase.
 */
public class GraalVMRollingFileAppender<E> extends RollingFileAppender<E> {

    /**
     * In the build phase of the image appender should not be started.
     */
    @Override
    public void start() {
        if (!ImageInfo.inImageBuildtimeCode()) {
            super.start();
        }
    }

    /**
     * While getting the even in NOT build image phase async appender must be started - otherwise start it.
     *
     * @param eventObject event to append.
     */
    @Override
    public void doAppend(E eventObject) {
        if (!ImageInfo.inImageBuildtimeCode()) {
            if (!isStarted()) {
                synchronized (this) {
                    if (!isStarted()) {
                        super.start();
                    }
                }
            }
            super.doAppend(eventObject);
        }
    }
}
