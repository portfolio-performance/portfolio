// Render a times series chart (visualized by line or area series), including title, legend, mouse over tooltip and horizontal marker (e.g. buys/sells)
// args.title              : Chart Title
// args.series             : array of objects containing the data series with the follwoing attributes
//            .name        : label of the series (e.g. used in the legend)
//            .data        : array of values pairs { x: 01234, y: 56789}, where 01234 is the time in EpochMilliseconds and 56789 the value to be displayed on the y axis
//            .color       : the color of the line, can be a constant color name or rgb-string like 'rgba(255, 0, 0, 0.4)', which will return a slightly transparent red
//            .renderer    : string constant can be of type 'area', 'line' or 'dottedline'
//            .linePattern : only applies to 'dottedline', a string with comma separated numbers (e.g. '4, 2') where each number is the length in pixels of a line/gap pattern
// args.interpolation      : a string constant defining the interpolation style between data points (see D3 documentation), default: 'monotone'
// args.numberFormat       : Number format for y-axis labels, as interpreted by http://code.google.com/p/jquery-numberformatter/, if omitted, the formatKMBT of Rickshaw is used
// args.numberFormatLocale : Locale used to fomrat number, default 'us'
// args.minY               : minimum y-value, default 'auto'
// args.maxY               : maximum y-value, default 'auto'
// args.showLegend         : true (default) if the legend should be displayed, when false, the legend will be hidden
function LineChart(args) {
	'use strict';
	var mouseDown = [false, false, false, false, false, false, false, false], graph, x_axis, y_axis, legend, highlight, hoverDetail, resize, vmarker;

	if (args === undefined)
		return;

	graph = new Rickshaw.Graph({
		element : document.querySelector("#chart"),
		min : (args !== undefined && args.minY !== undefined ? args.minY : 'auto'),
		max : (args !== undefined && args.maxY !== undefined ? args.maxY : undefined),
		renderer : 'multi',
		unstack : (args !== undefined && args.unstack !== undefined ? args.unstack : true),
		interpolation : (args.interpolation !== undefined ? args.interpolation : 'monotone'),
		xScale : d3.time.scale(),
		series : args.series
	});

	x_axis = new Rickshaw.Graph.Axis.Time({
		graph : graph
	});

	if (args !== undefined && args.verticalMarker !== undefined) {
		var vline = new Rickshaw.Graph.VerticalLine(graph, args.verticalMarker);
	};

	y_axis = new Rickshaw.Graph.Axis.Y({
		graph : graph,
		orientation : 'right',
		// tickFormat : Rickshaw.Fixtures.Number.formatKMBT,
		tickFormat : function(n) {
			var result;
			if (args !== undefined && args.numberFormat !== undefined) {
				result = jQuery.formatNumber(n, {
					format : args.numberFormat,
					locale : args.numberFormatLocale || "us"
				});
			} else {
				result = Rickshaw.Fixtures.Number.formatKMBT(n);
			}
			return result;

		},
		element : document.getElementById('y_axis')
	});

	if (!args.noLegend) {
		legend = new Rickshaw.Graph.Legend({
			graph : graph,
			element : document.getElementById('legend')
		});

		highlight = new Rickshaw.Graph.Behavior.Series.Highlight({
			graph : graph,
			legend : legend,
			disabledColor : function(seriesColor) {
				var transparency = 0.4, grayWeigth = 0.6, rgb;
				// handle rgba-color definition separately, since D3 only supports interpolation of RGB
				if (seriesColor.indexOf('rgba') === 0) {
					rgb = d3.rgb('rgb' + seriesColor.substr(4, seriesColor.lastIndexOf(',') + 1) + ')');
				} else {
					rgb = d3.rgb(seriesColor);
				}
				rgb = d3.rgb(d3.interpolateRgb(rgb, d3.rgb('#d8d8d8'))(grayWeigth));
				return 'rgba(' + rgb.r + ',' + rgb.g + ',' + rgb.b + ',' + transparency + ')';
			}
		});

	}

	hoverDetail = new Rickshaw.Graph.HoverDetail({
		graph : graph,

		xFormatter : function(x) {
			var content, // result html of tooltip
			series, // array of series of graph
			numSeries, // number of series in graph
			currentSeries, // current series referenced in series array
			swatch, // swatch displaying the color next to name if series as html
			numDataPoints, // number of data points in current series
			minX, // x-value of first data point
			maxX, // x-value of last data point
			index, // index based of x value relative to minX/maxX range
			y, // y-value of data point at index, displayed if no explicit label is present
			label, // optional label of the data point, if not present, the y-value is shown
			date = new Date(x * 1000);

			content = date.getFullYear() + "-" + ("0" + (date.getMonth() + 1)).slice(-2) + "-" + ("0" + date.getDate()).slice(-2);

			if (mouseDown[0]) {
				series = graph.series;
				numSeries = series.length;
				content += '<table id="detail_table">';
				for (var seriesIndex = 0; seriesIndex < numSeries; seriesIndex++) {
					currentSeries = series[seriesIndex];
					swatch = '<span class="detail_swatch" style="background-color: ' + series[seriesIndex].color + '"></span>';
					numDataPoints = series[seriesIndex].data.length;

					if (!currentSeries.noLegend) {

						content += '<tr>' + '<td>' + swatch + series[seriesIndex].name + '</td>';
						if (numDataPoints > 0) {
							minX = series[seriesIndex].data[0].x;
							maxX = series[seriesIndex].data[numDataPoints - 1].x;
							index = Math.round((x - minX) / (maxX - minX) * (numDataPoints - 1));
							if (x < minX) {
								index = 0;
							} else if (x > maxX) {
								index = numDataPoints - 1;
							}
							y = series[seriesIndex].data[index].y;
							content += '<td class="detail_value">' + y_axis.tickFormat(y) + '</td>';
						}
						content += '</tr>';
					}
				}
				content += '</table>';
			}

			return content;
		}
	});

	resize = function() {
		graph.configure({
			width : window.innerWidth - jQuery('#y_axis').width() - 4,
			height : window.innerHeight - jQuery('#slider').height() - (jQuery('#title').height() + jQuery('#legend').height() + 16)
		});

		graph.render();
		jQuery('#y_axis').height(jQuery('#chart').height());
		// y axis does not recognise the correct height on first refresh for whatever reason -> force resize
		y_axis.setSize(jQuery('#chart').height());

	};

	document.body.onmousedown = function(evt) {
		// 0= left
		// 1= middle
		// 2= right
		if (evt.button <= 8) {
			mouseDown[evt.button] = true;
		}
		hoverDetail.update();
	};

	document.body.onmouseup = function(evt) {
		if (evt.button <= 8) {
			mouseDown[evt.button] = false;
		}
		hoverDetail.update();
	};

	document.addEventListener("contextmenu", function(e) {
		e.preventDefault();
	}, false);

	if (args !== undefined && args.title !== undefined) {
		jQuery('#title').html(args.title);
	};
	window.addEventListener('resize', resize);
	resize();

}
