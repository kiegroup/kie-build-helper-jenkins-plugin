/*
 * Copyright 2015 JBoss by Red Hat
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

import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Custom {@link Builder} which downloads and unpacks Maven repo cache from specified URL.
 * <p>
 * This is needed for multi-repo builds as we can't install the artifacts directly into default local
 * repo (~/.m2). Downloading everything again (starting with empty local repository) for every build would take too
 * much time, the repo cache is build once and it can be reused in all the PR jobs.
 */
public class MavenRepoCacheBuilder extends Builder {

    private PrintStream buildLogger;

    @DataBoundConstructor
    public MavenRepoCacheBuilder() {
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        buildLogger = listener.getLogger();
        String mavenRepoCacheTgzUrl = KiePRBuildsHelper.getKiePRBuildsHelperDescriptor().getMavenRepoCacheTgzUrl();
        buildLogger.println("Using Maven repo cache builder.");
        if (mavenRepoCacheTgzUrl == null || "".equals(mavenRepoCacheTgzUrl)) {
            buildLogger.println("Error! No URL for Maven repo cache was specified. Configure one in global Jenkins configuration.");
            return false;
        }
        URL repoCacheUrl;
        try {
            repoCacheUrl = new URL(mavenRepoCacheTgzUrl);
        } catch (MalformedURLException e) {
            buildLogger.println("Malformed URL '" + mavenRepoCacheTgzUrl + "'! " + e.getMessage());
            throw new RuntimeException("Malformed URL '" + mavenRepoCacheTgzUrl + "' specified!", e);
        }
        FilePath workspace = build.getWorkspace();
        FilePath localMavenRepoFile = new FilePath(workspace, "maven-repo-cache.tar.gz");
        FilePath localMavenRepoDir = new FilePath(workspace, ".repository");

        try {
            buildLogger.println("Downloading file " + mavenRepoCacheTgzUrl);
            localMavenRepoFile.copyFrom(repoCacheUrl);
            buildLogger.println("Unpacking " + localMavenRepoFile + " into " + localMavenRepoDir);
            localMavenRepoFile.untar(localMavenRepoDir, FilePath.TarCompression.GZIP);
        } catch (Exception e) {
            buildLogger.println("Error while downloading/unpacking " + repoCacheUrl + "! " + e.getMessage());
            throw new RuntimeException("Error while downloading/unpacking " + repoCacheUrl + "!", e);
        }
        buildLogger.println("Maven repo cache successfully unpacked into " + localMavenRepoDir);
        return true;
    }

    /**
     * Descriptor for {@link MavenRepoCacheBuilder}. Used as a singleton.
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
            return "Use local Maven repo cache";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
            save();
            return super.configure(req, formData);
        }

    }

}
