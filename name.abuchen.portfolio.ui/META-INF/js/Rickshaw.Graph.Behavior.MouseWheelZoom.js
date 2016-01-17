Rickshaw.namespace('Rickshaw.Graph.Behavior.MouseWheelZoom');

Rickshaw.Graph.Behavior.MouseWheelZoom = function(args) {

	this.graph = args.graph;
	this.xScale = args.graph.xScale || d3.scale.linear();
	this.yScale = args.graph.yScale || d3.scale.linear();
	this.minRangeX = args.minRangeX || 604800; // 60*60*24*7 = 1 week;

	var self = this, initRange = {
		x : {
			min : undefined,
			max : undefined
		},
		y : {
			min : undefined,
			max : undefined
		}
	};

	this.graph.onConfigure(function() {
		self.graph.series
				.forEach(function(series) {
					if (series.disabled) {
						return;
					}

					var domain = self.graph.renderer.domain(series);
					if (initRange.x.min === undefined
							|| domain.x[0] < initRange.x.min) {
						initRange.x.min = domain.x[0];
					}
					if (initRange.x.max === undefined
							|| domain.x[1] > initRange.x.max) {
						initRange.x.max = domain.x[1];
					}
					if (initRange.y.min === undefined
							|| domain.y[0] < initRange.y.min) {
						initRange.y.min = domain.y[0];
					}
					if (initRange.y.max === undefined
							|| domain.y[1] > initRange.y.max) {
						initRange.y.max = domain.y[1];
					}
				});
	});

	this.zoom = {
		zoom_ratio : 0.075,
		getCurrentMinY : function() {
			var result;
			if (self.graph.min === undefined || self.graph.min === 'auto') {
				result = initRange.y.min;
			} else {
				result = self.graph.min;
			}
			return result;
		},
		getCurrentMaxY : function() {
			var result;
			if (self.graph.max === undefined || self.graph.max === 'auto') {
				result = initRange.y.max;
			} else {
				result = self.graph.max;
			}
			return result;
		},
		getCurrentMinX : function() {
			var result;
			if (self.graph.window === undefined
					|| self.graph.window.xMin === undefined) {
				result = initRange.x.min;
			} else {
				result = self.graph.window.xMin;
			}
			return result;
		},
		getCurrentMaxX : function() {
			if (self.graph.window === undefined
					|| self.graph.window.xMax === undefined) {
				result = initRange.x.max;
			} else {
				result = self.graph.window.xMax;
			}
			return result;
		},
		inY : function(y) {
			var minY, maxY, reduceBy, relativePos, coordinate;
			minY = this.getCurrentMinY();
			maxY = this.getCurrentMaxY();
			coordinate = (y == null) ? (maxY - minY) / 2 + minY : y;
			reduceBy = (maxY - minY) * this.zoom_ratio;
			relativePos = (coordinate - minY) / (maxY - minY);
			if ((maxY - minY) < (initRange.y.max - initRange.y.min)
					* this.zoom_ratio) {
				return;
			}
			self.graph.min = minY + (reduceBy * relativePos);
			self.graph.max = maxY - (reduceBy * (1 - relativePos));
			self.graph.render();
		},
		outY : function(y) {
			var minY, maxY, extendBy, relativePos, coordinate;
			minY = this.getCurrentMinY();
			maxY = this.getCurrentMaxY();
			coordinate = (y == null) ? (maxY - minY) / 2 + minY : y;
			extendBy = (maxY - minY) * this.zoom_ratio;
			relativePos = (coordinate - minY) / (maxY - minY);
			minY = minY - (extendBy * relativePos);
			maxY = maxY + (extendBy * (1 - relativePos));
			if (minY < initRange.y.min) {
				maxY = maxY + (initRange.y.min - minY);
			}
			if (maxY > initRange.y.max) {
				minY = minY - (maxY - initRange.y.max);
			}
			self.graph.min = (minY > initRange.y.min) ? minY : initRange.y.min;
			self.graph.max = (maxY < initRange.y.max) ? maxY : initRange.y.max;
			self.graph.render();
		},
		inX : function(x) {
			var minX, maxX, reduceBy, relativePos, coordinate;
			minX = this.getCurrentMinX();
			maxX = this.getCurrentMaxX();
			coordinate = (x == null) ? (maxX - minX) / 2 + minX : x;
			reduceBy = (maxX - minX) * this.zoom_ratio;
			relativePos = (coordinate - minX) / (maxX - minX);
			if ((maxX - minX) < self.minRangeX) {
				return;
			}
			self.graph.window.xMin = minX + (reduceBy * relativePos);
			self.graph.window.xMax = maxX - (reduceBy * (1 - relativePos));
			self.graph.render();
		},
		outX : function(x) {
			var minX, maxX, extendBy, relativePos, coordinate;

			minX = this.getCurrentMinX();
			maxX = this.getCurrentMaxX();
			coordinate = (x == null) ? (maxX - minX) / 2 + minX : x;
			extendBy = (maxX - minX) * this.zoom_ratio;
			relativePos = (coordinate - minX) / (maxX - minX);
			minX = minX - (extendBy * relativePos);
			maxX = maxX + (extendBy * (1 - relativePos));

			if (minX < initRange.x.min) {
				maxX = maxX + (initRange.x.min - minX);
			}

			if (maxX > initRange.x.max) {
				minX = minX - (maxX - initRange.x.max);
			}

			self.graph.window.xMin = (minX > initRange.x.min) ? minX
					: initRange.x.min;
			self.graph.window.xMax = (maxX < initRange.x.max) ? maxX
					: initRange.x.max;

			self.graph.render();
		},
		resetX : function() {
			self.graph.window.xMin = initRange.x.min;
			self.graph.window.xMax = initRange.x.max;
			self.graph.render();
		},
		resetY : function() {
			self.graph.min = initRange.y.min;
			self.graph.max = initRange.y.max;
			self.graph.render();
		},
		reset : function() {
			self.graph.window.xMin = initRange.x.min;
			self.graph.window.xMax = initRange.x.max;
			self.graph.min = initRange.y.min;
			self.graph.max = initRange.y.max;
			self.graph.render();
		}
	};

	$(self.graph.element)
			.mousewheel(
					function(event) {
						event.preventDefault();
						var parentOffset, width, height, relX, relY, translateX, translateY, x, y, swapXY;

						parentOffset = $(self.graph.element).parent().offset();
						width = self.graph.width;
						height = self.graph.height;
						relX = event.pageX - parentOffset.left;
						relY = event.pageY - parentOffset.top;
						translateX = self.xScale.domain([ 0, width ]).range(
								[ initRange.x.min, initRange.x.max ]);
						translateY = self.yScale.domain([ 0, height ]).range(
								[ initRange.y.min, initRange.y.max ]);
						x = translateX(relX);
						y = translateY(height - relY);
						swapXY = event.shiftKey;

						// swap x as y and y as x if shift was pressed
						if ((event.deltaY < 0 && !swapXY)
								|| (event.deltaX < 0 && swapXY)) {
							self.zoom.outY(y);
						} else if ((event.deltaY > 0 && !swapXY)
								|| (event.deltaX > 0 && swapXY)) {
							self.zoom.inY(y);
						} else if ((event.deltaX < 0 && !swapXY)
								|| (event.deltaY < 0 && swapXY)) {
							self.zoom.outX(x);
						} else if ((event.deltaX > 0 && !swapXY)
								|| (event.deltaY > 0 && swapXY)) {
							self.zoom.inX(x);
						}

					});

};
