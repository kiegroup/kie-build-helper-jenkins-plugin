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

public class KieGitHubRepository {
    private final String owner;
    private final String name;

    public KieGitHubRepository(String owner, String name) {
        this.owner = owner;
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getReadOnlyCloneURL() {
        return "git://github.com/" + owner + "/" + name;
    }

    /**
     * Determines base branch for this repository based on PR repo name and target branch.
     *
     * That information is used to clone repositories which do not directly contain any specific changes. Building
     * those base branches will decrease the number of false negatives caused by stale snapshots, as we will always
     * have the latest sources available.
     *
     * @param prRepoName     repository name against which the PR was submitted
     * @param prTargetBranch target branch specified in the PR
     * @return name of the base branch which matches the target PR branch
     */
    public String determineBaseBranch(String prRepoName, String prTargetBranch) {
        return KieRepositoryLists.getBaseBranchFor(this.name, prRepoName, prTargetBranch);
    }


    @Override
    public String toString() {
        return "KieGitHubRepository{" +
                "owner='" + owner + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KieGitHubRepository that = (KieGitHubRepository) o;

        if (owner != null ? !owner.equals(that.owner) : that.owner != null) return false;
        return !(name != null ? !name.equals(that.name) : that.name != null);

    }

    @Override
    public int hashCode() {
        int result = owner != null ? owner.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

}
