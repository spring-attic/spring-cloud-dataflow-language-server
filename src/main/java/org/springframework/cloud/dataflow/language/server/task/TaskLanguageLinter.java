/*
 * Copyright 2019-2020 the original author or authors.
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
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.dsl.TaskApp;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.resource.AppRegistrationResource;
import org.springframework.dsl.service.DslContext;
import org.springframework.dsl.service.reconcile.DefaultReconcileProblem;
import org.springframework.dsl.service.reconcile.Linter;
import org.springframework.dsl.service.reconcile.ReconcileProblem;
import org.springframework.hateoas.PagedModel;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;

@Component
public class TaskLanguageLinter extends AbstractTaskLanguageService implements Linter {

	private static final Logger log = LoggerFactory.getLogger(TaskLanguageLinter.class);

	@Override
	public Flux<ReconcileProblem> lint(DslContext context) {
		return Flux.defer(() -> {
			return parse(context.getDocument())
				.map(item -> checkName(context, item))
				.filter(item -> item.getDefinitionItem().getReconcileProblem() != null)
				.map(item -> item.getDefinitionItem().getReconcileProblem());
		});
	}

	private TaskItem checkName(DslContext context, TaskItem taskItem) {
		DefinitionItem definitionItem = taskItem.getDefinitionItem();
		if (definitionItem.getReconcileProblem() == null) {
			DataFlowOperations operations = resolveDataFlowOperations(context, definitionItem.getRange().getStart());
			if (operations == null) {
				return taskItem;
			}
			PagedModel<AppRegistrationResource> taskAppsResource = operations.appRegistryOperations()
					.list(ApplicationType.task);
			Set<String> validNames = taskAppsResource.getContent().stream()
					.map(appRegistration -> appRegistration.getName()).collect(Collectors.toSet());
			List<TaskApp> taskApps = definitionItem.getTaskNode().getTaskApps();
			for (TaskApp taskApp : taskApps) {
				if (!validNames.contains(taskApp.getName())) {
					DefaultReconcileProblem problem = new DefaultReconcileProblem(new ErrorProblemType(""),
							"Task app " + taskApp.getName() + " is not registered", definitionItem.getRange());
					definitionItem.reconcileProblem = problem;
					log.debug("Setting problem {}", problem);
					break;
				}
			}
		}
		return taskItem;
	}
}
