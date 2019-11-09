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
package org.springframework.cloud.dataflow.language.server.task;

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

public class TaskLanguageLenserTests {

	private final TaskLanguageLenser lenser = new TaskLanguageLenser();

	@BeforeEach
	public void setup() {
		lenser.setDataflowCacheService(new DataflowCacheService());
		lenser.setDataflowOperationsService(new DataFlowOperationsService());
	}

	@Test
	public void testTask() {
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0,
				AbstractTaskLanguageServiceTests.DSL_INLINE_NAME);
		List<CodeLens> lenses = lenser.lense(DslContext.builder().document(document).build()).toStream()
				.collect(Collectors.toList());
		assertThat(lenses).hasSize(3);
		assertThat(lenses.get(0).getCommand().getCommand()).isEqualTo("vscode-spring-cloud-dataflow.tasks.create");
		assertThat(lenses.get(0).getCommand().getArguments()).hasSize(4);
		assertThat(lenses.get(0).getCommand().getArguments().get(0)).isEqualTo("name");
		assertThat(lenses.get(0).getCommand().getArguments().get(1)).isNull();
		assertThat(lenses.get(0).getCommand().getArguments().get(2)).isNull();
		assertThat(lenses.get(0).getCommand().getArguments().get(3)).isEqualTo("timestamp");
		assertThat(lenses.get(1).getCommand().getCommand()).isEqualTo("vscode-spring-cloud-dataflow.tasks.destroy");
		assertThat(lenses.get(1).getCommand().getArguments()).hasSize(2);
		assertThat(lenses.get(1).getCommand().getArguments().get(0)).isEqualTo("name");
		assertThat(lenses.get(1).getCommand().getArguments().get(1)).isNull();
		assertThat(lenses.get(2).getCommand().getCommand()).isEqualTo("vscode-spring-cloud-dataflow.tasks.launch");
		assertThat(lenses.get(2).getCommand().getArguments()).hasSize(1);
		assertThat(lenses.get(2).getCommand().getArguments().get(0)).isEqualTo("name");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testTaskMultiEnv() {
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0,
				AbstractTaskLanguageServiceTests.DSL_ONE_MULTI_ENV);
		List<CodeLens> lenses = lenser.lense(DslContext.builder().document(document).build()).toStream()
				.collect(Collectors.toList());
		assertThat(lenses).hasSize(6);

		assertThat(lenses.get(0).getCommand().getCommand()).isEqualTo("vscode-spring-cloud-dataflow.tasks.launch");
		assertThat(lenses.get(0).getCommand().getArguments()).hasSize(4);
		assertThat(lenses.get(0).getCommand().getArguments().get(0)).isEqualTo("name3");
		assertThat(lenses.get(0).getCommand().getArguments().get(1)).isEqualTo("env1");
		assertThat((Map<String, String>)lenses.get(0).getCommand().getArguments().get(2)).hasSize(1);
		assertThat((Map<String, String>)lenses.get(0).getCommand().getArguments().get(2)).containsEntry("foo1", "bar1");
		assertThat(lenses.get(0).getRange()).isEqualTo(Range.from(0, 0, 0, 12));
		assertThat((List<String>)lenses.get(0).getCommand().getArguments().get(3)).hasSize(1);
		assertThat((List<String>)lenses.get(0).getCommand().getArguments().get(3)).containsOnly("--foo1=bar1");

		assertThat(lenses.get(1).getCommand().getCommand()).isEqualTo("vscode-spring-cloud-dataflow.tasks.launch");
		assertThat(lenses.get(1).getCommand().getArguments()).hasSize(4);
		assertThat(lenses.get(1).getCommand().getArguments().get(0)).isEqualTo("name3");
		assertThat(lenses.get(1).getCommand().getArguments().get(1)).isEqualTo("env2");
		assertThat((Map<String, String>)lenses.get(1).getCommand().getArguments().get(2)).hasSize(1);
		assertThat((Map<String, String>)lenses.get(1).getCommand().getArguments().get(2)).containsEntry("foo2", "bar2");
		assertThat(lenses.get(1).getRange()).isEqualTo(Range.from(4, 0, 4, 12));
		assertThat((List<String>)lenses.get(1).getCommand().getArguments().get(3)).hasSize(1);
		assertThat((List<String>)lenses.get(1).getCommand().getArguments().get(3)).containsOnly("--foo2=bar2");

		assertThat(lenses.get(2).getCommand().getCommand()).isEqualTo("vscode-spring-cloud-dataflow.tasks.launch");
		assertThat(lenses.get(2).getCommand().getArguments()).hasSize(4);
		assertThat(lenses.get(2).getCommand().getArguments().get(0)).isEqualTo("name3");
		assertThat(lenses.get(2).getCommand().getArguments().get(1)).isEqualTo("env4");
		assertThat((Map<String, String>)lenses.get(2).getCommand().getArguments().get(2)).hasSize(0);
		assertThat(lenses.get(2).getRange()).isEqualTo(Range.from(8, 0, 8, 12));
		assertThat((List<String>)lenses.get(2).getCommand().getArguments().get(3)).hasSize(1);
		assertThat((List<String>)lenses.get(2).getCommand().getArguments().get(3)).containsOnly("--foo4=bar4");

		assertThat(lenses.get(3).getCommand().getCommand()).isEqualTo("vscode-spring-cloud-dataflow.tasks.create");
		assertThat(lenses.get(3).getCommand().getArguments()).hasSize(4);
		assertThat(lenses.get(3).getCommand().getArguments().get(0)).isEqualTo("name3");
		assertThat(lenses.get(3).getCommand().getArguments().get(1)).isEqualTo("env3");
		assertThat(lenses.get(3).getCommand().getArguments().get(2)).isEqualTo("desc3");
		assertThat(lenses.get(3).getCommand().getArguments().get(3)).isEqualTo("timestamp");
		assertThat(lenses.get(3).getRange()).isEqualTo(Range.from(14, 0, 14, 9));

		assertThat(lenses.get(4).getCommand().getCommand()).isEqualTo("vscode-spring-cloud-dataflow.tasks.destroy");
		assertThat(lenses.get(4).getCommand().getArguments()).hasSize(2);
		assertThat(lenses.get(4).getCommand().getArguments().get(0)).isEqualTo("name3");
		assertThat(lenses.get(4).getCommand().getArguments().get(1)).isEqualTo("env3");
		assertThat(lenses.get(4).getRange()).isEqualTo(Range.from(14, 0, 14, 9));

		assertThat(lenses.get(5).getCommand().getCommand()).isEqualTo("vscode-spring-cloud-dataflow.tasks.launch");
		assertThat(lenses.get(5).getCommand().getArguments()).hasSize(1);
		assertThat(lenses.get(5).getCommand().getArguments().get(0)).isEqualTo("name3");
		assertThat(lenses.get(5).getRange()).isEqualTo(Range.from(14, 0, 14, 9));
	}

	@Test
	public void testTaskNoName() {
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0,
				AbstractTaskLanguageServiceTests.DSL_NO_NAME);
		List<CodeLens> lenses = lenser.lense(DslContext.builder().document(document).build()).toStream()
				.collect(Collectors.toList());
		assertThat(lenses).hasSize(0);
	}
}
