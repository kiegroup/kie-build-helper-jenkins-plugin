package org.kie.jenkinsci.plugins.kieprbuildshelper;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Extension;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Custom {@link Builder} which allows building upstream repositories during automated PR builds.
 * <p>
 * Building upstream repositories is usually needed when there are dependant PRs submitted into
 * different repositories.
 * <p>
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link UpstreamReposBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields
 * to remember the configuration.
 * <p>
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked.
 * <p>
 * <p>
 * What the builder does:
 * - collects info about the current PR and repository
 * - clones all needed upstream repos
 * - builds gathered upstream repositories using Maven
 */
public class UpstreamReposBuilder extends Builder {

    private static final Logger logger = Logger.getLogger(UpstreamReposBuilder.class.getName());

    @DataBoundConstructor
    public UpstreamReposBuilder() {
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        try {
            EnvVars envVars = build.getEnvironment(launcher.getListener());
            // get info about the PR from variables provided by GitHub Pull Request Builder plugin
            String prLink = envVars.get("ghprbPullLink");
            String prSrcBranch = envVars.get("ghprbSourceBranch");
            String prTargetBranch = envVars.get("ghprbTargetBranch");
            listener.getLogger().println("Working with PR: " + prLink);
            listener.getLogger().println("PR source branch: " + prSrcBranch);
            if (prLink == null || "".equals(prLink)) {
                throw new IllegalStateException("PR link not set! Make sure variable 'ghprbPullLink' contains valid link to GitHub Pull Request!");
            }
            if (prSrcBranch == null || "".equals(prSrcBranch)) {
                throw new IllegalStateException("PR source branch not set! Make sure variable 'ghprbSourceBranch' contains valid source branch for the configured GitHub Pull Request!");
            }
            if (prTargetBranch == null || "".equals(prTargetBranch)) {
                throw new IllegalStateException("PR target branch not set! Make sure variable 'ghprbTargetBranch' contains valid target branch for the configured GitHub Pull Request!");
            }
            GitHubRepositoryList kieRepoList = loadRepositoryList(prTargetBranch);
            String ghOAuthToken = getDescriptor().getGhOAuthToken();
            PrintStream logger = listener.getLogger();
            FilePath workspace = build.getWorkspace();

            if (ghOAuthToken == null) {
                logger.println("No GitHub OAuth token found. Please set one on global Jenkins configuration page.");
                return false;
            }
            FilePath upstreamReposDir = new FilePath(workspace, "upstream-repos");
            // clean-up the destination dir to avoid stale content
            logger.println("Cleaning-up directory " + upstreamReposDir.getRemote());
            upstreamReposDir.deleteRecursive();

            GitHub github = GitHub.connectUsingOAuth(ghOAuthToken);
            GitHubPRSummary prSummary = GitHubPRSummary.fromPRLink(prLink, prSrcBranch, github);
            List<GitHubRepository> upstreamRepos = gatherUpstreamReposToBuild(prSummary.getTargetRepo(), prSrcBranch, prSummary.getSourceRepoOwner(), kieRepoList, github);
            // clone upstream repositories
            for (GitHubRepository ghRepo : upstreamRepos) {
                FilePath repoDir = new FilePath(upstreamReposDir, ghRepo.getName());
                ghCloneAndCheckout(ghRepo, prSummary.getSourceBranch(), repoDir, listener);
            }
            // build upstream repositories using Maven
            for (GitHubRepository ghRepo : upstreamRepos) {
                buildMavenProject(new FilePath(upstreamReposDir, ghRepo.getName()), "/opt/tools/apache-maven-3.2.3", launcher, listener, envVars);
            }
        } catch (Exception ex) {
            listener.getLogger().println("Error while trying to clone needed upstream repositories! " + ex.getMessage());
            return false;
        }
        return true;
    }

    private GitHubRepositoryList loadRepositoryList(String branch) {
        // TODO make this work OOTB when new branch is added
        if ("master".equals(branch)) {
            return GitHubRepositoryList.fromClasspathResource(GitHubRepositoryList.KIE_REPO_LIST_MASTER_RESOURCE_PATH);
        } else if ("6.3.x".equals(branch)) {
            return GitHubRepositoryList.fromClasspathResource(GitHubRepositoryList.KIE_REPO_LIST_6_3_X_RESOURCE_PATH);
        } else if ("6.2.x".equals(branch)) {
            return GitHubRepositoryList.fromClasspathResource(GitHubRepositoryList.KIE_REPO_LIST_6_2_X_RESOURCE_PATH);
        } else {
            throw new IllegalArgumentException("Invalid PR target branch '" + branch + "'! Only master, 6.3.x and 6.2.x supported!");
        }
    }

