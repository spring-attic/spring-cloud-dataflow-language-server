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

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.dataflow.language.server.DataflowLanguages;
import org.springframework.dsl.document.Document;
import org.springframework.dsl.document.TextDocument;
import org.springframework.dsl.domain.CodeLens;
import org.springframework.dsl.service.DslContext;
import org.springframework.dsl.service.Lenser;

public class AppLanguageLenserTests {

	private final static Lenser lenser = new AppLanguageLenser();
	private final String CONTENT1 =
		"task.timestamp=maven://org.springframework.cloud.task.app:timestamp-task:1.3.0.RELEASE" +
		"\n" +
		"task.timestamp.metadata=maven://org.springframework.cloud.task.app:timestamp-task:jar:metadata:1.3.0.RELEASE";
	private final String CONTENT2 =
		"sink.tcp=maven://org.springframework.cloud.stream.app:tcp-sink-rabbit:2.0.1.RELEASE" +
		"\n" +
		"sink.tcp.metadata=maven://org.springframework.cloud.stream.app:tcp-sink-rabbit:jar:metadata:2.0.1.RELEASE" +
		"\n" +
		"source.tcp=maven://org.springframework.cloud.stream.app:tcp-source-rabbit:2.0.1.RELEASE" +
		"\n" +
		"source.tcp.metadata=maven://org.springframework.cloud.stream.app:tcp-source-rabbit:jar:metadata:2.0.1.RELEASE";
	private final String CONTENT3 =
		"source.time=maven://org.springframework.cloud.stream.app:time-source-rabbit:2.0.1.RELEASE" +
		"\n" +
		"source.time.metadata=maven://org.springframework.cloud.stream.app:time-source-rabbit:jar:metadata:2.0.1.RELEASE" +
		"\n" +
		"source.time=maven://org.springframework.cloud.stream.app:time-source-rabbit:2.0.0.RELEASE" +
		"\n" +
		"source.time.metadata=maven://org.springframework.cloud.stream.app:time-source-rabbit:jar:metadata:2.0.0.RELEASE";
	private final String CONTENT4 =
		"source.time=https://repo.spring.io/libs-release/org/springframework/cloud/stream/app/time-source-rabbit/2.1.0.RELEASE/time-source-rabbit-2.1.0.RELEASE.jar" +
		"\n" +
		"source.time.metadata=https://repo.spring.io/libs-release/org/springframework/cloud/stream/app/time-source-rabbit/2.1.0.RELEASE/time-source-rabbit-2.1.0.RELEASE-metadata.jar";

	@Test
	public void testOneMavenWithMetadata() {
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_APP, 0, CONTENT1);
		List<CodeLens> problems = lenser.lense(DslContext.builder().document(document).build()).toStream()
				.collect(Collectors.toList());
		assertThat(problems).hasSize(2);
		assertThat(problems.get(0).getCommand().getTitle()).isEqualTo(DataflowLanguages.COMMAND_APP_REGISTER_TITLE);
		assertThat(problems.get(0).getCommand().getArguments()).hasSize(4);
		assertThat(problems.get(0).getCommand().getArguments().get(0)).isEqualTo("task");
		assertThat(problems.get(0).getCommand().getArguments().get(1)).isEqualTo("timestamp");
		assertThat(problems.get(0).getCommand().getArguments().get(2))
				.isEqualTo("maven://org.springframework.cloud.task.app:timestamp-task:1.3.0.RELEASE");
		assertThat(problems.get(0).getCommand().getArguments().get(3))
				.isEqualTo("maven://org.springframework.cloud.task.app:timestamp-task:jar:metadata:1.3.0.RELEASE");
		assertThat(problems.get(1).getCommand().getTitle()).isEqualTo(DataflowLanguages.COMMAND_APP_UNREGISTER_TITLE);
		assertThat(problems.get(1).getCommand().getArguments()).hasSize(3);
		assertThat(problems.get(1).getCommand().getArguments().get(0)).isEqualTo("task");
	}

	@Test
	public void testOneHttpWithMetadata() {
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_APP, 0, CONTENT4);
		List<CodeLens> problems = lenser.lense(DslContext.builder().document(document).build()).toStream()
				.collect(Collectors.toList());
		assertThat(problems).hasSize(2);
		assertThat(problems.get(0).getCommand().getTitle()).isEqualTo(DataflowLanguages.COMMAND_APP_REGISTER_TITLE);
		assertThat(problems.get(0).getCommand().getArguments()).hasSize(4);
		assertThat(problems.get(0).getCommand().getArguments().get(0)).isEqualTo("source");
		assertThat(problems.get(0).getCommand().getArguments().get(1)).isEqualTo("time");
		assertThat(problems.get(0).getCommand().getArguments().get(2))
				.isEqualTo("https://repo.spring.io/libs-release/org/springframework/cloud/stream/app/time-source-rabbit/2.1.0.RELEASE/time-source-rabbit-2.1.0.RELEASE.jar");
		assertThat(problems.get(0).getCommand().getArguments().get(3))
				.isEqualTo("https://repo.spring.io/libs-release/org/springframework/cloud/stream/app/time-source-rabbit/2.1.0.RELEASE/time-source-rabbit-2.1.0.RELEASE-metadata.jar");
		assertThat(problems.get(1).getCommand().getTitle()).isEqualTo(DataflowLanguages.COMMAND_APP_UNREGISTER_TITLE);
		assertThat(problems.get(1).getCommand().getArguments()).hasSize(3);
		assertThat(problems.get(1).getCommand().getArguments().get(0)).isEqualTo("source");
	}

	@Test
	public void testMixedSameName() {
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_APP, 0, CONTENT2);
		List<CodeLens> problems = lenser.lense(DslContext.builder().document(document).build()).toStream()
				.collect(Collectors.toList());
		assertThat(problems).hasSize(4);
	}

	@Test
	public void testMultipleVersions() {
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_APP, 0, CONTENT3);
		List<CodeLens> problems = lenser.lense(DslContext.builder().document(document).build()).toStream()
				.collect(Collectors.toList());
		assertThat(problems).hasSize(4);
	}
}
