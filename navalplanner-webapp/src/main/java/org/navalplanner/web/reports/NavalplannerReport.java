/*
 * This file is part of ###PROJECT_NAME###
 *
 * Copyright (C) 2009 Fundación para o Fomento da Calidade Industrial e
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

public abstract class NavalplannerReport implements INavalplannerReport {

    private ExtendedJasperreport report;

    public ExtendedJasperreport getReport() {
        return report;
    }

    public void setReport(ExtendedJasperreport report) {
        this.report = report;
    }

    @Override
    public String show(String type) {
        this.report.setType(type);

        String URI = report.getEncodedSrc();
        return URI.replace("navalplanner-webapp/", "");
    }

    public void setParameters(Map parameters) {
        report.setParameters(parameters);
    }

    public void setDatasource(JRDataSource dataSource) {
        report.setDatasource(dataSource);
    }

}
