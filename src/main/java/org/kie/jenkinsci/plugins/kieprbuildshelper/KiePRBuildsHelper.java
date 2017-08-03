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
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Used to store global configuration which is shared between multiple builder classes.
 */
public class KiePRBuildsHelper extends JobProperty<Job<?, ?>> {

    @Override
    public KiePRBuildsHelperDescriptor getDescriptor() {
        return (KiePRBuildsHelperDescriptor) Jenkins.getInstance().getDescriptor(getClass());
    }

    public static KiePRBuildsHelperDescriptor getKiePRBuildsHelperDescriptor() {
        return (KiePRBuildsHelperDescriptor) Jenkins.getInstance().getDescriptor(KiePRBuildsHelper.class);
    }

    @Extension
    public static class KiePRBuildsHelperDescriptor extends JobPropertyDescriptor {

        private String ghOAuthToken;
        private String mavenRepoCacheTgzUrl;

        public KiePRBuildsHelperDescriptor() {
            super(KiePRBuildsHelper.class);
            load();
        }

        @DataBoundConstructor
        public KiePRBuildsHelperDescriptor(String ghOAuthToken, String mavenRepoCacheTgzUrl) {
            this.ghOAuthToken = ghOAuthToken;
            this.mavenRepoCacheTgzUrl = mavenRepoCacheTgzUrl;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            ghOAuthToken = formData.getString("ghOAuthToken");
            mavenRepoCacheTgzUrl = formData.getString("mavenRepoCacheTgzUrl");
            save();
            return super.configure(req, formData);
        }

        @Override
        public String getDisplayName() {
            return "KIE GitHub PRs Helper";
        }

        public String getGhOAuthToken() {
            return ghOAuthToken;
        }

        public String getMavenRepoCacheTgzUrl() {
            return mavenRepoCacheTgzUrl;
        }

    }

}
