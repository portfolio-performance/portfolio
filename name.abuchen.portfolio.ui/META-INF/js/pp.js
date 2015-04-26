!function(){
	
	var pp = {
		resizetime: new Date(1, 1, 2000, 12, 00, 00),
		timeout: false,
		delta: 200
	}
	
	pp.onResizeEnd = function(callback) {
		window.addEventListener('resize', function() {
			pp.resizetime = new Date();
		    if (pp.timeout === false) {
		    	pp.timeout = true;
		        setTimeout(function() { $resizeend(callback) }, pp.delta);
		    }
	    }, true);
	}
	
	function $resizeend(callback) {
	    if (new Date() - pp.resizetime < pp.delta) {
	        setTimeout(function() { $resizeend(callback) }, pp.delta);
	    } else {
	    	pp.timeout = false;
	    	callback();
	    }               
	}
	
	this.pp = pp;
}();
