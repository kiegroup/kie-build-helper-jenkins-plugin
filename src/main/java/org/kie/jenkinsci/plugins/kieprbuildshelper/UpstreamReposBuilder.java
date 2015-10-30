package org.kie.jenkinsci.plugins.kieprbuildshelper;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom {@link Builder} which allows building upstream repositories during automated PR builds.
 * <p>
 * Building upstream repositories is usually needed when there are dependant PRs submitted into
 * different repositories.
 * <p>
 * <p>
 * When the user configures the project and enables this builder,
 * {@link Descriptor#newInstance(StaplerRequest)} is invoked
 * and a new {@link KiePRBuildsHelper} is created. The created
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

    private PrintStream buildLogger;
    private String prLink;
    private String prSourceBranch;
    private String prTargetBranch;

    @DataBoundConstructor
    public UpstreamReposBuilder() {
    }

    /**
     * Initializes the fields from passed Environmental Variables and BuildListener
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
            buildLogger.println("Upstream repositories builder started.");
            EnvVars envVars = build.getEnvironment(launcher.getListener());
            initFromEnvVars(envVars);
            FilePath workspace = build.getWorkspace();

            GitHubRepositoryList kieRepoList = loadRepositoryList(prTargetBranch);
            String ghOAuthToken = KiePRBuildsHelper.getKiePRBuildsHelperDescriptor().getGhOAuthToken();

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
            filterOutUnnecessaryUpstreamRepos(prRepoName, kieRepoList.getList());
            Map<GitHubRepository, String> upstreamRepos = gatherUpstreamReposToBuild(prRepoName, prSourceBranch, prTargetBranch, prSummary.getSourceRepoOwner(), kieRepoList, github);
            // clone upstream repositories
            logUpstreamRepos(upstreamRepos);
            for (Map.Entry<GitHubRepository, String> entry : upstreamRepos.entrySet()) {
                GitHubRepository ghRepo = entry.getKey();
                String branch = entry.getValue();
                FilePath repoDir = new FilePath(upstreamReposDir, ghRepo.getName());
                ghCloneAndCheckout(ghRepo, branch, repoDir, listener);
            }
            // build upstream repositories using Maven
            for (Map.Entry<GitHubRepository, String> entry : upstreamRepos.entrySet()) {
                GitHubRepository ghRepo = entry.getKey();
                buildMavenProject(new FilePath(upstreamReposDir, ghRepo.getName()), "/opt/tools/apache-maven-3.2.3", workspace, launcher, listener, envVars);
            }
            buildLogger.println("Upstream repositories builder finished successfully.");
        } catch (Exception ex) {
            listener.getLogger().println("Unexpected error while executing the UpstreamReposBuilder! " + ex.getMessage());
            buildLogger.println("Upstream repositories builder finished with error!");
            return false;
        }
        return true;
    }

    private void filterOutUnnecessaryUpstreamRepos(String prRepoName, List<GitHubRepository> repoList) {
        if (Arrays.asList("droolsjbpm-knowledge", "drools", "optaplanner", "jbpm", "droolsjbpm-integration", "droolsjbpm-tools").contains(prRepoName)) {
            repoList.remove(new GitHubRepository("uberfire", "uberfire"));
            repoList.remove(new GitHubRepository("uberfire", "uberfire-extensions"));
            repoList.remove(new GitHubRepository("dashbuilder", "dashbuilder"));
        }
        // nothing depends on stuff from -tools repo
        repoList.remove(new GitHubRepository("droolsjbpm", "droolsjbpm-tools"));
        // nothing really depends on optaplanner (maybe the optaplanner-wb but that's still not actively developed)
        repoList.remove(new GitHubRepository("droolsjbpm", "optaplanner"));
        // no need to build docs, they are pretty much standalone
        repoList.remove(new GitHubRepository("droolsjbpm", "kie-docs"));


    }

    private void logUpstreamRepos(Map<GitHubRepository, String> upstreamRepos) {
        if (upstreamRepos.size() > 0) {
            buildLogger.println("Upstream GitHub repositories that will be cloned and build:");
            for (Map.Entry<GitHubRepository, String> entry : upstreamRepos.entrySet()) {
                // print as <URL>:<branch>
                buildLogger.println("\t" + entry.getKey().getReadOnlyCloneURL() + ":" + entry.getValue());
            }
        } else {
            buildLogger.println("No required upstream GitHub repositories found. This means the PR is not dependant on any upstream PR.");
        }
    }

    private GitHubRepositoryList loadRepositoryList(String branch) {
        // TODO make this work OOTB when new branch is added
        if ("master".equals(branch)) {
//            return GitHubRepositoryList.fromClasspathResource(GitHubRepositoryList.KIE_REPO_LIST_MASTER_RESOURCE_PATH);
            return KIERepositoryLists.getListForMasterBranch();
        } else if ("6.3.x".equals(branch)) {
//            return GitHubRepositoryList.fromClasspathResource(GitHubRepositoryList.KIE_REPO_LIST_6_3_X_RESOURCE_PATH);
            return KIERepositoryLists.getListFor63xBranch();
        } else if ("6.2.x".equals(branch)) {
//            return GitHubRepositoryList.fromClasspathResource(GitHubRepositoryList.KIE_REPO_LIST_6_2_X_RESOURCE_PATH);
            return KIERepositoryLists.getListFor62xBranch();
        } else {
            throw new IllegalArgumentException("Invalid PR target branch '" + branch + "'! Only master, 6.3.x and 6.2.x supported!");
        }
    }

    /**
     * Gather list of upstream repositories that needs to be build before the base repository (repository with the PR).
     *
     * @param prRepoName   GitHub repository name that the PR was submitted against
     * @param prSourceBranch source branch of the PR
     * @param prRepoOwner    owner of the repository with the source PR branch
     * @param github         GitHub API object used to talk to GitHub REST interface
     * @return Map of upstream repositories (with specific branches) that need to be build before the base repository
     */
    private Map<GitHubRepository, String> gatherUpstreamReposToBuild(String prRepoName, String prSourceBranch, String prTargetBranch, String prRepoOwner, GitHubRepositoryList kieRepoList, GitHub github) {
        Map<GitHubRepository, String> upstreamRepos = new LinkedHashMap<GitHubRepository, String>();
        for (GitHubRepository kieRepo : kieRepoList.getList()) {
            String kieRepoName = kieRepo.getName();
            if (kieRepoName.equals(prRepoName)) {
                // we encountered the base repo, so all upstream repos were already processed and we can return the result
                return upstreamRepos;
            }
            if (checkBranchExists(prRepoOwner + "/" + kieRepoName, prSourceBranch, github) &&
                    checkHasOpenPRAssociated(kieRepo.getOwner() + "/" + kieRepoName, prSourceBranch, prRepoOwner, github)) {
                upstreamRepos.put(new GitHubRepository(prRepoOwner, kieRepoName), prSourceBranch);
            } else {
                // otherwise just use the current target branch for that repo (we will build the most up to date sources)
                // this gets a little tricky as we need figure out which uberfire branch matches the target branches for KIE
                // e.g. for KIE 6.3.x branch we need UF 0.7.x and Dashuilder 0.3.x branches
                String baseBranch = determineBaseBranch(kieRepo, prRepoName , prTargetBranch);
                upstreamRepos.put(kieRepo, baseBranch);
            }
        }
        return upstreamRepos;
    }

    /**
     * Determines base branch for specified repository. This is used to clone repositories which do not directly
     * contain any specific changes. Building those base branches will decrease the number of false negatives caused
     * by stale snapshots, as we will always have the latest sources available.
     *
     * @param ghRepo         GitHub repository we want to get the branch for
     * @param prRepoName     repository name against which the PR was submitted
     * @param prTargetBranch target branch specified in the PR
     * @return name of the base branch which matches the target PR branch
     */
    private String determineBaseBranch(GitHubRepository ghRepo, String prRepoName, String prTargetBranch) {
        // all UF repositories share the branch names, so if the PR is against UF repo, the branch will always
        // be the same as the PR target branch
        if (prRepoName.startsWith("uberfire")) {
            return prTargetBranch;
        } else if (prRepoName.equals("dashbuilder")) {
            if ("master".equals(prTargetBranch)) {
                return "master";
            } else if ("0.7.x".equals(prTargetBranch)) {
                return "0.3.x";
            } else if ("0.5.x".equals(prTargetBranch)) {
                return "0.2.x";
            } else {
                throw new IllegalArgumentException("Invalid PR target branch for repo '" + prRepoName + "': " + prTargetBranch);
            }
        } else {
            // assume the repo is one of the core KIE repos (starting from droolsjbpm-build-bootstrap)
            if ("master".equals(prTargetBranch)) {
                return "master";
            } else if ("6.3.x".equals(prTargetBranch)) {
                if (isUberFireRepo(ghRepo)) {
                    return "0.7.x";
                } else if (isDashbuilderRepo(ghRepo)) {
                    return "0.3.x";
                } else {
                    return "6.3.x";
                }
            } else if ("6.2.x".equals(prTargetBranch)) {
                if (isUberFireRepo(ghRepo)) {
                    return "0.5.x";
                } else if (isDashbuilderRepo(ghRepo)) {
                    return "0.2.x";
                } else {
                    return "6.2.x";
                }
            } else {
                throw new IllegalArgumentException("Invalid PR target branch for repo '" + prRepoName + "': " + prTargetBranch);
            }
        }
    }

    private boolean isUberFireRepo(GitHubRepository repo) {
        return repo.getName().startsWith("uberfire");
    }

    private boolean isDashbuilderRepo(GitHubRepository repo) {
        return repo.getName().startsWith("dashbuilder");
    }

    /**
     * Checks whether GitHub repository has specific branch.
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
    private void ghCloneAndCheckout(GitHubRepository ghRepo, String branch, FilePath destDir, BuildListener buildListener) throws IOException, InterruptedException {
        destDir.mkdirs();
        GitClient git = Git.with(buildListener, new EnvVars())
                .in(destDir)
                .using("git")
                .getClient();
        git.clone(ghRepo.getReadOnlyCloneURL(), "origin", true, null);
        git.checkoutBranch(branch, "origin/" + branch);
    }

    /**
     * Builds Maven project from the specified working directory (contains pom.xml).
     *
     * @param projectWorkdir
     * @param mavenHome
     * @param launcher
     * @param listener
     * @param envVars
     */
    private void buildMavenProject(FilePath projectWorkdir, String mavenHome, FilePath jobWorkspace, Launcher launcher, BuildListener listener, EnvVars envVars) {
        int exitCode;
        String localMavenRepoPath = new FilePath(jobWorkspace, ".repository").getRemote();
        try {
            Proc proc = launcher.launch()
                    // TODO make this (Maven home + command) configurable, both on global and job level
                    .cmdAsSingleString(mavenHome + "/bin/mvn -B -e -T2C clean install -DskipTests -Dgwt.compiler.skip=true -Dmaven.repo.local=" + localMavenRepoPath)
                    .envs(envVars)
                    .pwd(projectWorkdir)
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
            return "Build dependent upstream repositories";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
            save();
            return super.configure(req,formData);
        }

    }

}

