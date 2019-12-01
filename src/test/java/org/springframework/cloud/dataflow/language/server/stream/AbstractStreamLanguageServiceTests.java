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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.dataflow.language.server.DataflowLanguages;
import org.springframework.cloud.dataflow.language.server.stream.AbstractStreamLanguageService.StreamItem;
import org.springframework.cloud.dataflow.language.server.support.DataFlowOperationsService;
import org.springframework.cloud.dataflow.language.server.support.DataflowCacheService;
import org.springframework.dsl.document.Document;
import org.springframework.dsl.document.TextDocument;
import org.springframework.dsl.domain.Range;

public class AbstractStreamLanguageServiceTests {

	private static final TestStreamLanguageService service = new TestStreamLanguageService();

	@BeforeEach
	public void setup() {
		service.setDataflowCacheService(new DataflowCacheService());
		service.setDataflowOperationsService(new DataFlowOperationsService());
	}

	public static final String DSL_WITHOUT_NAME =
		"time|log";

	public static final String DSL_ONE_MULTI_ENV =
		"-- @env env1\n" +
		"-- @prop foo1=bar1\n" +
		"\n" +
		"-- @env env2\n" +
		"-- @prop foo2=bar2\n" +
		"\n" +
		"-- @name name1\n" +
		"-- @desc desc1\n" +
		"time|log\n";

	public static final String DSL_STREAMS_SAME_NAMES =
		"-- @name name\n" +
		"-- @desc desc\n" +
		"-- @env env1\n" +
		"time|log\n" +
		"\n" +
		"-- @name name\n" +
		"-- @desc desc\n" +
		"-- @env env2\n" +
		"time|log\n";

	public static final String DSL_STREAMS_JUST_METADATA =
		"-- @name name\n" +
		"-- @desc desc\n" +
		"-- @env env1\n";

	public static final String DSL_STREAMS_ERROR_IN_OPTION =
		"ticktock = time --fixed-delay=| log";

	public static final String DSL_COMMENTS_IN_MULTI =
		"#\n" +
		"-- @env env1\n" +
		"-- @prop foo1=bar1\n" +
		"#\n" +
		"-- @name name\n" +
		"-- @desc desc\n" +
		"-- @env env1\n" +
		"time|log\n" +
		"#\n" +
		"#\n" +
		"\n" +
		"-- @name name\n" +
		"-- @desc desc\n" +
		"-- @env env2\n" +
		"time|log\n";

