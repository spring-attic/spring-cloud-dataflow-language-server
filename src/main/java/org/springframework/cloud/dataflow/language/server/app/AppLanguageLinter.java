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
package org.springframework.cloud.dataflow.language.server.app;

import org.springframework.dsl.service.DslContext;
import org.springframework.dsl.service.reconcile.Linter;
import org.springframework.dsl.service.reconcile.ReconcileProblem;

import reactor.core.publisher.Flux;

/**
 * {@link Linter} implementation for {@link DataflowLanguages#LANGUAGE_APP}.
 *
 * @author Janne Valkealahti
 *
 */
public class AppLanguageLinter extends AbstractAppLanguageService implements Linter {

    @Override
    public Flux<ReconcileProblem> lint(DslContext context) {
        return Flux.empty();
    }
}
