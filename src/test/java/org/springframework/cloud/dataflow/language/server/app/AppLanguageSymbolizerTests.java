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
import org.springframework.dsl.domain.DocumentSymbol;
import org.springframework.dsl.domain.SymbolInformation;
import org.springframework.dsl.service.DslContext;
import org.springframework.dsl.service.symbol.SymbolizeInfo;

public class AppLanguageSymbolizerTests {

	private final AppLanguageSymbolizer symbolizer = new AppLanguageSymbolizer();

	@Test
	public void testCommonTypes() {
		Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_APP, 0,
				AbstractAppLanguageServiceTests.DSL_COMMON_TYPES);
        SymbolizeInfo symbolizeInfo = symbolizer.symbolize(DslContext.builder().document(document).build());

        List<SymbolInformation> symbolInformations = symbolizeInfo.symbolInformations().toStream()
                .collect(Collectors.toList());
        List<DocumentSymbol> documentSymbols = symbolizeInfo.documentSymbols().toStream().collect(Collectors.toList());

        assertThat(symbolInformations).hasSize(6);
        assertThat(symbolInformations.get(0).getName()).isEqualTo("uri");
        assertThat(symbolInformations.get(1).getName()).isEqualTo("metadata");
        assertThat(symbolInformations.get(2).getName()).isEqualTo("time");
        assertThat(symbolInformations.get(3).getName()).isEqualTo("uri");
        assertThat(symbolInformations.get(4).getName()).isEqualTo("metadata");
        assertThat(symbolInformations.get(5).getName()).isEqualTo("log");

        assertThat(documentSymbols).hasSize(2);
        assertThat(documentSymbols.get(0).getName()).isEqualTo("time");
        assertThat(documentSymbols.get(0).getChildren()).hasSize(2);
        assertThat(documentSymbols.get(0).getChildren().get(0).getName()).isEqualTo("uri");
        assertThat(documentSymbols.get(0).getChildren().get(1).getName()).isEqualTo("metadata");

		assertThat(documentSymbols.get(1).getName()).isEqualTo("log");
        assertThat(documentSymbols.get(1).getChildren()).hasSize(2);
        assertThat(documentSymbols.get(1).getChildren().get(0).getName()).isEqualTo("uri");
        assertThat(documentSymbols.get(1).getChildren().get(1).getName()).isEqualTo("metadata");
	}
}
