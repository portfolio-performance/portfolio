Rickshaw.namespace('Rickshaw.Graph.VerticalLine');

Rickshaw.Graph.VerticalLine = Rickshaw.Class
		.create({

			initialize : function(graph, args) {
				var self = this;

				self.graph = graph;
				self.name = args.name;
				self.data = args.data;
				self.color = args.color || 'black';
				self.strokeWidth = args.strokeWidth || 2;
				self.strokePattern = args.strokePattern;
				self.showLabel = args.showLabel;
				self.labelColor = args.labelColor || self.color;
				self.labelOrientation = args.labelOrientation || 'top';
				self.visible = true;

				self.lastEvent = null;
				self.graph.onUpdate(function() {
					self.render()
				});

				self.onShow = args.onShow;
				self.onHide = args.onHide;
				self.onRender = args.onRender;
			},

			render : function() {
				var self = this;
				var graph = self.graph;
				var vis = graph.vis;
				vis.selectAll('VerticalLine').filter(self.name).remove();
				var elements = vis.append('svg:g', 'g').attr('class',
						'VerticalLine').attr('class', self.name);

				this.data.forEach(function(d) {
					elements.append('svg:line').attr('class', 'verticalline')
							.attr('x1', graph.x(d.x)).attr('y1', graph.y(0))
							.attr('x2', graph.x(d.x)).attr('y2', 0).style(
									'stroke-width', self.strokeWidth).style(
									'stroke', d.color || self.color).style(
									'stroke-dasharray', (self.linePattern));
				});

				if (self.showLabel) {
					var textNode = elements.selectAll('vlinetext').data(
							self.data).enter().append('text');

					var labelExtentX = 0;
					var labelStackY = 0;
					var labelPadding = 4;
					var graphY = graph.height;
					var labelPosDirection = (self.labelOrientation
							.toLowerCase() === 'top' ? 1 : -1); // 1=top->down;
																// -1=bottom->up
					var labelPosStartY = (self.labelOrientation.toLowerCase() === 'top' ? labelPadding
							: graph.height - labelPadding); // 1=top->down;
															// -1=bottom->up

					var textLabels = textNode
							.text(function(d) {
								return d.label;
							})
							.attr('x', function(d) {
								return graph.x(d.x) + labelPadding;
							})
							.attr('y', function(d) {
								return labelPosStartY;
								;
							})
							.attr(
									'fill',
									function(d) {
										return d.labelColor || d.color
												|| self.labelColor;
									})
							.attr('class', 'vlinetext')
							.each(
									function(d) {
										var w = this.getBBox().width;
										var h = this.getBBox().height;
										var flip = ((graph.x(d.x)
												+ labelPadding + w) > graph.width);
										var x = (flip ? (graph.x(d.x)
												- labelPadding - w) : graph
												.x(d.x)
												+ labelPadding);

										labelStackY = labelExtentX > x ? labelStackY
												+ (h * labelPosDirection)
												: labelPosStartY
														+ (h * labelPosDirection);
										labelExtentX = x + labelPadding + w;
										d3.select(this).attr('y', labelStackY)
												.attr('x', x);

									});
					;

				}

				if (typeof this.onRender == 'function') {
					this.onRender(args);
				}
			},

			update : function(e) {

				e = e || this.lastEvent;
				if (!e)
					return;
				this.lastEvent = e;

				this.visible && this.render();

			},

			hide : function() {
				this.elements.visible = false;

				if (typeof this.onHide == 'function') {
					this.onHide();
				}
			},

			show : function() {
				this.elements.visible = true;

				if (typeof this.onShow == 'function') {
					this.onShow();
				}
			}
		});