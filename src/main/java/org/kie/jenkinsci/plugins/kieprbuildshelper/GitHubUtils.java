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
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitHubUtils {

    public static final File GIT_REFERENCE_BASEDIR = new File("/home/jenkins/git-repos/");

    public static final Pattern GITHUB_PR_URL_PATTERN = Pattern.compile("\\w+://github.com/.+/(.+)/pull/\\d+");

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
    public static Optional<GitHubPRSummary> findOpenPullRequest(GitHubRepository repo, GitBranch sourceBranch, String prAuthor, GitHub github) {
        try {
            List<GHPullRequest> prs = getOpenPullRequests(repo, github);
            for (GHPullRequest pr : prs) {
                // check if the PR source branch and name of the fork are the ones we are looking for
                if (pr.getHead().getRef().equals(sourceBranch.getName()) &&
                        pr.getHead().getRepository().getOwner().getLogin().equals(prAuthor)) {
                    return Optional.of(GitHubPRSummary.fromGHPullRequest(pr, github));
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
    public static void cloneFetchCheckout(GitClient gitClient, GitHubRepository ghRepo, RefSpec refspec, File referenceDir)
            throws InterruptedException {
        gitClient.clone(ghRepo.getReadOnlyCloneURL(), "origin", true, referenceDir.getAbsolutePath());
        gitClient.fetch("origin", refspec);
        gitClient.checkout().ref(refspec.getDestination()).execute();
    }

    public static void cloneRepositories(FilePath basedir, List<Tuple<GitHubRepository, RefSpec>> repositoriesWithRefspec,
                                         File referenceBasedir, BuildListener listener) throws IOException, InterruptedException {
        for (Tuple<GitHubRepository, RefSpec> repoWithRefSpec : repositoriesWithRefspec) {
            GitHubRepository ghRepo = repoWithRefSpec._1();
            RefSpec refspec = repoWithRefSpec._2();
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

    public static void logRepositories(List<Tuple<GitHubRepository, RefSpec>> repos, PrintStream buildLogger) {
        if (repos.size() > 0) {
            buildLogger.println("GitHub repositories that will be cloned and built:");
            for (Tuple<GitHubRepository, RefSpec> repoWithRefSpec : repos) {
                // print as '<URL>:<refspec>'
                buildLogger.println("\t" + repoWithRefSpec._1().getReadOnlyCloneURL() + ":" + repoWithRefSpec._2());
            }
        } else {
            buildLogger.println("No required GitHub repositories found.");
        }
    }

    public static String extractRepositoryName(String prLink) {
        if (prLink == null) {
            throw new IllegalArgumentException("Supplied PR link is null");
        }

        Matcher matcher = GITHUB_PR_URL_PATTERN.matcher(prLink);

        while (matcher.find()) {
            return matcher.group(1);
        }

        throw new IllegalArgumentException("Supplied PR link '" + prLink + "' does not match expected pattern " + GITHUB_PR_URL_PATTERN );
    }
}
