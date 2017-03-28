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
            add(new KieGitHubRepository("errai", "errai"));
            add(new KieGitHubRepository("uberfire", "uberfire"));
            add(new KieGitHubRepository("dashbuilder", "dashbuilder"));
            add(new KieGitHubRepository("kiegroup", "droolsjbpm-build-bootstrap"));
            add(new KieGitHubRepository("kiegroup", "droolsjbpm-knowledge"));
            add(new KieGitHubRepository("kiegroup", "drools"));
            add(new KieGitHubRepository("kiegroup", "optaplanner"));
            add(new KieGitHubRepository("kiegroup", "jbpm"));
            add(new KieGitHubRepository("kiegroup", "droolsjbpm-integration"));
            add(new KieGitHubRepository("kiegroup", "droolsjbpm-tools"));
            add(new KieGitHubRepository("kiegroup", "kie-uberfire-extensions"));
            add(new KieGitHubRepository("kiegroup", "guvnor"));
            add(new KieGitHubRepository("kiegroup", "kie-wb-playground"));
            add(new KieGitHubRepository("kiegroup", "kie-wb-common"));
            add(new KieGitHubRepository("kiegroup", "jbpm-form-modeler"));
            add(new KieGitHubRepository("kiegroup", "drools-wb"));
            add(new KieGitHubRepository("kiegroup", "optaplanner-wb"));
            add(new KieGitHubRepository("kiegroup", "jbpm-designer"));
            add(new KieGitHubRepository("kiegroup", "jbpm-wb"));
            add(new KieGitHubRepository("kiegroup", "kie-docs"));
            add(new KieGitHubRepository("kiegroup", "kie-wb-distributions"));
        }};
        return new GitHubRepositoryList(repos);
    }

    public static GitHubRepositoryList getListFor65xBranch() {
        List<KieGitHubRepository> repos = new ArrayList<KieGitHubRepository>() {{
            add(new KieGitHubRepository("errai", "errai"));
            add(new KieGitHubRepository("uberfire", "uberfire"));
            add(new KieGitHubRepository("uberfire", "uberfire-extensions"));
            add(new KieGitHubRepository("dashbuilder", "dashbuilder"));
            add(new KieGitHubRepository("kiegroup", "droolsjbpm-build-bootstrap"));
            add(new KieGitHubRepository("kiegroup", "droolsjbpm-knowledge"));
            add(new KieGitHubRepository("kiegroup", "drools"));
            add(new KieGitHubRepository("kiegroup", "optaplanner"));
            add(new KieGitHubRepository("kiegroup", "jbpm"));
            add(new KieGitHubRepository("kiegroup", "droolsjbpm-integration"));
            add(new KieGitHubRepository("kiegroup", "droolsjbpm-tools"));
            add(new KieGitHubRepository("kiegroup", "kie-uberfire-extensions"));
            add(new KieGitHubRepository("kiegroup", "guvnor"));
            add(new KieGitHubRepository("kiegroup", "kie-wb-common"));
            add(new KieGitHubRepository("kiegroup", "jbpm-form-modeler"));
            add(new KieGitHubRepository("kiegroup", "drools-wb"));
            add(new KieGitHubRepository("kiegroup", "jbpm-designer"));
            add(new KieGitHubRepository("kiegroup", "jbpm-console-ng"));
            add(new KieGitHubRepository("kiegroup", "dashboard-builder"));
            add(new KieGitHubRepository("kiegroup", "optaplanner-wb"));
            add(new KieGitHubRepository("kiegroup", "jbpm-dashboard"));
            add(new KieGitHubRepository("kiegroup", "kie-docs"));
            add(new KieGitHubRepository("kiegroup", "kie-wb-distributions"));
            add(new KieGitHubRepository("kiegroup", "droolsjbpm-build-distribution"));
            add(new KieGitHubRepository("jboss-integration", "kie-eap-modules"));
        }};
        return new GitHubRepositoryList(repos);
    }

    public static GitHubRepositoryList getListFor64xBranch() {
        List<KieGitHubRepository> repos = new ArrayList<KieGitHubRepository>() {{
            add(new KieGitHubRepository("errai", "errai"));
            add(new KieGitHubRepository("uberfire", "uberfire"));
            add(new KieGitHubRepository("uberfire", "uberfire-extensions"));
            add(new KieGitHubRepository("dashbuilder", "dashbuilder"));
            add(new KieGitHubRepository("kiegroup", "droolsjbpm-build-bootstrap"));
            add(new KieGitHubRepository("kiegroup", "droolsjbpm-knowledge"));
            add(new KieGitHubRepository("kiegroup", "drools"));
            add(new KieGitHubRepository("kiegroup", "optaplanner"));
            add(new KieGitHubRepository("kiegroup", "jbpm"));
            add(new KieGitHubRepository("kiegroup", "droolsjbpm-integration"));
            add(new KieGitHubRepository("kiegroup", "droolsjbpm-tools"));
            add(new KieGitHubRepository("kiegroup", "kie-uberfire-extensions"));
            add(new KieGitHubRepository("kiegroup", "guvnor"));
            add(new KieGitHubRepository("kiegroup", "kie-wb-common"));
            add(new KieGitHubRepository("kiegroup", "jbpm-form-modeler"));
            add(new KieGitHubRepository("kiegroup", "drools-wb"));
            add(new KieGitHubRepository("kiegroup", "jbpm-designer"));
            add(new KieGitHubRepository("kiegroup", "jbpm-console-ng"));
            add(new KieGitHubRepository("kiegroup", "dashboard-builder"));
            add(new KieGitHubRepository("kiegroup", "optaplanner-wb"));
            add(new KieGitHubRepository("kiegroup", "jbpm-dashboard"));
            add(new KieGitHubRepository("kiegroup", "kie-docs"));
            add(new KieGitHubRepository("kiegroup", "kie-wb-distributions"));
            add(new KieGitHubRepository("kiegroup", "droolsjbpm-build-distribution"));
            add(new KieGitHubRepository("jboss-integration", "kie-eap-modules"));
        }};
        return new GitHubRepositoryList(repos);
    }

    public static GitHubRepositoryList getListFor63xBranch() {
        List<KieGitHubRepository> repos = new ArrayList<KieGitHubRepository>() {{
            add(new KieGitHubRepository("uberfire", "uberfire"));
            add(new KieGitHubRepository("uberfire", "uberfire-extensions"));
            add(new KieGitHubRepository("dashbuilder", "dashbuilder"));
            add(new KieGitHubRepository("kiegroup", "droolsjbpm-build-bootstrap"));
            add(new KieGitHubRepository("kiegroup", "droolsjbpm-knowledge"));
            add(new KieGitHubRepository("kiegroup", "drools"));
            add(new KieGitHubRepository("kiegroup", "optaplanner"));
            add(new KieGitHubRepository("kiegroup", "jbpm"));
            add(new KieGitHubRepository("kiegroup", "droolsjbpm-integration"));
            add(new KieGitHubRepository("kiegroup", "droolsjbpm-tools"));
            add(new KieGitHubRepository("kiegroup", "kie-uberfire-extensions"));
            add(new KieGitHubRepository("kiegroup", "guvnor"));
            add(new KieGitHubRepository("kiegroup", "kie-wb-common"));
            add(new KieGitHubRepository("kiegroup", "jbpm-form-modeler"));
            add(new KieGitHubRepository("kiegroup", "drools-wb"));
            add(new KieGitHubRepository("kiegroup", "jbpm-designer"));
            add(new KieGitHubRepository("kiegroup", "jbpm-console-ng"));
            add(new KieGitHubRepository("kiegroup", "dashboard-builder"));
            add(new KieGitHubRepository("kiegroup", "jbpm-dashboard"));
            add(new KieGitHubRepository("kiegroup", "kie-docs"));
            add(new KieGitHubRepository("kiegroup", "kie-wb-distributions"));
            add(new KieGitHubRepository("kiegroup", "droolsjbpm-build-distribution"));
            add(new KieGitHubRepository("jboss-integration", "kie-eap-modules"));
        }};
        return new GitHubRepositoryList(repos);
    }

    public static GitHubRepositoryList getListFor62xBranch() {
        List<KieGitHubRepository> repos = new ArrayList<KieGitHubRepository>() {{
            add(new KieGitHubRepository("uberfire", "uberfire"));
            add(new KieGitHubRepository("uberfire", "uberfire-extensions"));
            add(new KieGitHubRepository("dashbuilder", "dashbuilder"));
            add(new KieGitHubRepository("kiegroup", "droolsjbpm-build-bootstrap"));
            add(new KieGitHubRepository("kiegroup", "droolsjbpm-knowledge"));
            add(new KieGitHubRepository("kiegroup", "drools"));
            add(new KieGitHubRepository("kiegroup", "optaplanner"));
            add(new KieGitHubRepository("kiegroup", "jbpm"));
            add(new KieGitHubRepository("kiegroup", "droolsjbpm-integration"));
            add(new KieGitHubRepository("kiegroup", "droolsjbpm-tools"));
            add(new KieGitHubRepository("kiegroup", "kie-uberfire-extensions"));
            add(new KieGitHubRepository("kiegroup", "guvnor"));
            add(new KieGitHubRepository("kiegroup", "kie-wb-common"));
            add(new KieGitHubRepository("kiegroup", "jbpm-form-modeler"));
            add(new KieGitHubRepository("kiegroup", "drools-wb"));
            add(new KieGitHubRepository("kiegroup", "jbpm-designer"));
            add(new KieGitHubRepository("kiegroup", "jbpm-console-ng"));
            add(new KieGitHubRepository("kiegroup", "dashboard-builder"));
            add(new KieGitHubRepository("kiegroup", "jbpm-dashboard"));
            add(new KieGitHubRepository("kiegroup", "kie-docs"));
            add(new KieGitHubRepository("kiegroup", "kie-wb-distributions"));
            add(new KieGitHubRepository("kiegroup", "droolsjbpm-build-distribution"));
            add(new KieGitHubRepository("jboss-integration", "kie-eap-modules"));
        }};
        return new GitHubRepositoryList(repos);
    }

    private static final List<BranchMapping> BRANCH_MAPPINGS = initMappings();

    private static List<BranchMapping> initMappings() {
        List<BranchMapping> mappings = new ArrayList<>();
        // branches for errai, uf, dashbuilder, kie
        mappings.add(new BranchMapping("master", "master", "master", "master"));
        mappings.add(new BranchMapping("3.2", "0.9.x", "0.5.x", "6.5.x"));
        mappings.add(new BranchMapping("3.2", "0.8.x", "0.4.x", "6.4.x"));
        mappings.add(new BranchMapping("0.7.x", "0.3.x", "6.3.x"));
        mappings.add(new BranchMapping("0.5.x", "0.2.x", "6.2.x"));
        return mappings;
    }

    public static String getBaseBranchFor(String repo, String otherRepo, String otherBranch) {
        BranchMapping mapping = getMapping(otherRepo, otherBranch);
        if (isErraiRepo(repo)) {
            return mapping.getErraiBranch();
        }else if (isUberFireRepo(repo)) {
            return mapping.getUfBranch();
        } else if (isDashbuilderRepo(repo)) {
            return mapping.getDashBranch();
        } else {
            return mapping.getKieBranch();
        }
    }

    private static BranchMapping getMapping(String repoName, String branch) {
        for (BranchMapping mapping : BRANCH_MAPPINGS) {
            if (isErraiRepo(repoName) && mapping.getErraiBranch() != null && mapping.getErraiBranch().equals(branch)) {
                return mapping;
            } else if (isUberFireRepo(repoName) && mapping.getUfBranch().equals(branch)) {
                return mapping;
            } else if (isDashbuilderRepo(repoName) && mapping.getDashBranch().equals(branch)) {
                return mapping;
            } else if (mapping.getKieBranch().equals(branch)) {
                return mapping;
            }
        }
        throw new RuntimeException("No branch mapping found for repo " + repoName + " with branch " + branch);
    }

    private static boolean isErraiRepo(String repoName) {
        return repoName.startsWith("errai");
    }
    private static boolean isUberFireRepo(String repoName) {
        return repoName.startsWith("uberfire");
    }

    private static boolean isDashbuilderRepo(String repoName) {
        return repoName.startsWith("dashbuilder");
    }

    public static class BranchMapping {
        /**
         * Can be null, because not all KIE branches depend on SNAPSHOTs
         * (and thus don't need to build the Errai for every PR)
         */
        private final String erraiBranch;
        private final String ufBranch;
        private final String dashBranch;
        private final String kieBranch;

        public BranchMapping(String erraiBranch, String ufBranch, String dashBranch, String kieBranch) {
            this.erraiBranch = erraiBranch;
            this.ufBranch = ufBranch;
            this.dashBranch = dashBranch;
            this.kieBranch = kieBranch;
        }

        public BranchMapping(String ufBranch, String dashBranch, String kieBranch) {
            this.erraiBranch = null;
            this.ufBranch = ufBranch;
            this.dashBranch = dashBranch;
            this.kieBranch = kieBranch;
        }

        public String getErraiBranch() {
            return erraiBranch;
        }

        public String getUfBranch() {
            return ufBranch;
        }

        public String getDashBranch() {
            return dashBranch;
        }

        public String getKieBranch() {
            return kieBranch;
        }
    }

}
