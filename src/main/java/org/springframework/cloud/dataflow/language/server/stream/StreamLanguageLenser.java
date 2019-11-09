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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cloud.dataflow.core.dsl.StreamNode;
import org.springframework.cloud.dataflow.language.server.DataflowLanguages;
import org.springframework.cloud.dataflow.language.server.domain.DataflowStreamCreateParams;
import org.springframework.cloud.dataflow.language.server.domain.DataflowStreamDeployParams;
import org.springframework.cloud.dataflow.language.server.domain.DataflowStreamDestroyParams;
import org.springframework.cloud.dataflow.language.server.domain.DataflowStreamUndeployParams;
import org.springframework.dsl.document.DocumentText;
import org.springframework.dsl.domain.CodeLens;
import org.springframework.dsl.domain.Range;
import org.springframework.dsl.service.DslContext;
import org.springframework.dsl.service.Lenser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Flux;

@Component
public class StreamLanguageLenser extends AbstractStreamLanguageService implements Lenser {

	@Override
	public Flux<CodeLens> lense(DslContext context) {
		return parse(context.getDocument())
			.flatMap(item -> Flux.fromIterable(codeLensWithProperties(item))
				.concatWithValues(codeLensWithStream(item).toArray(new CodeLens[0])));
	}

	private List<CodeLens> codeLensWithProperties(StreamItem item) {
		return item.getDeployments().stream()
			.map(deployment -> {
				return CodeLens.codeLens()
					.range(deployment.getStartLineRange())
					.command()
						.command(DataflowLanguages.COMMAND_STREAM_DEPLOY)
						.title(DataflowLanguages.COMMAND_STREAM_DEPLOY_TITLE)
						.argument(DataflowStreamDeployParams.from(getStreamName(item),
							getStreamEnvironment(deployment, item),
							getDeploymentProperties(deployment.getItems())))
						.and()
					.build();
			})
			.collect(Collectors.toList());
	}

	private List<CodeLens> codeLensWithStream(StreamItem item) {
		String streamName = getStreamName(item);
		String streamEnvironment = getStreamEnvironment(item);
		String streamDescription = getStreamDescription(item);

		if (item.getDefinitionItem().getStreamNode() == null) {
			// no stream yet, no lenses
			return Collections.emptyList();
		}

		return Arrays.asList(
			CodeLens.codeLens()
				.range(item.getDefinitionItem().getRange())
				.command()
					.command(DataflowLanguages.COMMAND_STREAM_CREATE)
					.title(DataflowLanguages.COMMAND_STREAM_CREATE_TITLE)
					.argument(DataflowStreamCreateParams.from(streamName, streamEnvironment, streamDescription,
						getDefinition(item.getDefinitionItem().getStreamNode())))
					.and()
				.build(),
			CodeLens.codeLens()
				.range(item.getDefinitionItem().getRange())
				.command()
					.command(DataflowLanguages.COMMAND_STREAM_DESTROY)
					.title(DataflowLanguages.COMMAND_STREAM_DESTROY_TITLE)
					.argument(DataflowStreamDestroyParams.from(streamName, streamEnvironment))
					.and()
				.build(),
			CodeLens.codeLens()
				.range(item.getDefinitionItem().getRange())
				.command()
					.command(DataflowLanguages.COMMAND_STREAM_DEPLOY)
					.title(DataflowLanguages.COMMAND_STREAM_DEPLOY_TITLE)
					.argument(DataflowStreamDeployParams.from(streamName, streamEnvironment))
					.and()
				.build(),
			CodeLens.codeLens()
				.range(item.getDefinitionItem().getRange())
				.command()
					.command(DataflowLanguages.COMMAND_STREAM_UNDEPLOY)
					.title(DataflowLanguages.COMMAND_STREAM_UNDEPLOY_TITLE)
					.argument(DataflowStreamUndeployParams.from(streamName, streamEnvironment))
					.and()
				.build()
		);
	}

	private String getDefinition(StreamNode streamNode) {
		return streamNode.getStreamText().substring(streamNode.getStartPos());
	}

	private String getStreamName(StreamItem item) {
		DefinitionItem definitionItem = item.getDefinitionItem();
		String streamName = definitionItem.getStreamNode() != null ? definitionItem.getStreamNode().getName() : null;
		if (!StringUtils.hasText(streamName)) {
			DeploymentItem nameItem = definitionItem.getNameItem();
			if (nameItem != null) {
				Range contentRange = nameItem.getContentRange();
				streamName = nameItem.getText()
						.substring(contentRange.getStart().getCharacter() + 5, nameItem.getText().length()).trim()
						.toString();
			}
		}
		return streamName;
	}

	private String getStreamDescription(StreamItem item) {
		String streamDescription = null;
		DeploymentItem descItem = item.getDefinitionItem().getDescItem();
		if (descItem != null) {
			Range contentRange = descItem.getContentRange();
			streamDescription = descItem.getText()
					.substring(contentRange.getStart().getCharacter() + 5, descItem.getText().length()).trim()
					.toString();
		}
		return streamDescription;
	}

	private String getStreamEnvironment(StreamItem item) {
		String streamEnvironment = null;
		DeploymentItem envItem = item.getDefinitionItem().getEnvItem();
		if (envItem != null) {
			Range contentRange = envItem.getContentRange();
			streamEnvironment = envItem.getText()
					.substring(contentRange.getStart().getCharacter() + 4, envItem.getText().length()).trim()
					.toString();
		}
		return streamEnvironment;
	}

	private String getStreamEnvironment(DeploymentItems items, StreamItem item) {
		String streamEnvironment = null;
		DeploymentItem envItem = items.getEnvItem();
		if (envItem != null) {
			Range contentRange = envItem.getContentRange();
			streamEnvironment = envItem.getText()
					.substring(contentRange.getStart().getCharacter() + 4, envItem.getText().length()).trim()
					.toString();
		}
		if (!StringUtils.hasText(streamEnvironment)) {
			streamEnvironment = getStreamEnvironment(item);
		}
		return streamEnvironment;
	}

	private Map<String, String> getDeploymentProperties(List<DeploymentItem> items) {
		HashMap<String, String> properties = new HashMap<String, String>();
		items.stream().forEach(item -> {
			DocumentText[] split = item.getText().splitFirst('=');
			if (split.length == 2) {
				int firstAlphaNumeric = firstLetterOrDigit(split[0]);
				if (firstAlphaNumeric > -1) {
					int lastIndexOf = split[0].indexOf(propPrefix);
					if (lastIndexOf > -1) {
						properties.put(split[0].subSequence(lastIndexOf + propPrefix.length(), split[0].length()).toString().trim(),
						split[1].toString().trim());
					}
				}
			}
		});
		return properties;
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
