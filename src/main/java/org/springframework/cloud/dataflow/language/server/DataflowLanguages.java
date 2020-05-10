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
package org.springframework.cloud.dataflow.language.server;

import org.springframework.dsl.document.DocumentText;
import org.springframework.dsl.model.LanguageId;

/**
 * Various contansts for dataflow languages integration.
 *
 * @author Janne Valkealahti
 *
 */
public class DataflowLanguages {

	// stream dsl
	public final static String LANGUAGE_STREAM_ID = "scdfs";
	public final static String LANGUAGE_STREAM_DESC = "Spring Cloud Data Flow Stream Language";
	public final static LanguageId LANGUAGE_STREAM = LanguageId.languageId(LANGUAGE_STREAM_ID, LANGUAGE_STREAM_DESC);

	// stream runtime dsl
	public final static String LANGUAGE_STREAM_RUNTIME_ID = "scdfsr";
	public final static String LANGUAGE_STREAM_RUNTIME_DESC = "SCDF Stream Runtime";
	public final static LanguageId LANGUAGE_STREAM_RUNTIME = LanguageId.languageId(LANGUAGE_STREAM_RUNTIME_ID,
			LANGUAGE_STREAM_RUNTIME_DESC);

	// task dsl
	public final static String LANGUAGE_TASK_ID = "scdft";
	public final static String LANGUAGE_TASK_DESC = "Spring Cloud Data Flow Task Language";
	public final static LanguageId LANGUAGE_TASK = LanguageId.languageId(LANGUAGE_TASK_ID, LANGUAGE_TASK_DESC);

	// application import
	public final static String LANGUAGE_APP_ID = "scdfa";
	public final static String LANGUAGE_APP_DESC = "Spring Cloud Data Flow App Import Language";
	public final static LanguageId LANGUAGE_APP = LanguageId.languageId(LANGUAGE_APP_ID, LANGUAGE_APP_DESC);

	// commands and titles
	public final static String COMMAND_STREAM_DEPLOY = "vscode-spring-cloud-dataflow.streams.deploy";
	public final static String COMMAND_STREAM_DEPLOY_TITLE = "Deploy Stream";
	public final static String COMMAND_STREAM_UNDEPLOY = "vscode-spring-cloud-dataflow.streams.undeploy";
	public final static String COMMAND_STREAM_UNDEPLOY_TITLE = "Undeploy Stream";
	public final static String COMMAND_STREAM_CREATE = "vscode-spring-cloud-dataflow.streams.create";
	public final static String COMMAND_STREAM_CREATE_TITLE = "Create Stream";
	public final static String COMMAND_STREAM_DESTROY = "vscode-spring-cloud-dataflow.streams.destroy";
	public final static String COMMAND_STREAM_DESTROY_TITLE = "Destroy Stream";
	public final static String COMMAND_STREAM_DEBUG_ATTACH = "vscode-spring-cloud-dataflow.streams.debugattach";
	public final static String COMMAND_STREAM_DEBUG_ATTACH_TITLE = "Debug Attach Stream";
	public final static String COMMAND_STREAM_DEBUG_LAUNCH = "vscode-spring-cloud-dataflow.streams.debuglaunch";
	public final static String COMMAND_STREAM_DEBUG_LAUNCH_TITLE = "Debug Launch Stream";
	public final static String COMMAND_TASK_CREATE = "vscode-spring-cloud-dataflow.tasks.create";
	public final static String COMMAND_TASK_CREATE_TITLE = "Create Task";
	public final static String COMMAND_TASK_LAUNCH = "vscode-spring-cloud-dataflow.tasks.launch";
	public final static String COMMAND_TASK_LAUNCH_TITLE = "Launch Task";
	public final static String COMMAND_TASK_DESTROY = "vscode-spring-cloud-dataflow.tasks.destroy";
	public final static String COMMAND_TASK_DESTROY_TITLE = "Destroy Task";
	public final static String COMMAND_APP_REGISTER = "vscode-spring-cloud-dataflow.apps.register";
	public final static String COMMAND_APP_REGISTER_TITLE = "Register Application";
	public final static String COMMAND_APP_UNREGISTER = "vscode-spring-cloud-dataflow.apps.unregister";
	public final static String COMMAND_APP_UNREGISTER_TITLE = "Unregister Application";

	// dsl context attribute names
	public final static String CONTEXT_SESSION_ENVIRONMENTS_ATTRIBUTE = "jsonRpcSessionEnvironments";

	// dsl context attribute name for $/setTraceNotification
	public final static String CONTEXT_SESSION_TRACENOTIFICATION_ATTRIBUTE = "vscodeTraceNotification";

	// Text constants
	public static final DocumentText TEXT_ENV_PREFIX = DocumentText.from("@env");
	public static final DocumentText TEXT_NAME_PREFIX = DocumentText.from("@name");
	public static final DocumentText TEXT_DESC_PREFIX = DocumentText.from("@desc");
	public static final DocumentText TEXT_PROP_PREFIX = DocumentText.from("@prop");
	public static final DocumentText TEXT_ARG_PREFIX = DocumentText.from("@arg");
}
