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
import java.util.function.Function;

import org.springframework.cloud.dataflow.core.dsl.AppNode;
import org.springframework.cloud.dataflow.core.dsl.ArgumentNode;
import org.springframework.cloud.dataflow.core.dsl.StreamNode;
import org.springframework.dsl.domain.Range;
import org.springframework.dsl.domain.SymbolKind;
import org.springframework.dsl.service.DslContext;
import org.springframework.dsl.service.symbol.SymbolizeInfo;
import org.springframework.dsl.service.symbol.Symbolizer;
import org.springframework.dsl.support.DslUtils;
import org.springframework.dsl.symboltable.Symbol;
import org.springframework.dsl.symboltable.SymbolTable;
import org.springframework.dsl.symboltable.model.ClassSymbol;
import org.springframework.dsl.symboltable.model.LocalScope;
import org.springframework.dsl.symboltable.support.DefaultSymbolTable;
import org.springframework.dsl.symboltable.support.DocumentSymbolTableVisitor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Mono;

/**
 * {@link Symbolizer} for a a {@code stream language}.
 * <p>
 * Symbolizing a stream simply means to split it in a pieces which are a logical
 * structure parts in a dsl. This allows user or ide to know lexical references
 * in a dsl to do cross referencing i.e. to rename a symbol, or check symbol
 * validity in its scope.
 * <p>
 *
 * Optinally a stream can have a name:
 *
 * <pre>
 * stream1 = time | log
 *
 * <pre>
 *
 * Apps can have labels:
 *
 * <pre>
 * timeLabel: time | logLabel: log
 *
 * <pre>
 *
 * Instead of piping from an app, named destination can be used:
 *
 * <pre>
 * time > :myevents
 * :myevents > log
 *
 * <pre>
 *
 * @author Janne Valkealahti
 *
 */
@Component
public class StreamLanguageSymbolizer extends AbstractStreamLanguageService implements Symbolizer {

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
				visitor.setSymbolQuery(new SymbolQuery(query));
				table.visitSymbolTable(visitor);
				return visitor;
			})
			.map(visitor -> visitor.getSymbolizeInfo());
		return DslUtils.symbolizeInfoFromMono(symbolizeInfo);
	}

	private static SymbolTable buildTable(List<StreamItem> items) {
		DefaultSymbolTable table = new DefaultSymbolTable();
		for (StreamItem item : items) {
			StreamNode streamNode = item.getDefinitionItem().getStreamNode();
			if (streamNode == null) {
				continue;
			}
			int line = item.getDefinitionItem().getRange().getStart().getLine();
			int startPos = streamNode.getStartPos();
			int endPos = streamNode.getEndPos();
			String streamName = getStreamName(item);

			LocalScope streamScope = new LocalScope(table.getGlobalScope());
			table.getGlobalScope().nest(streamScope);

			StreamSymbol streamClass = new StreamSymbol(streamName != null ? streamName : "[unnamed]");
			streamClass.setRange(Range.from(line, startPos, line, endPos));
			streamScope.define(streamClass);

			for (int i = 0; i < streamNode.getAppNodes().size(); i++) {
				AppNode appNode = streamNode.getAppNodes().get(i);
				String appName = appNode.getName();
				ClassSymbol appClass;
				if (i == 0) {
					appClass = new SourceSymbol(appName);
				} else if (i == streamNode.getAppNodes().size() - 1) {
					appClass = new SinkSymbol(appName);
				} else {
					appClass = new ProcessorSymbol(appName);
				}
				appClass.setRange(Range.from(line, appNode.getStartPos(), line, appNode.getEndPos()));
				streamClass.define(appClass);
				for (ArgumentNode argumentNode : appNode.getArguments()) {
					StreamAppOptionSymbol argumentClass = new StreamAppOptionSymbol(argumentNode.getName());
					argumentClass.setRange(Range.from(line, argumentNode.getStartPos(), line, argumentNode.getEndPos()));
					appClass.define(argumentClass);
				}
			}
		}
		return table;
	}

	private static String getStreamName(StreamItem item) {
		String streamName = item.getDefinitionItem().getStreamNode().getName();
		if (!StringUtils.hasText(streamName)) {
			DeploymentItem nameItem = item.getDefinitionItem().getNameItem();
			if (nameItem != null) {
				Range contentRange = nameItem.getContentRange();
				streamName = nameItem.getText()
						.substring(contentRange.getStart().getCharacter() + 5, nameItem.getText().length()).trim()
						.toString();
			}
		}
		return streamName;
	}

	private static class SymbolQuery implements Function<Symbol, Boolean> {

		private final String query;

		SymbolQuery(String query) {
			this.query = query;
		}

		@Override
		public Boolean apply(Symbol symbol) {
			if (!StringUtils.hasText(query)) {
				return true;
			}
			if (query.startsWith("<")) {
				if (symbol instanceof SourceSymbol) {
					if (query.length() > 1) {
						return symbol.getName().startsWith(query.substring(1));
					}
					return true;
				}
			} else if (query.startsWith("^")) {
				if (symbol instanceof ProcessorSymbol) {
					if (query.length() > 1) {
						return symbol.getName().startsWith(query.substring(1));
					}
					return true;
				}
			} else if (query.startsWith(">")) {
				if (symbol instanceof SinkSymbol) {
					if (query.length() > 1) {
						return symbol.getName().startsWith(query.substring(1));
					}
					return true;
				}
			} else if (query.startsWith("@")) {
				if (symbol instanceof StreamSymbol) {
					if (query.length() > 1) {
						return symbol.getName().startsWith(query.substring(1));
					}
					return true;
				}
			} else {
				return symbol.getName().toLowerCase().contains(query.toLowerCase());
			}
			return false;
		}
	}

	public static class StreamBlock {

	}

	public static class StreamSymbol extends ClassSymbol {

		StreamSymbol(String name) {
			super(name);
		}

		@Override
		public SymbolKind getKind() {
			return SymbolKind.Class;
		}
	}

	public static class SourceSymbol extends ClassSymbol {

		SourceSymbol(String name) {
			super(name);
		}

		@Override
		public SymbolKind getKind() {
			return SymbolKind.Method;
		}
	}

	public static class ProcessorSymbol extends ClassSymbol {

		ProcessorSymbol(String name) {
			super(name);
		}

		@Override
		public SymbolKind getKind() {
			return SymbolKind.Method;
		}
	}

	public static class SinkSymbol extends ClassSymbol {

		SinkSymbol(String name) {
			super(name);
		}

		@Override
		public SymbolKind getKind() {
			return SymbolKind.Method;
		}
	}

	public static class StreamAppOptionSymbol extends ClassSymbol {

		StreamAppOptionSymbol(String name) {
			super(name);
		}

		@Override
		public SymbolKind getKind() {
			return SymbolKind.Field;
		}
	}
}
