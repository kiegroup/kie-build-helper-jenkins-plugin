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
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Upstream repos builder for standard (non PR) Jenkins builds. As opposed to {@link UpstreamReposBuilder} which is
 * intended for use with PR builds only.
 */
public class StandardBuildUpstreamReposBuilder extends Builder {

    private final String baseRepository;
    private final String branch;
    private final MavenBuildConfig mvnBuildConfig;

    private transient PrintStream buildLogger;

    @DataBoundConstructor
    public StandardBuildUpstreamReposBuilder(String baseRepository, String branch, String mvnHome, String mvnOpts, String mvnArgs) {
        this.baseRepository = baseRepository;
        this.branch = branch;
        this.mvnBuildConfig = new MavenBuildConfig(mvnHome, mvnOpts, mvnArgs);
    }

    public String getBaseRepository() {
        return baseRepository;
    }

    public String getBranch() {
        return branch;
    }

    public String getMvnHome() {
        return mvnBuildConfig.getMvnHome();
    }

    public String getMvnOpts() {
        return mvnBuildConfig.getMvnOpts();
    }

    public String getMvnArgs() {
        return mvnBuildConfig.getMvnArgs();
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        try {
            buildLogger = listener.getLogger();
            buildLogger.printf("Upstream repositories builder for standard builds started (repository=%s, branch=%s).%n", baseRepository, branch);
            EnvVars envVars = build.getEnvironment(launcher.getListener());

            FilePath workspace = build.getWorkspace();
            FilePath upstreamReposDir = new FilePath(workspace, "upstream-repos");
            // clean-up the destination directory to avoid stale content
            buildLogger.println("Cleaning-up directory " + upstreamReposDir.getRemote());
            upstreamReposDir.deleteRecursive();

            List<Tuple<GitHubRepository, GitBranch>> allRepos = RepositoryLists.createFor(GitHubRepository.from(baseRepository), new GitBranch(branch));
            List<Tuple<GitHubRepository, GitBranch>> filteredRepos =
                    RepositoryLists.filterOutUnnecessaryRepos(allRepos, GitHubRepository.from(baseRepository));
            List<Tuple<GitHubRepository, RefSpec>> upstreamRepos = gatherUpstreamReposToBuild(GitHubRepository.from(baseRepository), filteredRepos);

            GitHubUtils.logRepositories(upstreamRepos, buildLogger);
            // clone upstream repositories
            GitHubUtils.cloneRepositories(upstreamReposDir, upstreamRepos, GitHubUtils.GIT_REFERENCE_BASEDIR, listener);

            // build upstream repositories using Maven
            for (GitHubRepository repo : upstreamRepos.stream().map(Tuple::_1).collect(Collectors.toList())) {
                MavenProject mavenProject = new MavenProject(new FilePath(upstreamReposDir,
                        repo.getName()), mvnBuildConfig.getMvnHome(), mvnBuildConfig.getMvnOpts(), launcher, listener);
                mavenProject.build(mvnBuildConfig.getMvnArgs(), envVars, buildLogger);
            }
        } catch (Exception ex) {
            buildLogger.println("Unexpected error while executing the StandardBuildsUpstreamReposBuilder! " + ex.getMessage());
            ex.printStackTrace(buildLogger);
            return false;
        }

        buildLogger.println("Upstream repositories builder finished successfully.");
        return true;
    }

    /**
     * Gather list of upstream repositories that needs to be build before the base repository.
     *
     * @param baseRepo base GitHub repository
     * @return List of upstream repositories with refspecs that need to be build before the base repository
     */
    private List<Tuple<GitHubRepository, RefSpec>> gatherUpstreamReposToBuild(GitHubRepository baseRepo,
                                                                              List<Tuple<GitHubRepository, GitBranch>> allRepos) {
        List<Tuple<GitHubRepository, RefSpec>> upstreamRepos = new ArrayList<>();
        for (Tuple<GitHubRepository, GitBranch> repoWithBranch : allRepos) {
            GitHubRepository repo = repoWithBranch._1();
            if (repo.equals(baseRepo)) {
                // we encountered the base repo, so all upstream repos were already processed and we can return the result
                return upstreamRepos;
            }
            GitBranch branch = repoWithBranch._2();
            upstreamRepos.add(Tuple.of(repo, new RefSpec(branch.getName() + ":" + branch.getName() + "-build")));
        }
        return upstreamRepos;
    }

    @Override
    public StandardBuildUpstreamReposBuilder.Descriptor getDescriptor() {
        return (StandardBuildUpstreamReposBuilder.Descriptor) super.getDescriptor();
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
            return "Build required upstream repositories (for standard builds)";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
            save();
            return super.configure(req, formData);
        }

    }

}
