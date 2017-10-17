/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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

import java.util.List;

/***
 * Represents a mapping between a KIE branch and a list of upstream repositories (with specific branches)
 */
public class BranchMapping {

    private final GitBranch kieBranch;
    private final List<Tuple<GitHubRepository, GitBranch>> upstreamDeps;

    public BranchMapping(GitBranch kieBranch, List<Tuple<GitHubRepository, GitBranch>> upstreamDeps) {
        this.kieBranch = kieBranch;
        this.upstreamDeps = upstreamDeps;
    }

    public GitBranch getKieBranch() {
        return kieBranch;
    }

    public List<Tuple<GitHubRepository, GitBranch>> getUpstreamDeps() {
        return upstreamDeps;
    }

    @Override
    public String toString() {
        return "BranchMapping{" +
                "kieBranch=" + kieBranch +
                ", upstreamDeps=" + upstreamDeps +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BranchMapping that = (BranchMapping) o;

        if (kieBranch != null ? !kieBranch.equals(that.kieBranch) : that.kieBranch != null) {
            return false;
        }
        return upstreamDeps != null ? upstreamDeps.equals(that.upstreamDeps) : that.upstreamDeps == null;
    }

    @Override
    public int hashCode() {
        int result = kieBranch != null ? kieBranch.hashCode() : 0;
        result = 31 * result + (upstreamDeps != null ? upstreamDeps.hashCode() : 0);
        return result;
    }
}
