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
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.dataflow.language.server.DataflowLanguages;
import org.springframework.cloud.dataflow.language.server.support.DataFlowOperationsService;
import org.springframework.cloud.dataflow.language.server.support.DataflowCacheService;
import org.springframework.dsl.document.Document;
import org.springframework.dsl.document.TextDocument;
import org.springframework.dsl.service.DslContext;
import org.springframework.dsl.service.reconcile.ReconcileProblem;

public class StreamLanguageLinterTests {

	private final StreamLanguageLinter linter = new StreamLanguageLinter();

	@BeforeEach
	public void setup() {
		linter.setDataflowCacheService(new DataflowCacheService());
		linter.setDataflowOperationsService(new DataFlowOperationsService());
	}

	@Test
	public void testEmpty() {
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0, "");
		List<ReconcileProblem> problems = linter.lint(DslContext.builder().document(document).build()).toStream()
				.collect(Collectors.toList());
		assertThat(problems).hasSize(0);
	}

	@Test
	public void testJustOneApp() {
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0, "foo");
		List<ReconcileProblem> problems = linter.lint(DslContext.builder().document(document).build()).toStream()
				.collect(Collectors.toList());
		assertThat(problems).isEmpty();
	}

	@Test
	public void testDoublePipeErrorBetweenApps() {
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0, "stream = :aaa > fff||bbb");
		List<ReconcileProblem> problems = linter.lint(DslContext.builder().document(document).build()).toStream()
				.collect(Collectors.toList());
		assertThat(problems).hasSize(1);
		ReconcileProblem problem = problems.get(0);
		assertThat(problem.getMessage()).contains("do not use || between source/processor/sink apps in a stream");
		assertThat(problem.getRange().getStart().getLine()).isEqualTo(0);
		assertThat(problem.getRange().getStart().getCharacter()).isEqualTo(19);
		assertThat(problem.getRange().getEnd().getLine()).isEqualTo(0);
		assertThat(problem.getRange().getEnd().getCharacter()).isEqualTo(19);
	}

	@Test
	public void testLintsMultipleStreams() {
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0,
				"stream1 = time|log\nstream2 = time|log");
		List<ReconcileProblem> problems = linter.lint(DslContext.builder().document(document).build()).toStream()
				.collect(Collectors.toList());
		assertThat(problems).hasSize(0);
	}

	@Test
	public void testLintsMultipleStreamsWithEmptyLines() {
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0,
				"\nstream1 = time|log\n\nstream2 = time|log\n");
		List<ReconcileProblem> problems = linter.lint(DslContext.builder().document(document).build()).toStream()
				.collect(Collectors.toList());
		assertThat(problems).hasSize(0);
	}

	@Test
	public void testStreamWithDeployProperies() {
		String data =
			"#foo1=bar1\n" +
			"ticktock=time|log";
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0, data);
		List<ReconcileProblem> problems = linter.lint(DslContext.builder().document(document).build()).toStream()
				.collect(Collectors.toList());
		assertThat(problems).hasSize(0);
	}
}
