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
import org.eclipse.jgit.transport.RefSpec;
import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GitHub;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GitHubUtils {

    public static final File GIT_REFERENCE_BASEDIR = new File("/home/jenkins/git-repos/");

    public static List<GHPullRequest> getOpenPullRequests(GitHubRepository repo, GitHub github) {
        try {
            return github.getRepository(repo.getFullName()).getPullRequests(GHIssueState.OPEN);
        } catch (IOException e) {
            throw new RuntimeException("Failed to get open PRs for " + repo.getFullName(), e);
        }
    }

    /**
     * @param repo         GitHub repository to check against
     * @param sourceBranch source branch name
     * @param github       GitHub API object
     * @return optionally pull request which is both open and created against the specific source branch
     */
    public static Optional<GitHubPRSummary> findOpenPullRequest(GitHubRepository repo, String sourceBranch, String prAuthor, GitHub github) {
        try {
            List<GHPullRequest> prs = getOpenPullRequests(repo, github);
            for (GHPullRequest pr : prs) {
                // check if the PR source branch and name of the fork are the ones we are looking for
                if (pr.getHead().getRef().equals(sourceBranch) &&
                        pr.getHead().getRepository().getOwner().getLogin().equals(prAuthor)) {
                    return Optional.of(
                            new GitHubPRSummary(repo, pr.getNumber(), sourceBranch, prAuthor, pr.getMergeable()));
                }
            }
            return Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException("Failed to get info about PRs for " + repo, e);
        }
    }

    /**
     * Clones the specified repository, then fetches the requested refspec and checkouts the destination part.
     *
     * @param gitClient git client. Already setup to work in certain directory.
     * @param ghRepo GitHub repository to clone
     * @param refspec {@link RefSpec} to fetch. The destination part is then used for checkout
     *
     * @throws InterruptedException when interrupted while performing Git operations
     */
    public static void cloneFetchCheckout(GitClient gitClient, KieGitHubRepository ghRepo, RefSpec refspec, File referenceDir)
            throws InterruptedException {
        gitClient.clone(ghRepo.getReadOnlyCloneURL(), "origin", true, referenceDir.getAbsolutePath());
        gitClient.fetch("origin", refspec);
        gitClient.checkout().ref(refspec.getDestination()).execute();
    }

    public static void cloneRepositories(FilePath basedir, Map<KieGitHubRepository, RefSpec> repositoriesWithRefspec,
                                         File referenceBasedir, BuildListener listener) throws IOException, InterruptedException {
        for (Map.Entry<KieGitHubRepository, RefSpec> entry : repositoriesWithRefspec.entrySet()) {
            KieGitHubRepository ghRepo = entry.getKey();
            RefSpec refspec = entry.getValue();
            FilePath repoDir = new FilePath(basedir, ghRepo.getName());
            repoDir.mkdirs();
            GitClient gitClient = Git.with(listener, new EnvVars())
                    .in(repoDir)
                    .using("git")
                    .getClient();
            File referenceDir = new File(referenceBasedir, ghRepo.getName() + ".git");
            cloneFetchCheckout(gitClient, ghRepo, refspec, referenceDir);
        }
    }

    public static void logRepositories(Map<KieGitHubRepository, RefSpec> repos, PrintStream buildLogger) {
        if (repos.size() > 0) {
            buildLogger.println("GitHub repositories that will be cloned and build:");
            for (Map.Entry<KieGitHubRepository, RefSpec> entry : repos.entrySet()) {
                // print as '<URL>:<refspec>'
                buildLogger.println("\t" + entry.getKey().getReadOnlyCloneURL() + ":" + entry.getValue());
            }
        } else {
            buildLogger.println("No required GitHub repositories found.");
        }
    }
}
