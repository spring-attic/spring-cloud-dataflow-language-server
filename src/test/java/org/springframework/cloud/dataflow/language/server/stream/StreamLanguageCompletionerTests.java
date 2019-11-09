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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.dataflow.language.server.DataflowLanguages;
import org.springframework.cloud.dataflow.language.server.support.DataflowCacheService;
import org.springframework.cloud.dataflow.rest.client.CompletionOperations;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.resource.CompletionProposalsResource;
import org.springframework.cloud.dataflow.rest.resource.CompletionProposalsResource.Proposal;
import org.springframework.dsl.document.Document;
import org.springframework.dsl.document.TextDocument;
import org.springframework.dsl.domain.CompletionItem;
import org.springframework.dsl.domain.Position;
import org.springframework.dsl.service.DslContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class StreamLanguageCompletionerTests {

    @MockBean
    private DataFlowOperations dataFlowOperations;

    @MockBean
    private CompletionOperations completionOperations;

    @MockBean
    private CompletionProposalsResource proposalsResource;

    @Test
    public void testEmpty() {
        Proposal proposal = new Proposal("completion1", "explanation");
        Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0, "");
        Mockito.when(dataFlowOperations.completionOperations()).thenReturn(completionOperations);
        Mockito.when(completionOperations.streamCompletions(any(), anyInt())).thenReturn(proposalsResource);
        Mockito.when(proposalsResource.getProposals()).thenReturn(Arrays.asList(proposal));
        MockStreamLanguageCompletioner completioner = mockCompletioner();
        completioner.setDataflowCacheService(new DataflowCacheService());

        List<CompletionItem> completes = completioner
                .complete(DslContext.builder().document(document).build(), Position.zero()).toStream()
                .collect(Collectors.toList());
        assertThat(completes).hasSize(1);
        assertThat(completes.get(0).getTextEdit().getNewText()).isEqualTo("completion1");
    }

    @Test
    public void testCompletionWithinIncompleteApp() {
        Proposal proposal1 = new Proposal("ticktock = time", "");
        Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0, "ticktock = ti");
        Mockito.when(dataFlowOperations.completionOperations()).thenReturn(completionOperations);
        Mockito.when(completionOperations.streamCompletions(any(), anyInt())).thenReturn(proposalsResource);
        Mockito.when(proposalsResource.getProposals())
                .thenReturn(Arrays.asList(proposal1));
        MockStreamLanguageCompletioner completioner = mockCompletioner();
        completioner.setDataflowCacheService(new DataflowCacheService());

        List<CompletionItem> completes = completioner
                .complete(DslContext.builder().document(document).build(), Position.from(0, 13)).toStream()
                .collect(Collectors.toList());
        assertThat(completes).hasSize(1);
        assertThat(completes.get(0).getLabel()).isEqualTo("time");
    }

    @Test
    public void testTickTockCompletionFromBeforePipe() {
        Proposal proposal1 = new Proposal("ticktock = time --time-unit=", "The TimeUnit to apply to delay values.");
        Proposal proposal2 = new Proposal("ticktock = time --fixed-delay=", "Fixed delay for periodic triggers.");
        Proposal proposal3 = new Proposal("ticktock = time --cron=", "Cron expression value for the Cron Trigger.");
        Proposal proposal4 = new Proposal("ticktock = time --initial-delay=", "Initial delay for periodic triggers.");
        Proposal proposal5 = new Proposal("ticktock = time --max-messages=", "Maximum messages per poll, -1 means infinity.");
        Proposal proposal6 = new Proposal("ticktock = time --date-format=", "Format for the date value.");
        Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0, "ticktock = time | log");
        Mockito.when(dataFlowOperations.completionOperations()).thenReturn(completionOperations);
        Mockito.when(completionOperations.streamCompletions(any(), anyInt())).thenReturn(proposalsResource);
        Mockito.when(proposalsResource.getProposals())
                .thenReturn(Arrays.asList(proposal1, proposal2, proposal3, proposal4, proposal5, proposal6));
        MockStreamLanguageCompletioner completioner = mockCompletioner();
        completioner.setDataflowCacheService(new DataflowCacheService());

        List<CompletionItem> completes = completioner
                .complete(DslContext.builder().document(document).build(), Position.from(0, 16)).toStream()
                .collect(Collectors.toList());
        assertThat(completes).hasSize(6);

        assertThat(completes.get(0).getLabel()).isEqualTo("--time-unit=");
        assertThat(completes.get(0).getFilterText()).isEqualTo("ticktock = time --time-unit=");
        assertThat(completes.get(0).getTextEdit().getNewText()).isEqualTo("ticktock = time --time-unit=");

        assertThat(completes.get(1).getLabel()).isEqualTo("--fixed-delay=");
        assertThat(completes.get(1).getFilterText()).isEqualTo("ticktock = time --fixed-delay=");
        assertThat(completes.get(1).getTextEdit().getNewText()).isEqualTo("ticktock = time --fixed-delay=");

        assertThat(completes.get(2).getLabel()).isEqualTo("--cron=");
        assertThat(completes.get(2).getFilterText()).isEqualTo("ticktock = time --cron=");
        assertThat(completes.get(2).getTextEdit().getNewText()).isEqualTo("ticktock = time --cron=");

        assertThat(completes.get(3).getLabel()).isEqualTo("--initial-delay=");
        assertThat(completes.get(3).getFilterText()).isEqualTo("ticktock = time --initial-delay=");
        assertThat(completes.get(3).getTextEdit().getNewText()).isEqualTo("ticktock = time --initial-delay=");

        assertThat(completes.get(4).getLabel()).isEqualTo("--max-messages=");
        assertThat(completes.get(4).getFilterText()).isEqualTo("ticktock = time --max-messages=");
        assertThat(completes.get(4).getTextEdit().getNewText()).isEqualTo("ticktock = time --max-messages=");

        assertThat(completes.get(5).getLabel()).isEqualTo("--date-format=");
        assertThat(completes.get(5).getFilterText()).isEqualTo("ticktock = time --date-format=");
        assertThat(completes.get(5).getTextEdit().getNewText()).isEqualTo("ticktock = time --date-format=");
    }

    @Test
    public void testCorrectEnvPickedFromMetadata() {
        Document document = new TextDocument("fakeuri", DataflowLanguages.LANGUAGE_STREAM, 0,
                AbstractStreamLanguageServiceTests.DSL_STREAMS_JUST_METADATA);
        Mockito.when(dataFlowOperations.completionOperations()).thenReturn(completionOperations);
        Mockito.when(completionOperations.streamCompletions(any(), anyInt())).thenReturn(proposalsResource);
        Mockito.when(proposalsResource.getProposals()).thenReturn(Collections.emptyList());
        MockStreamLanguageCompletioner completioner = mockCompletioner();
        completioner.setDataflowCacheService(new DataflowCacheService());

        List<CompletionItem> completes = completioner
                .complete(DslContext.builder().document(document).build(), Position.from(3, 0)).toStream()
                .collect(Collectors.toList());
        assertThat(completes).hasSize(0);
        assertThat(completioner.nameToCheck).isEqualTo("env1");
    }

    private MockStreamLanguageCompletioner mockCompletioner() {
        return new MockStreamLanguageCompletioner();
    }

    private class MockStreamLanguageCompletioner extends StreamLanguageCompletioner {

        String nameToCheck;

        @Override
        protected DataFlowOperations resolveDataFlowOperations(DslContext context, Position position) {
            nameToCheck = resolveDefinedEnvironmentName(context, position);
            return dataFlowOperations;
        }
    }
}
