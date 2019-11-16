/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.dataflow.language.server.domain;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DataflowTaskLaunchParams extends DataflowTaskParams {

	private List<String> arguments = Collections.emptyList();
	private Map<String, String> properties = Collections.emptyMap();

	public DataflowTaskLaunchParams() {
	}

	public DataflowTaskLaunchParams(String name, String server, Map<String, String> properties,
			List<String> arguments) {
		super(name, server);
		if (properties != null) {
			this.properties = properties;
		}
		if (arguments != null) {
			this.arguments = arguments;
		}
	}

	public static DataflowTaskLaunchParams from(String name, String server) {
		return new DataflowTaskLaunchParams(name, server, null, null);
	}

	public static DataflowTaskLaunchParams from(String name, String server, Map<String, String> properties,
			List<String> arguments) {
		return new DataflowTaskLaunchParams(name, server, properties, arguments);
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public List<String> getArguments() {
		return arguments;
	}

	public void setArguments(List<String> arguments) {
		this.arguments = arguments;
	}

	@Override
	public String toString() {
		return "DataflowTaskLaunchParams [name=" + getName() + ", server=" + getServer() + ", arguments=" + arguments
				+ ", properties=" + properties + "]";
	}
}
