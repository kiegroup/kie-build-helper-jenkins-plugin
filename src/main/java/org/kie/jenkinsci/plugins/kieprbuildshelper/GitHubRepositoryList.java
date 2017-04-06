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

import java.util.Arrays;
import java.util.List;

public class GitHubRepositoryList {
    private final List<KieGitHubRepository> list;

    public GitHubRepositoryList(List<KieGitHubRepository> list) {
        this.list = list;
    }

    public List<KieGitHubRepository> getList() {
        return list;
    }

    public int size() {
        return list.size();
    }

    public boolean contains(KieGitHubRepository repo) {
        return list.contains(repo);
    }

    public void filterOutUnnecessaryUpstreamRepos(String prRepoName) {
        if (Arrays.asList("droolsjbpm-knowledge", "drools", "optaplanner").contains(prRepoName)) {
            list.remove(new KieGitHubRepository("errai", "errai"));
            list.remove(new KieGitHubRepository("uberfire", "uberfire"));
            list.remove(new KieGitHubRepository("uberfire", "uberfire-extensions"));
            list.remove(new KieGitHubRepository("dashbuilder", "dashbuilder"));
        }
        // nothing depends on stuff from -tools repo
        list.remove(new KieGitHubRepository("kiegroup", "droolsjbpm-tools"));
        // no need to build docs as other repos do not depend on them
        list.remove(new KieGitHubRepository("kiegroup", "kie-docs"));

        if ("kie-docs".equals(prRepoName)) {
            // we only need to build repos up to "guvnor" as that's what kie-docs-code depends on
            list.remove(new KieGitHubRepository("kiegroup", "kie-wb-playground"));
            list.remove(new KieGitHubRepository("kiegroup", "kie-wb-common"));
            list.remove(new KieGitHubRepository("kiegroup", "jbpm-form-modeler"));
            list.remove(new KieGitHubRepository("kiegroup", "drools-wb"));
            list.remove(new KieGitHubRepository("kiegroup", "optaplanner-wb"));
            list.remove(new KieGitHubRepository("kiegroup", "jbpm-designer"));
            list.remove(new KieGitHubRepository("kiegroup", "jbpm-wb"));
            list.remove(new KieGitHubRepository("kiegroup", "kie-wb-distributions"));
        }
    }

}
