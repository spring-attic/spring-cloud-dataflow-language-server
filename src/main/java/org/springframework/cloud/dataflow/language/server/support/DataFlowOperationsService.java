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
package org.springframework.cloud.dataflow.language.server.support;

import java.net.URI;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.dataflow.language.server.domain.DataflowEnvironmentParams.Environment;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.util.HttpClientConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Service class to share instances of a {@link DataFlowOperations}. Mostly done
 * because nature of DataFlowTemplate is that it connects to a dataflow server
 * immediately to get a root links. This makes it super expensive if we'd about
 * to build new instances every time.
 */
@Component
public class DataFlowOperationsService {

	private static final Logger log = LoggerFactory.getLogger(DataFlowOperationsService.class);
	private final Cache<Environment, DataFlowOperations> cache = Caffeine.newBuilder()
		.removalListener((key, value, cause) -> {
			log.debug("Entry removed {} {} {}", key, value, cause);
		})
		.build();

	public DataFlowOperations getDataFlowOperations(Environment environment) {
		return cache.get(environment, key -> buildDataFlowTemplate(key));
	}

	private DataFlowTemplate buildDataFlowTemplate(Environment environment) {
		log.debug("Building DataFlowTemplate for environment {}", environment);
		URI uri = URI.create(environment.getUrl());
		String username = environment.getCredentials().getUsername();
		String password = environment.getCredentials().getPassword();
		if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
			RestTemplate restTemplate = new RestTemplate();
			HttpClientConfigurer httpClientConfigurer = HttpClientConfigurer.create(uri);
			httpClientConfigurer.basicAuthCredentials(username, password);
			restTemplate.setRequestFactory(httpClientConfigurer.buildClientHttpRequestFactory());
			return new DataFlowTemplate(uri, restTemplate);
		} else {
			return new DataFlowTemplate(uri);
		}
	}

	@Override
	public String toString() {
        return "DataFlowOperationsService cache estimated size=" + cache.estimatedSize();
	}
}
