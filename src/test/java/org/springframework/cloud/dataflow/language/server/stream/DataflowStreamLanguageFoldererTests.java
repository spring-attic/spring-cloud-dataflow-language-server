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
import org.springframework.dsl.domain.FoldingRange;
import org.springframework.dsl.domain.FoldingRangeKind;
import org.springframework.dsl.service.DslContext;

public class DataflowStreamLanguageFoldererTests {

	private final DataflowStreamLanguageFolderer folderer = new DataflowStreamLanguageFolderer();

	@BeforeEach
	public void setup() {
		folderer.setDataflowCacheService(new DataflowCacheService());
		folderer.setDataflowOperationsService(new DataFlowOperationsService());
	}

	@Test
	public void testComments() {
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0,
				AbstractDataflowStreamLanguageServiceTests.DSL_COMMENTS_IN_MULTI);
		List<FoldingRange> folds = folderer.fold(DslContext.builder().document(document).build()).toStream()
				.collect(Collectors.toList());
		assertThat(folds).hasSize(5);

		assertThat(folds.get(0).getKind()).isEqualTo(FoldingRangeKind.region);
		assertThat(folds.get(0).getStartLine()).isEqualTo(1);
		assertThat(folds.get(0).getEndLine()).isEqualTo(7);

		assertThat(folds.get(1).getKind()).isEqualTo(FoldingRangeKind.comment);
		assertThat(folds.get(1).getStartLine()).isEqualTo(0);
		assertThat(folds.get(1).getEndLine()).isEqualTo(0);

		assertThat(folds.get(2).getKind()).isEqualTo(FoldingRangeKind.comment);
		assertThat(folds.get(2).getStartLine()).isEqualTo(3);
		assertThat(folds.get(2).getEndLine()).isEqualTo(3);

		assertThat(folds.get(3).getKind()).isEqualTo(FoldingRangeKind.region);
		assertThat(folds.get(3).getStartLine()).isEqualTo(10);
		assertThat(folds.get(3).getEndLine()).isEqualTo(14);

		assertThat(folds.get(4).getKind()).isEqualTo(FoldingRangeKind.comment);
		assertThat(folds.get(4).getStartLine()).isEqualTo(8);
		assertThat(folds.get(4).getEndLine()).isEqualTo(9);
	}
}
