function(out){
    out.push('<div ',this.domAttrs_(),
            ' z.type="ganttz.task.Task" idTask="', this.id,'"',
            ' class="box" >');

        out.push('<div class="task-labels">', this.getLabelsText(),'</div>');
        out.push('<div class="task-resources">');
            out.push('<div class="task-resources-inner">', this.getResourcesText(),'</div>');
        out.push('</div>');

        out.push('<div class="completion"></div>');
        out.push('<div class="completion2"></div>');

        out.push('<div id="tasktooltip', this.uuid,'" class="task_tooltip">',
                this.getTooltipText(),
                '</div>');

    out.push('</div>');
}