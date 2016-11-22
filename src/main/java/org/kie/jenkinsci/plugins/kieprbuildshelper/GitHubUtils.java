/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
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

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.BuildListener;
import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GitHub;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

public class GitHubUtils {

    /**
     * Checks whether GitHub repository has specific branch.
     *
     * Used to check if the fork has the same branch as repo with PR.
     *
     * @param fullRepoName full GitHub repository name (owner + name)
     * @param branch       branch to check
     * @param github       GitHub API object used to talk to GitHub REST interface
     * @return true if the branch exists, otherwise false
     */
    public static boolean checkBranchExists(String fullRepoName, String branch, GitHub github) {
        try {
            return github.getRepository(fullRepoName).getBranches().containsKey(branch);
        } catch (FileNotFoundException e) {
            // thrown when the repository does not exist -> branch does not exist either
            return false;
        } catch (IOException e) {
            throw new RuntimeException("Error while checking if branch '" + branch + "' exists in repo '" + fullRepoName + "'!", e);
        }
    }

    /**
     * Checks whether GitHub repository contains open PR with the same branch and owner. If so that means those two
     * PRs are connected and need to be built together.
     *
     * @param fullRepoName full GitHub repository name (owner + name)
     * @param prBranch     branch that was used to submit the PR
     * @param prRepoOwner  owner of the repository that contain the PR branch
     * @param github       GitHub API object used to talk to GitHub REST interface
     * @return true if the specified repository contains open PR with the same branch and owner, otherwise false
     */
    public static boolean checkHasOpenPRAssociated(String fullRepoName, String prBranch, String prRepoOwner, GitHub github) {
        try {
            List<GHPullRequest> prs = github.getRepository(fullRepoName).getPullRequests(GHIssueState.OPEN);
            for (GHPullRequest pr : prs) {
                // check if the PR source branch and name of the fork are the ones we are looking for
                if (pr.getHead().getRef().equals(prBranch) &&
                        pr.getHead().getRepository().getOwner().getLogin().equals(prRepoOwner)) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            throw new RuntimeException("Failed to get info about PRs for " + fullRepoName);
        }
    }

    /**
     * Clones GitHub repository into specified destination dir and checkouts the configured branch.
     *
     * @param ghRepo        GitHub repository to clone (contains both owner and repo name)
     * @param branch        branch to checkout once the repository was cloned
     * @param destDir       destination directory where to put the newly cloned repository
     * @param buildListener Jenkins BuildListener used by the GitClient to print status info
     */
    public static void cloneAndCheckout(KieGitHubRepository ghRepo, String branch, FilePath destDir,
                                        BuildListener buildListener) throws IOException, InterruptedException {
        destDir.mkdirs();
        GitClient git = Git.with(buildListener, new EnvVars())
                .in(destDir)
                .using("git")
                .getClient();
        git.clone(ghRepo.getReadOnlyCloneURL(), "origin", true, null);
        git.checkoutBranch(branch, "origin/" + branch);
    }

    public static void cloneRepositories(FilePath basedir, Map<KieGitHubRepository, String> repsotiriesWithBranches,
                                         BuildListener listener) throws IOException, InterruptedException {
        for (Map.Entry<KieGitHubRepository, String> entry : repsotiriesWithBranches.entrySet()) {
            KieGitHubRepository ghRepo = entry.getKey();
            String branch = entry.getValue();
            FilePath repoDir = new FilePath(basedir, ghRepo.getName());
            GitHubUtils.cloneAndCheckout(ghRepo, branch, repoDir, listener);
        }
    }

    public static void logRepositories(Map<KieGitHubRepository, String> repos, PrintStream buildLogger) {
        if (repos.size() > 0) {
            buildLogger.println("GitHub repositories that will be cloned and build:");
            for (Map.Entry<KieGitHubRepository, String> entry : repos.entrySet()) {
                // print as '<URL>:<branch>'
                buildLogger.println("\t" + entry.getKey().getReadOnlyCloneURL() + ":" + entry.getValue());
            }
        } else {
            buildLogger.println("No required GitHub repositories found.");
        }
    }
}
