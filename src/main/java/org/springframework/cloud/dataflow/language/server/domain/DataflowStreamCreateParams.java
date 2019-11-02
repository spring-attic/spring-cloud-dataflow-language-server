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

public class DataflowStreamCreateParams extends DataflowStreamParams {

	private String definition;
	private String description;

	public DataflowStreamCreateParams() {
	}

	public DataflowStreamCreateParams(String name, String server, String description, String definition) {
		super(name, server);
		this.definition = definition;
		this.description = description;
	}

	public static DataflowStreamCreateParams from(String name, String server, String description, String definition) {
		return new DataflowStreamCreateParams(name, server, description, definition);
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
		return "DataflowStreamCreateParams [name=" + getName() + ", definition=" + definition + ", description="
				+ description + ", server=" + getServer() + "]";
	}
}
