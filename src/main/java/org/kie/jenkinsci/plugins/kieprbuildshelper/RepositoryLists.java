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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.yaml.snakeyaml.Yaml;

public class RepositoryLists {

    private static Logger logger = Logger.getLogger(RepositoryLists.class.getName());

    public static final String KIE_ORG_UNIT = "kiegroup";
    public static final GitHubRepository KIE_BOOTSTRAP_REPO = new GitHubRepository(KIE_ORG_UNIT, "droolsjbpm-build-bootstrap");

    public static List<Tuple<GitHubRepository, GitBranch>> createFor(GitHubRepository repo, GitBranch branch) {
        return createFor(repo, branch, KIE_BOOTSTRAP_REPO, GitBranch.MASTER);
    }
    public static List<Tuple<GitHubRepository, GitBranch>> createFor(GitHubRepository repo, GitBranch branch,
                                                                     GitHubRepository bootstrapRepo, GitBranch bootstrapBranch) {
        String branchMappingContent = fetchBranchMappingFile(bootstrapRepo, bootstrapBranch);
        List<List<Tuple<GitHubRepository, GitBranch>>> allBranches = new ArrayList<>();
        Yaml yaml = new Yaml();
        Map<String, List> map = (Map<String, List>) yaml.load(branchMappingContent);
        for (Map.Entry<String, List> entry : map.entrySet()) {
            final GitBranch kieBranch = new GitBranch(entry.getKey());
            List<Map<String, String>> upstreamRepos = (List<Map<String, String>>) entry.getValue();
            List<Tuple<GitHubRepository, GitBranch>> repos = parseUpstreamReposBranchMapping(upstreamRepos);
            fetchKIERepositoryList(kieBranch).forEach(r -> repos.add(new Tuple<>(r, kieBranch)));
            allBranches.add(repos);
        }
        // now we have repository lists for all branches and need to filter the correct list based on the requested repo+branch
        Tuple<GitHubRepository, GitBranch> requested = new Tuple<>(repo, branch);
        for (List<Tuple<GitHubRepository, GitBranch>> repos : allBranches) {
            if (repos.contains(requested)) {
                return repos;
            }
        }
        throw new IllegalArgumentException("Can not create repository list for repository " + repo + " and branch " + branch);
    }

    public static List<GitHubRepository> fetchKIERepositoryList(GitBranch branch) {
        List<GitHubRepository> repos = new ArrayList<>();
        URL reposFileUrl = createUrlForRepositoryList(KIE_BOOTSTRAP_REPO, branch);
        try (InputStream input = reposFileUrl.openStream()) {
            for (String repoName : IOUtils.readLines(input)) {
                // this is an exception for 6.5.x and older branches
                if ("kie-eap-modules".equals(repoName)) {
                    repos.add(new GitHubRepository("jboss-integration", repoName));
                } else {
                    repos.add(new GitHubRepository(KIE_ORG_UNIT, repoName));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Can not fetch kiegroup repository list '" + reposFileUrl + "'!", e);
        }
        return repos;
    }

    public static String fetchBranchMappingFile(GitHubRepository bootstrapRepo, GitBranch bootstrapBranch) {
        String branchMappingFileContent;
        URL branchMappingUrl;
        try {
            branchMappingUrl = createUrlForBranchMappingFile(bootstrapRepo, bootstrapBranch);
            branchMappingFileContent = IOUtils.toString(branchMappingUrl);
            logger.fine("Raw content of the fetched branch mapping file:\n" + branchMappingFileContent);
        } catch (IOException e) {
            throw new RuntimeException("Can not fetch branch mapping file!", e);
        }
        return branchMappingFileContent;
    }

    private static List<Tuple<GitHubRepository, GitBranch>> parseUpstreamReposBranchMapping(List<Map<String, String>> upstreamRepos) {
        List<Tuple<GitHubRepository, GitBranch>> result = new ArrayList<>();
        for (Map<String, String> map : upstreamRepos) {
            for (Map.Entry<String, String> repoToBranch : map.entrySet()) {
                result.add(Tuple.of(GitHubRepository.from(repoToBranch.getKey()), new GitBranch(repoToBranch.getValue())));
            }
        }
        return result;
    }

    private static URL createUrlForBranchMappingFile(GitHubRepository bootstrapRepo, GitBranch branch) {
        String strUrl = "https://raw.githubusercontent.com/" + bootstrapRepo.getFullName() + "/" + branch.getName() + "/script/branch-mapping.yaml";
        try {
            return new URL(strUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid branch mapping file URL: " + strUrl, e);
        }
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
