package com.pro_crafting.tools.jasperreport;

import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.util.NullOutputStream;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class JasperReportCompilerTest {

    @BeforeAll
    static void beforeAll() throws IOException {
        FileUtils.deleteDirectory( new File("target/unitTestReports"));
    }

    /**
     * Test the normal generation of Jasper reports. The files are retrieved from the official
     * jasper examples folder. No errors or warnings should occur.
     *
     * @throws Exception When an unexpexted error occures.
     */
    @Test
    void testValidReportGeneration() throws Exception {
        JasperMojoConfiguration configuration = buildDefaultConfiguration();
        configuration.sourceDirectory = new File("target/test-classes/exampleFolders/sampleReports");
        configuration.outputDirectory = new File("target/unitTestReports/testValidReportGeneration");

        new JasperReportCompiler(configuration).compileReports();

        assertEquals(configuration.sourceDirectory.listFiles().length, configuration.outputDirectory.listFiles().length, "Files from sourcefolder do not correspond to files in the destinationFolder");
        assertAllFilesAreCompiled(configuration.sourceDirectory, configuration.outputDirectory);
    }

    /**
     * Test the normal generation of Jasper reports with additional properties. The files are
     * retrieved from the official jasper examples folder. No errors or warnings should occur.
     *
     * @throws Exception When an unexpexted error occures.
     */
    @Test
    void testGivenAdditionalPropertiesAreSetWhenTestingValidReportGenerationAndExportToPdfExpectNoErrors()
            throws Exception {
        // given
        JasperMojoConfiguration configuration = buildDefaultConfiguration();
        configuration.sourceDirectory = new File("target/test-classes/exampleFolders/sampleReports");
        configuration.outputDirectory = new File("target/unitTestReports/testGivenAdditionalPropertiesAreSetWhenTestingValidReportGenerationAndExportToPdfExpectNoErrors");
        configuration.additionalProperties = new HashMap<>();
        configuration.additionalProperties.put("net.sf.jasperreports.awt.ignore.missing.font", "true");
        configuration.additionalProperties.put("net.sf.jasperreports.default.pdf.font.name", "Courier");
        configuration.additionalProperties.put("net.sf.jasperreports.default.pdf.encoding", "UTF-8");
        configuration.additionalProperties.put("net.sf.jasperreports.default.pdf.embedded", "true");

        // when
        new JasperReportCompiler(configuration).compileReports();

        // then
        String defaultPdfFontName = DefaultJasperReportsContext.getInstance()
                .getProperty("net.sf.jasperreports.default.pdf.font.name");
        String pdfEmbeddedValue = DefaultJasperReportsContext.getInstance()
                .getProperty("net.sf.jasperreports.default.pdf.embedded");

        assertEquals(configuration.sourceDirectory.listFiles().length, configuration.outputDirectory.listFiles().length, "Files from sourcefolder do not correspond to files in the destinationFolder");
        assertAllFilesAreCompiled(configuration.sourceDirectory, configuration.outputDirectory);
        assertEquals("Courier", defaultPdfFontName, "default pdf font name has not been set properly");
        assertEquals("true", pdfEmbeddedValue, "net.sf.jasperreports.default.pdf.embedded has not been set properly");
        assertTrue(configuration.outputDirectory.isDirectory(), "Destination is not a directory");
        List<File> testFiles = Arrays.asList(configuration.outputDirectory.listFiles(pathname -> pathname.toString()
                .contains("PlainTextReportWithDefaultFontReport")));
        Assertions.assertEquals(1, testFiles.size());

        // when
        File file = new File(configuration.outputDirectory.getPath() + "/" + "PlainTextReportWithDefaultFontReport.jasper");
        try {
            JasperPrint print = JasperFillManager.fillReport(new FileInputStream(file), new HashMap<String, Object>());
            JasperExportManager.exportReportToPdfStream(print, new NullOutputStream());
        } catch (Exception e) {
            fail("Unable to createpdf", e);
        }
    }

    /**
     * For this method to work all files need to be in one folder. The could be enhanced later to
     * also search all subfolders.
     */
    private void assertAllFilesAreCompiled(File sourceFolder, File destinationFolder) {
        assertTrue(sourceFolder.isDirectory(), "Source folder is not a directory");
        assertTrue(destinationFolder.isDirectory(), "Destination is not a directory");
        Set<String> filenames = new HashSet<String>();
        for (File file : sourceFolder.listFiles()) {
            if (file.isFile()) {
                filenames.add(getNameWithoutSuffix(file, ".jrxml"));
            }
        }
        for (File file : destinationFolder.listFiles()) {
            if (file.isFile()) {
                filenames.remove(getNameWithoutSuffix(file, ".jasper"));
            }
        }
        assertTrue(filenames.isEmpty(), "Files from sourcefolder do not correspond to files in the destinationFolder");
    }

    private String getNameWithoutSuffix(File file, String suffix) {
        return file.getName()
                .substring(0, file.getName()
                        .indexOf(suffix));
    }

    /**
     * Test that an invalid Jasper file should stop the build completely by throwing an
     * {@link MojoExecutionException}.
     *
     * @throws Exception When an unexpected error occurs.
     */
    @Test
    public void testInvalidFilesStopBuild() throws Exception {
        // given
        JasperMojoConfiguration configuration = buildDefaultConfiguration();
        configuration.sourceDirectory = new File("src/test/resources/exampleFolders/brokenReports");
        configuration.outputDirectory = new File("target/unitTestReports/testInvalidFilesStopBuild");

        try {
            new JasperReportCompiler(configuration).compileReports();
            fail("An exception should have been thrown");
        } catch (MojoExecutionException e) {
            // then
            assertEquals(JasperReportCompiler.ERROR_JRE_COMPILE_ERROR, e.getMessage());
        }
    }

    /**
     * Test that skipping the plugin does not compile any Jasper file.
     *
     * @throws Exception When an unexpected error occurs.
     */
    @Test
    public void testSkipDoesntCompile() throws Exception {
        // given
        JasperMojoConfiguration configuration = buildDefaultConfiguration();
        configuration.skip = true;
        configuration.sourceDirectory = new File("target/test-classes/exampleFolders/sampleReports");
        configuration.outputDirectory = new File("target/unitTestReports/testSkipDoesntCompile");

        // when
        new JasperReportCompiler(configuration).compileReports();

        // then
        assertFalse(configuration.outputDirectory.exists(), "Output folder should not exist");
    }

    /**
     * Test that all files with an invalid suffix are not compiled.
     *
     * @throws Exception When an unexpected error occurs.
     */
    @Test
    public void testWrongSuffixDoesntCompile() throws Exception {
        // given
        JasperMojoConfiguration configuration = buildDefaultConfiguration();
        configuration.skip = true;
        configuration.sourceDirectory = new File("target/test-classes/exampleFolders/wrongExtensions");
        configuration.outputDirectory = new File("target/unitTestReports/testWrongSuffixDoesntCompile");

        // when
        new JasperReportCompiler(configuration).compileReports();

        // then
        Assertions.assertEquals(0, configuration.outputDirectory.length(), "Output folder should be empty");
    }

    /**
     * Test that an empty folder doesn't create errors but just does nothing.
     *
     * @throws Exception When an unexpected error occurs.
     */
    @Test
    public void testEmptyDoesNothing() throws Exception {
        // given
        JasperMojoConfiguration configuration = buildDefaultConfiguration();
        configuration.skip = true;
        configuration.sourceDirectory = new File("target/test-classes/exampleFolders/emptyFolder");
        configuration.outputDirectory = new File("target/unitTestReports/testEmptyDoesNothing");

        // when
        new JasperReportCompiler(configuration).compileReports();

        // then
        Assertions.assertEquals(0, configuration.outputDirectory.length(), "Output folder should be empty");
    }


    /**
     * Test that a non-existent sourceDirectory fails the build.
     *
     * @throws Exception When an unexpected error occurs.
     */
    @Test
    public void testNonExistentFolderStopBuild() throws Exception {
        // given
        JasperMojoConfiguration configuration = buildDefaultConfiguration();
        configuration.sourceDirectory = new File("target/test-classes/exampleFolders/nonExistentFolder");
        configuration.outputDirectory = new File("target/unitTestReports/testNonExistentFolderStopBuild");

        try {
            // when
            new JasperReportCompiler(configuration).compileReports();
            fail("An exception should have been thrown");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("nonExistentFolder"));
        }
    }

    /**
     * Test that a non-existent sourceDirectory are just skipped if failOnMissingSourceDirectory=true.
     *
     * @throws Exception When an unexpected error occurs.
     */
    @Test
    public void testNonExistentFolderAllowed() throws Exception {
        // given
        JasperMojoConfiguration configuration = buildDefaultConfiguration();
        configuration.failOnMissingSourceDirectory = false;
        configuration.sourceDirectory = new File("target/test-classes/exampleFolders/nonExistentFolder");
        configuration.outputDirectory = new File("target/unitTestReports/testNonExistentFolderAllowed");

        // when
        new JasperReportCompiler(configuration).compileReports();

        // then
        assertEquals(0, configuration.outputDirectory.list().length, "Output folder should be empty");
    }

    /**
     * Test that the folder structure of the output is the same as the folder structure of the
     * input.
     *
     * @throws Exception When an unexpected error occurs.
     */
    @Test
    public void testFolderStructure() throws Exception {
        // given
        JasperMojoConfiguration configuration = buildDefaultConfiguration();
        configuration.failOnMissingSourceDirectory = false;
        configuration.sourceDirectory = new File("target/test-classes/exampleFolders/folderStructure");
        configuration.outputDirectory = new File("target/unitTestReports/testFolderStructure");

        // when
        new JasperReportCompiler(configuration).compileReports();

        // then
        Set<String> filenames = detectFolderStructure(configuration.outputDirectory);
        String relativePath = configuration.outputDirectory.getAbsolutePath() + '/';
        String fileMissing = "A file in the folderstructure is missing";
        assertTrue(filenames.remove(new File(relativePath + "LandscapeReport.jasper").getAbsolutePath()), fileMissing);
        assertTrue(filenames.remove(new File(relativePath + "level.1/level.2.1/LateOrdersReport.jasper").getAbsolutePath()), fileMissing);
        assertTrue(filenames.remove(new File(relativePath + "level.1/level.2.2/MasterReport.jasper").getAbsolutePath()), fileMissing);
        assertTrue(filenames.remove(new File(relativePath + "level.1/level.2.2/Level.3/LineChartReport.jasper").getAbsolutePath()), fileMissing);
        assertTrue(filenames.isEmpty(), "There were more files found then expected");
    }

    private Set<String> detectFolderStructure(File folderToSearch) {
        Set<String> set = new HashSet<String>();
        for (File f : folderToSearch.listFiles()) {
            if (f.isDirectory()) {
                set.addAll(detectFolderStructure(f));
            } else {
                set.add(f.getAbsolutePath());
            }
        }
        return set;
    }

    private JasperMojoConfiguration buildDefaultConfiguration() {
        JasperMojoConfiguration configuration = new JasperMojoConfiguration();

        configuration.xmlValidation = true;
        configuration.numberOfThreads = 4;
        configuration.compiler = "net.sf.jasperreports.engine.design.JRJdtCompiler";
        configuration.outputDirectory = new File("target/jasper");
        configuration.sourceDirectory = new File("src/main/jasperreports");
        configuration.sourceFileExt = ".jrxml";
        configuration.outputFileExt = ".jasper";
        configuration.failOnMissingSourceDirectory = true;
        configuration.sourceScanner = "org.codehaus.plexus.compiler.util.scan.StaleSourceScanner";


        return configuration;
    }
}
