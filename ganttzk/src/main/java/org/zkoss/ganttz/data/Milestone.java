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

package org.zkoss.ganttz.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.zkoss.ganttz.util.WeakReferencedListeners;
import org.zkoss.ganttz.util.WeakReferencedListeners.IListenerNotification;

/**
 * @author Lorenzo Tilve Álvaro <ltilve@igalia.com>
 */
public class Milestone extends Task {

    public Milestone() {
        super();
    }

    public Milestone(ITaskFundamentalProperties fundamentalProperties) {
        super(fundamentalProperties);
    }

    private List<Task> tasks = new ArrayList<Task>();

    private boolean expanded = false;

    @Override
    public List<Task> getTasks() {
        return tasks;
    }

    @Override
    public boolean isExpanded() {
        return expanded;
    }

    @Override
    protected void setVisible(boolean visible) {
        super.setVisible(visible);
        if (!this.expanded) {
            return;
        }
        for (Task task : tasks) {
            task.setVisible(true);
        }
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

}
