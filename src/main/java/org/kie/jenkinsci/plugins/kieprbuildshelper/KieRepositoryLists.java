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

public class KieRepositoryLists {

    public static GitHubRepositoryList getListForMasterBranch() {
        List<KieGitHubRepository> repos = new ArrayList<KieGitHubRepository>() {{
            add(new KieGitHubRepository("uberfire", "uberfire"));
            add(new KieGitHubRepository("uberfire", "uberfire-extensions"));
            add(new KieGitHubRepository("dashbuilder", "dashbuilder"));
            add(new KieGitHubRepository("droolsjbpm", "droolsjbpm-build-bootstrap"));
            add(new KieGitHubRepository("droolsjbpm", "droolsjbpm-knowledge"));
            add(new KieGitHubRepository("droolsjbpm", "drools"));
            add(new KieGitHubRepository("droolsjbpm", "optaplanner"));
            add(new KieGitHubRepository("droolsjbpm", "jbpm"));
            add(new KieGitHubRepository("droolsjbpm", "droolsjbpm-integration"));
            add(new KieGitHubRepository("droolsjbpm", "droolsjbpm-tools"));
            add(new KieGitHubRepository("droolsjbpm", "kie-uberfire-extensions"));
            add(new KieGitHubRepository("droolsjbpm", "guvnor"));
            add(new KieGitHubRepository("droolsjbpm", "kie-wb-common"));
            add(new KieGitHubRepository("droolsjbpm", "jbpm-form-modeler"));
            add(new KieGitHubRepository("droolsjbpm", "drools-wb"));
            add(new KieGitHubRepository("droolsjbpm", "jbpm-designer"));
            add(new KieGitHubRepository("droolsjbpm", "jbpm-console-ng"));
            add(new KieGitHubRepository("droolsjbpm", "dashboard-builder"));
            add(new KieGitHubRepository("droolsjbpm", "optaplanner-wb"));
            add(new KieGitHubRepository("droolsjbpm", "jbpm-dashboard"));
            add(new KieGitHubRepository("droolsjbpm", "kie-docs"));
            add(new KieGitHubRepository("droolsjbpm", "kie-wb-distributions"));
            add(new KieGitHubRepository("droolsjbpm", "droolsjbpm-build-distribution"));
            add(new KieGitHubRepository("jboss-integration", "kie-eap-modules"));
        }};
        return new GitHubRepositoryList(repos);
    }

    public static GitHubRepositoryList getListFor63xBranch() {
        List<KieGitHubRepository> repos = new ArrayList<KieGitHubRepository>() {{
            add(new KieGitHubRepository("uberfire", "uberfire"));
            add(new KieGitHubRepository("uberfire", "uberfire-extensions"));
            add(new KieGitHubRepository("dashbuilder", "dashbuilder"));
            add(new KieGitHubRepository("droolsjbpm", "droolsjbpm-build-bootstrap"));
            add(new KieGitHubRepository("droolsjbpm", "droolsjbpm-knowledge"));
            add(new KieGitHubRepository("droolsjbpm", "drools"));
            add(new KieGitHubRepository("droolsjbpm", "optaplanner"));
            add(new KieGitHubRepository("droolsjbpm", "jbpm"));
            add(new KieGitHubRepository("droolsjbpm", "droolsjbpm-integration"));
            add(new KieGitHubRepository("droolsjbpm", "droolsjbpm-tools"));
            add(new KieGitHubRepository("droolsjbpm", "kie-uberfire-extensions"));
            add(new KieGitHubRepository("droolsjbpm", "guvnor"));
            add(new KieGitHubRepository("droolsjbpm", "kie-wb-common"));
            add(new KieGitHubRepository("droolsjbpm", "jbpm-form-modeler"));
            add(new KieGitHubRepository("droolsjbpm", "drools-wb"));
            add(new KieGitHubRepository("droolsjbpm", "jbpm-designer"));
            add(new KieGitHubRepository("droolsjbpm", "jbpm-console-ng"));
            add(new KieGitHubRepository("droolsjbpm", "dashboard-builder"));
            add(new KieGitHubRepository("droolsjbpm", "jbpm-dashboard"));
            add(new KieGitHubRepository("droolsjbpm", "kie-docs"));
            add(new KieGitHubRepository("droolsjbpm", "kie-wb-distributions"));
            add(new KieGitHubRepository("droolsjbpm", "droolsjbpm-build-distribution"));
            add(new KieGitHubRepository("jboss-integration", "kie-eap-modules"));
        }};
        return new GitHubRepositoryList(repos);
    }

    public static GitHubRepositoryList getListFor62xBranch() {
        List<KieGitHubRepository> repos = new ArrayList<KieGitHubRepository>() {{
            add(new KieGitHubRepository("uberfire", "uberfire"));
            add(new KieGitHubRepository("uberfire", "uberfire-extensions"));
            add(new KieGitHubRepository("dashbuilder", "dashbuilder"));
            add(new KieGitHubRepository("droolsjbpm", "droolsjbpm-build-bootstrap"));
            add(new KieGitHubRepository("droolsjbpm", "droolsjbpm-knowledge"));
            add(new KieGitHubRepository("droolsjbpm", "drools"));
            add(new KieGitHubRepository("droolsjbpm", "optaplanner"));
            add(new KieGitHubRepository("droolsjbpm", "jbpm"));
            add(new KieGitHubRepository("droolsjbpm", "droolsjbpm-integration"));
            add(new KieGitHubRepository("droolsjbpm", "droolsjbpm-tools"));
            add(new KieGitHubRepository("droolsjbpm", "kie-uberfire-extensions"));
            add(new KieGitHubRepository("droolsjbpm", "guvnor"));
            add(new KieGitHubRepository("droolsjbpm", "kie-wb-common"));
            add(new KieGitHubRepository("droolsjbpm", "jbpm-form-modeler"));
            add(new KieGitHubRepository("droolsjbpm", "drools-wb"));
            add(new KieGitHubRepository("droolsjbpm", "jbpm-designer"));
            add(new KieGitHubRepository("droolsjbpm", "jbpm-console-ng"));
            add(new KieGitHubRepository("droolsjbpm", "dashboard-builder"));
            add(new KieGitHubRepository("droolsjbpm", "jbpm-dashboard"));
            add(new KieGitHubRepository("droolsjbpm", "kie-docs"));
            add(new KieGitHubRepository("droolsjbpm", "kie-wb-distributions"));
            add(new KieGitHubRepository("droolsjbpm", "droolsjbpm-build-distribution"));
            add(new KieGitHubRepository("jboss-integration", "kie-eap-modules"));
        }};
        return new GitHubRepositoryList(repos);
    }
}
