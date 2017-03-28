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

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GitHubRepositoryList {
    private final List<KieGitHubRepository> list;

    public static GitHubRepositoryList fromClasspathResource(String resourcePath) {
        InputStream is = GitHubRepositoryList.class.getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IllegalArgumentException("Specified classpath resource '" + resourcePath + "' does not exist!");
        }
        List<String> lines;
        try {
            lines = IOUtils.readLines(is);
        } catch (IOException e) {
            throw new RuntimeException("Error while reading data from classpath resource '" + resourcePath + "'!", e);
        }
        List<KieGitHubRepository> list = new ArrayList<KieGitHubRepository>();
        for (String line : lines) {
            String[] parts = line.split("/");
            list.add(new KieGitHubRepository(parts[0], parts[1]));
        }
        return new GitHubRepositoryList(list);
    }

    public static GitHubRepositoryList forBranch(String branch) {
        // TODO make this work OOTB when new branch is added
        if ("master".equals(branch)) {
            return KieRepositoryLists.getListForMasterBranch();
        } else if (Arrays.asList("6.5.x", "0.9.x", "0.5.x").contains(branch)) {
            return KieRepositoryLists.getListFor65xBranch();
        } else if (Arrays.asList("6.4.x", "0.8.x", "0.4.x").contains(branch)) {
            return KieRepositoryLists.getListFor64xBranch();
        } else if (Arrays.asList("6.3.x", "0.7.x", "0.3.x").contains(branch)) {
            return KieRepositoryLists.getListFor63xBranch();
        } else if (Arrays.asList("6.2.x", "0.5.x", "0.2.x").contains(branch)) {
            return KieRepositoryLists.getListFor62xBranch();
        } else {
            throw new IllegalArgumentException("Invalid target branch '" + branch + "'!");
        }
    }

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
