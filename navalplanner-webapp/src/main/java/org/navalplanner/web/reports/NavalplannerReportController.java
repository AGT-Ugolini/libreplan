/*
 * This file is part of NavalPlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                    Desenvolvemento Tecnolóxico de Galicia
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.navalplanner.web.reports;

import java.util.Map;

import net.sf.jasperreports.engine.JRDataSource;

import org.navalplanner.web.common.components.ExtendedJasperreport;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Toolbarbutton;

/**
 *
 * Handles the basic behaviour of a Controller for showing reports
 *
 * All reports consists of several input components and a show button which retrieves the necessary data to
 * build resulting report. The method showReport takes care of this behaviour. In addition, when a new report
 * is shown, a link to the report shows up as well.
 *
 * @author Diego Pino Garcia <dpino@igalia.com>
 *
 */
public abstract class NavalplannerReportController extends GenericForwardComposer {

    private static final String HTML = "html";

    protected ComboboxOutputFormat outputFormat;

    protected Hbox URItext;

    protected Toolbarbutton URIlink;

    public void showReport(ExtendedJasperreport jasperreport) {
        final String type = outputFormat.getOutputFormat();

        NavalplannerReport report = new NavalplannerReport(jasperreport,
                getReportName());
        report.setDatasource(getDataSource());
        report.setParameters(getParameters());
        report.show(type);

        String URI = report.show(type);
        if (type.equals(HTML)) {
            URItext.setStyle("display: none");
            Executions.getCurrent().sendRedirect(URI, "_blank");
        } else {
            URItext.setStyle("display: inline");
            URIlink.setHref(URI);
        }
    }

    protected abstract JRDataSource getDataSource();

    protected abstract Map<String, Object> getParameters();

    protected abstract String getReportName();
}
