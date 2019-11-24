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

 package org.springframework.cloud.dataflow.language.server.controller;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.dataflow.language.server.DataflowLanguages;
import org.springframework.cloud.dataflow.language.server.domain.DataflowEnvironmentParams;
import org.springframework.cloud.dataflow.language.server.domain.DataflowEnvironmentParams.Environment;
import org.springframework.cloud.dataflow.language.server.domain.DataflowStreamCreateParams;
import org.springframework.cloud.dataflow.language.server.domain.DataflowStreamDeployParams;
import org.springframework.cloud.dataflow.language.server.domain.DataflowStreamDestroyParams;
import org.springframework.cloud.dataflow.language.server.domain.DataflowStreamUndeployParams;
import org.springframework.cloud.dataflow.language.server.domain.DataflowTaskCreateParams;
import org.springframework.cloud.dataflow.language.server.domain.DataflowTaskDestroyParams;
import org.springframework.cloud.dataflow.language.server.domain.DataflowTaskLaunchParams;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.util.HttpClientConfigurer;
import org.springframework.dsl.jsonrpc.annotation.JsonRpcNotification;
import org.springframework.dsl.jsonrpc.annotation.JsonRpcRequestMapping;
import org.springframework.dsl.jsonrpc.annotation.JsonRpcRequestParams;
import org.springframework.dsl.jsonrpc.session.JsonRpcSession;
import org.springframework.dsl.lsp.client.LspClient;
import org.springframework.dsl.service.DslContext;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import reactor.core.publisher.Mono;

/**
 * Controller which takes ownership of all lsp protocol communication for a
 * {@code scdf} namespace. Functionality behind this controller is defined
 * together with a lsp client which for correct functionality need to be aware
 * of these rules working with Spring Cloud Data Flow specific extensions of a
 * lsp protocol.
 *
 * @author Janne Valkealahti
 *
 */
@JsonRpcRequestMapping(method = "scdf/")
public class DataflowJsonRpcController {

	private final static Logger log = LoggerFactory.getLogger(DataflowJsonRpcController.class);

	/**
	 * Blindly inject given params into a session so that other methods can use this
	 * info from a {@link JsonRpcSession} available from a {@link DslContext}.
	 *
	 * @param params  the dataflow environment params
	 * @param session th json rpc session
	 */
	@JsonRpcRequestMapping(method = "environment")
	@JsonRpcNotification
	public void environmentNotification(@JsonRpcRequestParams DataflowEnvironmentParams params,
			JsonRpcSession session) {
		log.debug("Client sending new environment info, params {} and session id {}", params, session.getId());
		session.getAttributes().put(DataflowLanguages.CONTEXT_SESSION_ENVIRONMENTS_ATTRIBUTE, params);
	}

	@JsonRpcRequestMapping(method = "createStream")
	@JsonRpcNotification
	public Mono<Void> createStream(@JsonRpcRequestParams DataflowStreamCreateParams params, JsonRpcSession session,
			LspClient lspClient) {
		return Mono.fromRunnable(() -> {
			log.debug("Client sending stream create request, params {}", params);
			DataFlowOperations operations = getDataFlowOperations(session, params.getServer());
			if (operations != null) {
				log.debug("Creating stream {}", params);
				operations.streamOperations().createStream(params.getName(), params.getDefinition(), params.getDescription(), false);
			} else {
				log.info("Unable to create stream");
			}
		}).then(lspClient.notification().method("scdf/createdStream").exchange());
	}

	@JsonRpcRequestMapping(method = "deployStream")
	@JsonRpcNotification
	public Mono<Void> deployStream(@JsonRpcRequestParams DataflowStreamDeployParams params, JsonRpcSession session,
			LspClient lspClient) {
		return Mono.fromRunnable(() -> {
			log.debug("Client sending stream deploy request, params {}", params);
			DataFlowOperations operations = getDataFlowOperations(session, params.getServer());
			if (operations != null) {
				log.debug("Deploying stream {}", params);
				operations.streamOperations().deploy(params.getName(), params.getProperties());
			} else {
				log.info("Unable to deploy stream");
			}
		}).then(lspClient.notification().method("scdf/deployedStream").exchange());
	}

	@JsonRpcRequestMapping(method = "undeployStream")
	@JsonRpcNotification
	public Mono<Void> undeployStream(@JsonRpcRequestParams DataflowStreamUndeployParams params, JsonRpcSession session,
			LspClient lspClient) {
		return Mono.fromRunnable(() -> {
			log.debug("Client sending stream create request, params {}", params);
			DataFlowOperations operations = getDataFlowOperations(session, params.getServer());
			if (operations != null) {
				log.debug("Undeploying stream {}", params);
				operations.streamOperations().undeploy(params.getName());
			} else {
				log.info("Unable to undeploy stream");
			}
		}).then(lspClient.notification().method("scdf/undeployedStream").exchange());
	}

