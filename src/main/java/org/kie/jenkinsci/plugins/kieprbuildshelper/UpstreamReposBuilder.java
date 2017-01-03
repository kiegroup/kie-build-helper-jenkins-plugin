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
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Custom {@link Builder} which allows building upstream repositories during automated PR builds.
 *
 * Building upstream repositories is needed to get the most up-to-date upstream artifacts. Relying on SNAPSHOTs in remote
 * repositories is error prone.
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
 * - collects info about the current PR and repository
 * - clones all needed upstream repos
 * - builds gathered upstream repositories using Maven
 */
public class UpstreamReposBuilder extends Builder {

    private transient PrintStream buildLogger;


    private transient String prLink;
    private transient String prSourceBranch;
    private transient String prTargetBranch;

    @DataBoundConstructor
    public UpstreamReposBuilder() {
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
            GitHubPRSummary prSummary = GitHubPRSummary.fromPRLink(prLink, github);

            String prRepoName = prSummary.getTargetRepoName();
            kieRepoList.filterOutUnnecessaryUpstreamRepos(prRepoName);
            Map<KieGitHubRepository, RefSpec> upstreamRepos =
                    gatherUpstreamReposToBuild(prRepoName, prSourceBranch, prTargetBranch, prSummary.getSourceRepoOwner(), kieRepoList, github);
            // clone upstream repositories
            GitHubUtils.logRepositories(upstreamRepos, buildLogger);
            GitHubUtils.cloneRepositories(upstreamReposDir, upstreamRepos, GitHubUtils.GIT_REFERENCE_BASEDIR, listener);
            // build upstream repositories using Maven
            String mavenHome = globalSettings.getMavenHome();
            String mavenArgLine = globalSettings.getUpstreamBuildsMavenArgLine();
            String mavenOpts = globalSettings.getMavenOpts();
            for (KieGitHubRepository ghRepo : upstreamRepos.keySet()) {
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

    /**
     * Gather list of upstream repositories that needs to be build before the base repository (repository with the PR).
     *
     * @param prRepoName     GitHub repository name that the PR was submitted against
     * @param prSourceBranch source branch of the PR
     * @param prRepoOwner    owner of the repository with the source PR branch
     * @param github         GitHub API object used to talk to GitHub REST interface
     * @return Map of upstream repositories with git refspecs that need to be build before the base repository
     */
    private Map<KieGitHubRepository, RefSpec> gatherUpstreamReposToBuild(String prRepoName, String prSourceBranch, String prTargetBranch, String prRepoOwner, GitHubRepositoryList kieRepoList, GitHub github) {
        Map<KieGitHubRepository, RefSpec> upstreamRepos = new LinkedHashMap<>();
        for (KieGitHubRepository kieRepo : kieRepoList.getList()) {
            String kieRepoName = kieRepo.getName();
            if (kieRepoName.equals(prRepoName)) {
                // we encountered the base repo, so all upstream repos were already processed and we can return the result
                return upstreamRepos;
            }
            Optional<GitHubPRSummary> upstreamRepoPR = GitHubUtils.findOpenPullRequest(
                    new GitHubRepository(kieRepo.getOwner(), kieRepo.getName()), prSourceBranch, prRepoOwner, github);
            // in case the PR is there it also needs to be mergeable, if not fail fast
            upstreamRepoPR.ifPresent(pr -> {
                if (!pr.isMergeable()) {
                    throw new RuntimeException("PR " + pr.getNumber() + " for repo " + pr.getTargetRepo() + " is " +
                            "not automatically mergeable. Please fix the conflicts first!");
                }
            });
            String baseBranch = kieRepo.determineBaseBranch(prRepoName, prTargetBranch);
            RefSpec refspec = new RefSpec(upstreamRepoPR
                    .map(pr -> "pull/" + pr.getNumber() + "/merge:pr" + pr.getNumber() + "-" + prSourceBranch + "-merge")
                    .orElse(baseBranch + ":" + baseBranch + "-pr-build"));
            upstreamRepos.put(kieRepo, refspec);
        }
        return upstreamRepos;
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
            return "Build required upstream repositories (for PR builds)";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
            save();
            return super.configure(req, formData);
        }

    }

}

