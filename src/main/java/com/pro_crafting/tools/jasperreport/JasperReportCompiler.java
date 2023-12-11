package com.pro_crafting.tools.jasperreport;

import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRPropertiesUtil;
import net.sf.jasperreports.engine.JRReport;
import net.sf.jasperreports.engine.design.JRCompiler;
import net.sf.jasperreports.engine.design.JRJdtCompiler;
import net.sf.jasperreports.engine.xml.JRReportSaxParserFactory;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.SimpleSourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SourceMapping;
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class JasperReportCompiler {
    private static final Logger LOGGER = LoggerFactory.getLogger(JasperReportCompiler.class);
    
    static final String ERROR_JRE_COMPILE_ERROR =
            "Some Jasper reports could not be compiled. See log above for details.";

    private JasperMojoConfiguration configuration;

    public JasperReportCompiler(JasperMojoConfiguration configuration) {
        this.configuration = configuration;
    }

    void compileReports() throws MojoExecutionException {
        if (configuration.skip) {
            LOGGER.info( "Compiling Jasper reports is skipped." );
            return;
        }

        checkOutDirWritable();

        SourceMapping mapping = new SuffixMapping(configuration.sourceFileExt, configuration.outputFileExt);
        Set<File> sources = jrxmlFilesToCompile(mapping);
        if (sources.isEmpty()) {
            LOGGER.info("Nothing to compile - all Jasper reports are up to date");
            return;
        }

        LOGGER.info("Compiling {} Jasper reports design files.", sources.size());

        List<CompileTask> tasks = generateTasks(sources, mapping);
        if (tasks.isEmpty()) {
            LOGGER.info("Nothing to compile");
            return;
        }

        ClassLoader originalTCCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader(originalTCCL));
        try {
            configureJasper();
            executeTasks(tasks);
        } finally {
            if (originalTCCL != null) {
                Thread.currentThread().setContextClassLoader(originalTCCL);
            }
        }
    }

    /**
     * Determines source files to be compiled.
     *
     * @param mapping The source files
     *
     * @return set of jxml files to compile
     *
     * @throws MojoExecutionException When there's trouble with the input
     */
    private Set<File> jrxmlFilesToCompile(SourceMapping mapping) throws MojoExecutionException {
        if (!configuration.sourceDirectory.isDirectory()) {
            if (configuration.failOnMissingSourceDirectory) {
                throw new IllegalArgumentException("Configured source directory " + configuration.sourceDirectory + " is not a directory");
            } else {
                LOGGER.warn("Configured source directory {} is not a directory, skipping JasperReports reports compilation.", configuration.sourceDirectory);
                return Collections.emptySet();
            }
        }

        try {
            SourceInclusionScanner scanner = createSourceInclusionScanner();
            scanner.addSourceMapping(mapping);
            return scanner.getIncludedSources(configuration.sourceDirectory, configuration.outputDirectory);
        }
        catch (InclusionScanException e) {
            throw new MojoExecutionException("Error scanning source root: '" + configuration.sourceDirectory + "'.", e);
        }
    }

    /**
     * Check if the output directory exist and is writable. If not, try to
     * create an output dir and see if that is writable.
     *
     * @throws MojoExecutionException When the output directory is not writable
     */
    private void checkOutDirWritable() throws MojoExecutionException {
        if (!configuration.outputDirectory.exists()) {
            checkIfOutputCanBeCreated();
            checkIfOutputDirIsWritable();
            if (configuration.verbose) {
                LOGGER.info("Output dir check OK");
            }
        } else if (!configuration.outputDirectory.canWrite()) {
            throw new MojoExecutionException(
                    "The output dir exists but was not writable. "
                            + "Try running maven with the 'clean' goal.");
        }
    }

    private void configureJasper() {
        DefaultJasperReportsContext jrContext = DefaultJasperReportsContext.getInstance();

        jrContext.setProperty(JRReportSaxParserFactory.COMPILER_XML_VALIDATION, String.valueOf(configuration.xmlValidation));
        jrContext.setProperty(JRCompiler.COMPILER_PREFIX + JRReport.LANGUAGE_JAVA, configuration.compiler == null ? JRJdtCompiler.class.getName() : configuration.compiler);

        if (configuration.additionalProperties != null) {
            JRPropertiesUtil properties = JRPropertiesUtil.getInstance(jrContext);
            for (Map.Entry<String, String> additionalProperty : configuration.additionalProperties.entrySet()) {
                properties.setProperty(additionalProperty.getKey(), additionalProperty.getValue());
            }
        }
    }

    private ClassLoader getClassLoader(ClassLoader classLoader)
            throws MojoExecutionException {
        List<URL> classpath = new ArrayList<>();
        if (configuration.classpathElements != null) {
            for (String element : configuration.classpathElements) {
                try {
                    File f = new File(element);
                    classpath.add(f.toURI().toURL());
                    LOGGER.debug("Added to classpath {}", element);
                } catch (Exception e) {
                    throw new MojoExecutionException(
                            "Error setting classpath " + element + " " + e.getMessage());
                }
            }
        }

        if (configuration.additionalClasspath != null) {
            for (String element : configuration.additionalClasspath.split("[;]")) {
                try {
                    File f = new File(element);
                    classpath.add(f.toURI().toURL());
                    LOGGER.debug("Added additionalClasspath to classpath {}", element);
                } catch (Exception e) {
                    throw new MojoExecutionException("Error setting classpath " + element + " " + e.getMessage());
                }
            }
        }

        if (classpath.isEmpty()) {
            return classLoader;
        }

        URL[] urls = classpath.toArray(new URL[0]);
        return new URLClassLoader(urls, classLoader);
    }

    private List<CompileTask> generateTasks(Set<File> sources, SourceMapping mapping) throws MojoExecutionException {
        List<CompileTask> tasks = new LinkedList<>();
        try {
            String root = configuration.sourceDirectory.getCanonicalPath();

            for (File src : sources) {
                String srcName = getRelativePath(root, src);
                try {
                    File destination = mapping.getTargetFiles(configuration.outputDirectory, srcName).iterator().next();
                    createDestination(destination.getParentFile());
                    tasks.add(new CompileTask(src, destination, configuration.verbose));
                } catch (InclusionScanException e) {
                    throw new MojoExecutionException("Error compiling report design : " + src, e);
                }
            }
        }
        catch (IOException e) {
            throw new MojoExecutionException("Could not getCanonicalPath from source directory " + configuration.sourceDirectory, e);
        }
        return tasks;
    }

    private void createDestination(File destinationDirectory) throws MojoExecutionException {
        if (!destinationDirectory.exists()) {
            if (destinationDirectory.mkdirs()) {
                LOGGER.debug("Created directory {}", destinationDirectory.getName());
            } else {
                throw new MojoExecutionException("Could not create directory " + destinationDirectory.getName());
            }
        }
    }

    private void executeTasks(List<CompileTask> tasks) throws MojoExecutionException {
        ExecutorService threadPool = newThreadPool();
        try {
            long t1 = System.currentTimeMillis();
            List<Future<Void>> output =
                    threadPool.invokeAll(tasks);
            long time = (System.currentTimeMillis() - t1);
            LOGGER.info("Generated {} jasper reports in {} seconds", output.size(),  time / 1000.0);
            checkForExceptions(output);
        } catch (InterruptedException e) {
            LOGGER.error("Failed to compile Japser reports: Interrupted!", e);
            throw new MojoExecutionException("Error while compiling Jasper reports", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof JRException) {
                throw new MojoExecutionException(ERROR_JRE_COMPILE_ERROR, e);
            }
            else {
                throw new MojoExecutionException("Error while compiling Jasper reports", e);
            }
        } finally {
            threadPool.shutdown();
        }
    }

    private ExecutorService newThreadPool() {
        return Executors.newFixedThreadPool(configuration.numberOfThreads, new JasperReporterThreadFactory());
    }

    private void checkForExceptions(List<Future<Void>> output) throws InterruptedException, ExecutionException {
        for (Future<Void> future : output) {
            future.get();
        }
    }

    private SourceInclusionScanner createSourceInclusionScanner() throws MojoExecutionException {
        if (configuration.sourceScanner.equals(StaleSourceScanner.class.getName())) {
            return new StaleSourceScanner();
        } else if (configuration.sourceScanner.equals(SimpleSourceInclusionScanner.class.getName())) {
            return new SimpleSourceInclusionScanner(Collections.singleton("**/*" + configuration.sourceFileExt),
                    Collections.emptySet());
        } else {
            throw new MojoExecutionException("sourceScanner not supported: '" + configuration.sourceScanner + "'.");
        }
    }

    private void checkIfOutputCanBeCreated() throws MojoExecutionException {
        if (!configuration.outputDirectory.mkdirs()) {
            throw new MojoExecutionException(this, "Output folder could not be created", "Outputfolder "
                    + configuration.outputDirectory.getAbsolutePath() + " is not a folder");
        }
    }

    private void checkIfOutputDirIsWritable() throws MojoExecutionException {
        if (!configuration.outputDirectory.canWrite()) {
            throw new MojoExecutionException(this, "Could not write to output folder",
                    "Could not write to output folder: " + configuration.outputDirectory.getAbsolutePath());
        }
    }

    private String getRelativePath(String root, File file) throws MojoExecutionException {
        try {
            return file.getCanonicalPath().substring(root.length() + 1);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not getCanonicalPath from file " + file, e);
        }
    }
}
