/**
 * Copyright (C) 2014 The logback-extensions developers (logback-user@qos.ch)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.qos.logback.ext.loggly;

import java.util.ArrayList;

/**
 * Wraps a {@link java.util.ArrayList} of {@link java.lang.String}s to allow
 * specification of a list of tags in logback.xml like so:
 * <br /><br />
 * &lt;appender ...&gt;<br />
 * &nbsp;&nbsp;&lt;tags&gt;<br />
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;tag&gt;my-app&lt;/tag&gt;<br />
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;tag&gt;dev&lt;/tag&gt;<br />
 * &nbsp;&nbsp;&lt;/tags&gt;<br />
 * &lt;/appender&gt;
 * 
 * @author Robert Schmidtke
 * @since 0.1
 *
 */
public class LogglyTagList extends ArrayList<String> {
	
	private static final long serialVersionUID = -1421483678444031480L;

	public void addTag(String tag) {
		super.add(tag);
	}

}