    /**
     * Gather list of upstream repositories that needs to be build before the base repository (repository with the PR) as
     * they contain required changes.
     *
     * @param baseRepoName GitHub repository name that the PR was submitted against
     * @param prSrcBranch  source branch of the PR
     * @param prRepoOwner  owner of the repository with the source PR branch
     * @param github       GitHub API object used to talk to GitHub REST interface
     * @return list of upstream repositories that need to be build before the base repository
     */
    private List<GitHubRepository> gatherUpstreamReposToBuild(String baseRepoName, String prSrcBranch, String prRepoOwner, GitHubRepositoryList kieRepoList, GitHub github) {
        List<GitHubRepository> upstreamRepos = new ArrayList<GitHubRepository>();
        for (GitHubRepository kieRepo : kieRepoList.getList()) {
            String kieRepoName = kieRepo.getName();
            if (kieRepoName.equals(baseRepoName)) {
                // we encountered the base repo, so all upstream repos were already processed
                return upstreamRepos;
            }
            if (checkBranchExists(prRepoOwner + "/" + kieRepoName, prSrcBranch, github) &&
                    checkHasOpenPRAssociated(kieRepo.getOwner() + "/" + kieRepoName, prSrcBranch, prRepoOwner, github)) {
                upstreamRepos.add(new GitHubRepository(prRepoOwner, kieRepoName));
            }
        }
        return upstreamRepos;
    }

    /**
     * Checks whether GitHub repository has the specified branch.
     * <p>
     * Used to check if the fork has the same branch as repo with PR.
     *
     * @param fullRepoName full GitHub repository name (owner + name)
     * @param branch       branch to check
     * @param github       GitHub API object used to talk to GitHub REST interface
     * @return true if the branch exists, otherwise false
     */
    private boolean checkBranchExists(String fullRepoName, String branch, GitHub github) {
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
     * @param branch       branch used to submit the PR
     * @param prRepoOwner  owner of the repository that contain the PR branch
     * @param github       GitHub API object used to talk to GitHub REST interface
     * @return true if the specified repository contains open PR with the same branch and owner, otherwise false
     */
    private boolean checkHasOpenPRAssociated(String fullRepoName, String branch, String prRepoOwner, GitHub github) {
        try {
            List<GHPullRequest> prs = github.getRepository(fullRepoName).getPullRequests(GHIssueState.OPEN);
            for (GHPullRequest pr : prs) {
                // check if the PR source branch and name of the fork are the ones we are looking for
                if (pr.getHead().getRef().equals(branch) &&
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
     * @throws IOException, InterruptedException
     */
    private void ghCloneAndCheckout(GitHubRepository ghRepo, String branch, FilePath destDir, BuildListener buildListener) throws IOException, InterruptedException {
        destDir.mkdirs();
        GitClient git = Git.with(buildListener, new EnvVars())
                .in(destDir)
                .using("git")
                .getClient();
        git.clone("git://github.com/" + ghRepo.getOwner() + "/" + ghRepo.getName(), "origin", false, null);
        git.checkoutBranch(branch, "origin/" + branch);
    }

    /**
     * Builds Maven project from the specified working directory (contains pom.xml).
     *
     * @param workdir
     * @param mavenHome
     * @param launcher
     * @param listener
     * @param envVars
     */

    private void buildMavenProject(FilePath workdir, String mavenHome, Launcher launcher, BuildListener listener, EnvVars envVars) {
        int exitCode;
        try {
            Proc proc = launcher.launch()
                    // TODO make this (Maven home + command) configurable,  both on global and local level
                    .cmdAsSingleString(mavenHome + "/bin/mvn -B -e -T2C clean install -DskipTests -Dgwt.compiler.skip=true")
                    .envs(envVars)
                    .pwd(workdir)
                    .stdout(listener.getLogger())
                    .stderr(listener.getLogger())
                    .start();
            exitCode = proc.join();
        } catch (Exception e) {
            throw new RuntimeException("Error while executing Maven process!", e);
        }
        if (exitCode != 0) {
            throw new RuntimeException("Error while executing Maven process, non-zero exit code!");
        }
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * Descriptor for {@link UpstreamReposBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public DescriptorImpl() {
            load();
        }

        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         * <p>
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private String ghOAuthToken;

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Build dependent upstream repositories";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            ghOAuthToken = formData.getString("ghOAuthToken");
            save();
            return super.configure(req, formData);
        }

        /**
         * This method returns the configured OAuth token
         */
        public String getGhOAuthToken() {
            return ghOAuthToken;
        }
    }

}

