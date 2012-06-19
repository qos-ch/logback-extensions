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
package ch.qos.logback.ext.eclipse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

/**
 * Tests the {@link EclipseLogAppender} class
 *
 * @author Anthony Trinh
 * @since 0.1
 */
public class EclipseLogAppenderTest {

    private EclipseLogAppenderWithMockPlatform appender = null;
    private PatternLayoutEncoder encoder = null;
    
    @Before
    public void before() {
        // create the encoder and start it so that it initializes inner parameters
        // needed by the appender
        encoder = new PatternLayoutEncoder();
        encoder.setPattern("%msg");
        encoder.start();
        assertNotNull(encoder.getLayout());
        
        appender = new EclipseLogAppenderWithMockPlatform();
        appender.setEncoder(encoder);
    }

    @Test
    public void testNullBundleName() {
        // when bundleName == null
        appender.setBundleName(null);
        appender.start();
        // then appender should still start because the field is optional
        assertTrue(appender.isStarted());
        
        // verify valid default bundle name
        assertNotNull(appender.getBundleName());
        assertEquals(EclipseLogAppender.DEFAULT_BUNDLE_SYMBOLIC_NAME, appender.getBundleName());
    }

    @Test
    public void testNotValidBundleName() {
        // when bundleName is not valid
        appender.setBundleName("notvalid");
        appender.start();
        // then appender should not start
        assertFalse(appender.isStarted());
    }

    @Test
    public void testValidBundleName() {
        // when bundleName is valid (name of existing Eclipse bundle)
        appender.setBundleName(EclipseLogAppender.DEFAULT_BUNDLE_SYMBOLIC_NAME);
        appender.start();
        // then appender should start
        assertTrue(appender.isStarted());
    }

    @Test
    public void testNullEncoder() {
        // when encoder not specified
        appender.setEncoder(null);
        appender.start();
        // then appender should not start
        assertFalse(appender.isStarted());
    }
    
    @Test
    public void testAppendInfo() {
        MockEclipseErrorLog log = (MockEclipseErrorLog)appender.getPlatform().getLog(null);
        List<IStatus> events = log.getLog();
        
        //TODO: write me...
    }
}

/**
 * EclipseLogAppender that contains a mock Eclipse Platform class
 * (to facilitate unit tests)
 */
class EclipseLogAppenderWithMockPlatform extends EclipseLogAppender {
    static private final IPlatform _platform = new IPlatform() {
        private final MockEclipseErrorLog log = new MockEclipseErrorLog();
        
        @Override
        public ILog getLog(Bundle bundle) {
            return log;
        }
        
        @Override
        public Bundle getBundle(String bundleName) {
            if (log.getBundle().getSymbolicName().equals(bundleName)) {
                return log.getBundle();
            }
            return null;
        }
    };
        
    @Override
    protected IPlatform getPlatform() {
        return _platform;
    }
}

/**
 * Mocks the Eclipse Error Log (to facilitate unit tests)
 */
class MockEclipseErrorLog implements ILog {
    static private final MockEclipseBundle BUNDLE = new MockEclipseBundle();
    private List<IStatus> log;
    
    @Override
    public void addLogListener(ILogListener listener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Bundle getBundle() {
        return BUNDLE;
    }

    @Override
    public void log(IStatus status) {
        log.add(status);
    }

    @Override
    public void removeLogListener(ILogListener listener) {
        // TODO Auto-generated method stub
        
    }
    
    public List<IStatus> getLog() {
        return log;
    }
}

/**
 * Mocks the Eclipse Bundle (to facilitate unit tests)
 */
class MockEclipseBundle implements Bundle {

    @Override
    public int getState() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void start(int options) throws BundleException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void start() throws BundleException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void stop(int options) throws BundleException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void stop() throws BundleException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void update(InputStream input) throws BundleException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void update() throws BundleException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void uninstall() throws BundleException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Dictionary<String, String> getHeaders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getBundleId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getLocation() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceReference[] getRegisteredServices() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceReference[] getServicesInUse() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasPermission(Object permission) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public URL getResource(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Dictionary<String, String> getHeaders(String locale) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSymbolicName() {
        return EclipseLogAppender.DEFAULT_BUNDLE_SYMBOLIC_NAME;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Enumeration<String> getEntryPaths(String path) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public URL getEntry(String path) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getLastModified() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Enumeration<URL> findEntries(String path, String filePattern,
            boolean recurse) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BundleContext getBundleContext() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(
            int signersType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Version getVersion() {
        // TODO Auto-generated method stub
        return null;
    }

// XXX: The methods below are required in newer Eclipse versions.
// The eclipse jar used by Maven doesn't recognize these methods.
//
//    @Override
//    public int compareTo(Bundle arg0) {
//        // TODO Auto-generated method stub
//        return 0;
//    }
//
//    @Override
//    public <A> A adapt(Class<A> type) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public File getDataFile(String filename) {
//        // TODO Auto-generated method stub
//        return null;
//    }

}
