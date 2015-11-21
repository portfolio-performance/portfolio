Rickshaw.namespace('Rickshaw.Graph.Behavior.DragZoomedChart');

Rickshaw.Graph.Behavior.DragZoomedChart = function(args) {
	var self;
	self = this;

	this.graph = args.graph;
	this.xScale = args.graph.xScale || d3.scale.linear();
	this.yScale = args.graph.yScale || d3.scale.linear();

	this.isMouseDown = false;
	// true when mouse button was pressed down above chart, will be set false when released or mouse leaves chart area
	self.isMoving = false;
	// set to true when chart viewport change is processed to prevent mousemove event to trigger to often
	this.MIN_MOUSEMOVE_DELTA = 2;
	// min. distance to recognize mousemove as drag attempt
	this.mouseDownPos = {
		x : undefined,
		y : undefined
	};
	this.currentRange = {
		getCurrentMinY : function() {
			var result;
			if (self.graph.min === undefined || self.graph.min === 'auto') {
				result = self.initRange.y.min;
			} else {
				result = self.graph.min;
			}
			return result;
		},
		getCurrentMaxY : function() {
			var result;
			if (self.graph.max === undefined || self.graph.max === 'auto') {
				result = self.initRange.y.max;
			} else {
				result = self.graph.max;
			}
			return result;
		},
		getCurrentMinX : function() {
			var result;
			if (self.graph.window === undefined || self.graph.window.xMin === undefined) {
				result = self.initRange.x.min;
			} else {
				result = self.graph.window.xMin;
			}
			return result;
		},
		getCurrentMaxX : function() {
			if (self.graph.window === undefined || self.graph.window.xMax === undefined) {
				result = self.initRange.x.max;
			} else {
				result = self.graph.window.xMax;
			}
			return result;
		},
		translateScreen2CurrentChartRangeX : function(screenX) {
			return self.xScale.domain([0, self.graph.width]).range([this.getCurrentMinX(), this.getCurrentMaxX()])(screenX);

		},
		translateScreen2CurrentChartRangeY : function(screenY) {
			return self.yScale.domain([0, self.graph.height]).range([this.getCurrentMinY(), this.getCurrentMaxY()])(screenY);
		}
	};

	this.initRange = {
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
		self.graph.series.forEach(function(series) {
			if (series.disabled) {
				return;
			}
			var domain = self.graph.renderer.domain(series);
			if (self.initRange.x.min === undefined || domain.x[0] < initRange.x.min) {
				self.initRange.x.min = domain.x[0];
			}
			if (self.initRange.x.max === undefined || domain.x[1] > initRange.x.max) {
				self.initRange.x.max = domain.x[1];
			}
			if (self.initRange.y.min === undefined || domain.y[0] < initRange.y.min) {
				self.initRange.y.min = domain.y[0];
			}
			if (self.initRange.y.max === undefined || domain.y[1] > initRange.y.max) {
				self.initRange.y.max = domain.y[1];
			}
		});
	});

	this.MoveChartViewPort = function(relX, relY) {
		var minX, maxX, minY, maxY, deltaX, deltaY;
		minX = self.currentRange.getCurrentMinX();
		maxX = self.currentRange.getCurrentMaxX();
		minY = self.currentRange.getCurrentMinY();
		maxY = self.currentRange.getCurrentMaxY();

		deltaX = self.currentRange.translateScreen2CurrentChartRangeX(relX) - minX;
		deltaY = self.currentRange.translateScreen2CurrentChartRangeY(relY) - minY;

		if (minX + deltaX < self.initRange.x.min) {
			deltaX = self.initRange.x.min - minX;
		}
		if (maxX + deltaX > self.initRange.x.max) {
			deltaX = self.initRange.x.max - maxX;
		}
		if (minY + deltaY < self.initRange.y.min) {
			deltaY = self.initRange.y.min - minY;
		}
		if (maxY + deltaY > self.initRange.y.max) {
			deltaY = self.initRange.y.max - maxY;
		}

		self.graph.window.xMin = minX + deltaX;
		self.graph.window.xMax = maxX += deltaX;
		self.graph.min = minY + deltaY;
		self.graph.max = maxY + deltaY;
		self.graph.render();
	};

	$(self.graph.element).mousedown(function(event) {
		if (event.which === 1) {
			var chartElement;
			chartElement = $(self.graph.element);
			parentOffset = chartElement.parent().offset();
			self.mouseDownPos.x = event.pageX - parentOffset.left;
			self.mouseDownPos.y = event.pageY - parentOffset.top;
			self.isMouseDown = true;
			chartElement.css('cursor', 'grabbing');
		}
	});

	$(self.graph.element).mousemove(function(event) {
		if (self.isMouseDown && !self.isMoving) {
			var chartElement, x, y, relX, relY;
			chartElement = $(self.graph.element);
			parentOffset = chartElement.parent().offset();
			x = (event.pageX - parentOffset.left);
			y = (event.pageY - parentOffset.top);
			relX = (x - self.mouseDownPos.x) * -1;
			relY = (y - self.mouseDownPos.y);
			if (Math.abs(relX) > self.MIN_MOUSEMOVE_DELTA || Math.abs(relY) > self.MIN_MOUSEMOVE_DELTA) {
				self.isMoving = true;
				self.MoveChartViewPort(relX, relY);
				self.mouseDownPos.x = x;
				self.mouseDownPos.y = y;
				self.isMoving = false;
			}
		}
	});

	$(self.graph.element).mouseup(function(event) {
		var chartElement;
		chartElement = $(self.graph.element);
		chartElement.css('cursor', 'default');
		self.isMouseDown = false;
	});

	$(self.graph.element).mouseout(function(event) {
		$(this).mouseup();
	});

};
