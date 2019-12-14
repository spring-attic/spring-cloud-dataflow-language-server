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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.dataflow.language.server.DataflowLanguages;
import org.springframework.cloud.dataflow.language.server.support.DataFlowOperationsService;
import org.springframework.cloud.dataflow.language.server.support.DataflowCacheService;
import org.springframework.dsl.document.Document;
import org.springframework.dsl.document.TextDocument;
import org.springframework.dsl.domain.Position;
import org.springframework.dsl.domain.Range;
import org.springframework.dsl.domain.TextEdit;
import org.springframework.dsl.domain.WorkspaceEdit;
import org.springframework.dsl.service.DslContext;

public class StreamLanguageRenamerTests {

	private final StreamLanguageRenamer renamer = new StreamLanguageRenamer();

	@BeforeEach
	public void setup() {
		renamer.setDataflowCacheService(new DataflowCacheService());
		renamer.setDataflowOperationsService(new DataFlowOperationsService());
	}

    @Test
	public void testTapsLinkingRenameMain() {
        Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0,
				AbstractStreamLanguageServiceTests.DSL_TAPS_LINKING);
		WorkspaceEdit edit = renamer
				.rename(DslContext.builder().document(document).build(), Position.from(0, 1), "newName").block();
		assertThat(edit).isNotNull();
		assertThat(edit.getDocumentChangesTextDocumentEdits()).hasSize(1);
		assertThat(edit.getDocumentChangesTextDocumentEdits().get(0).getEdits()).hasSize(1);
		// TODO: need to do tap source support, commented lines are for that!
		// assertThat(edit.getDocumentChangesTextDocumentEdits().get(0).getEdits()).hasSize(3);

		TextEdit edit1 = TextEdit.textEdit()
			.newText("newName")
			.range(Range.from(0, 0, 0, 4))
			.build();
		assertThat(edit.getDocumentChangesTextDocumentEdits().get(0).getEdits().get(0)).isEqualTo(edit1);

		// TextEdit edit2 = TextEdit.textEdit()
		// 	.newText("newName")
		// 	.range(Range.from(1, 8, 1, 12))
		// 	.build();
		// assertThat(edit.getDocumentChangesTextDocumentEdits().get(0).getEdits().get(1)).isEqualTo(edit2);

		// TextEdit edit3 = TextEdit.textEdit()
		// 	.newText("newName")
		// 	.range(Range.from(2, 8, 2, 12))
		// 	.build();
		// assertThat(edit.getDocumentChangesTextDocumentEdits().get(0).getEdits().get(2)).isEqualTo(edit3);
	}

    @Test
	public void testTapsLinkingWithMetaRenameMain() {
        Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0,
				AbstractStreamLanguageServiceTests.DSL_TAPS_LINKING_WITH_META);
		WorkspaceEdit edit = renamer
				.rename(DslContext.builder().document(document).build(), Position.from(0, 10), "newName").block();
		assertThat(edit).isNotNull();
		assertThat(edit.getDocumentChangesTextDocumentEdits()).hasSize(1);
		assertThat(edit.getDocumentChangesTextDocumentEdits().get(0).getEdits()).hasSize(2);
		assertThat(edit.getDocumentChangesTextDocumentEdits().get(0).getEdits().get(0).getRange())
				.isEqualTo(Range.from(0, 9, 0, 13));
		assertThat(edit.getDocumentChangesTextDocumentEdits().get(0).getEdits().get(1).getRange())
				.isEqualTo(Range.from(1, 0, 1, 4));
	}
}
