package com.pro_crafting.tools.jasperreport;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.design.JRJdtCompiler;

public class TestCompilerConfigurationCompiler extends JRJdtCompiler {
    public TestCompilerConfigurationCompiler(JasperReportsContext jasperReportsContext) {
        super(jasperReportsContext);
    }

    @Override
    protected void checkLanguage(String language) throws JRException {
        // compileReport itself is finale, can not overwrite it therefore
        // but checklanguage is called first in compileReport
        jasperReportsContext.setProperty("testcompiler.called", "true");
        super.checkLanguage(language);
    }
}