	@Test
	public void testMetadataWithoutStream() {
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0,
			DSL_STREAMS_JUST_METADATA);
		List<StreamItem> result = service.parseCachedMono(document).block();
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getDeployments()).hasSize(0);
		assertThat(result.get(0).getDefinitionItem()).isNotNull();
		assertThat(result.get(0).getDefinitionItem().getEnvItem().getText().toString()).isEqualTo("-- @env env1");
		assertThat(result.get(0).getDefinitionItem().getDescItem().getText().toString()).isEqualTo("-- @desc desc");
		assertThat(result.get(0).getDefinitionItem().getNameItem().getText().toString()).isEqualTo("-- @name name");
		assertThat(result.get(0).getRange()).isEqualTo(Range.from(0, 0, 3, 0));
	}
	@Test
	public void testMultiEnvsAndNameDescDefinedInMetadata() {
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0,
				AbstractStreamLanguageServiceTests.DSL_ONE_MULTI_ENV);
		List<StreamItem> result = service.parse(document).collectList().block();
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getDeployments()).hasSize(2);
		assertThat(result.get(0).getDeployments().get(0).getItems()).hasSize(1);
		assertThat(result.get(0).getDeployments().get(0).getItems().get(0).getText().toString()).isEqualTo("-- @prop foo1=bar1");
		assertThat(result.get(0).getDeployments().get(0).getItems().get(0).getRange()).isEqualTo(Range.from(1, 0, 1, 18));
		assertThat(result.get(0).getDeployments().get(0).getItems().get(0).getContentRange()).isEqualTo(Range.from(1, 3, 1, 18));
		assertThat(result.get(0).getDeployments().get(0).getEnvItem().getText().toString()).isEqualTo("-- @env env1");
		assertThat(result.get(0).getDeployments().get(0).getEnvItem().getRange()).isEqualTo(Range.from(0, 0, 0, 12));
		assertThat(result.get(0).getDeployments().get(0).getEnvItem().getContentRange()).isEqualTo(Range.from(0, 3, 0, 12));
		assertThat(result.get(0).getDeployments().get(1).getItems()).hasSize(1);
		assertThat(result.get(0).getDeployments().get(1).getItems().get(0).getText().toString()).isEqualTo("-- @prop foo2=bar2");
		assertThat(result.get(0).getDeployments().get(1).getItems().get(0).getRange()).isEqualTo(Range.from(4, 0, 4, 18));
		assertThat(result.get(0).getDeployments().get(1).getItems().get(0).getContentRange()).isEqualTo(Range.from(4, 3, 4, 18));
		assertThat(result.get(0).getDeployments().get(1).getEnvItem().getText().toString()).isEqualTo("-- @env env2");
		assertThat(result.get(0).getDeployments().get(1).getEnvItem().getRange()).isEqualTo(Range.from(3, 0, 3, 12));
		assertThat(result.get(0).getDeployments().get(1).getEnvItem().getContentRange()).isEqualTo(Range.from(3, 3, 3, 12));
		assertThat(result.get(0).getDefinitionItem()).isNotNull();
		assertThat(result.get(0).getDefinitionItem().getEnvItem()).isNull();
		assertThat(result.get(0).getDefinitionItem().getNameItem().getText().toString()).isEqualTo("-- @name name1");
		assertThat(result.get(0).getDefinitionItem().getNameItem().getRange()).isEqualTo(Range.from(6, 0, 6, 14));
		assertThat(result.get(0).getDefinitionItem().getNameItem().getContentRange()).isEqualTo(Range.from(6, 3, 6, 14));
		assertThat(result.get(0).getDefinitionItem().getDescItem().getText().toString()).isEqualTo("-- @desc desc1");
		assertThat(result.get(0).getDefinitionItem().getDescItem().getRange()).isEqualTo(Range.from(7, 0, 7, 14));
		assertThat(result.get(0).getDefinitionItem().getDescItem().getContentRange()).isEqualTo(Range.from(7, 3, 7, 14));
		assertThat(result.get(0).getDefinitionItem().getRange()).isEqualTo(Range.from(8, 0, 8, 8));
		assertThat(result.get(0).getDefinitionItem().getReconcileProblem()).isNull();
		assertThat(result.get(0).getDefinitionItem().getStreamNode()).isNotNull();
		assertThat(result.get(0).getDefinitionItem().getStreamNode().getName()).isNull();
	}

	@Test
	public void testEmpty() {
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0, "");
		List<StreamItem> result = service.parse(document).collectList().block();
		assertThat(result).isEmpty();
	}

	@Test
	public void testNamedStream() {
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0, "ticktock=time|log");
		List<StreamItem> result = service.parse(document).collectList().block();
		assertThat(result).hasSize(1);
		assertThat(result.get(0)).isNotNull();
		assertThat(result.get(0).getRange()).isEqualTo(Range.from(0, 0, 0, 17));
		assertThat(result.get(0).getDefinitionItem()).isNotNull();
		assertThat(result.get(0).getDefinitionItem().getRange()).isEqualTo(Range.from(0, 0, 0, 17));
		assertThat(result.get(0).getDefinitionItem().getReconcileProblem()).isNull();
		assertThat(result.get(0).getDefinitionItem().getStreamNode()).isNotNull();
		assertThat(result.get(0).getDefinitionItem().getStreamNode().getName()).isEqualTo("ticktock");
		assertThat(result.get(0).getDeployments()).isEmpty();
	}

	@Test
	public void testMultipleNamedStream() {
		String data =
			"ticktock1=time|log\n" +
			"ticktock2=time|log";
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0, data);
		List<StreamItem> result = service.parse(document).collectList().block();
		assertThat(result).hasSize(2);
	}

	@Test
	public void testMultipleNamedStreamWithEmptyAndCommentLines() {
		String data =
			"\n" +
			"ticktock1=time|log\n" +
			"\n" +
			"ticktock2=time|log\n" +
			"#";
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0, data);
		List<StreamItem> result = service.parse(document).collectList().block();
		assertThat(result).hasSize(2);
	}

	@Test
	public void testOneDeployment() {
		String data =
			"-- @prop foo=bar\n" +
			"ticktock1=time|log";
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0, data);
		List<StreamItem> result = service.parse(document).collectList().block();
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getDeployments()).hasSize(1);
	}

	@Test
	public void testMultiDeployment() {
		String data =
			"-- @prop foo1=bar1\n" +
			"\n" +
			"-- @prop foo1=bar1\n" +
			"-- @prop foo2=bar2\n" +
			"\n" +
			"-- @prop foo1=bar1\n" +
			"-- @prop foo2=bar2\n" +
			"-- @prop foo3=bar3\n" +
			"\n" +
			"ticktock1=time|log";
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0, data);
		List<StreamItem> result = service.parse(document).collectList().block();
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getDeployments()).hasSize(3);
		assertThat(result.get(0).getDeployments().get(0).getItems()).hasSize(1);
		assertThat(result.get(0).getDeployments().get(0).getItems().get(0).getText().toString()).isEqualTo("-- @prop foo1=bar1");
		assertThat(result.get(0).getDeployments().get(1).getItems()).hasSize(2);
		assertThat(result.get(0).getDeployments().get(2).getItems()).hasSize(3);
	}

	@Test
	public void testMultiDeployment2() {
		String data =
			"-- @env env1\n" +
			"-- @prop foo1=bar1\n" +
			"--\n" +
			"-- @prop foo1=bar1\n" +
			"-- @prop foo2=bar2\n" +
			"\n" +
			"-- @env env2\n" +
			"-- @prop foo1=bar1\n" +
			"-- @prop foo2=bar2\n" +
			"-- @prop foo3=bar3\n" +
			"\n" +
			"-- @env env3\n" +
			"-- @name fooname\n" +
			"-- @desc foodesc\n" +
			"ticktock1=time|log";
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0, data);
		List<StreamItem> result = service.parse(document).collectList().block();
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getDeployments()).hasSize(3);
		assertThat(result.get(0).getDeployments().get(0).getItems()).hasSize(1);
		assertThat(result.get(0).getDeployments().get(0).getItems().get(0).getText().toString()).isEqualTo("-- @prop foo1=bar1");
		assertThat(result.get(0).getDeployments().get(0).getItems().get(0).getRange()).isEqualTo(Range.from(1, 0, 1, 18));
		assertThat(result.get(0).getDeployments().get(0).getItems().get(0).getContentRange()).isEqualTo(Range.from(1, 3, 1, 18));
		assertThat(result.get(0).getDeployments().get(0).getEnvItem().getText().toString()).isEqualTo("-- @env env1");
		assertThat(result.get(0).getDeployments().get(0).getEnvItem().getRange()).isEqualTo(Range.from(0, 0, 0, 12));
		assertThat(result.get(0).getDeployments().get(0).getEnvItem().getContentRange()).isEqualTo(Range.from(0, 3, 0, 12));

		assertThat(result.get(0).getDeployments().get(1).getItems()).hasSize(2);
		assertThat(result.get(0).getDeployments().get(2).getItems()).hasSize(3);

		assertThat(result.get(0).getDefinitionItem().getEnvItem()).isNotNull();
		assertThat(result.get(0).getDefinitionItem().getEnvItem().getText().toString()).isEqualTo("-- @env env3");
		assertThat(result.get(0).getDefinitionItem().getEnvItem().getRange()).isEqualTo(Range.from(11, 0, 11, 12));
		assertThat(result.get(0).getDefinitionItem().getEnvItem().getContentRange()).isEqualTo(Range.from(11, 3, 11, 12));
		assertThat(result.get(0).getDefinitionItem().getNameItem()).isNotNull();
		assertThat(result.get(0).getDefinitionItem().getNameItem().getText().toString()).isEqualTo("-- @name fooname");
		assertThat(result.get(0).getDefinitionItem().getNameItem().getRange()).isEqualTo(Range.from(12, 0, 12, 16));
		assertThat(result.get(0).getDefinitionItem().getNameItem().getContentRange()).isEqualTo(Range.from(12, 3, 12, 16));
		assertThat(result.get(0).getDefinitionItem().getDescItem()).isNotNull();
		assertThat(result.get(0).getDefinitionItem().getDescItem().getText().toString()).isEqualTo("-- @desc foodesc");
		assertThat(result.get(0).getDefinitionItem().getDescItem().getRange()).isEqualTo(Range.from(13, 0, 13, 16));
		assertThat(result.get(0).getDefinitionItem().getDescItem().getContentRange()).isEqualTo(Range.from(13, 3, 13, 16));
	}

	@Test
	public void testMultiDeployment3() {
		String data =
			"-- @env env3\n" +
			"-- @name fooname\n" +
			"-- @desc foodesc\n" +
			"ticktock1=time|log";
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0, data);
		List<StreamItem> result = service.parse(document).collectList().block();
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getDeployments()).hasSize(0);
		assertThat(result.get(0).getDefinitionItem().getEnvItem()).isNotNull();
		assertThat(result.get(0).getDefinitionItem().getEnvItem().getText().toString()).isEqualTo("-- @env env3");
		assertThat(result.get(0).getDefinitionItem().getEnvItem().getRange()).isEqualTo(Range.from(0, 0, 0, 12));
		assertThat(result.get(0).getDefinitionItem().getEnvItem().getContentRange()).isEqualTo(Range.from(0, 3, 0, 12));
		assertThat(result.get(0).getDefinitionItem().getNameItem()).isNotNull();
		assertThat(result.get(0).getDefinitionItem().getNameItem().getText().toString()).isEqualTo("-- @name fooname");
		assertThat(result.get(0).getDefinitionItem().getNameItem().getRange()).isEqualTo(Range.from(1, 0, 1, 16));
		assertThat(result.get(0).getDefinitionItem().getNameItem().getContentRange()).isEqualTo(Range.from(1, 3, 1, 16));
		assertThat(result.get(0).getDefinitionItem().getDescItem()).isNotNull();
		assertThat(result.get(0).getDefinitionItem().getDescItem().getText().toString()).isEqualTo("-- @desc foodesc");
		assertThat(result.get(0).getDefinitionItem().getDescItem().getRange()).isEqualTo(Range.from(2, 0, 2, 16));
		assertThat(result.get(0).getDefinitionItem().getDescItem().getContentRange()).isEqualTo(Range.from(2, 3, 2, 16));
	}

	@Test
	public void testErrorInDsl() {
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0,
				DSL_STREAMS_ERROR_IN_OPTION);
		List<StreamItem> result = service.parse(document).collectList().block();
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getDeployments()).hasSize(0);
		assertThat(result.get(0).getDefinitionItem().getDescItem()).isNull();
		assertThat(result.get(0).getDefinitionItem().getEnvItem()).isNull();
		assertThat(result.get(0).getDefinitionItem().getNameItem()).isNull();
		assertThat(result.get(0).getDefinitionItem().getReconcileProblem()).isNotNull();
	}

	@Test
	public void testCommensInMulti() {
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0,
				DSL_COMMENTS_IN_MULTI);
		List<StreamItem> result = service.parse(document).collectList().block();
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getCommentRanges()).hasSize(2);
		assertThat(result.get(0).getCommentRanges().get(0)).isEqualTo(Range.from(0, 0, 0, 1));
		assertThat(result.get(0).getCommentRanges().get(1)).isEqualTo(Range.from(3, 0, 3, 1));
		assertThat(result.get(1).getCommentRanges()).hasSize(1);
		assertThat(result.get(1).getCommentRanges().get(0)).isEqualTo(Range.from(8, 0, 9, 1));
	}


	@Test
	public void testDslWithoutName() {
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0, DSL_WITHOUT_NAME);
		List<StreamItem> result = service.parse(document).collectList().block();
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getDefinitionItem().getReconcileProblem()).isNull();
	}

	private static class TestStreamLanguageService extends AbstractStreamLanguageService {

		public TestStreamLanguageService() {
			setDataflowCacheService(new DataflowCacheService());
		}
	}
}
