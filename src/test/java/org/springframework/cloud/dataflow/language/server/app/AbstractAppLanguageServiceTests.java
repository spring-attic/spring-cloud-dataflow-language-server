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
package org.springframework.cloud.dataflow.language.server.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.dataflow.language.server.DataflowLanguages;
import org.springframework.cloud.dataflow.language.server.app.AbstractAppLanguageService.AppEntry;
import org.springframework.dsl.document.Document;
import org.springframework.dsl.document.TextDocument;

public class AbstractAppLanguageServiceTests {

	private static final TestAppLanguageService service = new TestAppLanguageService();

	public static final String DSL_COMMON_TYPES =
		"source.time=maven://org.springframework.cloud.stream.app:time-source-rabbit:2.0.1.RELEASE\n" +
		"source.time.metadata=source.time.metadata=maven://org.springframework.cloud.stream.app:time-source-rabbit:jar:metadata:2.0.1.RELEASE\n" +
		"sink.log=maven://org.springframework.cloud.stream.app:log-sink-rabbit:2.0.2.RELEASE\n" +
		"sink.log.metadata=maven://org.springframework.cloud.stream.app:log-sink-rabbit:jar:metadata:2.0.2.RELEASE";

	@Test
	public void testCommonTypes() {
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_APP, 0, DSL_COMMON_TYPES);
		Collection<AppEntry> result = service.parseApps(document);
		assertThat(result).hasSize(2);
	}

	private static class TestAppLanguageService extends AbstractAppLanguageService {

		public TestAppLanguageService() {
			super();
		}
	}
}
