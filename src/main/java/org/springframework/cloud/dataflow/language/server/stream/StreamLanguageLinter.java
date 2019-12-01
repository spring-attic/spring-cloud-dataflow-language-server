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

import org.springframework.dsl.service.DslContext;
import org.springframework.dsl.service.reconcile.DefaultReconcileProblem;
import org.springframework.dsl.service.reconcile.Linter;
import org.springframework.dsl.service.reconcile.ReconcileProblem;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class StreamLanguageLinter extends AbstractStreamLanguageService implements Linter {

	@Override
	public Flux<ReconcileProblem> lint(DslContext context) {
		return parse(context.getDocument())
			.flatMap(item -> Flux.concat(definitionProblem(item), nameProblem(item)));
	}

	private Mono<ReconcileProblem> definitionProblem(StreamItem item) {
		return Mono.justOrEmpty(item.getDefinitionItem().getReconcileProblem());
	}

	private Mono<ReconcileProblem> nameProblem(StreamItem item) {
		ReconcileProblem problem = null;
		String streamName = null;
		if (item.getDefinitionItem() != null && item.getDefinitionItem().getStreamNode() != null) {
			streamName = item.getDefinitionItem().getStreamNode().getStreamName();
		}
		if (!StringUtils.hasText(streamName)) {
			DeploymentItem nameItem = item.getDefinitionItem().getNameItem();
			if (nameItem == null || (nameItem != null && nameItem.getText().length() < 1)) {
				problem = new DefaultReconcileProblem(new ErrorProblemType(""), "Stream name missing", item.getRange());
			}
		}
		return Mono.justOrEmpty(problem);
	}
}
