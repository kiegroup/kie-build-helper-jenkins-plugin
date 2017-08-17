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

    private final int number;
    private final GitHubRepository targetRepo;
    private final GitBranch targetBranch;
    private final GitHubRepository sourceRepo;
    private final GitBranch sourceBranch;

    private final boolean mergeable;

    public GitHubPRSummary(int number, GitHubRepository targetRepo, GitBranch targetBranch, GitHubRepository sourceRepo, GitBranch sourceBranch, boolean mergeable) {
        this.number = number;
        this.targetRepo = targetRepo;
        this.targetBranch = targetBranch;
        this.sourceRepo = sourceRepo;
        this.sourceBranch = sourceBranch;
        this.mergeable = mergeable;
    }

    public int getNumber() {
        return number;
    }

    public GitHubRepository getTargetRepo() {
        return targetRepo;
    }

    public GitBranch getTargetBranch() {
        return targetBranch;
    }

    public String getTargetRepoName() {
        return targetRepo.getName();
    }

    public GitHubRepository getSourceRepo() {
        return sourceRepo;
    }

    public GitBranch getSourceBranch() {
        return sourceBranch;
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
        String str = removeGithubDotCom(prLink);
        String[] parts = str.split("/");
        String targetRepoOwner = parts[0];
        String targetRepoName = parts[1];
        // parts[2] == "pull", not needed
        int number = Integer.parseInt(parts[3]);
        GHPullRequest pr;
        try {
            pr = github.getRepository(targetRepoOwner + "/" + targetRepoName).getPullRequest(number);
            return GitHubPRSummary.fromGHPullRequest(pr);
        } catch (IOException e) {
            throw new RuntimeException("Error getting info about PR " + prLink, e);
        }
    }

    public static GitHubPRSummary fromGHPullRequest(final GHPullRequest pr) {
        String targetRepoOwner = pr.getBase().getUser().getLogin();
        String targetRepoName = pr.getBase().getRepository().getName();
        GitHubRepository targetRepo = new GitHubRepository(targetRepoOwner, targetRepoName);
        GitBranch targetBranch = new GitBranch(pr.getBase().getRef());

        String sourceRepoOwner = pr.getHead().getUser().getLogin();
        String sourceRepoName = pr.getHead().getRepository().getName();
        GitHubRepository sourceRepo = new GitHubRepository(sourceRepoOwner, sourceRepoName);
        GitBranch sourceBranch = new GitBranch(pr.getHead().getRef());

        Boolean isMergeable;
        String errorMsg = "Can not get 'mergeable' status for PR " + pr;
        try {
            isMergeable = pr.getMergeable();
            if (isMergeable == null) { // the method getMergeable() can return null which is not valid
                throw new NullPointerException(errorMsg + ". The github-api library returned null!");
            }
        } catch (IOException e) {
            throw new RuntimeException(errorMsg, e);
        }
        return new GitHubPRSummary(pr.getNumber(), targetRepo, targetBranch, sourceRepo, sourceBranch, isMergeable);
    }

    /**
     * Removes the 'github.com/' prefix from the specified the PR link.
     *
     * @param prLink the full PR link
     *
     * @return part of the PR link that contains the important info (repo owner, repo name and PR ID)
     */
    private static String removeGithubDotCom(String prLink) {
        int ghComIdx = prLink.indexOf("github.com");
        if (ghComIdx < 0) {
            throw new IllegalArgumentException("Provided Github PR link '" + prLink + "' is not valid, as it does not contain the string github.com!");
        }
        String noGhComPrStr = prLink.substring(ghComIdx + "github.com/".length());
        // now the string contains "<repoOwner>/<repoName>/pull/<pullId>"
        if (noGhComPrStr.endsWith("/")) {
            return noGhComPrStr.substring(0, noGhComPrStr.length() - 1);
        } else {
            return noGhComPrStr;
        }
    }

    @Override
    public String toString() {
        return "GitHubPRSummary{" +
                "number=" + number +
                ", targetRepo=" + targetRepo +
                ", targetBranch=" + targetBranch +
                ", sourceRepo=" + sourceRepo +
                ", sourceBranch=" + sourceBranch +
                ", mergeable=" + mergeable +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GitHubPRSummary that = (GitHubPRSummary) o;

        if (number != that.number) {
            return false;
        }
        if (mergeable != that.mergeable) {
            return false;
        }
        if (targetRepo != null ? !targetRepo.equals(that.targetRepo) : that.targetRepo != null) {
            return false;
        }
        if (targetBranch != null ? !targetBranch.equals(that.targetBranch) : that.targetBranch != null) {
            return false;
        }
        if (sourceRepo != null ? !sourceRepo.equals(that.sourceRepo) : that.sourceRepo != null) {
            return false;
        }
        return sourceBranch != null ? sourceBranch.equals(that.sourceBranch) : that.sourceBranch == null;
    }

    @Override
    public int hashCode() {
        int result = number;
        result = 31 * result + (targetRepo != null ? targetRepo.hashCode() : 0);
        result = 31 * result + (targetBranch != null ? targetBranch.hashCode() : 0);
        result = 31 * result + (sourceRepo != null ? sourceRepo.hashCode() : 0);
        result = 31 * result + (sourceBranch != null ? sourceBranch.hashCode() : 0);
        result = 31 * result + (mergeable ? 1 : 0);
        return result;
    }
}
