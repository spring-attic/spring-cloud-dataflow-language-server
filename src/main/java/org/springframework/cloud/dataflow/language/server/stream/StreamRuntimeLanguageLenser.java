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

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.cloud.dataflow.language.server.DataflowLanguages;
import org.springframework.dsl.DslException;
import org.springframework.dsl.domain.CodeLens;
import org.springframework.dsl.service.DslContext;
import org.springframework.dsl.service.Lenser;

import reactor.core.publisher.Flux;

public class StreamRuntimeLanguageLenser extends AbstractStreamLanguageService implements Lenser {

    public StreamRuntimeLanguageLenser() {
        super(DataflowLanguages.LANGUAGE_STREAM_RUNTIME);
    }

	@Override
	public Flux<CodeLens> lense(DslContext context) {
		return parse(context.getDocument())
				.flatMap(item -> Flux.fromArray(codeLensWithStream(item, UriPathInfo.from(context.getDocument().uri()))
						.toArray(new CodeLens[0])));
	}

	private List<CodeLens> codeLensWithStream(StreamItem item, UriPathInfo uriPathInfo) {
		String streamName = uriPathInfo.streamName;
		String streamEnvironment = uriPathInfo.streamEnvironment;

		if (item.getDefinitionItem().getStreamNode() == null) {
			// no stream yet, no lenses
			return Collections.emptyList();
		}

		return Arrays.asList(
			CodeLens.codeLens()
				.range(item.getDefinitionItem().getRange())
				.command()
					.command(DataflowLanguages.COMMAND_STREAM_DEPLOY)
					.title(DataflowLanguages.COMMAND_STREAM_DEPLOY_TITLE)
					.argument(streamName)
					.argument(streamEnvironment)
					.and()
				.build(),
			CodeLens.codeLens()
				.range(item.getDefinitionItem().getRange())
				.command()
					.command(DataflowLanguages.COMMAND_STREAM_UNDEPLOY)
					.title(DataflowLanguages.COMMAND_STREAM_UNDEPLOY_TITLE)
					.argument(streamName)
					.argument(streamEnvironment)
					.and()
				.build()
		);
	}

	private static class UriPathInfo {
		String streamName;
		String streamEnvironment;

		UriPathInfo(String streamName, String streamEnvironment) {
			this.streamName = streamName;
			this.streamEnvironment = streamEnvironment;
		}

		static UriPathInfo from(String uri) {
			try {
				// TODO: feels nasty, make pretty
				URI u = URI.create(uri);
				String host = u.getHost();
				String path = u.getPath();
				String[] split = path.split("/");
				return new UriPathInfo(split[2].split("\\.")[0], host);
			} catch (Exception e) {
				throw new DslException("Unable to parse document uri", e);
			}
		}
	}
}
