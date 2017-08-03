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

public class GitHubRepository {
    private final String owner;
    private final String name;

    /**
     * Create {@link GitHubRepository} represented by the specified string.
     *
     * The expected format of the string is {@code [<owner>/]<repo>}. The {@code <owner>} part is optional and if not
     * specified it is assumed the owner name is equal to the repo name (e.g. errai is treated as errai/errai).
     *
     * @param str string representation of the GitHub repository
     * @return {@link GitHubRepository} parsed from the string
     */
    public static GitHubRepository from(String str) {
        if (str.contains("/")) {
            String[] parts = str.split("/");
            if (parts.length != 2) {
                throw new IllegalArgumentException("String '" + str + "' cannot be parsed into valid GitHub repository!");
            }
            return new GitHubRepository(parts[0], parts[1]);
        }
        // no "/" means that the repository name and org. unit are equal
        return new GitHubRepository(str, str);
    }

    public GitHubRepository(String owner, String name) {
        if (owner == null || owner.isEmpty()) {
            throw new IllegalArgumentException("Repository 'owner' can not ben null or empty!");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Repository 'name' can not ben null or empty!");
        }
        this.owner = owner;
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return owner + "/" + name;
    }

    public String getReadOnlyCloneURL() {
        return "git://github.com/" + owner + "/" + name + ".git";
    }

    @Override
    public String toString() {
        return "GitHubRepository{" +
                "owner='" + owner + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GitHubRepository that = (GitHubRepository) o;

        if (owner != null ? !owner.equals(that.owner) : that.owner != null) return false;
        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        int result = owner != null ? owner.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
