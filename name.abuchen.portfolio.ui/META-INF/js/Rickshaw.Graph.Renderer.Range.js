Rickshaw.namespace('Rickshaw.Graph.Renderer.Range');

Rickshaw.Graph.Renderer.Range = Rickshaw.Class.create(Rickshaw.Graph.Renderer,
		{

			name : 'range',

			defaults : function($super) {

				return Rickshaw.extend($super(), {
					unstack : false,
					fill : false,
					stroke : false
				});
			},

			seriesPathFactory : function() {

				var graph = this.graph;

				var factory = d3.svg.area().x(function(d) {
					return graph.x(d.x)
				}).y0(function(d) {
					return graph.y(d.y)
				}).y1(function(d) {
					return graph.y(d.y2)
				}).interpolate(graph.interpolation).tension(this.tension);

				factory.defined && factory.defined(function(d) {
					return d.y !== null
				});
				return factory;
			},

			seriesStrokeBottomFactory : function() {

				var graph = this.graph;

				var factory = d3.svg.line().x(function(d) {
					return graph.x(d.x)
				}).y(function(d) {
					return graph.y(d.y)
				}).interpolate(graph.interpolation).tension(this.tension);

				factory.defined && factory.defined(function(d) {
					return d.y !== null
				});
				return factory;
			},

			seriesStrokeTopFactory : function() {

				var graph = this.graph;

				var factory = d3.svg.line().x(function(d) {
					return graph.x(d.x)
				}).y(function(d) {
					return graph.y(d.y2)
				}).interpolate(graph.interpolation).tension(this.tension);

				factory.defined && factory.defined(function(d) {
					return d.y !== null
				});
				return factory;
			},

			render : function(args) {

				args = args || {};

				var graph = this.graph;
				var series = args.series || graph.series;

				var vis = args.vis || graph.vis;
				vis.selectAll('*').remove();

				// insert or stacked areas so strokes lay on top of areas
				var method = this.unstack ? 'append' : 'insert';

				var data = series.filter(function(s) {
					return !s.disabled
				}).map(function(s) {
					return s.stack
				});

				var nodes = vis.selectAll("path").data(data).enter()[method](
						"svg:g", 'g');

				nodes.append("svg:path").attr("d", this.seriesPathFactory())
						.attr("class", 'range');

				nodes.append("svg:path").attr("d",
						this.seriesStrokeBottomFactory()).attr("class",
						'rangeline_bottom');
				nodes.append("svg:path").attr("d",
						this.seriesStrokeTopFactory()).attr("class",
						'rangeline_top');

				var i = 0;
				series.forEach(function(series) {
					if (series.disabled)
						return;
					series.path = nodes[0][i++];
					this._styleSeries(series);
				}, this);
			},

			_styleSeries : function(series) {

				if (!series.path)
					return;

				d3.select(series.path).select('.range').attr('fill',
						series.color);

				if (series.stroke) {
					d3.select(series.path).select('.rangeline_top').attr(
							'fill', 'none').attr(
							'stroke',
							series.stroke
									|| d3.interpolateRgb(series.color, 'black')
											(0.125)).attr('stroke-width',
							series.strokeWidth || this.strokeWidth);
					d3.select(series.path).select('.rangeline_bottom').attr(
							'fill', 'none').attr(
							'stroke',
							series.stroke
									|| d3.interpolateRgb(series.color, 'black')
											(0.125)).attr('stroke-width',
							series.strokeWidth || this.strokeWidth);

					if (series.strokePattern || this.strokePattern) {
						d3.select(series.path).select('.rangeline_top').style(
								"stroke-dasharray", (series.strokePattern));
						d3.select(series.path).select('.rangeline_bottom')
								.style("stroke-dasharray",
										(series.strokePattern));
					}
				} else {
					d3.select(series.path).select('.rangeline_top').attr(
							'fill', 'none').attr('stroke-width', 0);
					d3.select(series.path).select('.rangeline_bottom').attr(
							'fill', 'none').attr('stroke-width', 0);
				}

				if (series.className) {
					series.path.setAttribute('class', series.className);
				}
			}
		});
