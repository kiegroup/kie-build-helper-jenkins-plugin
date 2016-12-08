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
import org.eclipse.jgit.transport.RefSpec;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Custom {@link Builder} which allows building downstream repositories during automated Jenkins PR builds.
 *
 * Building downstream repositories is usually needed when there are dependant PRs submitted into
 * different repositories. That way we can make sure all downstream repositories are still green, even after
 * applying the change in the repository under test.
 *
 * When the user configures the project and enables this builder,
 * {@link Descriptor#newInstance(StaplerRequest)} is invoked
 * and a new {@link KiePRBuildsHelper} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields
 * to remember the configuration.
 *
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked.
 *
 * What the builder does:
 * - collects info about the current repository and branch to test
 * - clones all needed downstream repositories
 * - builds gathered downstream repositories using Maven (full build with tests enabled)
 */
public class DownstreamReposBuilder extends Builder {

    private transient PrintStream buildLogger;

    private transient String prLink;
    private transient String prSourceBranch;
    private transient String prTargetBranch;

    /**
     *  Maven argument line can be configured at two different levels:
     *   1) per job, directly as part of the build step
     *   2) globally for all jobs (Manage Jenkins -> Configure System)
     *
     *  Per-job configuration has precedence over the global one.
     */
    private String mvnArgLine;

    @DataBoundConstructor
    public DownstreamReposBuilder(String mvnArgLine) {
        this.mvnArgLine = mvnArgLine;
    }

    public String getMvnArgLine() {
        return mvnArgLine;
    }

    /**
     * Initializes the fields from passed Environmental Variables
     *
     * @param envVars set of environment variables
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
            buildLogger.println("Downstream repositories builder for PR builds started.");
            EnvVars envVars = build.getEnvironment(launcher.getListener());
            initFromEnvVars(envVars);
            FilePath workspace = build.getWorkspace();

            GitHubRepositoryList kieRepoList = GitHubRepositoryList.forBranch(prTargetBranch);

            KiePRBuildsHelper.KiePRBuildsHelperDescriptor globalSettings = KiePRBuildsHelper.getKiePRBuildsHelperDescriptor();

            String ghOAuthToken = globalSettings.getGhOAuthToken();
            if (isEmpty(ghOAuthToken)) {
                buildLogger.println("No GitHub OAuth configured. Please set one on global Jenkins configuration page.");
                return false;
            }
            FilePath downstreamReposDir = new FilePath(workspace, "downstream-repos");
            // clean-up the destination directory to avoid stale content
            buildLogger.println("Cleaning-up directory " + downstreamReposDir.getRemote());
            downstreamReposDir.deleteRecursive();

            GitHub github = GitHub.connectUsingOAuth(ghOAuthToken);
            // get info about the PR from variables provided by GitHub Pull Request Builder plugin
            GitHubPRSummary prSummary = GitHubPRSummary.fromPRLink(prLink, github);

            String prRepoName = prSummary.getTargetRepoName();
            kieRepoList.filterOutUnnecessaryUpstreamRepos(prRepoName);

            Map<KieGitHubRepository, RefSpec> downstreamRepos =
                    gatherDownstreamReposToBuild(prRepoName, prSummary.getSourceBranch(), prTargetBranch, prSummary.getSourceRepoOwner(), kieRepoList, github);
            GitHubUtils.logRepositories(downstreamRepos, buildLogger);
            GitHubUtils.cloneRepositories(downstreamReposDir, downstreamRepos, GitHubUtils.GIT_REFERENCE_BASEDIR, listener);
            // build downstream repositories using Maven
            String mavenHome = globalSettings.getMavenHome();

            String mavenOpts = globalSettings.getMavenOpts();
            for (KieGitHubRepository ghRepo : downstreamRepos.keySet()) {
                MavenProject mavenProject = new MavenProject(new FilePath(downstreamReposDir,
                        ghRepo.getName()), mavenHome, mavenOpts, launcher, listener);
                mavenProject.build(mvnArgLine, envVars, buildLogger);
            }
        } catch (Exception ex) {
            buildLogger.println("Unexpected error while executing the DownstreamReposBuilder! " + ex.getMessage());
            ex.printStackTrace(buildLogger);
            return false;
        }
        buildLogger.println("Downstream repositories builder finished successfully.");
        return true;
    }

    private boolean isEmpty(String str) {
        return str == null || "".equals(str);
    }

    /**
     * Gather list of downstream repositories that needs to be build before the base repository (repository with the PR).
     *
     * @param baseRepoName  GitHub repository name that the PR was submitted against
     * @param prSourceBranch  source branch of the PR
     * @param baseRepoOwner owner of the repository with the source PR branch
     * @param github        GitHub API client used to talk to the GitHub REST interface
     * @return Map of downstream repositories (with git refspecs) that need to be build before the base repository
     */
    private Map<KieGitHubRepository, RefSpec> gatherDownstreamReposToBuild(String baseRepoName, String prSourceBranch,
                                                                          String prTargetBranch, String baseRepoOwner,
                                                                          GitHubRepositoryList kieRepoList, GitHub github) {
        Map<KieGitHubRepository, RefSpec> downstreamRepos = new LinkedHashMap<>();
        boolean baseRepoFound = false;
        for (KieGitHubRepository kieRepo : kieRepoList.getList()) {
            String kieRepoName = kieRepo.getName();
            if (kieRepoName.equals(baseRepoName)) {
                // we encountered the base repo, the repos after this one are the downstream ones
                baseRepoFound = true;
                continue;
            } else if (!baseRepoFound) {
                continue;
            }

            Optional<GitHubPRSummary> downstreamRepoPR = GitHubUtils.findOpenPullRequest(
                    new GitHubRepository(kieRepo.getOwner(), kieRepo.getName()), this.prSourceBranch, baseRepoOwner, github);
            // in case the PR is there it also needs to be mergeable, if not fail fast
            downstreamRepoPR.ifPresent(pr -> {
                if (!pr.isMergeable()) {
                    throw new RuntimeException("PR " + pr.getNumber() + " for repo " + pr.getTargetRepo() + " is " +
                            "not automatically mergeable. Please fix the conflicts first!");
                }
            });
            String baseBranch = kieRepo.determineBaseBranch(baseRepoName, prTargetBranch);
            RefSpec refspec = new RefSpec(downstreamRepoPR
                    .map(pr -> "pull/" + pr.getNumber() + "/merge:pr" + pr.getNumber() + "-" + prSourceBranch + "-merge")
                    .orElse(baseBranch + ":" + baseBranch + "-pr-build"));
            downstreamRepos.put(kieRepo, refspec);
        }
        return downstreamRepos;
    }

    @Override
    public DownstreamReposBuilder.Descriptor getDescriptor() {
        return (DownstreamReposBuilder.Descriptor) super.getDescriptor();
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
            return "Build dependent downstream repositories (for PR builds)";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
            save();
            return super.configure(req, formData);
        }

    }

}

