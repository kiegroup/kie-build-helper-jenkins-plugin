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
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Custom {@link Builder} which allows building downstream repositories during automated Jenkins builds.
 * <p/>
 * Building downstream repositories is usually needed when there are dependant PRs submitted into
 * different repositories. That way we can make sure all downstream repositories are still green, even after
 * applying the change in the repository under test.
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
 * - collects info about the current repository and branch to test
 * - clones all needed downstream repositories
 * - builds gathered downstream repositories using Maven (full build with tests enabled)
 */
public class DownstreamReposBuilder extends Builder {

    private final KiePRBuildsHelper.KiePRBuildsHelperDescriptor globalSettings;

    private PrintStream buildLogger;

    private String baseRepoName;
    private String baseRepoOwner;
    private String sourceBranch;
    private String targetBranch;

    private String mavenArgLine;

    /**
     * Flag that indicates whether the surrounding build is a Pull Request (PR) build or not.
     * PR build slightly changes the behavior when looking for repositories and branches. The repo+branch
     * also needs to have open PR associated with to be considered. If the build is not for PR, it is enough
     * if the branch just exists (no need to have open PR associated with it).
     **/
    private boolean isPRBuild;

    @DataBoundConstructor
    public DownstreamReposBuilder() {
        this.globalSettings = KiePRBuildsHelper.getKiePRBuildsHelperDescriptor();
    }

    public DownstreamReposBuilder(PrintStream buildLogger, KiePRBuildsHelper.KiePRBuildsHelperDescriptor globalSettings) {
        this.buildLogger = buildLogger;
        this.globalSettings = globalSettings;
    }

    public String getBaseRepoName() {
        return baseRepoName;
    }

    public String getBaseRepoOwner() {
        return baseRepoOwner;
    }

    public String getSourceBranch() {
        return sourceBranch;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public String getMavenArgLine() {
        return mavenArgLine;
    }

    /**
     * Initializes the fields from passed Environmental Variables
     */
    public void initFromEnvVars(EnvVars envVars) {
        sourceBranch = envVars.get("sourceBranch");
        if (isEmpty(sourceBranch)) {
            sourceBranch = envVars.get("ghprbSourceBranch");
        }
        targetBranch = envVars.get("targetBranch");
        if (isEmpty(targetBranch)) {
            targetBranch = envVars.get("ghprbTargetBranch");
        }
        String baseGhRepo = envVars.get("baseRepo");
        if (isEmpty(baseGhRepo)) {
            baseGhRepo = envVars.get("ghprbGhRepository");
        }
        mavenArgLine = envVars.get("downstreamBuildMavenArgLine");
        if (isEmpty(mavenArgLine)) {
            mavenArgLine = globalSettings.getDownstreambuildsMavenArgLine();
        }

        isPRBuild = !isEmpty(envVars.get("ghprbPullLink"));

        if (isEmpty(baseGhRepo)) {
            throw new IllegalStateException("Base repository not set! Make sure variable 'baseRepo' contains valid " +
                    "repository owner and repo name (in format <owner>/<repo-name>!");
        }
        if (isEmpty(sourceBranch)) {
            throw new IllegalStateException("Source branch not set! Make sure variable 'sourceBranch' contains valid " +
                    "source branch for the configured GitHub Repository!");
        }
        if (isEmpty(targetBranch)) {
            throw new IllegalStateException("Target branch not set! Make sure variable 'targetBranch' contains valid " +
                    "target branch for the configured GitHub repository!");
        }
        if (isEmpty(mavenArgLine)) {
            throw new IllegalStateException("Maven arg line for downstream build not set! Make sure variable 'downstreamBuildMavenArgLine' " +
                    "contains valid Maven argument line (e.g. '-e -B clean install'). You can also set the arg line" +
                    "globally, on Jenkins configuration page.");
        }
        String[] parts = baseGhRepo.split("/");
        baseRepoOwner = parts[0];
        baseRepoName = parts[1];

        buildLogger.println("Working with repository: https://github.com/" + baseRepoOwner + "/" + baseRepoName);
        buildLogger.println("Source branch: " + sourceBranch);
        buildLogger.println("Target branch: " + targetBranch);
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        try {
            buildLogger = listener.getLogger();
            buildLogger.println("Downstream repositories builder started.");
            EnvVars envVars = build.getEnvironment(launcher.getListener());
            initFromEnvVars(envVars);
            FilePath workspace = build.getWorkspace();

            GitHubRepositoryList kieRepoList = GitHubRepositoryList.forBranch(targetBranch);

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

            Map<KieGitHubRepository, String> downstreamRepos =
                    gatherDownstreamReposToBuild(baseRepoName, sourceBranch, targetBranch, baseRepoOwner, kieRepoList, github);
            GitHubUtils.logRepositories(downstreamRepos, buildLogger);
            GitHubUtils.cloneRepositories(downstreamReposDir, downstreamRepos, listener);
            // build downstream repositories using Maven
            String mavenHome = globalSettings.getMavenHome();

            String mavenOpts = globalSettings.getMavenOpts();
            if (!mavenArgLine.contains("-Dmaven.repo.local=")) {
                mavenArgLine = mavenArgLine + " -Dmaven.repo.local=" + new FilePath(workspace, ".repository").getRemote();
            }
            for (Map.Entry<KieGitHubRepository, String> entry : downstreamRepos.entrySet()) {
                KieGitHubRepository ghRepo = entry.getKey();
                MavenProject mavenProject = new MavenProject(new FilePath(downstreamReposDir,
                        ghRepo.getName()), mavenHome, mavenOpts, launcher, listener);
                mavenProject.build(mavenArgLine, envVars, buildLogger);
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
     * @param baseRepoName     GitHub repository name that the PR was submitted against
     * @param sourceBranch source branch of the PR
     * @param baseRepoOwner    owner of the repository with the source PR branch
     * @param github         GitHub API object used to talk to GitHub REST interface
     * @return Map of downstream repositories (with specific branches) that need to be build before the base repository
     */
    private Map<KieGitHubRepository, String> gatherDownstreamReposToBuild(String baseRepoName, String sourceBranch,
                                                                          String targetBranch, String baseRepoOwner,
                                                                          GitHubRepositoryList kieRepoList, GitHub github) {
        Map<KieGitHubRepository, String> downstreamRepos = new LinkedHashMap<KieGitHubRepository, String>();
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

            boolean branchExists = GitHubUtils.checkBranchExists(baseRepoOwner + "/" + kieRepoName, sourceBranch, github);
            boolean hasOpenPRAssociated = GitHubUtils.checkHasOpenPRAssociated(kieRepo.getOwner() + "/" + kieRepoName, sourceBranch, baseRepoOwner, github);
            if (isPRBuild && branchExists && hasOpenPRAssociated) {
                downstreamRepos.put(new KieGitHubRepository(baseRepoOwner, kieRepoName), sourceBranch);
            } else if (!isPRBuild && branchExists) {
                downstreamRepos.put(new KieGitHubRepository(baseRepoOwner, kieRepoName), sourceBranch);
            } else {
                // otherwise just use the current target branch for that repo (we will build the most up to date sources)
                // this gets a little tricky as we need figure out which uberfire branch matches the target branches for KIE
                // e.g. for KIE 6.3.x branch we need UF 0.7.x and Dashuilder 0.3.x branches
                String baseBranch = kieRepo.determineBaseBranch(baseRepoName, targetBranch);
                downstreamRepos.put(kieRepo, baseBranch);
            }
        }
        return downstreamRepos;
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
            return "Build dependent downstream repositories";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
            save();
            return super.configure(req, formData);
        }

    }

}

