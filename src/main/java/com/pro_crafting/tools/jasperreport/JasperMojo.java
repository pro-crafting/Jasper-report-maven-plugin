package com.pro_crafting.tools.jasperreport;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 */

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This plugin compiles jasper source files to the target folder. While doing
 * so, it keeps the folder structure intact.
 */
@Mojo(defaultPhase = LifecyclePhase.PROCESS_RESOURCES, name = "jasper", requiresDependencyResolution =
		ResolutionScope.COMPILE, threadSafe = true)
public class JasperMojo extends AbstractMojo {
	private static final Logger LOGGER = LoggerFactory.getLogger(JasperMojo.class);

	/**
	 * This is the java compiler used
	 */
	@Parameter(defaultValue = "net.sf.jasperreports.jdt.JRJdtCompiler")
	private String compiler;

	/**
	 * This is where the .jasper files are written.
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}/jasper")
	private File outputDirectory;

	/**
	 * This is where the xml report design files should be.
	 */
	@Parameter(defaultValue = "src/main/jasperreports")
	private File sourceDirectory;

	/**
	 * The extension of the source files to look for. Finds files with a .jrxml
	 * extension by default.
	 */
	@Parameter(defaultValue = ".jrxml")
	private String sourceFileExt;

	/**
	 * The extension of the compiled report files. Creates files with a .jasper
	 * extension by default.
	 *
	 */
	@Parameter(defaultValue = ".jasper")
	private String outputFileExt;

	/**
	 * Check the source files before compiling. Default value is true.
	 *
	 */
	@Parameter(defaultValue = "true")
	private boolean xmlValidation;

	/**
     * Set this to "true" to bypass compiling reports. Default value is false.
     *
     */
    @Parameter( defaultValue = "false" )
    private boolean skip;

	/**
	 * If verbose is true the plug-in will report which reports it is compiling
	 * and which files are being skipped.
	 *
	 */
	@Parameter(defaultValue = "false")
	private boolean verbose;

	/**
	 * The number of threads the reporting will use. Default is 4 which is good
	 * for a lot of reports on a hard drive (in stead of SSD). If you only have
	 * a few, or if you have SSD, it might be faster to set it to 2.
	 *
	 */
	@Parameter(defaultValue = "4")
	private int numberOfThreads;

	@Parameter(property = "project.compileClasspathElements")
	private List<String> classpathElements;

	/**
	 * Use this parameter to add additional properties to the Jasper compiler.
	 * For example.
	 *
	 * <pre>
	 * {@code
	 * <configuration>
	 * 	...
	 * 		<additionalProperties>
	 * 			<net.sf.jasperreports.awt.ignore.missing.font>true
	 * 			</net.sf.jasperreports.awt.ignore.missing.font>
	 *          <net.sf.jasperreports.default.pdf.font.name>Courier</net.sf.jasperreports.default.pdf.font.name>
	 *          <net.sf.jasperreports.default.pdf.encoding>UTF-8</net.sf.jasperreports.default.pdf.encoding>
	 *          <net.sf.jasperreports.default.pdf.embedded>true</net.sf.jasperreports.default.pdf.embedded>
	 * </additionalProperties>
	 * </configuration>
	 * }
	 * </pre>
	 *
	 */
	@Parameter
	private Map<String, String> additionalProperties;

	/**
	 * If failOnMissingSourceDirectory is on the plug-in will fail the build if
	 * source directory does not exist. Default value is true.
	 *
	 */
	@Parameter(defaultValue = "true")
	private boolean failOnMissingSourceDirectory;

	/**
	 * This is the source inclusion scanner class used, an
	 * <code>org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner</code>
	 * implementation class. Currently only <code></code>org.codehaus.plexus.compiler.util.scan.SimpleSourceInclusionScanner</code>
	 * and <code>org.codehaus.plexus.compiler.util.scan.StaleSourceScanner</code> are supported.
	 *
	 */
	@Parameter(defaultValue = "org.codehaus.plexus.compiler.util.scan.StaleSourceScanner")
	private String sourceScanner;

	/**
	 * Provides the option to add additional JARs to the Classpath for compiling. This is handy in case you have
	 * references to external Java-Beans in your JasperReports.
	 *
	 * <pre>
	 * {@code
	 * <configuration>
	 *  ...
	 *      <additionalClasspath>/web/lib/ServiceBeans.jar;/web/lib/WebForms.jar</additionalClasspath>
	 *  ...
	 * </configuration>
	 * }
	 * </pre>
	 *
	 */
	@Parameter
	private String additionalClasspath;

	@Override
	public void execute() throws MojoExecutionException {
		JasperMojoConfiguration configuration = mapConfiguration();
		if (verbose) {
			logConfiguration(configuration);
		}

		new JasperReportCompiler(configuration).compileReports();
	}

	JasperMojoConfiguration mapConfiguration() {
		JasperMojoConfiguration configuration = new JasperMojoConfiguration();
		configuration.compiler = compiler;
		configuration.outputDirectory = outputDirectory;
		configuration.sourceDirectory = sourceDirectory;
		configuration.sourceFileExt = sourceFileExt;
		configuration.outputFileExt = outputFileExt;
		configuration.xmlValidation = xmlValidation;
		configuration.skip = skip;
		configuration.verbose = verbose;
		configuration.numberOfThreads = numberOfThreads;
		configuration.classpathElements = classpathElements;
		configuration.additionalProperties = additionalProperties;
		configuration.failOnMissingSourceDirectory = failOnMissingSourceDirectory;
		configuration.sourceScanner = sourceScanner;
		configuration.additionalClasspath = additionalClasspath;

		return configuration;
	}

	private void logConfiguration(JasperMojoConfiguration configuration) {
		LOGGER.info("Generating Jasper reports");
		LOGGER.info("Output dir: {}", configuration.outputDirectory.getAbsolutePath());
		LOGGER.info("Source dir: {}", configuration.sourceDirectory.getAbsolutePath());
		LOGGER.info("Output ext: {}", configuration.outputFileExt);
		LOGGER.info("Source ext: {}", configuration.sourceFileExt);
		LOGGER.info("Additional properties: {}", configuration.additionalProperties);
		LOGGER.info("XML Validation: {}", configuration.xmlValidation);
		LOGGER.info("JasperReports Compiler: {}", configuration.compiler);
		LOGGER.info("Number of threads: {}", configuration.numberOfThreads);
		LOGGER.info("classpathElements: {}", configuration.classpathElements);
		LOGGER.info("Additional Classpath: {}", configuration.additionalClasspath);
		LOGGER.info("Source Scanner: {}", configuration.sourceScanner);
	}
}
