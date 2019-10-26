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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.dataflow.language.server.domain.DataflowEnvironmentParams.Credentials;
import org.springframework.cloud.dataflow.language.server.domain.DataflowEnvironmentParams.Environment;

public class DataflowEnvironmentParamsTests {

	@Test
	public void testEnvironmentEquals1() {
		Environment env1 = new Environment();
		env1.setName("name");
		env1.setUrl("url");
		Credentials cred1 = new Credentials();
		cred1.setUsername("username");
		cred1.setPassword("password");
		env1.setCredentials(cred1);

		Environment env2 = new Environment();
		env2.setName("name");
		env2.setUrl("url");
		Credentials cred2 = new Credentials();
		cred2.setUsername("username");
		cred2.setPassword("password");
		env2.setCredentials(cred2);

		assertThat(env1).isEqualTo(env2);
	}

	@Test
	public void testEnvironmentEquals2() {
		Environment env1 = new Environment();
		env1.setName("name");
		env1.setUrl("url");
		Credentials cred1 = new Credentials();
		cred1.setUsername("");
		cred1.setPassword("");
		env1.setCredentials(cred1);

		Environment env2 = new Environment();
		env2.setName("name");
		env2.setUrl("url");
		Credentials cred2 = new Credentials();
		cred2.setUsername("");
		cred2.setPassword("");
		env2.setCredentials(cred2);

		assertThat(env1).isEqualTo(env2);
	}
}
