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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class KieGithubRepositoryBaseBranchTest {

    @Parameterized.Parameters(name = "{0}, {1}, {2} -> {3}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // upstream repo builds, master branch
                {new KieGitHubRepository("errai", "errai"), "uberfire", "master", "master"},
                {new KieGitHubRepository("errai", "errai"), "dashbuilder", "master", "master"},
                {new KieGitHubRepository("errai", "errai"), "guvnor", "master", "master"},
                {new KieGitHubRepository("uberfire", "uberfire"), "uberfire", "master", "master"},
                {new KieGitHubRepository("uberfire", "uberfire"), "dashbuilder", "master", "master"},
                {new KieGitHubRepository("uberfire", "uberfire"), "guvnor", "master", "master"},
                {new KieGitHubRepository("dashbuilder", "dashbuilder"), "guvnor", "master", "master"},
                {new KieGitHubRepository("droolsjbpm", "guvnor"), "jbpm-console-ng", "master", "master"},
                // upstream repo builds, 0.9.x + 0.5.x + 6.5.x branches
                {new KieGitHubRepository("errai", "errai"), "uberfire", "0.9.x", "3.2"},
                {new KieGitHubRepository("errai", "errai"), "dashbuilder", "0.5.x", "3.2"},
                {new KieGitHubRepository("errai", "errai"), "dashbuilder", "6.5.x", "3.2"},
                {new KieGitHubRepository("uberfire", "uberfire"), "uberfire", "0.9.x", "0.9.x"},
                {new KieGitHubRepository("uberfire", "uberfire"), "uberfire-extensions", "0.9.x", "0.9.x"},
                {new KieGitHubRepository("uberfire", "uberfire"), "dashbuilder", "0.5.x", "0.9.x"},
                {new KieGitHubRepository("uberfire", "uberfire-extensions"), "dashbuilder", "0.5.x", "0.9.x"},
                {new KieGitHubRepository("uberfire", "uberfire"), "guvnor", "6.5.x", "0.9.x"},
                {new KieGitHubRepository("uberfire", "uberfire-extensions"), "guvnor", "6.5.x", "0.9.x"},
                {new KieGitHubRepository("dashbuilder", "dashbuilder"), "guvnor", "6.5.x", "0.5.x"},
                {new KieGitHubRepository("droolsjbpm", "guvnor"), "jbpm-console-ng", "6.5.x", "6.5.x"},
                // upstream repo builds, 0.8.x + 0.4.x + 6.4.x branches
                {new KieGitHubRepository("errai", "errai"), "uberfire", "0.8.x", "3.2"},
                {new KieGitHubRepository("errai", "errai"), "dashbuilder", "0.4.x", "3.2"},
                {new KieGitHubRepository("errai", "errai"), "dashbuilder", "6.4.x", "3.2"},
                {new KieGitHubRepository("uberfire", "uberfire"), "uberfire", "0.8.x", "0.8.x"},
                {new KieGitHubRepository("uberfire", "uberfire"), "uberfire-extensions", "0.8.x", "0.8.x"},
                {new KieGitHubRepository("uberfire", "uberfire"), "dashbuilder", "0.4.x", "0.8.x"},
                {new KieGitHubRepository("uberfire", "uberfire-extensions"), "dashbuilder", "0.4.x", "0.8.x"},
                {new KieGitHubRepository("uberfire", "uberfire"), "guvnor", "6.4.x", "0.8.x"},
                {new KieGitHubRepository("uberfire", "uberfire-extensions"), "guvnor", "6.4.x", "0.8.x"},
                {new KieGitHubRepository("dashbuilder", "dashbuilder"), "guvnor", "6.4.x", "0.4.x"},
                {new KieGitHubRepository("droolsjbpm", "guvnor"), "jbpm-console-ng", "6.4.x", "6.4.x"},
                // upstream repo builds, 0.7.x + 0.3.x + 6.3.x branches
                {new KieGitHubRepository("uberfire", "uberfire"), "uberfire", "0.7.x", "0.7.x"},
                {new KieGitHubRepository("uberfire", "uberfire"), "uberfire-extensions", "0.7.x", "0.7.x"},
                {new KieGitHubRepository("uberfire", "uberfire"), "dashbuilder", "0.3.x", "0.7.x"},
                {new KieGitHubRepository("uberfire", "uberfire-extensions"), "dashbuilder", "0.3.x", "0.7.x"},
                {new KieGitHubRepository("uberfire", "uberfire"), "guvnor", "6.3.x", "0.7.x"},
                {new KieGitHubRepository("uberfire", "uberfire-extensions"), "guvnor", "6.3.x", "0.7.x"},
                {new KieGitHubRepository("dashbuilder", "dashbuilder"), "guvnor", "6.3.x", "0.3.x"},
                {new KieGitHubRepository("droolsjbpm", "guvnor"), "jbpm-console-ng", "6.3.x", "6.3.x"},
                // upstream repo builds, 0.5.x  + 0.2.x + 6.2.x branches
                {new KieGitHubRepository("uberfire", "uberfire"), "uberfire", "0.5.x", "0.5.x"},
                {new KieGitHubRepository("uberfire", "uberfire"), "uberfire-extensions", "0.5.x", "0.5.x"},
                {new KieGitHubRepository("uberfire", "uberfire"), "dashbuilder", "0.2.x", "0.5.x"},
                {new KieGitHubRepository("uberfire", "uberfire-extensions"), "dashbuilder", "0.2.x", "0.5.x"},
                {new KieGitHubRepository("uberfire", "uberfire"), "guvnor", "6.2.x", "0.5.x"},
                {new KieGitHubRepository("uberfire", "uberfire-extensions"), "guvnor", "6.2.x", "0.5.x"},
                {new KieGitHubRepository("dashbuilder", "dashbuilder"), "guvnor", "6.2.x", "0.2.x"},
                {new KieGitHubRepository("droolsjbpm", "guvnor"), "jbpm-console-ng", "6.2.x", "6.2.x"},
                // downstream repo builds
        });
    }

    @Parameterized.Parameter(0)
    public KieGitHubRepository repo;

    @Parameterized.Parameter(1)
    public String prRepo;

    @Parameterized.Parameter(2)
    public String prTargetBranch;

    @Parameterized.Parameter(3)
    public String expectedBaseBranch;

    @Test
    public void test() {
        String baseBranch = KieRepositoryLists.getBaseBranchFor(repo.getName(), prRepo, prTargetBranch);
        assertEquals(expectedBaseBranch, baseBranch);
    }
}
