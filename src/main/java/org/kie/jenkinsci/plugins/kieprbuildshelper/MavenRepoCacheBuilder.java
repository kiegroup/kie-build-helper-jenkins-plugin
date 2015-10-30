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
 * Custom {@link Builder} which dwnloads and unpacks Maven repo cache from specified URL.
 * <p>
 * This is needed fro multi-repo builds as we can't install the artifacts into default local
 * repo (~/.m2). However, downloading everything again for every build would take too much time.
 * The repo cache is build once and it can be reused in all the PR jobs.
 */
public class MavenRepoCacheBuilder extends Builder {


    @DataBoundConstructor
    public MavenRepoCacheBuilder() {
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        String mavenRepoCacheZipUrl = KiePRBuildsHelper.getKiePRBuildsHelperDescriptor().getMavenRepoCacheZipUrl();
        listener.getLogger().println("Using Maven repo cache: " + mavenRepoCacheZipUrl);
        return true;
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
            return "Use local Maven repo cache";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
            save();
            return super.configure(req,formData);
        }

    }

}
