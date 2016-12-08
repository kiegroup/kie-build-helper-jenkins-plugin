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

import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GitHub;

import java.io.IOException;

public class GitHubPRSummary {

    private final GitHubRepository targetRepo;
    private final int number;
    private final String sourceBranch;
    private final String sourceRepoOwner;
    private final boolean mergeable;

    public GitHubPRSummary(GitHubRepository targetRepo, int number, String sourceBranch, String sourceRepoOwner, boolean mergeable) {
        this.targetRepo = targetRepo;
        this.number = number;
        this.sourceBranch = sourceBranch;
        this.sourceRepoOwner = sourceRepoOwner;
        this.mergeable = mergeable;
    }

    public GitHubRepository getTargetRepo() {
        return targetRepo;
    }

    public String getTargetRepoName() {
        return targetRepo.getName();
    }

    public int getNumber() {
        return number;
    }

    public String getSourceBranch() {
        return sourceBranch;
    }

    public String getSourceRepoOwner() {
        return sourceRepoOwner;
    }

    public boolean isMergeable() {
        return mergeable;
    }

    /**
     * Creates a PR summary from provided link, getting some of the info directly from Github.
     *
     * @param prLink pull request link, e.g. https://github.com/droolsjbpm/drools-wb/pull/77
     * @param github configured Github instance used to talk to Github REST API
     *
     * @return summary about the GitHub PR
     */
    public static GitHubPRSummary fromPRLink(String prLink, GitHub github) {
        String str = preProcessPRLink(prLink);
        String[] parts = str.split("/");
        String targetRepoOwner = parts[0];
        String targetRepoName = parts[1];
        GitHubRepository targetRepo = new GitHubRepository(targetRepoOwner, targetRepoName);
        // parts[2] == "pull", not needed
        int number = Integer.parseInt(parts[3]);
        GHPullRequest pr;
        try {
            pr = github.getRepository(targetRepoOwner + "/" + targetRepoName).getPullRequest(number);
            String sourceRepoOwner = pr.getHead().getRepository().getOwner().getLogin();
            String sourceBranch = pr.getHead().getRef();
            return new GitHubPRSummary(
                    targetRepo,
                    number,
                    sourceBranch,
                    sourceRepoOwner,
                    pr.getMergeable()
            );
        } catch (IOException e) {
            throw new RuntimeException("Error when getting info about PR " + prLink, e);
        }
    }

    /**
     * Pre-processes the PR link, removing the unnecessary parts like "github.com" and trailing slash.
     *
     * @param prLink the full PR link
     *
     * @return part of the PR link that contains the important info (repo owner, repo name and PR ID)
     */
    private static String preProcessPRLink(String prLink) {
        int ghComIdx = prLink.indexOf("github.com");
        if (ghComIdx < 0) {
            throw new IllegalArgumentException("Provided Github PR link '" + prLink + "' is not valid, as it does not contain the string github.com!");
        }
        String noGhComPrInfo = prLink.substring(ghComIdx + "github.com/".length());
        // now the string contains "<repoOwner>/<repoName>/pull/<pullId>"
        if (noGhComPrInfo.endsWith("/")) {
            return noGhComPrInfo.substring(0, noGhComPrInfo.length() - 1);
        } else {
            return noGhComPrInfo;
        }
    }

    @Override
    public String toString() {
        return "GitHubPRSummary{" +
                "targetRepo=" + targetRepo +
                ", number=" + number +
                ", sourceBranch='" + sourceBranch + '\'' +
                ", sourceRepoOwner='" + sourceRepoOwner + '\'' +
                ", mergeable=" + mergeable +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GitHubPRSummary that = (GitHubPRSummary) o;

        if (number != that.number) return false;
        if (mergeable != that.mergeable) return false;
        if (targetRepo != null ? !targetRepo.equals(that.targetRepo) : that.targetRepo != null) return false;
        if (sourceBranch != null ? !sourceBranch.equals(that.sourceBranch) : that.sourceBranch != null) return false;
        return sourceRepoOwner != null ? sourceRepoOwner.equals(that.sourceRepoOwner) : that.sourceRepoOwner == null;
    }

    @Override
    public int hashCode() {
        int result = targetRepo != null ? targetRepo.hashCode() : 0;
        result = 31 * result + number;
        result = 31 * result + (sourceBranch != null ? sourceBranch.hashCode() : 0);
        result = 31 * result + (sourceRepoOwner != null ? sourceRepoOwner.hashCode() : 0);
        result = 31 * result + (mergeable ? 1 : 0);
        return result;
    }
}
