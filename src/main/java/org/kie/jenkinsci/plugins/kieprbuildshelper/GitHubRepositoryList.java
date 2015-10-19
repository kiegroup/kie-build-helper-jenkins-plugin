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

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GitHubRepositoryList {
    public static String KIE_REPO_LIST_MASTER_RESOURCE_PATH = "/repository-list-master.txt";
    public static String KIE_REPO_LIST_6_3_X_RESOURCE_PATH = "/repository-list-6.3.x.txt";
    public static String KIE_REPO_LIST_6_2_X_RESOURCE_PATH = "/repository-list-6.2.x.txt";

    private final List<GitHubRepository> list;

    public static GitHubRepositoryList fromClasspathResource(String resourcePath) {
        InputStream is = GitHubRepositoryList.class.getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IllegalArgumentException("Specified classpath resource '" + resourcePath + "' does not exist!");
        }
        List<String> lines;
        try {
            lines = IOUtils.readLines(is);
        } catch (IOException e) {
            throw new RuntimeException("Error while reading data from classpath resource '" + resourcePath + "'!", e);
        }
        List<GitHubRepository> repoList = new ArrayList<GitHubRepository>();
        for (String line : lines) {
            String[] parts = line.split("/");
            repoList.add(new GitHubRepository(parts[0], parts[1]));
        }
        return new GitHubRepositoryList(repoList);
    }

    public GitHubRepositoryList(List<GitHubRepository> list) {
        this.list = list;
    }

    public List<GitHubRepository> getList() {
        return list;
    }

    public int size() {
        return list.size();
    }
}
