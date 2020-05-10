/*
 * Copyright 2020 the original author or authors.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.dataflow.language.server.DataflowLanguages;
import org.springframework.cloud.dataflow.language.server.domain.TraceNotificationParams;
import org.springframework.dsl.jsonrpc.annotation.JsonRpcNotification;
import org.springframework.dsl.jsonrpc.annotation.JsonRpcRequestMapping;
import org.springframework.dsl.jsonrpc.annotation.JsonRpcRequestParams;
import org.springframework.dsl.jsonrpc.session.JsonRpcSession;

/**
 * Controller which listens vscode $/setTraceNotification messages.
 *
 * @author Janne Valkealahti
 *
 */
@JsonRpcRequestMapping(method = "$/")
public class TraceNotificationController {

	private final static Logger log = LoggerFactory.getLogger(TraceNotificationController.class);

	@JsonRpcRequestMapping(method = "setTraceNotification")
	@JsonRpcNotification
	public void traceNotification(@JsonRpcRequestParams TraceNotificationParams params,
			JsonRpcSession session) {
		log.debug("Client sending new traceNotification info, params {} and session id {}", params, session.getId());
		session.getAttributes().put(DataflowLanguages.CONTEXT_SESSION_TRACENOTIFICATION_ATTRIBUTE, params);
	}
}
