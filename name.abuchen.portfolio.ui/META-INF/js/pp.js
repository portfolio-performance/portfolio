!function() {

	var pp = {
		resizetime : new Date(1, 1, 2000, 12, 00, 00),
		timeout : false,
		delta : 200
	}

	pp.onResizeEnd = function(callback) {
		window.addEventListener('resize', function() {
			pp.resizetime = new Date();
			if (pp.timeout === false) {
				pp.timeout = true;
				setTimeout(function() {
					$resizeend(callback)
				}, pp.delta);
			}
		}, true);
	}

	function $resizeend(callback) {
		if (new Date() - pp.resizetime < pp.delta) {
			setTimeout(function() {
				$resizeend(callback)
			}, pp.delta);
		} else {
			pp.timeout = false;
			callback();
		}
	}

	pp.getInitialHeight = function() {
		if (typeof (document.height) == 'number') {
			height = document.height;
		} else {
			height = window.innerHeight - 4;
		}
		if (height <=0){
			height = 50;
		}		
		return height;
	}
	
	pp.getInitialWidth = function() {
		if (typeof (document.width) == 'number') {
			width = document.width;
		} else {
			width = window.innerWidth - 4;
		}
		if (width <=0){
			width = 50;
		}		
		return width;
	}
	
	pp.getInnerHeight = function() {
		if (typeof (document.height) == 'number') {
			return window.innerHeight;
		} else {
			return window.innerHeight - 4;
		}
	}
	
	pp.getInnerWidth = function() {
		if (typeof (document.width) == 'number') {
			return window.innerWidth;
		} else {
			return window.innerWidth - 4;
		}
	}

	this.pp = pp;
}();
