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

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GitHubRepositoryListTest {

    @Test
    public void shouldSuccessfullyLoadListForMaster() {
        GitHubRepositoryList repoList = GitHubRepositoryList.fromClasspathResource(GitHubRepositoryList.KIE_REPO_LIST_MASTER_RESOURCE_PATH);
        Assert.assertEquals(23, repoList.size());
        Assert.assertEquals(new KieGitHubRepository("uberfire", "uberfire"), repoList.getList().get(0));
        Assert.assertEquals(new KieGitHubRepository("jboss-integration", "kie-eap-modules"), repoList.getList().get(22));
    }

    @Test (expected = IllegalArgumentException.class)
    public void shouldReportFailureForNonExistentResource() {
        GitHubRepositoryList repoList = GitHubRepositoryList.fromClasspathResource("non-existing");
    }

    @Test
    public void shouldFilterOutUFAndDashbuilderReposForDroolsRepo() {
        GitHubRepositoryList ghList = GitHubRepositoryList.forBranch("master");
        ghList.filterOutUnnecessaryUpstreamRepos("drools");
        assertFalse(ghList.contains(new KieGitHubRepository("uberfire", "uberfire")));
        assertFalse(ghList.contains(new KieGitHubRepository("uberfire", "uberfire-extensions")));
        assertFalse(ghList.contains(new KieGitHubRepository("dashbuilder", "dashbuilder")));

        assertTrue(ghList.contains(new KieGitHubRepository("droolsjbpm", "droolsjbpm-knowledge")));
    }

}
