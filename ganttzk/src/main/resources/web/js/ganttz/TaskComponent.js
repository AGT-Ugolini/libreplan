zk.$package("ganttz");

/*
 * This YAHOO code is here because it's used for the Drag&Drop. Once the Drag&Drop is implemented with jQuery
 * this code must be removed
 * */
YAHOO.example.DDRegion = function(id, sGroup, config) {
    this.cont = config.cont;
    YAHOO.example.DDRegion.superclass.constructor.apply(this, arguments);
};

var myDom = YAHOO.util.Dom, myEvent = YAHOO.util.Event

YAHOO.extend(YAHOO.example.DDRegion, YAHOO.util.DD, {
    cont : null,
    init : function() {
        // Call the parent's init method
    YAHOO.example.DDRegion.superclass.init.apply(this, arguments);
    this.initConstraints();

    myEvent.on(window, 'resize', function() {
        this.initConstraints();
    }, this, true);
},
initConstraints : function() {

    // Get the top, right, bottom and left positions
    var region = myDom.getRegion(this.cont);

    // Get the element we are working on
    var el = this.getEl();

    // Get the xy position of it
    var xy = myDom.getXY(el);

    // Get the width and height
    var width = parseInt(myDom.getStyle(el, 'width'), 10);
    var height = parseInt(myDom.getStyle(el, 'height'), 10);

    // Set left to x minus left
    var left = xy[0] - region.left;

    // Set right to right minus x minus width
    var right = region.right - xy[0] - width;

    // Set top to y minus top
    var top = xy[1] - region.top;

    // Set bottom to bottom minus y minus height
    var bottom = region.bottom - xy[1] - height;

    // Set the constraints based on the above calculations
    this.setXConstraint(left, right);
    this.setYConstraint(top, bottom);
}
});


ganttz.TaskComponent = zk.$extends(zk.Widget, {
    $define :{
        resourcesText    : null,
        labelsText    : null,
        tooltipText : null
    },
    bind_ : function(event){
        this.$supers('bind_', arguments);
        this.domListen_(this.$n(), "onMouseover", '_showTooltip');
        this.domListen_(this.$n(), "onMouseout", '_hideTooltip');
        if( jq(this.$n()).attr('movingtasksenabled') ) this._addDragDrop();
    },
    unbind_ : function(event){
        this.domUnlisten_(this.$n(), "onMouseout", '_hideTooltip');
        this.domUnlisten_(this.$n(), "onMouseover", '_showTooltip');
        this.$supers('unbind_', arguments);
    },
    addDependency : function(){
        this._createArrow();
    },
    consolidateNewDependency : function(task){
        zAu.send( new zk.Event(this, 'onAddDependency', [task.id]));
        ganttz.DependencyList.getInstance().clear();
    },
    addRelatedDependency : function(dependency){
        if(this._dependencies == undefined) this._dependencies = [];
        this._dependencies.push(dependency);
    },
    _addDragDrop : function(){
        var dragdropregion = this._getDragDropRegion();

        dragdropregion.on('dragEvent', this.proxy(function(ev) {
            // Slight overload. It could be more efficent to overwrite the YUI
            // method
            // that is setting the top property
                jq(this.$n()).css('top','');
                if (this._dependencies != undefined) {
                    jq.each(this._dependencies, function(index, dependency){
                        dependency.draw();
                    });
                }
            }), null, false);
         // Register the event endDragEvent
        dragdropregion.on('endDragEvent', this.proxy(function(ev) {
            var position = jq(this.$n()).position();

            zAu.send(new zk.Event(this, 'onUpdatePosition', [new String(position.left), new String(position.top)]));
//            zkau.send( {
//                uuid : cmp.id,
//                cmd : "updatePosition",
//                data : [ cmp.style.left, cmp.style.top ]
//            });
        }), null, false);
    },
    _createArrow : function(){
        var WGTdependencylist = ganttz.DependencyList.getInstance();
        var unlinkedDependency = new ganttz.UnlinkedDependencyComponent();
        unlinkedDependency.setOrigin(this.$n());

        WGTdependencylist.appendChild(unlinkedDependency, true);
        WGTdependencylist.rerender();

        unlinkedDependency.draw();
    },
    _getDragDropRegion : function(){
        if (typeof (this._dragDropRegion) == 'undefined') {
            // Create the laned drag&drop component
            this._dragDropRegion = new YAHOO.example.DDRegion(this.uuid, '', {
                cont : this.parent.getId()
            });
        }
        return this._dragDropRegion;
    },
    _showTooltip : function(){
        this.mouseOverTask = true;
        this._tooltipTimeout = setTimeout(jq.proxy(function(offset) {
            var element = jq("#tasktooltip" + this.uuid);
            if (element!=null) {
                element.show();
                offset = ganttz.GanttPanel.getInstance().getXMouse()
                        - element.parent().offset().left
                        - jq('.leftpanelcontainer').offsetWidth
                        - this.$class._PERSPECTIVES_WIDTH
                        + jq('.rightpanellayout div').scrollLeft();
                element.css( 'left' , offset +'px' );
            }
        }, this), this.$class._TOOLTIP_DELAY);
    },
    _hideTooltip : function(){
        this.mouseOverTask = false;
        if (this._tooltipTimeout) {
            clearTimeout(this._tooltipTimeout);
        }
        jq('#tasktooltip' + this.uuid).hide();
    },
    moveDeadline : function(width){
        jq('#deadline' + this.parent.uuid).css('left', width);
    },
    moveConsolidatedline : function(width){
        jq('#consolidatedline' + this.parent.uuid).css('left', width);
    },
    resizeCompletionAdvance : function(width){
        jq('#' + this.uuid + ' > .completion:first').css('width', width);
    },
    resizeCompletion2Advance : function(width){
        jq('#' + this.uuid + ' > .completion2:first').css('width', width);
    },
    setClass : function(){},
    showTaskLabel : function(){
        jq('#'+ this.uuid + ' .task-labels').show();
    },
    hideTaskLabel : function(){
        jq('#'+ this.uuid + ' .task-labels').hide();
    },
    showResourceTooltip : function(){
        jq('#'+ this.uuid + ' .task-resources').show();
    },
    hideResourceTooltip : function(){
        jq('#'+ this.uuid + ' .task-resources').hide();
    }
},{
    //"Class" methods and properties
    _TOOLTIP_DELAY : 10, // 10 milliseconds
    _PERSPECTIVES_WIDTH : 80,
    CORNER_WIDTH : 20,
    HEIGHT : 10,
    HALF_HEIGHT : 5
});