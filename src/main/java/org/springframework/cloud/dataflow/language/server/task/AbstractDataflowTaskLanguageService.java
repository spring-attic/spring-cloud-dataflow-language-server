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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.core.dsl.ParseException;
import org.springframework.cloud.dataflow.core.dsl.TaskNode;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.dataflow.language.server.DataflowLanguages;
import org.springframework.cloud.dataflow.language.server.support.DataFlowOperationsService;
import org.springframework.cloud.dataflow.language.server.support.DataflowCacheService;
import org.springframework.dsl.document.Document;
import org.springframework.dsl.document.DocumentText;
import org.springframework.dsl.domain.Position;
import org.springframework.dsl.domain.Range;
import org.springframework.dsl.service.AbstractDslService;
import org.springframework.dsl.service.reconcile.DefaultReconcileProblem;
import org.springframework.dsl.service.reconcile.ProblemSeverity;
import org.springframework.dsl.service.reconcile.ProblemType;
import org.springframework.dsl.service.reconcile.ReconcileProblem;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public abstract class AbstractDataflowTaskLanguageService extends AbstractDslService {

	private static final Logger log = LoggerFactory.getLogger(AbstractDataflowTaskLanguageService.class);
	protected DataFlowOperationsService dataflowOperationsService;
	protected DataflowCacheService dataflowCacheService;
	private static final DocumentText envPrefix = DocumentText.from("@env");
	private static final DocumentText namePrefix = DocumentText.from("@name");
	private static final DocumentText descPrefix = DocumentText.from("@desc");
	protected static final DocumentText propPrefix = DocumentText.from("@prop");
	protected static final DocumentText argPrefix = DocumentText.from("@arg");

	public AbstractDataflowTaskLanguageService() {
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
					if (contentStart > -1 && lineContent.startsWith(envPrefix, contentStart)) {
						envItem = item;
					} else if (contentStart > -1 && lineContent.startsWith(namePrefix, contentStart)) {
						nameItem = item;
					} else if (contentStart > -1 && lineContent.startsWith(descPrefix, contentStart)) {
						descItem = item;
					} else if (contentStart > -1 && lineContent.startsWith(propPrefix, contentStart)) {
						launchItems.add(item);
					} else if (contentStart > -1 && lineContent.startsWith(argPrefix, contentStart)) {
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

	private DocumentText parseName(DocumentText text) {
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
				taskName = nameItem.getText()
						.substring(contentRange.getStart().getCharacter() + 6, nameItem.getText().length()).trim()
						.toString();
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
		private ReconcileProblem reconcileProblem;
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
