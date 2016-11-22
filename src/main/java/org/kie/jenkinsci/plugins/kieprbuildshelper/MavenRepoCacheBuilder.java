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
 *
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
        buildLogger.println("Using local Maven repo cache builder.");
        if (mavenRepoCacheTgzUrl == null || "".equals(mavenRepoCacheTgzUrl)) {
            buildLogger.println("Error! No URL for Maven repo cache specified. Configure one in global Jenkins configuration.");
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
        // root directory on the slave, e.g. "/home/jenkins" or "c:\jenkins"
        FilePath remoteFSRoot = build.getBuiltOn().getRootPath();
        FilePath localMavenRepoFile = new FilePath(remoteFSRoot, "maven-repo-cache.tar.gz");
        FilePath localMavenRepoDir = new FilePath(workspace, ".repository");

        String remoteChecksumUrl = mavenRepoCacheTgzUrl + ".md5";
        try {
            if (shouldDownloadRepoCache(remoteChecksumUrl, localMavenRepoFile)) {
                downloadRepoCache(repoCacheUrl, localMavenRepoFile);
            } else {
                buildLogger.println("Local Maven repo cache file at " + localMavenRepoFile + " is up-to-date and will be used as is.");
            }
            unpackCacheArchive(localMavenRepoFile, localMavenRepoDir);
        } catch (Exception e) {
            buildLogger.println("Error while downloading/unpacking " + repoCacheUrl + "! " + e.getMessage());
            throw new RuntimeException("Error while downloading/unpacking " + repoCacheUrl + "!", e);
        }
        buildLogger.println("Maven repo cache successfully unpacked into " + localMavenRepoDir);
        return true;
    }

    /**
     * Determines whether it is needed to download the cache archive.
     *
     * The archive should be downloaded only if required, meaning:
     *   - there is no local version of the archive (e.g. this is first use of the cache for the particular slave)
     *   - there is no local file with the MD5 checksum
     *   - there is no remote file with the MD5 checksum (so we can not check if the checksum changed or not)
     *   - the checksum of the remote archive is different from the local one (likely means the cache was updated
     *     and thus needs to downloaded again)
     *
     * @param checksumUrlStr URL of the latest cache archive (as String)
     * @param localCacheFile local file with the cache
     *
     * @return true if the cache needs to be downloaded, otherwise false (and the current cache file should be used)
     */
    private boolean shouldDownloadRepoCache(String checksumUrlStr, FilePath localCacheFile) {
        try {
            if (!localCacheFile.exists()) {
                return true;
            }
            FilePath checksumFileForLocalCache = new FilePath(localCacheFile.getParent(), localCacheFile.getName() + ".md5");
            if (!checksumFileForLocalCache.exists()) {
                return true;
            }

            URL checksumUrl = new URL(checksumUrlStr);
            FilePath checksumFileForRemoteCache = new FilePath(localCacheFile.getParent(), localCacheFile.getName() + ".md5.remote");
            // get the file which contains checksum for the remote file
            checksumFileForRemoteCache.copyFrom(checksumUrl);
            String localFileChecksum = checksumFileForLocalCache.readToString();
            String remoteFileChecksum = checksumFileForRemoteCache.readToString();
            return !localFileChecksum.trim().equals(remoteFileChecksum.trim());
        } catch (Exception e) {
            buildLogger.println("Error encountered while checking if we need to download the latest cache. Forcing the download." + e.getMessage());
            return true;
        }

    }

    /**
     * Downloads the specified repo cache. Fetches also the checksum file if it exists.
     *
     * @param cacheUrl URL of the repo cache archive
     * @param destFile destination file to download to
     * @throws Exception
     */
    private void downloadRepoCache(URL cacheUrl, FilePath destFile) throws Exception {
        buildLogger.println("Downloading repo cache " + cacheUrl + " into " + destFile);
        destFile.copyFrom(cacheUrl);

        // download the checksum file as well (if it is available)
        String checksumUrlStr = cacheUrl.toExternalForm() + ".md5";
        URL checksumUrl;
        try {
            checksumUrl = new URL(checksumUrlStr);
            buildLogger.println("Storing checksum file " + checksumUrl + " for future reference.");
            new FilePath(destFile.getParent(), destFile.getName() + ".md5").copyFrom(checksumUrl);
        } catch (Exception e) {
            buildLogger.println("Error while downloading checksum file " + checksumUrlStr + "! " + e.getMessage());
            // do not fail just because the checksum file could not be downloaded
        }
    }

    private void unpackCacheArchive(FilePath archive, FilePath destDir) throws Exception {
        buildLogger.println("Unpacking " + archive + " into " + destDir);
        archive.untar(destDir, FilePath.TarCompression.GZIP);
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
