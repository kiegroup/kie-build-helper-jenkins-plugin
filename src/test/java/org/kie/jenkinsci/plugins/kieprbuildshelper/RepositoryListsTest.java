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

import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class RepositoryListsTest {

    private static final GitHubRepository DROOLS_REPO = new GitHubRepository("kiegroup","drools");
    private static final GitBranch BRANCH_72X = new GitBranch("7.2.x");

    @Test
    public void fetchRepositoryListForMaster() {
        List<Tuple<GitHubRepository, GitBranch>> repos = RepositoryLists.create(Collections.emptyList(),
                                                                                Tuple.of(RepositoryLists.KIE_BOOTSTRAP_REPO, GitBranch.MASTER),
                                                                                GitBranch.MASTER);
        // don't do too specific assertions as the repo list may change at any time as the test would then start failing
        // check just that the list is not empty as there should always some repos
        Assertions.assertThat(repos).isNotEmpty();
    }

    @Test
    public void fetchRepositoryListFor72x() {
        List<Tuple<GitHubRepository, GitBranch>> repos = RepositoryLists.create(Collections.emptyList(),
                                                                                Tuple.of(RepositoryLists.KIE_BOOTSTRAP_REPO, BRANCH_72X),
                                                                                new GitBranch("7.2.x"));
        // repo list for 7.2.x should be stable enough to make assertions on those
        Assertions.assertThat(repos).containsExactly(
                Tuple.of(new GitHubRepository("kiegroup", "droolsjbpm-build-bootstrap"), BRANCH_72X),
                Tuple.of(new GitHubRepository("kiegroup", "droolsjbpm-knowledge"), BRANCH_72X),
                Tuple.of(new GitHubRepository("kiegroup", "drools"), BRANCH_72X),
                Tuple.of(new GitHubRepository("kiegroup", "optaplanner"), BRANCH_72X),
                Tuple.of(new GitHubRepository("kiegroup", "jbpm"), BRANCH_72X),
                Tuple.of(new GitHubRepository("kiegroup", "droolsjbpm-integration"), BRANCH_72X),
                Tuple.of(new GitHubRepository("kiegroup", "droolsjbpm-tools"), BRANCH_72X),
                Tuple.of(new GitHubRepository("kiegroup", "kie-uberfire-extensions"), BRANCH_72X),
                Tuple.of(new GitHubRepository("kiegroup", "guvnor"), BRANCH_72X),
                Tuple.of(new GitHubRepository("kiegroup", "kie-wb-playground"), BRANCH_72X),
                Tuple.of(new GitHubRepository("kiegroup", "kie-wb-common"), BRANCH_72X),
                Tuple.of(new GitHubRepository("kiegroup", "jbpm-form-modeler"), BRANCH_72X),
                Tuple.of(new GitHubRepository("kiegroup", "drools-wb"), BRANCH_72X),
                Tuple.of(new GitHubRepository("kiegroup", "optaplanner-wb"), BRANCH_72X),
                Tuple.of(new GitHubRepository("kiegroup", "jbpm-designer"), BRANCH_72X),
                Tuple.of(new GitHubRepository("kiegroup", "jbpm-wb"), BRANCH_72X),
                Tuple.of(new GitHubRepository("kiegroup", "kie-docs"), BRANCH_72X),
                Tuple.of(new GitHubRepository("kiegroup", "kie-wb-distributions"), BRANCH_72X)
        );
    }

}
