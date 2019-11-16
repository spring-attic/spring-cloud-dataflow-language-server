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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cloud.dataflow.core.dsl.TaskNode;
import org.springframework.cloud.dataflow.language.server.DataflowLanguages;
import org.springframework.cloud.dataflow.language.server.domain.DataflowTaskCreateParams;
import org.springframework.cloud.dataflow.language.server.domain.DataflowTaskDestroyParams;
import org.springframework.cloud.dataflow.language.server.domain.DataflowTaskLaunchParams;
import org.springframework.dsl.document.DocumentText;
import org.springframework.dsl.domain.CodeLens;
import org.springframework.dsl.domain.Range;
import org.springframework.dsl.service.DslContext;
import org.springframework.dsl.service.Lenser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Flux;

@Component
public class TaskLanguageLenser extends AbstractTaskLanguageService implements Lenser {

	@Override
	public Flux<CodeLens> lense(DslContext context) {
		return parse(context.getDocument())
			.flatMap(item -> Flux.fromIterable(codeLensWithProperties(item))
				.concatWithValues(codeLensWithTask(item).toArray(new CodeLens[0])));
	}

	private List<CodeLens> codeLensWithProperties(TaskItem item) {
		return item.getDeployments().stream()
			.map(deployment -> {
				return CodeLens.codeLens()
					.range(deployment.getStartLineRange())
					.command()
						.command(DataflowLanguages.COMMAND_TASK_LAUNCH)
						.title(DataflowLanguages.COMMAND_TASK_LAUNCH_TITLE)
						.argument(DataflowTaskLaunchParams.from(getTaskName(item),
							getTaskEnvironment(deployment, item),
							getDeploymentProperties(deployment.getItems()),
							getCommandLineArgs(deployment.getArgItems())))
						.and()
					.build();
			})
			.collect(Collectors.toList());
	}

	private List<CodeLens> codeLensWithTask(TaskItem item) {
		if (item.getDefinitionItem().getTaskNode() == null) {
			// no task yet, no lenses
			return Collections.emptyList();
		}
		String taskName = getTaskName(item);
		String taskEnvironment = getTaskEnvironment(item);
		String taskDescription = getTaskDescription(item);
		return Arrays.asList(
			CodeLens.codeLens()
				.range(item.getDefinitionItem().getRange())
				.command()
					.command(DataflowLanguages.COMMAND_TASK_CREATE)
					.title(DataflowLanguages.COMMAND_TASK_CREATE_TITLE)
					.argument(DataflowTaskCreateParams.from(taskName, taskEnvironment,
						getDefinition(item.getDefinitionItem().getTaskNode()), taskDescription))
					.and()
				.build(),
			CodeLens.codeLens()
				.range(item.getDefinitionItem().getRange())
				.command()
					.command(DataflowLanguages.COMMAND_TASK_DESTROY)
					.title(DataflowLanguages.COMMAND_TASK_DESTROY_TITLE)
					.argument(DataflowTaskDestroyParams.from(taskName, taskEnvironment))
					.and()
				.build(),
			CodeLens.codeLens()
				.range(item.getDefinitionItem().getRange())
				.command()
					.command(DataflowLanguages.COMMAND_TASK_LAUNCH)
					.title(DataflowLanguages.COMMAND_TASK_LAUNCH_TITLE)
					.argument(DataflowTaskLaunchParams.from(taskName, taskEnvironment))
					.and()
				.build()
		);
	}

	private String getDefinition(TaskNode taskNode) {
		return taskNode.getTaskText().toString();
	}

	private String getTaskName(TaskItem item) {
		DefinitionItem definitionItem = item.getDefinitionItem();
		String taskName = definitionItem.getTaskNode() != null ? definitionItem.getTaskNode().getName() : null;
		if (!StringUtils.hasText(taskName)) {
			LaunchItem nameItem = definitionItem.getNameItem();
			if (nameItem != null) {
				Range contentRange = nameItem.getContentRange();
				taskName = nameItem.getText()
						.substring(contentRange.getStart().getCharacter() + 5, nameItem.getText().length()).trim()
						.toString();
			}
		}
		return taskName;
	}

	private String getTaskDescription(TaskItem item) {
		String taskDescription = null;
		LaunchItem descItem = item.getDefinitionItem().getDescItem();
		if (descItem != null) {
			Range contentRange = descItem.getContentRange();
			taskDescription = descItem.getText()
					.substring(contentRange.getStart().getCharacter() + 5, descItem.getText().length()).trim()
					.toString();
		}
		return taskDescription;
	}

	private String getTaskEnvironment(TaskItem item) {
		String taskEnvironment = null;
		LaunchItem envItem = item.getDefinitionItem().getEnvItem();
		if (envItem != null) {
			Range contentRange = envItem.getContentRange();
			taskEnvironment = envItem.getText()
					.substring(contentRange.getStart().getCharacter() + 4, envItem.getText().length()).trim()
					.toString();
		}
		return taskEnvironment;
	}

	private String getTaskEnvironment(LaunchItems items, TaskItem item) {
		String taskEnvironment = null;
		LaunchItem envItem = items.getEnvItem();
		if (envItem != null) {
			Range contentRange = envItem.getContentRange();
			taskEnvironment = envItem.getText()
					.substring(contentRange.getStart().getCharacter() + 4, envItem.getText().length()).trim()
					.toString();
		}
		if (!StringUtils.hasText(taskEnvironment)) {
			taskEnvironment = getTaskEnvironment(item);
		}
		return taskEnvironment;
	}

	private Map<String, String> getDeploymentProperties(List<LaunchItem> items) {
		HashMap<String, String> properties = new HashMap<String, String>();
		items.stream().forEach(item -> {
			DocumentText[] split = item.getText().splitFirst('=');
			if (split.length == 2) {
				int firstAlphaNumeric = firstLetterOrDigit(split[0]);
				if (firstAlphaNumeric > -1) {
					int lastIndexOf = split[0].indexOf(DataflowLanguages.TEXT_PROP_PREFIX);
					if (lastIndexOf > -1) {
						properties.put(split[0].subSequence(lastIndexOf + DataflowLanguages.TEXT_PROP_PREFIX.length(),
								split[0].length()).toString().trim(), split[1].toString().trim());
					}
				}
			}
		});
		return properties;
	}
	private List<String> getCommandLineArgs(List<LaunchItem> items) {
		List<String> args = new ArrayList<>();
		items.stream().forEach(item -> {
			int lastIndexOf = item.getText().indexOf(DataflowLanguages.TEXT_ARG_PREFIX);
			if (lastIndexOf > -1) {
				args.add(item.getText()
						.subSequence(lastIndexOf + DataflowLanguages.TEXT_ARG_PREFIX.length(), item.getText().length())
						.toString().trim());
			}
		});
		return args;
	}

	private static int firstLetterOrDigit(DocumentText text) {
		for (int i = 0; i < text.length(); i++) {
			if (Character.isLetterOrDigit(text.charAt(i))) {
				return i;
			}
		}
		return -1;
	}
}
