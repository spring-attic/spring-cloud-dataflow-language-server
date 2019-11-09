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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.resource.CompletionProposalsResource;
import org.springframework.dsl.domain.CompletionItem;
import org.springframework.dsl.domain.MarkupKind;
import org.springframework.dsl.domain.Position;
import org.springframework.dsl.domain.Range;
import org.springframework.dsl.service.Completioner;
import org.springframework.dsl.service.DslContext;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;

@Component
public class StreamLanguageCompletioner extends AbstractStreamLanguageService implements Completioner {

	private static final Logger log = LoggerFactory.getLogger(StreamLanguageCompletioner.class);

	@Override
	public Flux<CompletionItem> complete(DslContext context, Position position) {
		return Flux.defer(() -> {
			log.trace("Start of complete request");
			DataFlowOperations dataFlowOperations = resolveDataFlowOperations(context, position);
			if (dataFlowOperations == null) {
				return Flux.empty();
			}
			Range prefixRange = Range.from(position.getLine(), 0, position.getLine(), position.getCharacter());
			String prefix = context.getDocument().content(prefixRange).toString();
			log.trace("Start of complete request scdf");
			CompletionProposalsResource proposals = dataFlowOperations.completionOperations()
					.streamCompletions(prefix, 1);
			log.trace("End of complete request scdf");
			return Flux.fromIterable(proposals.getProposals())
				.map(proposal -> {
					return CompletionItem.completionItem()
						.label(resultLabel(prefix, proposal.getText()))
						.documentation()
							.kind(MarkupKind.plaintext)
							.value(proposal.getExplanation())
							.and()
						// need to have filter as it defaults to label and we changed it and it doesn't match newText
						.filterText(proposal.getText())
						.textEdit()
							.range(Range.from(position.getLine(), 0, position.getLine(), position.getCharacter()))
							.newText(proposal.getText())
							.and()
						.build();
				})
				.doOnComplete(() -> {
					log.trace("End of complete request");
				});
		});
	}

	private static String resultLabel(String left, String right) {
		int interestingPrefixStart = interestingPrefixStart(left);
		String leftnew = left.substring(0, interestingPrefixStart);
		String commonPrefix = commonPrefix(leftnew, right);
		return right.substring(commonPrefix.length());
	}

	private static int interestingPrefixStart(String left) {
		for (int i = left.length() - 1; i > 0; i--) {
			if (Character.isWhitespace(left.charAt(i))) {
				return i + 1;
			}
		}
		return 0;
	}

	private static String commonPrefix(String left, String right) {
		int min = Math.min(left.length(), right.length());
		for (int i = 0; i < min; i++) {
			if (left.charAt(i) != right.charAt(i)) {
				return left.substring(0, i);
			}
		}
		return left.substring(0, min);
	}
}
