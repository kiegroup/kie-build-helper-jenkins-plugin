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
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DownstreamReposBuilderTest {

    private KiePRBuildsHelper.KiePRBuildsHelperDescriptor globalSettings;

    @Before
    public void setup() {
        globalSettings = Mockito.mock(KiePRBuildsHelper.KiePRBuildsHelperDescriptor.class);
        Mockito.when(globalSettings.getDownstreambuildsMavenArgLine()).thenReturn("clean install");
    }

    @Test
    public void shouldInitializeFromEnvVarsAndJobConfigWithoutPRInfo() {
        EnvVars envVars = new EnvVars(
                "baseRepo", "droolsjbpm/drools",
                "sourceBranch", "myBranch",
                "targetBranch", "master"
        );

        DownstreamReposBuilder downstreamReposBuilder = new DownstreamReposBuilder("-e -B clean verify", System.out, globalSettings);
        downstreamReposBuilder.initFromEnvVars(envVars);
        Assertions.assertThat(downstreamReposBuilder.getBaseRepoName()).isEqualTo("drools");
        Assertions.assertThat(downstreamReposBuilder.getBaseRepoOwner()).isEqualTo("droolsjbpm");
        Assertions.assertThat(downstreamReposBuilder.getSourceBranch()).isEqualTo("myBranch");
        Assertions.assertThat(downstreamReposBuilder.getTargetBranch()).isEqualTo("master");
        Assertions.assertThat(downstreamReposBuilder.getMvnArgLine()).isEqualTo("-e -B clean verify");
    }

    @Test
    public void shouldInitializeFromEnvVarsWithPRInfo() {
        EnvVars envVars = new EnvVars(
                "ghprbGhRepository", "droolsjbpm/drools",
                "ghprbSourceBranch", "myBranch",
                "ghprbTargetBranch", "master"
        );
        DownstreamReposBuilder downstreamReposBuilder = new DownstreamReposBuilder("", System.out, globalSettings);
        downstreamReposBuilder.initFromEnvVars(envVars);
        Assertions.assertThat(downstreamReposBuilder.getBaseRepoName()).isEqualTo("drools");
        Assertions.assertThat(downstreamReposBuilder.getBaseRepoOwner()).isEqualTo("droolsjbpm");
        Assertions.assertThat(downstreamReposBuilder.getSourceBranch()).isEqualTo("myBranch");
        Assertions.assertThat(downstreamReposBuilder.getTargetBranch()).isEqualTo("master");
        // per project arg line not specified, the global one should be used
        Assertions.assertThat(downstreamReposBuilder.getMvnArgLine()).isEqualTo("clean install");
    }

}
