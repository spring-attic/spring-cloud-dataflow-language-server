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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.core.dsl.ParseException;
import org.springframework.cloud.dataflow.core.dsl.StreamNode;
import org.springframework.cloud.dataflow.core.dsl.StreamParser;
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
import org.springframework.dsl.model.LanguageId;
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

public abstract class AbstractStreamLanguageService extends AbstractDslService {

	private static final Logger log = LoggerFactory.getLogger(AbstractStreamLanguageService.class);
	protected DataFlowOperationsService dataflowOperationsService;
	protected DataflowCacheService dataflowCacheService;

	public AbstractStreamLanguageService() {
		super(DataflowLanguages.LANGUAGE_STREAM);
	}

	public AbstractStreamLanguageService(LanguageId languageId) {
		super(languageId);
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
		DataflowEnvironmentParams params = session
				.getAttribute(DataflowLanguages.CONTEXT_SESSION_ENVIRONMENTS_ATTRIBUTE);
		String defaultEnvironment = resolveEnvironmentName(context, position, params);
		List<Environment> environments = params.getEnvironments();
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
		if (defaultEnvironment == null) {
			defaultEnvironment = params.getDefaultEnvironment();
		}
		return defaultEnvironment;
	}

	protected String resolveDefinedEnvironmentName(DslContext context, Position position) {
		for (StreamItem item : parseCached(context.getDocument())) {
			if (DslUtils.isPositionInRange(position, item.getRange())) {
				DefinitionItem definitionItem = item.getDefinitionItem();
				if (definitionItem != null) {
					DeploymentItem envItem = definitionItem.getEnvItem();
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

	protected Mono<List<StreamItem>> parseCachedMono(Document document) {
		return Mono.defer(() -> {
			return Mono.just(parseCached(document));
		});
	}

	protected List<StreamItem> parseCached(Document document) {
		String cacheKey = document.uri() + "#" + document.getVersion();
		log.debug("Used cache key for streamItemCache is {}", cacheKey);
		return dataflowCacheService.getStreamItemCache().get(cacheKey, key -> {
			return parseStreams(document);
		});
	}

	protected Flux<StreamItem> parse(Document document) {
		return parseCachedMono(document).flatMapMany(items -> Flux.fromIterable(items));
	}

	private List<StreamItem> parseStreams(Document document) {
		ArrayList<StreamItem> items = new ArrayList<>();
		StreamItem item = null;
		do {
			item = parseNextStream(document, item);
			if (item != null) {
				items.add(item);
			}
		} while (item != null);
		return items;
	}

	private StreamItem parseNextStream(Document document, StreamItem previous) {
		List<DeploymentItems> deployments = new ArrayList<>();
		List<DeploymentItem> deploymentItems = new ArrayList<>();
		DeploymentItem envItem = null;
		DeploymentItem nameItem = null;
		DeploymentItem descItem = null;
		Range deploymentItemsRange = null;
		Position deploymentItemsStart = null;
		Position deploymentItemsEnd = null;
		StreamItem streamItem = null;
		int lineCount = document.lineCount();
		int start = previous != null ? previous.range.getEnd().getLine() + 1 : 0;
		Range lineRange = null;
		List<Range> commentRanges = new ArrayList<>();
		Range commentRange = null;

		for (int line = start; streamItem == null && line < lineCount; line++) {
			lineRange = document.getLineRange(line);
			DocumentText lineContent = document.content(lineRange);
			DocumentText trim = lineContent.trimStart();
			if (trim.hasText() && Character.isLetterOrDigit(trim.charAt(0))) {
				DefinitionItem definitionItem = parseDefinition(lineContent, line);
				definitionItem.range = lineRange;
				definitionItem.envItem = envItem;
				definitionItem.nameItem = nameItem;
				definitionItem.descItem = descItem;
				streamItem = new StreamItem();
				streamItem.definitionItem = definitionItem;
				streamItem.range = Range.from(start, 0, line, lineContent.length());
				if (!deploymentItems.isEmpty()) {
					DeploymentItems items = new DeploymentItems();
					items.envItem = envItem;
					items.startLineRange = deploymentItemsRange;
					items.range = Range.from(deploymentItemsStart, deploymentItemsEnd);
					items.items.addAll(deploymentItems);
					deployments.add(items);
				}
				streamItem.deployments.addAll(new ArrayList<>(deployments));
				deploymentItems.clear();
				deployments.clear();
				envItem = null;
				nameItem = null;
				descItem = null;
				deploymentItemsStart = null;
			} else {

				if (trim.length() > 0 && trim.charAt(0) == '#') {
					if (commentRange == null) {
						commentRange = Range.from(lineRange);
					} else {
						commentRange = commentRange.extend(lineRange);
					}
				} else if (commentRange != null) {
					commentRanges.add(commentRange);
					commentRange = null;
				}

				if (trim.length() > 2 && (trim.charAt(0) == '#' || trim.charAt(0) == '-')) {
					deploymentItemsEnd = lineRange.getEnd();
					DeploymentItem item = new DeploymentItem();
					item.range = lineRange;
					item.text = lineContent;

					boolean match = true;
					int contentStart = findContentStart(lineContent);
					if (contentStart > -1) {
						item.contentRange = Range.from(lineRange.getStart().getLine(), contentStart,
								lineRange.getEnd().getLine(), lineRange.getEnd().getCharacter());
					}
					if (contentStart > -1 && lineContent.startsWith(DataflowLanguages.TEXT_ENV_PREFIX, contentStart)) {
						envItem = item;
					} else if (contentStart > -1 && lineContent.startsWith(DataflowLanguages.TEXT_NAME_PREFIX, contentStart)) {
						nameItem = item;
					} else if (contentStart > -1 && lineContent.startsWith(DataflowLanguages.TEXT_DESC_PREFIX, contentStart)) {
						descItem = item;
					} else if (contentStart > -1 && lineContent.startsWith(DataflowLanguages.TEXT_PROP_PREFIX, contentStart)) {
						deploymentItems.add(item);
					} else {
						match = false;
					}
					if (match) {
						if (deploymentItemsStart == null) {
							deploymentItemsStart = lineRange.getStart();
							deploymentItemsRange = lineRange;
						}
					}
				} else {
					if (!deploymentItems.isEmpty()) {
						DeploymentItems items = new DeploymentItems();
						items.envItem = envItem;
						items.startLineRange = deploymentItemsRange;
						items.range = Range.from(deploymentItemsStart, deploymentItemsEnd);
						items.items.addAll(deploymentItems);
						deployments.add(items);
						envItem = null;
						nameItem = null;
						descItem = null;
						deploymentItemsStart = null;
					}
					deploymentItems.clear();
				}
			}
		}
		// no check case when with metadata but no dsl
		if (envItem != null || nameItem != null || descItem != null) {
			DefinitionItem definitionItem = new DefinitionItem();
			definitionItem.envItem = envItem;
			definitionItem.nameItem = nameItem;
			definitionItem.descItem = descItem;
			streamItem = new StreamItem();
			streamItem.definitionItem = definitionItem;
			streamItem.range = Range.from(deploymentItemsStart, lineRange.getEnd());
		}

		if (streamItem != null) {
			streamItem.getCommentRanges().addAll(commentRanges);
		}

		return streamItem;
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

	private DefinitionItem parseDefinition(DocumentText text, int line) {
		DefinitionItem definitionItem = new DefinitionItem();
		try {
			StreamParser parser = new StreamParser(text.toString());
			definitionItem.streamNode = parser.parse();
			String parsedName = definitionItem.streamNode.getStreamName();
			if (StringUtils.hasText(parsedName)) {
				int index = text.indexOf(parsedName);
				if (index > -1) {
					definitionItem.nameRange = Range.from(line, index, line, index + parsedName.length());
				}
			}
		} catch (ParseException e) {
			String message = e.getMessage();
			int position = e.getPosition();
			Range range = Range.from(line, position, line, position);
			DefaultReconcileProblem problem = new DefaultReconcileProblem(new ErrorProblemType(""), message, range);
			definitionItem.reconcileProblem = problem;
		}
		return definitionItem;
	}

	public static class DeploymentItems {
		private List<DeploymentItem> items = new ArrayList<>();
		private Range startLineRange;
		private Range range;
		private DeploymentItem envItem;

		public List<DeploymentItem> getItems() {
			return items;
		}

		public DeploymentItem getEnvItem() {
			return envItem;
		}

		public Range getStartLineRange() {
			return startLineRange;
		}

		public Range getRange() {
			return range;
		}
	}

	public static class DeploymentItem {
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
		private StreamNode streamNode;
		private Range range;
		private Range nameRange;
		private ReconcileProblem reconcileProblem;
		private DeploymentItem envItem;
		private DeploymentItem nameItem;
		private DeploymentItem descItem;

		public StreamNode getStreamNode() {
			return streamNode;
		}

		public Range getRange() {
			return range;
		}

		public Range getNameRange() {
			return nameRange;
		}

		public ReconcileProblem getReconcileProblem() {
			return reconcileProblem;
		}

		public DeploymentItem getEnvItem() {
			return envItem;
		}

		public DeploymentItem getNameItem() {
			return nameItem;
		}

		public DeploymentItem getDescItem() {
			return descItem;
		}
	}

	public static class StreamItem {
		private List<DeploymentItems> deployments = new ArrayList<>();
		private DefinitionItem definitionItem;
		private Range range;
		private List<Range> commentRanges = new ArrayList<>();

		public List<DeploymentItems> getDeployments() {
			return deployments;
		}

		public DefinitionItem getDefinitionItem() {
			return definitionItem;
		}

		public Range getRange() {
			return range;
		}

		public List<Range> getCommentRanges() {
			return commentRanges;
		}
	}
}
