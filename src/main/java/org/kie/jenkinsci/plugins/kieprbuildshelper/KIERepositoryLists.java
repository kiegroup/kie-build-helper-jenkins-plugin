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

import java.util.ArrayList;
import java.util.List;

public class KIERepositoryLists {

    public static GitHubRepositoryList getListForMasterBranch() {
        List<GitHubRepository> repos = new ArrayList<GitHubRepository>() {{
            add(new GitHubRepository("uberfire", "uberfire"));
            add(new GitHubRepository("uberfire", "uberfire-extensions"));
            add(new GitHubRepository("dashbuilder", "dashbuilder"));
            add(new GitHubRepository("droolsjbpm", "droolsjbpm-build-bootstrap"));
            add(new GitHubRepository("droolsjbpm", "droolsjbpm-knowledge"));
            add(new GitHubRepository("droolsjbpm", "drools"));
            add(new GitHubRepository("droolsjbpm", "optaplanner"));
            add(new GitHubRepository("droolsjbpm", "jbpm"));
            add(new GitHubRepository("droolsjbpm", "droolsjbpm-integration"));
            add(new GitHubRepository("droolsjbpm", "droolsjbpm-tools"));
            add(new GitHubRepository("droolsjbpm", "kie-uberfire-extensions"));
            add(new GitHubRepository("droolsjbpm", "guvnor"));
            add(new GitHubRepository("droolsjbpm", "kie-wb-common"));
            add(new GitHubRepository("droolsjbpm", "jbpm-form-modeler"));
            add(new GitHubRepository("droolsjbpm", "drools-wb"));
            add(new GitHubRepository("droolsjbpm", "jbpm-designer"));
            add(new GitHubRepository("droolsjbpm", "jbpm-console-ng"));
            add(new GitHubRepository("droolsjbpm", "dashboard-builder"));
            add(new GitHubRepository("droolsjbpm", "optaplanner-wb"));
            add(new GitHubRepository("droolsjbpm", "jbpm-dashboard"));
            add(new GitHubRepository("droolsjbpm", "kie-docs"));
            add(new GitHubRepository("droolsjbpm", "kie-wb-distributions"));
            add(new GitHubRepository("droolsjbpm", "droolsjbpm-build-distribution"));
            add(new GitHubRepository("jboss-integration", "kie-eap-modules"));
        }};
        return new GitHubRepositoryList(repos);
    }

    public static GitHubRepositoryList getListFor63xBranch() {
        List<GitHubRepository> repos = new ArrayList<GitHubRepository>() {{
            add(new GitHubRepository("uberfire", "uberfire"));
            add(new GitHubRepository("uberfire", "uberfire-extensions"));
            add(new GitHubRepository("dashbuilder", "dashbuilder"));
            add(new GitHubRepository("droolsjbpm", "droolsjbpm-build-bootstrap"));
            add(new GitHubRepository("droolsjbpm", "droolsjbpm-knowledge"));
            add(new GitHubRepository("droolsjbpm", "drools"));
            add(new GitHubRepository("droolsjbpm", "optaplanner"));
            add(new GitHubRepository("droolsjbpm", "jbpm"));
            add(new GitHubRepository("droolsjbpm", "droolsjbpm-integration"));
            add(new GitHubRepository("droolsjbpm", "droolsjbpm-tools"));
            add(new GitHubRepository("droolsjbpm", "kie-uberfire-extensions"));
            add(new GitHubRepository("droolsjbpm", "guvnor"));
            add(new GitHubRepository("droolsjbpm", "kie-wb-common"));
            add(new GitHubRepository("droolsjbpm", "jbpm-form-modeler"));
            add(new GitHubRepository("droolsjbpm", "drools-wb"));
            add(new GitHubRepository("droolsjbpm", "jbpm-designer"));
            add(new GitHubRepository("droolsjbpm", "jbpm-console-ng"));
            add(new GitHubRepository("droolsjbpm", "dashboard-builder"));
            add(new GitHubRepository("droolsjbpm", "jbpm-dashboard"));
            add(new GitHubRepository("droolsjbpm", "kie-docs"));
            add(new GitHubRepository("droolsjbpm", "kie-wb-distributions"));
            add(new GitHubRepository("droolsjbpm", "droolsjbpm-build-distribution"));
            add(new GitHubRepository("jboss-integration", "kie-eap-modules"));
        }};
        return new GitHubRepositoryList(repos);
    }

    public static GitHubRepositoryList getListFor62xBranch() {
        List<GitHubRepository> repos = new ArrayList<GitHubRepository>() {{
            add(new GitHubRepository("uberfire", "uberfire"));
            add(new GitHubRepository("uberfire", "uberfire-extensions"));
            add(new GitHubRepository("dashbuilder", "dashbuilder"));
            add(new GitHubRepository("droolsjbpm", "droolsjbpm-build-bootstrap"));
            add(new GitHubRepository("droolsjbpm", "droolsjbpm-knowledge"));
            add(new GitHubRepository("droolsjbpm", "drools"));
            add(new GitHubRepository("droolsjbpm", "optaplanner"));
            add(new GitHubRepository("droolsjbpm", "jbpm"));
            add(new GitHubRepository("droolsjbpm", "droolsjbpm-integration"));
            add(new GitHubRepository("droolsjbpm", "droolsjbpm-tools"));
            add(new GitHubRepository("droolsjbpm", "kie-uberfire-extensions"));
            add(new GitHubRepository("droolsjbpm", "guvnor"));
            add(new GitHubRepository("droolsjbpm", "kie-wb-common"));
            add(new GitHubRepository("droolsjbpm", "jbpm-form-modeler"));
            add(new GitHubRepository("droolsjbpm", "drools-wb"));
            add(new GitHubRepository("droolsjbpm", "jbpm-designer"));
            add(new GitHubRepository("droolsjbpm", "jbpm-console-ng"));
            add(new GitHubRepository("droolsjbpm", "dashboard-builder"));
            add(new GitHubRepository("droolsjbpm", "jbpm-dashboard"));
            add(new GitHubRepository("droolsjbpm", "kie-docs"));
            add(new GitHubRepository("droolsjbpm", "kie-wb-distributions"));
            add(new GitHubRepository("droolsjbpm", "droolsjbpm-build-distribution"));
            add(new GitHubRepository("jboss-integration", "kie-eap-modules"));
        }};
        return new GitHubRepositoryList(repos);
    }
}
