package org.kie.jenkinsci.plugins.kieprbuildshelper;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom {@link Builder} which allows building upstream repositories during automated PR builds.
 * <p/>
 * Building upstream repositories is usually needed when there are dependant PRs submitted into
 * different repositories.
 * <p/>
 * When the user configures the project and enables this builder,
 * {@link Descriptor#newInstance(StaplerRequest)} is invoked
 * and a new {@link KiePRBuildsHelper} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields
 * to remember the configuration.
 * <p/>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked.
 * <p/>
 * What the builder does:
 * - collects info about the current PR and repository
 * - clones all needed upstream repos
 * - builds gathered upstream repositories using Maven
 */
public class UpstreamReposBuilder extends Builder {

    private PrintStream buildLogger;
    private String prLink;
    private String prSourceBranch;
    private String prTargetBranch;

    @DataBoundConstructor
    public UpstreamReposBuilder() {
    }

    /**
     * Initializes the fields from passed Environmental Variables
     */
    public void initFromEnvVars(EnvVars envVars) {
        prLink = envVars.get("ghprbPullLink");
        prSourceBranch = envVars.get("ghprbSourceBranch");
        prTargetBranch = envVars.get("ghprbTargetBranch");
        buildLogger.println("Working with PR: " + prLink);
        buildLogger.println("PR source branch: " + prSourceBranch);
        buildLogger.println("PR target branch: " + prTargetBranch);
        if (prLink == null || "".equals(prLink)) {
            throw new IllegalStateException("PR link not set! Make sure variable 'ghprbPullLink' contains valid link to GitHub Pull Request!");
        }
        if (prSourceBranch == null || "".equals(prSourceBranch)) {
            throw new IllegalStateException("PR source branch not set! Make sure variable 'ghprbSourceBranch' contains valid source branch for the configured GitHub Pull Request!");
        }
        if (prTargetBranch == null || "".equals(prTargetBranch)) {
            throw new IllegalStateException("PR target branch not set! Make sure variable 'ghprbTargetBranch' contains valid target branch for the configured GitHub Pull Request!");
        }
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        try {
            buildLogger = listener.getLogger();
            buildLogger.println("Upstream repositories builder for PR builds started.");
            EnvVars envVars = build.getEnvironment(launcher.getListener());
            initFromEnvVars(envVars);
            FilePath workspace = build.getWorkspace();

            GitHubRepositoryList kieRepoList = GitHubRepositoryList.forBranch(prTargetBranch);
            KiePRBuildsHelper.KiePRBuildsHelperDescriptor globalSettings = KiePRBuildsHelper.getKiePRBuildsHelperDescriptor();
            String ghOAuthToken = globalSettings.getGhOAuthToken();

            if (ghOAuthToken == null) {
                buildLogger.println("No GitHub OAuth token found. Please set one on global Jenkins configuration page.");
                return false;
            }
            FilePath upstreamReposDir = new FilePath(workspace, "upstream-repos");
            // clean-up the destination directory to avoid stale content
            buildLogger.println("Cleaning-up directory " + upstreamReposDir.getRemote());
            upstreamReposDir.deleteRecursive();

            GitHub github = GitHub.connectUsingOAuth(ghOAuthToken);
            // get info about the PR from variables provided by GitHub Pull Request Builder plugin
            GitHubPRSummary prSummary = GitHubPRSummary.fromPRLink(prLink, prSourceBranch, github);

            String prRepoName = prSummary.getTargetRepo();
            kieRepoList.filterOutUnnecessaryUpstreamRepos(prRepoName);
            Map<KieGitHubRepository, String> upstreamRepos =
                    gatherUpstreamReposToBuild(prRepoName, prSourceBranch, prTargetBranch, prSummary.getSourceRepoOwner(), kieRepoList, github);
            // clone upstream repositories
            logUpstreamRepos(upstreamRepos);
            for (Map.Entry<KieGitHubRepository, String> entry : upstreamRepos.entrySet()) {
                KieGitHubRepository ghRepo = entry.getKey();
                String branch = entry.getValue();
                FilePath repoDir = new FilePath(upstreamReposDir, ghRepo.getName());
                ghCloneAndCheckout(ghRepo, branch, repoDir, listener);
            }
            // build upstream repositories using Maven
            String mavenHome = globalSettings.getMavenHome();
            String mavenArgLine = globalSettings.getUpstreamBuildsMavenArgLine();
            String mavenOpts = globalSettings.getMavenOpts();
            if (!mavenArgLine.contains("-Dmaven.repo.local=")) {
                mavenArgLine = mavenArgLine + " -Dmaven.repo.local=" + new FilePath(workspace, ".repository").getRemote();
            }
            for (Map.Entry<KieGitHubRepository, String> entry : upstreamRepos.entrySet()) {
                KieGitHubRepository ghRepo = entry.getKey();
                MavenProject mavenProject = new MavenProject(new FilePath(upstreamReposDir,
                        ghRepo.getName()), mavenHome, mavenOpts, launcher, listener);
                mavenProject.build(mavenArgLine, envVars, buildLogger);
            }
        } catch (Exception ex) {
            buildLogger.println("Unexpected error while executing the UpstreamReposBuilder! " + ex.getMessage());
            ex.printStackTrace(buildLogger);
            return false;
        }
        buildLogger.println("Upstream repositories builder finished successfully.");
        return true;
    }

    private void logUpstreamRepos(Map<KieGitHubRepository, String> upstreamRepos) {
        if (upstreamRepos.size() > 0) {
            buildLogger.println("Upstream GitHub repositories that will be cloned and build:");
            for (Map.Entry<KieGitHubRepository, String> entry : upstreamRepos.entrySet()) {
                // print as '<URL>:<branch>'
                buildLogger.println("\t" + entry.getKey().getReadOnlyCloneURL() + ":" + entry.getValue());
            }
        } else {
            buildLogger.println("No required upstream GitHub repositories found.");
        }
    }

    /**
     * Gather list of upstream repositories that needs to be build before the base repository (repository with the PR).
     *
     * @param prRepoName     GitHub repository name that the PR was submitted against
     * @param prSourceBranch source branch of the PR
     * @param prRepoOwner    owner of the repository with the source PR branch
     * @param github         GitHub API object used to talk to GitHub REST interface
     * @return Map of upstream repositories (with specific branches) that need to be build before the base repository
     */
    private Map<KieGitHubRepository, String> gatherUpstreamReposToBuild(String prRepoName, String prSourceBranch, String prTargetBranch, String prRepoOwner, GitHubRepositoryList kieRepoList, GitHub github) {
        Map<KieGitHubRepository, String> upstreamRepos = new LinkedHashMap<KieGitHubRepository, String>();
        for (KieGitHubRepository kieRepo : kieRepoList.getList()) {
            String kieRepoName = kieRepo.getName();
            if (kieRepoName.equals(prRepoName)) {
                // we encountered the base repo, so all upstream repos were already processed and we can return the result
                return upstreamRepos;
            }
            if (checkBranchExists(prRepoOwner + "/" + kieRepoName, prSourceBranch, github) &&
                    checkHasOpenPRAssociated(kieRepo.getOwner() + "/" + kieRepoName, prSourceBranch, prRepoOwner, github)) {
                upstreamRepos.put(new KieGitHubRepository(prRepoOwner, kieRepoName), prSourceBranch);
            } else {
                // otherwise just use the current target branch for that repo (we will build the most up to date sources)
                // this gets a little tricky as we need figure out which uberfire branch matches the target branches for KIE
                // e.g. for KIE 6.3.x branch we need UF 0.7.x and Dashuilder 0.3.x branches
                String baseBranch = kieRepo.determineBaseBranch(prRepoName, prTargetBranch);
                upstreamRepos.put(kieRepo, baseBranch);
            }
        }
        return upstreamRepos;
    }

    /**
     * Checks whether GitHub repository has specific branch.
     * <p/>
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
     * @param prBranch     branch that was used to submit the PR
     * @param prRepoOwner  owner of the repository that contain the PR branch
     * @param github       GitHub API object used to talk to GitHub REST interface
     * @return true if the specified repository contains open PR with the same branch and owner, otherwise false
     */
    private boolean checkHasOpenPRAssociated(String fullRepoName, String prBranch, String prRepoOwner, GitHub github) {
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
     * @throws IOException, InterruptedException
     */
    private void ghCloneAndCheckout(KieGitHubRepository ghRepo, String branch, FilePath destDir, BuildListener buildListener) throws IOException, InterruptedException {
        destDir.mkdirs();
        GitClient git = Git.with(buildListener, new EnvVars())
                .in(destDir)
                .using("git")
                .getClient();
        git.clone(ghRepo.getReadOnlyCloneURL(), "origin", true, null);
        git.checkoutBranch(branch, "origin/" + branch);
    }



    @Override
    public Descriptor getDescriptor() {
        return (Descriptor) super.getDescriptor();
    }

    /**
     * Descriptor for {@link KiePRBuildsHelper}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class Descriptor extends BuildStepDescriptor<Builder> {

        public Descriptor() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Build dependent upstream repositories (for PR builds)";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
            save();
            return super.configure(req, formData);
        }

    }

}

