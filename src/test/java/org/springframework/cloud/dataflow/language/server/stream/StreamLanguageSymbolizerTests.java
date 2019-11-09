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
import org.springframework.dsl.domain.DocumentSymbol;
import org.springframework.dsl.domain.Range;
import org.springframework.dsl.domain.SymbolInformation;
import org.springframework.dsl.domain.SymbolKind;
import org.springframework.dsl.service.DslContext;
import org.springframework.dsl.service.symbol.SymbolizeInfo;

public class StreamLanguageSymbolizerTests {

    private final StreamLanguageSymbolizer symbolizer = new StreamLanguageSymbolizer();

	@BeforeEach
	public void setup() {
		symbolizer.setDataflowCacheService(new DataflowCacheService());
		symbolizer.setDataflowOperationsService(new DataFlowOperationsService());
	}

    @Test
    public void testSimpleStreamWithName() {
        Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0, "stream1 = time --initial-delay=1000 | log --name=mylogger");
        SymbolizeInfo symbolizeInfo = symbolizer.symbolize(DslContext.builder().document(document).build());

        List<SymbolInformation> symbolInformations = symbolizeInfo.symbolInformations().toStream()
                .collect(Collectors.toList());
        List<DocumentSymbol> documentSymbols = symbolizeInfo.documentSymbols().toStream().collect(Collectors.toList());

        assertThat(symbolInformations).hasSize(5);
        assertThat(symbolInformations.get(0).getName()).isEqualTo("initial-delay");
        assertThat(symbolInformations.get(1).getName()).isEqualTo("time");
        assertThat(symbolInformations.get(2).getName()).isEqualTo("name");
        assertThat(symbolInformations.get(3).getName()).isEqualTo("log");
        assertThat(symbolInformations.get(4).getName()).isEqualTo("stream1");

        assertThat(documentSymbols).hasSize(1);
        assertThat(documentSymbols.get(0).getName()).isEqualTo("stream1");
        assertThat(documentSymbols.get(0).getDetail()).isNull();
        assertThat(documentSymbols.get(0).getKind()).isEqualTo(SymbolKind.Class);
        assertThat(documentSymbols.get(0).getRange()).isEqualTo(Range.from(0, 10, 0, 57));
        assertThat(documentSymbols.get(0).getSelectionRange()).isEqualTo(Range.from(0, 10, 0, 57));
        assertThat(documentSymbols.get(0).getChildren()).isNotNull();
        assertThat(documentSymbols.get(0).getChildren()).hasSize(2);

        assertThat(documentSymbols.get(0).getChildren().get(0)).isNotNull();
        assertThat(documentSymbols.get(0).getChildren().get(0).getName()).isEqualTo("time");
        assertThat(documentSymbols.get(0).getChildren().get(0).getKind()).isEqualTo(SymbolKind.Method);
        assertThat(documentSymbols.get(0).getChildren().get(0).getRange()).isEqualTo(Range.from(0, 10, 0, 35));
        assertThat(documentSymbols.get(0).getChildren().get(0).getSelectionRange()).isEqualTo(Range.from(0, 10, 0, 35));
        assertThat(documentSymbols.get(0).getChildren().get(0).getChildren()).isNotNull();
        assertThat(documentSymbols.get(0).getChildren().get(0).getChildren()).hasSize(1);
        assertThat(documentSymbols.get(0).getChildren().get(0).getChildren().get(0).getName()).isEqualTo("initial-delay");
        assertThat(documentSymbols.get(0).getChildren().get(0).getChildren().get(0).getRange()).isEqualTo(Range.from(0, 15, 0, 35));

