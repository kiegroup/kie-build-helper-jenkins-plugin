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

public class MavenBuildConfig {
    private final String mavenHome;
    private final String mavenOpts;
    private final String mavenArgs;

    public MavenBuildConfig(String mavenHome, String mavenOpts, String mavenArgs) {
        this.mavenHome = mavenHome;
        this.mavenOpts = mavenOpts;
        this.mavenArgs = mavenArgs;
    }

    public String getMavenHome() {
        return mavenHome;
    }

    public String getMavenOpts() {
        return mavenOpts;
    }

    public String getMavenArgs() {
        return mavenArgs;
    }
}
