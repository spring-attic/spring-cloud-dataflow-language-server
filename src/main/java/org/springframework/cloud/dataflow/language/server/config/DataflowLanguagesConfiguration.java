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
package org.springframework.cloud.dataflow.language.server.config;

import org.springframework.cloud.dataflow.language.server.app.AppLanguageLenser;
import org.springframework.cloud.dataflow.language.server.app.AppLanguageLinter;
import org.springframework.cloud.dataflow.language.server.app.AppLanguageSymbolizer;
import org.springframework.cloud.dataflow.language.server.controller.DataflowJsonRpcController;
import org.springframework.cloud.dataflow.language.server.stream.StreamLanguageCompletioner;
import org.springframework.cloud.dataflow.language.server.stream.StreamLanguageFolderer;
import org.springframework.cloud.dataflow.language.server.stream.StreamLanguageHoverer;
import org.springframework.cloud.dataflow.language.server.stream.StreamLanguageLenser;
import org.springframework.cloud.dataflow.language.server.stream.StreamLanguageLinter;
import org.springframework.cloud.dataflow.language.server.stream.StreamLanguageSymbolizer;
import org.springframework.cloud.dataflow.language.server.stream.StreamRuntimeLanguageLenser;
import org.springframework.cloud.dataflow.language.server.task.TaskLanguageLenser;
import org.springframework.cloud.dataflow.language.server.task.TaskLanguageLinter;
import org.springframework.cloud.dataflow.language.server.task.TaskLanguageSymbolizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dsl.lsp.server.support.JvmLspExiter;
import org.springframework.dsl.lsp.server.support.LspExiter;
import org.springframework.dsl.service.Completioner;
import org.springframework.dsl.service.Folderer;
import org.springframework.dsl.service.Hoverer;
import org.springframework.dsl.service.Lenser;
import org.springframework.dsl.service.reconcile.Linter;
import org.springframework.dsl.service.symbol.Symbolizer;

@Configuration
public class DataflowLanguagesConfiguration {

    @Bean
	public LspExiter lspExiter() {
		return new JvmLspExiter();
	}

    @Bean
    public DataflowJsonRpcController dataflowJsonRpcController() {
        return new DataflowJsonRpcController();
    }

    @Bean
    public Linter dataflowStreamLanguageLinter() {
        return new StreamLanguageLinter();
    }

    @Bean
    public Lenser dataflowStreamLanguageLenser() {
        return new StreamLanguageLenser();
    }

    @Bean
    public Hoverer dataflowStreamLanguageHoverer() {
        return new StreamLanguageHoverer();
    }

    @Bean
    public Symbolizer dataflowStreamLanguageSymbolizer() {
        return new StreamLanguageSymbolizer();
    }

    @Bean
    public Completioner dataflowStreamLanguageCompletioner() {
        return new StreamLanguageCompletioner();
    }

    @Bean
    public Folderer dataflowStreamLanguageFolderer() {
        return new StreamLanguageFolderer();
    }

    @Bean
    public Lenser dataflowStreamRuntimeLanguageLenser() {
        return new StreamRuntimeLanguageLenser();
    }

    @Bean
    public Linter dataflowTaskLanguageLinter() {
        return new TaskLanguageLinter();
    }

    @Bean
    public Lenser dataflowTaskLanguageLenser() {
        return new TaskLanguageLenser();
    }

    @Bean
    public Symbolizer dataflowTaskLanguageSymbolizer() {
        return new TaskLanguageSymbolizer();
    }

    @Bean
    public Lenser appLanguageLenser() {
        return new AppLanguageLenser();
    }

    @Bean
    public Linter appLanguageLinter() {
        return new AppLanguageLinter();
    }

    @Bean
    public Symbolizer appLanguageSymbolizer() {
        return new AppLanguageSymbolizer();
    }
}
