Rickshaw.namespace('Rickshaw.Graph.Behavior.MouseWheelZoom');

Rickshaw.Graph.Behavior.MouseWheelZoom = function(args) {

	this.graph = args.graph;
	this.xScale = args.graph.xScale || d3.scale.linear();
	this.yScale = args.graph.yScale || d3.scale.linear();

	var self = this;
	var initRange = {
		x : {
			min : undefined,
			max : undefined
		},
		y : {
			min : undefined,
			max : undefined
		}
	}

	this.graph.onConfigure(function() {
		self.graph.series.forEach(function(series) {
			if (series.disabled)
				return;
			var domain = self.graph.renderer.domain(series);
			if (initRange.x.min == undefined || domain.x[0] < initRange.x.min)
				initRange.x.min = domain.x[0];
			if (initRange.x.max == undefined || domain.x[1] > initRange.x.max)
				initRange.x.max = domain.x[1];
			if (initRange.y.min == undefined || domain.y[0] < initRange.y.min)
				initRange.y.min = domain.y[0];
			if (initRange.y.max == undefined || domain.y[1] > initRange.y.max)
				initRange.y.max = domain.y[1];
		});
	});

	$(self.graph.element)
			.mousewheel(
					function(event) {

						var zoom = {
							zoom_ratio : 0.075,
							getCurrentMinY : function() {
								if (self.graph.min == undefined
										|| self.graph.min == 'auto')
									return initRange.y.min;
								else
									return self.graph.min;
							},
							getCurrentMaxY : function() {
								if (self.graph.max == undefined
										|| self.graph.max == 'auto')
									return initRange.y.max;
								else
									return self.graph.max;
							},
							getCurrentMinX : function() {
								if (self.graph.window == undefined
										|| self.graph.window.xMin == undefined)
									return initRange.x.min;
								else
									return self.graph.window.xMin;
							},
							getCurrentMaxX : function() {
								if (self.graph.window == undefined
										|| self.graph.window.xMax == undefined)
									return initRange.x.max;
								else
									return self.graph.window.xMax;
							},
							inY : function(coordinate) {
								var minY = this.getCurrentMinY();
								var maxY = this.getCurrentMaxY();
								var reduceBy = (maxY - minY) * this.zoom_ratio;
								var relativePos = (coordinate - minY)
										/ (maxY - minY);

								if ((maxY - minY) < (initRange.y.max - initRange.y.min)
										* this.zoom_ratio)
									return;

								self.graph.min = minY
										+ (reduceBy * relativePos);
								self.graph.max = maxY
										- (reduceBy * (1 - relativePos));
								
								self.graph.render();
							},
							outY : function(coordinate) {
								var minY = this.getCurrentMinY();
								var maxY = this.getCurrentMaxY();
								var extendBy = (maxY - minY) * this.zoom_ratio;
								var relativePos = (coordinate - minY)
										/ (maxY - minY);

								minY = minY - (extendBy * relativePos);
								maxY = maxY + (extendBy * (1 - relativePos));

								if (minY < initRange.y.min)
									maxY = maxY + (initRange.y.min - minY);
								if (maxY > initRange.y.max)
									minY = minY - (maxY - initRange.y.max);

								self.graph.min = (minY > initRange.y.min) ? minY
										: initRange.y.min;
								self.graph.max = (maxY < initRange.y.max) ? maxY
										: initRange.y.max;
								
								self.graph.render();
							},
							inX : function(coordinate) {
								var minX = this.getCurrentMinX();
								var maxX = this.getCurrentMaxX();						
								var reduceBy = (maxX - minX) * this.zoom_ratio;
								var relativePos = (coordinate - minX)
										/ (maxX - minX);

								if ((maxX - minX) < (initRange.x.max - initRange.x.min)
										* this.zoom_ratio)
									return;

								self.graph.window.xMin = minX
										+ (reduceBy * relativePos);
								self.graph.window.xMax = maxX
										- (reduceBy * (1 - relativePos));
								
								self.graph.render();
							},
							outX : function(coordinate) {
								var minX = this.getCurrentMinX();
								var maxX = this.getCurrentMaxX();							
								var extendBy = (maxX - minX) * this.zoom_ratio;
								var relativePos = (coordinate - minX)
										/ (maxX - minX);

								minX = minX - (extendBy * relativePos);
								maxX = maxX + (extendBy * (1 - relativePos));

								if (minX < initRange.x.min)
									maxX = maxX + (initRange.x.min - minX);
								if (maxX > initRange.x.max)
									minX = minX - (maxX - initRange.x.max);

								self.graph.window.xMin = (minX > initRange.x.min) ? minX
										: initRange.x.min;
								self.graph.window.xMax = (maxX < initRange.x.max) ? maxX
										: initRange.x.max;

								self.graph.render();
							}
						}

						var parentOffset = $(self.graph.element).parent()
								.offset()
						width = self.graph.width, height = self.graph.height,
								relX = event.pageX - parentOffset.left,
								relY = event.pageY - parentOffset.top,
								translateX = self.xScale.domain([ 0, width ])
										.range(
												[ initRange.x.min,
														initRange.x.max ]),
								translateY = self.yScale.domain([ 0, height ])
										.range(
												[ initRange.y.min,
														initRange.y.max ]);
						x = translateX(relX), y = translateY(self.graph.height
								- relY);
						
						// swap x as y and y as x if shift was pressed
						if (event.deltaY < 0 && !event.shiftKey)
							zoom.outY(y);
						else if (event.deltaY > 0 && !event.shiftKey)
							zoom.inY(y);
						else if (event.deltaX < 0 && !event.shiftKey)
							zoom.outX(x);
						else if (event.deltaX > 0 && !event.shiftKey)
							zoom.inX(x);
						else if (event.deltaY < 0 && event.shiftKey)
							zoom.outX(x);
						else if (event.deltaY > 0 && event.shiftKey)
							zoom.inX(x);
						else if (event.deltaX < 0 && event.shiftKey)
							zoom.outY(y);
						else if (event.deltaX > 0 && event.shiftKey)
							zoom.inY(y);

					});

};