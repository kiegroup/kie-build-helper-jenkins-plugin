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
public class UpstreamReposBuilder extends AbstractPRBuilder {

    @DataBoundConstructor
    public UpstreamReposBuilder(String mvnHome, String mvnOpts, String mvnArgs) {
        super(mvnHome, mvnOpts, mvnArgs);
    }

    @Override
    protected String getDescription() {
        return "Upstream repositories builder for PR builds";
    }

    @Override
    protected FilePath getBuildDir(FilePath workspace) {
        return new FilePath(workspace, "upstream-repos");
    }

    /**
     * Get list of upstream repositories that need to be build before the base repository (repository with the PR).
     *
     * @param prRepo GitHub repository that the PR was submitted against
     * @param allRepos list of all repositories for the specific build chain
     * @return list of upstream repositories that need to be build before the base repository
     */
    @Override
    protected List<Tuple<GitHubRepository, GitBranch>> getReposToBuild(GitHubRepository prRepo, List<Tuple<GitHubRepository, GitBranch>> allRepos) {
        List<Tuple<GitHubRepository, GitBranch>> result = new ArrayList<>();
        for (Tuple<GitHubRepository, GitBranch> repoWithBranch : allRepos) {
            GitHubRepository repo = repoWithBranch._1();
            if (repo.equals(prRepo)) {
                // we encountered the PR repo, so all upstream repos were already processed and we can return the result
                return result;
            }
            result.add(repoWithBranch);
        }
        throw new IllegalStateException("PR repository not found in the list of all repositories!");
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

