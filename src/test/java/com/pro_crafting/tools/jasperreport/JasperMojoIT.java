package com.pro_crafting.tools.jasperreport;

import com.soebes.itf.extension.assertj.MavenITAssertions;
import com.soebes.itf.jupiter.extension.MavenGoal;
import com.soebes.itf.jupiter.extension.MavenJupiterExtension;
import com.soebes.itf.jupiter.extension.MavenTest;
import com.soebes.itf.jupiter.maven.MavenExecutionResult;
import org.junit.jupiter.api.Assertions;

import java.io.File;

@MavenJupiterExtension
class JasperMojoIT {
    /**
     * Test that configuration set within a pom are correctly mapped to @{@link JasperMojoConfiguration}
     *
     * @throws Exception
     *             When an unexpexted error occures.
     */
    @MavenGoal("${project.groupId}:${project.artifactId}:${project.version}:jasper")
    @MavenTest
    void testConfigurationMapping(MavenExecutionResult result) {

        MavenITAssertions.assertThat(result).out().info().contains("Output dir: "+new File("/outputDirectory").getAbsolutePath());
        MavenITAssertions.assertThat(result).out().info().contains("Source dir: "+new File("/sourceDirectory").getAbsolutePath());
        MavenITAssertions.assertThat(result).out().info().contains("Output ext: outputFileExt");
        MavenITAssertions.assertThat(result).out().info().contains("Source ext: sourceFileExt");
        MavenITAssertions.assertThat(result).out().info().contains("Additional properties: {net.sf.jasperreports.awt.ignore.missing.font=true}");
        MavenITAssertions.assertThat(result).out().info().contains("XML Validation: false");
        MavenITAssertions.assertThat(result).out().info().contains("JasperReports Compiler: compiler");
        MavenITAssertions.assertThat(result).out().info().contains("Number of threads: 2");
        MavenITAssertions.assertThat(result).out().info().contains("Additional Classpath: additionalClasspath");
        MavenITAssertions.assertThat(result).out().info().contains("Source Scanner: sourceScanner");
    }
}
