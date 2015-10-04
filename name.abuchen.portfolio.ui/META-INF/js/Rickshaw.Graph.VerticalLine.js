Rickshaw.namespace('Rickshaw.Graph.VerticalLine');

Rickshaw.Graph.VerticalLine = Rickshaw.Class.create({
		initialize : function (args) {

		this.graph = args.graph;
		this.name = args.name;
		
		var elements = this.elements = vis.append("svg:g", 'g').attr('Class', 'VerticalLine').attr('Class', this.name);
		
		
			this.lastEvent = null;
			this._addListeners();

			this.onShow = args.onShow;
			this.onHide = args.onHide;
			this.onRender = args.onRender;
		},

		update : function (e) {

			e = e || this.lastEvent;
			if (!e)
				return;
			this.lastEvent = e;

		},

		hide : function () {
			this.visible = false;
			this.element.classList.add('inactive');

			if (typeof this.onHide == 'function') {
				this.onHide();
			}
		},

		show : function () {
			this.visible = true;
			this.element.classList.remove('inactive');

			if (typeof this.onShow == 'function') {
				this.onShow();
			}
		},
		render : function (args) {
			var graph = this.graph;

			if (typeof this.onRender == 'function') {
				this.onRender(args);
			}

		}

		_addListeners : function () {

			this.graph.onUpdate(function () {
				this.update()
			}
				.bind(this));

		}

	}

		/*
		 * Rickshaw.namespace('Rickshaw.Graph.Renderer.VerticalLine');
		 * 
		 * Rickshaw.Graph.Renderer.VerticalLine =
		 * Rickshaw.Class.create(Rickshaw.Graph.Renderer, {
		 * 
		 * name : 'verticalline',
		 * 
		 * defaults : function ($super) {
		 * 
		 * return Rickshaw.extend($super(), { unstack : true, fill : false,
		 * stroke : true, linePattern : undefined, showLabel : true }); },
		 * 
		 * line : function (d) { return d3.svg.line() .x(function (d) { return
		 * d.x; }) .y(function (d) { return d.y; }); },
		 * 
		 * render : function (args) {
		 * 
		 * args = args || {};
		 * 
		 * var graph = this.graph, series = args.series || graph.series,
		 * 
		 * vis = args.vis || graph.vis; vis.selectAll('*').remove();
		 * 
		 * var data = series .filter(function (s) { return !s.disabled; });
		 * 
		 * var nodes = vis.selectAll("verticalline") .data(data)
		 * .enter().append("svg:g", 'g');
		 * 
		 * console.log(nodes);
		 * 
		 * var i = 0; series.forEach(function (series) { if (series.disabled)
		 * return;
		 * 
		 * console.log(series);
		 * 
		 * series.data.forEach(function (d) { var node =
		 * nodes.append("svg:line") .attr("class", 'verticalline') .attr("x1",
		 * graph.x(d.x)) .attr("y1", graph.y(0)) .attr("x2", graph.x(d.x))
		 * .attr("y2", 0) .style("stroke-width", series.strokeWidth
		 * ).style("stroke", series.color).style("stroke-dasharray",
		 * (series.linePattern)); console.log(d); });
		 * 
		 * series.path = nodes[0][i++];
		 * 
		 * this._styleSeries(series); }, this); },
		 * 
		 * _styleSeries : function (series) {
		 * 
		 * if (!series.path) return;
		 * 
		 * if (series.linePattern) {
		 * d3.select(series.path).select('.verticalline').style("stroke-dasharray",
		 * (series.linePattern)); }
		 * 
		 * if (series.className) { series.path.setAttribute('class',
		 * series.className); }
		 *  } });
		 */
