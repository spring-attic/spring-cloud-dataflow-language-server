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

import java.util.List;

import org.springframework.dsl.domain.DocumentSymbol;
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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@link Symbolizer} for a {@code app language}.
 *
 * @author Janne Valkealahti
 *
 */
public class AppLanguageSymbolizer extends AbstractAppLanguageService implements Symbolizer {

	@Override
	public SymbolizeInfo symbolize(DslContext context) {
		return symbolizeInternal(context, null);
	}

	@Override
	public SymbolizeInfo symbolize(DslContext context, String query) {
		return symbolizeInternal(context, query);
	}

	private SymbolizeInfo symbolizeInternal(DslContext context, String query) {
		Mono<SymbolizeInfo> symbolizeInfo = Flux.fromIterable(parseApps(context.getDocument()))
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

	private static SymbolTable buildTable(List<AppEntry> items) {
		DefaultSymbolTable table = new DefaultSymbolTable();
		for (AppEntry item : items) {
			String appName = item.getName();

			LocalScope appScope = new LocalScope(table.getGlobalScope());
			table.getGlobalScope().nest(appScope);

			AppSymbol appClass = new AppSymbol(appName != null ? appName : "[unnamed]");
			appClass.setRange(item.getAppRange());
			appScope.define(appClass);

			AppUriSymbol appUriSymbol = new AppUriSymbol("uri");
			appUriSymbol.setRange(item.getAppRange());
			appClass.define(appUriSymbol);

			AppMetadataSymbol appMetadataSymbol = new AppMetadataSymbol("metadata");
			appMetadataSymbol.setRange(item.getMetadataRange());
			appClass.define(appMetadataSymbol);
		}
		return table;
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

	public static class AppSymbol extends ClassSymbol {

		AppSymbol(String name) {
			super(name);
		}

		@Override
		public SymbolKind getKind() {
			return SymbolKind.Class;
		}
	}

	public static class AppUriSymbol extends ClassSymbol {

		AppUriSymbol(String name) {
			super(name);
		}

		@Override
		public SymbolKind getKind() {
			return SymbolKind.Constructor;
		}
	}

	public static class AppMetadataSymbol extends ClassSymbol {

		AppMetadataSymbol(String name) {
			super(name);
		}

		@Override
		public SymbolKind getKind() {
			return SymbolKind.Event;
		}
	}
}
