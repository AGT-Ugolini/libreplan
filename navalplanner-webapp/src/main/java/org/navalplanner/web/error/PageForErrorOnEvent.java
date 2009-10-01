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

package org.navalplanner.web.error;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.navalplanner.web.common.components.I18n;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.util.GenericForwardComposer;

public class PageForErrorOnEvent extends GenericForwardComposer {

    private static final Log LOG = LogFactory.getLog(PageForErrorOnEvent.class);

    private Component modalWindow;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        logError();
        modalWindow = comp;
        I18n i18NMessge = (I18n) modalWindow.getFellowIfAny("message");
        if (i18NMessge != null) {
            i18NMessge.forceLoad();
        }
    }

    private void logError() {
        Throwable exception = (Throwable) Executions.getCurrent().getAttribute(
                "javax.servlet.error.exception");
        String errorMessage = (String) Executions.getCurrent().getAttribute(
                "javax.servlet.error.message");
        LOG.error(errorMessage, exception);
    }

    public void onClick$continueWorking() {
        modalWindow.detach();
    }

    public void onClick$reload() {
        Executions.sendRedirect(null);
    }

    public void onClick$quitSession() {
        HttpServletRequest nativeRequest = (HttpServletRequest) Executions
                .getCurrent().getNativeRequest();
        nativeRequest.getSession().invalidate();
        Executions.sendRedirect("/");
    }

}
