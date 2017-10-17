/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.jenkinsci.plugins.kieprbuildshelper;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class BranchMappingFactoryTest {

    @Test
    public void createDefaultBranchMappings() {
        Set<BranchMapping> branchMappings = BranchMappingFactory.createFrom(RepositoryLists.KIE_BOOTSTRAP_REPO, GitBranch.MASTER);
        Assertions.assertThat(branchMappings).isNotEmpty();
    }

    @Test
    public void createCustomBranchMappings() {
        String yaml = "master:\n" +
                "  - errai/errai: master\n" +
                "  - kiegroup/kie-soup: master\n" +
                "  - AppFormer/uberfire: master\n" +
                "  - dashbuilder/dashbuilder: master\n" +
                "\n" +
                "7.4.x:\n" +
                "  - errai/errai: 4.0.x\n" +
                "  - AppFormer/uberfire: 1.4.x\n" +
                "  - dashbuilder/dashbuilder: 1.0.x\n" +
                "\n" +
                "7.3.x:\n" +
                "  - errai/errai: 4.0.x\n" +
                "  - AppFormer/uberfire: 1.3.x\n" +
                "  - dashbuilder/dashbuilder: 0.9.x";
        Set<BranchMapping> branchMappings = BranchMappingFactory.createFrom(yaml);
        Assertions.assertThat(branchMappings).hasSize(3);

        List<Tuple<GitHubRepository, GitBranch>> upstreamRepos74x = Arrays.asList(
                Tuple.of(new GitHubRepository("errai", "errai"), new GitBranch("4.0.x")),
                Tuple.of(new GitHubRepository("AppFormer", "uberfire"), new GitBranch("1.4.x")),
                Tuple.of(new GitHubRepository("dashbuilder", "dashbuilder"), new GitBranch("1.0.x"))
        );
        BranchMapping expected74xMapping = new BranchMapping(new GitBranch("7.4.x"), upstreamRepos74x);
        Assertions.assertThat(branchMappings).contains(expected74xMapping);
    }
}
