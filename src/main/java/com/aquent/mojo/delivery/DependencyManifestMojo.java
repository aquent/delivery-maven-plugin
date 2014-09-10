package com.aquent.mojo.delivery;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Locate the pathnames of the dependency artifacts.
 */
@Mojo(name = "dependency-manifest", requiresDependencyResolution = ResolutionScope.TEST)
public class DependencyManifestMojo extends AbstractMojo {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    @Component
    private MavenProject project;

    @Parameter(property = "outputFile")
    private File outputFile;

    @Parameter(property = "excludeTransitive", defaultValue = "false")
    private boolean excludeTransitive;

    @Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
    private ArtifactRepository localRepository;

    public void execute() throws MojoExecutionException {

        @SuppressWarnings("unchecked")
        Set<Artifact> artifacts = excludeTransitive ? project.getDependencyArtifacts() : project.getArtifacts();

        StringBuilder sb = new StringBuilder();

        if (artifacts == null || artifacts.isEmpty()) {
            getLog().info("Project has no dependencies");
        } else {
            String basedir = localRepository.getBasedir();
            for (Artifact artifact : artifacts) {
                String path = artifact.getFile().getPath();
                if (!path.startsWith(basedir)) {
                    getLog().warn("Skipping artifact with invalid local repository path: " + path);
                    continue;
                }
                path = path.substring(basedir.length());
                while (path.startsWith(File.separator)) {
                    path = path.substring(File.separator.length());
                }
                sb.append(path);
                sb.append(LINE_SEPARATOR);
            }
        }

        if (outputFile != null) {
            try {
                getLog().info("Writing dependency manifest: " + outputFile);
                writeOutputFile(sb.toString());
            } catch (IOException ex) {
                throw new MojoExecutionException("Error writing file: " + ex.toString(), ex);
            }
        } else if (sb.length() > 0) {
            getLog().info("Dependency manifest:" + LINE_SEPARATOR + sb.toString());
        }
    }

    private void writeOutputFile(String outputString) throws IOException {
        outputFile.getParentFile().mkdirs();
        Writer writer = null;
        try {
            writer = new FileWriter(outputFile);
            writer.write(outputString);
        } finally {
            if (writer != null) writer.close();
        }
    }
}
