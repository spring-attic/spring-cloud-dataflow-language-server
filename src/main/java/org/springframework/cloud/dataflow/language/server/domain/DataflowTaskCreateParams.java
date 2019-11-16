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

public class DataflowTaskCreateParams extends DataflowTaskParams {

	private String definition;
	private String description;

	public DataflowTaskCreateParams() {
	}

	public DataflowTaskCreateParams(String name, String server, String definition, String description) {
		super(name, server);
		this.definition = definition;
		this.description = description;
	}

	public static DataflowTaskCreateParams from(String name, String server, String definition, String description) {
		return new DataflowTaskCreateParams(name, server, definition, description);
	}

	public String getDefinition() {
		return definition;
	}

	public void setDefinition(String definition) {
		this.definition = definition;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return "DataflowTaskCreateParams [name=" + getName() + ", definition=" + definition + ", description="
				+ description + ", server=" + getServer() + "]";
	}
}
