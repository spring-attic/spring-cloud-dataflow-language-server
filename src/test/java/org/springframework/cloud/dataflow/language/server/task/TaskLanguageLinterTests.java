/*
 * Copyright 2019-2020 the original author or authors.
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
import static org.mockito.ArgumentMatchers.any;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.dataflow.language.server.DataflowLanguages;
import org.springframework.cloud.dataflow.language.server.support.DataFlowOperationsService;
import org.springframework.cloud.dataflow.language.server.support.DataflowCacheService;
import org.springframework.cloud.dataflow.rest.client.AppRegistryOperations;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.resource.AppRegistrationResource;
import org.springframework.dsl.document.Document;
import org.springframework.dsl.document.TextDocument;
import org.springframework.dsl.domain.Position;
import org.springframework.dsl.service.DslContext;
import org.springframework.dsl.service.reconcile.ReconcileProblem;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.PagedModel.PageMetadata;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class TaskLanguageLinterTests {

    @MockBean
	private DataFlowOperations dataFlowOperations;

    @MockBean
	private AppRegistryOperations appRegistryOperations;

	private final TaskLanguageLinter linter = new TaskLanguageLinter();

	@BeforeEach
	public void setup() {
		linter.setDataflowCacheService(new DataflowCacheService());
		linter.setDataflowOperationsService(new DataFlowOperationsService());
	}

	@Test
	public void testEmpty() {
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_TASK, 0, "");
		List<ReconcileProblem> problems = linter.lint(DslContext.builder().document(document).build()).toStream()
				.collect(Collectors.toList());
		assertThat(problems).hasSize(0);
	}

	@Test
	public void testValidWithName() {
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_TASK, 0, "t1=timestamp");
		List<ReconcileProblem> problems = linter.lint(DslContext.builder().document(document).build()).toStream()
				.collect(Collectors.toList());
		assertThat(problems).hasSize(0);
	}

	// @Test
	// public void testValidWithoutName() {
	// 	Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_TASK, 0, "timestamp");
	// 	List<ReconcileProblem> problems = linter.lint(DslContext.builder().document(document).build()).toStream()
	// 			.collect(Collectors.toList());
	// 	assertThat(problems).hasSize(1);
	// }

	@Test
	public void testValidWithOption() {
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_TASK, 0, "t1=timestamp --format=yyyy-MM-DD");
		List<ReconcileProblem> problems = linter.lint(DslContext.builder().document(document).build()).toStream()
				.collect(Collectors.toList());
		assertThat(problems).hasSize(0);
	}

	@Test
	public void testMissingAppRegistration() {
		PageMetadata metadata = null;
		PagedModel<AppRegistrationResource> model = PagedModel.of(Collections.emptyList(), metadata);
        Mockito.when(dataFlowOperations.appRegistryOperations()).thenReturn(appRegistryOperations);
        Mockito.when(appRegistryOperations.list(any())).thenReturn(model);
		MockTaskLanguageLinter linter = mockLinter();
		linter.setDataflowCacheService(new DataflowCacheService());

		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_TASK, 0, "timestamp");
		List<ReconcileProblem> problems = linter.lint(DslContext.builder().document(document).build()).toStream()
				.collect(Collectors.toList());
		assertThat(problems).hasSize(1);
	}

    private MockTaskLanguageLinter mockLinter() {
        return new MockTaskLanguageLinter();
    }

    private class MockTaskLanguageLinter extends TaskLanguageLinter {

        @Override
        protected DataFlowOperations resolveDataFlowOperations(DslContext context, Position position) {
            return dataFlowOperations;
        }
    }
}
