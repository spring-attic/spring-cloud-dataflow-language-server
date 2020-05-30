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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.core.dsl.ParseException;
import org.springframework.cloud.dataflow.core.dsl.TaskNode;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.dataflow.language.server.DataflowLanguages;
import org.springframework.cloud.dataflow.language.server.domain.DataflowEnvironmentParams;
import org.springframework.cloud.dataflow.language.server.domain.DataflowEnvironmentParams.Environment;
import org.springframework.cloud.dataflow.language.server.support.DataFlowOperationsService;
import org.springframework.cloud.dataflow.language.server.support.DataflowCacheService;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.dsl.document.Document;
import org.springframework.dsl.document.DocumentText;
import org.springframework.dsl.domain.Position;
import org.springframework.dsl.domain.Range;
import org.springframework.dsl.jsonrpc.session.JsonRpcSession;
import org.springframework.dsl.lsp.LspSystemConstants;
import org.springframework.dsl.service.AbstractDslService;
import org.springframework.dsl.service.DslContext;
import org.springframework.dsl.service.reconcile.DefaultReconcileProblem;
import org.springframework.dsl.service.reconcile.ProblemSeverity;
import org.springframework.dsl.service.reconcile.ProblemType;
import org.springframework.dsl.service.reconcile.ReconcileProblem;
import org.springframework.dsl.support.DslUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public abstract class AbstractTaskLanguageService extends AbstractDslService {

	private static final Logger log = LoggerFactory.getLogger(AbstractTaskLanguageService.class);
	protected DataFlowOperationsService dataflowOperationsService;
	protected DataflowCacheService dataflowCacheService;

	public AbstractTaskLanguageService() {
		super(DataflowLanguages.LANGUAGE_TASK);
	}

	@Autowired
	public void setDataflowOperationsService(DataFlowOperationsService dataflowOperationsService) {
		this.dataflowOperationsService = dataflowOperationsService;
	}

	@Autowired
	public void setDataflowCacheService(DataflowCacheService dataflowCacheService) {
		this.dataflowCacheService = dataflowCacheService;
	}

	protected static class ErrorProblemType implements ProblemType {

		private final String code;

		ErrorProblemType(String code) {
			this.code = code;
		}

		@Override
		public ProblemSeverity getSeverity() {
			return ProblemSeverity.ERROR;
		}

		@Override
		public String getCode() {
			return code;
		}
	}

	protected DataFlowOperations resolveDataFlowOperations(DslContext context, Position position) {
		JsonRpcSession session = context.getAttribute(LspSystemConstants.CONTEXT_SESSION_ATTRIBUTE);
		if (session == null) {
			return null;
		}
		DataflowEnvironmentParams params = session
				.getAttribute(DataflowLanguages.CONTEXT_SESSION_ENVIRONMENTS_ATTRIBUTE);
		String defaultEnvironment = resolveEnvironmentName(context, position, params);
		List<Environment> environments = params != null ? params.getEnvironments() : Collections.emptyList();
		Environment environment = environments.stream()
			.filter(env -> ObjectUtils.nullSafeEquals(defaultEnvironment, env.getName()))
			.findFirst()
			.orElse(null);
		if (environment != null) {
			try {
				log.debug("Getting DataFlowTemplate for environment {}", defaultEnvironment);
				return dataflowOperationsService.getDataFlowOperations(environment, params.getTrustssl());
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}

	protected String resolveEnvironmentName(DslContext context, Position position, DataflowEnvironmentParams params) {
		String defaultEnvironment = resolveDefinedEnvironmentName(context, position);
		if (defaultEnvironment == null && params != null) {
			defaultEnvironment = params.getDefaultEnvironment();
		}
		return defaultEnvironment;
	}

	protected String resolveDefinedEnvironmentName(DslContext context, Position position) {
		for (TaskItem item : parseCached(context.getDocument())) {
			if (DslUtils.isPositionInRange(position, item.getRange())) {
				DefinitionItem definitionItem = item.getDefinitionItem();
				if (definitionItem != null) {
					LaunchItem envItem = definitionItem.getEnvItem();
					if (envItem != null) {
						Range contentRange = envItem.getContentRange();
						String envName = envItem.getText()
								.substring(contentRange.getStart().getCharacter() + 5, envItem.getText().length())
								.trim().toString();
						if (StringUtils.hasText(envName)) {
							return envName;
						}
					}
				}
			}
		}
		return null;
	}

	protected Flux<TaskItem> parse(Document document) {
		return parseCachedMono(document).flatMapMany(items -> Flux.fromIterable(items));
	}

	protected Mono<List<TaskItem>> parseCachedMono(Document document) {
		return Mono.defer(() -> {
			return Mono.just(parseCached(document));
		});
	}

	protected List<TaskItem> parseCached(Document document) {
		String cacheKey = document.uri() + "#" + document.getVersion();
		log.debug("Used cache key for streamItemCache is {}", cacheKey);
		return dataflowCacheService.getTaskItemCache().get(cacheKey, key -> {
			return parseTasks(document);
		});
	}

	private List<TaskItem> parseTasks(Document document) {
		ArrayList<TaskItem> items = new ArrayList<>();
		TaskItem item = null;
		do {
			item = parseNextTask(document, item);
			if (item != null) {
				items.add(item);
			}
		} while (item != null);
		return items;
	}

	private static int findContentStart(DocumentText text) {
		for (int i = 0; i < text.length(); i++) {
			if (Character.isWhitespace(text.charAt(i))) {
				continue;
			} else if (text.charAt(i) == '-') {
				continue;
			} else if (text.charAt(i) == '#') {
				continue;
			} else if (text.charAt(i) == '@') {
				return i;
			} else if (Character.isLetterOrDigit(text.charAt(i))) {
				return i;
			}
		}
		return -1;
	}

	private TaskItem parseNextTask(Document document, TaskItem previous) {
		List<LaunchItems> launches = new ArrayList<>();
		List<LaunchItem> launchItems = new ArrayList<>();
		List<LaunchItem> launchArgItems = new ArrayList<>();
		LaunchItem envItem = null;
		LaunchItem nameItem = null;
		LaunchItem descItem = null;
		Range launchItemsRange = null;
		Position launchItemsStart = null;
		Position launchItemsEnd = null;
		TaskItem taskItem = null;
		int lineCount = document.lineCount();
		int start = previous != null ? previous.range.getEnd().getLine() + 1 : 0;
		Range lineRange = null;

		for (int line = start; taskItem == null && line < lineCount; line++) {
			lineRange = document.getLineRange(line);
			DocumentText lineContent = document.content(lineRange);
			DocumentText trim = lineContent.trimStart();
			if (trim.hasText() && Character.isLetterOrDigit(trim.charAt(0))) {
				DefinitionItem definitionItem = parseDefinition(lineContent, nameItem, line);
				definitionItem.range = lineRange;
				definitionItem.envItem = envItem;
				definitionItem.nameItem = nameItem;
				definitionItem.descItem = descItem;
				taskItem = new TaskItem();
				taskItem.definitionItem = definitionItem;
				taskItem.range = Range.from(start, 0, line, lineContent.length());
				if (!launchItems.isEmpty()) {
					LaunchItems items = new LaunchItems();
					items.envItem = envItem;
					items.startLineRange = launchItemsRange;
					items.range = Range.from(launchItemsStart, launchItemsEnd);
					items.items.addAll(launchItems);
					launches.add(items);
				}
				taskItem.deployments.addAll(new ArrayList<>(launches));
				launchItems.clear();
				launchArgItems.clear();
				launches.clear();
				envItem = null;
				nameItem = null;
				descItem = null;
				launchItemsStart = null;
			} else {
				if (trim.length() > 2 && (trim.charAt(0) == '#' || trim.charAt(0) == '-')) {
					LaunchItem item = new LaunchItem();
					item.range = lineRange;
					item.text = lineContent;

					int contentStart = findContentStart(lineContent);
					if (contentStart > -1) {
						item.contentRange = Range.from(lineRange.getStart().getLine(), contentStart,
								lineRange.getEnd().getLine(), lineRange.getEnd().getCharacter());
					}
					boolean match = true;
					if (contentStart > -1 && lineContent.startsWith(DataflowLanguages.TEXT_ENV_PREFIX, contentStart)) {
						envItem = item;
					} else if (contentStart > -1 && lineContent.startsWith(DataflowLanguages.TEXT_NAME_PREFIX, contentStart)) {
						nameItem = item;
					} else if (contentStart > -1 && lineContent.startsWith(DataflowLanguages.TEXT_DESC_PREFIX, contentStart)) {
						descItem = item;
					} else if (contentStart > -1 && lineContent.startsWith(DataflowLanguages.TEXT_PROP_PREFIX, contentStart)) {
						launchItems.add(item);
					} else if (contentStart > -1 && lineContent.startsWith(DataflowLanguages.TEXT_ARG_PREFIX, contentStart)) {
						launchArgItems.add(item);
					} else {
						match = false;
					}
					if (match) {
						if (launchItemsStart == null) {
							launchItemsStart = lineRange.getStart();
							launchItemsRange = lineRange;
						}
						launchItemsEnd = lineRange.getEnd();
					}
				} else {
					if (!launchItems.isEmpty() || !launchArgItems.isEmpty()) {
						LaunchItems items = new LaunchItems();
						items.envItem = envItem;
						items.startLineRange = launchItemsRange;
						items.range = Range.from(launchItemsStart, launchItemsEnd);
						items.items.addAll(launchItems);
						items.argItems.addAll(launchArgItems);
						launches.add(items);
						envItem = null;
						nameItem = null;
						descItem = null;
						launchItemsStart = null;
					}
					launchItems.clear();
					launchArgItems.clear();
				}
			}
		}
		// no check case when with metadata but no dsl
		if (envItem != null || nameItem != null || descItem != null) {
			DefinitionItem definitionItem = new DefinitionItem();
			definitionItem.envItem = envItem;
			definitionItem.nameItem = nameItem;
			definitionItem.descItem = descItem;
			taskItem = new TaskItem();
			taskItem.definitionItem = definitionItem;
			taskItem.range = Range.from(launchItemsStart, lineRange.getEnd());
		}

		return taskItem;
	}

	protected DocumentText parseName(DocumentText text) {
		if (text.length() == 0) {
			return null;
		}
		int i = 0;
		do {
			char c = text.charAt(i);
			if (Character.isLetterOrDigit(c) || Character.isWhitespace(c)) {
				i++;
				continue;
			} else if (c == '=') {
				return text.subtext(0, i + 1);
			}
			i++;
		} while (i < text.length());
		return null;
	}

	private DefinitionItem parseDefinition(DocumentText text, LaunchItem nameItem, int line) {
		DefinitionItem definitionItem = new DefinitionItem();
		try {
			int l = 0;
			String taskName = null;
			DocumentText name = parseName(text);
			if (name != null) {
				l = name.length();
				name = name.trim();
				int i = name.length();
				name = name.subtext(0, i - 1);
				name = name.trim();
				taskName = name.toString();
			}
			if (taskName == null && nameItem != null) {
				Range contentRange = nameItem.getContentRange();
				DocumentText nameItemText = nameItem.getText();
				int s = contentRange.getStart().getCharacter() + 6;
				if (nameItemText.length() > s) {
					taskName = nameItemText.substring(s, nameItem.getText().length()).trim().toString();
				}
			}
			if (taskName == null) {
				Range range = Range.from(line, 0, line, 0);
				DefaultReconcileProblem problem = new DefaultReconcileProblem(new ErrorProblemType(""),
						"Task Definition must have a name", range);
				definitionItem.reconcileProblem = problem;
				return definitionItem;
			}
			TaskParser parser = new TaskParser(taskName, text.subtext(l, text.length()).toString(), true, true);
			TaskNode node = parser.parse();
			definitionItem.taskNode = node;
		} catch (ParseException e) {
			String message = e.getMessage();
			int position = e.getPosition();
			Range range = Range.from(line, position, line, position);
			DefaultReconcileProblem problem = new DefaultReconcileProblem(new ErrorProblemType(""), message, range);
			definitionItem.reconcileProblem = problem;
		}
		return definitionItem;
	}

	public static class LaunchItems {
		private List<LaunchItem> items = new ArrayList<>();
		private List<LaunchItem> argItems = new ArrayList<>();
		private Range startLineRange;
		private Range range;
		private LaunchItem envItem;

		public List<LaunchItem> getItems() {
			return items;
		}

		public List<LaunchItem> getArgItems() {
			return argItems;
		}

		public LaunchItem getEnvItem() {
			return envItem;
		}

		public Range getStartLineRange() {
			return startLineRange;
		}

		public Range getRange() {
			return range;
		}
	}

	public static class LaunchItem {
		private Range contentRange;
		private Range range;
		private DocumentText text;

		public Range getRange() {
			return range;
		}

		public Range getContentRange() {
			return contentRange;
		}

		public DocumentText getText() {
			return text;
		}
	}

	public static class DefinitionItem {
		private TaskNode taskNode;
		private Range range;
		protected ReconcileProblem reconcileProblem;
		private LaunchItem envItem;
		private LaunchItem nameItem;
		private LaunchItem descItem;

		public TaskNode getTaskNode() {
			return taskNode;
		}

		public Range getRange() {
			return range;
		}

		public ReconcileProblem getReconcileProblem() {
			return reconcileProblem;
		}

		public LaunchItem getEnvItem() {
			return envItem;
		}

		public LaunchItem getNameItem() {
			return nameItem;
		}

		public LaunchItem getDescItem() {
			return descItem;
		}
	}

	public static class TaskItem {
		private List<LaunchItems> deployments = new ArrayList<>();
		private DefinitionItem definitionItem;
		private Range range;

		public List<LaunchItems> getDeployments() {
			return deployments;
		}

		public DefinitionItem getDefinitionItem() {
			return definitionItem;
		}

		public Range getRange() {
			return range;
		}
	}
}
