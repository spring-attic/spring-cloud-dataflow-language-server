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
import org.springframework.cloud.dataflow.language.server.stream.DataflowStreamLanguageCompletioner;
import org.springframework.cloud.dataflow.language.server.stream.DataflowStreamLanguageHoverer;
import org.springframework.cloud.dataflow.language.server.stream.DataflowStreamLanguageLenser;
import org.springframework.cloud.dataflow.language.server.stream.DataflowStreamLanguageLinter;
import org.springframework.cloud.dataflow.language.server.stream.DataflowStreamLanguageSymbolizer;
import org.springframework.cloud.dataflow.language.server.stream.DataflowStreamRuntimeLanguageLenser;
import org.springframework.cloud.dataflow.language.server.task.DataflowTaskLanguageLenser;
import org.springframework.cloud.dataflow.language.server.task.DataflowTaskLanguageLinter;
import org.springframework.cloud.dataflow.language.server.task.DataflowTaskLanguageSymbolizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dsl.lsp.server.support.JvmLspExiter;
import org.springframework.dsl.lsp.server.support.LspExiter;
import org.springframework.dsl.service.Completioner;
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
        return new DataflowStreamLanguageLinter();
    }

    @Bean
    public Lenser dataflowStreamLanguageLenser() {
        return new DataflowStreamLanguageLenser();
    }

    @Bean
    public Hoverer dataflowStreamLanguageHoverer() {
        return new DataflowStreamLanguageHoverer();
    }

    @Bean
    public Symbolizer dataflowStreamLanguageSymbolizer() {
        return new DataflowStreamLanguageSymbolizer();
    }

    @Bean
    public Completioner dataflowStreamLanguageCompletioner() {
        return new DataflowStreamLanguageCompletioner();
    }

    @Bean
    public Lenser dataflowStreamRuntimeLanguageLenser() {
        return new DataflowStreamRuntimeLanguageLenser();
    }

    @Bean
    public Linter dataflowTaskLanguageLinter() {
        return new DataflowTaskLanguageLinter();
    }

    @Bean
    public Lenser dataflowTaskLanguageLenser() {
        return new DataflowTaskLanguageLenser();
    }

    @Bean
    public Symbolizer dataflowTaskLanguageSymbolizer() {
        return new DataflowTaskLanguageSymbolizer();
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
