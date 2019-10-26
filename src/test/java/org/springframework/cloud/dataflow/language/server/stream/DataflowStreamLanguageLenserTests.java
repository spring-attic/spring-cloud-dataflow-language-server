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
package org.springframework.cloud.dataflow.language.server.stream;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.dataflow.language.server.DataflowLanguages;
import org.springframework.cloud.dataflow.language.server.support.DataFlowOperationsService;
import org.springframework.cloud.dataflow.language.server.support.DataflowCacheService;
import org.springframework.dsl.document.Document;
import org.springframework.dsl.document.TextDocument;
import org.springframework.dsl.domain.CodeLens;
import org.springframework.dsl.domain.Range;
import org.springframework.dsl.service.DslContext;

public class DataflowStreamLanguageLenserTests {

	private final DataflowStreamLanguageLenser lenser = new DataflowStreamLanguageLenser();

	@BeforeEach
	public void setup() {
		lenser.setDataflowCacheService(new DataflowCacheService());
		lenser.setDataflowOperationsService(new DataFlowOperationsService());
	}

	@Test
	public void testLintsMultipleStreams() {
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0,
				"stream1 = time|log\nstream2 = time|log");
		List<CodeLens> lenses = lenser.lense(DslContext.builder().document(document).build()).toStream()
				.collect(Collectors.toList());
		assertThat(lenses).hasSize(8);
	}

	@Test
	public void testLensesSingleStreamsWithDeployProperties() {
		String data =
			"-- @prop foo1=bar1\n" +
			"stream1 = time|log";

		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0, data);
		List<CodeLens> lenses = lenser.lense(DslContext.builder().document(document).build()).toStream()
				.collect(Collectors.toList());
		assertThat(lenses).hasSize(5);
		assertThat(lenses.get(0).getRange()).isEqualTo(Range.from(0, 0, 0, 18));
		assertThat(lenses.get(1).getRange()).isEqualTo(Range.from(1, 0, 1, 18));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testLintsMultipleStreamsWithDeployProperties() {
		String data =
			"-- @prop foo1=bar1\n" +
			"-- @prop foo2=bar2\n" +
			"stream1 = time|log\n" +
			"-- @prop foo3=bar3\n" +
			"stream2 = time|log";
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0, data);
		List<CodeLens> lenses = lenser.lense(DslContext.builder().document(document).build()).toStream()
				.collect(Collectors.toList());
		assertThat(lenses).hasSize(10);
		assertThat(lenses.get(0).getCommand().getTitle()).isEqualTo(DataflowLanguages.COMMAND_STREAM_DEPLOY_TITLE);
		assertThat(lenses.get(0).getCommand().getCommand()).isEqualTo(DataflowLanguages.COMMAND_STREAM_DEPLOY);
		assertThat(lenses.get(0).getRange()).isEqualTo(Range.from(0, 0, 0, 18));
		assertThat(lenses.get(0).getCommand().getArguments()).hasSize(3);
		assertThat(lenses.get(0).getCommand().getArguments().get(0)).isEqualTo("stream1");
		assertThat(lenses.get(0).getCommand().getArguments().get(1)).isNull();
		assertThat(lenses.get(0).getCommand().getArguments().get(2)).isInstanceOf(Map.class);
		assertThat((Map<String, String>)lenses.get(0).getCommand().getArguments().get(2)).hasSize(2);
		assertThat((Map<String, String>)lenses.get(0).getCommand().getArguments().get(2)).containsEntry("foo1", "bar1");
		assertThat((Map<String, String>)lenses.get(0).getCommand().getArguments().get(2)).containsEntry("foo2", "bar2");
		assertThat(lenses.get(0).getRange()).isEqualTo(Range.from(0, 0, 0, 18));
		assertThat(lenses.get(1).getRange()).isEqualTo(Range.from(2, 0, 2, 18));
		assertThat(lenses.get(5).getCommand().getTitle()).isEqualTo(DataflowLanguages.COMMAND_STREAM_DEPLOY_TITLE);
		assertThat(lenses.get(5).getCommand().getCommand()).isEqualTo(DataflowLanguages.COMMAND_STREAM_DEPLOY);
		assertThat(lenses.get(5).getRange()).isEqualTo(Range.from(3, 0, 3, 18));
		assertThat(lenses.get(6).getRange()).isEqualTo(Range.from(4, 0, 4, 18));
	}

	@Test
	public void testLintsMultipleStreamsWithDeployProperties2() {
		String data =
			"-- @prop foo1=bar1\n" +
			"\n" +
			"-- @prop foo2=bar2\n" +
			"stream1 = time|log";
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0, data);
		List<CodeLens> lenses = lenser.lense(DslContext.builder().document(document).build()).toStream()
				.collect(Collectors.toList());
		assertThat(lenses).hasSize(6);
		assertThat(lenses.get(0).getCommand().getTitle()).isEqualTo(DataflowLanguages.COMMAND_STREAM_DEPLOY_TITLE);
		assertThat(lenses.get(0).getRange()).isEqualTo(Range.from(0, 0, 0, 18));
		assertThat(lenses.get(1).getCommand().getTitle()).isEqualTo(DataflowLanguages.COMMAND_STREAM_DEPLOY_TITLE);
		assertThat(lenses.get(1).getRange()).isEqualTo(Range.from(2, 0, 2, 18));
	}

	@Test
	public void testMultiEnvsAndNameDescDefinedInMetadata() {
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0,
				AbstractDataflowStreamLanguageServiceTests.DSL_ONE_MULTI_ENV);
		List<CodeLens> lenses = lenser.lense(DslContext.builder().document(document).build()).toStream()
				.collect(Collectors.toList());
		assertThat(lenses).hasSize(6);

		assertThat(lenses.get(0).getCommand().getTitle()).isEqualTo(DataflowLanguages.COMMAND_STREAM_DEPLOY_TITLE);
		assertThat(lenses.get(0).getCommand().getArguments()).hasSize(3);
		assertThat(lenses.get(0).getCommand().getArguments().get(0)).isEqualTo("name1");
		assertThat(lenses.get(0).getCommand().getArguments().get(1)).isEqualTo("env1");

		assertThat(lenses.get(1).getCommand().getTitle()).isEqualTo(DataflowLanguages.COMMAND_STREAM_DEPLOY_TITLE);
		assertThat(lenses.get(1).getCommand().getArguments()).hasSize(3);
		assertThat(lenses.get(1).getCommand().getArguments().get(0)).isEqualTo("name1");
		assertThat(lenses.get(1).getCommand().getArguments().get(1)).isEqualTo("env2");

		assertThat(lenses.get(2).getCommand().getTitle()).isEqualTo(DataflowLanguages.COMMAND_STREAM_CREATE_TITLE);
		assertThat(lenses.get(2).getRange()).isEqualTo(Range.from(8, 0, 8, 8));
		assertThat(lenses.get(2).getCommand().getArguments()).hasSize(4);
		assertThat(lenses.get(2).getCommand().getArguments().get(0)).isEqualTo("name1");
		assertThat(lenses.get(2).getCommand().getArguments().get(1)).isNull();
		assertThat(lenses.get(2).getCommand().getArguments().get(2)).isEqualTo("desc1");
		assertThat(lenses.get(2).getCommand().getArguments().get(3)).isEqualTo("time|log");

		assertThat(lenses.get(3).getCommand().getTitle()).isEqualTo(DataflowLanguages.COMMAND_STREAM_DESTROY_TITLE);

		assertThat(lenses.get(4).getCommand().getTitle()).isEqualTo(DataflowLanguages.COMMAND_STREAM_DEPLOY_TITLE);

		assertThat(lenses.get(5).getCommand().getTitle()).isEqualTo(DataflowLanguages.COMMAND_STREAM_UNDEPLOY_TITLE);
	}

	@Test
	public void testJustMetadataWithoutStream() {
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0,
				AbstractDataflowStreamLanguageServiceTests.DSL_STREAMS_JUST_METADATA);
		List<CodeLens> lenses = lenser.lense(DslContext.builder().document(document).build()).toStream()
				.collect(Collectors.toList());
		assertThat(lenses).hasSize(0);
	}
}
