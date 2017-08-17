/*
 * Copyright 2015 JBoss by Red Hat
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

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class GitHubRepositoryListTest {

    @Test
    public void shouldNotFilterOutErraiAndUFAndDashbuilderReposForDroolsjbpmIntegrationRepo() {
        List<Tuple<GitHubRepository, GitBranch>> fullList =
                RepositoryLists.createFor(new GitHubRepository("kiegroup", "droolsjbpm-integration"), GitBranch.MASTER);
        List<Tuple<GitHubRepository, GitBranch>> filtered =
                RepositoryLists.filterOutUnnecessaryRepos(fullList, new GitHubRepository("kiegroup", "droolsjbpm-integration"));

        List<GitHubRepository> repos = filtered.stream().map(Tuple::_1).collect(Collectors.toList());
        assertThat(repos).contains(
                new GitHubRepository("errai", "errai"),
                new GitHubRepository("AppFormer", "uberfire"),
                new GitHubRepository("dashbuilder", "dashbuilder"),
                new GitHubRepository("kiegroup", "droolsjbpm-knowledge")
        );
    }

}