	@JsonRpcRequestMapping(method = "destroyStream")
	@JsonRpcNotification
	public Mono<Void> destroyStream(@JsonRpcRequestParams DataflowStreamDestroyParams params, JsonRpcSession session,
			LspClient lspClient) {
		return Mono.fromRunnable(() -> {
			log.debug("Client sending stream destroy request, params {}", params);
			DataFlowOperations operations = getDataFlowOperations(session, params.getServer());
			if (operations != null) {
				log.debug("Destroying stream {}", params);
				operations.streamOperations().destroy(params.getName());
			} else {
				log.info("Unable to destroy stream");
			}
		}).then(lspClient.notification().method("scdf/destroyedStream").exchange());
	}

	@JsonRpcRequestMapping(method = "createTask")
	@JsonRpcNotification
	public Mono<Void> createTask(@JsonRpcRequestParams DataflowTaskCreateParams params, JsonRpcSession session,
			LspClient lspClient) {
		return Mono.fromRunnable(() -> {
			log.debug("Client sending task create request, params {}", params);
			DataFlowOperations operations = getDataFlowOperations(session, params.getServer());
			if (operations != null) {
				log.debug("Creating task {}", params);
				operations.taskOperations().create(params.getName(), params.getDefinition(), params.getDescription());
			} else {
				log.info("Unable to create task");
			}
		}).then(lspClient.notification().method("scdf/createdTask").exchange());
	}

	@JsonRpcRequestMapping(method = "launchTask")
	@JsonRpcNotification
	public Mono<Void> launchTask(@JsonRpcRequestParams DataflowTaskLaunchParams params, JsonRpcSession session,
			LspClient lspClient) {
		return Mono.fromRunnable(() -> {
			log.debug("Client sending task launch request, params {}", params);
			DataFlowOperations operations = getDataFlowOperations(session, params.getServer());
			if (operations != null) {
				log.debug("Creating task {}", params);
				operations.taskOperations().launch(params.getName(), params.getProperties(), params.getArguments(), null);
			} else {
				log.info("Unable to launch task");
			}
		}).then(lspClient.notification().method("scdf/launchedTask").exchange());
	}

	@JsonRpcRequestMapping(method = "destroyTask")
	@JsonRpcNotification
	public Mono<Void> destroyTask(@JsonRpcRequestParams DataflowTaskDestroyParams params, JsonRpcSession session,
			LspClient lspClient) {
		return Mono.fromRunnable(() -> {
			log.debug("Client sending task destroy request, params {}", params);
			DataFlowOperations operations = getDataFlowOperations(session, params.getServer());
			if (operations != null) {
				log.debug("Destroying task {}", params);
				operations.taskOperations().destroy(params.getName());
			} else {
				log.info("Unable to destroy task");
			}
		}).then(lspClient.notification().method("scdf/destroyedTask").exchange());
	}

	protected DataFlowOperations getDataFlowOperations(JsonRpcSession session, String server) {
		org.springframework.cloud.dataflow.language.server.domain.DataflowEnvironmentParams params = session
				.getAttribute(DataflowLanguages.CONTEXT_SESSION_ENVIRONMENTS_ATTRIBUTE);
		List<Environment> environments = params.getEnvironments();
		Environment environment = environments.stream()
			.filter(e -> ObjectUtils.nullSafeEquals(e.getName(), server))
			.findAny()
			.orElse(null);
		return buildDataFlowTemplate(environment, params.getTrustssl());
	}

	private DataFlowTemplate buildDataFlowTemplate(Environment environment, Boolean trustssl) {
		URI uri = URI.create(environment.getUrl());
		String username = environment.getCredentials().getUsername();
		String password = environment.getCredentials().getPassword();
		if ((StringUtils.hasText(username) && StringUtils.hasText(password)) || (trustssl != null && trustssl)) {
			RestTemplate restTemplate = new RestTemplate();
			HttpClientConfigurer httpClientConfigurer = HttpClientConfigurer.create(uri);
			if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
				httpClientConfigurer.basicAuthCredentials(username, password);
			}
			if (trustssl != null && trustssl) {
				httpClientConfigurer.skipTlsCertificateVerification(true);
			}
			restTemplate.setRequestFactory(httpClientConfigurer.buildClientHttpRequestFactory());
			return new DataFlowTemplate(uri, restTemplate);
		} else {
			return new DataFlowTemplate(uri);
		}
	}
}
