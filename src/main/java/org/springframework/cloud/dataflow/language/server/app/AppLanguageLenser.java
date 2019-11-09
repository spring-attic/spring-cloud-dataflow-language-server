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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.dataflow.language.server.DataflowLanguages;
import org.springframework.dsl.domain.CodeLens;
import org.springframework.dsl.service.DslContext;
import org.springframework.dsl.service.Lenser;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;

/**
 * {@link Lenser} implementation for {@link DataflowLanguages#LANGUAGE_APP}.
 *
 * @author Janne Valkealahti
 *
 */
@Component
public class AppLanguageLenser extends AbstractAppLanguageService implements Lenser {

	private final static Logger log = LoggerFactory.getLogger(AppLanguageLenser.class);

	@Override
	public Flux<CodeLens> lense(DslContext context) {
		return Flux.defer(() -> {
			return Flux.fromIterable(parseApps(context.getDocument()))
				.flatMap(item -> {
					return Flux.just(
						CodeLens.codeLens()
							.range(item.getAppRange())
							.command()
								.command(DataflowLanguages.COMMAND_APP_REGISTER)
								.title(DataflowLanguages.COMMAND_APP_REGISTER_TITLE)
								.argument(item.getType())
								.argument(item.getName())
								.argument(item.getAppUri())
								.argument(item.getMetadataUri())
								.and()
							.build(),
						CodeLens.codeLens()
							.range(item.getAppRange())
							.command()
								.command(DataflowLanguages.COMMAND_APP_UNREGISTER)
								.title(DataflowLanguages.COMMAND_APP_UNREGISTER_TITLE)
								.argument(item.getType())
								.argument(item.getName())
								.argument(item.getVersion())
								.and()
							.build()
					);
				})
				.doOnNext(l -> {
					log.info("LENS: {}", l.getCommand().getArguments());
				})
			;
		});
	}
}
