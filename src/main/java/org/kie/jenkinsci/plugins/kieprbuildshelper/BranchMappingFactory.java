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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

public class BranchMappingFactory {
    private static Logger logger = LoggerFactory.getLogger(BranchMappingFactory.class);

    public static Set<BranchMapping> createFrom(GitHubRepository repo, GitBranch branch) {
        return createFrom(fetchBranchMappingFile(repo, branch));
    }

    public static Set<BranchMapping> createFrom(String content) {
        Set<BranchMapping> branchMappings = new HashSet<>();
        Yaml yaml = new Yaml();
        Map<String, List> map = (Map<String, List>) yaml.load(content);
        for (Map.Entry<String, List> entry : map.entrySet()) {
            final GitBranch kieBranch = new GitBranch(entry.getKey());
            List<Map<String, String>> upstreamDeps = (List<Map<String, String>>) entry.getValue();

            List<Tuple<GitHubRepository, GitBranch>> parsedUpstreamDeps = parseUpstreamReposBranchMapping(upstreamDeps);
            branchMappings.add(new BranchMapping(kieBranch, parsedUpstreamDeps));
        }
        return branchMappings;
    }

    public static BranchMapping getBranchMapping(Set<BranchMapping> branchMappings, GitHubRepository targetRepo, GitBranch prTargetBranch) {
        Tuple<GitHubRepository, GitBranch> target = Tuple.of(targetRepo, prTargetBranch);
        for (BranchMapping branchMapping : branchMappings) {
            if (branchMapping.getUpstreamDeps().contains(target)) {
                return branchMapping;
            }
        }
        // not an upstream repo, so it is one of KIE repos
        for (BranchMapping branchMapping : branchMappings) {
            if (branchMapping.getKieBranch().equals(prTargetBranch)) {
                return branchMapping;
            }
        }
        throw new IllegalStateException("Can not find KIE target branch:" + target + ", " + branchMappings);
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

    public static String fetchBranchMappingFile(GitHubRepository repo, GitBranch branch) {
        String branchMappingFileContent;
        URL branchMappingUrl;
        try {
            branchMappingUrl = createUrlForBranchMappingFile(repo, branch);
            branchMappingFileContent = IOUtils.toString(branchMappingUrl);
            logger.trace("Raw content of the fetched branch mapping file ({}):\n{}", branchMappingUrl, branchMappingFileContent);
        } catch (IOException e) {
            throw new RuntimeException("Can not fetch branch mapping file!", e);
        }
        return branchMappingFileContent;
    }

    private static URL createUrlForBranchMappingFile(GitHubRepository bootstrapRepo, GitBranch branch) {
        String strUrl = "https://raw.githubusercontent.com/" + bootstrapRepo.getFullName() + "/" + branch.getName() + "/script/branch-mapping.yaml";
        try {
            return new URL(strUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid branch mapping file URL: " + strUrl, e);
        }
    }

}
