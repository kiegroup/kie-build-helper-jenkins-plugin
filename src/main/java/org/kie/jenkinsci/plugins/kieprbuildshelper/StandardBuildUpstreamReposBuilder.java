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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Upstream repos builder for standard (non PR) Jenkins builds. As opposed to {@link UpstreamReposBuilder} which is
 * intended for use with PR builds only.
 */
public class StandardBuildUpstreamReposBuilder extends Builder {

    private final String baseRepository;
    private final String branch;

    private transient PrintStream buildLogger;

    @DataBoundConstructor
    public StandardBuildUpstreamReposBuilder(String baseRepository, String branch) {
        this.baseRepository = baseRepository;
        this.branch = branch;
    }

    public String getBaseRepository() {
        return baseRepository;
    }

    public String getBranch() {
        return branch;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        try {
            buildLogger = listener.getLogger();
            buildLogger.printf("Upstream repositories builder for standard builds started (repository=%s, branch=%s).%n", baseRepository, branch);
            EnvVars envVars = build.getEnvironment(launcher.getListener());

            FilePath workspace = build.getWorkspace();

            GitHubRepositoryList kieRepoList = KieRepositoryLists.getListForBranch(baseRepository,
                                                                                   branch);
            FilePath upstreamReposDir = new FilePath(workspace, "upstream-repos");
            // clean-up the destination directory to avoid stale content
            buildLogger.println("Cleaning-up directory " + upstreamReposDir.getRemote());
            upstreamReposDir.deleteRecursive();

            kieRepoList.filterOutUnnecessaryUpstreamRepos(baseRepository);
            Map<KieGitHubRepository, RefSpec> upstreamRepos = gatherUpstreamReposToBuild(baseRepository, branch, kieRepoList);
            // clone upstream repositories
            GitHubUtils.logRepositories(upstreamRepos, buildLogger);
            GitHubUtils.cloneRepositories(upstreamReposDir, upstreamRepos, GitHubUtils.GIT_REFERENCE_BASEDIR, listener);
            KiePRBuildsHelper.KiePRBuildsHelperDescriptor globalSettings = KiePRBuildsHelper.getKiePRBuildsHelperDescriptor();
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
     * @param baseRepoName   GitHub repository name
     * @param baseRepoBranch branch name
     * @return Map of upstream repositories with refspecs that need to be build before the base repository
     */
    private Map<KieGitHubRepository, RefSpec> gatherUpstreamReposToBuild(String baseRepoName, String baseRepoBranch, GitHubRepositoryList kieRepoList) {
        Map<KieGitHubRepository, RefSpec> upstreamRepos = new LinkedHashMap<>();
        for (KieGitHubRepository kieRepo : kieRepoList.getList()) {
            String kieRepoName = kieRepo.getName();
            if (kieRepoName.equals(baseRepoName)) {
                // we encountered the base repo, so all upstream repos were already processed and we can return the result
                return upstreamRepos;
            }
            String branch = KieRepositoryLists.getBaseBranchFor(kieRepoName, baseRepoName, baseRepoBranch);
            upstreamRepos.put(kieRepo, new RefSpec(branch + ":" + branch + "-build"));
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