        assertThat(documentSymbols.get(0).getChildren().get(1)).isNotNull();
        assertThat(documentSymbols.get(0).getChildren().get(1).getName()).isEqualTo("log");
        assertThat(documentSymbols.get(0).getChildren().get(1).getKind()).isEqualTo(SymbolKind.Method);
        assertThat(documentSymbols.get(0).getChildren().get(1).getRange()).isEqualTo(Range.from(0, 38, 0, 57));
        assertThat(documentSymbols.get(0).getChildren().get(1).getSelectionRange()).isEqualTo(Range.from(0, 38, 0, 57));
        assertThat(documentSymbols.get(0).getChildren().get(1).getChildren()).isNotNull();
        assertThat(documentSymbols.get(0).getChildren().get(1).getChildren()).hasSize(1);
        assertThat(documentSymbols.get(0).getChildren().get(1).getChildren().get(0).getName()).isEqualTo("name");
        assertThat(documentSymbols.get(0).getChildren().get(1).getChildren().get(0).getRange()).isEqualTo(Range.from(0, 42, 0, 57));
    }

    @Test
    public void testQuery() {
        Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0, "stream1 = time --initial-delay=1000 | log --name=mylogger");

        SymbolizeInfo symbolizeInfo = symbolizer.symbolize(DslContext.builder().document(document).build(), "<");
        List<SymbolInformation> symbolInformations = symbolizeInfo.symbolInformations().toStream()
                .collect(Collectors.toList());
        assertThat(symbolInformations).hasSize(1);

        symbolizeInfo = symbolizer.symbolize(DslContext.builder().document(document).build(), "<time");
        symbolInformations = symbolizeInfo.symbolInformations().toStream().collect(Collectors.toList());
        assertThat(symbolInformations).hasSize(1);

        symbolizeInfo = symbolizer.symbolize(DslContext.builder().document(document).build(), "<xx");
        symbolInformations = symbolizeInfo.symbolInformations().toStream().collect(Collectors.toList());
        assertThat(symbolInformations).hasSize(0);

        symbolizeInfo = symbolizer.symbolize(DslContext.builder().document(document).build(), ">");
        symbolInformations = symbolizeInfo.symbolInformations().toStream().collect(Collectors.toList());
        assertThat(symbolInformations).hasSize(1);

        symbolizeInfo = symbolizer.symbolize(DslContext.builder().document(document).build(), ">log");
        symbolInformations = symbolizeInfo.symbolInformations().toStream().collect(Collectors.toList());
        assertThat(symbolInformations).hasSize(1);

        symbolizeInfo = symbolizer.symbolize(DslContext.builder().document(document).build(), ">xx");
        symbolInformations = symbolizeInfo.symbolInformations().toStream().collect(Collectors.toList());
        assertThat(symbolInformations).hasSize(0);

        symbolizeInfo = symbolizer.symbolize(DslContext.builder().document(document).build(), "@");
        symbolInformations = symbolizeInfo.symbolInformations().toStream().collect(Collectors.toList());
        assertThat(symbolInformations).hasSize(1);

        symbolizeInfo = symbolizer.symbolize(DslContext.builder().document(document).build(), "@stream");
        symbolInformations = symbolizeInfo.symbolInformations().toStream().collect(Collectors.toList());
        assertThat(symbolInformations).hasSize(1);

        symbolizeInfo = symbolizer.symbolize(DslContext.builder().document(document).build(), "@xx");
        symbolInformations = symbolizeInfo.symbolInformations().toStream().collect(Collectors.toList());
        assertThat(symbolInformations).hasSize(0);

        symbolizeInfo = symbolizer.symbolize(DslContext.builder().document(document).build(), "stream");
        symbolInformations = symbolizeInfo.symbolInformations().toStream().collect(Collectors.toList());
        assertThat(symbolInformations).hasSize(1);
    }

    @Test
    public void testPartialOnlyName() {
        Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0, "stream1 =");
        SymbolizeInfo symbolizeInfo = symbolizer.symbolize(DslContext.builder().document(document).build());

        List<SymbolInformation> symbolInformations = symbolizeInfo.symbolInformations().toStream()
                .collect(Collectors.toList());
        List<DocumentSymbol> documentSymbols = symbolizeInfo.documentSymbols().toStream().collect(Collectors.toList());

        assertThat(symbolInformations).hasSize(0);
        assertThat(documentSymbols).hasSize(0);
    }

    @Test
    public void testStreamsSameNameDifferentEnv() {
        Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0,
                AbstractStreamLanguageServiceTests.DSL_STREAMS_SAME_NAMES);
        SymbolizeInfo symbolizeInfo = symbolizer.symbolize(DslContext.builder().document(document).build());

        List<SymbolInformation> symbolInformations = symbolizeInfo.symbolInformations().toStream()
                .collect(Collectors.toList());
        List<DocumentSymbol> documentSymbols = symbolizeInfo.documentSymbols().toStream().collect(Collectors.toList());

        assertThat(symbolInformations).hasSize(6);
        assertThat(symbolInformations.get(0).getName()).isEqualTo("time");
        assertThat(symbolInformations.get(1).getName()).isEqualTo("log");
        assertThat(symbolInformations.get(2).getName()).isEqualTo("name");
        assertThat(symbolInformations.get(3).getName()).isEqualTo("time");
        assertThat(symbolInformations.get(4).getName()).isEqualTo("log");
        assertThat(symbolInformations.get(5).getName()).isEqualTo("name");

        assertThat(documentSymbols).hasSize(2);

        assertThat(documentSymbols.get(0).getName()).isEqualTo("name");
        assertThat(documentSymbols.get(0).getDetail()).isNull();
        assertThat(documentSymbols.get(0).getKind()).isEqualTo(SymbolKind.Class);
        assertThat(documentSymbols.get(0).getRange()).isEqualTo(Range.from(3, 0, 3, 8));
        assertThat(documentSymbols.get(0).getSelectionRange()).isEqualTo(Range.from(3, 0, 3, 8));
        assertThat(documentSymbols.get(0).getChildren()).isNotNull();
        assertThat(documentSymbols.get(0).getChildren()).hasSize(2);

        assertThat(documentSymbols.get(0).getChildren().get(0)).isNotNull();
        assertThat(documentSymbols.get(0).getChildren().get(0).getName()).isEqualTo("time");
        assertThat(documentSymbols.get(0).getChildren().get(0).getKind()).isEqualTo(SymbolKind.Method);
        assertThat(documentSymbols.get(0).getChildren().get(0).getRange()).isEqualTo(Range.from(3, 0, 3, 4));
        assertThat(documentSymbols.get(0).getChildren().get(0).getSelectionRange()).isEqualTo(Range.from(3, 0, 3, 4));
        assertThat(documentSymbols.get(0).getChildren().get(0).getChildren()).isNull();

        assertThat(documentSymbols.get(0).getChildren().get(1)).isNotNull();
        assertThat(documentSymbols.get(0).getChildren().get(1).getName()).isEqualTo("log");
        assertThat(documentSymbols.get(0).getChildren().get(1).getKind()).isEqualTo(SymbolKind.Method);
        assertThat(documentSymbols.get(0).getChildren().get(1).getRange()).isEqualTo(Range.from(3, 5, 3, 8));
        assertThat(documentSymbols.get(0).getChildren().get(1).getSelectionRange()).isEqualTo(Range.from(3, 5, 3, 8));
        assertThat(documentSymbols.get(0).getChildren().get(1).getChildren()).isNull();

        assertThat(documentSymbols.get(1).getName()).isEqualTo("name");
        assertThat(documentSymbols.get(1).getDetail()).isNull();
        assertThat(documentSymbols.get(1).getKind()).isEqualTo(SymbolKind.Class);
        assertThat(documentSymbols.get(1).getRange()).isEqualTo(Range.from(8, 0, 8, 8));
        assertThat(documentSymbols.get(1).getSelectionRange()).isEqualTo(Range.from(8, 0, 8, 8));
        assertThat(documentSymbols.get(1).getChildren()).isNotNull();
        assertThat(documentSymbols.get(1).getChildren()).hasSize(2);

        assertThat(documentSymbols.get(1).getChildren().get(0)).isNotNull();
        assertThat(documentSymbols.get(1).getChildren().get(0).getName()).isEqualTo("time");
        assertThat(documentSymbols.get(1).getChildren().get(0).getKind()).isEqualTo(SymbolKind.Method);
        assertThat(documentSymbols.get(1).getChildren().get(0).getRange()).isEqualTo(Range.from(8, 0, 8, 4));
        assertThat(documentSymbols.get(1).getChildren().get(0).getSelectionRange()).isEqualTo(Range.from(8, 0, 8, 4));
        assertThat(documentSymbols.get(1).getChildren().get(0).getChildren()).isNull();

        assertThat(documentSymbols.get(1).getChildren().get(1)).isNotNull();
        assertThat(documentSymbols.get(1).getChildren().get(1).getName()).isEqualTo("log");
        assertThat(documentSymbols.get(1).getChildren().get(1).getKind()).isEqualTo(SymbolKind.Method);
        assertThat(documentSymbols.get(1).getChildren().get(1).getRange()).isEqualTo(Range.from(8, 5, 8, 8));
        assertThat(documentSymbols.get(1).getChildren().get(1).getSelectionRange()).isEqualTo(Range.from(8, 5, 8, 8));
        assertThat(documentSymbols.get(1).getChildren().get(1).getChildren()).isNull();
    }
}
