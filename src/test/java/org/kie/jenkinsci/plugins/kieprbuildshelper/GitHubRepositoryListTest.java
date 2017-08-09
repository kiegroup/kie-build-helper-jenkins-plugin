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

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GitHubRepositoryListTest {

    @Test
    public void shouldFilterOutUFAndDashbuilderReposForDroolsRepo() {
        GitHubRepositoryList ghList = KieRepositoryLists.getListForBranch("drools",
                                                                          "master");
        ghList.filterOutUnnecessaryUpstreamRepos("drools");
        assertFalse(ghList.contains(new KieGitHubRepository("errai", "errai")));
        assertFalse(ghList.contains(new KieGitHubRepository("uberfire", "uberfire")));
        assertFalse(ghList.contains(new KieGitHubRepository("uberfire", "uberfire-extensions")));
        assertFalse(ghList.contains(new KieGitHubRepository("dashbuilder", "dashbuilder")));

        assertTrue(ghList.contains(new KieGitHubRepository("kiegroup", "droolsjbpm-knowledge")));
    }

    @Test
    public void shouldNotFilterOutErraiAndUFAndDashbuilderReposForDroolsjbpmIntegrationRepo() {
        GitHubRepositoryList ghList = KieRepositoryLists.getListForBranch("droolsjbpm-integration",
                                                                          "master");
        ghList.filterOutUnnecessaryUpstreamRepos("droolsjbpm-integration");
        assertTrue(ghList.contains(new KieGitHubRepository("errai", "errai")));
        assertTrue(ghList.contains(new KieGitHubRepository("uberfire", "uberfire")));
        assertTrue(ghList.contains(new KieGitHubRepository("kiegroup", "droolsjbpm-knowledge")));
        assertTrue(ghList.contains(new KieGitHubRepository("dashbuilder", "dashbuilder")));
    }

}
