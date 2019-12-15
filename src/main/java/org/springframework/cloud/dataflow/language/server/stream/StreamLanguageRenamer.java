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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.cloud.dataflow.language.server.stream.StreamLanguageSymbolizer.StreamSymbol;
import org.springframework.dsl.domain.Position;
import org.springframework.dsl.domain.TextDocumentEdit.TextDocumentEditBuilder;
import org.springframework.dsl.domain.WorkspaceEdit;
import org.springframework.dsl.domain.WorkspaceEdit.WorkspaceEditBuilder;
import org.springframework.dsl.service.DslContext;
import org.springframework.dsl.service.Renamer;
import org.springframework.dsl.support.DslUtils;
import org.springframework.dsl.symboltable.Symbol;
import org.springframework.dsl.symboltable.SymbolTable;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import reactor.core.publisher.Mono;

@Component
public class StreamLanguageRenamer extends AbstractStreamLanguageService implements Renamer {

	@Override
	public Mono<WorkspaceEdit> rename(DslContext context, Position position, String newName) {
		return parseItems(context)
			.map(items -> StreamLanguageSymbolizer.buildTable(items))
			.flatMap(table -> buildEdits(context, table, position, newName));
	}

	private Mono<List<StreamItem>> parseItems(DslContext context) {
		return parse(context.getDocument()).collectList();
	}

	private static Mono<WorkspaceEdit> buildEdits(DslContext context, SymbolTable table, Position position,
			String newName) {
		// build edits for given position by finding matching symbols
		return Mono.justOrEmpty(findSymbol(table, position))
			.flatMap(symbol -> {
				List<Symbol> symbols = table.getAllSymbols().stream()
					.filter(s -> !(s instanceof StreamSymbol))
					.filter(s -> symbol == s || ObjectUtils.nullSafeEquals(s.getName(), symbol.getName()))
					.collect(Collectors.toList());
				// don't expect any IO so return just or empty
				if (!symbols.isEmpty()) {
					WorkspaceEditBuilder<WorkspaceEdit> weBuilder = WorkspaceEdit.workspaceEdit();
					TextDocumentEditBuilder<WorkspaceEditBuilder<WorkspaceEdit>> tdeBuilder = weBuilder
						.documentChangesTextDocumentEdits();
					tdeBuilder.textDocument()
						.uri(context.getDocument().uri())
						.version(context.getDocument().getVersion());
					symbols.stream().forEach(s -> tdeBuilder.edits().newText(newName).range(s.getRange()));
					return Mono.just(weBuilder.build());
				} else {
					// no matches so we don't want workspace edit with empty text document edits
					return Mono.empty();
				}
			});
	}

	private static Optional<? extends Symbol> findSymbol(SymbolTable table, Position position) {
		// find symbol for position
		return table.getAllSymbols().stream()
			.filter(s -> !(s instanceof StreamSymbol))
			.filter(symbol -> DslUtils.isPositionInRange(position, symbol.getRange()))
			.findFirst();
	}
}
