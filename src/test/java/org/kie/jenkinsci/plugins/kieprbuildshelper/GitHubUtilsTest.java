package org.kie.jenkinsci.plugins.kieprbuildshelper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GitHubUtilsTest {

    @Test
    public void extractRepositoryName() {
        String result = GitHubUtils.extractRepositoryName("https://github.com/kiegroup/optaplanner-wb/pull/198");

        assertEquals("optaplanner-wb", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void extractRepositoryNameWrongUrl() {
        GitHubUtils.extractRepositoryName("https://wrong/url/pull/125");
    }
}
