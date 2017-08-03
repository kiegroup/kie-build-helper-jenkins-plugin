package org.kie.jenkinsci.plugins.kieprbuildshelper;

import java.util.ArrayList;
import java.util.List;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

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
public class DownstreamReposBuilder extends AbstractPRBuilder {

    @DataBoundConstructor
    public DownstreamReposBuilder(String mvnHome, String mvnOpts, String mvnArgs) {
        super(mvnHome, mvnOpts, mvnArgs);
    }

    @Override
    protected String getDescription() {
        return "Downstream repositories builder for PR builds";
    }

    @Override
    protected FilePath getBuildDir(FilePath workspace) {
        return new FilePath(workspace, "downstream-repos");
    }

    /**
     * Get list of downstream repositories that needs to be build before the base repository (repository with the PR).
     *
     * @param prRepo GitHub repository that the PR was submitted against
     * @param allRepos list of all repositories for the specific branch
     * @return list of downstream repositories that need to be build after the base repository
     */
    @Override
    protected List<Tuple<GitHubRepository, GitBranch>> getReposToBuild(GitHubRepository prRepo, List<Tuple<GitHubRepository, GitBranch>> allRepos) {
        List<Tuple<GitHubRepository, GitBranch>> downstreamRepos = new ArrayList<>();
        boolean prRepoFound = false;
        for (Tuple<GitHubRepository, GitBranch> repoWithBranch : RepositoryLists.filterOutUnnecessaryRepos(allRepos, prRepo)) {
            GitHubRepository repo = repoWithBranch._1();
            if (prRepoFound) {
                downstreamRepos.add(repoWithBranch);
            }
            if (repo.equals(prRepo)) {
                prRepoFound = true;
            }
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

