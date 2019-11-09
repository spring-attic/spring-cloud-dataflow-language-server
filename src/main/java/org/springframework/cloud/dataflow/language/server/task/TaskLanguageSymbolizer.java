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

import java.util.List;

import org.springframework.cloud.dataflow.core.dsl.TaskNode;
import org.springframework.dsl.domain.DocumentSymbol;
import org.springframework.dsl.domain.Range;
import org.springframework.dsl.domain.SymbolInformation;
import org.springframework.dsl.domain.SymbolKind;
import org.springframework.dsl.service.DslContext;
import org.springframework.dsl.service.symbol.SymbolizeInfo;
import org.springframework.dsl.service.symbol.Symbolizer;
import org.springframework.dsl.symboltable.SymbolTable;
import org.springframework.dsl.symboltable.model.ClassSymbol;
import org.springframework.dsl.symboltable.model.LocalScope;
import org.springframework.dsl.symboltable.support.DefaultSymbolTable;
import org.springframework.dsl.symboltable.support.DocumentSymbolTableVisitor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@link Symbolizer} for a {@code task language}.
 *
 * @author Janne Valkealahti
 *
 */
@Component
public class TaskLanguageSymbolizer extends AbstractTaskLanguageService implements Symbolizer {

	@Override
	public SymbolizeInfo symbolize(DslContext context) {
		return symbolizeInternal(context, null);
	}

	@Override
	public SymbolizeInfo symbolize(DslContext context, String query) {
		return symbolizeInternal(context, query);
	}

	private SymbolizeInfo symbolizeInternal(DslContext context, String query) {
		Mono<SymbolizeInfo> symbolizeInfo = parse(context.getDocument())
			.collectList()
			.map(items -> buildTable(items))
			.map(table -> {
				DocumentSymbolTableVisitor visitor = new DocumentSymbolTableVisitor(context.getDocument().uri());
				table.visitSymbolTable(visitor);
				return visitor;
			})
			.map(visitor -> visitor.getSymbolizeInfo());
		return new SymbolizeInfoWrapper(symbolizeInfo);
	}

	private static SymbolTable buildTable(List<TaskItem> items) {
		DefaultSymbolTable table = new DefaultSymbolTable();
		for (TaskItem item : items) {
			TaskNode taskNode = item.getDefinitionItem().getTaskNode();
			if (taskNode == null) {
				continue;
			}
			int line = item.getDefinitionItem().getRange().getStart().getLine();
			int startPos = taskNode.getStartPos();
			int endPos = taskNode.getEndPos();
			String taskName = getTaskName(item);

			LocalScope taskScope = new LocalScope(table.getGlobalScope());
			table.getGlobalScope().nest(taskScope);

			TaskSymbol taskClass = new TaskSymbol(taskName != null ? taskName : "[unnamed]");
			taskClass.setRange(Range.from(line, startPos, line, endPos));
			taskScope.define(taskClass);
		}
		return table;
	}

	private static String getTaskName(TaskItem item) {
		String taskName = item.getDefinitionItem().getTaskNode().getName();
		if (!StringUtils.hasText(taskName)) {
			LaunchItem nameItem = item.getDefinitionItem().getNameItem();
			if (nameItem != null) {
				Range contentRange = nameItem.getContentRange();
				taskName = nameItem.getText()
						.substring(contentRange.getStart().getCharacter() + 5, nameItem.getText().length()).trim()
						.toString();
			}
		}
		return taskName;
	}

	private static class SymbolizeInfoWrapper implements SymbolizeInfo {

		private final Mono<SymbolizeInfo> symbolizeInfo;

		SymbolizeInfoWrapper(Mono<SymbolizeInfo> symbolizeInfo) {
			this.symbolizeInfo = symbolizeInfo.cache();
		}

		@Override
		public Flux<DocumentSymbol> documentSymbols() {
			return symbolizeInfo.map(si -> si.documentSymbols()).flatMapMany(i -> i);
		}

		@Override
		public Flux<SymbolInformation> symbolInformations() {
			return symbolizeInfo.map(si -> si.symbolInformations()).flatMapMany(i -> i);
		}
	}

	public static class TaskSymbol extends ClassSymbol {

		TaskSymbol(String name) {
			super(name);
		}

		@Override
		public SymbolKind getKind() {
			return SymbolKind.Class;
		}
	}
}