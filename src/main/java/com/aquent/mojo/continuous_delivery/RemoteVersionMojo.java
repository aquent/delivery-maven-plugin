package com.aquent.mojo.continuous_delivery;

/*
 * The MIT License
 *
 * Copyright (c) 2004, The Codehaus
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataResolutionException;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Resolve the highest version of this project found in a remote repository.
 * This mojo sets the following properties:
 *
 * <pre>
 *   [propertyPrefix].version
 *   [propertyPrefix].majorVersion
 *   [propertyPrefix].minorVersion
 *   [propertyPrefix].incrementalVersion
 * </pre>
 */
@Mojo(name = "remote-version", defaultPhase = LifecyclePhase.VALIDATE)
public class RemoteVersionMojo extends AbstractMojo {

    @Component
    private MavenProject project;

    @Component
    private ArtifactFactory artifactFactory;

    @Component
    private RepositoryMetadataManager repositoryMetadataManager;

    @Parameter(defaultValue = "${localRepository}", readonly = true)
    private ArtifactRepository localRepository;

    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true)
    private List<ArtifactRepository> remoteArtifactRepositories;

    @Parameter(defaultValue = "remoteVersion")
    private String propertyPrefix;

    @Parameter(property = "remoteRepositoryId")
    private String remoteRepositoryId;

    private void defineVersionProperty(String name, String value) {
        project.getProperties().put(propertyPrefix + '.' + name, value);
    }

    private void defineVersionProperty(String name, int value) {
        defineVersionProperty(name, Integer.toString(value));
    }

    private ArtifactRepository getRemoteRepository() {
        for (ArtifactRepository repository : remoteArtifactRepositories) {
            if (remoteRepositoryId.equals(repository.getId())) {
                return repository;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public void execute() {
        try {
            Artifact artifact = artifactFactory.createArtifact(project.getGroupId(), project.getArtifactId(), "", "", "");
            RepositoryMetadata metadata = new ArtifactRepositoryMetadata(artifact);
            repositoryMetadataManager.resolveAlways(metadata, localRepository, getRemoteRepository());
            if (metadata.getMetadata() != null && metadata.getMetadata().getVersioning() != null) {
                List<String> allVersions = metadata.getMetadata().getVersioning().getVersions();
                ArtifactVersion foundVersion = null;
                for (String version : allVersions) {
                    ArtifactVersion artifactVersion = new DefaultArtifactVersion(version);
                    if (foundVersion == null || artifactVersion.compareTo(foundVersion) > 0) {
                        foundVersion = artifactVersion;
                    }
                }
                if (foundVersion != null) {
                    defineVersionProperty("version", foundVersion.toString());
                    defineVersionProperty("majorVersion", foundVersion.getMajorVersion());
                    defineVersionProperty("minorVersion", foundVersion.getMinorVersion());
                    defineVersionProperty("incrementalVersion", foundVersion.getIncrementalVersion());
                }
            }
        } catch (RepositoryMetadataResolutionException ex) {
            getLog().warn(ex.toString());
        }
    }
}
