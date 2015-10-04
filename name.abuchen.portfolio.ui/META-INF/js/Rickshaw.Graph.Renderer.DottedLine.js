Rickshaw.namespace('Rickshaw.Graph.Renderer.DottedLine');

Rickshaw.Graph.Renderer.DottedLine = Rickshaw.Class.create(
		Rickshaw.Graph.Renderer, {

			name : 'dottedline',

			defaults : function($super) {

				return Rickshaw.extend($super(), {
					unstack : true,
					fill : false,
					stroke : true,
					interpolation : 'linear',
					linePattern : undefined
				});
			},

			seriesPathFactory : function() {

				var graph = this.graph;
				var interpolation;

				var factory = d3.svg.line().x(function(d) {
					return graph.x(d.x)
				}).y(function(d) {
					return graph.y(d.y + d.y0)
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

				var nodes = vis.selectAll("line").data(data).enter()[method](
						"svg:g", 'g');

				nodes.append("svg:path").attr("d", this.seriesPathFactory())
						.attr("class", 'line');

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

				d3.select(series.path).select('.line').attr('fill', 'none')
						.attr("stroke-width",
								series.strokeWidth || this.strokeWidth).attr(
								"stroke", series.color);

				if (series.strokePattern) {
					d3.select(series.path).select('.line').style(
							"stroke-dasharray", (series.strokePattern));
				}

				if (series.className) {
					series.path.setAttribute('class', series.className);
				}

			}
		});
