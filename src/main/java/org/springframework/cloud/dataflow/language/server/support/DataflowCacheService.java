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

import java.time.Duration;
import java.util.List;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.springframework.cloud.dataflow.language.server.stream.AbstractDataflowStreamLanguageService.StreamItem;
import org.springframework.cloud.dataflow.language.server.task.AbstractDataflowTaskLanguageService.TaskItem;
import org.springframework.stereotype.Component;

/**
 * Service sharing various caches.
 *
 * @author Janne Valkealahti
 *
 */
@Component
public class DataflowCacheService {

	private final Cache<String, List<StreamItem>> streamItemCache = Caffeine.newBuilder()
		.expireAfterAccess(Duration.ofMinutes(1))
		.build();

	private final Cache<String, List<TaskItem>> taskItemCache = Caffeine.newBuilder()
		.expireAfterAccess(Duration.ofMinutes(1))
		.build();

	public Cache<String, List<StreamItem>> getStreamItemCache() {
		return streamItemCache;
	}

	public Cache<String, List<TaskItem>> getTaskItemCache() {
		return taskItemCache;
	}
}
