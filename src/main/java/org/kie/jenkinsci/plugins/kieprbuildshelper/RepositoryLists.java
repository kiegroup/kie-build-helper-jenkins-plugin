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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

public class RepositoryLists {

    public static final String KIE_ORG_UNIT = "kiegroup";
    public static final GitHubRepository KIE_BOOTSTRAP_REPO = new GitHubRepository(KIE_ORG_UNIT, "droolsjbpm-build-bootstrap");


    public static List<Tuple<GitHubRepository, GitBranch>> create(Tuple<GitHubRepository, GitBranch> repositoryListLocation,
                                                                  GitBranch kieBranch) {

        List<Tuple<GitHubRepository, GitBranch>> repos = new ArrayList<>();
        fetchKIERepositoryList(repositoryListLocation._1(), repositoryListLocation._2()).forEach(r -> repos.add(new Tuple<>(r, kieBranch)));
        return repos;
    }

    public static List<GitHubRepository> fetchKIERepositoryList(GitHubRepository repo, GitBranch branch) {
        List<GitHubRepository> repos = new ArrayList<>();
        URL reposFileUrl = createUrlForRepositoryList(repo, branch);
        try (InputStream input = reposFileUrl.openStream()) {
            for (String repoName : IOUtils.readLines(input)) {
            	repos.add(new GitHubRepository(KIE_ORG_UNIT, repoName));
            }
        } catch (IOException e) {
            throw new RuntimeException("Can not fetch kiegroup repository list '" + reposFileUrl + "'!", e);
        }
        return repos;
    }

    private static URL createUrlForRepositoryList(GitHubRepository repo, GitBranch branch) {
        String strUrl = "https://raw.githubusercontent.com/" + repo.getFullName() + "/" + branch.getName() + "/script/repository-list.txt";
        try {
            return new URL(strUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid repository-list URL: " + strUrl, e);
        }
    }

    /**
     * TODO: this is an ugly hack. The dependency between repositories (or directly modules) should to be checked automatically for every build
     */
    public static List<Tuple<GitHubRepository, GitBranch>> filterOutUnnecessaryRepos(List<Tuple<GitHubRepository, GitBranch>> repos, GitHubRepository baseRepo) {
    	// nothing depends on stuff from -tools repo
        repos.removeIf(repo -> repo._1().equals(new GitHubRepository("kiegroup", "droolsjbpm-tools")));
        // no need to build docs as other repos do not depend on them
        repos.removeIf(repo -> repo._1().equals(new GitHubRepository("kiegroup", "kie-docs")));

        if ("kie-docs".equals(baseRepo.getName())) {
            // we only need to build repos up to "guvnor" as that's what kie-docs-code depends on
            repos.removeIf(repo -> repo._1().equals(new GitHubRepository("kiegroup", "kie-wb-playground")));
            repos.removeIf(repo -> repo._1().equals(new GitHubRepository("kiegroup", "kie-wb-common")));
            repos.removeIf(repo -> repo._1().equals(new GitHubRepository("kiegroup", "drools-wb")));
            repos.removeIf(repo -> repo._1().equals(new GitHubRepository("kiegroup", "optaplanner-wb")));
            repos.removeIf(repo -> repo._1().equals(new GitHubRepository("kiegroup", "jbpm-designer")));
            repos.removeIf(repo -> repo._1().equals(new GitHubRepository("kiegroup", "jbpm-wb")));
            repos.removeIf(repo -> repo._1().equals(new GitHubRepository("kiegroup", "kie-wb-distributions")));
        }
        return repos;
    }
}
