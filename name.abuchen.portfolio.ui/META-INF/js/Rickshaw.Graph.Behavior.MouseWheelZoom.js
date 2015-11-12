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

	console.log(this.yScale);

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
						var ZOOM_RATIO = 0.1;
						var zoomInY = function(coordinate, minY, maxY) {
							var reduceBy = (maxY - minY) * ZOOM_RATIO;
							var relativePos = (coordinate - minY)
									/ (maxY - minY);
							self.graph.min = minY + (reduceBy * relativePos);
							self.graph.max = maxY
									- (reduceBy * (1 - relativePos));
							self.graph.render();

						}, zoomOutY = function(coordinate, minY, maxY) {
							var extendBy = (maxY - minY) * ZOOM_RATIO;
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
						}, zoomInX = function(coordinate) {

						}, zoomOutX = function(coordinate) {

						};

						var parentOffset = $(self.graph.element).parent()
								.offset()
						width = self.graph.height, height = self.graph.height,
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

						if (event.deltaY != 0) {
							var minY, maxY;
							if (self.graph.min == undefined
									|| self.graph.min == 'auto')
								minY = initRange.y.min;
							else
								minY = self.graph.min;
							if (self.graph.max == undefined
									|| self.graph.max == 'auto')
								maxY = initRange.y.max;
							else
								maxY = self.graph.max;

							if (event.deltaY < 0) { // -1=down; +1=up
								zoomOutY(y, minY, maxY);
							} else {
								zoomInY(y, minY, maxY);
							}
						}

						if (event.deltaX < 0) { // -1=down; +1=up
							zoomOutX(x);
						} else if (event.deltaX > 0) {
							zoomInX(x);
						}
					});

};